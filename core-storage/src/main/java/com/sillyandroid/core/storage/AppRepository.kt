package com.sillyandroid.core.storage

import com.sillyandroid.core.model.CharacterCard
import com.sillyandroid.core.model.ExtensionPackage
import com.sillyandroid.core.model.ImportConflictMode
import com.sillyandroid.core.model.Preset
import com.sillyandroid.core.model.RegexRule
import com.sillyandroid.core.model.RegexRuleSet
import com.sillyandroid.core.model.ThemeConfig
import com.sillyandroid.core.model.WorldBook
import com.sillyandroid.core.model.WorldEntry
import com.sillyandroid.core.network.ConsoleLogger
import java.nio.charset.Charset
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

interface LibraryRepository {
    val characters: StateFlow<List<CharacterCard>>
    val worldBooks: StateFlow<List<WorldBook>>
    val regexRuleSets: StateFlow<List<RegexRuleSet>>
    val presets: StateFlow<List<Preset>>
    val extensions: StateFlow<List<ExtensionPackage>>
    val themes: StateFlow<List<ThemeConfig>>
    val selectedThemeId: StateFlow<String?>
}

interface ImportRepository {
    val importConflictMode: StateFlow<ImportConflictMode>
    fun setImportConflictMode(mode: ImportConflictMode)
    fun importByName(fileName: String, bytes: ByteArray): String
    fun installExtensionFromGit(url: String, ref: String?): String
}

interface ExtensionRepository {
    fun dispatchEvent(event: String)
    fun toggleExtension(extensionId: String)
    fun applyBeforeSendExtensions(input: String): String
    fun applyAfterReceiveExtensions(output: String): String
}

interface ThemeRepository {
    fun applyTheme(themeId: String)
}

interface ChatRepository {
    fun applyRegexToAssistant(text: String): String
    fun buildWorldbookContext(userInput: String): String
}

object StorageProvider {
    private val store = InMemoryAppStore()

    val library: LibraryRepository = store
    val importer: ImportRepository = store
    val extensions: ExtensionRepository = store
    val themes: ThemeRepository = store
    val chat: ChatRepository = store
}

/**
 * 兼容旧调用入口：保留 AppRepository，内部已解耦到独立接口。
 */
object AppRepository :
    LibraryRepository by StorageProvider.library,
    ImportRepository by StorageProvider.importer,
    ExtensionRepository by StorageProvider.extensions,
    ThemeRepository by StorageProvider.themes,
    ChatRepository by StorageProvider.chat {

    fun selectedTheme(): ThemeConfig? {
        val id = selectedThemeId.value ?: return null
        return themes.value.firstOrNull { it.id == id }
    }
}

private class InMemoryAppStore :
    LibraryRepository,
    ImportRepository,
    ExtensionRepository,
    ThemeRepository,
    ChatRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val _characters = MutableStateFlow<List<CharacterCard>>(emptyList())
    override val characters: StateFlow<List<CharacterCard>> = _characters.asStateFlow()

    private val _worldBooks = MutableStateFlow<List<WorldBook>>(emptyList())
    override val worldBooks: StateFlow<List<WorldBook>> = _worldBooks.asStateFlow()

    private val _regexRuleSets = MutableStateFlow<List<RegexRuleSet>>(emptyList())
    override val regexRuleSets: StateFlow<List<RegexRuleSet>> = _regexRuleSets.asStateFlow()

    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    override val presets: StateFlow<List<Preset>> = _presets.asStateFlow()

    private val _extensions = MutableStateFlow<List<ExtensionPackage>>(emptyList())
    override val extensions: StateFlow<List<ExtensionPackage>> = _extensions.asStateFlow()

    private val _themes = MutableStateFlow<List<ThemeConfig>>(emptyList())
    override val themes: StateFlow<List<ThemeConfig>> = _themes.asStateFlow()

    private val _selectedThemeId = MutableStateFlow<String?>(null)
    override val selectedThemeId: StateFlow<String?> = _selectedThemeId.asStateFlow()

    private val _importConflictMode = MutableStateFlow(ImportConflictMode.Rename)
    override val importConflictMode: StateFlow<ImportConflictMode> = _importConflictMode.asStateFlow()

    override fun setImportConflictMode(mode: ImportConflictMode) {
        _importConflictMode.value = mode
        ConsoleLogger.log("import conflict mode=$mode")
    }

    override fun dispatchEvent(event: String) {
        activeExtensionsWithHook(event).forEach { ext ->
            ConsoleLogger.log("event dispatched event=$event ext=${ext.id}")
        }
    }

    override fun importByName(fileName: String, bytes: ByteArray): String {
        return runCatching {
            val lower = fileName.lowercase()
            when {
                lower.endsWith(".png") || lower.endsWith(".webp") -> {
                    val card = importCharacterImage(fileName, bytes)
                    "已导入角色卡: ${card.name}"
                }

                lower.endsWith(".json") -> {
                    val text = bytes.toString(Charset.forName("UTF-8"))
                    importJsonSmart(fileName, text)
                }

                else -> throw IllegalArgumentException("不支持的文件类型: $fileName")
            }
        }.onFailure {
            ConsoleLogger.log("import failed file=$fileName error=${it.message}")
        }.getOrThrow()
    }

    override fun installExtensionFromGit(url: String, ref: String?): String {
        val cleanUrl = url.trim()
        require(cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) {
            "请输入合法 Git URL（http/https）"
        }
        val sourceName = cleanUrl.substringAfterLast('/').removeSuffix(".git").ifBlank { "extension" }
        val name = resolveOrKeepName(sourceName, _extensions.value.map { it.name })
        val ext = ExtensionPackage(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "来自 Git 安装",
            hooks = listOf("onAppStart", "onSettingsOpen", "beforeSend", "afterReceive"),
            permissions = listOf("chatRead", "chatWrite"),
            sourceUrl = cleanUrl,
            sourceRef = ref?.trim()?.ifBlank { null },
            enabled = false,
        )
        _extensions.update { upsertByMode(it, ext, sourceName) }
        ConsoleLogger.log("install extension from git url=$cleanUrl ref=${ext.sourceRef}")
        return "已添加扩展来源: ${ext.name}"
    }

    override fun toggleExtension(extensionId: String) {
        _extensions.update { list ->
            list.map { ext -> if (ext.id == extensionId) ext.copy(enabled = !ext.enabled) else ext }
        }
        ConsoleLogger.log("extension toggled id=$extensionId")
    }

    override fun applyTheme(themeId: String) {
        _selectedThemeId.value = themeId
        ConsoleLogger.log("theme applied id=$themeId")
    }

    override fun applyBeforeSendExtensions(input: String): String {
        var content = input
        activeExtensionsWithHook("beforeSend").forEach { ext ->
            if (!requirePermission(ext, "chatWrite")) return@forEach
            if (ext.beforeSendPrefix.isNotBlank()) {
                content = ext.beforeSendPrefix + content
                ConsoleLogger.log("extension beforeSend id=${ext.id} applied")
            }
        }
        return content
    }

    override fun applyAfterReceiveExtensions(output: String): String {
        var content = output
        activeExtensionsWithHook("afterReceive").forEach { ext ->
            if (!requirePermission(ext, "chatRead")) return@forEach
            if (ext.afterReceiveSuffix.isNotBlank()) {
                content += ext.afterReceiveSuffix
                ConsoleLogger.log("extension afterReceive id=${ext.id} applied")
            }
        }
        return content
    }

    private fun activeExtensionsWithHook(hook: String): List<ExtensionPackage> {
        return _extensions.value.filter { it.enabled && it.hooks.any { h -> h.equals(hook, ignoreCase = true) } }
    }

    private fun requirePermission(ext: ExtensionPackage, permission: String): Boolean {
        val allowed = ext.permissions.any { it.equals(permission, ignoreCase = true) } ||
            ext.permissions.any { it.equals("*", ignoreCase = true) }
        if (!allowed) {
            ConsoleLogger.log("permission denied ext=${ext.id} need=$permission")
        }
        return allowed
    }

    override fun applyRegexToAssistant(text: String): String {
        var output = text
        _regexRuleSets.value.flatMap { it.rules }
            .filter { it.enabled && it.applyTo.lowercase() == "assistant" }
            .forEach { rule ->
                output = runCatching {
                    Regex(rule.findRegex).replace(output, rule.replaceString)
                }.getOrDefault(output)
            }
        return output
    }

    override fun buildWorldbookContext(userInput: String): String {
        val matched = _worldBooks.value.flatMap { wb ->
            wb.entries.filter { entry ->
                entry.enabled && entry.keys.any { key ->
                    key.isNotBlank() && userInput.contains(key, ignoreCase = true)
                }
            }
        }
        if (matched.isEmpty()) return ""
        return matched.joinToString(separator = "\n") { "[World] ${it.content}" }
    }

    private fun importCharacterImage(fileName: String, bytes: ByteArray): CharacterCard {
        val textChunk = extractCharaFromPng(bytes) ?: extractCharaFromWebp(bytes)
            ?: throw IllegalArgumentException("图片内未找到酒馆角色卡字段（chara/chara_card_v2）")

        val root = json.parseToJsonElement(textChunk).jsonObject
        val data = when {
            root["data"] is JsonObject -> root["data"]!!.jsonObject
            root["chara_card_v2"] is JsonObject -> root["chara_card_v2"]!!.jsonObject
            else -> root
        }

        val card = parseCharacterFromJsonObject(fileName, data)

        _characters.update { old ->
            when (_importConflictMode.value) {
                ImportConflictMode.Rename -> old + card
                ImportConflictMode.Overwrite -> {
                    val idx = old.indexOfFirst { it.name == card.name }
                    if (idx >= 0) old.toMutableList().apply { set(idx, card.copy(name = card.name)) }.toList()
                    else old + card.copy(name = card.name)
                }
            }
        }
        ConsoleLogger.log("import character success name=${card.name} mode=${_importConflictMode.value}")
        return card
    }

    private fun parseCharacterFromJsonObject(fileName: String, data: JsonObject): CharacterCard {
        val body = if (data["data"] is JsonObject) data["data"]!!.jsonObject else data
        val sourceName = body.str("name") ?: body.str("char_name") ?: data.str("name") ?: fileName.substringBeforeLast('.')
        val id = UUID.randomUUID().toString()
        return CharacterCard(
            id = id,
            name = resolveOrKeepName(sourceName, _characters.value.map { it.name }),
            description = body.str("description") ?: body.str("char_persona") ?: "",
            personality = body.str("personality") ?: "",
            scenario = body.str("scenario") ?: "",
            firstMessage = body.str("first_mes") ?: body.str("firstMessage") ?: body.str("char_greeting") ?: "",
            mesExample = body.str("mes_example") ?: body.str("example_dialogue") ?: "",
            creator = body.str("creator") ?: "",
            tags = body.arrayStr("tags"),
            avatarPath = fileName,
        )
    }

    private fun importJsonSmart(fileName: String, text: String): String {
        val rootElement = json.parseToJsonElement(text)
        val rootObj = rootElement as? JsonObject
        val rootArr = rootElement as? JsonArray

        if (rootObj == null && rootArr == null) {
            throw IllegalArgumentException("无法识别该 JSON 结构")
        }

        if (rootObj != null && isCharacterJson(rootObj)) {
            val data = when {
                rootObj["data"] is JsonObject -> rootObj["data"]!!.jsonObject
                rootObj["chara_card_v2"] is JsonObject -> rootObj["chara_card_v2"]!!.jsonObject
                else -> rootObj
            }
            val card = parseCharacterFromJsonObject(fileName, data)
            _characters.update { old ->
                when (_importConflictMode.value) {
                    ImportConflictMode.Rename -> old + card
                    ImportConflictMode.Overwrite -> {
                        val idx = old.indexOfFirst { it.name == card.name }
                        if (idx >= 0) old.toMutableList().apply { set(idx, card.copy(name = card.name)) }.toList()
                        else old + card.copy(name = card.name)
                    }
                }
            }
            ConsoleLogger.log("import character json success name=${card.name}")
            return "已导入角色卡: ${card.name}"
        }

        if (rootObj?.containsKey("theme") == true || rootObj?.containsKey("tokens") == true || fileName.lowercase().contains("theme")) {
            val theme = parseTheme(fileName, rootObj ?: JsonObject(emptyMap()))
            _themes.update { upsertByMode(it, theme, theme.name) }
            if (_selectedThemeId.value == null) _selectedThemeId.value = theme.id
            ConsoleLogger.log("import theme success name=${theme.name}")
            return "已导入主题: ${theme.name}"
        }

        if (rootObj != null && (rootObj.containsKey("manifest") || rootObj.containsKey("permissions") || rootObj.containsKey("entry"))) {
            val ext = parseExtension(fileName, rootObj)
            _extensions.update { upsertByMode(it, ext, ext.name) }
            ConsoleLogger.log("import extension success name=${ext.name}")
            return "已导入扩展: ${ext.name}"
        }

        if (rootObj != null && (rootObj.containsKey("entries") || rootObj.containsKey("world_info"))) {
            val wb = parseWorldBook(fileName, rootObj)
            _worldBooks.update { upsertByMode(it, wb, wb.name) }
            ConsoleLogger.log("import worldbook success name=${wb.name}")
            return "已导入世界书: ${wb.name}"
        }

        if (rootObj?.containsKey("regex") == true || rootObj?.containsKey("rules") == true) {
            val rs = parseRegexSet(fileName, rootObj)
            _regexRuleSets.update { upsertByMode(it, rs, rs.name) }
            ConsoleLogger.log("import regex success name=${rs.name}")
            return "已导入正则: ${rs.name}"
        }

        if (
            rootObj?.containsKey("temperature") == true ||
            rootObj?.containsKey("model") == true ||
            rootObj?.containsKey("temp") == true ||
            rootObj?.containsKey("top_p") == true ||
            rootObj?.containsKey("top_k") == true ||
            rootObj?.containsKey("rep_pen") == true ||
            rootObj?.containsKey("sampler_order") == true ||
            rootObj?.containsKey("order") == true
        ) {
            val preset = parsePreset(fileName, rootObj)
            _presets.update { upsertByMode(it, preset, preset.name) }
            ConsoleLogger.log("import preset success name=${preset.name}")
            return "已导入预设: ${preset.name}"
        }

        if (rootArr != null) {
            val rules = rootArr.mapIndexedNotNull { _, e ->
                val o = e as? JsonObject ?: return@mapIndexedNotNull null
                val find = o.str("find") ?: o.str("regex") ?: return@mapIndexedNotNull null
                RegexRule(
                    id = UUID.randomUUID().toString(),
                    findRegex = find,
                    replaceString = o.str("replace") ?: "",
                    applyTo = o.str("applyTo") ?: "assistant",
                    enabled = o.bool("enabled") ?: true,
                )
            }
            if (rules.isNotEmpty()) {
                val sourceName = fileName.substringBeforeLast('.')
                val name = resolveOrKeepName(sourceName, _regexRuleSets.value.map { it.name })
                val rs = RegexRuleSet(UUID.randomUUID().toString(), name, rules)
                _regexRuleSets.update { upsertByMode(it, rs, sourceName) }
                ConsoleLogger.log("import regex array success name=${rs.name}")
                return "已导入正则: ${rs.name}"
            }
        }

        throw IllegalArgumentException("无法识别该 JSON 类型: $fileName")
    }

    private fun <T> upsertByMode(old: List<T>, item: T, sourceName: String): List<T> {
        return when (_importConflictMode.value) {
            ImportConflictMode.Rename -> old + item
            ImportConflictMode.Overwrite -> {
                val idx = old.indexOfFirst { extractName(it) == sourceName }
                if (idx >= 0) old.toMutableList().apply { set(idx, item) }.toList() else old + item
            }
        }
    }

    private fun extractName(item: Any?): String = when (item) {
        is ThemeConfig -> item.name
        is ExtensionPackage -> item.name
        is WorldBook -> item.name
        is RegexRuleSet -> item.name
        is Preset -> item.name
        is CharacterCard -> item.name
        else -> ""
    }

    private fun resolveOrKeepName(name: String, existing: List<String>): String {
        return when (_importConflictMode.value) {
            ImportConflictMode.Overwrite -> name
            ImportConflictMode.Rename -> resolveNameConflict(name, existing)
        }
    }

    private fun resolveNameConflict(name: String, existing: List<String>): String {
        if (!existing.contains(name)) return name
        var index = 1
        while (existing.contains("$name ($index)")) {
            index++
        }
        return "$name ($index)"
    }

    private fun parseTheme(fileName: String, root: JsonObject): ThemeConfig {
        val id = UUID.randomUUID().toString()
        val sourceName = root.str("name") ?: fileName.substringBeforeLast('.')
        val name = resolveOrKeepName(sourceName, _themes.value.map { it.name })
        val tokenObj = when {
            root["tokens"] is JsonObject -> root["tokens"] as JsonObject
            root["theme"] is JsonObject -> root["theme"] as JsonObject
            else -> JsonObject(emptyMap())
        }
        val tokens = tokenObj.mapValues { (_, value) ->
            (value as? JsonPrimitive)?.contentOrNull ?: value.toString()
        }
        return ThemeConfig(id = id, name = name, tokens = tokens)
    }

    private fun parseExtension(fileName: String, root: JsonObject): ExtensionPackage {
        val manifest = if (root["manifest"] is JsonObject) root["manifest"]!!.jsonObject else root
        val hooks = manifest.arrayStr("hooks")
        val sourceName = manifest.str("name") ?: fileName.substringBeforeLast('.')
        val name = resolveOrKeepName(sourceName, _extensions.value.map { it.name })
        return ExtensionPackage(
            id = manifest.str("id") ?: UUID.randomUUID().toString(),
            name = name,
            version = manifest.str("version") ?: "1.0.0",
            description = manifest.str("description") ?: "",
            permissions = manifest.arrayStr("permissions"),
            hooks = hooks,
            beforeSendPrefix = manifest.str("beforeSendPrefix") ?: "",
            afterReceiveSuffix = manifest.str("afterReceiveSuffix") ?: "",
            enabled = false,
        )
    }

    private fun parseWorldBook(fileName: String, root: JsonObject): WorldBook {
        val entriesRaw = when {
            root["entries"] is JsonArray -> root["entries"]!!.jsonArray
            root["world_info"] is JsonArray -> root["world_info"]!!.jsonArray
            else -> JsonArray(emptyList())
        }

        val entries = entriesRaw.mapIndexedNotNull { idx, e ->
            val o = e as? JsonObject ?: return@mapIndexedNotNull null
            val keys = o.arrayStr("keys").ifEmpty { listOfNotNull(o.str("key")) }
            WorldEntry(
                uid = o.str("uid") ?: "$idx",
                keys = keys,
                content = o.str("content") ?: o.str("entry") ?: "",
                enabled = o.bool("enabled") ?: true,
            )
        }

        val sourceName = root.str("name") ?: fileName.substringBeforeLast('.')
        val name = resolveOrKeepName(sourceName, _worldBooks.value.map { it.name })
        return WorldBook(
            id = UUID.randomUUID().toString(),
            name = name,
            entries = entries,
        )
    }

    private fun parseRegexSet(fileName: String, root: JsonObject): RegexRuleSet {
        val rulesArray = when {
            root["regex"] is JsonArray -> root["regex"]!!.jsonArray
            root["rules"] is JsonArray -> root["rules"]!!.jsonArray
            else -> JsonArray(emptyList())
        }

        val rules = rulesArray.mapIndexedNotNull { _, e ->
            val o = e as? JsonObject ?: return@mapIndexedNotNull null
            val find = o.str("find") ?: o.str("regex") ?: return@mapIndexedNotNull null
            RegexRule(
                id = o.str("id") ?: UUID.randomUUID().toString(),
                findRegex = find,
                replaceString = o.str("replace") ?: "",
                applyTo = o.str("applyTo") ?: o.str("scope") ?: "assistant",
                enabled = o.bool("enabled") ?: true,
            )
        }.ifEmpty {
            listOf(
                RegexRule(
                    id = UUID.randomUUID().toString(),
                    findRegex = root.str("find") ?: ".*",
                    replaceString = root.str("replace") ?: "",
                    applyTo = root.str("applyTo") ?: "assistant",
                    enabled = root.bool("enabled") ?: true,
                ),
            )
        }

        val sourceName = root.str("name") ?: fileName.substringBeforeLast('.')
        val name = resolveOrKeepName(sourceName, _regexRuleSets.value.map { it.name })
        return RegexRuleSet(
            id = UUID.randomUUID().toString(),
            name = name,
            rules = rules,
        )
    }

    private fun parsePreset(fileName: String, root: JsonObject): Preset {
        val sourceName = root.str("name") ?: fileName.substringBeforeLast('.')
        val name = resolveOrKeepName(sourceName, _presets.value.map { it.name })

        val resolvedModel = root.str("model")
            ?: root.str("preset")
            ?: root.str("api")
            ?: "gpt-4o-mini"

        val resolvedTemp = root.double("temperature")
            ?: root.double("temp")
            ?: root.double("t")
            ?: 0.8

        val system = root.str("system_prompt")
            ?: root.str("systemPrompt")
            ?: root.str("prompt")
            ?: root.str("instruction")
            ?: ""

        return Preset(
            id = UUID.randomUUID().toString(),
            name = name,
            model = resolvedModel,
            temperature = resolvedTemp,
            systemPrompt = system,
        )
    }

    private fun extractCharaFromPng(bytes: ByteArray): String? {
        var offset = 8
        while (offset + 12 < bytes.size) {
            val length = readInt(bytes, offset)
            val type = bytes.copyOfRange(offset + 4, offset + 8).toString(Charsets.US_ASCII)
            val dataStart = offset + 8
            val dataEnd = dataStart + length
            if (dataEnd + 4 > bytes.size) break

            if (type == "tEXt" || type == "iTXt") {
                val raw = bytes.copyOfRange(dataStart, dataEnd).toString(Charsets.UTF_8)
                val payload = when {
                    raw.startsWith("chara\u0000") -> raw.substringAfter('\u0000')
                    raw.startsWith("chara_card_v2\u0000") -> raw.substringAfter('\u0000')
                    else -> null
                }
                if (payload != null) return decodeBase64IfNeeded(payload)
            }
            offset = dataEnd + 4
        }
        return null
    }

    private fun extractCharaFromWebp(bytes: ByteArray): String? {
        val ascii = bytes.toString(Charsets.ISO_8859_1)
        val keys = listOf("chara\u0000", "chara_card_v2\u0000")
        for (key in keys) {
            val idx = ascii.indexOf(key)
            if (idx < 0) continue
            val start = idx + key.length
            val tail = ascii.substring(start)
            val endJson = tail.indexOfLast { it == '}' }
            if (endJson > 0) {
                val payload = tail.substring(0, endJson + 1)
                return decodeBase64IfNeeded(payload)
            }
            val lineEnd = tail.indexOf('\u0000').let { if (it <= 0) tail.length else it }
            return decodeBase64IfNeeded(tail.substring(0, lineEnd))
        }
        return null
    }

    private fun decodeBase64IfNeeded(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) return trimmed
        return runCatching {
            val decoded = java.util.Base64.getDecoder().decode(trimmed)
            decoded.toString(Charsets.UTF_8)
        }.getOrDefault(trimmed)
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun isCharacterJson(root: JsonObject): Boolean {
        return root.containsKey("spec") ||
            root.containsKey("first_mes") ||
            root.containsKey("char_name") ||
            root.containsKey("char_persona") ||
            root.containsKey("chara_card_v2") ||
            root.containsKey("data")
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.bool(key: String): Boolean? =
        (this[key] as? JsonPrimitive)?.booleanOrNull

    private fun JsonObject.double(key: String): Double? =
        (this[key] as? JsonPrimitive)?.doubleOrNull

    private fun JsonObject.arrayStr(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
}

package com.sillyandroid.feature.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sillyandroid.core.model.ImportConflictMode
import com.sillyandroid.core.model.Role
import com.sillyandroid.feature.importer.readFileBytesFromUri

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    readBytes: (Uri) -> Pair<String, ByteArray>?,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val worlds by viewModel.worldBooks.collectAsStateWithLifecycle()
    val extensions by viewModel.extensions.collectAsStateWithLifecycle()

    var gitUrl by remember { mutableStateOf("") }
    var gitRef by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            readBytes(uri)?.let { (name, bytes) ->
                viewModel.importBytes(name, bytes)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TopPanelBar(
            selected = uiState.topPanel,
            onSelect = viewModel::switchPanel,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(8.dp)) {
                when (uiState.topPanel) {
                    SettingsPanel.Preset -> PresetPanel(
                        presets = presets,
                        selectedPresetId = uiState.selectedPresetId,
                        conflictMode = uiState.importConflictMode,
                        onSelectPreset = viewModel::selectPreset,
                        onSetConflictMode = viewModel::setConflictMode,
                        onImport = { launcher.launch("*/*") },
                    )

                    SettingsPanel.Api -> ApiPanel(
                        uiState = uiState,
                        onUpdateProvider = viewModel::updateProvider,
                        onRefreshModels = viewModel::refreshModels,
                    )

                    SettingsPanel.World -> WorldPanel(
                        worlds = worlds.map { it.name },
                        onImport = { launcher.launch("*/*") },
                    )

                    SettingsPanel.Extension -> ExtensionPanel(
                        extensions = extensions,
                        gitUrl = gitUrl,
                        gitRef = gitRef,
                        onGitUrlChange = { gitUrl = it },
                        onGitRefChange = { gitRef = it },
                        onInstall = { viewModel.installExtensionFromGit(gitUrl, gitRef.ifBlank { null }) },
                        onToggle = viewModel::toggleExtension,
                    )

                    SettingsPanel.Persona -> PersonaPanel(
                        persona = uiState.persona,
                        onPersonaChange = viewModel::updatePersona,
                    )

                    SettingsPanel.Character -> CharacterPanel(
                        characters = characters.map { it.name to it.id },
                        selectedCharacterId = uiState.selectedCharacterId,
                        onSelectCharacter = viewModel::selectCharacter,
                        onImport = { launcher.launch("*/*") },
                    )
                }
            }
        }

        uiState.importError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (uiState.lastImportMessage.isNotBlank()) {
            Text(uiState.lastImportMessage, style = MaterialTheme.typography.bodySmall)
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = when (msg.role) {
                                    Role.System -> "System"
                                    Role.User -> "User"
                                    Role.Assistant -> "Assistant"
                                },
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(msg.content)
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = uiState.input,
                    onValueChange = viewModel::updateInput,
                    label = { Text("输入消息") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::newChat) {
                        Text("新聊天")
                    }
                    Button(
                        onClick = viewModel::send,
                        enabled = !uiState.isLoading,
                    ) {
                        Text("发送")
                    }
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopPanelBar(
    selected: SettingsPanel,
    onSelect: (SettingsPanel) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                SettingsPanel.Preset to "预设",
                SettingsPanel.Api to "API",
                SettingsPanel.World to "世界书",
                SettingsPanel.Extension to "扩展",
                SettingsPanel.Persona to "User",
                SettingsPanel.Character to "角色卡",
            ).forEach { (panel, title) ->
                Button(onClick = { onSelect(panel) }) {
                    Text(if (selected == panel) "[$title]" else title)
                }
            }
        }
    }
}

@Composable
private fun PresetPanel(
    presets: List<com.sillyandroid.core.model.Preset>,
    selectedPresetId: String?,
    conflictMode: ImportConflictMode,
    onSelectPreset: (String?) -> Unit,
    onSetConflictMode: (ImportConflictMode) -> Unit,
    onImport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("预设选择 / 导入 / 调整", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onImport) { Text("导入预设") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RadioButton(selected = conflictMode == ImportConflictMode.Rename, onClick = { onSetConflictMode(ImportConflictMode.Rename) })
            Text("重命名")
            RadioButton(selected = conflictMode == ImportConflictMode.Overwrite, onClick = { onSetConflictMode(ImportConflictMode.Overwrite) })
            Text("覆盖")
        }
        if (presets.isEmpty()) {
            Text("暂无预设")
        } else {
            presets.take(8).forEach { preset ->
                Button(onClick = { onSelectPreset(preset.id) }) {
                    Text("${preset.name}${if (preset.id == selectedPresetId) "（当前）" else ""}")
                }
            }
        }
    }
}

@Composable
private fun ApiPanel(
    uiState: ChatUiState,
    onUpdateProvider: (String, String, String) -> Unit,
    onRefreshModels: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("API 预设 / 链接", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = uiState.providerConfig.baseUrl,
            onValueChange = { onUpdateProvider(it, uiState.providerConfig.apiKey, uiState.providerConfig.model) },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        OutlinedTextField(
            value = uiState.providerConfig.apiKey,
            onValueChange = { onUpdateProvider(uiState.providerConfig.baseUrl, it, uiState.providerConfig.model) },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onRefreshModels, enabled = !uiState.isModelsLoading) {
                Text(if (uiState.isModelsLoading) "获取中..." else "获取模型")
            }
            if (uiState.isModelsLoading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            }
        }
        OutlinedTextField(
            value = uiState.providerConfig.model,
            onValueChange = { onUpdateProvider(uiState.providerConfig.baseUrl, uiState.providerConfig.apiKey, it) },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (uiState.availableModels.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.availableModels.take(10).forEach { model ->
                    Button(onClick = {
                        onUpdateProvider(uiState.providerConfig.baseUrl, uiState.providerConfig.apiKey, model)
                    }) {
                        Text(model)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldPanel(
    worlds: List<String>,
    onImport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("世界书导入 / 调整", style = MaterialTheme.typography.titleSmall)
        Button(onClick = onImport) { Text("导入世界书") }
        if (worlds.isEmpty()) Text("暂无世界书") else worlds.take(10).forEach { Text("- $it") }
    }
}

@Composable
private fun ExtensionPanel(
    extensions: List<com.sillyandroid.core.model.ExtensionPackage>,
    gitUrl: String,
    gitRef: String,
    onGitUrlChange: (String) -> Unit,
    onGitRefChange: (String) -> Unit,
    onInstall: () -> Unit,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("扩展管理 / 下载", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = gitUrl,
            onValueChange = onGitUrlChange,
            label = { Text("Git URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = gitRef,
            onValueChange = onGitRefChange,
            label = { Text("Branch or tag（可选）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(onClick = onInstall, enabled = gitUrl.isNotBlank()) { Text("安装扩展") }
        if (extensions.isEmpty()) {
            Text("暂无扩展")
        } else {
            extensions.take(10).forEach { ext ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${ext.name} (${if (ext.enabled) "已启用" else "未启用"})")
                    Button(onClick = { onToggle(ext.id) }) { Text(if (ext.enabled) "停用" else "启用") }
                }
            }
        }
    }
}

@Composable
private fun PersonaPanel(
    persona: String,
    onPersonaChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("User 人设", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = persona,
            onValueChange = onPersonaChange,
            label = { Text("Persona") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CharacterPanel(
    characters: List<Pair<String, String>>,
    selectedCharacterId: String?,
    onSelectCharacter: (String?) -> Unit,
    onImport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("角色卡选择 / 导入", style = MaterialTheme.typography.titleSmall)
        Button(onClick = onImport) { Text("导入角色卡") }
        if (characters.isEmpty()) {
            Text("暂无角色")
        } else {
            characters.take(10).forEach { (name, id) ->
                Button(onClick = { onSelectCharacter(id) }) {
                    Text("$name${if (id == selectedCharacterId) "（当前）" else ""}")
                }
            }
        }
    }
}

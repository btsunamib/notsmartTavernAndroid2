package com.sillyandroid.feature.importer

import com.sillyandroid.core.model.ImportConflictMode
import com.sillyandroid.core.model.Preset
import com.sillyandroid.core.model.RegexRuleSet
import com.sillyandroid.core.model.ThemeConfig
import com.sillyandroid.core.model.WorldBook
import com.sillyandroid.core.model.CharacterCard
import com.sillyandroid.core.model.ExtensionPackage
import kotlinx.coroutines.flow.StateFlow

interface ImportUseCasePort {
    val characters: StateFlow<List<CharacterCard>>
    val worldBooks: StateFlow<List<WorldBook>>
    val regexSets: StateFlow<List<RegexRuleSet>>
    val presets: StateFlow<List<Preset>>
    val extensions: StateFlow<List<ExtensionPackage>>
    val themes: StateFlow<List<ThemeConfig>>
    val selectedThemeId: StateFlow<String?>
    val importConflictMode: StateFlow<ImportConflictMode>

    fun importBytes(fileName: String, bytes: ByteArray): String

    fun setConflictMode(mode: ImportConflictMode)

    fun toggleExtension(extensionId: String)

    fun applyTheme(themeId: String)
}

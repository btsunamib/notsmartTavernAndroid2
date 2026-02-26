package com.sillyandroid.feature.importer

import com.sillyandroid.core.model.ImportConflictMode
import com.sillyandroid.core.storage.ExtensionRepository
import com.sillyandroid.core.storage.ImportRepository
import com.sillyandroid.core.storage.LibraryRepository
import com.sillyandroid.core.storage.ThemeRepository
import kotlinx.coroutines.flow.StateFlow

class ImportUseCase(
    private val libraryRepository: LibraryRepository,
    private val importRepository: ImportRepository,
    private val extensionRepository: ExtensionRepository,
    private val themeRepository: ThemeRepository,
) : ImportUseCasePort {
    override val characters = libraryRepository.characters
    override val worldBooks = libraryRepository.worldBooks
    override val regexSets = libraryRepository.regexRuleSets
    override val presets = libraryRepository.presets
    override val extensions = libraryRepository.extensions
    override val themes = libraryRepository.themes
    override val selectedThemeId: StateFlow<String?> = libraryRepository.selectedThemeId
    override val importConflictMode: StateFlow<ImportConflictMode> = importRepository.importConflictMode

    override fun importBytes(fileName: String, bytes: ByteArray): String {
        return importRepository.importByName(fileName, bytes)
    }

    override fun setConflictMode(mode: ImportConflictMode) {
        importRepository.setImportConflictMode(mode)
    }

    override fun toggleExtension(extensionId: String) {
        extensionRepository.toggleExtension(extensionId)
    }

    override fun applyTheme(themeId: String) {
        themeRepository.applyTheme(themeId)
    }
}

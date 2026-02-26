package com.sillyandroid.feature.importer

import androidx.lifecycle.ViewModel
import com.sillyandroid.core.model.ImportConflictMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ImportUiState(
    val lastMessage: String = "请选择文件导入（PNG/JSON）",
    val error: String? = null,
)

class ImportViewModel(
    private val importUseCase: ImportUseCasePort,
) : ViewModel() {
    val characters = importUseCase.characters
    val worldBooks = importUseCase.worldBooks
    val regexSets = importUseCase.regexSets
    val presets = importUseCase.presets
    val extensions = importUseCase.extensions
    val themes = importUseCase.themes
    val selectedThemeId = importUseCase.selectedThemeId
    val importConflictMode = importUseCase.importConflictMode

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun importBytes(fileName: String, bytes: ByteArray) {
        runCatching {
            importUseCase.importBytes(fileName, bytes)
        }.onSuccess { msg ->
            _uiState.update { it.copy(lastMessage = msg, error = null) }
        }.onFailure { e ->
            _uiState.update { it.copy(error = e.message ?: "导入失败") }
        }
    }

    fun installExtensionFromGit(url: String, ref: String?) {
        runCatching {
            importUseCase.installExtensionFromGit(url, ref)
        }.onSuccess { msg ->
            _uiState.update { it.copy(lastMessage = msg, error = null) }
        }.onFailure { e ->
            _uiState.update { it.copy(error = e.message ?: "扩展安装失败") }
        }
    }

    fun setConflictMode(mode: ImportConflictMode) {
        importUseCase.setConflictMode(mode)
        _uiState.update { it.copy(lastMessage = "冲突策略已切换为 $mode", error = null) }
    }

    fun toggleExtension(extensionId: String) {
        importUseCase.toggleExtension(extensionId)
        _uiState.update { it.copy(lastMessage = "已切换扩展开关", error = null) }
    }

    fun applyTheme(themeId: String) {
        importUseCase.applyTheme(themeId)
        _uiState.update { it.copy(lastMessage = "已应用主题", error = null) }
    }
}

package com.sillyandroid.app

import androidx.lifecycle.ViewModel
import com.sillyandroid.feature.chat.ChatUseCasePort
import com.sillyandroid.feature.importer.ImportUseCasePort

class AppCoordinatorViewModel(
    private val chatUseCase: ChatUseCasePort,
    private val importUseCase: ImportUseCasePort,
) : ViewModel() {
    val themes = importUseCase.themes
    val selectedThemeId = importUseCase.selectedThemeId

    fun onAppStart() {
        chatUseCase.dispatchEvent("onAppStart")
    }

    fun onSettingsOpen() {
        chatUseCase.dispatchEvent("onSettingsOpen")
    }
}

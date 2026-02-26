package com.sillyandroid.app

import com.sillyandroid.core.network.ChatClient
import com.sillyandroid.core.network.OpenAiCompatibleClient
import com.sillyandroid.core.storage.ChatRepository
import com.sillyandroid.core.storage.ExtensionRepository
import com.sillyandroid.core.storage.ImportRepository
import com.sillyandroid.core.storage.LibraryRepository
import com.sillyandroid.core.storage.StorageProvider
import com.sillyandroid.core.storage.ThemeRepository
import com.sillyandroid.feature.chat.ChatUseCase
import com.sillyandroid.feature.chat.ChatUseCasePort
import com.sillyandroid.feature.chat.ChatViewModel
import com.sillyandroid.feature.importer.ImportUseCase
import com.sillyandroid.feature.importer.ImportUseCasePort
import com.sillyandroid.feature.importer.ImportViewModel

class AppGraph {
    // repositories
    val libraryRepository: LibraryRepository = StorageProvider.library
    val importRepository: ImportRepository = StorageProvider.importer
    val extensionRepository: ExtensionRepository = StorageProvider.extensions
    val themeRepository: ThemeRepository = StorageProvider.themes
    val chatRepository: ChatRepository = StorageProvider.chat

    // network
    val chatClient: ChatClient = OpenAiCompatibleClient()

    // usecases
    val chatUseCase: ChatUseCasePort = ChatUseCase(
        chatClient = chatClient,
        libraryRepository = libraryRepository,
        extensionRepository = extensionRepository,
        importRepository = importRepository,
        chatRepository = chatRepository,
    )

    val importUseCase: ImportUseCasePort = ImportUseCase(
        libraryRepository = libraryRepository,
        importRepository = importRepository,
        extensionRepository = extensionRepository,
        themeRepository = themeRepository,
    )

    // viewmodels
    fun createChatViewModel(): ChatViewModel = ChatViewModel(chatUseCase)

    fun createImportViewModel(): ImportViewModel = ImportViewModel(importUseCase)

    fun createAppCoordinatorViewModel(): AppCoordinatorViewModel =
        AppCoordinatorViewModel(chatUseCase, importUseCase)
}

package com.sillyandroid.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sillyandroid.feature.chat.ChatViewModel
import com.sillyandroid.feature.importer.ImportViewModel

class AppCoordinatorViewModelFactory(
    private val graph: AppGraph,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppCoordinatorViewModel::class.java)) {
            return graph.createAppCoordinatorViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class ChatViewModelFactory(
    private val graph: AppGraph,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return graph.createChatViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

class ImportViewModelFactory(
    private val graph: AppGraph,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImportViewModel::class.java)) {
            return graph.createImportViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

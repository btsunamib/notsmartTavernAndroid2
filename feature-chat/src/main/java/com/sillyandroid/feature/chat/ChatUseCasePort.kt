package com.sillyandroid.feature.chat

import com.sillyandroid.core.model.ChatMessage
import com.sillyandroid.core.model.CharacterCard
import com.sillyandroid.core.model.ExtensionPackage
import com.sillyandroid.core.model.Preset
import com.sillyandroid.core.model.ProviderConfig
import com.sillyandroid.core.model.WorldBook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatUseCasePort {
    val characters: StateFlow<List<CharacterCard>>
    val presets: StateFlow<List<Preset>>
    val worldBooks: StateFlow<List<WorldBook>>
    val extensions: StateFlow<List<ExtensionPackage>>

    fun prepareUserInput(rawInput: String): String

    fun dispatchEvent(event: String)

    suspend fun fetchModels(config: ProviderConfig): List<String>

    fun importBytes(fileName: String, bytes: ByteArray): String

    fun installExtensionFromGit(url: String, ref: String?): String

    fun toggleExtension(extensionId: String)

    fun streamAssistantReply(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        persona: String,
        selectedCharacterId: String?,
        selectedPresetId: String?,
    ): Flow<AssistantOutput>
}

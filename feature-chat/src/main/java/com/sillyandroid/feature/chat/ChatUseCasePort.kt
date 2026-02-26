package com.sillyandroid.feature.chat

import com.sillyandroid.core.model.ChatMessage
import com.sillyandroid.core.model.CharacterCard
import com.sillyandroid.core.model.Preset
import com.sillyandroid.core.model.ProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatUseCasePort {
    val characters: StateFlow<List<CharacterCard>>
    val presets: StateFlow<List<Preset>>

    fun prepareUserInput(rawInput: String): String

    fun dispatchEvent(event: String)

    fun streamAssistantReply(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        persona: String,
        selectedCharacterId: String?,
        selectedPresetId: String?,
    ): Flow<AssistantOutput>
}

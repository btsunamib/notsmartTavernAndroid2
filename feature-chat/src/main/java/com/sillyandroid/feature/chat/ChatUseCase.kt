package com.sillyandroid.feature.chat

import com.sillyandroid.core.model.ChatMessage
import com.sillyandroid.core.model.NetworkMessage
import com.sillyandroid.core.model.ProviderConfig
import com.sillyandroid.core.model.Role
import com.sillyandroid.core.network.ChatClient
import com.sillyandroid.core.network.OpenAiCompatibleClient
import com.sillyandroid.core.storage.ChatRepository
import com.sillyandroid.core.storage.ExtensionRepository
import com.sillyandroid.core.storage.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

data class AssistantOutput(
    val rendered: String,
    val raw: String,
)

class ChatUseCase(
    private val chatClient: ChatClient,
    private val libraryRepository: LibraryRepository,
    private val extensionRepository: ExtensionRepository,
    private val chatRepository: ChatRepository,
) : ChatUseCasePort {
    override val characters: StateFlow<List<com.sillyandroid.core.model.CharacterCard>> = libraryRepository.characters
    override val presets: StateFlow<List<com.sillyandroid.core.model.Preset>> = libraryRepository.presets

    override fun prepareUserInput(rawInput: String): String {
        return extensionRepository.applyBeforeSendExtensions(rawInput)
    }

    override fun dispatchEvent(event: String) {
        extensionRepository.dispatchEvent(event)
    }

    override suspend fun fetchModels(config: ProviderConfig): List<String> {
        return chatClient.fetchModels(config)
    }

    override fun streamAssistantReply(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        persona: String,
        selectedCharacterId: String?,
        selectedPresetId: String?,
    ): Flow<AssistantOutput> {
        val networkMessages = buildNetworkMessages(
            messages = messages,
            persona = persona,
            selectedCharacterId = selectedCharacterId,
            selectedPresetId = selectedPresetId,
        )

        return chatClient.streamChat(config, networkMessages).accumulateAssistantOutput()
    }

    private fun Flow<String>.accumulateAssistantOutput(): Flow<AssistantOutput> {
        var raw = ""
        return map { delta ->
            raw += delta
            val regexProcessed = chatRepository.applyRegexToAssistant(raw)
            val extensionProcessed = extensionRepository.applyAfterReceiveExtensions(regexProcessed)
            AssistantOutput(rendered = extensionProcessed, raw = raw)
        }
    }

    private fun buildNetworkMessages(
        messages: List<ChatMessage>,
        persona: String,
        selectedCharacterId: String?,
        selectedPresetId: String?,
    ): List<NetworkMessage> {
        val selectedCharacter = characters.value.firstOrNull { it.id == selectedCharacterId }
        val selectedPreset = presets.value.firstOrNull { it.id == selectedPresetId }

        val charPrompt = buildString {
            if (selectedCharacter != null) {
                appendLine("角色名: ${selectedCharacter.name}")
                if (selectedCharacter.description.isNotBlank()) appendLine("描述: ${selectedCharacter.description}")
                if (selectedCharacter.personality.isNotBlank()) appendLine("性格: ${selectedCharacter.personality}")
                if (selectedCharacter.scenario.isNotBlank()) appendLine("场景: ${selectedCharacter.scenario}")
                if (selectedCharacter.firstMessage.isNotBlank()) appendLine("首句参考: ${selectedCharacter.firstMessage}")
            }
        }

        val worldContext = chatRepository.buildWorldbookContext(messages.lastOrNull { it.role == Role.User }?.content.orEmpty())
        val presetPrompt = selectedPreset?.systemPrompt.orEmpty()

        val systemPrompt = listOf(persona, presetPrompt, charPrompt, worldContext)
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n\n")

        val system = NetworkMessage(role = "system", content = systemPrompt)
        val history = messages.map {
            NetworkMessage(
                role = when (it.role) {
                    Role.System -> "system"
                    Role.User -> "user"
                    Role.Assistant -> "assistant"
                },
                content = it.content,
            )
        }
        return listOf(system) + history
    }
}

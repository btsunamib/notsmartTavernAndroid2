package com.sillyandroid.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sillyandroid.core.model.ChatMessage
import com.sillyandroid.core.model.ProviderConfig
import com.sillyandroid.core.model.Role
import com.sillyandroid.core.network.ConsoleLogger
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val providerConfig: ProviderConfig = ProviderConfig(
        baseUrl = "https://api.openai.com/v1",
        apiKey = "",
        model = "gpt-4o-mini",
        temperature = 0.8,
    ),
    val persona: String = "你是一个有帮助、简洁的助手。",
    val selectedCharacterId: String? = null,
    val selectedPresetId: String? = null,
)

class ChatViewModel(
    private val chatUseCase: ChatUseCasePort,
) : ViewModel() {

    val characters = chatUseCase.characters
    val presets = chatUseCase.presets

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun updateInput(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun updateProvider(
        baseUrl: String,
        apiKey: String,
        model: String,
    ) {
        _uiState.update {
            it.copy(
                providerConfig = it.providerConfig.copy(
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    model = model,
                ),
            )
        }
    }

    fun updatePersona(value: String) {
        _uiState.update { it.copy(persona = value) }
    }

    fun selectCharacter(id: String?) {
        _uiState.update { it.copy(selectedCharacterId = id) }
        ConsoleLogger.log("selected character id=$id")
    }

    fun selectPreset(id: String?) {
        _uiState.update { state ->
            val selected = presets.value.firstOrNull { it.id == id }
            val nextConfig = if (selected != null) {
                state.providerConfig.copy(
                    model = selected.model,
                    temperature = selected.temperature,
                )
            } else {
                state.providerConfig
            }
            state.copy(selectedPresetId = id, providerConfig = nextConfig)
        }
        ConsoleLogger.log("selected preset id=$id")
    }

    fun newChat() {
        _uiState.update { it.copy(messages = emptyList(), error = null) }
        ConsoleLogger.log("new chat started")
    }

    fun onAppStart() {
        chatUseCase.dispatchEvent("onAppStart")
    }

    fun onSettingsOpen() {
        chatUseCase.dispatchEvent("onSettingsOpen")
    }

    fun send() {
        val current = _uiState.value
        val rawInput = current.input.trim()
        if (rawInput.isEmpty() || current.isLoading) return

        if (current.providerConfig.apiKey.isBlank()) {
            _uiState.update { it.copy(error = "请先在设置里输入 API Key") }
            return
        }

        val text = chatUseCase.prepareUserInput(rawInput)

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = Role.User,
            content = text,
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                input = "",
                isLoading = true,
                error = null,
            )
        }

        viewModelScope.launch {
            val assistantId = UUID.randomUUID().toString()
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMessage(
                        id = assistantId,
                        role = Role.Assistant,
                        content = "",
                    ),
                )
            }

            try {
                chatUseCase.streamAssistantReply(
                    config = _uiState.value.providerConfig,
                    messages = _uiState.value.messages,
                    persona = _uiState.value.persona,
                    selectedCharacterId = _uiState.value.selectedCharacterId,
                    selectedPresetId = _uiState.value.selectedPresetId,
                ).collect { output ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantId) msg.copy(content = output.rendered, rawOutput = output.raw)
                                else msg
                            },
                        )
                    }
                }
                val rawLen = _uiState.value.messages.firstOrNull { it.id == assistantId }?.rawOutput?.length ?: 0
                ConsoleLogger.log("assistant completed chars=$rawLen")
            } catch (e: Exception) {
                ConsoleLogger.log("chat error=${e.message}")
                _uiState.update { it.copy(error = e.message ?: "未知错误") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

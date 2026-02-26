package com.sillyandroid.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProviderConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val temperature: Double = 0.8,
)

@Serializable
enum class ImportConflictMode {
    Rename,
    Overwrite,
}

@Serializable
data class CharacterCard(
    val id: String,
    val name: String,
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val firstMessage: String = "",
    val mesExample: String = "",
    val creator: String = "",
    val tags: List<String> = emptyList(),
    val avatarPath: String? = null,
)

@Serializable
data class WorldBook(
    val id: String,
    val name: String,
    val entries: List<WorldEntry> = emptyList(),
)

@Serializable
data class WorldEntry(
    val uid: String,
    val keys: List<String> = emptyList(),
    val content: String = "",
    val enabled: Boolean = true,
)

@Serializable
data class RegexRuleSet(
    val id: String,
    val name: String,
    val rules: List<RegexRule> = emptyList(),
)

@Serializable
data class RegexRule(
    val id: String,
    val findRegex: String,
    val replaceString: String,
    val applyTo: String = "assistant",
    val enabled: Boolean = true,
)

@Serializable
data class Preset(
    val id: String,
    val name: String,
    val model: String = "gpt-4o-mini",
    val temperature: Double = 0.8,
    val systemPrompt: String = "",
)

@Serializable
data class ExtensionPackage(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val description: String = "",
    val permissions: List<String> = emptyList(),
    val hooks: List<String> = emptyList(),
    val beforeSendPrefix: String = "",
    val afterReceiveSuffix: String = "",
    val enabled: Boolean = false,
)

@Serializable
data class ThemeConfig(
    val id: String,
    val name: String,
    val tokens: Map<String, String> = emptyMap(),
)

@Serializable
data class Persona(
    val name: String,
    val content: String,
)

@Serializable
data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
    val rawOutput: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
enum class Role {
    @SerialName("system")
    System,

    @SerialName("user")
    User,

    @SerialName("assistant")
    Assistant,
}

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<NetworkMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.8,
)

@Serializable
data class NetworkMessage(
    val role: String,
    val content: String,
)

@Serializable
data class OpenAiChunk(
    val id: String? = null,
    val choices: List<Choice> = emptyList(),
) {
    @Serializable
    data class Choice(
        val index: Int = 0,
        val delta: Delta = Delta(),
        @SerialName("finish_reason")
        val finishReason: String? = null,
    )

    @Serializable
    data class Delta(
        val role: String? = null,
        val content: String? = null,
    )
}

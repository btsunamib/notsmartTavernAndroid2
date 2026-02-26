package com.sillyandroid.core.network

import com.sillyandroid.core.model.ChatCompletionRequest
import com.sillyandroid.core.model.NetworkMessage
import com.sillyandroid.core.model.OpenAiChunk
import com.sillyandroid.core.model.ProviderConfig
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

interface ChatClient {
    fun streamChat(
        config: ProviderConfig,
        messages: List<NetworkMessage>,
    ): Flow<String>
}

class OpenAiCompatibleClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder().build(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ChatClient {

    override fun streamChat(
        config: ProviderConfig,
        messages: List<NetworkMessage>,
    ): Flow<String> = flow {
        val requestBody = ChatCompletionRequest(
            model = config.model,
            messages = messages,
            stream = true,
            temperature = config.temperature,
        )

        val requestJson = json.encodeToString(requestBody)
        ConsoleLogger.log("request url=${config.baseUrl}/chat/completions model=${config.model}")
        ConsoleLogger.log("request body=$requestJson")

        val request = Request.Builder()
            .url("${config.baseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                ConsoleLogger.log("response error code=${response.code} body=$errorBody")
                throw IOException("HTTP ${response.code}: $errorBody")
            }

            val body = response.body ?: throw IOException("empty response body")
            body.source().use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload == "[DONE]") {
                        ConsoleLogger.log("stream finished [DONE]")
                        break
                    }

                    try {
                        val chunk = json.decodeFromString(OpenAiChunk.serializer(), payload)
                        val text = chunk.choices.firstOrNull()?.delta?.content
                        if (!text.isNullOrEmpty()) {
                            ConsoleLogger.log("stream delta=$text")
                            emit(text)
                        }
                    } catch (e: Exception) {
                        ConsoleLogger.log("chunk parse error=${e.message} payload=$payload")
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}

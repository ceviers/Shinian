package com.cevier.shinian.data

import com.cevier.shinian.model.ChatMessage
import com.cevier.shinian.model.LlmConfig
import com.cevier.shinian.model.LlmModel
import com.cevier.shinian.model.LlmProvider
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LlmApiClient {
    fun listModels(config: LlmConfig): List<LlmModel> {
        val response = request(config, "models", "GET")
        val data = JSONObject(response).optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                if (id.isBlank()) continue
                val modalities = item.optJSONArray("input_modalities")
                    ?: item.optJSONArray("inputModalities")
                add(
                    LlmModel(
                        id = id,
                        name = item.optString("name").ifBlank { id },
                        supportsImages = modalities?.containsValue("image") == true,
                    ),
                )
            }
        }.distinctBy(LlmModel::id).sortedBy(LlmModel::id)
    }

    fun complete(config: LlmConfig, messages: List<ChatMessage>): String {
        require(config.isReady) { "请先完成 LLM 配置" }
        val body = chatBody(config, messages, stream = false)
        val response = JSONObject(request(config, "chat/completions", "POST", body.toString()))
        val message = response.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?: error("接口没有返回回复内容")
        return message.contentText().ifBlank { error("模型返回了空回复") }
    }

    fun completeStreaming(
        config: LlmConfig,
        messages: List<ChatMessage>,
        onDelta: (String) -> Unit,
    ): String {
        require(config.isReady) { "请先完成 LLM 配置" }
        val connection = openConnection(
            config = config,
            path = "chat/completions",
            method = "POST",
            body = chatBody(config, messages, stream = true).toString(),
        )
        return try {
            val status = connection.responseCode
            if (status !in 200..299) {
                val response = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                error(apiError(response, status))
            }
            val fullText = StringBuilder()
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload == "[DONE]") break
                    if (payload.isBlank()) continue
                    val delta = runCatching {
                        JSONObject(payload)
                            .optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("delta")
                            ?.contentText()
                            .orEmpty()
                    }.getOrDefault("")
                    if (delta.isNotEmpty()) {
                        fullText.append(delta)
                        onDelta(delta)
                    }
                }
            }
            fullText.toString().ifBlank { error("模型返回了空回复") }
        } finally {
            connection.disconnect()
        }
    }

    private fun chatBody(config: LlmConfig, messages: List<ChatMessage>, stream: Boolean): JSONObject {
        val requestMessages = JSONArray()
        messages.forEach { message ->
            requestMessages.put(JSONObject().apply {
                put("role", message.role)
                if (message.role == "user" && message.imageDataUrl != null) {
                    put("content", JSONArray().apply {
                        if (message.text.isNotBlank()) {
                            put(JSONObject().put("type", "text").put("text", message.text))
                        }
                        put(
                            JSONObject()
                                .put("type", "image_url")
                                .put(
                                    "image_url",
                                    JSONObject()
                                        .put("url", message.imageDataUrl)
                                        .put("detail", "auto"),
                                ),
                        )
                    })
                } else {
                    put("content", message.text)
                }
            })
        }
        val body = JSONObject()
            .put("model", config.modelId)
            .put("messages", requestMessages)
            .put("stream", stream)
        if (config.provider == LlmProvider.DEEPSEEK) {
            body.put("thinking", JSONObject().put("type", if (config.thinkingEnabled) "enabled" else "disabled"))
            body.put("reasoning_effort", if (config.thinkingEnabled) "high" else "none")
        } else {
            body.put("reasoning", JSONObject().put("effort", if (config.thinkingEnabled) "high" else "none"))
        }
        return body
    }

    private fun request(config: LlmConfig, path: String, method: String, body: String? = null): String {
        val connection = openConnection(config, path, method, body)
        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                error(apiError(response, status))
            }
            response
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(
        config: LlmConfig,
        path: String,
        method: String,
        body: String?,
    ): HttpURLConnection {
        require(config.apiKey.isNotBlank()) { "请输入 API Key" }
        val endpoint = "${config.baseUrl.trimEnd('/')}/$path"
        return (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", if (body != null) "text/event-stream, application/json" else "application/json")
            if (body != null) {
                doOutput = true
                outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }
        }
    }

    private fun apiError(response: String, status: Int): String {
        val message = runCatching {
            JSONObject(response).optJSONObject("error")?.optString("message")
        }.getOrNull().orEmpty()
        return message.ifBlank { "接口请求失败（HTTP $status）" }
    }

    private fun JSONArray.containsValue(value: String): Boolean =
        (0 until length()).any { optString(it).equals(value, ignoreCase = true) }

    private fun JSONObject.contentText(): String {
        return when (val content = opt("content")) {
            is String -> content
            is JSONArray -> buildString {
                for (index in 0 until content.length()) {
                    val text = content.optJSONObject(index)?.optString("text").orEmpty()
                    if (text.isNotBlank()) append(text)
                }
            }
            else -> ""
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 180_000
    }
}

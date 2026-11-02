package com.cevier.shinian.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.cevier.shinian.model.ChatMessage
import com.cevier.shinian.model.LlmConfig
import com.cevier.shinian.model.LlmModel
import com.cevier.shinian.model.LlmProvider
import com.cevier.shinian.model.RecentConversation
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class LlmPrivateStore(private val context: Context) {
    private val configFile = context.filesDir.resolve("llm-config.bin")
    private val conversationFile = context.filesDir.resolve("llm-recent-conversation.bin")

    @Synchronized
    fun loadConfig(): LlmConfig {
        if (!configFile.exists()) return LlmConfig()
        return runCatching {
            val root = JSONObject(decrypt(configFile.readBytes()).decodeToString())
            val provider = runCatching { LlmProvider.valueOf(root.getString("provider")) }
                .getOrDefault(LlmProvider.DEEPSEEK)
            val models = root.optJSONArray("models")?.let { items ->
                buildList {
                    for (index in 0 until items.length()) {
                        val item = items.optJSONObject(index) ?: continue
                        val id = item.optString("id")
                        if (id.isNotBlank()) {
                            add(LlmModel(id, item.optString("name").ifBlank { id }, item.optBoolean("supportsImages")))
                        }
                    }
                }
            }.orEmpty()
            LlmConfig(
                provider = provider,
                apiKey = root.optString("apiKey"),
                baseUrl = root.optString("baseUrl").ifBlank { provider.defaultBaseUrl },
                modelId = root.optString("modelId"),
                modelName = root.optString("modelName"),
                supportsImages = root.optBoolean("supportsImages", false),
                models = models,
                thinkingEnabled = root.optBoolean("thinkingEnabled", true),
            )
        }.getOrDefault(LlmConfig())
    }

    @Synchronized
    fun saveConfig(config: LlmConfig) {
        val models = JSONArray().apply {
            config.configuredModels.forEach { model ->
                put(JSONObject()
                    .put("id", model.id)
                    .put("name", model.name)
                    .put("supportsImages", model.supportsImages))
            }
        }
        val json = JSONObject().apply {
            put("provider", config.provider.name)
            put("apiKey", config.apiKey)
            put("baseUrl", config.baseUrl)
            put("modelId", config.modelId)
            put("modelName", config.modelName)
            put("supportsImages", config.supportsImages)
            put("models", models)
            put("thinkingEnabled", config.thinkingEnabled)
        }
        configFile.writeBytes(encrypt(json.toString().encodeToByteArray()))
    }

    @Synchronized
    fun loadConversation(): RecentConversation {
        if (!conversationFile.exists()) return RecentConversation()
        return runCatching {
            val root = JSONObject(decrypt(conversationFile.readBytes()).decodeToString())
            val items = root.optJSONArray("messages") ?: JSONArray()
            val messages = buildList {
                for (index in 0 until items.length()) {
                    val item = items.getJSONObject(index)
                    add(
                        ChatMessage(
                            id = item.getLong("id"),
                            role = item.getString("role"),
                            text = item.optString("text"),
                            imageDataUrl = item.optString("imageDataUrl").takeIf(String::isNotBlank),
                        ),
                    )
                }
            }
            RecentConversation(messages, root.optLong("updatedAt"))
        }.getOrDefault(RecentConversation())
    }

    @Synchronized
    fun saveConversation(conversation: RecentConversation) {
        val messages = JSONArray()
        conversation.messages.forEach { message ->
            messages.put(JSONObject().apply {
                put("id", message.id)
                put("role", message.role)
                put("text", message.text)
                put("imageDataUrl", message.imageDataUrl ?: "")
            })
        }
        val json = JSONObject()
            .put("messages", messages)
            .put("updatedAt", conversation.updatedAt)
        conversationFile.writeBytes(encrypt(json.toString().encodeToByteArray()))
    }

    @Synchronized
    fun clearConversation() {
        conversationFile.delete()
    }

    fun prepareImage(uri: Uri): String {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val width = info.size.width
            val height = info.size.height
            val scale = minOf(1f, IMAGE_MAX_DIMENSION.toFloat() / maxOf(width, height))
            decoder.setTargetSize(
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
            )
        }
        val flattened = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(decoded, 0f, 0f, null)
        }
        return try {
            val bytes = ByteArrayOutputStream().use { output ->
                check(flattened.compress(Bitmap.CompressFormat.JPEG, IMAGE_JPEG_QUALITY, output))
                output.toByteArray()
            }
            "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        } finally {
            decoded.recycle()
            flattened.recycle()
        }
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }
        return cipher.iv + cipher.doFinal(plain)
    }

    private fun decrypt(payload: ByteArray): ByteArray {
        require(payload.size > IV_SIZE) { "本地 LLM 数据无效" }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(128, payload.copyOfRange(0, IV_SIZE)),
            )
        }
        return cipher.doFinal(payload.copyOfRange(IV_SIZE, payload.size))
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "glitter_llm_private_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val IMAGE_MAX_DIMENSION = 1600
        const val IMAGE_JPEG_QUALITY = 84
    }
}

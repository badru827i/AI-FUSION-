package com.aifusion.app.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getApiKey(): String {
        val stored = prefs.getString(KEY_API, "").orEmpty()
        if (stored.isBlank()) return ""
        return runCatching {
            val parts = stored.split(':', limit = 2)
            require(parts.size == 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
        }.getOrDefault("")
    }

    fun setApiKey(value: String) {
        if (value.isBlank()) {
            prefs.edit().remove(KEY_API).apply()
            return
        }

        runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val body = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            prefs.edit().putString(KEY_API, "$iv:$body").apply()
        }
    }

    fun getModel(): String =
        prefs.getString(KEY_MODEL, DEFAULT_MODEL)?.trim().orEmpty()
            .ifBlank { DEFAULT_MODEL }

    fun setModel(value: String) {
        val model = value.trim().ifBlank { DEFAULT_MODEL }
        prefs.edit().putString(KEY_MODEL, model).apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-6-luna"
        private const val PREFS = "ai_fusion_secure_settings"
        private const val KEY_API = "openai_api_key"
        private const val KEY_MODEL = "openai_model"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "ai_fusion_openai_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

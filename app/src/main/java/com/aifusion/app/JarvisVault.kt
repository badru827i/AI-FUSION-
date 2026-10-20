package com.aifusion.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Local-only vault for the owner face embedding and ESP32 shared secret.
 * Raw camera frames are never written to disk.
 */
internal class JarvisVault(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.noBackupFilesDir, "jarvis-private").apply { mkdirs() }

    fun hasOwnerTemplate(): Boolean = File(root, "owner-template.enc").isFile

    @Synchronized
    fun saveOwnerTemplate(embedding: FloatArray) {
        require(embedding.size in 32..4096) { "Embedding dimension is not supported." }
        val bytes = ByteBuffer.allocate(4 + embedding.size * 4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(embedding.size)
            .apply { embedding.forEach { putFloat(it) } }
            .array()
        writeEncrypted("owner-template.enc", bytes)
    }

    @Synchronized
    fun loadOwnerTemplate(): FloatArray? {
        val raw = readEncrypted("owner-template.enc") ?: return null
        if (raw.size < 4) return null
        val buffer = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN)
        val size = buffer.int
        if (size !in 32..4096 || raw.size != 4 + size * 4) return null
        return FloatArray(size) { buffer.float }
    }

    @Synchronized
    fun deleteOwnerTemplate() {
        File(root, "owner-template.enc").delete()
    }

    fun saveRobotEndpoint(endpoint: String) {
        val cleaned = endpoint.trim().trimEnd('/')
        require(cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            "Use a local HTTP/HTTPS URL."
        }
        // The endpoint is a host address, not a credential. Private directory + no backup.
        File(root, "robot-endpoint.txt").writeText(cleaned, StandardCharsets.UTF_8)
    }

    fun loadRobotEndpoint(): String =
        runCatching { File(root, "robot-endpoint.txt").readText(StandardCharsets.UTF_8).trim() }
            .getOrDefault("")

    @Synchronized
    fun saveRobotSecret(secretHex: String) {
        require(secretHex.matches(Regex("[0-9a-fA-F]{64}"))) {
            "The robot key must contain exactly 64 hexadecimal characters (32 bytes)."
        }
        writeEncrypted("robot-key.enc", secretHex.lowercase().toByteArray(StandardCharsets.UTF_8))
    }

    @Synchronized
    fun loadRobotSecret(): String? =
        readEncrypted("robot-key.enc")?.toString(StandardCharsets.UTF_8)

    fun hasRobotSecret(): Boolean = File(root, "robot-key.enc").isFile

    @Synchronized
    fun clearRobotSecret() {
        File(root, "robot-key.enc").delete()
    }

    private fun writeEncrypted(fileName: String, plaintext: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext)
        val payload = ByteBuffer.allocate(1 + iv.size + encrypted.size)
            .put(iv.size.toByte())
            .put(iv)
            .put(encrypted)
            .array()

        val target = File(root, fileName)
        val temp = File(root, "$fileName.tmp")
        temp.outputStream().use { it.write(payload); it.fd.sync() }
        if (target.exists() && !target.delete()) {
            temp.delete()
            error("Cannot replace protected local data.")
        }
        if (!temp.renameTo(target)) {
            temp.delete()
            error("Cannot atomically save protected local data.")
        }
    }

    private fun readEncrypted(fileName: String): ByteArray? {
        val file = File(root, fileName)
        if (!file.isFile) return null
        return try {
            val payload = file.readBytes()
            if (payload.size < 1 + 12 + 16) return null
            val ivLength = payload[0].toInt() and 0xff
            if (ivLength !in 12..16 || payload.size <= 1 + ivLength) return null
            val iv = payload.copyOfRange(1, 1 + ivLength)
            val encrypted = payload.copyOfRange(1 + ivLength, payload.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted)
        } catch (_: Exception) {
            // Fail closed. Never return unauthenticated or partially decrypted data.
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "aifusion_jarvis_local_v1"
    }
}

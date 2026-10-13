package com.aifusion.app

import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal local-network client for the separately installed AI-FUSION Mini Server.
 * Network work is called from Dispatchers.IO by the UI.
 */
object MiniServerClient {
    fun test(baseUrl: String, token: String): String {
        if (baseUrl.isBlank()) return "Masukkan alamat API OPPO dahulu."
        if (!baseUrl.startsWith("http://", ignoreCase = true)) {
            return "Alamat mesti bermula dengan http:// kerana Mini Server tempatan menggunakan HTTP."
        }
        val normalized = baseUrl.trimEnd('/')
        return try {
            val health = request(normalized + "/health", null)
            if (health.first != 200 || !health.second.contains("\"status\":\"ok\"")) {
                return "OPPO tidak memberi respons health yang sah (HTTP " + health.first + ")."
            }
            if (token.isBlank()) {
                return "OPPO boleh dicapai (/health OK), tetapi masukkan token API untuk menguji /api/v1/info."
            }
            val info = request(normalized + "/api/v1/info", token)
            when {
                info.first == 200 && info.second.contains("AI-FUSION Mini Server") ->
                    "✓ Sambungan berjaya • AI-FUSION Mini Server dijumpai • API v1"
                info.first == 401 ->
                    "Alamat OPPO boleh dicapai, tetapi token salah. Salin semula token dari Mini Server."
                info.first == 404 ->
                    "OPPO boleh dicapai tetapi endpoint info tidak ditemui. Kemas kini Mini Server."
                else -> "OPPO boleh dicapai, tetapi API info gagal (HTTP " + info.first + ")."
            }
        } catch (e: Exception) {
            val message = e.message?.take(160).orEmpty()
            "Sambungan gagal: " + (message.ifBlank { e.javaClass.simpleName }) +
                ". Semak IP, port 8765, hotspot dan status START SERVER."
        }
    }

    private fun request(address: String, token: String?): Pair<Int, String> {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4500
            readTimeout = 4500
            useCaches = false
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("X-AI-FUSION-Token", token)
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            code to body
        } finally {
            connection.disconnect()
        }
    }
    /**
     * Sends the user's prompt to the authenticated OPPO helper endpoint.
     * The helper currently acknowledges receipt; inference stays on the A27.
     * Call from Dispatchers.IO.
     */
    fun handoffChat(baseUrl: String, token: String, message: String): String {
        if (baseUrl.isBlank() || token.isBlank()) return "OPPO helper belum dikonfigurasi."
        val normalized = baseUrl.trim().trimEnd('/')
        return try {
            val payload = org.json.JSONObject().put("message", message).toString() + "\n"
            val connection = (URL(normalized + "/api/v1/chat").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 2500
                readTimeout = 2500
                doOutput = true
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("X-AI-FUSION-Token", token)
                setFixedLengthStreamingMode(payload.toByteArray(Charsets.UTF_8).size)
            }
            try {
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code == 200 && body.contains("\"accepted\":true")) {
                    "OPPO helper menerima mesej • jawapan AI dijana pada A27"
                } else if (code == 404) {
                    "Mini Server perlu dikemas kini untuk endpoint chat."
                } else {
                    "OPPO helper tidak tersedia (HTTP $code); jawapan dijana secara lokal."
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            "OPPO helper tidak dapat dicapai; jawapan dijana secara lokal."
        }
    }

}

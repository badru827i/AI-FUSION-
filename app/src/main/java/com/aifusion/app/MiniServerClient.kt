package com.aifusion.app

import java.net.HttpURLConnection
import java.net.URL
import java.net.URI
import java.net.URLEncoder
import java.net.Socket
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.SecureRandom
import android.util.Base64

/**
 * Minimal local-network client for the separately installed AI-FUSION Mini Server.
 * Network work is called from Dispatchers.IO by the UI.
 */
object MiniServerClient {
    fun test(baseUrl: String, token: String): String {
        if (baseUrl.isBlank()) return "Masukkan alamat API Mini Server dahulu."
        if (!baseUrl.startsWith("http://", ignoreCase = true)) {
            return "Alamat mesti bermula dengan http:// kerana Mini Server tempatan menggunakan HTTP."
        }
        val normalized = baseUrl.trimEnd('/')
        return try {
            val health = request(normalized + "/health", null)
            if (health.first != 200 || !health.second.contains("\"status\":\"ok\"")) {
                return "Mini Server tidak memberi respons health yang sah (HTTP " + health.first + ")."
            }
            if (token.isBlank()) {
                return "OPPO boleh dicapai (/health OK), tetapi masukkan token API untuk menguji /api/v1/info."
            }
            val info = request(normalized + "/api/v1/info", token)
            when {
                info.first == 200 && info.second.contains("AI-FUSION Mini Server") -> {
                    val ws = testWebSocket(normalized, token)
                    if (ws) "✓ API HTTP aktif • WebSocket realtime berjaya"
                    else "✓ API HTTP aktif • WebSocket belum dapat disambungkan"
                }
                info.first == 401 ->
                    "Alamat OPPO boleh dicapai, tetapi token salah. Salin semula token dari Mini Server."
                info.first == 404 ->
                    "Server boleh dicapai tetapi endpoint info tidak ditemui. Kemas kini Mini Server."
                else -> "Server boleh dicapai, tetapi API info gagal (HTTP " + info.first + ")."
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
     * Sends a lightweight authenticated prompt handoff to Mini Server.
     * The current server acknowledges receipt; it does not generate the answer yet.
     * Call from Dispatchers.IO.
     */
    fun handoffChat(baseUrl: String, token: String, message: String): String {
        if (baseUrl.isBlank() || token.isBlank()) return "Mini Server belum dikonfigurasikan."
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
                    "Mini Server menerima mesej • jawapan masih dijana pada telefon utama"
                } else if (code == 404) {
                    "Mini Server perlu dikemas kini untuk endpoint chat."
                } else {
                    "Mini Server tidak tersedia (HTTP $code); jawapan dijana secara lokal."
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            "Mini Server tidak dapat dicapai; jawapan dijana secara lokal."
        }
    }


    /** Performs a short authenticated WebSocket handshake check, then closes it. */
    private fun testWebSocket(baseUrl: String, token: String): Boolean {
        var socket: Socket? = null
        return try {
            val uri = URI(baseUrl)
            val host = uri.host ?: return false
            val port = if (uri.port >= 0) uri.port else 80
            socket = Socket()
            socket.connect(java.net.InetSocketAddress(host, port), 3500)
            socket.soTimeout = 3500
            val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val key = Base64.encodeToString(nonce, Base64.NO_WRAP)
            val tokenQuery = URLEncoder.encode(token, "UTF-8")
            val hostHeader = if (uri.port >= 0) "$host:$port" else host
            val request = "GET /ws?token=$tokenQuery HTTP/1.1\r\n" +
                "Host: $hostHeader\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: $key\r\n" +
                "Sec-WebSocket-Version: 13\r\n\r\n"
            socket.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.US_ASCII))
            reader.readLine()?.contains(" 101 ") == true
        } catch (_: Exception) {
            false
        } finally {
            try { socket?.close() } catch (_: Exception) { }
        }
    }

}

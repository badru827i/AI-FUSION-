package com.aifusion.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal data class RobotGatewayResult(
    val ok: Boolean,
    val message: String,
    val statusCode: Int? = null
)

/**
 * HMAC-authenticated local ESP32 protocol. No face image or face embedding leaves the phone.
 * HTTP is supported for a private LAN only; never expose the ESP32 port to the public Internet.
 */
internal object JarvisRobotGateway {
    private const val CONNECT_TIMEOUT_MS = 2500
    private const val READ_TIMEOUT_MS = 3000

    fun testStatus(baseUrl: String): RobotGatewayResult = runCatching {
        val response = request(baseUrl, "/api/v1/status", "GET", null)
        val json = JSONObject(response.second)
        if (response.first in 200..299) {
            RobotGatewayResult(
                true,
                "ESP32 connected · " + json.optString("device", "AI-FUSION Robot") +
                    " · movement " + if (json.optBoolean("motionEnabled", false)) "enabled" else "locked",
                response.first
            )
        } else {
            RobotGatewayResult(false, json.optString("error", "HTTP " + response.first), response.first)
        }
    }.getOrElse { RobotGatewayResult(false, safeError(it)) }

    fun authorize(baseUrl: String, secretHex: String): RobotGatewayResult = runCatching {
        val secret = decodeSecret(secretHex)
        val challengeResponse = request(baseUrl, "/api/v1/challenge", "GET", null)
        if (challengeResponse.first !in 200..299) {
            return RobotGatewayResult(false, "ESP32 challenge failed (HTTP " + challengeResponse.first + ").", challengeResponse.first)
        }

        val nonce = JSONObject(challengeResponse.second).optString("nonce")
        require(nonce.matches(Regex("[0-9a-fA-F]{32}"))) { "ESP32 returned an invalid challenge." }

        val proof = hmacHex(secret, "AUTH|$nonce")
        val body = JSONObject().put("nonce", nonce).put("proof", proof).toString()
        val response = request(baseUrl, "/api/v1/authorize", "POST", body)
        val json = runCatching { JSONObject(response.second) }.getOrDefault(JSONObject())

        if (response.first in 200..299 && json.optBoolean("authorized", false)) {
            RobotGatewayResult(
                true,
                "Local session authorized. ESP32 movement remains locked until its hardware configuration is reviewed.",
                response.first
            )
        } else {
            RobotGatewayResult(false, json.optString("error", "Robot authorization denied."), response.first)
        }
    }.getOrElse { RobotGatewayResult(false, safeError(it)) }

    fun command(baseUrl: String, secretHex: String?, command: String): RobotGatewayResult = runCatching {
        val supported = setOf("STOP", "STATUS", "FORWARD", "BACKWARD", "LEFT", "RIGHT", "SERVO_CENTER")
        require(command in supported) { "Unsupported robot command." }

        // STOP is fail-safe and intentionally works even if the app has no saved key.
        val counter = System.currentTimeMillis()
        val bodyJson = JSONObject().put("command", command).put("counter", counter)
        if (command != "STOP") {
            val secret = decodeSecret(secretHex ?: error("Save the ESP32 key first."))
            bodyJson.put("proof", hmacHex(secret, "CMD|$counter|$command"))
        } else if (!secretHex.isNullOrBlank()) {
            val secret = runCatching { decodeSecret(secretHex) }.getOrNull()
            if (secret != null) bodyJson.put("proof", hmacHex(secret, "CMD|$counter|STOP"))
        }

        val response = request(baseUrl, "/api/v1/command", "POST", bodyJson.toString())
        val json = runCatching { JSONObject(response.second) }.getOrDefault(JSONObject())
        val message = json.optString(
            "message",
            json.optString("error", if (response.first in 200..299) "Command acknowledged." else "HTTP " + response.first)
        )
        RobotGatewayResult(response.first in 200..299 && json.optBoolean("ok", false), message, response.first)
    }.getOrElse { RobotGatewayResult(false, safeError(it)) }

    private fun request(baseUrl: String, path: String, method: String, body: String?): Pair<Int, String> {
        val cleanBase = baseUrl.trim().trimEnd('/')
        require(cleanBase.startsWith("http://") || cleanBase.startsWith("https://")) {
            "Enter the local ESP32 address, for example http://192.168.4.1"
        }
        requireAllowedLocalHost(cleanBase)
        val connection = (URL(cleanBase + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            return code to text
        } finally {
            connection.disconnect()
        }
    }

    private fun requireAllowedLocalHost(base: String) {
        val uri = URI(base)
        val host = uri.host ?: error("Invalid robot address.")
        require(uri.userInfo == null) { "Do not put credentials inside the robot URL." }
        val localSuffix = host.equals("localhost", true) || host.endsWith(".local", true)
        val resolved = runCatching { InetAddress.getByName(host) }.getOrNull()
        val privateAddress = resolved?.let {
            it.isAnyLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress || it.isSiteLocalAddress
        } == true
        require(localSuffix || privateAddress) {
            "For privacy, AI-FUSION only sends robot credentials to a local/private network address."
        }
        require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
            "Only HTTP or HTTPS is supported."
        }
    }

    private fun decodeSecret(hex: String): ByteArray {
        require(hex.matches(Regex("[0-9a-fA-F]{64}"))) {
            "The ESP32 key must be 64 hexadecimal characters (32 bytes)."
        }
        return ByteArray(32) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun hmacHex(secret: ByteArray, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        return mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun safeError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("cleartext", true) -> "Sambungan ditolak oleh Android. Semak konfigurasi rangkaian tempatan."
            message.contains("refused", true) || message.contains("timeout", true) ->
                "ESP32 tidak dapat dicapai. Semak hotspot/Wi-Fi dan alamat tempatan."
            message.isBlank() -> "Operasi robot gagal. Semak alamat dan konfigurasi."
            else -> message.take(180)
        }
    }
}

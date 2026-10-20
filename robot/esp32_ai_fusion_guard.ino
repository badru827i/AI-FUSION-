/*
 * AI-FUSION JARVIS ESP32 local security gateway.
 *
 * Libraries:
 *   - ArduinoJson 6.x
 *   - ESP32 Arduino core (WebServer, Preferences, mbedTLS)
 *
 * SAFETY DEFAULT:
 *   Motion outputs are locked in this sample. Do not enable actuation until the exact
 *   ESP32 board, motor driver, wiring, current limits, watchdog and physical E-stop
 *   have been reviewed. The E-stop must physically cut motor-driver enable; software
 *   must never be the only emergency-stop mechanism.
 */

#include <WiFi.h>
#include <WebServer.h>
#include <Preferences.h>
#include <ArduinoJson.h>
#include <mbedtls/md.h>
#include <esp_system.h>

static const char *WIFI_SSID = "CHANGE_TO_PHONE_HOTSPOT";
static const char *WIFI_PASSWORD = "CHANGE_THIS_PASSWORD";

// Paste the 64-hex key generated inside AI-FUSION, then erase it from any shared copy.
// Never commit a real key or Wi-Fi password to a public repository.
static const char *ROBOT_SHARED_KEY_HEX = "REPLACE_WITH_64_HEX_CHARACTERS_FROM_AI_FUSION";

// Deliberately false: this security gateway must not guess motor-driver pin assignments.
static constexpr bool MOTOR_OUTPUTS_ENABLED = false;
static constexpr uint32_t CHALLENGE_WINDOW_MS = 10000;
static constexpr uint32_t AUTH_SESSION_MS = 45000;

WebServer server(80);
Preferences prefs;
String activeNonce;
uint32_t challengeExpiresAt = 0;
uint32_t authorizedUntil = 0;
uint64_t lastCounter = 0;

static bool hexNibble(char c, uint8_t &value) {
  if (c >= '0' && c <= '9') { value = c - '0'; return true; }
  if (c >= 'a' && c <= 'f') { value = c - 'a' + 10; return true; }
  if (c >= 'A' && c <= 'F') { value = c - 'A' + 10; return true; }
  return false;
}

static bool readSharedKey(uint8_t out[32]) {
  if (strlen(ROBOT_SHARED_KEY_HEX) != 64) return false;
  for (size_t i = 0; i < 32; ++i) {
    uint8_t hi, lo;
    if (!hexNibble(ROBOT_SHARED_KEY_HEX[i * 2], hi) ||
        !hexNibble(ROBOT_SHARED_KEY_HEX[i * 2 + 1], lo)) return false;
    out[i] = (hi << 4) | lo;
  }
  return true;
}

static String bytesToHex(const uint8_t *bytes, size_t length) {
  static const char hex[] = "0123456789abcdef";
  String result;
  result.reserve(length * 2);
  for (size_t i = 0; i < length; ++i) {
    result += hex[(bytes[i] >> 4) & 0x0f];
    result += hex[bytes[i] & 0x0f];
  }
  return result;
}

static bool hmacFor(const String &message, String &outHex) {
  uint8_t key[32];
  uint8_t digest[32];
  if (!readSharedKey(key)) return false;
  const mbedtls_md_info_t *info = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
  if (!info) return false;
  const int result = mbedtls_md_hmac(
      info, key, sizeof(key),
      reinterpret_cast<const unsigned char *>(message.c_str()),
      message.length(), digest);
  memset(key, 0, sizeof(key));
  if (result != 0) {
    memset(digest, 0, sizeof(digest));
    return false;
  }
  outHex = bytesToHex(digest, sizeof(digest));
  memset(digest, 0, sizeof(digest));
  return true;
}

static bool constantTimeEqual(const String &a, const String &b) {
  if (a.length() != b.length()) return false;
  uint8_t difference = 0;
  for (size_t i = 0; i < a.length(); ++i) difference |= uint8_t(a[i]) ^ uint8_t(b[i]);
  return difference == 0;
}

static void stopOutputs() {
  // Safe default: this sample has no motor pin map and always reports outputs locked.
  // Once hardware is confirmed, this function must drive the physical motor-enable LOW.
  authorizedUntil = 0;
}

static bool sessionIsActive() {
  return int32_t(authorizedUntil - millis()) > 0;
}

static void sendJson(int status, JsonDocument &document) {
  String output;
  serializeJson(document, output);
  server.send(status, "application/json", output);
}

static void sendError(int status, const char *code) {
  StaticJsonDocument<192> doc;
  doc["ok"] = false;
  doc["error"] = code;
  doc["motionEnabled"] = false;
  sendJson(status, doc);
}

static void handleStatus() {
  StaticJsonDocument<256> doc;
  doc["ok"] = true;
  doc["device"] = "AI-FUSION ESP32";
  doc["authorized"] = sessionIsActive();
  doc["motionEnabled"] = MOTOR_OUTPUTS_ENABLED;
  doc["hardwareActuation"] = "locked-unconfigured";
  sendJson(200, doc);
}

static void handleChallenge() {
  uint8_t randomBytes[16];
  for (size_t i = 0; i < sizeof(randomBytes); i += 4) {
    uint32_t randomWord = esp_random();
    memcpy(randomBytes + i, &randomWord, 4);
  }
  activeNonce = bytesToHex(randomBytes, sizeof(randomBytes));
  challengeExpiresAt = millis() + CHALLENGE_WINDOW_MS;

  StaticJsonDocument<192> doc;
  doc["nonce"] = activeNonce;
  doc["expiresInMs"] = CHALLENGE_WINDOW_MS;
  doc["device"] = "AI-FUSION ESP32";
  sendJson(200, doc);
  memset(randomBytes, 0, sizeof(randomBytes));
}

static void handleAuthorize() {
  StaticJsonDocument<384> doc;
  if (deserializeJson(doc, server.arg("plain"))) return sendError(400, "invalid_json");

  const String nonce = doc["nonce"] | "";
  const String suppliedProof = doc["proof"] | "";
  if (activeNonce.length() != 32 || nonce != activeNonce ||
      int32_t(challengeExpiresAt - millis()) <= 0) {
    activeNonce = "";
    return sendError(401, "challenge_invalid_or_expired");
  }

  String expectedProof;
  if (!hmacFor("AUTH|" + nonce, expectedProof) ||
      !constantTimeEqual(expectedProof, suppliedProof)) {
    activeNonce = "";
    return sendError(401, "proof_invalid");
  }

  // One-time challenge prevents replaying a captured authorization proof.
  activeNonce = "";
  authorizedUntil = millis() + AUTH_SESSION_MS;

  StaticJsonDocument<192> response;
  response["authorized"] = true;
  response["expiresInMs"] = AUTH_SESSION_MS;
  response["motionEnabled"] = MOTOR_OUTPUTS_ENABLED;
  response["message"] = "Local session authorized; motor outputs remain locked.";
  sendJson(200, response);
}

static void handleCommand() {
  StaticJsonDocument<512> doc;
  if (deserializeJson(doc, server.arg("plain"))) return sendError(400, "invalid_json");

  const String command = doc["command"] | "";

  // STOP is allowed without a session/key; the future implementation must de-energize outputs.
  if (command == "STOP") {
    stopOutputs();
    StaticJsonDocument<192> response;
    response["ok"] = true;
    response["message"] = "STOP accepted; outputs locked.";
    response["motionEnabled"] = false;
    sendJson(200, response);
    return;
  }

  if (!sessionIsActive()) return sendError(401, "session_expired");
  if (command != "STATUS" && command != "FORWARD" && command != "BACKWARD" &&
      command != "LEFT" && command != "RIGHT" && command != "SERVO_CENTER") {
    return sendError(400, "command_not_supported");
  }

  const uint64_t counter = doc["counter"] | uint64_t(0);
  const String suppliedProof = doc["proof"] | "";
  if (counter == 0 || counter <= lastCounter) return sendError(409, "counter_replayed");

  String expectedProof;
  char counterText[24];
  snprintf(counterText, sizeof(counterText), "%llu", (unsigned long long)counter);
  const String signingMessage = "CMD|" + String(counterText) + "|" + command;
  if (!hmacFor(signingMessage, expectedProof) ||
      !constantTimeEqual(expectedProof, suppliedProof)) {
    return sendError(401, "proof_invalid");
  }

  // Persist before processing any action so a replay after reboot is rejected.
  lastCounter = counter;
  prefs.putULong64("lastCtr", lastCounter);

  if (command == "STATUS") return handleStatus();

  // Do not add guessed GPIO mappings. This sample intentionally refuses all movement.
  if (!MOTOR_OUTPUTS_ENABLED) return sendError(423, "motion_locked_by_firmware");

  // SAFETY TODO (only after a hardware review):
  // Implement bounded-duration motion here with a watchdog and physical E-stop input.
  // Never execute unlimited movement or accept free-form GPIO values from the phone.
  return sendError(423, "actuator_driver_not_configured");
}

void setup() {
  Serial.begin(115200);
  prefs.begin("aifusion-guard", false);
  lastCounter = prefs.getULong64("lastCtr", 0);
  stopOutputs();

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  const uint32_t start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 15000) {
    delay(250);
  }

  if (WiFi.status() != WL_CONNECTED) {
    // Local setup fallback; connect the phone to this Wi-Fi only for provisioning/testing.
    WiFi.mode(WIFI_AP);
    WiFi.softAP("AI-FUSION-Robot-Setup", "FUSION-Setup-1");
    Serial.println("Setup AP address: 192.168.4.1");
  } else {
    Serial.print("Local gateway address: ");
    Serial.println(WiFi.localIP());
  }

  server.on("/api/v1/status", HTTP_GET, handleStatus);
  server.on("/api/v1/challenge", HTTP_GET, handleChallenge);
  server.on("/api/v1/authorize", HTTP_POST, handleAuthorize);
  server.on("/api/v1/command", HTTP_POST, handleCommand);
  server.onNotFound([] { sendError(404, "not_found"); });
  server.begin();
}

void loop() {
  server.handleClient();
  if (!sessionIsActive()) stopOutputs();
  delay(2);
}

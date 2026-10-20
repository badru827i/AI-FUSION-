# JARVIS Privacy Guard — AI-FUSION

This change adds a local face-identity pipeline, an encrypted on-device owner vault, a strong-Android-biometric gate, and an ESP32 gateway protocol. It is a safety-oriented foundation, not a claim of production-grade certified biometric security.

## What is implemented

- CameraX front-camera preview plus bundled ML Kit face detection.
- Import and validation of a separately licensed TFLite **face-embedding** model.
- Local enrollment from multiple frames and local cosine-similarity matching.
- Active head-movement variation check as a basic replay obstacle.
- Face embedding stored with AES-GCM; the AES key is generated in Android Keystore.
- Camera bitmaps/crops are processed in memory and are not written to disk.
- Robot shared key stored encrypted in the app's private no-backup directory; app backup is disabled.
- Android BIOMETRIC_STRONG prompt required after a face match before a robot authorization session is requested.
- Local-network HTTP requests are restricted to private/local hosts by the app; commands are authenticated using HMAC-SHA256 and monotonically increasing counters.
- ESP32 sketch supports one-time challenges, expiring authorization sessions and replay checks.
- ESP32 firmware rejects motion commands by default (MOTOR_OUTPUTS_ENABLED = false); STOP is always accepted.

## Install a compatible face-embedding model

No third-party biometric weights are bundled by default. Model weights have their own licensing and preprocessing requirements, so use only a model you are legally allowed to use and whose input/output contract is known.

In **JARVIS · Privacy Guard**, tap **Import model** and choose a compatible .tflite model. The current adapter requires:

- Input tensor: [1,112,112,3], float32, RGB pixels normalized to [-1,1].
- Output tensor: [1,D], float32 face embedding, where D is between 32 and 4096.
- The model must emit an identity embedding, not a two-image “same/different” score.
- The model must be trained for face verification and its crop/alignment requirements must match the app's preprocessing.

A model file that loads successfully is not necessarily accurate for authentication. Calibrate the similarity threshold with representative consented samples, false-accept/false-reject tests, different lighting, glasses, pose and the exact phone model. The initial threshold in code is only a starting point.

Face detection from ML Kit is not face recognition. Actual identity matching will stay unavailable until a compatible embedding model is imported.

## Owner enrollment and verification

1. Open the AI-FUSION drawer and choose **JARVIS Security**.
2. Import a compatible model.
3. Tap **Daftar muka**, then keep one face visible and move the head slowly left and right.
4. Tap **Kenali pemilik** for a new local match. The screen requires multiple matching frames plus a range of head poses.
5. For a robot authorization session, the matched face is not enough: Android must approve a strong biometric prompt and the ESP32 must verify its challenge proof.

The head-pose variation is only a basic active check. It is **not** a dedicated anti-spoofing model and must not be treated as proof against all photos, screen replays, masks or generated video. High-risk actions need the second factor and independent physical safety controls.

## Provision the ESP32 example

File: robot/esp32_ai_fusion_guard.ino

1. Install ArduinoJson 6.x and use an ESP32 Arduino core with WebServer, Preferences and mbedTLS.
2. Enter your own phone hotspot SSID/password in the sketch. These are placeholders in source control.
3. In the app, tap **Jana kunci**, then **Salin kunci**. Paste the exact 64-hex-character value into ROBOT_SHARED_KEY_HEX locally in your firmware working copy. Never commit a real key or share it publicly.
4. Save the same key in the app and set the ESP32's private IP address. The app will reject public Internet hosts for credentials.
5. Flash the sketch. Test /api/v1/status, then test app **Uji sambungan** and the authorization flow.

The challenge protocol is:

- GET /api/v1/challenge → one-time random nonce.
- POST /api/v1/authorize → HMAC-SHA256 of AUTH|nonce; successful session expires after 45 seconds.
- POST /api/v1/command → HMAC-SHA256 of CMD|counter|command; old counters are rejected and saved in ESP32 NVS.
- STOP is accepted regardless of an active authorization session.

The ESP32 example intentionally does **not** energize motors or move the servo. The user's actual board revision, motor driver, wiring, power supply/current limits and servo wiring have not been identified; pin assignments must not be guessed. Keep motor outputs disabled until the driver-specific code is reviewed, add a hardware watchdog, and wire a physical emergency stop so it can cut motor-driver enable independently of the app and ESP32.

## Privacy and transport limitations

- The phone does not send face images or face embeddings to the OPPO Mini Server or ESP32.
- The Mini Server is not part of face matching; its role can remain orchestration/memory only.
- Android Keystore protects the AES key; encrypted files remain private to the app and are excluded from backup. A rooted/compromised device can still undermine local security.
- HMAC authenticates a command but does not encrypt HTTP traffic. Use only a trusted private Wi-Fi/hotspot, never forward the port to the Internet, and prefer authenticated TLS for production.
- The app uses Android's BIOMETRIC_STRONG system prompt. Android decides which strong modality to present; an app cannot reliably force a fingerprint sensor on every manufacturer/device.
- Re-enrollment overwrites the previous template. Do not use this prototype as the sole access-control mechanism for dangerous machinery.

## Verification still required

Run the repository's Android GitHub Actions build and test this on the target phone. Face-model thresholds, false-accept rates, presentation-attack resistance, ESP32 compatibility and physical motor interlocks all require real-device validation before this can be considered production-ready.

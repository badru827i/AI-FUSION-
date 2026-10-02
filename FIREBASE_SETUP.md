# Firebase account setup for AI-FUSION

AI-FUSION now supports a real Google-to-Firebase account flow:

Google Credential Manager -> Google ID token -> Firebase Authentication -> Firebase UID -> Cloud Firestore.

## One-time setup

1. Create/select a Firebase project.
2. Add an Android app with package name: `com.aifusion.app`.
3. Add the SHA-1 fingerprint for the signing certificate used by the APK.
4. Enable Google in Firebase Authentication.
5. Create/enable Cloud Firestore.
6. Download `google-services.json` and place it at:
   `app/google-services.json`
7. Do **not** commit `google-services.json` if your repository policy requires keeping Firebase configuration private; add it through your local/CI build environment instead.
8. In AI-FUSION Settings, enter the **Web/server OAuth client ID** in the existing Google OAuth Client ID field. Firebase's Android Google sign-in documentation specifies that `setServerClientId()` uses the server/Web client ID, not the Android client ID.

## Firestore structure

- `users/{uid}` — profile metadata
- `users/{uid}/chats/{chatId}` — cloud chat history

The included `firestore.rules` restricts each user to their own UID path.

Until `google-services.json` is connected and Firebase is configured, the app keeps its existing local-first chat behavior.

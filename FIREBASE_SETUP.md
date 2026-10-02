# AI-FUSION Google Account Setup (zero-server)

AI-FUSION uses Google Credential Manager for optional Google account sign-in. The Google credential is used only to identify the local account; chat history stays on the phone.

## One-time Google setup

1. Create an Android OAuth client for package name `com.aifusion.app`.
2. Add the SHA-1/SHA-256 fingerprints for the signing certificates used by the APK.
3. Create a Web/server OAuth client ID in the same Google Cloud project.
4. In AI-FUSION Settings, paste that Web OAuth Client ID into **Google OAuth Client ID** and tap **Save**.
5. Tap **Continue with Google** and choose your Google account.

The app uses the Web/server client ID with Credential Manager's Google ID option. No Firebase project, Firestore database, Railway service, or AI server is required for the login flow.

## Local data

The app stores only the account display name, email, and Google unique ID needed for the local account state. Google ID tokens are not stored by AI-FUSION.

Chat history remains in the existing local chat store.
# AI-FUSION

Next-generation full AI Assistant for Android.

## Chat Core 3.0

- ChatGPT-style composer with keyboard resize
- New Chat + local compressed history
- Streaming-style lightweight response output with status indicator
- BM / English / mixed-language routing
- Voice input and response speech
- Home / Back / New Chat navigation

## Research Core

- Multi-Agent Research Team: discovery, verification search and current-change search
- Parallel web research using the phone's own Internet connection
- Source links with numbered citations
- Live Research Monitor polling
- Research results are presented as source previews; source agreement is not treated as proof

## Smart Device Engine

- RAM + CPU detection
- Conservative GPU capability detection
- NPU capability detection where the Android feature list exposes it
- Low RAM / Balanced / Performance modes
- Model tier routing to avoid unnecessarily large local models

## Compression & Resource Manager

- GZIP compression for local chat history
- App cache size and available RAM status
- Manual temporary-cache cleanup
- No permanent chat storage on the Railway backend

## Local Model Manager

- Import metadata for .onnx and .tflite models
- Persistent URI access where supported
- Recommended model tier follows the device performance mode
- Runtime inference remains a separate adapter so large models are not loaded accidentally

## AI Skills

The skills catalog includes Vision/OCR, Files, Code, Image Create, Video Create, 3D Model Design, Model Manager, Voice and local/offline workflows.

The existing Google sign-in shell and original UI foundation are retained. Backend/model execution adapters can be connected later without restructuring the chat UI.

## Build

Android 7.0+ (minSdk 24).

GitHub Actions builds the debug APK on pushes and pull requests to main.

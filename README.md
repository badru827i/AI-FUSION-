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
- Automatic temporary-cache cleanup above a small threshold plus manual cleanup
- Chat history is kept in the app's local store; it is not sent to a backend by the local chat layer

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

## Local Text-to-3D Studio

- Chat Core routes 3D requests into the in-app Text-to-3D Studio.
- Local parameter controls: prompt, quality, detail, style, texture, symmetry and OBJ export.
- Geometry validation and an available-RAM guard run before local mesh generation.
- Generated OBJ projects are stored under app-private persistent storage rather than the temporary cache.
- The current built-in path is a deterministic procedural geometry fallback. It does **not** claim neural text-to-3D inference.
- `OnDeviceTextTo3DModel` is the integration contract for a real, licence-compatible on-device generative model. The adapter must be implemented and tested for Android runtime, memory, and offline operation before neural generation is enabled.
- Unsupported style/texture options fail clearly in fallback mode instead of pretending they were applied.
- No inference server or cloud endpoint is used by this module.

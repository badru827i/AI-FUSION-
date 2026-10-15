package com.aifusion.app.core

/**
 * Honest no-model fallback. This is deliberately not presented as generative AI:
 * genuine chat must come from a compatible local GGUF model or a configured API.
 */
object LocalAnswerEngine {
    fun answer(
        query: String,
        capabilities: DeviceCapabilities,
        network: NetworkState,
        modelCount: Int
    ): String {
        val language = detectLanguage(query)
        val message = when (language) {
            ChatLanguage.BM -> "AI chat belum mempunyai model yang boleh menjana jawapan sebenar. Import model GGUF yang serasi dalam Model Manager, atau masukkan API key dalam Settings untuk mengaktifkan AI online. Model ONNX/TFLite biasa hanya boleh diuji sebagai inferens; ia tidak automatik menjadi chatbot tanpa tokenizer dan runtime generatif yang serasi."
            ChatLanguage.ENGLISH -> "AI chat does not currently have a model capable of generating a real answer. Import a compatible GGUF model in Model Manager, or configure an API key in Settings to enable online AI. A generic ONNX/TFLite model is not automatically a chatbot without a compatible tokenizer and generative runtime."
            ChatLanguage.MIXED -> "AI chat belum ada model generatif yang serasi. Import GGUF dalam Model Manager atau set API key di Settings. ONNX/TFLite sahaja tidak automatik menjadi chatbot tanpa tokenizer dan generative runtime yang sepadan."
        }
        return message + " Status: ${if (modelCount > 0) "$modelCount model diimport" else "tiada model diimport"}; ${NetworkGuardian.label(network)}."
    }
}
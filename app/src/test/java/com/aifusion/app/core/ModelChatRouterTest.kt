package com.aifusion.app.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelChatRouterTest {
    private val lowRam = DeviceCapabilities(
        ramMb = 4096,
        cpuCores = 8,
        lowRamDevice = true,
        gpuApi = "OpenGL ES GPU",
        npuAvailable = false,
        mode = PerformanceMode.LOW_RAM,
        modelTier = "Small / quantized local model"
    )

    @Test
    fun onnxImportIsNotMistakenForChatCapableModel() {
        val model = LocalModel(
            uri = "content://models/local-model.onnx",
            name = "local-model.onnx",
            format = "ONNX",
            sizeBytes = 900L * 1024L * 1024L
        )

        val route = ModelChatRouter.route(listOf(model), lowRam)

        assertFalse(route.chatCapable)
        assertTrue(route.label.contains("ONNX", ignoreCase = true))
        assertTrue(route.reason.contains("tokenizer", ignoreCase = true))
        assertTrue(route.reason.contains("generation", ignoreCase = true))
    }

    @Test
    fun compatibleSizedGgufIsSelectedForChat() {
        val model = LocalModel(
            uri = "content://models/chat.gguf",
            name = "chat.gguf",
            format = "GGUF",
            sizeBytes = 500L * 1024L * 1024L
        )

        val route = ModelChatRouter.route(listOf(model), lowRam)

        assertTrue(route.chatCapable)
        assertTrue(route.model?.name == "chat.gguf")
    }
}

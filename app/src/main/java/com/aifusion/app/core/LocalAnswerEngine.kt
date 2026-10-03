package com.aifusion.app.core

/**
 * Local-first answer layer. It never sends the question to a server.
 * Imported model execution can be connected to a compatible runtime later.
 */
object LocalAnswerEngine {
    fun answer(
        query: String,
        capabilities: DeviceCapabilities,
        network: NetworkState,
        modelCount: Int
    ): String {
        val q = query.lowercase()
        val topic = when {
            "3d" in q || "model 3d" in q -> "3D Design lokal boleh bina dan paparkan OBJ tanpa server."
            "research" in q || "kajian" in q -> "Research Core boleh pecahkan soalan dan simpan hasil secara lokal."
            "ram" in q || "gpu" in q || "npu" in q || "cpu" in q ->
                "Hardware Monitor membaca RAM/CPU dan GPU jika tersedia; NPU tidak dipalsukan jika tiada API umum."
            "4g" in q || "5g" in q || "network" in q || "internet" in q ->
                "Network Guardian mengesan Wi-Fi, 4G/5G dan status metered."
            "onnx" in q || "tflite" in q || "model" in q ->
                "Model lokal yang diimport disimpan pada telefon; runtime bergantung pada format/model serasi."
            else -> "AI-FUSION menggunakan local-first routing dan tidak memerlukan Railway untuk jawapan asas."
        }
        val compute = when {
            capabilities.npuAvailable -> "CPU + GPU/NPU jika disokong"
            capabilities.gpuApi.contains("GPU", true) -> "CPU + GPU"
            else -> "CPU"
        }
        return "Jawapan lokal: $topic Compute: $compute. Model lokal: $modelCount. Network: ${NetworkGuardian.label(network)}."
    }
}
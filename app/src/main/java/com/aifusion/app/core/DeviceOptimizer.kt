package com.aifusion.app.core

import android.app.ActivityManager
import android.content.Context

enum class PerformanceMode {
    LOW_RAM, BALANCED, PERFORMANCE
}

data class DeviceCapabilities(
    val ramMb: Long,
    val cpuCores: Int,
    val lowRamDevice: Boolean,
    val gpuApi: String,
    val npuAvailable: Boolean,
    val mode: PerformanceMode,
    val modelTier: String
)

object DeviceOptimizer {
    fun detect(context: Context): DeviceCapabilities {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val ramMb = (memoryInfo.totalMem / (1024L * 1024L)).coerceAtLeast(0L)
        val lowRam = activityManager?.isLowRamDevice == true || ramMb < 4500
        val mode = when {
            lowRam || ramMb < 4500 -> PerformanceMode.LOW_RAM
            ramMb < 7000 -> PerformanceMode.BALANCED
            else -> PerformanceMode.PERFORMANCE
        }

        val featureNames = context.packageManager.systemAvailableFeatures
            .mapNotNull { it.name }

        val npu = featureNames.any {
            it.contains("neural", ignoreCase = true) ||
            it.contains("nnapi", ignoreCase = true)
        }

        val gpu = when {
            featureNames.any { it.contains("vulkan", ignoreCase = true) } -> "Vulkan-capable GPU"
            featureNames.any { it.startsWith("android.hardware.opengles") } -> "OpenGL ES GPU"
            else -> "GPU API unknown"
        }

        val modelTier = when (mode) {
            PerformanceMode.LOW_RAM -> "Small / quantized local model"
            PerformanceMode.BALANCED -> "Small–medium local model"
            PerformanceMode.PERFORMANCE -> "Medium–large local model"
        }

        return DeviceCapabilities(
            ramMb = ramMb,
            cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            lowRamDevice = lowRam,
            gpuApi = gpu,
            npuAvailable = npu,
            mode = mode,
            modelTier = modelTier
        )
    }

    fun label(mode: PerformanceMode): String = when (mode) {
        PerformanceMode.LOW_RAM -> "Low RAM"
        PerformanceMode.BALANCED -> "Balanced"
        PerformanceMode.PERFORMANCE -> "Performance"
    }
}

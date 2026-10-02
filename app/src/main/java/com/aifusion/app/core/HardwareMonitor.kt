package com.aifusion.app.core

import android.app.ActivityManager
import android.content.Context
import java.io.File
import java.util.Locale

data class HardwareSample(
    val cpuPercent: Float,
    val ramPercent: Float,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val gpuPercent: Float?,
    val npuPercent: Float?
)

object HardwareMonitor {
    private var lastCpuTotal = 0L
    private var lastCpuIdle = 0L

    fun read(context: Context): HardwareSample {
        val memory = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memory)

        val totalRam = (memory.totalMem / 1048576L).coerceAtLeast(1L)
        val availableRam = (memory.availMem / 1048576L).coerceAtLeast(0L)
        val usedRam = (totalRam - availableRam).coerceIn(0L, totalRam)
        val ramPercent = usedRam.toFloat() * 100f / totalRam.toFloat()

        val cpu = readCpuPercent()
        val gpu = readGpuPercent()

        // Android has no generic public API for instantaneous NPU utilization.
        // Keep this null rather than inventing a percentage.
        return HardwareSample(
            cpuPercent = cpu,
            ramPercent = ramPercent,
            ramUsedMb = usedRam,
            ramTotalMb = totalRam,
            gpuPercent = gpu,
            npuPercent = null
        )
    }

    private fun readCpuPercent(): Float {
        return runCatching {
            val line = File("/proc/stat").bufferedReader().useLines { lines ->
                lines.firstOrNull { it.startsWith("cpu ") }
            } ?: return 0f

            val values = line.trim().split(Regex("\\s+")).drop(1).map { it.toLongOrNull() ?: 0L }
            if (values.size < 4) return 0f

            val idle = values[3] + (values.getOrNull(4) ?: 0L)
            val total = values.sum()
            val totalDelta = total - lastCpuTotal
            val idleDelta = idle - lastCpuIdle
            lastCpuTotal = total
            lastCpuIdle = idle

            if (totalDelta <= 0L) 0f
            else ((totalDelta - idleDelta).toFloat() * 100f / totalDelta.toFloat()).coerceIn(0f, 100f)
        }.getOrDefault(0f)
    }

    private fun readGpuPercent(): Float? {
        val paths = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpubusy",
            "/sys/devices/platform/kgsl-3d0/kgsl/kgsl-3d0/gpubusy"
        )
        for (path in paths) {
            val value = runCatching { File(path).takeIf { it.canRead() }?.readText()?.trim() }.getOrNull()
            if (!value.isNullOrBlank()) {
                val parts = value.split(Regex("\\s+"))
                if (parts.size >= 2) {
                    val busy = parts[0].toLongOrNull()
                    val total = parts[1].toLongOrNull()
                    if (busy != null && total != null && total > 0L) {
                        return (busy.toDouble() * 100.0 / total.toDouble()).toFloat().coerceIn(0f, 100f)
                    }
                }
            }
        }
        return null
    }

    fun percentText(value: Float?): String =
        value?.let { String.format(Locale.US, "%.0f%%", it) } ?: "N/A"
}

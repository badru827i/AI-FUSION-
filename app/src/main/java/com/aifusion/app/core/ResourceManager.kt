package com.aifusion.app.core

import android.app.ActivityManager
import android.content.Context

data class ResourceStatus(
    val availableRamMb: Long,
    val appCacheMb: Long
)

object ResourceManager {
    private const val CACHE_LIMIT_MB = 32L

    fun status(context: Context): ResourceStatus {
        val memoryInfo = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memoryInfo)
        return ResourceStatus(
            availableRamMb = memoryInfo.availMem / (1024L * 1024L),
            appCacheMb = directorySize(context.cacheDir) / (1024L * 1024L)
        )
    }

    fun enforceCacheLimit(context: Context) {
        val cacheMb = directorySize(context.cacheDir) / (1024L * 1024L)
        if (cacheMb > CACHE_LIMIT_MB) {
            clearTemporaryCache(context)
        }
    }

    fun clearTemporaryCache(context: Context) {
        context.cacheDir.listFiles()?.forEach { file ->
            runCatching { file.deleteRecursively() }
        }
    }

    private fun directorySize(file: java.io.File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { directorySize(it) } ?: 0L
    }
}

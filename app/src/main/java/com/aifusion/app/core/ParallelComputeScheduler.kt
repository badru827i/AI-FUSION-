package com.aifusion.app.core

import android.content.Context

enum class ComputeUnit { CPU, GPU, NPU }

data class ParallelPlan(val units: Set<ComputeUnit>, val reason: String)

object ParallelComputeScheduler {
    fun plan(context: Context, task: String): ParallelPlan {
        val caps = DeviceOptimizer.detect(context)
        val units = linkedSetOf(ComputeUnit.CPU)
        if (caps.gpuApi.contains("Vulkan", true) || caps.gpuApi.contains("OpenGL", true)) units += ComputeUnit.GPU
        if (caps.npuAvailable) units += ComputeUnit.NPU
        val reason = when {
            caps.lowRamDevice -> "Low-RAM safe mode: CPU-first; accelerators only when supported."
            task.contains("3d", true) || task.contains("render", true) ->
                "3D workload: CPU coordinates while GPU handles graphics when available."
            task.contains("ai", true) || task.contains("model", true) ->
                "AI workload: CPU coordinates and NPU/GPU are preferred for supported operations."
            else -> "Adaptive parallel mode based on detected hardware."
        }
        return ParallelPlan(units, reason)
    }
}
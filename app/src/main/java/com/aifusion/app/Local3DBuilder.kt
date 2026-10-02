package com.aifusion.app

import java.io.File

object Local3DBuilder {
    fun build(prompt: String, directory: File): File {
        val clean = prompt.trim().lowercase()
        val (name, vertices, faces) = when {
            "pyramid" in clean || "piramid" in clean -> Triple(
                "Pyramid",
                listOf(
                    Triple(-1f, 0f, -1f), Triple(1f, 0f, -1f),
                    Triple(1f, 0f, 1f), Triple(-1f, 0f, 1f),
                    Triple(0f, 1.5f, 0f)
                ),
                listOf(
                    intArrayOf(1, 2, 3, 4), intArrayOf(1, 2, 5),
                    intArrayOf(2, 3, 5), intArrayOf(3, 4, 5), intArrayOf(4, 1, 5)
                )
            )
            "plane" in clean || "lantai" in clean || "flat" in clean -> Triple(
                "Plane",
                listOf(
                    Triple(-1f, 0f, -1f), Triple(1f, 0f, -1f),
                    Triple(1f, 0f, 1f), Triple(-1f, 0f, 1f)
                ),
                listOf(intArrayOf(1, 2, 3, 4))
            )
            else -> Triple(
                "Cube",
                listOf(
                    Triple(-1f, -1f, -1f), Triple(1f, -1f, -1f),
                    Triple(1f, 1f, -1f), Triple(-1f, 1f, -1f),
                    Triple(-1f, -1f, 1f), Triple(1f, -1f, 1f),
                    Triple(1f, 1f, 1f), Triple(-1f, 1f, 1f)
                ),
                listOf(
                    intArrayOf(1, 2, 3, 4), intArrayOf(5, 8, 7, 6),
                    intArrayOf(1, 5, 6, 2), intArrayOf(2, 6, 7, 3),
                    intArrayOf(3, 7, 8, 4), intArrayOf(5, 1, 4, 8)
                )
            )
        }

        if (!directory.exists() && !directory.mkdirs()) error("Tidak dapat menyediakan folder 3D")

        val file = File(directory, "AI3D_${System.currentTimeMillis()}.obj")
        file.bufferedWriter().use { out ->
            out.appendLine("# AI-FUSION Local 3D Builder")
            out.appendLine("# Prompt: $prompt")
            out.appendLine("o $name")
            vertices.forEach { (x, y, z) -> out.appendLine("v $x $y $z") }
            faces.forEach { face -> out.appendLine("f ${face.joinToString(" ")}") }
        }
        return file
    }
}

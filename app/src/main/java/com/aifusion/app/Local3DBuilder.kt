package com.aifusion.app

import java.io.File
import kotlin.math.cos
import kotlin.math.sin

object Local3DBuilder {
    fun build(prompt: String, directory: File): File {
        val clean = prompt.trim().lowercase()
        val shape = when {
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
            "cylinder" in clean || "silinder" in clean -> cylinder()
            "robot" in clean || "android" in clean -> robot()
            "car" in clean || "kereta" in clean || "automotif" in clean -> carBody()
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
        val (name, vertices, faces) = shape

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

    private fun cylinder(): Triple<String, List<Triple<Float, Float, Float>>, List<IntArray>> {
        val sides = 16
        val vertices = buildList {
            repeat(sides) { i ->
                val a = 2.0 * Math.PI * i / sides
                add(Triple(cos(a).toFloat(), -1f, sin(a).toFloat()))
                add(Triple(cos(a).toFloat(), 1f, sin(a).toFloat()))
            }
        }
        val faces = buildList {
            for (i in 0 until sides) {
                val n = (i + 1) % sides
                add(intArrayOf(i * 2 + 1, n * 2 + 1, n * 2 + 2, i * 2 + 2))
            }
        }
        return Triple("Cylinder", vertices, faces)
    }

    private fun robot(): Triple<String, List<Triple<Float, Float, Float>>, List<IntArray>> {
        val vertices = mutableListOf<Triple<Float, Float, Float>>()
        val faces = mutableListOf<IntArray>()
        fun box(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float) {
            val start = vertices.size + 1
            vertices += listOf(
                Triple(cx-sx,cy-sy,cz-sz), Triple(cx+sx,cy-sy,cz-sz),
                Triple(cx+sx,cy+sy,cz-sz), Triple(cx-sx,cy+sy,cz-sz),
                Triple(cx-sx,cy-sy,cz+sz), Triple(cx+sx,cy-sy,cz+sz),
                Triple(cx+sx,cy+sy,cz+sz), Triple(cx-sx,cy+sy,cz+sz)
            )
            faces += boxFaces(start)
        }
        box(0f, 0f, 0f, 0.7f, 1.0f, 0.45f)
        box(0f, 1.45f, 0f, 0.5f, 0.45f, 0.4f)
        box(-0.95f, -0.15f, 0f, 0.18f, 0.75f, 0.18f)
        box(0.95f, -0.15f, 0f, 0.18f, 0.75f, 0.18f)
        return Triple("Robot", vertices, faces)
    }

    private fun carBody(): Triple<String, List<Triple<Float, Float, Float>>, List<IntArray>> {
        val vertices = mutableListOf<Triple<Float, Float, Float>>()
        val faces = mutableListOf<IntArray>()
        fun box(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float) {
            val start = vertices.size + 1
            vertices += listOf(
                Triple(cx-sx,cy-sy,cz-sz), Triple(cx+sx,cy-sy,cz-sz),
                Triple(cx+sx,cy+sy,cz-sz), Triple(cx-sx,cy+sy,cz-sz),
                Triple(cx-sx,cy-sy,cz+sz), Triple(cx+sx,cy-sy,cz+sz),
                Triple(cx+sx,cy+sy,cz+sz), Triple(cx-sx,cy+sy,cz+sz)
            )
            faces += boxFaces(start)
        }
        box(0f, 0f, 0f, 1.9f, 0.35f, 0.8f)
        box(0.35f, 0.62f, 0f, 1.05f, 0.3f, 0.68f)
        return Triple("Car Body", vertices, faces)
    }

    private fun boxFaces(start: Int): List<IntArray> = listOf(
        intArrayOf(start,start+1,start+2,start+3),
        intArrayOf(start+4,start+7,start+6,start+5),
        intArrayOf(start,start+4,start+5,start+1),
        intArrayOf(start+1,start+5,start+6,start+2),
        intArrayOf(start+2,start+6,start+7,start+3),
        intArrayOf(start+4,start,start+3,start+7)
    )
}

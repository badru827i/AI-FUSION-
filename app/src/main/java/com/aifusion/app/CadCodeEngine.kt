package com.aifusion.app

import java.io.File
import kotlin.math.cos
import kotlin.math.sin

object CadCodeEngine {
    fun build(code: String, directory: File): File {
        val source = code.trim()
        val lower = source.lowercase()
        return when {
            Regex("""box\s*\(""").containsMatchIn(lower) -> buildBox(parseArgs(source, "box", 3), directory)
            Regex("""cylinder\s*\(""").containsMatchIn(lower) -> buildCylinder(parseArgs(source, "cylinder", 2), directory)
            Regex("""pyramid\s*\(""").containsMatchIn(lower) -> buildPyramid(parseArgs(source, "pyramid", 3), directory)
            else -> error("CAD code tidak dikenali. Guna box(w,h,d), cylinder(r,h) atau pyramid(w,h,d).")
        }
    }
    private fun parseArgs(code: String, fn: String, count: Int): List<Float> {
        val m = Regex("""$fn\s*\(([^)]*)\)""", RegexOption.IGNORE_CASE).find(code)
            ?: error("Sintaks $fn(...) tidak sah")
        val values = m.groupValues[1].split(',').mapNotNull { Regex("""-?\d+(?:\.\d+)?""").find(it)?.value?.toFloatOrNull() }
        require(values.size >= count) { "$fn perlukan $count parameter." }
        return values.take(count).map { it.coerceIn(0.01f, 10000f) }
    }
    private fun write(directory: File, name: String, vertices: List<Triple<Float,Float,Float>>, faces: List<IntArray>): File {
        if (!directory.exists() && !directory.mkdirs()) error("Tidak dapat menyediakan folder 3D")
        val file = File(directory, "AI3D_CAD_${System.currentTimeMillis()}.obj")
        file.bufferedWriter().use { out ->
            out.appendLine("# AI-FUSION CAD Code")
            out.appendLine("o $name")
            vertices.forEach { (x,y,z) -> out.appendLine("v $x $y $z") }
            faces.forEach { out.appendLine("f ${it.joinToString(" ")}") }
        }
        return file
    }
    private fun buildBox(a: List<Float>, dir: File): File {
        val (w,h,d)=a; val x=w/2; val y=h/2; val z=d/2
        val v=listOf(Triple(-x,-y,-z),Triple(x,-y,-z),Triple(x,y,-z),Triple(-x,y,-z),Triple(-x,-y,z),Triple(x,-y,z),Triple(x,y,z),Triple(-x,y,z))
        val f=listOf(intArrayOf(1,2,3,4),intArrayOf(5,8,7,6),intArrayOf(1,5,6,2),intArrayOf(2,6,7,3),intArrayOf(3,7,8,4),intArrayOf(5,1,4,8))
        return write(dir,"CAD Box",v,f)
    }
    private fun buildCylinder(a: List<Float>, dir: File): File {
        val radius=a[0]; val height=a[1]; val sides=32
        val v=mutableListOf<Triple<Float,Float,Float>>()
        repeat(sides){i->val t=2.0*Math.PI*i/sides;v+=Triple((radius*cos(t)).toFloat(),-height/2,(radius*sin(t)).toFloat());v+=Triple((radius*cos(t)).toFloat(),height/2,(radius*sin(t)).toFloat())}
        val f=mutableListOf<IntArray>()
        repeat(sides){i->val n=(i+1)%sides;f+=intArrayOf(i*2+1,n*2+1,n*2+2,i*2+2)}
        return write(dir,"CAD Cylinder",v,f)
    }
    private fun buildPyramid(a: List<Float>, dir: File): File {
        val (w,h,d)=a; val x=w/2; val z=d/2
        val v=listOf(Triple(-x,0f,-z),Triple(x,0f,-z),Triple(x,0f,z),Triple(-x,0f,z),Triple(0f,h,0f))
        val f=listOf(intArrayOf(1,2,3,4),intArrayOf(1,2,5),intArrayOf(2,3,5),intArrayOf(3,4,5),intArrayOf(4,1,5))
        return write(dir,"CAD Pyramid",v,f)
    }
}

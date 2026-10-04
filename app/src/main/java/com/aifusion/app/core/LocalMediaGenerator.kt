package com.aifusion.app.core

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import java.io.File
import kotlin.math.sin

data class MediaGenerationResult(val file: File, val mimeType: String, val type: String)

object LocalMediaGenerator {
    fun generateImage(context: Context, prompt: String): MediaGenerationResult {
        val width = 1024
        val height = 1024
        val bitmap = android.graphics.Bitmap.createBitmap(
            width, height, android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(android.graphics.Color.rgb(7, 11, 20))
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = android.graphics.Color.rgb(60, 220, 255)

        for (r in 120..420 step 60) canvas.drawCircle(512f, 470f, r.toFloat(), paint)
        for (i in 0 until 11) {
            val y = 110f + i * 70f
            canvas.drawLine(90f, y, 934f, y, paint)
        }
        for (i in 0 until 8) {
            val x = 120f + i * 110f
            canvas.drawLine(x, 120f, x, 820f, paint)
        }

        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 44f
        canvas.drawText("AI-FUSION LOCAL CREATE", 72f, 92f, paint)

        paint.textSize = 32f
        prompt.trim().take(160).chunked(38).take(4).forEachIndexed { index, line ->
            canvas.drawText(line, 72f, 790f + index * 42f, paint)
        }

        val file = File(
            context.getExternalFilesDir("generated"),
            "AI_FUSION_${System.currentTimeMillis()}.png"
        )
        file.parentFile?.mkdirs()
        file.outputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return MediaGenerationResult(file, "image/png", "PNG")
    }

    fun generateVideo(
        context: Context,
        prompt: String,
        durationSeconds: Int = 3
    ): MediaGenerationResult {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)

        val width = 480
        val height = 854
        val fps = 24
        val frames = durationSeconds.coerceIn(1, 5) * fps
        val file = File(
            context.getExternalFilesDir("generated"),
            "AI_FUSION_${System.currentTimeMillis()}.mp4"
        )
        file.parentFile?.mkdirs()

        val format = MediaFormat.createVideoFormat("video/avc", width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            )
            setInteger(MediaFormat.KEY_BIT_RATE, 1_800_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType("video/avc")
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1
        val info = MediaCodec.BufferInfo()

        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            for (frame in 0 until frames) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val input = codec.getInputBuffer(inputIndex) ?: continue
                    input.clear()
                    fillNv12(input, width, height, frame, prompt.hashCode())
                    val pts = frame * 1_000_000L / fps
                    codec.queueInputBuffer(inputIndex, 0, input.position(), pts, 0)
                }
                drain(codec, muxer, info, { track ->
                    if (!muxerStarted) {
                        trackIndex = track
                        muxer.start()
                        muxerStarted = true
                    }
                })
            }

            val eosIndex = codec.dequeueInputBuffer(10_000)
            if (eosIndex >= 0) {
                codec.queueInputBuffer(
                    eosIndex,
                    0,
                    0,
                    frames * 1_000_000L / fps,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }

            var eos = false
            repeat(40) {
                eos = drain(codec, muxer, info, { track ->
                    if (!muxerStarted) {
                        trackIndex = track
                        muxer.start()
                        muxerStarted = true
                    }
                }) || eos
                if (eos) return@repeat
                Thread.sleep(10)
            }

            check(muxerStarted && trackIndex >= 0 && file.length() > 1024L) {
                "Video encoder tidak menghasilkan MP4 yang sah."
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
            if (muxerStarted) runCatching { muxer?.stop() }
            muxer?.release()
        }

        return MediaGenerationResult(file, "video/mp4", "MP4")
    }

    private fun drain(
        codec: MediaCodec,
        muxer: MediaMuxer?,
        info: MediaCodec.BufferInfo,
        onTrack: (Int) -> Unit
    ): Boolean {
        var eos = false
        while (true) {
            val index = codec.dequeueOutputBuffer(info, 0)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> return eos
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    onTrack(requireNotNull(muxer).addTrack(codec.outputFormat))
                }
                index >= 0 -> {
                    val output = codec.getOutputBuffer(index)
                    if (output != null && info.size > 0 && muxer != null) {
                        output.position(info.offset)
                        output.limit(info.offset + info.size)
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            muxer.writeSampleData(0, output, info)
                        }
                    }
                    eos = eos || (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                    codec.releaseOutputBuffer(index, false)
                    if (eos) return true
                }
            }
        }
    }

    private fun fillNv12(
        buffer: java.nio.ByteBuffer,
        width: Int,
        height: Int,
        frame: Int,
        seed: Int
    ) {
        val ySize = width * height
        val shift = (frame * 7 + seed).ushr(2) and 255
        repeat(height) { y ->
            repeat(width) { x ->
                val wave = (sin((x + shift) * 0.04) * 35).toInt()
                buffer.put(((x + y + wave + frame * 5) and 255).toByte())
            }
        }
        repeat(ySize / 2) { i ->
            val uv = (96 + ((i + shift) % 96)).coerceIn(0, 255)
            buffer.put(uv.toByte())
        }
    }
}

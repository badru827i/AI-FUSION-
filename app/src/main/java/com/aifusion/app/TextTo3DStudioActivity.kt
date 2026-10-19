package com.aifusion.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.io.File

class TextTo3DStudioActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PROMPT = "ai_fusion_text_to_3d_prompt"
    }

    private lateinit var promptInput: EditText
    private lateinit var detailValue: TextView
    private lateinit var detailSlider: SeekBar
    private lateinit var qualitySpinner: Spinner
    private lateinit var styleSpinner: Spinner
    private lateinit var textureSpinner: Spinner
    private lateinit var symmetryCheck: CheckBox
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "AI-FUSION 3D Studio"
        window.statusBarColor = Color.rgb(10, 14, 22)
        window.navigationBarColor = Color.rgb(10, 14, 22)
        setContentView(buildContent())
        promptInput.setText(intent.getStringExtra(EXTRA_PROMPT).orEmpty().ifBlank {
            "Kereta sport futuristik, badan hitam metalik, spoiler besar"
        })
    }

    private fun buildContent(): ScrollView {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(24))
            setBackgroundColor(Color.rgb(10, 14, 22))
        }
        fun label(text: String, size: Float = 14f): TextView =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(Color.rgb(232, 239, 249))
                setPadding(0, dp(12), 0, dp(6))
            }
        fun spinner(values: List<String>): Spinner {
            return Spinner(this).apply {
                adapter = ArrayAdapter(
                    this@TextTo3DStudioActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    values
                )
            }
        }
        page.addView(label("AI-FUSION 3D Studio", 24f))
        page.addView(label("Text-to-3D • pemprosesan setempat • eksport OBJ", 13f).apply {
            setTextColor(Color.rgb(137, 157, 183))
        })
        page.addView(label("Arahan model"))
        promptInput = EditText(this).apply {
            hint = "Terangkan objek 3D yang mahu dibina"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.rgb(25, 32, 45))
            minLines = 3
            maxLines = 6
            gravity = android.view.Gravity.TOP
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        page.addView(promptInput, matchWidth())

        page.addView(label("Kualiti"))
        qualitySpinner = spinner(listOf("Fast", "Balanced", "High detail"))
        qualitySpinner.setSelection(2)
        page.addView(qualitySpinner, matchWidth())

        page.addView(label("Gaya"))
        styleSpinner = spinner(listOf("Mechanical CAD", "Realistic", "Stylized", "Low-poly"))
        page.addView(styleSpinner, matchWidth())

        page.addView(label("Tekstur"))
        textureSpinner = spinner(listOf("None", "PBR", "AI-generated"))
        page.addView(textureSpinner, matchWidth())

        page.addView(label("Tahap perincian"))
        detailValue = label("100%")
        page.addView(detailValue)
        detailSlider = SeekBar(this).apply {
            max = 90
            progress = 90
        }
        page.addView(detailSlider, matchWidth())
        detailSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                detailValue.text = "${progress + 10}%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        symmetryCheck = CheckBox(this).apply {
            text = "Simetri kiri dan kanan"
            isChecked = true
            setTextColor(Color.rgb(232, 239, 249))
        }
        page.addView(symmetryCheck)

        page.addView(label("Eksport"))
        page.addView(label("OBJ • geometri mesh", 13f).apply {
            setTextColor(Color.rgb(137, 157, 183))
        })

        val generate = Button(this).apply {
            text = "Jana model 3D"
            setOnClickListener { generateModel() }
        }
        page.addView(generate, matchWidth())

        val previewNote = label(
            "Model dijana disimpan secara tempatan. Preview dibuka dalam 3D Studio sedia ada.",
            12f
        ).apply { setTextColor(Color.rgb(137, 157, 183)) }
        page.addView(previewNote)

        status = label(
            "Enjin generatif neural belum dipasang. Mod prosedural luar talian tersedia sebagai fallback.",
            12f
        ).apply { setTextColor(Color.rgb(255, 196, 100)) }
        page.addView(status)

        page.addView(Button(this).apply {
            text = "Kembali ke Chat"
            setOnClickListener { finish() }
        }, matchWidth())

        return ScrollView(this).apply {
            addView(page, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private fun generateModel() {
        val quality = when (qualitySpinner.selectedItemPosition) {
            0 -> Text3DQuality.FAST
            1 -> Text3DQuality.BALANCED
            else -> Text3DQuality.HIGH_DETAIL
        }
        val style = when (styleSpinner.selectedItemPosition) {
            1 -> Text3DStyle.REALISTIC
            2 -> Text3DStyle.STYLIZED
            3 -> Text3DStyle.LOW_POLY
            else -> Text3DStyle.MECHANICAL_CAD
        }
        val texture = when (textureSpinner.selectedItemPosition) {
            1 -> Text3DTexture.PBR
            2 -> Text3DTexture.GENERATED
            else -> Text3DTexture.NONE
        }
        val request = Text3DRequest(
            prompt = promptInput.text?.toString().orEmpty(),
            quality = quality,
            detailPercent = detailSlider.progress + 10,
            style = style,
            texture = texture,
            symmetry = symmetryCheck.isChecked,
            format = Text3DFormat.OBJ
        )
        LocalTextTo3DEngine.validate(request)?.let {
            AlertDialog.Builder(this).setTitle("Parameter tidak sah").setMessage(it)
                .setPositiveButton("OK", null).show()
            return
        }

        status.text = "Menyemak RAM dan membina geometri setempat…"
        status.setTextColor(Color.rgb(137, 200, 255))
        val result = runCatching {
            LocalTextTo3DEngine.generate(this, request, File(cacheDir, "fusion3d"))
        }
        result.onSuccess { generated ->
            status.text = "${generated.engineLabel} • ${generated.vertexCount} vertex • ${generated.faceCount} faces\n${generated.notice}"
            status.setTextColor(Color.rgb(255, 196, 100))
            startActivity(
                Intent(this, Fusion3DActivity::class.java)
                    .putExtra(Fusion3DActivity.EXTRA_MODEL_PATH, generated.file.absolutePath)
            )
        }.onFailure { error ->
            status.text = "Penjanaan gagal: ${error.message ?: "ralat tidak diketahui"}"
            status.setTextColor(Color.rgb(255, 125, 125))
        }
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

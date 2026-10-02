package com.aifusion.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import org.the3deer.android.engine.Model
import org.the3deer.android.engine.ModelEngine
import org.the3deer.android.engine.ModelEngineViewModel
import org.the3deer.android.engine.renderer.GLRenderer
import org.the3deer.android.engine.renderer.GLSurfaceView
import org.the3deer.android.engine.services.LoaderRegistry
import org.the3deer.android.engine.services.fbx.FbxLoaderTask
import org.the3deer.android.engine.services.wavefront.WavefrontLoaderTask
import org.the3deer.android.util.ContentUtils
import java.io.File
import java.net.URI

class Fusion3DActivity : ComponentActivity(), ContentUtils.ContentResolver {

    private lateinit var surface: GLSurfaceView
    private lateinit var statusView: TextView
    private lateinit var modelNameView: TextView
    private lateinit var viewModel: ModelEngineViewModel
    private var activeUri: String? = null
    private var currentModel: Model? = null

    private val openModel = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Some providers do not offer persistable access; use the URI for this session.
        }

        val name = displayName(uri)
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension !in setOf("obj", "fbx")) {
            statusView.text = "3D error: hanya OBJ dan FBX disokong."
            return@registerForActivityResult
        }
        loadModel(uri, name, extension)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContentUtils.setContext(this)
        ContentUtils.setContentResolver(this)

        LoaderRegistry.register("obj") { uri, listener -> WavefrontLoaderTask(uri, listener) }
        LoaderRegistry.register("fbx") { uri, listener -> FbxLoaderTask(uri, listener) }

        setContentView(R.layout.activity_fusion_3d)
        surface = findViewById(R.id.fusion_3d_surface)
        statusView = findViewById(R.id.fusion_3d_status)
        modelNameView = findViewById(R.id.fusion_3d_model_name)

        try {
            surface.setEGLContextClientVersion(3)
        } catch (_: RuntimeException) {
            surface.setEGLContextClientVersion(2)
        }

        viewModel = ViewModelProvider(this)[ModelEngineViewModel::class.java]
        surface.setRenderer(GLRenderer(viewModel.glScreen, viewModel))
        surface.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        findViewById<Button>(R.id.fusion_3d_open).setOnClickListener {
            openModel.launch(arrayOf("*/*"))
        }
        findViewById<Button>(R.id.fusion_3d_build).setOnClickListener {
            showBuildDialog()
        }
        findViewById<Button>(R.id.fusion_3d_analyze).setOnClickListener {
            analyzeScene()
        }
        findViewById<Button>(R.id.fusion_3d_selected).setOnClickListener {
            analyzeSelected()
        }
        findViewById<Button>(R.id.fusion_3d_back).setOnClickListener {
            finish()
        }

        statusView.text = "3D siap • 1 jari rotate 360° • pinch zoom • 2 jari pan"
    }

    private fun displayName(uri: Uri): String {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getString(0)
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "model.3d" } ?: "model.3d"
    }

    private fun loadModel(androidUri: Uri, name: String, extension: String) {
        val uriString = androidUri.toString()
        activeUri = uriString
        val model = Model(URI.create(uriString), name, extension)
        currentModel = model
        modelNameView.text = name
        statusView.text = "Loading $extension…"

        viewModel.initEngine(model.id.toString(), model.name, model.type) {
            try {
                val engine = viewModel.getEngine(uriString)
                    ?: error("3D engine gagal diinisialisasi")
                engine.addOrReplace("gl.surfaceView", surface)
                engine.addOrReplace("gl.renderer", surface.renderer)

                viewModel.loadEngine(uriString) {
                    viewModel.startEngine(uriString) {
                        viewModel.setActiveEngine(uriString)
                        runOnUiThread {
                            statusView.text = "Model dibuka • $extension • sentuh untuk rotate 360°"
                        }
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    statusView.text = "3D error: ${error.message ?: "unknown"}"
                }
            }
        }
    }

    private fun showBuildDialog() {
        val input = EditText(this)
        input.hint = "Contoh: cube, pyramid, plane"
        AlertDialog.Builder(this)
            .setTitle("Build 3D secara lokal")
            .setMessage("AI-FUSION bina mesh OBJ asas terus pada telefon tanpa server.")
            .setView(input)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Build") { _, _ ->
                try {
                    val file = Local3DBuilder.build(
                        input.text?.toString().orEmpty().ifBlank { "cube" },
                        File(cacheDir, "fusion3d")
                    )
                    loadModel(Uri.fromFile(file), file.name, "obj")
                } catch (error: Exception) {
                    statusView.text = "Build 3D error: ${error.message ?: "unknown"}"
                }
            }
            .show()
    }

    private fun activeEngine(): ModelEngine? {
        val uri = activeUri ?: return null
        return viewModel.getEngine(uri)
    }

    private fun analyzeScene() {
        val scene = activeEngine()?.model?.activeScene
        if (scene == null) {
            statusView.text = "Tiada model 3D aktif."
            return
        }

        val objects = scene.objects
        val lines = objects.take(60).mapIndexed { index, obj ->
            val largest = runCatching { obj.currentDimensions2.largest }.getOrDefault(0f)
            val name = obj.name?.takeIf { it.isNotBlank() } ?: obj.id ?: "Object"
            "${index + 1}. $name • size ${"%.2f".format(largest)}"
        }

        val summary = buildString {
            append("AI 3D understanding\n")
            append("Model: ${currentModel?.name ?: "unknown"}\n")
            append("Objects: ${objects.size}\n\n")
            append(if (lines.isEmpty()) "Tiada mesh object dikesan." else lines.joinToString("\n"))
        }

        AlertDialog.Builder(this)
            .setTitle("3D Structure")
            .setMessage(summary)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun analyzeSelected() {
        val obj = activeEngine()?.model?.activeScene?.selectedObject
        if (obj == null) {
            statusView.text = "Tap satu objek dahulu, kemudian tekan Analyze selected."
            return
        }

        val largest = runCatching { obj.currentDimensions2.largest }.getOrDefault(0f)
        val name = obj.name?.takeIf { it.isNotBlank() } ?: obj.id ?: "Object"
        AlertDialog.Builder(this)
            .setTitle("AI 3D Object")
            .setMessage("Object: $name\nEstimated size: ${"%.2f".format(largest)}\n\nAI-FUSION membaca objek yang dipilih daripada scene 3D.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun resolveUri(uri: URI): URI? = uri

    override fun onResume() {
        super.onResume()
        if (::surface.isInitialized) surface.onResume()
    }

    override fun onPause() {
        if (::surface.isInitialized) surface.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        activeUri?.let { if (::viewModel.isInitialized) viewModel.resetEngine(it) }
        activeUri = null
        currentModel = null
        super.onDestroy()
    }
}

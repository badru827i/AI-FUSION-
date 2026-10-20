package com.aifusion.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal enum class JarvisScanMode { OFF, ENROLL_OWNER, VERIFY_OWNER }

class JarvisGuardActivity : FragmentActivity() {
    internal lateinit var vault: JarvisVault
        private set
    internal lateinit var embedder: LocalFaceEmbedder
        private set

    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: FaceDetector? = null
    private val frameExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)
    @Volatile private var scanMode = JarvisScanMode.OFF
    private var lastFrameAt = 0L
    private var lastSampleAt = 0L
    private val samples = Collections.synchronizedList(mutableListOf<FloatArray>())
    private var minYaw = Float.MAX_VALUE
    private var maxYaw = -Float.MAX_VALUE
    private var matchStreak = 0
    @Volatile private var statusSink: ((String) -> Unit)? = null
    @Volatile private var modelInstallSink: ((Boolean, String) -> Unit)? = null
    @Volatile private var verifiedSink: ((Boolean) -> Unit)? = null
    @Volatile private var ownerSink: ((Boolean) -> Unit)? = null
    private var prompt: BiometricPrompt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vault = JarvisVault(this)
        embedder = LocalFaceEmbedder(this)
        val faceOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        detector = FaceDetection.getClient(faceOptions)

        val modelPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                runCatching { embedder.installModel(uri) }
                    .onSuccess { notifyModelInstalled(true, "Model muka disimpan secara setempat dan berjaya disemak.") }
                    .onFailure { notifyModelInstalled(embedder.isReady, "Model ditolak: " + (it.message ?: "format tidak disokong")) }
            }
        }

        setContent {
            MaterialTheme {
                JarvisGuardScreen(
                    activity = this,
                    initialModelReady = embedder.isReady,
                    initialOwnerRegistered = vault.hasOwnerTemplate(),
                    initialEndpoint = vault.loadRobotEndpoint(),
                    initialSecretSaved = vault.hasRobotSecret(),
                    onPickModel = { modelPicker.launch(arrayOf("application/octet-stream", "application/x-tflite", "*/*")) }
                )
            }
        }
    }

    fun configureUi(statusCallback: (String) -> Unit, modelCallback: (Boolean, String) -> Unit) {
        statusSink = statusCallback
        modelInstallSink = modelCallback
    }

    private fun notifyModelInstalled(ready: Boolean, message: String) {
        runOnUiThread {
            modelInstallSink?.invoke(ready, message)
            statusSink?.invoke(message)
        }
    }

    fun beginScan(
        previewView: PreviewView,
        mode: JarvisScanMode,
        statusCallback: (String) -> Unit,
        verifiedCallback: (Boolean) -> Unit,
        ownerCallback: (Boolean) -> Unit
    ) {
        if (!embedder.isReady) {
            statusCallback("Import model pengecaman muka berlesen dahulu. Pengesan muka sahaja tidak mengenali identiti.")
            return
        }
        statusSink = statusCallback
        verifiedSink = verifiedCallback
        ownerSink = ownerCallback
        cameraProvider?.unbindAll()
        scanMode = mode
        samples.clear()
        minYaw = Float.MAX_VALUE
        maxYaw = -Float.MAX_VALUE
        matchStreak = 0
        lastFrameAt = 0L
        lastSampleAt = 0L
        sendStatus(
            if (mode == JarvisScanMode.ENROLL_OWNER)
                "Pendaftaran aktif: lihat kamera, kemudian pusing kepala perlahan ke kiri dan kanan."
            else
                "Pengesahan aktif: satu muka sahaja. Gerakkan kepala perlahan ke kiri dan kanan."
        )
        bindCamera(previewView)
    }

    fun stopScan(statusCallback: ((String) -> Unit)? = null) {
        scanMode = JarvisScanMode.OFF
        cameraProvider?.unbindAll()
        statusCallback?.invoke("Imbasan dihentikan.")
    }

    fun promptStrongBiometric(callback: (Boolean, String) -> Unit) {
        val availability = BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            callback(false, "Biometrik kelas kuat tidak tersedia pada peranti ini.")
            return
        }

        prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    callback(true, "Pengesahan biometrik Android berjaya.")
                }

                override fun onAuthenticationFailed() {
                    callback(false, "Biometrik tidak sepadan. Cuba lagi.")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    callback(false, errString.toString())
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("AI-FUSION JARVIS")
            .setSubtitle("Sahkan identiti sebelum membenarkan sesi robot")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Batal")
            .build()
        prompt?.authenticate(info)
    }

    fun generateRobotKey(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun bindCamera(previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            runCatching {
                val provider = future.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(640, 480))
                    .build()
                analysis.setAnalyzer(frameExecutor) { proxy -> analyseFrame(proxy) }
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
            }.onFailure {
                sendStatus("Kamera tidak dapat dimulakan: " + (it.message ?: "ralat kamera"))
                scanMode = JarvisScanMode.OFF
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyseFrame(proxy: ImageProxy) {
        if (scanMode == JarvisScanMode.OFF || !busy.compareAndSet(false, true)) {
            proxy.close()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastFrameAt < 550L) {
            proxy.close()
            busy.set(false)
            return
        }
        lastFrameAt = now

        val mediaImage = proxy.image
        if (mediaImage == null) {
            proxy.close()
            busy.set(false)
            return
        }

        val input = try {
            InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
        } catch (error: Exception) {
            proxy.close()
            busy.set(false)
            sendStatus("Rangka kamera tidak sah.")
            return
        }

        val task: Task<List<Face>>? = detector?.process(input)
        if (task == null) {
            proxy.close()
            busy.set(false)
            return
        }
        task.addOnSuccessListener(frameExecutor) { faces ->
            runCatching { handleFaces(proxy, faces) }
                .onFailure { sendStatus("Pemprosesan wajah gagal: " + (it.message ?: "ralat model")) }
        }.addOnFailureListener(frameExecutor) {
            sendStatus("Pengesan muka gagal membaca bingkai.")
        }.addOnCompleteListener(frameExecutor) {
            proxy.close()
            busy.set(false)
        }
    }

    private fun handleFaces(proxy: ImageProxy, faces: List<Face>) {
        if (scanMode == JarvisScanMode.OFF) return
        if (faces.size != 1) {
            matchStreak = 0
            sendStatus(if (faces.isEmpty()) "Cari muka dalam pencahayaan yang baik…" else "Lebih daripada satu muka dikesan. Pastikan hanya pemilik berada di kamera.")
            return
        }

        val face = faces.first()
        if (face.boundingBox.width() < 90 || face.boundingBox.height() < 90) {
            sendStatus("Dekatkan muka sedikit dan pastikan pencahayaan mencukupi.")
            return
        }

        val bitmap = proxyToUprightBitmap(proxy) ?: run {
            sendStatus("Bingkai kamera tidak dapat ditukar.")
            return
        }
        val crop = cropAndAlignFace(bitmap, face)
        bitmap.recycle()
        if (crop == null) {
            sendStatus("Muka tidak berada sepenuhnya dalam bingkai.")
            return
        }

        val embedding = try {
            embedder.embed(crop)
        } finally {
            crop.recycle()
        }
        val yaw = face.headEulerAngleY
        minYaw = min(minYaw, yaw)
        maxYaw = max(maxYaw, yaw)
        val yawRange = maxYaw - minYaw
        val now = System.currentTimeMillis()

        when (scanMode) {
            JarvisScanMode.OFF -> Unit
            JarvisScanMode.ENROLL_OWNER -> {
                if (now - lastSampleAt >= 700L && samples.size < 12) {
                    samples.add(embedding)
                    lastSampleAt = now
                }
                if (samples.size >= 5 && yawRange >= 15f) {
                    val template = LocalFaceEmbedder.averageEmbeddings(samples.toList())
                    vault.saveOwnerTemplate(template)
                    scanMode = JarvisScanMode.OFF
                    cameraProvider?.unbindAll()
                    ownerSink?.invoke(true)
                    sendStatus("Profil pemilik disimpan secara terenkripsi. Imej kamera tidak disimpan.")
                } else {
                    sendStatus("Sampel pemilik " + samples.size + "/5 · gerakkan kepala perlahan ke kiri dan kanan.")
                }
            }
            JarvisScanMode.VERIFY_OWNER -> {
                val registered = vault.loadOwnerTemplate()
                if (registered == null) {
                    scanMode = JarvisScanMode.OFF
                    sendStatus("Belum ada profil pemilik. Daftar muka dahulu.")
                    return
                }
                val similarity = LocalFaceEmbedder.cosineSimilarity(embedding, registered)
                if (similarity >= MIN_SIMILARITY) {
                    matchStreak += 1
                } else {
                    matchStreak = 0
                }
                if (matchStreak >= 3 && yawRange >= 15f) {
                    scanMode = JarvisScanMode.OFF
                    cameraProvider?.unbindAll()
                    verifiedSink?.invoke(true)
                    sendStatus("Wajah sepadan dengan profil pemilik. Pengesahan biometrik kedua masih diperlukan untuk robot.")
                } else {
                    val score = (similarity * 100f).roundToInt()
                    sendStatus("Padanan muka $score% · pengesahan $matchStreak/3 · gerakkan kepala kiri/kanan.")
                }
            }
        }
    }

    private fun cropAndAlignFace(bitmap: Bitmap, face: Face): Bitmap? {
        val box = face.boundingBox
        val padX = (box.width() * 0.18f).roundToInt()
        val padY = (box.height() * 0.18f).roundToInt()
        val left = (box.left - padX).coerceIn(0, bitmap.width - 1)
        val top = (box.top - padY).coerceIn(0, bitmap.height - 1)
        val right = (box.right + padX).coerceIn(left + 1, bitmap.width)
        val bottom = (box.bottom + padY).coerceIn(top + 1, bitmap.height)
        if (right - left < 40 || bottom - top < 40) return null

        val crop = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        if (leftEye == null || rightEye == null) return crop

        val angle = Math.toDegrees(
            atan2(
                (rightEye.y - leftEye.y).toDouble(),
                (rightEye.x - leftEye.x).toDouble()
            )
        ).toFloat()
        if (kotlin.math.abs(angle) < 2f || kotlin.math.abs(angle) > 25f) return crop

        val matrix = Matrix().apply { postRotate(-angle, crop.width / 2f, crop.height / 2f) }
        val aligned = Bitmap.createBitmap(crop, 0, 0, crop.width, crop.height, matrix, true)
        if (aligned !== crop) crop.recycle()
        return aligned
    }

    private fun proxyToUprightBitmap(proxy: ImageProxy): Bitmap? {
        return runCatching {
            val width = proxy.width
            val height = proxy.height
            val planes = proxy.planes
            val nv21 = ByteArray(width * height + (width * height / 2))
            var position = 0

            val yPlane = planes[0]
            val yBuffer = yPlane.buffer.duplicate()
            for (row in 0 until height) {
                val rowStart = row * yPlane.rowStride
                for (col in 0 until width) {
                    nv21[position++] = yBuffer.get(rowStart + col * yPlane.pixelStride)
                }
            }

            val uPlane = planes[1]
            val vPlane = planes[2]
            val uBuffer = uPlane.buffer.duplicate()
            val vBuffer = vPlane.buffer.duplicate()
            val chromaHeight = height / 2
            val chromaWidth = width / 2
            for (row in 0 until chromaHeight) {
                for (col in 0 until chromaWidth) {
                    nv21[position++] = vBuffer.get(row * vPlane.rowStride + col * vPlane.pixelStride)
                    nv21[position++] = uBuffer.get(row * uPlane.rowStride + col * uPlane.pixelStride)
                }
            }

            val stream = ByteArrayOutputStream()
            YuvImage(nv21, ImageFormat.NV21, width, height, null)
                .compressToJpeg(Rect(0, 0, width, height), 82, stream)
            val raw = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
                ?: return null
            val rotation = proxy.imageInfo.rotationDegrees
            if (rotation == 0) return raw
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val upright = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            if (upright !== raw) raw.recycle()
            upright
        }.getOrNull()
    }

    private fun sendStatus(message: String) {
        runOnUiThread { statusSink?.invoke(message) }
    }

    override fun onPause() {
        scanMode = JarvisScanMode.OFF
        cameraProvider?.unbindAll()
        super.onPause()
    }

    override fun onDestroy() {
        scanMode = JarvisScanMode.OFF
        cameraProvider?.unbindAll()
        detector?.close()
        embedder.close()
        frameExecutor.shutdownNow()
        super.onDestroy()
    }

    companion object {
        // Starting threshold; it must be calibrated against the exact licensed model on real devices.
        private const val MIN_SIMILARITY = 0.55f
    }
}

@Composable
private fun JarvisGuardScreen(
    activity: JarvisGuardActivity,
    initialModelReady: Boolean,
    initialOwnerRegistered: Boolean,
    initialEndpoint: String,
    initialSecretSaved: Boolean,
    onPickModel: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var modelReady by remember { mutableStateOf(initialModelReady) }
    var ownerRegistered by remember { mutableStateOf(initialOwnerRegistered) }
    var faceVerified by remember { mutableStateOf(false) }
    var robotSession by remember { mutableStateOf(false) }
    var robotSecretSaved by remember { mutableStateOf(initialSecretSaved) }
    var endpoint by remember { mutableStateOf(initialEndpoint) }
    var robotSecret by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(
        if (initialModelReady) "Model muka tersedia. Semua pemprosesan muka berjalan pada telefon."
        else "Model pengenalan muka belum dipasang. Pengesan muka sahaja tidak boleh mengenali pemilik."
    ) }
    var pendingMode by remember { mutableStateOf(JarvisScanMode.OFF) }
    SideEffect {
        activity.configureUi(
            statusCallback = { status = it },
            modelCallback = { ready, message -> modelReady = ready; status = message }
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val preview = previewView
        if (granted && preview != null && pendingMode != JarvisScanMode.OFF) {
            activity.beginScan(preview, pendingMode, { status = it }, { faceVerified = it }, { ownerRegistered = it })
        } else if (!granted) {
            status = "Kebenaran kamera diperlukan untuk pengecaman muka setempat."
        }
    }

    fun begin(mode: JarvisScanMode) {
        faceVerified = false
        robotSession = false
        val preview = previewView
        if (preview == null) {
            status = "Kamera sedang disediakan. Cuba sekali lagi."
            return
        }
        if (!modelReady) {
            status = "Import model pengenalan muka yang serasi dan berlesen dahulu."
            return
        }
        pendingMode = mode
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            activity.beginScan(preview, mode, { status = it }, { faceVerified = it }, { ownerRegistered = it })
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("JARVIS · Privacy Guard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Pengecaman muka setempat, peti simpanan terenkripsi dan gerbang keselamatan robot.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1 · Owner Face ID", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (modelReady) "Model pengecaman: tersedia" else "Model pengecaman: belum dipasang",
                        color = if (modelReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (ownerRegistered) "Profil pemilik: berdaftar pada peranti ini" else "Profil pemilik: belum berdaftar",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(285.dp),
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                previewView = this
                            }
                        },
                        update = { previewView = it }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onPickModel) { Text("Import model") }
                        TextButton(onClick = { begin(JarvisScanMode.ENROLL_OWNER) }, enabled = modelReady) {
                            Text("Daftar muka")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { begin(JarvisScanMode.VERIFY_OWNER) }, enabled = modelReady && ownerRegistered) {
                            Text("Kenali pemilik")
                        }
                        TextButton(onClick = {
                            activity.stopScan { status = it }
                            activity.vault.deleteOwnerTemplate()
                            ownerRegistered = false
                            faceVerified = false
                            robotSession = false
                            status = "Profil muka setempat dipadam."
                        }, enabled = ownerRegistered) {
                            Text("Padam profil")
                        }
                    }
                    Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Privasi: imej hanya diproses dalam RAM; yang disimpan hanyalah vektor ciri muka terenkripsi. Gerakan kepala ialah semakan asas, bukan anti-spoof yang dijamin.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("2 · Pengesahan tindakan sensitif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (faceVerified) "Muka sepadan — menunggu biometrik Android kelas kuat."
                        else "Kenali pemilik dahulu. Kamera sahaja tidak membuka kawalan robot.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            if (!faceVerified) {
                                status = "Sahkan wajah pemilik dahulu."
                            } else {
                                activity.promptStrongBiometric { success, message ->
                                    if (!success) {
                                        status = message
                                        robotSession = false
                                    } else {
                                        status = "Biometrik Android lulus. Menguji kebenaran ESP32…"
                                        scope.launch {
                                            val secret = robotSecret.takeIf { it.isNotBlank() } ?: activity.vault.loadRobotSecret()
                                            val result = withContext(Dispatchers.IO) {
                                                if (secret.isNullOrBlank()) RobotGatewayResult(false, "Simpan kunci ESP32 dahulu.")
                                                else JarvisRobotGateway.authorize(endpoint, secret)
                                            }
                                            robotSession = result.ok
                                            status = result.message
                                        }
                                    }
                                }
                            }
                        },
                        enabled = faceVerified && endpoint.isNotBlank() && robotSecretSaved
                    ) {
                        Text("Fingerprint / biometrik + kebenaran robot")
                    }
                    Text(
                        "Android memilih biometrik kelas kuat yang tersedia; aplikasi tidak boleh memaksa jenis sensor cap jari pada semua telefon.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("3 · ESP32 local gateway", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Alamat ESP32") },
                        placeholder = { Text("http://192.168.4.1") }
                    )
                    OutlinedTextField(
                        value = robotSecret,
                        onValueChange = { robotSecret = it.filter { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' }.take(64) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text("Kunci ESP32 · 64 aksara hex") },
                        placeholder = { Text(if (robotSecretSaved) "Disimpan terenkripsi; kosongkan untuk guna kunci tersimpan" else "Jana kunci dan salin ke firmware ESP32") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            robotSecret = activity.generateRobotKey()
                            status = "Kunci baharu dijana. Salin nilai yang sama ke firmware ESP32 sebelum pairing."
                        }) { Text("Jana kunci") }
                        TextButton(onClick = {
                            if (robotSecret.isBlank()) status = "Jana atau masukkan kunci dahulu."
                            else {
                                clipboard.setText(AnnotatedString(robotSecret))
                                status = "Kunci disalin ke clipboard. Jangan kongsi atau tampal ke repo awam."
                            }
                        }) { Text("Salin kunci") }
                    }
                    TextButton(onClick = {
                        runCatching {
                            activity.vault.saveRobotEndpoint(endpoint)
                            if (robotSecret.isNotBlank()) activity.vault.saveRobotSecret(robotSecret)
                            robotSecretSaved = activity.vault.hasRobotSecret()
                            status = "Tetapan disimpan dalam storan peribadi terenkripsi. Kunci tidak disegerakkan."
                            robotSession = false
                        }.onFailure { status = it.message ?: "Tetapan gagal disimpan." }
                    }) { Text("Simpan tetapan") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            scope.launch {
                                status = "Menguji ESP32 pada rangkaian tempatan…"
                                val result = withContext(Dispatchers.IO) { JarvisRobotGateway.testStatus(endpoint) }
                                status = result.message
                            }
                        }) { Text("Uji sambungan") }
                        TextButton(onClick = {
                            scope.launch {
                                val secret = robotSecret.takeIf { it.isNotBlank() } ?: activity.vault.loadRobotSecret()
                                status = "Menghantar arahan STOP…"
                                val result = withContext(Dispatchers.IO) {
                                    JarvisRobotGateway.command(endpoint, secret, "STOP")
                                }
                                status = result.message
                                if (!result.ok) robotSession = false
                            }
                        }) { Text("EMERGENCY STOP") }
                    }
                    Text(
                        if (robotSession) "Sesi gateway: dibenarkan buat sementara" else "Sesi gateway: terkunci",
                        color = if (robotSession) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Firmware contoh sengaja mengunci pergerakan motor. Arahan gerak hanya patut diaktifkan selepas pin, driver motor, had arus dan emergency-stop fizikal disahkan.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

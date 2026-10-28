package com.aifusion.app

import android.Manifest
import android.graphics.BitmapFactory
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.aifusion.app.core.ChatMessage
import com.aifusion.app.core.ChatSession
import com.aifusion.app.core.DeviceCapabilities
import com.aifusion.app.core.HardwareMonitor
import com.aifusion.app.core.HardwareSample
import com.aifusion.app.core.DeviceOptimizer
import com.aifusion.app.core.LocalChatStore
import com.aifusion.app.core.AiChatClient
import com.aifusion.app.core.ApiKeyStore
import com.aifusion.app.core.LocalModel
import com.aifusion.app.core.LocalAnswerEngine
import com.aifusion.app.core.LocalLlamaEngine
import com.aifusion.app.core.LocalOcrEngine
import com.aifusion.app.core.NetworkGuardian
import com.aifusion.app.core.ParallelComputeScheduler
import com.aifusion.app.core.ModelManager
import com.aifusion.app.core.ModelChatRouter
import com.aifusion.app.core.ResourceManager
import com.aifusion.app.core.ResourceStatus
import com.aifusion.app.core.PerformanceMode
import com.aifusion.app.core.detectLanguage
import com.aifusion.app.core.localResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import android.util.Base64
import java.util.Locale

private enum class AppScreen {
    CHAT, HISTORY, SKILLS, RESEARCH, DEVICE, MODEL_MANAGER, SETTINGS
}

private data class GoogleAccountUi(
    val displayName: String,
    val email: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiFusionTheme {
                AiFusionApp()
            }
        }
    }
}

@Composable
private fun AiFusionApp() {
    // Open directly into the working chat; keep feature navigation inside the chat UI.
    AiFusionMainApp()
}

@Composable
private fun AiFusionLaunchScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF080D18)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.7f))
            NeuronFusionMark(modifier = Modifier.size(220.dp))
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "AI-FUSION",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEAF6FF)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "YOUR PERSONAL AI, REIMAGINED",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF8A9BFF),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "One assistant. A world of possibilities.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8F9ABD)
            )
            Spacer(modifier = Modifier.height(30.dp))
            androidx.compose.material3.LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(0.82f).height(3.dp).clip(RoundedCornerShape(8.dp)),
                color = Color(0xFF8A9BFF),
                trackColor = Color(0xFF272F50)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "STARTING NEURAL CORE",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A9BFF),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(0.8f))
            Text(
                text = "LOCAL-FIRST  •  SMART  •  READY",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8F9ABD)
            )
        }
    }
}

@Composable
private fun AiFusionLoadingScreen() {
    val transition = rememberInfiniteTransition(label = "loadingPulse")
    val progress by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingProgress"
    )
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF080D18)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.65f))
            NeuronFusionMark(modifier = Modifier.size(220.dp))
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Waking up neural network",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFEAF6FF)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connecting local intelligence…",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF8F9ABD)
            )
            Spacer(modifier = Modifier.height(30.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(8.dp)),
                color = Color(0xFF8A9BFF),
                trackColor = Color(0xFF272F50)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "INITIALIZING NEURAL CORE",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A9BFF),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Chat Core 3.0  •  Smart Device Engine",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8F9ABD)
            )
            Spacer(modifier = Modifier.weight(0.9f))
            Text(
                text = "LOCAL-FIRST  •  DEVICE OPTIMIZATION",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8F9ABD)
            )
        }
    }
}

@Composable
private fun NeuronFusionMark(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "neuronNetwork")
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neuronPulse"
    )
    val rotation by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neuronRotation"
    )
    Canvas(modifier = modifier) {
        val cyan = Color(0xFF8A9BFF)
        val violet = Color(0xFFC18BFF)
        val white = Color(0xFFF1F2FF)
        val c = Offset(size.width / 2f, size.height / 2f)
        val scale = size.minDimension / 220f
        val points = listOf(
            Offset(0.50f, 0.50f), Offset(0.28f, 0.24f), Offset(0.12f, 0.34f),
            Offset(0.72f, 0.22f), Offset(0.88f, 0.34f), Offset(0.84f, 0.56f),
            Offset(0.91f, 0.75f), Offset(0.69f, 0.80f), Offset(0.55f, 0.92f),
            Offset(0.28f, 0.76f), Offset(0.11f, 0.66f), Offset(0.16f, 0.50f),
            Offset(0.42f, 0.10f), Offset(0.42f, 0.90f), Offset(0.74f, 0.10f)
        ).map { Offset(it.x * size.width, it.y * size.height) }
        val links = listOf(
            0 to 1, 1 to 2, 0 to 3, 3 to 4, 0 to 5, 5 to 6,
            0 to 7, 7 to 8, 0 to 9, 9 to 10, 0 to 11, 1 to 12,
            3 to 14, 9 to 13, 11 to 2, 5 to 4, 7 to 9
        )
        rotate(rotation, pivot = c) {
            links.forEachIndexed { index, pair ->
                val linkColor = if (index % 4 == 0) violet else cyan
                drawLine(
                    color = linkColor.copy(alpha = if (index % 3 == 0) 0.9f else 0.52f),
                    start = points[pair.first],
                    end = points[pair.second],
                    strokeWidth = 1.5f * scale
                )
            }
            points.forEachIndexed { index, point ->
                val radius = if (index == 0) 5.2f else if (index % 3 == 0) 4f else 3f
                drawCircle(
                    color = (if (index % 4 == 0) violet else cyan).copy(alpha = pulse),
                    radius = radius * scale * (if (index == 0) 1.12f else 1f),
                    center = point
                )
                drawCircle(
                    color = white.copy(alpha = if (index == 0) 0.95f else 0.72f),
                    radius = (radius * 0.36f) * scale,
                    center = point
                )
            }
            drawCircle(
                color = cyan.copy(alpha = 0.13f * pulse),
                radius = 48f * scale,
                center = c,
                style = Stroke(width = 2f * scale)
            )
            drawCircle(
                color = cyan.copy(alpha = 0.25f * pulse),
                radius = 61f * scale,
                center = c,
                style = Stroke(width = 1.2f * scale)
            )
        }
        // Fusion spark in the center
        val spark = Path()
        spark.moveTo(c.x + 3f * scale, c.y - 24f * scale)
        spark.lineTo(c.x - 13f * scale, c.y + 1f * scale)
        spark.lineTo(c.x - 2f * scale, c.y + 1f * scale)
        spark.lineTo(c.x - 5f * scale, c.y + 22f * scale)
        spark.lineTo(c.x + 14f * scale, c.y - 7f * scale)
        spark.lineTo(c.x + 2f * scale, c.y - 7f * scale)
        spark.close()
        drawPath(spark, color = violet.copy(alpha = pulse))
        drawPath(spark, color = white.copy(alpha = 0.9f), style = Stroke(width = 1.1f * scale))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiFusionMainApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val store = remember(context) { LocalChatStore(context) }
    val modelStore = remember(context) { ModelManager(context) }
    val apiKeyStore = remember(context) { ApiKeyStore(context) }

    var models by remember { mutableStateOf(modelStore.list()) }
    var resourceStatus by remember { mutableStateOf(ResourceManager.status(context)) }
    var modelTestStatus by rememberSaveable { mutableStateOf("") }
    var mediaStatus by rememberSaveable { mutableStateOf("") }

    var screen by rememberSaveable { mutableStateOf(AppScreen.CHAT) }
    var sessionId by rememberSaveable {
        mutableStateOf(store.listSessions().firstOrNull()?.first ?: System.currentTimeMillis())
    }
    val initialSession = remember(sessionId) { store.load(sessionId) }
    var messages by remember(sessionId) {
        mutableStateOf(initialSession?.messages ?: emptyList())
    }
    var draft by rememberSaveable { mutableStateOf("") }
    var nextMessageId by rememberSaveable {
        mutableStateOf((messages.maxOfOrNull { it.id } ?: 0L) + 1L)
    }
    var generating by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var researchQuery by rememberSaveable { mutableStateOf("") }
    var showHardwareMonitor by rememberSaveable { mutableStateOf(false) }
    var showToolMenu by rememberSaveable { mutableStateOf(false) }
    var hardwareSample by remember { mutableStateOf(HardwareMonitor.read(context)) }
    var hardwareHistory by remember { mutableStateOf(listOf(hardwareSample)) }

    LaunchedEffect(Unit) {
        while (true) {
            val sample = HardwareMonitor.read(context)
            hardwareSample = sample
            hardwareHistory = (hardwareHistory + sample).takeLast(30)
            delay(1000L)
        }
    }

    var capabilities by remember {
        mutableStateOf(DeviceOptimizer.detect(context))
    }

    var showSettingsPassword by rememberSaveable { mutableStateOf(false) }
    var aiApiKey by remember { mutableStateOf(apiKeyStore.getApiKey()) }
    var aiModel by rememberSaveable { mutableStateOf(apiKeyStore.getModel()) }
    var aiSettingsStatus by rememberSaveable { mutableStateOf("") }
    val miniServerPrefs = remember(context) {
        context.getSharedPreferences("ai_fusion_mini_server", android.content.Context.MODE_PRIVATE)
    }
    var miniServerUrl by rememberSaveable {
        mutableStateOf(miniServerPrefs.getString("base_url", "").orEmpty())
    }
    var miniServerToken by rememberSaveable {
        mutableStateOf(
            apiKeyStore.getMiniServerToken().ifBlank {
                miniServerPrefs.getString("token", "").orEmpty()
            }
        )
    }
    var miniServerStatus by rememberSaveable { mutableStateOf("Belum diuji") }
    var testingMiniServer by remember { mutableStateOf(false) }
    // OAuth client ID is injected at build time from the GitHub Actions secret.
    // It is intentionally not editable or displayed in the app UI.
    val googlePrefs = remember(context) { context.getSharedPreferences("ai_fusion_google", android.content.Context.MODE_PRIVATE) }
    val buildClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
    var signingIn by remember { mutableStateOf(false) }
    var account by remember {
        mutableStateOf(
            googlePrefs.getString("email", null)?.let { email ->
                GoogleAccountUi(
                    displayName = googlePrefs.getString("display_name", "Google user").orEmpty().ifBlank { "Google user" },
                    email = email
                )
            }
        )
    }

    LaunchedEffect(Unit) {
        ResourceManager.enforceCacheLimit(context)
        resourceStatus = ResourceManager.status(context)

        // Migrate any previously stored plain-text Mini Server token to Keystore-backed encryption.
        val secureToken = apiKeyStore.getMiniServerToken()
        val legacyToken = miniServerPrefs.getString("token", "").orEmpty()
        if (secureToken.isNotBlank()) {
            miniServerToken = secureToken
            miniServerPrefs.edit().remove("token").apply()
        } else if (legacyToken.isNotBlank()) {
            if (apiKeyStore.setMiniServerToken(legacyToken)) {
                miniServerToken = legacyToken
                miniServerPrefs.edit().remove("token").apply()
            } else {
                miniServerToken = legacyToken
                miniServerStatus = "Token lama belum dapat dienkripsi. Tekan Simpan untuk cuba lagi."
            }
        }
    }

    var ttsReady by remember { mutableStateOf(false) }
    var pendingSpeech by remember { mutableStateOf<String?>(null) }
    val tts = remember(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { initStatus ->
            if (initStatus == TextToSpeech.SUCCESS) {
                val malayMalaysia = Locale.forLanguageTag("ms-MY")
                val malayLanguage = Locale("ms")
                val malaysiaStatus = engine.isLanguageAvailable(malayMalaysia)
                val languageStatus = engine.isLanguageAvailable(malayLanguage)
                val selectedLocale = when {
                    malaysiaStatus >= TextToSpeech.LANG_AVAILABLE -> malayMalaysia
                    languageStatus >= TextToSpeech.LANG_AVAILABLE -> malayLanguage
                    else -> Locale.ENGLISH
                }
                val selectedStatus = engine.setLanguage(selectedLocale)
                if (selectedStatus < TextToSpeech.LANG_AVAILABLE) {
                    engine.language = Locale.ENGLISH
                }
                engine.setSpeechRate(0.96f)
                engine.setPitch(1.0f)
                ttsReady = true
            }
        }
        engine
    }
    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun speak(text: String) {
        val spoken = text
            .replace(Regex("!\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)"), "$1")
            .replace(Regex("https?://\\S+"), " pautan ")
            .replace(Regex("(?m)^\\s{0,3}#{1,6}\\s*"), "")
            .replace(Regex("(?m)^\\s*[-*+]\\s+"), "")
            .replace(Regex("[*_~]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (spoken.isBlank()) return
        if (!ttsReady) {
            pendingSpeech = spoken
            return
        }
        val maxLength = TextToSpeech.getMaxSpeechInputLength().coerceAtMost(3500)
        val chunks = mutableListOf<String>()
        var remaining = spoken
        while (remaining.length > maxLength) {
            var cut = remaining.lastIndexOf(". ", maxLength)
            if (cut < maxLength / 2) cut = remaining.lastIndexOf(" ", maxLength)
            if (cut < 1) cut = maxLength
            val end = cut + if (remaining.getOrNull(cut) == '.') 1 else 0
            chunks += remaining.substring(0, end).trim()
            remaining = remaining.substring(end).trim()
        }
        if (remaining.isNotBlank()) chunks += remaining
        chunks.forEachIndexed { index, chunk ->
            tts.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "ai-fusion-response-$index"
            )
        }
    }

    LaunchedEffect(ttsReady, pendingSpeech) {
        val queued = pendingSpeech
        if (ttsReady && !queued.isNullOrBlank()) {
            pendingSpeech = null
            speak(queued)
        }
    }

    fun saveCurrent() {
        if (messages.isEmpty()) return
        val firstUser = messages.firstOrNull { it.fromUser }?.text.orEmpty()
        val title = firstUser.ifBlank { "AI-FUSION Chat" }.take(48)
        val session = ChatSession(sessionId, title, messages)
        store.save(session)
    }

    fun startNewChat() {
        saveCurrent()
        sessionId = System.currentTimeMillis()
        messages = emptyList()
        nextMessageId = 1L
        draft = ""
        status = ""
        screen = AppScreen.CHAT
    }

    fun openSession(session: ChatSession) {
        sessionId = session.id
        messages = session.messages
        nextMessageId = (messages.maxOfOrNull { it.id } ?: 0L) + 1L
        draft = ""
        status = ""
        screen = AppScreen.CHAT
    }

    fun openGeneratedFile(result: com.aifusion.app.core.MediaGenerationResult) {
        runCatching {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.aifusion.app.fileprovider",
                result.file
            )
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, result.mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        }.onFailure {
            mediaStatus = "Fail buka " + result.type + ": " + (it.message ?: "tiada aplikasi viewer")
        }
    }

    suspend fun sendMessage(text: String) {
        val clean = text.trim()
        if (clean.isBlank() || generating) return

        if (clean.startsWith("Research:", ignoreCase = true)) {
            researchQuery = clean.substringAfter(":").trim()
            screen = AppScreen.RESEARCH
            draft = ""
            return
        }

        val detectedTool = FusionToolRouter.detectTool(clean)

        if (detectedTool == FusionToolRouter.Tool.CAD_CODE) {
            generating = true
            status = "CAD Coding • building deterministic geometry…"
            val userId = nextMessageId++
            val aiId = nextMessageId++
            messages = messages + ChatMessage(userId, true, clean)
            messages = messages + ChatMessage(aiId, false, "CAD Code diterima. Membina geometry tepat secara lokal…")
            draft = ""
            runCatching {
                val file = CadCodeEngine.build(clean, java.io.File(context.cacheDir, "fusion3d"))
                context.startActivity(Intent(context, Fusion3DActivity::class.java).putExtra(Fusion3DActivity.EXTRA_MODEL_PATH, file.absolutePath))
                status = "CAD Coding • model siap • OBJ • local"
            }.onFailure {
                messages = messages.dropLast(1) + ChatMessage(aiId, false, "CAD Code error: ${it.message ?: "sintaks tidak sah"}")
                status = "CAD Code error"
            }
            generating = false
            saveCurrent()
            return
        }

        if (detectedTool == FusionToolRouter.Tool.THREE_D) {
            generating = true
            status = "3D Tool • building local mesh…"
            val userId = nextMessageId++
            val aiId = nextMessageId++
            messages = messages + ChatMessage(userId, true, clean)
            messages = messages + ChatMessage(
                aiId,
                false,
                "AI-FUSION 3D Tool: saya bina model OBJ asas secara lokal dan buka 3D Studio untuk rotate 360°."
            )
            draft = ""
            saveCurrent()

            runCatching {
                val file = FusionToolRouter.build3D(context, clean)
                context.startActivity(
                    Intent(context, Fusion3DActivity::class.java).putExtra(
                        Fusion3DActivity.EXTRA_MODEL_PATH,
                        file.absolutePath
                    )
                )
                status = "3D Tool • model siap • OBJ • local"
            }.onFailure {
                messages = messages.dropLast(1) + ChatMessage(
                    aiId,
                    false,
                    "3D Tool error: ${it.message ?: "model tidak dapat dibina"}"
                )
                status = "3D Tool error"
            }
            generating = false
            saveCurrent()
            return
        }

        val routedTool = detectedTool

        // Every non-chat tool is executed through the central 52-tool router.
        // Model/file-dependent tools report a truthful capability status when
        // their required input/runtime has not been supplied.
        if (routedTool != FusionToolRouter.Tool.CHAT &&
            routedTool != FusionToolRouter.Tool.IMAGE_CREATE &&
            routedTool != FusionToolRouter.Tool.VIDEO_CREATE) {
            generating = true
            val userId = nextMessageId++
            val aiId = nextMessageId++
            messages = messages + ChatMessage(userId, true, clean)
            messages = messages + ChatMessage(aiId, false, "")
            draft = ""
            status = FusionToolRouter.allTools().firstOrNull { it.tool == routedTool }?.name ?: "Tool"
            val result = FusionToolRouter.execute(context, routedTool, clean)
            messages = messages.dropLast(1) + ChatMessage(
                aiId,
                false,
                (if (result.success) "✓ " else "⚠ ") + result.message
            )
            if (routedTool == FusionToolRouter.Tool.TTS && result.success) {
                speak(clean.substringAfter(":", clean).trim())
            }
            generating = false
            saveCurrent()
            return
        }

        if (routedTool == FusionToolRouter.Tool.IMAGE_CREATE || routedTool == FusionToolRouter.Tool.VIDEO_CREATE) {
            generating = true
            val userId = nextMessageId++
            val aiId = nextMessageId++
            messages = messages + ChatMessage(userId, true, clean)
            messages = messages + ChatMessage(aiId, false, "")
            draft = ""
            status = if (routedTool == FusionToolRouter.Tool.IMAGE_CREATE) "Image Create • local/offline" else "Video Create • local/offline"
            runCatching {
                withContext(kotlinx.coroutines.Dispatchers.Default) {
                    if (routedTool == FusionToolRouter.Tool.IMAGE_CREATE) {
                        com.aifusion.app.core.LocalMediaGenerator.generateImage(context, clean)
                    } else {
                        com.aifusion.app.core.LocalMediaGenerator.generateVideo(context, clean, 3)
                    }
                }
            }.onSuccess { result ->
                val label = if (result.type == "PNG") "Local image siap" else "Local video siap"
                messages = messages.dropLast(1) + ChatMessage(
                    aiId,
                    false,
                    label + ": " + result.file.name + "\n\nIni ialah generator prosedural lokal; bukan model diffusion/video AI."
                )
                mediaStatus = result.type + " ready • " + result.file.name
                openGeneratedFile(result)
            }.onFailure { error ->
                messages = messages.dropLast(1) + ChatMessage(
                    aiId,
                    false,
                    "Create error: " + (error.message ?: "gagal menjana media")
                )
                mediaStatus = "Create error"
            }
            generating = false
            saveCurrent()
            return
        }

        generating = true
        status = "● ● ●"
        val userId = nextMessageId++
        val aiId = nextMessageId++
        messages = messages + ChatMessage(userId, true, clean)
        saveCurrent()
        messages = messages + ChatMessage(aiId, false, "")
        draft = ""

        val network = NetworkGuardian.state(context)
        val plan = ParallelComputeScheduler.plan(context, clean)
        val local = LocalAnswerEngine.answer(
            query = clean,
            capabilities = capabilities,
            network = network,
            modelCount = models.size
        )

        // Choose a real chat backend before deciding whether to call remote AI.
        // A configured API key must make ordinary chat work too, not only queries
        // containing words such as "search" or "latest". Prefer local GGUF when present;
        // use the remote model for all chat when no compatible local chat model exists.
        var onlineUsed = false
        var onlineFailed = false
        var remoteErrorMessage = ""
        val modelRoute = ModelChatRouter.route(models, capabilities)
        val localGgufModel = modelRoute.model?.takeIf { modelRoute.chatCapable }
        val asksForWeb = listOf("terkini", "latest", "current", "cari", "search", "web")
            .any { clean.contains(it, ignoreCase = true) }
        val shouldTryRemote = aiApiKey.isNotBlank() &&
            (localGgufModel == null || asksForWeb)

        // Real local inference when a GGUF model has been imported.
        // Current GGUF runtime is CPU/NEON on arm64-v8a; unsupported phones safely fall back.
        val localNeural = if (!shouldTryRemote && localGgufModel != null) {
            runCatching {
                LocalLlamaEngine.generate(
                    context = context,
                    modelUri = android.net.Uri.parse(localGgufModel.uri),
                    modelName = localGgufModel.name,
                    prompt = clean,
                    capabilities = capabilities
                )
            }.getOrNull()
        } else null

        val response = if (shouldTryRemote) {
            val conversationForModel = messages.filter { it.text.isNotBlank() }.takeLast(24)
            try {
                val answer = AiChatClient.generateReply(
                    apiKey = aiApiKey.trim(),
                    model = aiModel.trim(),
                    conversation = conversationForModel
                )
                onlineUsed = true
                answer
            } catch (error: Exception) {
                onlineFailed = true
                remoteErrorMessage = error.message.orEmpty()
                    .replace('\n', ' ')
                    .replace('\r', ' ')
                    .take(120)
                localNeural ?: local
            }
        } else {
            localNeural ?: local
        }

        // Send a lightweight authenticated handoff to Mini Server when configured.
        // Mini Server currently acknowledges receipt; model inference remains on this phone.
        val miniHelperStatus = if (miniServerUrl.isNotBlank() && miniServerToken.isNotBlank()) {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                MiniServerClient.handoffChat(miniServerUrl, miniServerToken, clean)
            }
        } else ""

        val words = response.split(" ")
        var partial = ""
        words.forEachIndexed { index, word ->
            partial = if (index == 0) word else partial + " " + word
            messages = messages.dropLast(1) + ChatMessage(aiId, false, partial)
            status = "● ● ●"
            delay(if (capabilities.mode == PerformanceMode.LOW_RAM) 55L else 35L)
        }

        generating = false
        val answerRoute = when {
            onlineUsed -> "AI Assistant • OpenAI API • " + aiModel.trim()
            onlineFailed && localNeural != null ->
                "OpenAI unavailable" + remoteErrorMessage.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty() + " • Local GGUF"
            onlineFailed ->
                "OpenAI unavailable" + remoteErrorMessage.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty() + " • Local fallback"
            localNeural != null -> "Local GGUF • CPU/NEON • " + localGgufModel?.name.orEmpty()
            else -> "Fallback • no compatible chat model • import GGUF or configure API key"
        }
        status = if (miniHelperStatus.startsWith("Mini Server menerima mesej")) {
            "$answerRoute • Mini Server connected"
        } else answerRoute
        saveCurrent()
    }

    fun generateLocalImage() {
        scope.launch {
            mediaStatus = "Generating local image…"
            val result = runCatching {
                withContext(kotlinx.coroutines.Dispatchers.Default) {
                    com.aifusion.app.core.LocalMediaGenerator.generateImage(
                        context,
                        messages.lastOrNull()?.text ?: "AI-FUSION concept"
                    )
                }
            }.getOrNull()
            mediaStatus = if (result != null) {
                "Image ready • ${result.file.name} • local/offline"
            } else {
                "Image generation failed."
            }
            result?.let { openGeneratedFile(it) }
        }
    }

    fun generateLocalVideo() {
        scope.launch {
            mediaStatus = "Generating local video…"
            val result = runCatching {
                withContext(kotlinx.coroutines.Dispatchers.Default) {
                    com.aifusion.app.core.LocalMediaGenerator.generateVideo(
                        context,
                        messages.lastOrNull()?.text ?: "AI-FUSION motion concept"
                    )
                }
            }.getOrNull()
            mediaStatus = if (result != null) {
                "Video ready • ${result.file.name} • local/offline"
            } else {
                "Video generation failed on this device."
            }
            result?.let { openGeneratedFile(it) }
        }
    }

    fun testModel(model: com.aifusion.app.core.LocalModel) {
        scope.launch {
            modelTestStatus = "Testing ${model.name}…"
            val uri = android.net.Uri.parse(model.uri)
            when {
                model.format.equals("ONNX", true) -> {
                    val result = runCatching {
                        com.aifusion.app.core.OnnxInferenceEngine.smokeTest(
                            context = context,
                            uri = uri,
                            modelName = model.name,
                            capabilities = capabilities
                        )
                    }.getOrNull()
                    modelTestStatus = if (result != null) {
                        "ONNX OK • ${result.accelerator} • input ${result.inputShape.contentToString()} • outputs ${result.outputCount}"
                    } else {
                        "ONNX test failed or model input is unsupported for generic smoke inference."
                    }
                }
                model.format.equals("TFLITE", true) -> {
                    val result = runCatching {
                        com.aifusion.app.core.LiteRtInferenceEngine.smokeTest(
                            context = context,
                            uri = uri,
                            modelName = model.name,
                            capabilities = capabilities
                        )
                    }.getOrNull()
                    modelTestStatus = if (result != null) {
                        "TFLite/LiteRT OK • ${result.accelerator} • input ${result.inputShape.contentToString()} • outputs ${result.outputCount}"
                    } else {
                        "TFLite/LiteRT test failed or model input is unsupported for generic smoke inference."
                    }
                }
                else -> modelTestStatus = "This model format has no direct smoke runner."
            }
        }
    }

    val voiceLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) {
                draft = spoken
            }
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ms-MY")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Cakap kepada AI-FUSION")
            }
            voiceLauncher.launch(intent)
        }
    }

    fun startVoice() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ms-MY")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Cakap kepada AI-FUSION")
            }
            voiceLauncher.launch(intent)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val ocrLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val idUser = nextMessageId++
                val idAi = nextMessageId++
                screen = AppScreen.CHAT
                messages = messages + ChatMessage(idUser, true, "Image OCR: " + (uri.lastPathSegment ?: "image"))
                messages = messages + ChatMessage(idAi, false, "")
                draft = ""
                generating = true
                status = "OCR • local"
                val text = runCatching { LocalOcrEngine.recognize(context, uri) }.getOrNull()
                val response = when {
                    !text.isNullOrBlank() -> "OCR berjaya (local):\n\n$text"
                    else -> "OCR tidak dapat membaca imej ini pada peranti."
                }
                messages = messages.dropLast(1) + ChatMessage(idAi, false, response)
                generating = false
                status = if (!text.isNullOrBlank()) "Vision/OCR • local" else "Vision/OCR unavailable"
                saveCurrent()
            }
        }
    }

    val modelPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val name = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            } ?: uri.lastPathSegment ?: "local-model"

            val sizeBytes = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else 0L
            } ?: 0L

            // Universal model import: ModelManager detects the actual supported file family.
            // Runtime support is shown in Model Manager; GGUF can run locally today.
            modelStore.add(uri, name, "", sizeBytes)
            models = modelStore.list()
        }
    }


    suspend fun signInGoogle() {
        signingIn = true
        status = "Opening Google Sign-In…"

        try {
            val activity = context as? Activity
                ?: error("Google Sign-In memerlukan Activity context")

            val clientId = buildClientId
            require(clientId.endsWith(".apps.googleusercontent.com")) {
                "Google Web Client ID tidak sah atau belum dimasukkan melalui GitHub Actions"
            }

            fun createNonce(): String {
                val bytes = ByteArray(32)
                SecureRandom().nextBytes(bytes)
                return Base64.encodeToString(
                    bytes,
                    Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
                )
            }

            val credentialManager = CredentialManager.create(activity)

            // Use the explicit Sign in with Google button flow as the primary
            // path. Android's current guidance specifically recommends this
            // flow for accounts that require re-authentication.
            fun buildRequest() = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetSignInWithGoogleOption.Builder(clientId)
                        .setNonce(createNonce())
                        .build()
                )
                .build()

            var credential = try {
                credentialManager.getCredential(
                    context = activity,
                    request = buildRequest()
                ).credential
            } catch (firstError: Exception) {
                // Clear stale Credential Manager state and retry once. This
                // handles cached/re-auth states without changing app data.
                runCatching {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                }
                status = "Refreshing Google account…"
                credentialManager.getCredential(
                    context = activity,
                    request = buildRequest()
                ).credential
            }

            if (
                credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                error("Google tidak memulangkan credential ID token yang sah")
            }

            val googleCredential = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (_: GoogleIdTokenParsingException) {
                error("ID token Google tidak dapat dibaca")
            }

            val email = googleCredential.email.orEmpty().trim()
            val idToken = googleCredential.idToken.orEmpty().trim()
            require(idToken.isNotBlank()) { "Google tidak memulangkan ID token" }
            require(email.isNotBlank()) { "Google tidak memulangkan email akaun" }

            val displayName = googleCredential.displayName.orEmpty().trim().ifBlank {
                email.substringBefore("@").ifBlank { "Google user" }
            }
            val uniqueId = googleCredential.uniqueId.orEmpty().trim()

            googlePrefs.edit()
                .putString("display_name", displayName)
                .putString("email", email)
                .putString("unique_id", uniqueId)
                .apply()

            account = GoogleAccountUi(
                displayName = displayName,
                email = email
            )
            status = "Google account connected"
        } catch (error: Exception) {
            val message = error.message.orEmpty()
                .replace("\\n", " ")
                .replace("\\r", " ")
                .trim()
                .take(260)

            status = when {
                message.contains("16") &&
                    message.contains("reauth", true) ->
                    "Google OAuth mismatch • daftar SHA-1 debug di Google Cloud"
                message.contains("10") &&
                    (message.contains("DEVELOPER_ERROR", true) ||
                     message.contains("developer", true) ||
                     message.contains("status code", true)) ->
                    "Google OAuth error (10) • semak package + SHA-1"
                message.contains("12501") ||
                    message.contains("canceled", true) ||
                    message.contains("cancelled", true) ->
                    "Google Sign-In dibatalkan"
                message.isBlank() ->
                    "Google Sign-In gagal — cuba lagi"
                else ->
                    "Google Sign-In error: $message"
            }
        } finally {
            signingIn = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(0.84f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("AI-FUSION", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    DrawerItem(Icons.Outlined.Home, "Home") {
                        screen = AppScreen.CHAT
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Outlined.Add, "New chat") {
                        startNewChat()
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Outlined.Description, "Chat history") {
                        screen = AppScreen.HISTORY
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Outlined.Public, "Research Core") {
                        screen = AppScreen.RESEARCH
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Outlined.ViewInAr, "3D Studio") {
                        context.startActivity(Intent(context, Fusion3DActivity::class.java))
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Outlined.AutoAwesome, "AI Skills") {
                        screen = AppScreen.SKILLS
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Outlined.Memory, "Smart Device") {
                        capabilities = DeviceOptimizer.detect(context)
                        screen = AppScreen.DEVICE
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Outlined.Memory, "Model Manager") {
                        models = modelStore.list()
                        screen = AppScreen.MODEL_MANAGER
                        scope.launch { drawerState.close() }
                    }
                    DrawerItem(Icons.Outlined.Settings, "Settings") {
                        screen = AppScreen.SETTINGS
                        scope.launch { drawerState.close() }
                    }

                    Spacer(Modifier.weight(1f))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "AI-FUSION Chat Core 3.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Local-first • lightweight • adaptive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                when (screen) {
                                    AppScreen.CHAT -> "AI-FUSION"
                                    AppScreen.HISTORY -> "Chat history"
                                    AppScreen.SKILLS -> "AI Skills"
                                    AppScreen.RESEARCH -> "Research Core"
                                    AppScreen.DEVICE -> "Smart Device"
                                    AppScreen.MODEL_MANAGER -> "Model Manager"
                                    AppScreen.SETTINGS -> "Settings"
                                },
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF1F2FF)
                            )
                            if (screen == AppScreen.CHAT) {
                                Text(
                                    "Assistant • Chat Core 3.0",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF909CBD)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (screen == AppScreen.CHAT) {
                                    showToolMenu = true
                                } else {
                                    screen = AppScreen.CHAT
                                }
                            }
                        ) {
                            Icon(
                                if (screen == AppScreen.CHAT) Icons.Outlined.AutoAwesome else Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = if (screen == AppScreen.CHAT) "AI tools" else "Back",
                                tint = Color(0xFFA7B3FF)
                            )
                        }
                    },
                    actions = {
                        if (screen == AppScreen.CHAT) {
                            IconButton(onClick = { screen = AppScreen.HISTORY }) {
                                Icon(Icons.Outlined.Description, contentDescription = "Chat history", tint = Color(0xFFADB8FF))
                            }
                            IconButton(onClick = { startNewChat() }) {
                                Icon(Icons.Outlined.Add, contentDescription = "New chat", tint = Color(0xFFADB8FF))
                            }
                        } else if (screen == AppScreen.DEVICE) {
                            IconButton(onClick = { showHardwareMonitor = true }) {
                                Icon(Icons.Outlined.Memory, contentDescription = "Live hardware monitor", tint = Color(0xFFADB8FF))
                            }
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF090D1A),
                        titleContentColor = Color(0xFFF1F2FF),
                        navigationIconContentColor = Color(0xFFA7B3FF),
                        actionIconContentColor = Color(0xFFA7B3FF)
                    )
                )
            }
        ) { padding ->
            when (screen) {
                AppScreen.CHAT -> ChatHome(
                    modifier = Modifier.padding(padding),
                    hardwareSample = hardwareSample,
                    messages = messages,
                    draft = draft,
                    generating = generating,
                    status = status,
                    onDraftChange = { draft = it },
                    onSend = { scope.launch { sendMessage(draft) } },
                    onVoice = { startVoice() },
                    onSpeak = { speak(it) },
                    onOpenResearch = {
                        researchQuery = it
                        screen = AppScreen.RESEARCH
                    },
                    onOpen3D = {
                        draft = "Bina model 3D: "
                    },
                    onTools = { showToolMenu = true }
                )

                AppScreen.HISTORY -> HistoryScreen(
                    modifier = Modifier.padding(padding),
                    store = store,
                    onOpenChat = { openSession(it) }
                )

                AppScreen.SKILLS -> SkillsScreen(
                    modifier = Modifier.padding(padding),
                    onUseSkill = { skill ->
                        if (skill.name.contains("Research", ignoreCase = true) ||
                            skill.name.contains("Web", ignoreCase = true)
                        ) {
                            researchQuery = ""
                            screen = AppScreen.RESEARCH
                        } else {
                            screen = AppScreen.CHAT
                            draft = skill.prompt
                        }
                    }
                )

                AppScreen.RESEARCH -> ResearchScreen(
                    modifier = Modifier.padding(padding),
                    initialQuery = researchQuery,
                    onBackToChat = {
                        researchQuery = it
                        screen = AppScreen.CHAT
                        draft = it
                    }
                )

                AppScreen.DEVICE -> DeviceScreen(
                    modifier = Modifier.padding(padding),
                    capabilities = capabilities,
                    selectedMode = DeviceOptimizer.selectedMode(context),
                    onSetMode = { mode ->
                        if (mode == null) {
                            DeviceOptimizer.clearMode(context)
                        } else {
                            DeviceOptimizer.setMode(context, mode)
                        }
                        capabilities = DeviceOptimizer.detect(context)
                    }
                )

                AppScreen.MODEL_MANAGER -> ModelManagerScreen(
                    modifier = Modifier.padding(padding),
                    models = models,
                    modelTier = capabilities.modelTier,
                    testStatus = modelTestStatus,
                    onImport = { modelPickerLauncher.launch(arrayOf("*/*")) },
                    onTest = { testModel(it) },
                    onDelete = {
                        modelStore.remove(it)
                        models = modelStore.list()
                    }
                )

                AppScreen.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    miniServerUrl = miniServerUrl,
                    miniServerToken = miniServerToken,
                    miniServerStatus = miniServerStatus,
                    testingMiniServer = testingMiniServer,
                    onMiniServerUrlChange = { miniServerUrl = it },
                    onMiniServerTokenChange = { miniServerToken = it },
                    onSaveMiniServer = {
                        miniServerUrl = miniServerUrl.trim().trimEnd('/')
                        miniServerToken = miniServerToken.trim()
                        val tokenSaved = apiKeyStore.setMiniServerToken(miniServerToken)
                        miniServerPrefs.edit()
                            .putString("base_url", miniServerUrl)
                            .remove("token")
                            .apply()
                        miniServerStatus = if (tokenSaved) {
                            "Tetapan Mini Server disimpan. Token dienkripsi pada telefon ini."
                        } else {
                            "Gagal menyimpan token dengan selamat. Semak ruang storan dan cuba lagi."
                        }
                    },
                    onTestMiniServer = {
                        scope.launch {
                            testingMiniServer = true
                            miniServerStatus = "Menguji API + WebSocket realtime…"
                            miniServerStatus = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                MiniServerClient.test(miniServerUrl.trim().trimEnd('/'), miniServerToken.trim())
                            }
                            testingMiniServer = false
                        }
                    },
                    aiApiKey = aiApiKey,
                    aiModel = aiModel,
                    aiSettingsStatus = aiSettingsStatus,
                    onAiApiKeyChange = { aiApiKey = it },
                    onAiModelChange = { aiModel = it },
                    onSaveAiSettings = {
                        val keySaved = apiKeyStore.setApiKey(aiApiKey.trim())
                        apiKeyStore.setModel(aiModel.trim())
                        aiApiKey = apiKeyStore.getApiKey()
                        aiModel = apiKeyStore.getModel()
                        aiSettingsStatus = when {
                            !keySaved -> "Gagal menyimpan API key dengan selamat. Cuba lagi."
                            aiApiKey.isBlank() -> "API key dikosongkan. Chat menggunakan mod fallback setempat."
                            else -> "Tetapan disimpan • Model: $aiModel"
                        }
                    },
                    onClearAiKey = {
                        val removed = apiKeyStore.setApiKey("")
                        aiApiKey = ""
                        aiSettingsStatus = if (removed) {
                            "API key dibuang. Chat menggunakan mod fallback setempat."
                        } else {
                            "Gagal membuang API key daripada storan. Cuba lagi."
                        }
                    },
                    account = account,
                    googleConfigured = buildClientId.isNotBlank() && buildClientId.contains(".apps.googleusercontent.com"),
                    resourceStatus = resourceStatus,
                    onClearCache = {
                        ResourceManager.clearTemporaryCache(context)
                        resourceStatus = ResourceManager.status(context)
                    },
                    onSignIn = {
                        scope.launch { signInGoogle() }
                    },
                    onSignOut = {
                        scope.launch {
                            runCatching {
                                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                            }
                            googlePrefs.edit().clear().apply()
                            account = null
                            status = "Signed out • Local chats remain on this device"
                        }
                    }
                )
            }

            if (showHardwareMonitor) {
                HardwareMonitorPanel(
                    sample = hardwareSample,
                    history = hardwareHistory,
                    onClose = { showHardwareMonitor = false }
                )
            }

            if (showToolMenu) {
                AlertDialog(
                    onDismissRequest = { showToolMenu = false },
                    title = { Text("AI-FUSION Tools") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = {
                                showToolMenu = false
                                researchQuery = ""
                                screen = AppScreen.RESEARCH
                            }) { Text("Research / Fact Check") }
                            TextButton(onClick = {
                                showToolMenu = false
                                screen = AppScreen.MODEL_MANAGER
                                models = modelStore.list()
                            }) { Text("Local Model Manager") }
                            TextButton(onClick = {
                                showToolMenu = false
                                capabilities = DeviceOptimizer.detect(context)
                                screen = AppScreen.DEVICE
                            }) { Text("Smart Device / CPU-GPU-NPU") }
                            TextButton(onClick = {
                                showToolMenu = false
                                context.startActivity(Intent(context, Fusion3DActivity::class.java))
                            }) { Text("3D Studio") }
                            TextButton(onClick = {
                                showToolMenu = false
                                ocrLauncher.launch(arrayOf("image/*"))
                            }) { Text("Image / OCR") }
                            TextButton(onClick = {
                                showToolMenu = false
                                generateLocalImage()
                            }) { Text("Create Image • Local") }
                            TextButton(onClick = {
                                showToolMenu = false
                                generateLocalVideo()
                            }) { Text("Create Video • Local") }
                            TextButton(onClick = {
                                showToolMenu = false
                                screen = AppScreen.HISTORY
                            }) { Text("Chat history") }
                            TextButton(onClick = {
                                showToolMenu = false
                                screen = AppScreen.SETTINGS
                            }) { Text("Settings") }
                            TextButton(onClick = {
                                showToolMenu = false
                                screen = AppScreen.SKILLS
                            }) { Text("All AI Skills") }
                            if (mediaStatus.isNotBlank()) {
                                Text(mediaStatus, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showToolMenu = false }) { Text("Close") }
                    }
                )
            }
        }
    }
}
@Composable
private fun LoginScreen(
    status: String,
    signingIn: Boolean,
    onSignIn: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(82.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                "AI-FUSION",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in with Google untuk mula menggunakan AI Assistant.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSignIn,
                enabled = !signingIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (signingIn) "Signing in…" else "Continue with Google")
            }

            if (status.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "Chat, history dan data AI disimpan secara lokal pada telefon.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HardwareMonitorPanel(
    sample: HardwareSample,
    history: List<HardwareSample>,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Live Hardware", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Local device telemetry • 1s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close monitor")
                }
            }

            HardwareMetric("CPU", HardwareMonitor.percentText(sample.cpuPercent), history.map { it.cpuPercent })
            HardwareMetric("RAM", "${sample.ramUsedMb} / ${sample.ramTotalMb} MB • ${HardwareMonitor.percentText(sample.ramPercent)}", history.map { it.ramPercent })
            HardwareMetric("GPU", HardwareMonitor.percentText(sample.gpuPercent), history.mapNotNull { it.gpuPercent })
            HardwareMetric("NPU", HardwareMonitor.percentText(sample.npuPercent), history.mapNotNull { it.npuPercent })

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("AI compute route", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when {
                            sample.npuPercent != null -> "LOCAL AI → NPU"
                            sample.gpuPercent != null -> "LOCAL AI → GPU / CPU"
                            else -> "LOCAL AI → CPU"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "N/A bermaksud Android/chipset tidak mendedahkan penggunaan masa nyata untuk sensor tersebut.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HardwareMetric(
    title: String,
    value: String,
    points: List<Float>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(value, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(8.dp))
            if (points.isEmpty()) {
                Text("Telemetry unavailable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val lineColor = MaterialTheme.colorScheme.primary
                Canvas(Modifier.fillMaxWidth().height(52.dp)) {
                    val max = points.size.coerceAtLeast(2)
                    val step = size.width / (max - 1).toFloat()
                    for (i in 0 until points.size - 1) {
                        val x1 = i * step
                        val x2 = (i + 1) * step
                        val y1 = size.height - (points[i].coerceIn(0f, 100f) / 100f * size.height)
                        val y2 = size.height - (points[i + 1].coerceIn(0f, 100f) / 100f * size.height)
                        drawLine(
                            color = lineColor,
                            start = androidx.compose.ui.geometry.Offset(x1, y1),
                            end = androidx.compose.ui.geometry.Offset(x2, y2),
                            strokeWidth = 4f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.size(14.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ChatHome(
    modifier: Modifier,
    hardwareSample: HardwareSample,
    messages: List<ChatMessage>,
    draft: String,
    generating: Boolean,
    status: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit,
    onSpeak: (String) -> Unit,
    onOpenResearch: (String) -> Unit,
    onOpen3D: () -> Unit,
    onTools: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080B17),
                        Color(0xFF0D1325),
                        Color(0xFF090D1A)
                    )
                )
            )
    ) {
        HardwareStatusStrip(sample = hardwareSample)
        if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                EmptyState(onOpenResearch, onOpen3D)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message, onSpeak)
                }
                if (generating) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(34.dp),
                                shape = RoundedCornerShape(13.dp),
                                color = Color(0xFF20284A),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6377E8).copy(alpha = 0.45f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp),
                                        tint = Color(0xFFA9B5FF)
                                    )
                                }
                            }
                            Spacer(Modifier.size(10.dp))
                            Column {
                                Text(
                                    "AI-FUSION sedang berfikir",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFC4CCF4)
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    status.ifBlank { "Memproses permintaan…" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF929DBD)
                                )
                            }
                        }
                    }
                }
            }
        }

        Composer(
            draft = draft,
            onDraftChange = onDraftChange,
            onSend = onSend,
            onVoice = onVoice,
            onTools = onTools,
            modifier = Modifier.fillMaxWidth().imePadding()
        )
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun HardwareStatusStrip(sample: HardwareSample) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        @Composable
        fun Metric(label: String, value: Int, tint: Color) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF11192C).copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.34f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier.size(5.dp).clip(CircleShape).background(tint)
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9BA6C8)
                    )
                    Text(
                        "$value%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE9ECFF)
                    )
                }
            }
        }

        Metric("CPU", sample.cpuPercent.coerceIn(0f, 100f).toInt(), Color(0xFF8EA0FF))
        sample.gpuPercent?.takeIf { it > 0f }?.let {
            Metric("GPU", it.coerceIn(0f, 100f).toInt(), Color(0xFFC18BFF))
        }
        sample.npuPercent?.takeIf { it > 0f }?.let {
            Metric("NPU", it.coerceIn(0f, 100f).toInt(), Color(0xFF72D9D0))
        }
        Spacer(Modifier.weight(1f))
        Text(
            "LOCAL CORE",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF9CA9FF),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyState(
    onOpenResearch: (String) -> Unit,
    onOpen3D: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFF11182C).copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6B7FF0).copy(alpha = 0.34f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NeuronFusionMark(modifier = Modifier.size(94.dp))
                Spacer(Modifier.height(2.dp))
                Text(
                    "AI-FUSION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA8B4FF)
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    "Apa yang ingin kita selesaikan hari ini?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F2FF),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tanya, kaji sumber atau cipta sesuatu. AI-FUSION sedia membantu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA5AEC9),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickCard(Icons.Outlined.Public, "Research", { onOpenResearch("") }, Modifier.weight(1f))
            QuickCard(Icons.Outlined.AutoAwesome, "Create", { onOpenResearch("Create: ") }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickCard(Icons.Outlined.ViewInAr, "3D Design", onOpen3D, Modifier.weight(1f))
            QuickCard(Icons.Outlined.AttachFile, "Files & Vision", { onOpenResearch("Files: ") }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Tekan + untuk semua tools dan model tempatan",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF818CAB),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun QuickCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(66.dp),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141D34).copy(alpha = 0.96f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF485B9C).copy(alpha = 0.48f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF26305A),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFA9B6FF))
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE4E8FF)
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onSpeak: (String) -> Unit) {
    if (message.fromUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 7.dp),
                color = Color(0xFF293A83),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7589FF).copy(alpha = 0.42f))
            ) {
                Text(
                    message.text.ifBlank { "…" },
                    color = Color(0xFFF3F4FF),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(13.dp),
                color = Color(0xFF20284A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6579E8).copy(alpha = 0.42f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = "AI-FUSION",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFADB8FF)
                    )
                }
            }
            Spacer(Modifier.size(10.dp))
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(topStart = 7.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp),
                color = Color(0xFF11192B).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF344269).copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(start = 15.dp, end = 11.dp, top = 13.dp, bottom = 5.dp)) {
                    Text(
                        "AI-FUSION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFABB7FF)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        message.text.ifBlank { "…" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFE6E9F7)
                    )
                    if (message.text.isNotBlank()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(
                                onClick = { onSpeak(message.text) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Speaker,
                                    contentDescription = "Read answer aloud",
                                    modifier = Modifier.size(17.dp),
                                    tint = Color(0xFF9EABF5)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit,
    onTools: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        shape = RoundedCornerShape(27.dp),
        color = Color(0xFF11182A).copy(alpha = 0.98f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5868B6).copy(alpha = 0.55f)),
        tonalElevation = 0.dp,
        shadowElevation = 7.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFA4B0FF))
                    Text(
                        "AI-FUSION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFADB7FF)
                    )
                }
                Text(
                    "LOCAL-FIRST",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7F8DB6)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(onClick = onTools, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = "All tools", tint = Color(0xFFABB7FF))
                }

                androidx.compose.foundation.text.BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f).padding(horizontal = 7.dp, vertical = 11.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFF1F2FF)),
                    singleLine = false,
                    maxLines = 6,
                    decorationBox = { inner ->
                        Box {
                            if (draft.isBlank()) {
                                Text(
                                    "Mesej AI-FUSION…",
                                    color = Color(0xFF828DAC)
                                )
                            }
                            inner()
                        }
                    }
                )

                if (draft.isBlank()) {
                    IconButton(onClick = onVoice, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Outlined.Mic, contentDescription = "Voice input", tint = Color(0xFFABB7FF))
                    }
                } else {
                    Surface(
                        modifier = Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onSend),
                        shape = CircleShape,
                        color = Color(0xFF5B6CF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA0A9FF).copy(alpha = 0.55f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    miniServerUrl: String,
    miniServerToken: String,
    miniServerStatus: String,
    testingMiniServer: Boolean,
    onMiniServerUrlChange: (String) -> Unit,
    onMiniServerTokenChange: (String) -> Unit,
    onSaveMiniServer: () -> Unit,
    onTestMiniServer: () -> Unit,
    aiApiKey: String,
    aiModel: String,
    aiSettingsStatus: String,
    onAiApiKeyChange: (String) -> Unit,
    onAiModelChange: (String) -> Unit,
    onSaveAiSettings: () -> Unit,
    onClearAiKey: () -> Unit,
    account: GoogleAccountUi?,
    googleConfigured: Boolean,
    resourceStatus: ResourceStatus,
    onClearCache: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("AI-FUSION Mini Server", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Sambungkan telefon ini ke Mini Server melalui hotspot/Wi-Fi yang sama. Masukkan alamat API dan token yang dipaparkan dalam aplikasi Mini Server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = miniServerUrl,
                    onValueChange = onMiniServerUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Alamat API Mini Server") },
                    placeholder = { Text("http://192.168.x.x:8765") }
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = miniServerToken,
                    onValueChange = onMiniServerTokenChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text("Token API Mini Server") },
                    placeholder = { Text("Salin daripada aplikasi OPPO") }
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveMiniServer) { Text("Simpan") }
                    TextButton(onClick = onTestMiniServer, enabled = !testingMiniServer) {
                        Text(if (testingMiniServer) "Menguji…" else "Uji sambungan")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    miniServerStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sambungan ini menguji API tempatan sahaja; ia belum menjana jawapan AI melalui OPPO. Gunakan hanya pada hotspot/Wi-Fi yang dipercayai.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("AI Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (aiApiKey.isBlank()) {
                        "Offline fallback aktif. Tambah OpenAI API key untuk aktifkan chat AI sebenar."
                    } else {
                        "Online AI aktif. API key disimpan secara terenkripsi pada peranti."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = aiApiKey,
                    onValueChange = onAiApiKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text("OpenAI API Key") },
                    placeholder = { Text("sk-...") }
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = aiModel,
                    onValueChange = onAiModelChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Model") },
                    placeholder = { Text("gpt-6-luna") }
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveAiSettings) {
                        Text("Save AI")
                    }
                    TextButton(onClick = onClearAiKey) {
                        Text("Remove key")
                    }
                }
                if (aiSettingsStatus.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        aiSettingsStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Jangan masukkan key orang lain atau commit API key ke repository.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Google Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    account?.email ?: "Not connected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onSignIn,
                    enabled = googleConfigured
                ) {
                    Text(if (account == null) "Continue with Google" else "Reconnect Google")
                }
                AnimatedVisibility(account != null) {
                    Column {
                        Text(
                            "Google account connected • identity only, chats stay local",
                            modifier = Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onSignOut) {
                            Text("Sign out")
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Compression & RAM Manager", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Available RAM: " + resourceStatus.availableRamMb + " MB",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "App cache: " + resourceStatus.appCacheMb + " MB",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onClearCache) {
                    Text("Clear temporary cache")
                }
            }
        }

        Text(
            "Storage: chat history is GZIP-compressed and kept on-device. Network is reserved for web research and future backend integration.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

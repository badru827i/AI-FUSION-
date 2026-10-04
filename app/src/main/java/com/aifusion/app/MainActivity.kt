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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.GetCredentialRequest
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
import com.aifusion.app.core.ResourceManager
import com.aifusion.app.core.ResourceStatus
import com.aifusion.app.core.PerformanceMode
import com.aifusion.app.core.detectLanguage
import com.aifusion.app.core.localResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiFusionApp() {
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

    LaunchedEffect(showHardwareMonitor) {
        if (showHardwareMonitor) {
            while (true) {
                val sample = HardwareMonitor.read(context)
                hardwareSample = sample
                hardwareHistory = (hardwareHistory + sample).takeLast(30)
                delay(1000L)
            }
        }
    }

    var capabilities by remember {
        mutableStateOf(DeviceOptimizer.detect(context))
    }

    var showSettingsPassword by rememberSaveable { mutableStateOf(false) }
    var aiApiKey by remember { mutableStateOf(apiKeyStore.getApiKey()) }
    var aiModel by rememberSaveable { mutableStateOf(apiKeyStore.getModel()) }
    // OAuth client ID is injected at build time from the GitHub Actions secret.
    // It is intentionally not editable or displayed in the app UI.
    val googlePrefs = remember(context) { context.getSharedPreferences("ai_fusion_google", android.content.Context.MODE_PRIVATE) }
    val buildClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
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
    }

    val tts = remember(context) {
        TextToSpeech(context) { }
    }
    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun speak(text: String) {
        val languageResult = tts.setLanguage(Locale.forLanguageTag("ms-MY"))
        val bm = languageResult == TextToSpeech.LANG_AVAILABLE ||
            languageResult == TextToSpeech.LANG_COUNTRY_AVAILABLE
        if (!bm) {
            tts.language = Locale.ENGLISH
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ai-fusion-response")
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

        if (FusionToolRouter.detectTool(clean) == FusionToolRouter.Tool.THREE_D) {
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

        val routedTool = FusionToolRouter.detectTool(clean)
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

        // Local-first: answer on-device first. Remote AI is only an optional
        // fallback when the user has configured an API key and the local
        // layer cannot provide a useful answer.
        var onlineUsed = false
        var onlineFailed = false
        val shouldTryRemote = aiApiKey.isNotBlank() &&
            (clean.contains("terkini", true) ||
             clean.contains("latest", true) ||
             clean.contains("current", true) ||
             clean.contains("cari", true) ||
             clean.contains("search", true) ||
             clean.contains("web", true))

        // Real local inference when a GGUF model has been imported.
        // Current GGUF runtime is CPU/NEON on arm64-v8a; unsupported phones safely fall back.
        val localGgufModel = models.firstOrNull { it.format.equals("GGUF", ignoreCase = true) }
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
        } else {
            null
        }

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
            } catch (_: Exception) {
                onlineFailed = true
                localNeural ?: local
            }
        } else {
            localNeural ?: local
        }

        val words = response.split(" ")
        var partial = ""
        words.forEachIndexed { index, word ->
            partial = if (index == 0) word else partial + " " + word
            messages = messages.dropLast(1) + ChatMessage(aiId, false, partial)
            status = "● ● ●"
            delay(if (capabilities.mode == PerformanceMode.LOW_RAM) 55L else 35L)
        }

        generating = false
        status = when {
            onlineUsed -> "AI Assistant • web/remote • " + aiModel.trim()
            onlineFailed && localNeural != null -> "Remote unavailable • Local GGUF"
            onlineFailed -> "Remote unavailable • Local fallback"
            localNeural != null -> "Local GGUF • CPU/NEON • " + localGgufModel?.name.orEmpty()
            else -> "Local AI • " + plan.units.joinToString("+")
        }
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
                                fontWeight = FontWeight.SemiBold
                            )
                            if (screen == AppScreen.CHAT) {
                                Text(
                                    "Assistant",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (screen == AppScreen.CHAT) {
                                    scope.launch { drawerState.open() }
                                } else {
                                    screen = AppScreen.CHAT
                                }
                            }
                        ) {
                            Icon(
                                if (screen == AppScreen.CHAT) {
                                    Icons.Outlined.Menu
                                } else {
                                    Icons.AutoMirrored.Outlined.ArrowBack
                                },
                                contentDescription = if (screen == AppScreen.CHAT) "Menu" else "Back"
                            )
                        }
                    },
                    actions = {
                        if (screen == AppScreen.CHAT) {
                            IconButton(onClick = { startNewChat() }) {
                                Icon(Icons.Outlined.Add, contentDescription = "New chat")
                            }
                        } else if (screen == AppScreen.DEVICE) {
                            IconButton(onClick = { showHardwareMonitor = true }) {
                                Icon(Icons.Outlined.Memory, contentDescription = "Live hardware monitor")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            when (screen) {
                AppScreen.CHAT -> ChatHome(
                    modifier = Modifier.padding(padding),
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
                    aiApiKey = aiApiKey,
                    aiModel = aiModel,
                    onAiApiKeyChange = { aiApiKey = it },
                    onAiModelChange = { aiModel = it },
                    onSaveAiSettings = {
                        apiKeyStore.setApiKey(aiApiKey.trim())
                        apiKeyStore.setModel(aiModel.trim())
                        aiApiKey = apiKeyStore.getApiKey()
                        aiModel = apiKeyStore.getModel()
                    },
                    onClearAiKey = {
                        apiKeyStore.setApiKey("")
                        aiApiKey = ""
                    },
                    account = account,
                    resourceStatus = resourceStatus,
                    onClearCache = {
                        ResourceManager.clearTemporaryCache(context)
                        resourceStatus = ResourceManager.status(context)
                    },
                    onSignIn = {
                        val id = buildClientId
                        if (id.isNotBlank() && id.contains(".apps.googleusercontent.com")) {
                            scope.launch {
                                try {
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setServerClientId(id)
                                        .setFilterByAuthorizedAccounts(false)
                                        .setAutoSelectEnabled(true)
                                        .setNonce(java.util.UUID.randomUUID().toString())
                                        .build()

                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    val result = CredentialManager.create(context).getCredential(
                                        request = request,
                                        context = context
                                    )
                                    val credential = result.credential

                                    if (
                                        credential is CustomCredential &&
                                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    ) {
                                        val googleCredential = try {
                                            GoogleIdTokenCredential.createFrom(credential.data)
                                        } catch (_: GoogleIdTokenParsingException) {
                                            null
                                        }

                                        googleCredential?.let {
                                            val email = it.email.orEmpty().trim()
                                            if (email.isBlank()) error("Google tidak memulangkan email akaun")
                                            val displayName = it.displayName.orEmpty().trim().ifBlank {
                                                email.substringBefore("@").ifBlank { "Google user" }
                                            }
                                            val uniqueId = it.uniqueId.orEmpty().trim()
                                            googlePrefs.edit()
                                                .putString("display_name", displayName)
                                                .putString("email", email)
                                                .putString("unique_id", uniqueId)
                                                .apply()
                                            account = GoogleAccountUi(
                                                displayName = displayName,
                                                email = email
                                            )
                                            status = "Signed in • Google account active (local)"
                                        }
                                    }
                                } catch (error: Exception) {
                                    val message = error.message
                                        ?.replace("\\n", " ")
                                        ?.take(180)
                                        .orEmpty()
                                    status = if (message.isBlank()) {
                                        "Google/Firebase sign-in failed"
                                    } else {
                                        "Sign-in error: $message"
                                    }
                                }
                            }
                        }
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
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (messages.isEmpty()) {
            EmptyState(onOpenResearch, onOpen3D)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message, onSpeak)
                }
                if (generating) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Text(
                                status.ifBlank { "AI sedang berfikir…" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!generating && messages.isNotEmpty() && status.isNotBlank()) {
            Text(
                status,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
private fun EmptyState(
    onOpenResearch: (String) -> Unit,
    onOpen3D: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(70.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Hi, saya AI-FUSION",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Tanya apa sahaja. Saya boleh bantu fikir, cari, bina dan semak.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(22.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickCard(
                Icons.Outlined.Public,
                "Research",
                { onOpenResearch("") },
                Modifier.weight(1f)
            )
            QuickCard(
                Icons.Outlined.AutoAwesome,
                "Create",
                { onOpenResearch("Create: ") },
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickCard(
                Icons.Outlined.ViewInAr,
                "3D Design",
                onOpen3D,
                Modifier.weight(1f)
            )
            QuickCard(
                Icons.Outlined.AttachFile,
                "Files & Vision",
                { onOpenResearch("Files: ") },
                Modifier.weight(1f)
            )
        }
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
        modifier = modifier.height(74.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onSpeak: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.fromUser) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
        }

        Column(
            horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                modifier = if (message.fromUser) Modifier.fillMaxWidth(0.88f) else Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (message.fromUser) 20.dp else 6.dp,
                    bottomEnd = if (message.fromUser) 6.dp else 20.dp
                ),
                color = if (message.fromUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                }
            ) {
                Text(
                    text = message.text.ifBlank { "…" },
                    color = if (message.fromUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (!message.fromUser && message.text.isNotBlank()) {
                IconButton(
                    onClick = { onSpeak(message.text) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Outlined.Speaker,
                        contentDescription = "Read answer",
                        modifier = Modifier.size(17.dp)
                    )
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
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = 5.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onTools) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Tools",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            androidx.compose.foundation.text.BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 6.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = false,
                maxLines = 5,
                decorationBox = { inner ->
                    Box {
                        if (draft.isBlank()) {
                            Text(
                                "Tanya AI-FUSION…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )

            IconButton(onClick = onVoice) {
                Icon(
                    Icons.Outlined.Mic,
                    contentDescription = "Voice",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                modifier = Modifier.size(42.dp).clip(CircleShape).clickable(
                    enabled = draft.isNotBlank(),
                    onClick = onSend
                ),
                shape = CircleShape,
                color = if (draft.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(18.dp),
                        tint = if (draft.isNotBlank()) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    aiApiKey: String,
    aiModel: String,
    onAiApiKeyChange: (String) -> Unit,
    onAiModelChange: (String) -> Unit,
    onSaveAiSettings: () -> Unit,
    onClearAiKey: () -> Unit,
    account: GoogleAccountUi?,
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
                    enabled = savedClientId.contains(".apps.googleusercontent.com")
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

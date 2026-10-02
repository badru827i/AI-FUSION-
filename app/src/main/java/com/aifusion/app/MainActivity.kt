package com.aifusion.app

import android.Manifest
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
    val googlePrefs = remember(context) { context.getSharedPreferences("ai_fusion_google", android.content.Context.MODE_PRIVATE) }
    var clientId by rememberSaveable { mutableStateOf(googlePrefs.getString("client_id", "").orEmpty()) }
    var savedClientId by rememberSaveable { mutableStateOf(googlePrefs.getString("client_id", "").orEmpty()) }
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
        val languageResult = tts.setLanguage(Locale("ms", "MY"))
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
                        Fusion3DActivity.EXTRA_MODEL_URI,
                        android.net.Uri.fromFile(file).toString()
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

        generating = true
        status = "● ● ●"
        val userId = nextMessageId++
        val aiId = nextMessageId++
        messages = messages + ChatMessage(userId, true, clean)
        saveCurrent()
        messages = messages + ChatMessage(aiId, false, "")
        draft = ""

        var onlineUsed = false
        var onlineFailed = false
        val conversationForModel = messages
            .filter { it.text.isNotBlank() }
            .takeLast(24)

        val response = if (aiApiKey.isNotBlank()) {
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
                localResponse(
                    query = clean,
                    capabilities = capabilities,
                    language = detectLanguage(clean)
                )
            }
        } else {
            localResponse(
                query = clean,
                capabilities = capabilities,
                language = detectLanguage(clean)
            )
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
            onlineUsed -> "AI Assistant • " + aiModel.trim()
            onlineFailed -> "AI connection failed • Local fallback"
            else -> "Local Chat Core • Add an API key in Settings for online AI"
        }
        saveCurrent()
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

            val format = name.substringAfterLast('.', "unknown").lowercase()
            if (format == "onnx" || format == "tflite" || format == "lite") {
                modelStore.add(uri, name, format, sizeBytes)
                models = modelStore.list()
            }
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
                    Divider()
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
                        Text(
                            when (screen) {
                                AppScreen.CHAT -> "AI-FUSION"
                                AppScreen.HISTORY -> "Chat history"
                                AppScreen.SKILLS -> "AI Skills"
                                AppScreen.RESEARCH -> "Research Core"
                                AppScreen.DEVICE -> "Smart Device"
                                AppScreen.MODEL_MANAGER -> "Model Manager"
                                AppScreen.SETTINGS -> "Settings"
                                else -> "AI-FUSION"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (screen == AppScreen.CHAT) {
                                scope.launch { drawerState.open() }
                            } else {
                                screen = AppScreen.CHAT
                            }
                        }) {
                            Icon(
                                if (screen == AppScreen.CHAT) Icons.Outlined.Menu else Icons.Outlined.ArrowBack,
                                contentDescription = if (screen == AppScreen.CHAT) "Menu" else "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showHardwareMonitor = true }) {
                            Icon(Icons.Outlined.Memory, contentDescription = "Live hardware monitor")
                        }
                        if (screen == AppScreen.CHAT) {
                            IconButton(onClick = { startNewChat() }) {
                                Icon(Icons.Outlined.Add, contentDescription = "New chat")
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
                    }
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
                    onImport = { modelPickerLauncher.launch(arrayOf("*/*")) },
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
                    clientId = clientId,
                    savedClientId = savedClientId,
                    account = account,
                    showClientId = showSettingsPassword,
                    onShowClientId = { showSettingsPassword = !showSettingsPassword },
                    onClientIdChange = { clientId = it },
                    onSaveClientId = {
                        savedClientId = clientId.trim()
                        googlePrefs.edit().putString("client_id", savedClientId).apply()
                    },
                    resourceStatus = resourceStatus,
                    onClearCache = {
                        ResourceManager.clearTemporaryCache(context)
                        resourceStatus = ResourceManager.status(context)
                    },
                    onSignIn = {
                        val id = savedClientId.trim()
                        if (id.isNotBlank() && id.contains(".apps.googleusercontent.com")) {
                            scope.launch {
                                try {
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setServerClientId(id)
                                        .setFilterByAuthorizedAccounts(false)
                                        .setAutoSelectEnabled(true)
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

            AnimatedVisibility(
                visible = showHardwareMonitor,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it })
            ) {
                HardwareMonitorPanel(
                    sample = hardwareSample,
                    history = hardwareHistory,
                    onClose = { showHardwareMonitor = false }
                )
            }
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
    onOpen3D: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(modifier.fillMaxSize()) {
        if (messages.isEmpty()) {
            EmptyState(onOpenResearch, onOpen3D)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message, onSpeak)
                }
                if (generating) {
                    item {
                        Text(
                            status.ifBlank { "● ● ●" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        if (!generating && messages.isNotEmpty() && status.isNotBlank()) {
            Text(
                status,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Composer(
            draft = draft,
            onDraftChange = onDraftChange,
            onSend = onSend,
            onVoice = onVoice,
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
        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "What would you like to ask AI?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "AI Assistant • online AI + offline fallback • local history",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(22.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickCard(Icons.Outlined.Public, "Research", { onOpenResearch("") }, Modifier.weight(1f))
            QuickCard(Icons.Outlined.ViewInAr, "3D Design", onOpen3D, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickCard(Icons.Outlined.AutoAwesome, "Create", { onOpenResearch("Create: ") }, Modifier.weight(1f))
            QuickCard(Icons.Outlined.AttachFile, "Files", { onOpenResearch("Files: ") }, Modifier.weight(1f))
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
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, onSpeak: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (message.fromUser) 18.dp else 5.dp,
                    bottomEnd = if (message.fromUser) 5.dp else 18.dp
                ),
                color = if (message.fromUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
            ) {
                Text(
                    text = message.text,
                    color = if (message.fromUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (!message.fromUser && message.text.isNotBlank()) {
                IconButton(onClick = { onSpeak(message.text) }) {
                    Icon(Icons.Outlined.Speaker, contentDescription = "Speak")
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
    modifier: Modifier
) {
    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Add, contentDescription = "Tools")
            }
            androidx.compose.foundation.text.BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f).padding(vertical = 13.dp, horizontal = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = false,
                maxLines = 6,
                decorationBox = { inner ->
                    Box {
                        if (draft.isBlank()) {
                            Text(
                                "Message AI-FUSION…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )
            IconButton(onClick = onVoice) {
                Icon(Icons.Outlined.Mic, contentDescription = "Voice")
            }
            IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                Icon(Icons.Outlined.Send, contentDescription = "Send")
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
    clientId: String,
    savedClientId: String,
    account: GoogleAccountUi?,
    resourceStatus: ResourceStatus,
    onClearCache: () -> Unit,
    showClientId: Boolean,
    onShowClientId: () -> Unit,
    onClientIdChange: (String) -> Unit,
    onSaveClientId: () -> Unit,
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
                Text("Google OAuth Client ID", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = clientId,
                    onValueChange = onClientIdChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showClientId) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    label = { Text("Client ID") },
                    placeholder = { Text("xxxx.apps.googleusercontent.com") },
                    trailingIcon = {
                        TextButton(onClick = onShowClientId) {
                            Text(if (showClientId) "Hide" else "Show")
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveClientId) {
                        Text("Save")
                    }
                    Text(
                        "Local-first",
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
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

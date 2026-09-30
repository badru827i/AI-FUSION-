package com.aifusion.app

import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
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
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

private data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String
)

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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var clientId by rememberSaveable { mutableStateOf("") }
    var savedClientId by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable(stateSaver = null) { mutableStateOf<GoogleAccountUi?>(null) }

    var draft by rememberSaveable { mutableStateOf("") }
    var nextId by rememberSaveable { mutableStateOf(1L) }
    val messages = remember { mutableStateOf(listOf<ChatMessage>()) }
    val listState = rememberLazyListState()

    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(messages.value.size) {
        if (messages.value.isNotEmpty()) {
            listState.animateScrollToItem(messages.value.lastIndex)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(0.82f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "AI-FUSION",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    DrawerItem(
                        icon = Icons.Outlined.Add,
                        title = "New chat",
                        onClick = {
                            messages.value = emptyList()
                            draft = ""
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItem(
                        icon = Icons.Outlined.Description,
                        title = "Chat history",
                        onClick = {}
                    )
                    DrawerItem(
                        icon = Icons.Outlined.Settings,
                        title = "Settings",
                        onClick = {
                            showSettings = true
                            scope.launch { drawerState.close() }
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    Divider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "AI-FUSION 4.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Full AI Assistant foundation",
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
                            if (showSettings) "Settings" else "AI-FUSION",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (showSettings) showSettings = false
                            else scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                if (showSettings) Icons.Outlined.Close else Icons.Outlined.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            if (account == null) {
                                Icon(Icons.Outlined.Person, contentDescription = "Account")
                            } else {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            account?.displayName?.firstOrNull()?.uppercase() ?: "G",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (showSettings) {
                SettingsScreen(
                    modifier = Modifier.padding(padding),
                    clientId = clientId,
                    savedClientId = savedClientId,
                    account = account,
                    onClientIdChange = { clientId = it },
                    onSaveClientId = { savedClientId = clientId.trim() },
                    onSignIn = {
                        val id = savedClientId.trim()
                        if (id.isBlank() || !id.contains(".apps.googleusercontent.com")) return@SettingsScreen
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

                                val result = credentialManager.getCredential(
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

                                    if (googleCredential != null) {
                                        account = GoogleAccountUi(
                                            displayName = googleCredential.displayName ?: "Google user",
                                            email = googleCredential.id
                                        )
                                    }
                                }
                            } catch (_: Exception) {
                                // Keep UI stable; provider/backend errors are handled in later auth layer.
                            }
                        }
                    },
                    onBack = { showSettings = false }
                )
            } else {
                ChatHome(
                    modifier = Modifier.padding(padding),
                    messages = messages.value,
                    draft = draft,
                    listState = listState,
                    onDraftChange = { draft = it },
                    onSend = {
                        val text = draft.trim()
                        if (text.isNotEmpty()) {
                            val id = nextId
                            nextId += 1
                            messages.value = messages.value + ChatMessage(id, true, text)
                            messages.value = messages.value + ChatMessage(
                                id + 1,
                                false,
                                "AI-FUSION Chat Core is ready. Connect the Railway AI endpoint in the next phase for live answers."
                            )
                            nextId += 1
                            draft = ""
                        }
                    },
                    onOpenTools = { tool -> draft = tool },
                    imeBottom = WindowInsets.ime
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
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
    listState: androidx.compose.foundation.lazy.LazyListState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenTools: (String) -> Unit,
    imeBottom: WindowInsets
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (messages.isEmpty()) {
            EmptyState(onOpenTools = onOpenTools)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 18.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }
        }

        Composer(
            draft = draft,
            onDraftChange = onDraftChange,
            onSend = onSend,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        )
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun EmptyState(onOpenTools: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
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
            "What would you like to explore?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "AI-FUSION is your new all-purpose AI assistant.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(26.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickCard(
                icon = Icons.Outlined.Public,
                title = "Research",
                onClick = { onOpenTools("Research: ") },
                modifier = Modifier.weight(1f)
            )
            QuickCard(
                icon = Icons.Outlined.Visibility,
                title = "Vision",
                onClick = { onOpenTools("Vision: ") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickCard(
                icon = Icons.Outlined.AutoAwesome,
                title = "Create",
                onClick = { onOpenTools("Create: ") },
                modifier = Modifier.weight(1f)
            )
            QuickCard(
                icon = Icons.Outlined.AttachFile,
                title = "Files",
                onClick = { onOpenTools("Files: ") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
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
    }
}

@Composable
private fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
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
                Icon(Icons.Outlined.Add, contentDescription = "Add")
            }
            androidx.compose.foundation.text.BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 13.dp, horizontal = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = false,
                maxLines = 5,
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
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Mic, contentDescription = "Voice")
            }
            IconButton(
                onClick = onSend,
                enabled = draft.isNotBlank()
            ) {
                Icon(Icons.Outlined.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    clientId: String,
    savedClientId: String,
    account: GoogleAccountUi?,
    onClientIdChange: (String) -> Unit,
    onSaveClientId: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit
) {
    var showClientId by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onSignIn,
                    enabled = savedClientId.contains(".apps.googleusercontent.com")
                ) {
                    Text(if (account == null) "Continue with Google" else "Reconnect Google")
                }
                AnimatedVisibility(account != null) {
                    Text(
                        "Connected on this device. Server-side ID-token validation will be added with the auth backend.",
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Text("Google OAuth Client ID", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Use your Google Cloud OAuth Web client ID.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
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
                        TextButton(onClick = { showClientId = !showClientId }) {
                            Text(if (showClientId) "Hide" else "Show")
                        }
                    },
                    supportingText = {
                        Text(
                            when {
                                clientId.isBlank() -> "Client ID is optional until Google sign-in is needed."
                                clientId.contains(".apps.googleusercontent.com") -> "Format looks valid."
                                else -> "Expected a Google OAuth client ID ending in .apps.googleusercontent.com"
                            }
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveClientId) {
                        Text("Save")
                    }
                    TextButton(onClick = onBack) {
                        Text("Back to chat")
                    }
                }
            }
        }

        Text(
            "AI-FUSION settings will grow here: Railway backend, model manager, voice, research, vision and creative tools.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

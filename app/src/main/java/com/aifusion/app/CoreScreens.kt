package com.aifusion.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.aifusion.app.core.ChatSession
import com.aifusion.app.core.DeviceCapabilities
import com.aifusion.app.core.DeviceOptimizer
import com.aifusion.app.core.LocalChatStore
import com.aifusion.app.core.LocalModel
import com.aifusion.app.core.PerformanceMode
import com.aifusion.app.core.ResearchCore
import com.aifusion.app.core.ResearchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    modifier: Modifier,
    store: LocalChatStore,
    onOpenChat: (ChatSession) -> Unit
) {
    var sessions by remember { mutableStateOf(store.listSessions()) }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Local Chat History", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { sessions = store.listSessions() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
            }
        }

        if (sessions.isEmpty()) {
            Text(
                "Tiada chat disimpan lagi. Chat disimpan secara lokal pada telefon.",
                modifier = Modifier.padding(18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions, key = { it.first }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.second, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "ID " + item.first,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    store.load(item.first)?.let(onOpenChat)
                                }
                            ) {
                                Text("Open")
                            }
                            IconButton(onClick = {
                                store.delete(item.first)
                                sessions = store.listSessions()
                            }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceScreen(
    modifier: Modifier,
    capabilities: DeviceCapabilities,
    selectedMode: PerformanceMode?,
    onSetMode: (PerformanceMode?) -> Unit
) {
    Column(
        modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Smart Device Engine", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Auto-detect resource level, then route local workloads to a safer model tier.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        InfoCard("RAM", capabilities.ramMb.toString() + " MB")
        InfoCard("CPU", capabilities.cpuCores.toString() + " cores")
        InfoCard("GPU", capabilities.gpuApi)
        InfoCard("NPU", if (capabilities.npuAvailable) "Detected" else "Not detected / unknown")
        InfoCard("Model tier", capabilities.modelTier)

        Text("Performance mode", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ModeButton("Auto", selectedMode == null) { onSetMode(null) }
            ModeButton("Low RAM", selectedMode == PerformanceMode.LOW_RAM) { onSetMode(PerformanceMode.LOW_RAM) }
            ModeButton("Balanced", selectedMode == PerformanceMode.BALANCED) { onSetMode(PerformanceMode.BALANCED) }
            ModeButton("Performance", selectedMode == PerformanceMode.PERFORMANCE) { onSetMode(PerformanceMode.PERFORMANCE) }
        }

        Text(
            "Current route: " + DeviceOptimizer.label(capabilities.mode),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "NPU detection is conservative because Android does not expose one universal public NPU identity across all vendors.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ModeButton(title: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(title) }
    } else {
        TextButton(onClick = onClick) { Text(title) }
    }
}

@Composable
private fun InfoCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ResearchScreen(
    modifier: Modifier,
    initialQuery: String,
    onBackToChat: (String) -> Unit
) {
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    var running by remember { mutableStateOf(false) }
    var monitor by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<ResearchResult>>(emptyList()) }
    var status by remember { mutableStateOf("Research Core idle") }
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    suspend fun runResearch() {
        if (query.isBlank() || running) return
        running = true
        status = "Running 3 parallel research agents…"
        try {
            results = ResearchCore.research(query)
        } catch (error: Exception) {
            results = emptyList()
            status = "Research error: " + (error.message ?: "unknown")
        }
        if (results.isNotEmpty()) {
            status = "Agents finished. Results are source previews, not guaranteed fact verification."
        } else if (status.startsWith("Running")) {
            status = "No sources returned."
        }
        running = false
    }

    LaunchedEffect(monitor, query) {
        while (monitor && query.isNotBlank()) {
            runResearch()
            delay(30_000)
        }
    }

    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Research Core", style = MaterialTheme.typography.headlineSmall)
            Text(
                "3 agents work in parallel: discovery, verification search and current-change search.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("Research query") },
                placeholder = { Text("Contoh: teknologi AI telefon 2026") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { scope.launch { runResearch() } },
                        enabled = query.isNotBlank() && !running
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 3.dp))
                        Text(if (running) "Searching…" else "Search")
                    }
                    TextButton(onClick = { onBackToChat(query) }) {
                        Text("Use in chat")
                    }
                }
                Row {
                    Text("Live monitor", modifier = Modifier.padding(top = 12.dp))
                    Switch(checked = monitor, onCheckedChange = { monitor = it })
                }
            }
            Text(status, style = MaterialTheme.typography.bodySmall)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(results, key = { it.agentName }) { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(result.agentName, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(result.summary)
                        result.sources.forEachIndexed { index, source ->
                            TextButton(
                                onClick = { runCatching { uriHandler.openUri(source.url) } },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "[" + (index + 1) + "] " + source.title,
                                    modifier = Modifier.fillMaxWidth()
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
fun ModelManagerScreen(
    modifier: Modifier,
    models: List<LocalModel>,
    modelTier: String,
    onImport: () -> Unit,
    onDelete: (String) -> Unit
) {
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Local Model Manager", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Import .tflite / .onnx files without loading a large model into RAM until an execution adapter is selected.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Recommended route: " + modelTier, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onImport) {
                Icon(Icons.Outlined.Upload, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 3.dp))
                Text("Import model")
            }
        }

        if (models.isEmpty()) {
            Text(
                "Tiada model diimport. Model Manager hanya mengurus fail/model metadata; runtime inference kekal modular.",
                modifier = Modifier.padding(18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(models, key = { it.uri }) { model ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(model.name, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    model.format.uppercase() + " • " + (model.sizeBytes / (1024L * 1024L)) + " MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onDelete(model.uri) }) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}

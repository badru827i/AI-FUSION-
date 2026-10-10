package com.aifusion.app

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val FusionBg = Color(0xFF090C12)
private val FusionSurface = Color(0xFF121822)
private val FusionBorder = Color(0xFF253141)
private val FusionBlue = Color(0xFF9BB9FF)
private val FusionCyan = Color(0xFF63D9E8)
private val FusionText = Color(0xFFEAF0FA)
private val FusionMuted = Color(0xFF93A1B5)

/**
 * AI-FUSION Assistant UI 3.0 home surface.
 * Additive UI component; existing chat engines and navigation are intentionally untouched.
 */
@Composable
fun AiFusionHomeV3(
    onNewChat: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    onOpenEngine: () -> Unit = {},
    onOpenResearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = FusionBg) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeuronMarkV3(Modifier.size(42.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("AI-FUSION", color = FusionText, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text("ASSISTANT 3.0", color = FusionMuted,
                        style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onOpenEngine) {
                    Icon(Icons.Outlined.Settings, contentDescription = "AI Engine settings", tint = FusionMuted)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Apa yang kita buat hari ini?", color = FusionText,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("Satu assistant untuk idea, research dan kerja harian.",
                    color = FusionMuted, style = MaterialTheme.typography.bodyMedium)
            }

            Card(
                onClick = onOpenChat,
                colors = CardDefaults.cardColors(containerColor = FusionSurface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FusionBorder)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF1C2B40)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Memory, contentDescription = null, tint = FusionBlue)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("AI Assistant", color = FusionText, fontWeight = FontWeight.SemiBold)
                            Text("Local-first · Device aware", color = FusionMuted,
                                style = MaterialTheme.typography.labelSmall)
                        }
                        Text("READY", color = FusionCyan, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold)
                    }
                    Text("Tanya apa sahaja…", color = FusionMuted, style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickChipV3("Mula chat", onClick = onNewChat)
                        QuickChipV3("Research", onClick = onOpenResearch)
                    }
                }
            }

            Text("TOOLS", color = FusionMuted, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToolTileV3("Research", "Cari & semak sumber", Icons.Outlined.Public,
                    Modifier.weight(1f), onOpenResearch)
                ToolTileV3("AI Engine", "Model & prestasi", Icons.Outlined.Tune,
                    Modifier.weight(1f), onOpenEngine)
            }

            Spacer(Modifier.weight(1f))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E141D)),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FusionBorder)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(FusionCyan))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Smart Device Engine", color = FusionText, fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium)
                        Text("UI ringan · sesuai kemampuan telefon", color = FusionMuted,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onOpenEngine) { Text("Status") }
                }
            }
        }
    }
}

@Composable
private fun QuickChipV3(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        color = Color(0xFF1A2433), shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, FusionBorder)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            color = FusionBlue, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ToolTileV3(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FusionSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, FusionBorder)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = FusionBlue, modifier = Modifier.size(23.dp))
            Text(title, color = FusionText, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = FusionMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Lightweight animated neural symbol shared by the new home and splash screens. */
@Composable
fun NeuronMarkV3(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fusion-neuron-v3")
    val pulse by transition.animateFloat(
        initialValue = 0.58f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse), label = "pulse"
    )
    Canvas(modifier) {
        val nodes = listOf(
            Offset(.50f,.50f), Offset(.20f,.28f), Offset(.14f,.62f), Offset(.35f,.13f),
            Offset(.75f,.19f), Offset(.88f,.45f), Offset(.72f,.78f), Offset(.46f,.89f),
            Offset(.22f,.80f), Offset(.50f,.28f), Offset(.66f,.53f)
        ).map { Offset(it.x * size.width, it.y * size.height) }
        val edges = listOf(0 to 1, 1 to 2, 1 to 3, 0 to 4, 4 to 5, 0 to 6, 6 to 7, 7 to 8, 8 to 2, 0 to 9, 9 to 3, 9 to 4, 0 to 10, 10 to 6, 10 to 5)
        edges.forEachIndexed { i, e ->
            drawLine(FusionCyan.copy(alpha = if (i % 3 == 0) pulse else .45f),
                nodes[e.first], nodes[e.second], strokeWidth = size.minDimension * .018f,
                cap = StrokeCap.Round)
        }
        nodes.forEachIndexed { i, p ->
            drawCircle(FusionCyan.copy(alpha = pulse), size.minDimension * if (i == 0) .055f else .033f, p)
            drawCircle(FusionText.copy(alpha = .9f), size.minDimension * .012f, p)
        }
        drawCircle(FusionBlue.copy(alpha = .22f * pulse), size.minDimension * .28f,
            Offset(size.width / 2, size.height / 2), style = Stroke(width = size.minDimension * .012f))
    }
}

/** Compact engine overview that can be connected to HardwareMonitor values. */
@Composable
fun AiFusionEngineStatusV3(
    cpu: String = "—",
    gpu: String = "—",
    ram: String = "—",
    model: String = "Belum dipilih",
    localServer: String = "Tidak disambung",
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxSize(), color = FusionBg) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹  Back", color = FusionBlue) }
                Spacer(Modifier.weight(1f))
                Text("AI ENGINE", color = FusionMuted, style = MaterialTheme.typography.labelMedium)
            }
            Text("Device status", color = FusionText, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold)
            Text("Ringkasan sumber yang boleh dipaparkan tanpa animasi berat.",
                color = FusionMuted, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCardV3("CPU", cpu, Modifier.weight(1f))
                MetricCardV3("GPU", gpu, Modifier.weight(1f))
                MetricCardV3("RAM", ram, Modifier.weight(1f))
            }
            StatusLineV3("Model aktif", model)
            StatusLineV3("Laptop Local Server", localServer)
            StatusLineV3("Optimization", "Auto · Low RAM aware")
            Spacer(Modifier.weight(1f))
            Text("Nilai sebenar perlu dibekalkan daripada HardwareMonitor / ModelManager.",
                color = FusionMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MetricCardV3(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FusionSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, FusionBorder)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = FusionMuted, style = MaterialTheme.typography.labelMedium)
            Text(value, color = FusionText, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StatusLineV3(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = FusionSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FusionBorder)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = FusionMuted, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(value, color = FusionText, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
            }
        }
    }
}

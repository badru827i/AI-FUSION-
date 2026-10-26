package com.aifusion.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val LaunchInk = Color(0xFF090B1B)
private val LaunchIndigo = Color(0xFF5267FF)
private val LaunchBlue = Color(0xFF5D9BFF)
private val LaunchViolet = Color(0xFF9B6BFF)
private val LaunchCyan = Color(0xFF73E7FF)
private val LaunchWhite = Color(0xFFF3F5FF)
private val LaunchMuted = Color(0xFFA7B1D2)

/**
 * Lightweight animated entry for AI-FUSION.
 * The glass look is built from translucent fills and fine borders rather than
 * expensive real-time blur, keeping it smooth on mid-range Android devices.
 */
class AiFusionLaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.rgb(9, 11, 27)
        window.navigationBarColor = android.graphics.Color.rgb(9, 11, 27)
        setContent {
            AiFusionTheme {
                GlassNeuralLaunch {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }
            }
        }
    }
}

@Composable
private fun GlassNeuralLaunch(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        // Short, polished reveal; never hold the user behind a fake long loading task.
        delay(1650)
        onFinished()
    }

    val motion = rememberInfiniteTransition(label = "glassNeuralMotion")
    val breathe by motion.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreBreathing"
    )
    val glow by motion.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    val progress by motion.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF080A18), Color(0xFF10112C), Color(0xFF090B1B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Soft ambient pools and thin neural pathways; intentionally low contrast.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LaunchBlue.copy(alpha = glow * 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * 0.28f),
                    radius = size.minDimension * 0.48f
                ),
                radius = size.minDimension * 0.48f,
                center = Offset(size.width * 0.18f, size.height * 0.28f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LaunchViolet.copy(alpha = glow * 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.86f, size.height * 0.72f),
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.86f, size.height * 0.72f)
            )
            val nodes = listOf(
                Offset(.13f, .37f), Offset(.27f, .31f), Offset(.39f, .39f),
                Offset(.61f, .32f), Offset(.78f, .39f), Offset(.88f, .31f),
                Offset(.18f, .62f), Offset(.34f, .69f), Offset(.54f, .61f),
                Offset(.72f, .69f), Offset(.86f, .61f)
            )
            val links = listOf(0 to 1, 1 to 2, 2 to 8, 3 to 4, 4 to 5, 6 to 7, 7 to 8, 8 to 9, 9 to 10, 1 to 3, 4 to 8)
            links.forEachIndexed { index, pair ->
                drawLine(
                    color = (if (index % 2 == 0) LaunchBlue else LaunchViolet).copy(alpha = 0.10f + glow * 0.10f),
                    start = Offset(nodes[pair.first].x * size.width, nodes[pair.first].y * size.height),
                    end = Offset(nodes[pair.second].x * size.width, nodes[pair.second].y * size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            nodes.forEachIndexed { index, node ->
                drawCircle(
                    color = (if (index % 2 == 0) LaunchCyan else LaunchViolet).copy(alpha = 0.20f + glow * 0.28f),
                    radius = if (index == 2 || index == 8) 3.5.dp.toPx() else 2.dp.toPx(),
                    center = Offset(node.x * size.width, node.y * size.height)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .alpha(0.98f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .scale(breathe),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.38f
                    drawCircle(
                        color = LaunchBlue.copy(alpha = glow * 0.12f),
                        radius = radius * 1.45f,
                        center = center
                    )
                    drawCircle(
                        color = LaunchViolet.copy(alpha = glow * 0.12f),
                        radius = radius * 1.18f,
                        center = center,
                        style = Stroke(width = 10.dp.toPx())
                    )
                    drawCircle(
                        brush = Brush.linearGradient(listOf(LaunchBlue, LaunchViolet, LaunchCyan)),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                    val points = listOf(
                        Offset(.50f, .50f), Offset(.29f, .27f), Offset(.12f, .40f),
                        Offset(.72f, .24f), Offset(.88f, .40f), Offset(.81f, .65f),
                        Offset(.63f, .82f), Offset(.37f, .79f), Offset(.17f, .65f),
                        Offset(.45f, .12f), Offset(.54f, .91f), Offset(.91f, .76f)
                    ).map { Offset(it.x * size.width, it.y * size.height) }
                    val edges = listOf(0 to 1, 1 to 2, 0 to 3, 3 to 4, 0 to 5, 5 to 6, 0 to 7, 7 to 8, 1 to 9, 6 to 10, 6 to 11, 0 to 6)
                    edges.forEachIndexed { index, edge ->
                        drawLine(
                            color = (if (index % 3 == 0) LaunchViolet else LaunchCyan).copy(alpha = 0.42f + glow * 0.30f),
                            start = points[edge.first],
                            end = points[edge.second],
                            strokeWidth = if (index % 3 == 0) 1.7.dp.toPx() else 1.1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    points.forEachIndexed { index, point ->
                        val r = if (index == 0) 6.dp.toPx() else if (index % 3 == 0) 3.6.dp.toPx() else 2.6.dp.toPx()
                        drawCircle(LaunchBlue.copy(alpha = 0.22f + glow * 0.26f), r * 2.2f, point)
                        drawCircle(if (index % 2 == 0) LaunchCyan else LaunchViolet, r, point)
                        drawCircle(LaunchWhite.copy(alpha = 0.86f), r * 0.32f, point)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "AI-FUSION",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = LaunchWhite
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "LOCAL INTELLIGENCE · BEAUTIFULLY CONNECTED",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = LaunchCyan
            )
            Spacer(Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xAA171A38),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x665D9BFF))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Text(
                        text = "Waking up neural network",
                        color = LaunchWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Preparing your local AI workspace…",
                        color = LaunchMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = LaunchCyan,
                        trackColor = Color(0xFF303353)
                    )
                    Text(
                        text = "CHAT CORE 3.0  ·  LOCAL-FIRST",
                        color = LaunchBlue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Smooth by design. Private by default.",
                color = LaunchMuted.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

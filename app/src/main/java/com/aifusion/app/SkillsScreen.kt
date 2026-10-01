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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class FusionSkill(
    val name: String,
    val description: String,
    val prompt: String,
    val icon: ImageVector,
    val category: String
)

private val fusionSkills = listOf(
    FusionSkill("Chat Core", "Reasoning, writing, planning and conversation.", "Use Chat Core to solve this task step by step.", Icons.Outlined.AutoAwesome, "Core"),
    FusionSkill("Deep Research", "Break a question into parallel research tasks.", "Use Deep Research to investigate this question with evidence.", Icons.Outlined.Search, "Research"),
    FusionSkill("Deep Fact Verification", "Cross-check claims and identify uncertainty.", "Verify every important claim and show the supporting evidence.", Icons.Outlined.Visibility, "Research"),
    FusionSkill("Live Research Monitor", "Track current information and changes.", "Monitor this topic for current updates and explain what changed.", Icons.Outlined.Public, "Research"),
    FusionSkill("Web & Site Reader", "Read and summarize web pages or sources.", "Read the provided site/source and extract the useful information.", Icons.Outlined.Language, "Research"),
    FusionSkill("Source Compare", "Compare sources and separate facts from opinions.", "Compare these sources, reconcile differences and cite the evidence.", Icons.Outlined.Description, "Research"),
    FusionSkill("Vision & OCR", "Understand images, screenshots and scanned text.", "Analyze this image, detect useful details and extract text.", Icons.Outlined.Visibility, "Multimodal"),
    FusionSkill("Voice", "Speech input and natural spoken responses.", "Prepare a natural voice-ready answer for this request.", Icons.Outlined.GraphicEq, "Multimodal"),
    FusionSkill("Files", "Read, summarize, organize and transform documents.", "Analyze the attached file and return the key information.", Icons.Outlined.Description, "Tools"),
    FusionSkill("Code", "Generate, explain, debug and refactor code.", "Act as a coding assistant and solve this programming task.", Icons.Outlined.Code, "Tools"),
    FusionSkill("Image Create", "Create visual concepts and image prompts.", "Create a detailed visual concept for this request.", Icons.Outlined.Image, "Create"),
    FusionSkill("Video Create", "Plan AI video scenes, motion and generation prompts.", "Create a video-generation plan with scenes, motion and timing.", Icons.Outlined.VideoLibrary, "Create"),
    FusionSkill("3D Model Design", "Design 3D objects with dimensions, topology and materials.", "Design a 3D model for this idea. Include dimensions, parts, topology, materials, assembly and an export-ready modeling plan.", Icons.Outlined.ViewInAr, "Create"),
    FusionSkill("3D Scene & CAD Plan", "Build a structured 3D scene or CAD workflow.", "Create a structured 3D/CAD plan with objects, measurements, constraints and assembly steps.", Icons.Outlined.Build, "Create"),
    FusionSkill("Model Manager", "Route and manage local AI models.", "Choose an efficient local model strategy for this task.", Icons.Outlined.Memory, "AI Core"),
    FusionSkill("Hardware Scheduler", "Adapt work to CPU, GPU, NPU and device limits.", "Plan this workload for the available CPU, GPU and NPU resources.", Icons.Outlined.Settings, "AI Core"),
    FusionSkill("Local AI / Offline", "Prefer on-device processing and minimize network use.", "Solve this locally/offline where practical and only use network data when needed.", Icons.Outlined.Memory, "AI Core"),
    FusionSkill("Research Team", "Coordinate multiple specialized reasoning agents.", "Split this task into specialist agents, then merge and verify their findings.", Icons.Outlined.ShoppingCart, "AI Core")
)

@Composable
fun SkillsScreen(
    modifier: Modifier = Modifier,
    onUseSkill: (FusionSkill) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "AI-FUSION Skills",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Chat Core + research + multimodal + creation tools",
            modifier = Modifier.padding(horizontal = 18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(fusionSkills, key = { it.name }) { skill ->
                Card(
                    onClick = { onUseSkill(skill) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (skill.name == "3D Model Design") {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(skill.icon, contentDescription = null)
                        Column {
                            Text(skill.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                skill.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                skill.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

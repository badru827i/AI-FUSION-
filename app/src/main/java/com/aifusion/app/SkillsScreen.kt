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

private val fusionSkills = FusionToolRouter.allTools().map { info ->
    FusionSkill(
        name = info.name,
        description = info.description,
        prompt = "Use ${info.name} untuk: ",
        icon = Icons.Outlined.AutoAwesome,
        category = info.tool.name
    )
}

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

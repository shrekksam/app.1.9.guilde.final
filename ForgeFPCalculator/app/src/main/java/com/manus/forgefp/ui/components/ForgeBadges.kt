package com.manus.forgefp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manus.forgefp.ui.theme.ForgePalette
import com.manus.forgefp.ui.theme.ForgeRadii
import com.manus.forgefp.ui.theme.ForgeSpacing

/** Sémantique d'un badge de statut. Le rouge est réservé à [DANGER]. */
enum class ForgeStatus { NEUTRAL, PRIMARY, SUCCESS, WARNING, DANGER, GOLD }

private data class StatusColors(val content: Color, val container: Color)

private fun colorsFor(status: ForgeStatus): StatusColors = when (status) {
    ForgeStatus.NEUTRAL -> StatusColors(ForgePalette.TextSecondary, ForgePalette.SurfaceSecondary)
    ForgeStatus.PRIMARY -> StatusColors(ForgePalette.PrimaryStrong, ForgePalette.Primary.copy(alpha = 0.12f))
    ForgeStatus.SUCCESS -> StatusColors(ForgePalette.SuccessStrong, ForgePalette.Success.copy(alpha = 0.14f))
    ForgeStatus.WARNING -> StatusColors(ForgePalette.WarningStrong, ForgePalette.Warning.copy(alpha = 0.16f))
    ForgeStatus.DANGER -> StatusColors(ForgePalette.DangerStrong, ForgePalette.Danger.copy(alpha = 0.14f))
    ForgeStatus.GOLD -> StatusColors(ForgePalette.GoldStrong, ForgePalette.Gold.copy(alpha = 0.16f))
}

/** Badge générique, non coloré par défaut. */
@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = ForgePalette.TextSecondary,
    containerColor: Color = ForgePalette.SurfaceSecondary,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(ForgeRadii.pill))
            .background(containerColor)
            .padding(horizontal = ForgeSpacing.md, vertical = ForgeSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.xs),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = contentColor)
    }
}

/**
 * Badge de statut : associe une sémantique de couleur à un libellé.
 * Un [icon] optionnel renforce le sens (accessibilité : ne pas se reposer
 * uniquement sur la couleur).
 */
@Composable
fun StatusBadge(
    status: ForgeStatus,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = colorsFor(status)
    Row(
        modifier
            .clip(RoundedCornerShape(ForgeRadii.pill))
            .background(colors.container)
            .padding(horizontal = ForgeSpacing.md, vertical = ForgeSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.xs),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(14.dp).clearAndSetSemantics {},
            )
        }
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.content)
    }
}

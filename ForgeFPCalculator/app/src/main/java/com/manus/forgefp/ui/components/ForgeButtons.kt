package com.manus.forgefp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manus.forgefp.ui.theme.ForgeElevation
import com.manus.forgefp.ui.theme.ForgeMotion
import com.manus.forgefp.ui.theme.ForgePalette
import com.manus.forgefp.ui.theme.ForgeRadii
import com.manus.forgefp.ui.theme.ForgeSpacing

/** Bouton principal plein (action primaire). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val background by animateColorAsState(
        targetValue = when {
            !enabled -> ForgePalette.SurfaceSecondary
            pressed -> ForgePalette.PrimaryStrong
            hovered || focused -> ForgePalette.PrimaryLight
            else -> ForgePalette.Primary
        },
        animationSpec = tween(ForgeMotion.fast),
        label = "primaryButtonBackground",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(ForgeMotion.fast),
        label = "primaryButtonScale",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(ForgeRadii.md),
        color = background,
        contentColor = if (enabled) ForgePalette.OnAccent else ForgePalette.TextSecondary,
        shadowElevation = if (pressed || !enabled) 0.dp else ForgeElevation.card,
        border = if (focused) BorderStroke(2.dp, ForgePalette.PrimaryStrong) else null,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(
            Modifier.padding(horizontal = ForgeSpacing.lg, vertical = ForgeSpacing.md).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(ForgeSpacing.sm))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** Bouton secondaire (contour discret, action non destructive). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val background by animateColorAsState(
        targetValue = when {
            !enabled -> ForgePalette.SurfaceSecondary
            pressed -> ForgePalette.SurfaceSecondary
            hovered || focused -> ForgePalette.SurfaceSecondary
            else -> ForgePalette.Card
        },
        animationSpec = tween(ForgeMotion.fast),
        label = "secondaryButtonBackground",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(ForgeRadii.md),
        color = background,
        contentColor = if (enabled) ForgePalette.PrimaryStrong else ForgePalette.TextSecondary,
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = if (focused) ForgePalette.Primary else ForgePalette.Border,
        ),
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 48.dp),
    ) {
        Row(
            Modifier.padding(horizontal = ForgeSpacing.lg, vertical = ForgeSpacing.md).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(ForgeSpacing.sm))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Bouton d'action compact (copier, partager…). Le libellé sert de
 * contentDescription ; une icône seule doit fournir un [contentDescription].
 */
@Composable
fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val background by animateColorAsState(
        targetValue = when {
            !enabled -> ForgePalette.SurfaceSecondary
            pressed -> ForgePalette.Primary.copy(alpha = 0.16f)
            hovered || focused -> ForgePalette.Primary.copy(alpha = 0.10f)
            else -> ForgePalette.Card
        },
        animationSpec = tween(ForgeMotion.fast),
        label = "actionButtonBackground",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(ForgeRadii.md),
        color = background,
        contentColor = if (enabled) ForgePalette.PrimaryStrong else ForgePalette.TextSecondary,
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = if (focused) ForgePalette.Primary else ForgePalette.Border,
        ),
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ForgeSpacing.md, vertical = ForgeSpacing.sm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(ForgeSpacing.xs))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

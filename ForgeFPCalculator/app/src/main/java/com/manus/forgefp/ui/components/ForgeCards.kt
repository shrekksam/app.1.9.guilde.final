package com.manus.forgefp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.manus.forgefp.ui.SparkleIcon
import com.manus.forgefp.ui.theme.ForgeElevation
import com.manus.forgefp.ui.theme.ForgePalette
import com.manus.forgefp.ui.theme.ForgeRadii
import com.manus.forgefp.ui.theme.ForgeSpacing

/** Surface commune aux panneaux du calculateur et de la bibliothèque. */
@Composable
fun ForgeCard(
    modifier: Modifier = Modifier,
    containerColor: Color = ForgePalette.Card,
    borderColor: Color = ForgePalette.Border,
    contentPadding: PaddingValues = PaddingValues(ForgeSpacing.xl),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(ForgeRadii.lg),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = ForgeElevation.card,
        contentColor = ForgePalette.TextPrimary,
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ForgeSpacing.xxs)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = ForgePalette.TextPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ForgePalette.TextSecondary)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(ForgeSpacing.sm))
            trailing()
        }
    }
}

/** Petite métrique : utilisée seulement quand deux valeurs tiennent côte à côte. */
@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = ForgePalette.TextPrimary,
    supporting: String? = null,
    icon: ImageVector? = null,
) {
    ForgeCard(
        modifier = modifier,
        containerColor = ForgePalette.SurfaceSecondary,
        borderColor = Color.Transparent,
        contentPadding = PaddingValues(ForgeSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(ForgeSpacing.xs))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary, maxLines = 2)
        }
        Spacer(Modifier.height(ForgeSpacing.xs))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (supporting != null) {
            Text(supporting, style = MaterialTheme.typography.labelSmall, color = ForgePalette.TextSecondary)
        }
    }
}

/** Conseil contextuel avec un accent coloré discret. */
@Composable
fun AssistantCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    status: ForgeStatus = ForgeStatus.PRIMARY,
) {
    val accent = when (status) {
        ForgeStatus.PRIMARY -> ForgePalette.PrimaryStrong
        ForgeStatus.SUCCESS -> ForgePalette.SuccessStrong
        ForgeStatus.WARNING -> ForgePalette.WarningStrong
        ForgeStatus.DANGER -> ForgePalette.DangerStrong
        ForgeStatus.GOLD -> ForgePalette.GoldStrong
        ForgeStatus.NEUTRAL -> ForgePalette.TextSecondary
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(ForgeRadii.md),
        color = if (status == ForgeStatus.GOLD) ForgePalette.GoldSoft else ForgePalette.SurfaceSecondary,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(ForgeSpacing.md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.md),
        ) {
            Box(
                Modifier.size(30.dp).background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(SparkleIcon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ForgeSpacing.xxs)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = accent)
                Text(message, style = MaterialTheme.typography.bodySmall, color = ForgePalette.TextSecondary)
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = SparkleIcon,
) {
    Column(
        modifier = modifier.fillMaxWidth().background(ForgePalette.Card, RoundedCornerShape(ForgeRadii.lg))
            .padding(horizontal = ForgeSpacing.xxl, vertical = ForgeSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ForgeSpacing.sm),
    ) {
        Box(Modifier.size(54.dp).background(ForgePalette.SurfaceSecondary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = ForgePalette.PrimaryStrong, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(ForgeSpacing.xs))
        Text(title, style = MaterialTheme.typography.titleMedium, color = ForgePalette.TextPrimary)
        Text(message, style = MaterialTheme.typography.bodySmall, color = ForgePalette.TextSecondary)
    }
}

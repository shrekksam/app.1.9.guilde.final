package com.manus.forgefp.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manus.forgefp.ui.BuildingIcon
import com.manus.forgefp.ui.FolderIcon
import com.manus.forgefp.ui.theme.ForgePalette
import com.manus.forgefp.ui.theme.ForgeRadii
import com.manus.forgefp.ui.theme.ForgeSpacing

/** Bandeau d'accueil : l'illustration provient toujours du GB sélectionné. */
@Composable
fun ForgeHeader(
    buildingName: String,
    buildingId: String,
    imagePath: String?,
    level: Int?,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    onOpenDocuments: (() -> Unit)? = null,
) {
    val bitmap = rememberAssetBitmap(imagePath)
    Box(
        modifier.fillMaxWidth().background(
            Brush.linearGradient(listOf(ForgePalette.BackgroundDark, ForgePalette.BackgroundDarkSecondary)),
        ),
    ) {
        // Un discret anneau rappelle l'univers du jeu sans ajouter d'image artificielle.
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 16.dp)
                .size(168.dp).border(1.dp, ForgePalette.Gold.copy(alpha = 0.10f), CircleShape),
        )
        Column(
            Modifier.fillMaxWidth().padding(horizontal = if (wide) ForgeSpacing.xxxl else ForgeSpacing.lg)
                .padding(top = ForgeSpacing.xl, bottom = ForgeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(ForgeSpacing.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(ForgeRadii.md))
                        .background(ForgePalette.Gold),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("F", color = ForgePalette.BackgroundDark, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
                }
                Spacer(Modifier.width(ForgeSpacing.sm))
                Column(Modifier.weight(1f)) {
                    Text("FORGE FP", color = ForgePalette.OnDarkPrimary, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.2.sp)
                    Text("CALCULATEUR DE FIL", color = ForgePalette.OnDarkMuted, style = MaterialTheme.typography.labelSmall, letterSpacing = 0.5.sp)
                }
                if (onOpenDocuments != null) {
                    DocumentsAccessButton(wide, onOpenDocuments)
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ForgeSpacing.sm)) {
                    Text(
                        "STRATÉGIE · CONTRIBUTIONS",
                        color = ForgePalette.Gold,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        if (wide) "Chaque FP compte." else "Chaque FP\ncompte.",
                        color = ForgePalette.OnDarkPrimary,
                        style = if (wide) MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp, lineHeight = 44.sp)
                        else MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, lineHeight = 31.sp),
                    )
                    Text(
                        "Préparez vos places en toute confiance.",
                        color = ForgePalette.OnDarkSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                BuildingArtwork(bitmap, buildingName, wide)
            }

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(ForgeRadii.md))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(ForgeRadii.md))
                    .padding(horizontal = ForgeSpacing.md, vertical = ForgeSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(ForgePalette.Gold))
                Spacer(Modifier.width(ForgeSpacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (buildingId.isBlank()) "MODE PERSONNALISÉ" else "BÂTIMENT SÉLECTIONNÉ",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForgePalette.OnDarkMuted,
                        letterSpacing = 0.6.sp,
                    )
                    Text(
                        buildingName,
                        style = MaterialTheme.typography.labelLarge,
                        color = ForgePalette.OnDarkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (level != null) {
                    Spacer(Modifier.width(ForgeSpacing.sm))
                    Text(
                        "Niv. $level",
                        style = MaterialTheme.typography.labelMedium,
                        color = ForgePalette.Gold,
                        modifier = Modifier.clip(RoundedCornerShape(ForgeRadii.pill))
                            .background(ForgePalette.Gold.copy(alpha = 0.13f))
                            .padding(horizontal = ForgeSpacing.sm, vertical = ForgeSpacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentsAccessButton(wide: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(ForgeRadii.pill),
        color = Color.White.copy(alpha = 0.10f),
        contentColor = ForgePalette.OnDarkPrimary,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        modifier = Modifier.semantics { contentDescription = "Ouvrir les documents" },
    ) {
        Row(
            Modifier.padding(horizontal = ForgeSpacing.md, vertical = ForgeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.xs),
        ) {
            Icon(FolderIcon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(if (wide) "Documents" else "Docs", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun BuildingArtwork(bitmap: Bitmap?, name: String, wide: Boolean) {
    val shape = RoundedCornerShape(ForgeRadii.xl)
    Box(
        Modifier.width(if (wide) 252.dp else 140.dp)
            .height(if (wide) 182.dp else 148.dp)
            .clip(shape)
            .background(ForgePalette.OnDarkPrimary.copy(alpha = 0.05f))
            .border(1.dp, ForgePalette.Gold.copy(alpha = 0.16f), shape)
            .semantics {
                contentDescription = if (bitmap != null) "Illustration : $name" else "Image indisponible pour $name"
            },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(ForgeSpacing.xs),
            )
        } else {
            Icon(BuildingIcon, contentDescription = null, tint = ForgePalette.OnDarkMuted, modifier = Modifier.size(40.dp))
        }
    }
}

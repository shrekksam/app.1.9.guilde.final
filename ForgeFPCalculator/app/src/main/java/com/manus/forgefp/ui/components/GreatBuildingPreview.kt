package com.manus.forgefp.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.manus.forgefp.ui.BuildingIcon
import com.manus.forgefp.ui.theme.ForgePalette
import com.manus.forgefp.ui.theme.ForgeRadii
import com.manus.forgefp.ui.theme.ForgeSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Décode paresseusement une image d'asset en bitmap, en sous-échantillonnant
 * pour éviter de charger une image inutilement grande en mémoire. Le résultat
 * est mémorisé par chemin : aucun décodage répété lors des recompositions.
 */
@Composable
fun rememberAssetBitmap(path: String?, targetWidthPx: Int = 640): Bitmap? {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = null, path, targetWidthPx) {
        value = if (path.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    val longest = maxOf(bounds.outWidth, bounds.outHeight)
                    while (longest / (sample * 2) >= targetWidthPx) sample *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sample }
                    context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }
                }.getOrNull()
            }
        }
    }.value
}

/**
 * Composant réutilisable d'aperçu d'un Grand Bâtiment.
 *
 * - [id] et [name] identifient le bâtiment.
 * - [imagePath] est le chemin d'asset de l'image (ou `null` si absente).
 * - [level] affiche un badge de niveau optionnel.
 *
 * Si l'image n'existe pas dans les données, un placeholder propre est affiché :
 * aucune image n'est inventée.
 */
@Composable
fun GreatBuildingPreview(
    id: String,
    name: String,
    imagePath: String?,
    level: Int?,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 168.dp,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val bitmap = rememberAssetBitmap(imagePath)
    val hasImage = bitmap != null

    Column(modifier, verticalArrangement = Arrangement.spacedBy(ForgeSpacing.md)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .clip(RoundedCornerShape(ForgeRadii.md))
                .background(ForgePalette.BackgroundDarkSecondary)
                .semantics {
                    contentDescription = if (hasImage) "Aperçu de $name" else "Image indisponible pour $name"
                },
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // Dégradé bas pour la lisibilité des badges superposés.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.55f to androidx.compose.ui.graphics.Color.Transparent,
                                1f to ForgePalette.BackgroundDark.copy(alpha = 0.72f),
                            ),
                        ),
                )
            } else {
                PlaceholderArtwork()
            }

            if (level != null) {
                Box(Modifier.fillMaxSize().padding(ForgeSpacing.md), contentAlignment = Alignment.TopEnd) {
                    LevelBadge(level)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    color = ForgePalette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    id,
                    style = MaterialTheme.typography.labelSmall,
                    color = ForgePalette.TextSecondary,
                )
            }
        }

        extra()
    }
}

@Composable
private fun LevelBadge(level: Int) {
    Box(
        Modifier
            .clip(RoundedCornerShape(ForgeRadii.pill))
            .background(ForgePalette.BackgroundDark.copy(alpha = 0.78f))
            .padding(horizontal = ForgeSpacing.md, vertical = ForgeSpacing.xs),
    ) {
        Text(
            "Niveau $level",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ForgePalette.OnDarkPrimary,
        )
    }
}

/** Placeholder neutre et propre lorsqu'aucune image n'est fournie. */
@Composable
private fun PlaceholderArtwork() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ForgeSpacing.sm),
        modifier = Modifier.padding(ForgeSpacing.lg),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(ForgeRadii.md))
                .background(ForgePalette.BackgroundDark.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = BuildingIcon,
                contentDescription = null,
                tint = ForgePalette.OnDarkSecondary,
                modifier = Modifier.size(30.dp),
            )
        }
        Text(
            "Image indisponible",
            style = MaterialTheme.typography.labelMedium,
            color = ForgePalette.OnDarkSecondary,
        )
        Text(
            "Aucune image n'est fournie pour ce bâtiment.",
            style = MaterialTheme.typography.labelSmall,
            color = ForgePalette.OnDarkMuted,
        )
    }
}

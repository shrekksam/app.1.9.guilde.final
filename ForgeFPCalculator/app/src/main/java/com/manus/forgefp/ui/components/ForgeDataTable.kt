package com.manus.forgefp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.manus.forgefp.ui.theme.ForgePalette
import com.manus.forgefp.ui.theme.ForgeRadii
import com.manus.forgefp.ui.theme.ForgeSpacing

/** Définition d'une colonne de tableau. */
data class DataColumn(
    val header: String,
    val weight: Float = 1f,
    val align: TextAlign = TextAlign.Start,
)

/**
 * Tableau de données réutilisable et responsive (poids de colonnes).
 * Affiche clairement « Données indisponibles » lorsque aucune ligne n'est
 * fournie, plutôt qu'une valeur inventée.
 */
@Composable
fun DataTable(
    columns: List<DataColumn>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "Données indisponibles",
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ForgeRadii.md))
            .background(ForgePalette.Card),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(ForgePalette.SurfaceSecondary)
                .padding(horizontal = ForgeSpacing.md, vertical = ForgeSpacing.sm),
        ) {
            columns.forEach { column ->
                Text(
                    column.header.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ForgePalette.TextSecondary,
                    textAlign = column.align,
                    modifier = Modifier.weight(column.weight),
                )
            }
        }

        if (rows.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(ForgeSpacing.lg), contentAlignment = Alignment.Center) {
                Text(
                    emptyMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = ForgePalette.TextSecondary,
                )
            }
        } else {
            rows.forEachIndexed { index, row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (index % 2 == 1) ForgePalette.SurfaceSecondary.copy(alpha = 0.55f) else ForgePalette.Card,
                        )
                        .padding(horizontal = ForgeSpacing.md, vertical = ForgeSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    columns.forEachIndexed { columnIndex, column ->
                        val value = row.getOrNull(columnIndex) ?: "—"
                        Text(
                            value,
                            style = if (columnIndex == 0) {
                                MaterialTheme.typography.labelLarge
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            fontWeight = if (columnIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (columnIndex == 0) ForgePalette.PrimaryStrong else ForgePalette.TextPrimary,
                            textAlign = column.align,
                            modifier = Modifier.weight(column.weight),
                        )
                    }
                }
                if (index < rows.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ForgeSpacing.md)
                            .background(ForgePalette.Border.copy(alpha = 0.5f))
                            .padding(top = 1.dp),
                    ) {}
                }
            }
        }
    }
}

/** Petit utilitaire pour aligner un groupe de métriques en colonnes égales. */
@Composable
fun MetricRow(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ForgeSpacing.sm),
        content = content,
    )
}

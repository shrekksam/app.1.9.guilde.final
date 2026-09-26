package com.manus.forgefp.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manus.forgefp.ui.CloseIcon
import com.manus.forgefp.ui.ChevronLeftIcon
import com.manus.forgefp.ui.ChevronRightIcon
import com.manus.forgefp.ui.MenuIcon
import com.manus.forgefp.ui.SearchIcon
import com.manus.forgefp.ui.ZoomInIcon
import com.manus.forgefp.ui.ZoomOutIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Type de document importé. */
enum class DocumentKind { PDF, EXCEL }

/** Résultat du chargement d'un document, prêt à être affiché. */
sealed interface DocumentContent {
    data class Pdf(val reader: PdfReader) : DocumentContent
    data class Spreadsheet(val data: SpreadsheetData) : DocumentContent
    data class Failure(val message: String) : DocumentContent
}

/** Charge le document importé selon son type. */
fun loadDocument(context: android.content.Context, uri: Uri, kind: DocumentKind): DocumentContent =
    runCatching {
        when (kind) {
            DocumentKind.PDF -> DocumentContent.Pdf(PdfReader(context, uri))
            DocumentKind.EXCEL -> DocumentContent.Spreadsheet(ExcelReader.read(context, uri))
        }
    }.getOrElse { DocumentContent.Failure(it.message ?: "Impossible de lire le document.") }

/**
 * Viewer de document : rendu PDF page par page ou tableur interactif
 * (en-têtes figés, numéros de ligne, largeurs automatiques, zoom, recherche,
 * et affichage des images intégrées au bon emplacement).
 */
@Composable
fun DocumentViewer(content: DocumentContent, modifier: Modifier = Modifier) {
    when (content) {
        is DocumentContent.Pdf -> PdfViewer(content.reader, modifier)
        is DocumentContent.Spreadsheet -> SpreadsheetViewer(content.data, modifier)
        is DocumentContent.Failure -> Box(
            modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Erreur de lecture\n${content.message}", color = MaterialTheme.colorScheme.error)
        }
    }
}

// --------------------------------------------------------------------- PDF

@Composable
private fun PdfViewer(reader: PdfReader, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    var zoom by remember { mutableStateOf(1f) }
    // Résolution de rendu fixe (suffisamment nette jusqu'à un zoom confortable) :
    // le bitmap n'est donc pas re-rendu à chaque cran de zoom, seule sa taille
    // affichée change, ce qui rend le zoom immédiat et fluide.
    val renderWidthPx = remember(configuration.screenWidthDp) {
        with(density) { (configuration.screenWidthDp.dp * 2.5f).roundToPx() }.coerceAtLeast(600)
    }
    val lock = remember { Any() }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${reader.pageCount} page(s)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { zoom = (zoom - 0.25f).coerceAtLeast(0.5f) }) {
                Icon(ZoomOutIcon, contentDescription = "Zoom arrière")
            }
            Text("${(zoom * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            IconButton(onClick = { zoom = (zoom + 0.25f).coerceAtMost(3f) }) {
                Icon(ZoomInIcon, contentDescription = "Zoom avant")
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items((0 until reader.pageCount).toList(), key = { it }) { index ->
                val bitmap by produceState<Bitmap?>(initialValue = null, index, renderWidthPx) {
                    value = runCatching {
                        synchronized(lock) { reader.renderPage(index, renderWidthPx) }
                    }.getOrNull()
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val bmp = bitmap
                    if (bmp == null) {
                        Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // Taille réelle affichée = taille du bitmap mise à l'échelle par le zoom ;
                        // défilement horizontal propre à la page quand elle dépasse l'écran.
                        val pageWidth = with(density) { bmp.width.toDp() } * zoom
                        val pageHeight = with(density) { bmp.height.toDp() } * zoom
                        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.width(pageWidth).height(pageHeight).clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                    Text(
                        "Page ${index + 1} / ${reader.pageCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Excel

@Composable
private fun SpreadsheetViewer(data: SpreadsheetData, modifier: Modifier = Modifier) {
    if (data.isEmpty) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Le classeur ne contient aucune donnée lisible.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    var selectedSheet by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var zoom by remember { mutableStateOf(1f) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var fullscreenImage by remember { mutableStateOf<SheetImage?>(null) }
    var sheetMenuExpanded by remember { mutableStateOf(false) }

    val sheet = data.sheets.getOrElse(selectedSheet) { data.sheets.first() }

    // Cellules correspondant à la recherche (ligne, colonne).
    val matches = remember(sheet, query) {
        if (query.isBlank()) emptySet()
        else buildSet {
            sheet.rows.forEachIndexed { r, row ->
                row.forEachIndexed { c, value ->
                    if (value.contains(query, ignoreCase = true)) add(r to c)
                }
            }
        }
    }

    // Ordre de navigation pour les flèches précédent/suivant de la barre de
    // formule : les résultats de recherche s'il y en a, sinon les cellules
    // non vides de la feuille, dans l'ordre de lecture.
    val navigableCells = remember(sheet, matches) {
        if (matches.isNotEmpty()) {
            matches.sortedWith(compareBy({ it.first }, { it.second }))
        } else {
            buildList {
                sheet.rows.forEachIndexed { r, row ->
                    row.forEachIndexed { c, value -> if (value.isNotBlank()) add(r to c) }
                }
            }
        }
    }

    fun navigate(delta: Int) {
        if (navigableCells.isEmpty()) return
        val current = selectedCell?.let { navigableCells.indexOf(it) } ?: -1
        val next = if (current < 0) 0 else (current + delta).mod(navigableCells.size)
        selectedCell = navigableCells[next]
    }

    Column(modifier.fillMaxSize()) {
        // Barre d'outils : recherche + zoom.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Rechercher…") },
                leadingIcon = { Icon(SearchIcon, contentDescription = null, Modifier.size(20.dp)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.weight(1f),
            )
            if (query.isNotBlank()) {
                Text(
                    "${matches.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            IconButton(onClick = { zoom = (zoom - 0.2f).coerceAtLeast(0.6f) }) {
                Icon(ZoomOutIcon, contentDescription = "Zoom arrière")
            }
            IconButton(onClick = { zoom = (zoom + 0.2f).coerceAtMost(2.4f) }) {
                Icon(ZoomInIcon, contentDescription = "Zoom avant")
            }
        }

        SheetTable(
            sheet = sheet,
            zoom = zoom,
            matches = matches,
            selectedCell = selectedCell,
            onSelectCell = { selectedCell = it },
            onImageClick = { fullscreenImage = it },
            modifier = Modifier.weight(1f),
        )

        // Barre de formule : référence + contenu de la cellule sélectionnée,
        // avec navigation précédent/suivant — juste au-dessus des onglets,
        // comme dans un tableur mobile classique.
        val cell = selectedCell
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "ƒx",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
                if (cell != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            "${columnLabel(cell.second)}${cell.first + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    if (cell == null) "Saisissez du texte ou une formule"
                    else sheet.rows.getOrNull(cell.first)?.getOrNull(cell.second).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { navigate(-1) }, enabled = navigableCells.isNotEmpty()) {
                    Icon(ChevronLeftIcon, contentDescription = "Cellule précédente")
                }
                IconButton(onClick = { navigate(1) }, enabled = navigableCells.isNotEmpty()) {
                    Icon(ChevronRightIcon, contentDescription = "Cellule suivante")
                }
            }
        }

        // Barre d'onglets : menu de feuilles + liste défilante des feuilles,
        // tout en bas de l'écran — agencement habituel d'un tableur mobile.
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    IconButton(onClick = { sheetMenuExpanded = true }) {
                        Icon(MenuIcon, contentDescription = "Liste des feuilles")
                    }
                    DropdownMenu(expanded = sheetMenuExpanded, onDismissRequest = { sheetMenuExpanded = false }) {
                        data.sheets.forEachIndexed { index, s ->
                            DropdownMenuItem(
                                text = { Text(s.name, fontWeight = if (index == selectedSheet) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedSheet = index
                                    selectedCell = null
                                    sheetMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    data.sheets.forEachIndexed { index, s ->
                        FilterChip(
                            selected = index == selectedSheet,
                            onClick = { selectedSheet = index; selectedCell = null },
                            label = { Text(s.name) },
                        )
                    }
                }
            }
        }
    }

    fullscreenImage?.let { image ->
        FullscreenImageDialog(image = image, onDismiss = { fullscreenImage = null })
    }
}

/** Entrée du tableau : une ligne de données ou une image intégrée. */
private sealed interface TableEntry {
    data class Data(val rowIndex: Int) : TableEntry
    data class Image(val image: SheetImage) : TableEntry
}

@Composable
private fun SheetTable(
    sheet: SheetData,
    zoom: Float,
    matches: Set<Pair<Int, Int>>,
    selectedCell: Pair<Int, Int>?,
    onSelectCell: (Pair<Int, Int>) -> Unit,
    onImageClick: (SheetImage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnCount = sheet.columnCount.coerceAtLeast(1)

    // Largeurs de colonnes calculées d'après le contenu (bornées), mises à l'échelle par le zoom.
    val columnWidths = remember(sheet, zoom) {
        (0 until columnCount).map { c ->
            val maxChars = sheet.rows.asSequence().filter { row -> row.any(String::isNotBlank) }
                .take(300).maxOfOrNull { it.getOrElse(c) { "" }.length } ?: 0
            val chars = maxChars.coerceIn(4, 42)
            ((chars * 8f) + 20f).dp * zoom
        }
    }
    val rowNumberWidth = remember(sheet, zoom) {
        val digits = sheet.rowCount.toString().length.coerceAtLeast(2)
        ((digits * 9f) + 18f).dp * zoom
    }
    val fontSize = (13f * zoom).coerceIn(9f, 22f).sp
    val rowHeight = (30f * zoom).coerceIn(22f, 60f).dp

    // Un seul état de défilement horizontal partagé => en-tête, lignes et images synchronisés.
    val hScroll = rememberScrollState()

    // Liste ordonnée : chaque ligne de données, suivie des images ancrées sur cette ligne.
    val entries = remember(sheet) {
        val list = mutableListOf<TableEntry>()
        val imagesByRow = sheet.images.groupBy { it.anchorRow }
        for (r in 0 until sheet.rows.size) {
            list.add(TableEntry.Data(r))
            imagesByRow[r]?.forEach { list.add(TableEntry.Image(it)) }
        }
        imagesByRow.filterKeys { it >= sheet.rows.size }.toSortedMap()
            .values.flatten().forEach { list.add(TableEntry.Image(it)) }
        list
    }

    val headerBg = MaterialTheme.colorScheme.surfaceVariant
    val headerFg = MaterialTheme.colorScheme.onSurfaceVariant
    val headerActiveBg = MaterialTheme.colorScheme.tertiaryContainer
    val headerActiveFg = MaterialTheme.colorScheme.onTertiaryContainer
    val rowNumBg = MaterialTheme.colorScheme.surfaceVariant
    val rowNumFg = MaterialTheme.colorScheme.onSurfaceVariant
    val matchBg = MaterialTheme.colorScheme.tertiaryContainer
    val selectionBorder = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Column(modifier.fillMaxSize()) {
        // En-tête figé : coin + lettres de colonnes. La colonne de la cellule
        // sélectionnée est mise en évidence, comme dans un tableur classique.
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier.width(rowNumberWidth).height(rowHeight).background(headerBg)
                    .border(0.5.dp, gridColor),
                contentAlignment = Alignment.Center,
            ) {
                Text("#", style = MaterialTheme.typography.labelSmall, color = headerFg, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.horizontalScroll(hScroll)) {
                for (c in 0 until columnCount) {
                    val isActiveColumn = selectedCell?.second == c
                    Box(
                        Modifier.width(columnWidths[c]).height(rowHeight)
                            .background(if (isActiveColumn) headerActiveBg else headerBg)
                            .border(0.5.dp, gridColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            columnLabel(c),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isActiveColumn) headerActiveFg else headerFg,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Corps : numéros de ligne figés + cellules et images défilantes.
        LazyColumn(Modifier.weight(1f)) {
            items(entries.size) { entryIndex ->
                when (val entry = entries[entryIndex]) {
                    is TableEntry.Data -> {
                        val rowIndex = entry.rowIndex
                        val row = sheet.rows[rowIndex]
                        val isActiveRow = selectedCell?.first == rowIndex
                        Row(Modifier.fillMaxWidth()) {
                            Box(
                                Modifier.width(rowNumberWidth).height(rowHeight)
                                    .background(if (isActiveRow) headerActiveBg else rowNumBg)
                                    .border(0.5.dp, gridColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${rowIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isActiveRow) headerActiveFg else rowNumFg,
                                    fontWeight = if (isActiveRow) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                )
                            }
                            Row(Modifier.horizontalScroll(hScroll)) {
                                for (c in 0 until columnCount) {
                                    val value = row.getOrElse(c) { "" }
                                    val isMatch = matches.contains(rowIndex to c)
                                    val isSelected = selectedCell == (rowIndex to c)
                                    val fillArgb = sheet.colorAt(rowIndex, c)
                                    val fillColor = fillArgb?.let { Color(it) }
                                    // La couleur d'origine de la cellule (issue du fichier) reste
                                    // visible même sélectionnée : on la garde en fond et on ajoute
                                    // une bordure d'accent, plutôt que de la recouvrir — comme le
                                    // fait un tableur classique.
                                    val baseBg = when {
                                        fillColor != null -> fillColor
                                        rowIndex == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        rowIndex % 2 == 0 -> MaterialTheme.colorScheme.surface
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    }
                                    val bg = if (isMatch) matchBg.copy(alpha = 0.7f).compositeOver(baseBg) else baseBg
                                    // Sur un fond de cellule coloré (issu du fichier), on choisit une
                                    // couleur de texte lisible plutôt que la couleur de texte par défaut du thème.
                                    val textColor = if (fillColor != null) {
                                        readableTextColor(bg)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                    Box(
                                        Modifier
                                            .width(columnWidths[c])
                                            .height(rowHeight)
                                            .background(bg)
                                            .border(if (isSelected) 2.dp else 0.5.dp, if (isSelected) selectionBorder else gridColor)
                                            .clickable { onSelectCell(rowIndex to c) }
                                            .padding(horizontal = 6.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = fontSize),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is TableEntry.Image -> {
                        // Décalage horizontal = largeur des numéros de ligne + colonnes précédentes.
                        val offset: Dp = rowNumberWidth +
                            columnWidths.take(entry.image.anchorColumn.coerceIn(0, columnWidths.size))
                                .fold(0.dp) { acc, w -> acc + w }
                        Row(Modifier.fillMaxWidth()) {
                            Spacer(Modifier.width(rowNumberWidth))
                            Row(Modifier.horizontalScroll(hScroll)) {
                                Spacer(Modifier.width(offset - rowNumberWidth))
                                SheetImageItem(
                                    image = entry.image,
                                    zoom = zoom,
                                    onClick = { onImageClick(entry.image) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Affiche une image intégrée, décodée hors du thread principal. */
@Composable
private fun SheetImageItem(image: SheetImage, zoom: Float, onClick: () -> Unit) {
    val bitmap by produceState<Bitmap?>(initialValue = null, image) {
        value = withContext(Dispatchers.Default) {
            runCatching { BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size) }.getOrNull()
        }
    }

    val bmp = bitmap
    if (bmp == null) {
        Box(
            Modifier.width(200.dp).height(120.dp).padding(8.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(Modifier.size(24.dp)) }
        return
    }

    // Taille naturelle : dimensions du dessin (EMU -> dp) si présentes, sinon taille
    // intrinsèque du bitmap. Bornée pour rester raisonnable, mise à l'échelle par le zoom.
    val naturalWidth = if (image.widthEmu > 0) (image.widthEmu / 9525f).dp
    else (bmp.width / 2f).dp
    val naturalHeight = if (image.heightEmu > 0) (image.heightEmu / 9525f).dp
    else (bmp.height / 2f).dp
    val maxWidth = 1600.dp
    val scale = if (naturalWidth > maxWidth) maxWidth / naturalWidth else 1f
    val width = (naturalWidth * scale * zoom).coerceAtLeast(120.dp)
    val height = (naturalHeight * scale * zoom).coerceAtLeast(80.dp)

    Box(
        Modifier
            .padding(vertical = 6.dp)
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
    ) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Image intégrée",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Affiche une image intégrée en plein écran, avec pincement pour zoomer,
 * glissement pour se déplacer une fois zoomé, et double-tap pour
 * zoomer/dézoomer rapidement — comme une visionneuse d'image habituelle.
 * Les boutons +/- restent disponibles pour un contrôle plus fin.
 */
@Composable
private fun FullscreenImageDialog(image: SheetImage, onDismiss: () -> Unit) {
    val bitmap by produceState<Bitmap?>(initialValue = null, image) {
        value = withContext(Dispatchers.Default) {
            runCatching { BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size) }.getOrNull()
        }
    }
    var zoom by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    fun applyZoom(newZoom: Float) {
        val clamped = newZoom.coerceIn(1f, 6f)
        zoom = clamped
        if (clamped <= 1f) offset = Offset.Zero
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(CloseIcon, contentDescription = "Fermer")
                    }
                    Text(
                        "Image intégrée",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { applyZoom(zoom - 0.5f) }) {
                        Icon(ZoomOutIcon, contentDescription = "Zoom arrière")
                    }
                    Text("${(zoom * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    IconButton(onClick = { applyZoom(zoom + 0.5f) }) {
                        Icon(ZoomInIcon, contentDescription = "Zoom avant")
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val newZoom = (zoom * gestureZoom).coerceIn(1f, 6f)
                                offset = if (newZoom <= 1f) Offset.Zero else offset + pan
                                zoom = newZoom
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = { applyZoom(if (zoom > 1f) 1f else 2.5f) })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = bitmap
                    if (bmp == null) {
                        CircularProgressIndicator()
                    } else {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Image intégrée",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .graphicsLayer(
                                    scaleX = zoom,
                                    scaleY = zoom,
                                    translationX = offset.x,
                                    translationY = offset.y,
                                ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Choisit noir ou blanc selon la luminance du fond, pour rester lisible sur
 * n'importe quelle couleur de remplissage venant du fichier (jaune vif, violet
 * foncé, etc.), plutôt que d'utiliser la couleur de texte fixe du thème.
 */
private fun readableTextColor(background: Color): Color =
    // Seuil WCAG : contraste noir supérieur au contraste blanc au-dessus de 0,179.
    if (background.luminance() > 0.179f) Color.Black else Color.White

/** Convertit un index de colonne (base 0) en lettre de colonne (A, B, …, AA). */
private fun columnLabel(index: Int): String {
    var i = index
    val sb = StringBuilder()
    while (i >= 0) {
        sb.insert(0, ('A' + (i % 26)))
        i = i / 26 - 1
    }
    return sb.toString()
}

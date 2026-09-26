package com.manus.forgefp.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import jxl.Workbook
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipInputStream

/**
 * Lecteur de classeurs Excel.
 *
 * - `.xlsx` (OOXML) : parseur maison qui lit le contenu réel des feuilles
 *   (sharedStrings, valeurs, chaînes en ligne), reconstruit la grille et
 *   extrait les **images intégrées** (dessins OOXML) avec leur position.
 * - `.xls` (BIFF) : lecture via la bibliothèque jxl.
 *
 * Le résultat est une [SpreadsheetData] exploitable par un viewer (onglets de
 * feuilles + tableau + images), et non un simple affichage brut de fichier.
 */
object ExcelReader {

    fun read(context: Context, uri: Uri): SpreadsheetData {
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()?.lowercase() ?: (uri.lastPathSegment ?: "").lowercase()
        val mime = runCatching { context.contentResolver.getType(uri)?.lowercase() }.getOrNull()
        // Les URI du sélecteur Android ne conservent pas toujours l'extension.
        val isXls = name.endsWith(".xls") || mime == "application/vnd.ms-excel"
        return if (isXls) readXls(context, uri) else readXlsx(context, uri)
    }

    // ------------------------------------------------------------------ .xls

    private fun readXls(context: Context, uri: Uri): SpreadsheetData {
        val sheets = mutableListOf<SheetData>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            val workbook = Workbook.getWorkbook(input)
            try {
                for (sheet in workbook.sheets) {
                    val rows = ArrayList<List<String>>()
                    for (r in 0 until sheet.rows) {
                        val row = ArrayList<String>(sheet.columns)
                        for (c in 0 until sheet.columns) {
                            row.add(sheet.getCell(c, r).contents?.trim() ?: "")
                        }
                        rows.add(row)
                    }
                    sheets.add(SheetData(sheet.name, trimRows(rows)))
                }
            } finally {
                workbook.close()
            }
        }
        return SpreadsheetData(sheets)
    }

    // ----------------------------------------------------------------- .xlsx

    private fun readXlsx(context: Context, uri: Uri): SpreadsheetData {
        val entries = HashMap<String, ByteArray>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return readXlsxEntries(entries)
    }

    /** Point d'entrée indépendant d'Android, permettant de tester de vrais assemblages OOXML. */
    internal fun readXlsxEntries(entries: Map<String, ByteArray>): SpreadsheetData {
        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val workbookRels = entries["xl/_rels/workbook.xml.rels"]
        val rels = workbookRels?.let { parseRels(it) } ?: emptyMap()
        val sheetRefs = entries["xl/workbook.xml"]?.let { parseWorkbook(it) } ?: emptyList()
        val stylesTarget = workbookRels?.let { findRelatedPart(it, "styles") }
        val stylesPath = stylesTarget?.let { resolvePath("xl", it) } ?: "xl/styles.xml"
        val stylesBytes = entries[stylesPath]
        val themeTarget = workbookRels?.let { findRelatedPart(it, "theme") }
        val themePath = themeTarget?.let { resolvePath("xl", it) } ?: "xl/theme/theme1.xml"
        val styles = OoxmlStyles.parse(stylesBytes, entries[themePath])

        val sheets = mutableListOf<SheetData>()
        sheetRefs.forEachIndexed { index, ref ->
            val target = rels[ref.relId] ?: "worksheets/sheet${index + 1}.xml"
            val path = resolvePath("xl", target)
            val bytes = entries[path] ?: return@forEachIndexed
            val parsed = parseSheet(bytes, sharedStrings, styles)
            val images = parseSheetImages(entries, path)

            // Les images/merges/styles comptent même lorsque les dernières lignes n'ont
            // pas de valeur : trimRows() seul supprimait leurs coordonnées physiques.
            val lastValueRow = parsed.rows.entries.filter { row -> row.value.any(String::isNotBlank) }
                .maxOfOrNull { it.key } ?: -1
            val lastColorRow = parsed.colors.keys.maxOfOrNull { it.row } ?: -1
            val lastImageRow = images.maxOfOrNull { it.anchorRow } ?: -1
            val lastStyleRow = parsed.rowColors.keys.maxOrNull() ?: -1
            val observedLast = maxOf(lastValueRow, lastColorRow, lastImageRow, lastStyleRow)
            // Une plage fusionnée gigantesque ne doit pas matérialiser toute la feuille
            // si elle ne contient aucune autre donnée. Les lignes réellement présentes
            // (texte, couleur ou image) ne sont, elles, jamais tronquées.
            val lastMergeRow = parsed.merges.filter { range ->
                val anchor = CellPos(range.firstRow, range.firstCol)
                range.firstRow <= maxOf(observedLast, 0) && (
                    parsed.rows[anchor.row]?.getOrNull(anchor.col)?.isNotBlank() == true ||
                    parsed.colors[anchor] != null || parsed.rowColors[anchor.row] != null ||
                    parsed.columnColors.any { anchor.col in it.firstCol..it.lastCol }
                )
            }.maxOfOrNull { minOf(it.lastRow, maxOf(observedLast, 0) + 1_000) } ?: -1
            val lastRow = maxOf(observedLast, lastMergeRow, if (parsed.columnColors.isNotEmpty()) 0 else -1)
            val rows: List<List<String>> = if (lastRow < 0) emptyList() else
                List(lastRow + 1) { parsed.rows[it]?.toList() ?: emptyList() }

            propagateMergedColors(parsed, rows.lastIndex)
            val lastCol = maxOf(rows.maxOfOrNull { it.lastIndex } ?: -1,
                parsed.colors.keys.maxOfOrNull { it.col } ?: -1,
                images.maxOfOrNull { it.anchorColumn } ?: -1)
            val conditional = OoxmlConditionalFormatting.parse(bytes, styles)
            OoxmlConditionalFormatting.apply(conditional, rows, parsed.colors, lastCol)
            // Une couleur calculée sur l'ancre d'une fusion s'étend à toute la fusion.
            propagateMergedColors(parsed, rows.lastIndex)
            sheets.add(SheetData(ref.name, rows, images, parsed.colors,
                parsed.rowColors, parsed.columnColors))
        }
        return SpreadsheetData(sheets)
    }

    private fun findRelatedPart(bytes: ByteArray, type: String): String? {
        val parser = newParser(bytes)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && localName(parser.name) == "Relationship" &&
                parser.localAttribute("Type")?.endsWith("/$type") == true &&
                parser.localAttribute("TargetMode") != "External"
            ) return parser.localAttribute("Target")
            event = parser.next()
        }
        return null
    }

    /**
     * Résout un `Target` de fichier `.rels` par rapport au dossier contenant la
     * partie source, en gérant les chemins relatifs (`../media/image1.png`) et
     * absolus (`/xl/media/image1.png`).
     *
     * OOXML place les relations d'une partie dans un sous-dossier `_rels/` de
     * son propre dossier (ex. `xl/worksheets/_rels/sheet1.xml.rels`), et les
     * `Target` qu'elles contiennent sont donc écrits relativement à ce dossier
     * parent (`xl/worksheets`), pas relativement au fichier `.rels` lui-même.
     * Une image collée dans une feuille, par exemple, est presque toujours
     * référencée via un chemin remontant d'un ou plusieurs niveaux
     * (`../drawings/drawing1.xml`, puis `../media/image1.png`) : ignorer les
     * `..` (comme le faisait l'ancienne version) produit une clé qui ne
     * correspond à aucune entrée du zip, et l'image est alors silencieusement
     * absente du résultat.
     *
     * [base] : chemin du dossier contenant la partie source (ex. "xl",
     * "xl/worksheets", "xl/drawings"), sans slash final.
     */
    private fun resolvePath(base: String, target: String): String {
        if (target.startsWith("/")) return target.removePrefix("/")
        val parts = ArrayDeque(base.split('/').filter { it.isNotEmpty() })
        for (segment in target.split('/')) {
            when (segment) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeLast()
                else -> parts.addLast(segment)
            }
        }
        return parts.joinToString("/")
    }

    private data class SheetRef(val name: String, val relId: String)

    private fun parseWorkbook(bytes: ByteArray): List<SheetRef> {
        val result = mutableListOf<SheetRef>()
        val parser = newParser(bytes)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && localName(parser.name) == "sheet") {
                val name = parser.localAttribute("name") ?: "Feuille"
                val relId = parser.localAttribute("id") ?: ""
                result.add(SheetRef(name, relId))
            }
            event = parser.next()
        }
        return result
    }

    private fun parseRels(bytes: ByteArray): Map<String, String> {
        val result = HashMap<String, String>()
        val parser = newParser(bytes)
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && localName(parser.name) == "Relationship") {
                val id = parser.localAttribute("Id")
                val target = parser.localAttribute("Target")
                if (id != null && target != null && parser.localAttribute("TargetMode") != "External") {
                    result[id] = target
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val parser = newParser(bytes)
        var event = parser.eventType
        var current: StringBuilder? = null
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (localName(parser.name)) {
                    "si" -> current = StringBuilder()
                    "t" -> if (current != null) current.append(parser.nextText())
                }
                XmlPullParser.END_TAG -> if (localName(parser.name) == "si") {
                    result.add(current?.toString() ?: "")
                    current = null
                }
            }
            event = parser.next()
        }
        return result
    }

    private data class ParsedSheet(
        val rows: MutableMap<Int, MutableList<String>> = HashMap(),
        val colors: MutableMap<CellPos, Int> = HashMap(),
        val rowColors: MutableMap<Int, Int> = HashMap(),
        val columnColors: MutableList<ColumnFill> = mutableListOf(),
        val merges: MutableList<CellRange> = mutableListOf(),
    )

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>, styles: OoxmlStyles): ParsedSheet {
        val sheet = ParsedSheet()
        val parser = newParser(bytes)
        var event = parser.eventType
        var currentRowIndex = -1
        var lastColumnIndex = -1
        var inCell = false
        var cellRow = -1
        var cellColumn = -1
        var cellType: String? = null
        var cellStyle: Int? = null
        var cellValue: String? = null
        var inlineText: StringBuilder? = null

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (localName(parser.name)) {
                    "col" -> {
                        val color = styles.colorForStyleIndex(parser.localAttribute("style")?.toIntOrNull())
                        val first = parser.localAttribute("min")?.toIntOrNull()?.minus(1)
                        val last = parser.localAttribute("max")?.toIntOrNull()?.minus(1)
                        if (color != null && first != null && last != null && first in 0..16_383 && last in first..16_383) {
                            sheet.columnColors.add(ColumnFill(first, last, color))
                        }
                    }
                    "row" -> {
                        val number = parser.localAttribute("r")?.toIntOrNull()?.minus(1)
                        currentRowIndex = number?.takeIf { it in 0..1_048_575 } ?: currentRowIndex + 1
                        lastColumnIndex = -1
                        sheet.rows.getOrPut(currentRowIndex) { mutableListOf() }
                        styles.colorForStyleIndex(parser.localAttribute("s")?.toIntOrNull())
                            ?.let { sheet.rowColors[currentRowIndex] = it }
                    }
                    "c" -> {
                        inCell = true
                        val ref = parser.localAttribute("r")
                        cellRow = rowIndex(ref) ?: currentRowIndex.coerceAtLeast(0)
                        cellColumn = if (ref != null) columnIndex(ref) else lastColumnIndex + 1
                        lastColumnIndex = cellColumn
                        cellType = parser.localAttribute("t")
                        cellStyle = parser.localAttribute("s")?.toIntOrNull()
                        cellValue = null
                        inlineText = null
                    }
                    "v" -> if (inCell) cellValue = parser.nextText()
                    "t" -> if (inCell && cellType == "inlineStr") {
                        inlineText = (inlineText ?: StringBuilder()).append(parser.nextText())
                    }
                    "mergeCell" -> sheet.merges.addAll(parseCellRanges(parser.localAttribute("ref")))
                }
                XmlPullParser.END_TAG -> if (localName(parser.name) == "c" && inCell) {
                    val row = sheet.rows.getOrPut(cellRow) { mutableListOf() }
                    // Un <c r="C4"> après <c r="A4"> doit remplacer l'indice C,
                    // jamais être simplement ajouté en fin de liste.
                    while (row.size <= cellColumn) row.add("")
                    row[cellColumn] = when (cellType) {
                        "s" -> cellValue?.toIntOrNull()?.let { sharedStrings.getOrElse(it) { "" } } ?: ""
                        "inlineStr" -> inlineText?.toString() ?: ""
                        else -> cellValue ?: ""
                    }.trim()
                    styles.colorForStyleIndex(cellStyle)?.let { sheet.colors[CellPos(cellRow, cellColumn)] = it }
                    inCell = false
                }
            }
            event = parser.next()
        }
        return sheet
    }

    private fun propagateMergedColors(sheet: ParsedSheet, lastRow: Int) {
        if (lastRow < 0) return
        for (range in sheet.merges) {
            val anchor = CellPos(range.firstRow, range.firstCol)
            val anchorColor = sheet.colors[anchor] ?: sheet.rowColors[anchor.row] ?:
                sheet.columnColors.lastOrNull { anchor.col in it.firstCol..it.lastCol }?.argb ?: continue
            for (r in range.firstRow..minOf(range.lastRow, lastRow)) {
                // Excel permet 16 384 colonnes, mais les fusions purement décoratives
                // non peuplées sont bornées pour éviter des millions d'entrées mémoire.
                for (c in range.firstCol..minOf(range.lastCol, range.firstCol + 255)) {
                    sheet.colors[CellPos(r, c)] = anchorColor
                }
            }
        }
    }

    // ------------------------------------------------------------- images

    /**
     * Extrait les images intégrées d'une feuille.
     *
     * Chemin OOXML : `xl/worksheets/_rels/sheetN.xml.rels` -> `drawingN.xml`
     * -> `xl/drawings/_rels/drawingN.xml.rels` -> `xl/media/imageN.*`.
     */
    private fun parseSheetImages(entries: Map<String, ByteArray>, sheetPath: String): List<SheetImage> {
        val sheetFileName = sheetPath.substringAfterLast('/')
        val sheetDir = sheetPath.substringBeforeLast('/', "xl")
        val sheetRelsPath = "$sheetDir/_rels/$sheetFileName.rels"
        val sheetRels = entries[sheetRelsPath]?.let { parseRels(it) } ?: return emptyList()

        val images = mutableListOf<SheetImage>()
        sheetRels.values.forEach { drawingTarget ->
            if (!drawingTarget.contains("drawing", ignoreCase = true)) return@forEach
            val drawingPath = resolvePath(sheetDir, drawingTarget)
            val drawingBytes = entries[drawingPath] ?: return@forEach
            val drawingDir = drawingPath.substringBeforeLast('/', "xl")
            val drawingFileName = drawingPath.substringAfterLast('/')
            val drawingRelsPath = "$drawingDir/_rels/$drawingFileName.rels"
            val drawingRels = entries[drawingRelsPath]?.let { parseRels(it) } ?: emptyMap()
            images.addAll(parseDrawing(drawingBytes, drawingRels, entries, drawingDir))
        }
        return images
    }

    private fun parseDrawing(
        bytes: ByteArray,
        rels: Map<String, String>,
        entries: Map<String, ByteArray>,
        drawingDir: String,
    ): List<SheetImage> {
        val result = mutableListOf<SheetImage>()
        val parser = newParser(bytes)
        var event = parser.eventType

        var anchorRow = 0
        var anchorColumn = 0
        var widthEmu = 0L
        var heightEmu = 0L
        var embedId: String? = null
        var inFrom = false
        var currentTag: String? = null

        fun flush() {
            val id = embedId
            if (id != null) {
                val target = rels[id]
                if (target != null) {
                    val mediaPath = resolvePath(drawingDir, target)
                    val data = entries[mediaPath]
                    if (data != null) {
                        result.add(SheetImage(data, anchorRow, anchorColumn, widthEmu, heightEmu))
                    }
                }
            }
            anchorRow = 0; anchorColumn = 0; widthEmu = 0L; heightEmu = 0L; embedId = null
        }

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = localName(parser.name)
                    currentTag = tag
                    when (tag) {
                        "from" -> inFrom = true
                        "ext" -> {
                            widthEmu = parser.getAttributeValue(null, "cx")?.toLongOrNull() ?: 0L
                            heightEmu = parser.getAttributeValue(null, "cy")?.toLongOrNull() ?: 0L
                        }
                        "blip" -> {
                            embedId = parser.localAttribute("embed")
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inFrom) {
                        val text = parser.text?.trim() ?: ""
                        when (currentTag) {
                            "col" -> anchorColumn = text.toIntOrNull() ?: anchorColumn
                            "row" -> anchorRow = text.toIntOrNull() ?: anchorRow
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (localName(parser.name)) {
                        "from" -> inFrom = false
                        "oneCellAnchor", "twoCellAnchor", "absoluteAnchor" -> flush()
                    }
                    currentTag = null
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun newParser(bytes: ByteArray): XmlPullParser = newOoxmlParser(bytes)

    /** Supprime les lignes entièrement vides en fin de feuille. */
    private fun trimRows(rows: List<List<String>>): List<List<String>> {
        var last = rows.size
        while (last > 0 && rows[last - 1].all { it.isBlank() }) last--
        return rows.subList(0, last).map { it.toList() }
    }
}

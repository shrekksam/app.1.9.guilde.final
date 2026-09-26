package com.manus.forgefp.document

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** OOXML utilise aussi bien les espaces de noms par défaut que les préfixes x:, a:, xdr:… */
internal fun localName(name: String?): String? = name?.substringAfterLast(':')

internal fun newOoxmlParser(bytes: ByteArray): XmlPullParser =
    XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        .newPullParser().apply { setInput(bytes.inputStream(), null) }

internal fun XmlPullParser.localAttribute(name: String): String? =
    getAttributeValue(null, name) ?: (0 until attributeCount)
        .firstOrNull { localName(getAttributeName(it)) == name }
        ?.let { getAttributeValue(it) }

/** Coordonnées physiques du classeur, indépendantes des balises <row> effectivement écrites. */
internal data class CellRange(
    val firstRow: Int,
    val lastRow: Int,
    val firstCol: Int,
    val lastCol: Int,
) {
    fun contains(row: Int, col: Int): Boolean = row in firstRow..lastRow && col in firstCol..lastCol
}

private const val MAX_EXCEL_ROW = 1_048_575
private const val MAX_EXCEL_COL = 16_383
private val addressRegex = Regex("^\\$?([A-Za-z]{1,3})\\$?([0-9]+)$")
private val columnRegex = Regex("^\\$?([A-Za-z]{1,3})$")
private val rowRegex = Regex("^\\$?([0-9]+)$")

internal fun columnIndex(ref: String?): Int {
    val letters = ref?.trim()?.replace("$", "")?.takeWhile { it.isLetter() }.orEmpty()
    if (letters.isEmpty()) return 0
    var index = 0
    for (letter in letters.uppercase()) index = index * 26 + (letter - 'A' + 1)
    return (index - 1).coerceIn(0, MAX_EXCEL_COL)
}

internal fun rowIndex(ref: String?): Int? {
    val digits = ref?.trim()?.replace("$", "")?.dropWhile { it.isLetter() } ?: return null
    return digits.toIntOrNull()?.takeIf { it in 1..MAX_EXCEL_ROW + 1 }?.minus(1)
}

/** Une référence ou plusieurs plages séparées par des espaces (attribut sqref). */
internal fun parseCellRanges(refs: String?): List<CellRange> = refs.orEmpty()
    .trim().split(Regex("\\s+")).mapNotNull { entry ->
        if (entry.isEmpty()) return@mapNotNull null
        val ends = entry.split(':')
        if (ends.size > 2) return@mapNotNull null
        val start = parseEnd(ends[0]) ?: return@mapNotNull null
        val end = parseEnd(ends.getOrElse(1) { ends[0] }) ?: return@mapNotNull null
        // Il faut deux références du même type : cellule, colonne ou ligne.
        if (start.kind != end.kind) return@mapNotNull null
        CellRange(
            minOf(start.row, end.row), maxOf(start.lastRow, end.lastRow),
            minOf(start.col, end.col), maxOf(start.lastCol, end.lastCol),
        )
    }

private data class RangeEnd(val kind: Int, val row: Int, val lastRow: Int, val col: Int, val lastCol: Int)

private fun parseEnd(ref: String): RangeEnd? {
    addressRegex.matchEntire(ref)?.let {
        val row = it.groupValues[2].toIntOrNull()?.takeIf { n -> n in 1..MAX_EXCEL_ROW + 1 }?.minus(1)
            ?: return null
        val col = columnIndex(it.groupValues[1])
        return RangeEnd(0, row, row, col, col)
    }
    columnRegex.matchEntire(ref)?.let {
        val col = columnIndex(it.groupValues[1])
        return RangeEnd(1, 0, MAX_EXCEL_ROW, col, col)
    }
    rowRegex.matchEntire(ref)?.let {
        val row = it.groupValues[1].toIntOrNull()?.takeIf { n -> n in 1..MAX_EXCEL_ROW + 1 }?.minus(1)
            ?: return null
        return RangeEnd(2, row, row, 0, MAX_EXCEL_COL)
    }
    return null
}

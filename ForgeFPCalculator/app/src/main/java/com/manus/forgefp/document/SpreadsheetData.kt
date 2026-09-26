package com.manus.forgefp.document

/** Données d'un classeur tableur (Excel) prêtes à être affichées par le viewer. */
data class SpreadsheetData(
    val sheets: List<SheetData>,
) {
    val isEmpty: Boolean get() = sheets.isEmpty() || sheets.all { it.rows.isEmpty() && it.images.isEmpty() }
}

/** Remplissage par défaut d'une plage de colonnes (balise <col style="…">). */
data class ColumnFill(val firstCol: Int, val lastCol: Int, val argb: Int)

/**
 * Les indices de [rows], [cellColors] et les ancres de [images] sont toujours les
 * numéros physiques OOXML (base 0). Les lignes absentes du XML restent vides
 * dans [rows] : elles ne décalent ni les styles ni les dessins.
 */
data class SheetData(
    val name: String,
    val rows: List<List<String>>,
    val images: List<SheetImage> = emptyList(),
    val cellColors: Map<CellPos, Int> = emptyMap(),
    val rowColors: Map<Int, Int> = emptyMap(),
    val columnColors: List<ColumnFill> = emptyList(),
) {
    val columnCount: Int by lazy {
        val lastUsed = maxOf(
            rows.maxOfOrNull { it.lastIndex } ?: -1,
            cellColors.keys.maxOfOrNull { it.col } ?: -1,
            images.maxOfOrNull { it.anchorColumn } ?: -1,
        )
        // Une plage A:XFD stylée ne doit pas forcer le rendu de 16 384 colonnes vides.
        val styleLimit = maxOf(lastUsed + 16, 16)
        val lastStyled = columnColors.filter { it.firstCol <= styleLimit }
            .maxOfOrNull { minOf(it.lastCol, styleLimit) } ?: -1
        maxOf(lastUsed, lastStyled) + 1
    }

    val rowCount: Int get() = rows.size

    fun colorAt(row: Int, col: Int): Int? = cellColors[CellPos(row, col)]
        ?: rowColors[row]
        ?: columnColors.lastOrNull { col in it.firstCol..it.lastCol }?.argb
}

/** Coordonnées (base 0) d'une cellule dans la grille. */
data class CellPos(val row: Int, val col: Int)

/**
 * Image intégrée dans une feuille (dessin OOXML).
 * [anchorRow] / [anchorColumn] sont les coordonnées physiques (base 0) de
 * l'ancrage « from » du dessin, même si la ligne n'existe pas dans sheetData.
 */
data class SheetImage(
    val bytes: ByteArray,
    val anchorRow: Int,
    val anchorColumn: Int,
    val widthEmu: Long,
    val heightEmu: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SheetImage) return false
        return anchorRow == other.anchorRow &&
            anchorColumn == other.anchorColumn &&
            widthEmu == other.widthEmu &&
            heightEmu == other.heightEmu &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + anchorRow
        result = 31 * result + anchorColumn
        result = 31 * result + widthEmu.hashCode()
        result = 31 * result + heightEmu.hashCode()
        return result
    }
}

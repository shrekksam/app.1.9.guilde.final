package com.manus.forgefp.document

import org.xmlpull.v1.XmlPullParser
import kotlin.math.roundToInt

/** Sous-ensemble déterministe des règles de remplissage OOXML, sans moteur de formules Excel. */
internal object OoxmlConditionalFormatting {
    internal data class ScaleStop(val type: String, val value: String?)
    internal data class ColorScale(val stops: List<ScaleStop>, val colors: List<Int>)
    internal data class Rule(
        val ranges: List<CellRange>,
        val type: String,
        val priority: Int,
        val stopIfTrue: Boolean,
        val operator: String?,
        val text: String?,
        val formulas: List<String>,
        val fill: Int?,
        val scale: ColorScale?,
    )

    /** Les règles sont conservées avec leur priorité, pas appliquées dans l'ordre XML. */
    fun parse(sheetXml: ByteArray, styles: OoxmlStyles): List<Rule> {
        val result = mutableListOf<Rule>()
        val parser = newOoxmlParser(sheetXml)
        var ranges = emptyList<CellRange>()
        var current: RuleBuilder? = null
        var inScale = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val tag = localName(parser.name).orEmpty()
            when (event) {
                XmlPullParser.START_TAG -> when (tag) {
                    "conditionalFormatting" -> ranges = parseCellRanges(parser.localAttribute("sqref"))
                    "cfRule" -> if (ranges.isNotEmpty()) {
                        current = RuleBuilder(
                            ranges = ranges,
                            type = parser.localAttribute("type").orEmpty(),
                            priority = parser.localAttribute("priority")?.toIntOrNull() ?: Int.MAX_VALUE,
                            stopIfTrue = parser.localAttribute("stopIfTrue") == "1" ||
                                parser.localAttribute("stopIfTrue") == "true",
                            operator = parser.localAttribute("operator"),
                            text = parser.localAttribute("text"),
                            fill = styles.colorForDxf(parser.localAttribute("dxfId")?.toIntOrNull()),
                        )
                    }
                    "formula" -> if (current != null) current!!.formulas.add(parser.nextText())
                    "colorScale" -> if (current != null) inScale = true
                    "cfvo" -> if (inScale) current?.stops?.add(
                        ScaleStop(parser.localAttribute("type").orEmpty(), parser.localAttribute("val")),
                    )
                    "color" -> if (inScale) styles.resolveColor(parser)?.let { current?.colors?.add(it) }
                }
                XmlPullParser.END_TAG -> when (tag) {
                    "colorScale" -> inScale = false
                    "cfRule" -> {
                        current?.build()?.let(result::add)
                        current = null
                    }
                    "conditionalFormatting" -> ranges = emptyList()
                }
            }
            event = parser.next()
        }
        return result.withIndex().sortedWith(compareBy({ it.value.priority }, { it.index })).map { it.value }
    }

    private class RuleBuilder(
        val ranges: List<CellRange>,
        val type: String,
        val priority: Int,
        val stopIfTrue: Boolean,
        val operator: String?,
        val text: String?,
        val fill: Int?,
    ) {
        val formulas = mutableListOf<String>()
        val stops = mutableListOf<ScaleStop>()
        val colors = mutableListOf<Int>()
        fun build(): Rule = Rule(ranges, type, priority, stopIfTrue, operator, text, formulas.toList(),
            fill, if (stops.size in 2..3 && stops.size == colors.size) ColorScale(stops.toList(), colors.toList()) else null)
    }

    /** Applique les règles sur les dimensions effectivement utilisées, jamais sur 1 048 576 lignes. */
    fun apply(
        rules: List<Rule>,
        rows: List<List<String>>,
        cellColors: MutableMap<CellPos, Int>,
        lastUsedColumn: Int,
    ) {
        if (rows.isEmpty() || rules.isEmpty()) return
        val assignments = HashMap<CellPos, Int>()
        val stopped = HashSet<CellPos>()
        val observedLastCol = maxOf(lastUsedColumn, rows.maxOfOrNull { it.lastIndex } ?: -1,
            cellColors.keys.maxOfOrNull { it.col } ?: -1)
        for (rule in rules) {
            if (rule.fill == null && rule.scale == null && !rule.stopIfTrue) continue
            val gradient = rule.scale?.let { resolveScale(it, rule.ranges, rows) }
            if (rule.type == "colorScale" && gradient == null) continue
            // Une expression peut colorer des cellules vides en fonction d'une autre colonne.
            // On montre quelques colonnes adjacentes, mais pas une plage entière XFD:XFD vide.
            val lastCol = if (rule.type == "expression") maxOf(observedLastCol,
                minOf(observedLastCol + 32, 255)) else observedLastCol
            if (lastCol < 0) continue
            for (range in rule.ranges) {
                val endRow = minOf(range.lastRow, rows.lastIndex)
                val endCol = minOf(range.lastCol, lastCol)
                if (range.firstRow > endRow || range.firstCol > endCol) continue
                for (r in range.firstRow..endRow) {
                    for (c in range.firstCol..endCol) {
                        val pos = CellPos(r, c)
                        if (pos in stopped) continue
                        val value = rows[r].getOrNull(c).orEmpty()
                        val color = when (rule.type) {
                            "colorScale" -> gradient?.colorFor(value.toDoubleOrNull()?.takeIf { it.isFinite() })
                            else -> if (matches(rule, value, pos, rows)) rule.fill else null
                        }
                        val matched = color != null || (rule.stopIfTrue &&
                            rule.type != "colorScale" && matches(rule, value, pos, rows))
                        if (color != null && pos !in assignments) assignments[pos] = color
                        if (matched && rule.stopIfTrue) stopped.add(pos)
                    }
                }
            }
        }
        cellColors.putAll(assignments)
    }

    private fun matches(rule: Rule, value: String, pos: CellPos, rows: List<List<String>>): Boolean {
        val needle = rule.text ?: searchPattern.find(rule.formulas.firstOrNull().orEmpty())?.groupValues?.get(2)
        return when (rule.type) {
            "containsText" -> value.isNotBlank() && !needle.isNullOrEmpty() && value.contains(needle, ignoreCase = true)
            "notContainsText" -> value.isNotBlank() && !needle.isNullOrEmpty() && !value.contains(needle, ignoreCase = true)
            "beginsWith" -> value.isNotBlank() && !needle.isNullOrEmpty() && value.startsWith(needle, ignoreCase = true)
            "endsWith" -> value.isNotBlank() && !needle.isNullOrEmpty() && value.endsWith(needle, ignoreCase = true)
            "cellIs" -> cellIs(rule, value, pos, rows)
            "expression" -> expression(rule.formulas.firstOrNull().orEmpty(), pos, rows, rule.ranges.first())
            else -> false // Une formule inconnue ne doit pas donner une couleur incorrecte.
        }
    }

    private fun cellIs(rule: Rule, value: String, pos: CellPos, rows: List<List<String>>): Boolean {
        val number = value.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return false
        val anchor = rule.ranges.first()
        val first = numericFormula(rule.formulas.getOrNull(0), pos, anchor, rows) ?: return false
        val second = numericFormula(rule.formulas.getOrNull(1), pos, anchor, rows)
        return when (rule.operator) {
            "greaterThan" -> number > first
            "greaterThanOrEqual" -> number >= first
            "lessThan" -> number < first
            "lessThanOrEqual" -> number <= first
            "equal" -> number == first
            "notEqual" -> number != first
            "between" -> second != null && number >= first && number <= second
            "notBetween" -> second != null && (number < first || number > second)
            else -> false
        }
    }

    private fun numericFormula(formula: String?, pos: CellPos, anchor: CellRange, rows: List<List<String>>): Double? {
        val source = formula?.trim()?.removePrefix("=") ?: return null
        source.toDoubleOrNull()?.takeIf { it.isFinite() }?.let { return it }
        return referencedValue(source, pos, anchor, rows)?.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    private val cellAddress = Regex("^(\\$?)([A-Za-z]{1,3})(\\$?)([0-9]+)$")
    private fun referencedValue(ref: String, pos: CellPos, anchor: CellRange, rows: List<List<String>>): String? {
        val parts = cellAddress.matchEntire(ref) ?: return null
        val baseRow = parts.groupValues[4].toIntOrNull()?.minus(1) ?: return null
        val baseCol = columnIndex(parts.groupValues[2])
        val row = if (parts.groupValues[3] == "$") baseRow else baseRow + pos.row - anchor.firstRow
        val col = if (parts.groupValues[1] == "$") baseCol else baseCol + pos.col - anchor.firstCol
        return rows.getOrNull(row)?.getOrNull(col)
    }

    private val searchPattern = Regex("(?i)(SEARCH|FIND)\\(\\s*\"([^\"]+)\"\\s*[,;]\\s*(\\$?[A-Z]{1,3}\\$?[0-9]+)\\s*\\)")
    private val countIfPattern = Regex("(?i)COUNTIF\\(\\s*(\\$?[A-Z]{1,3}\\$?[0-9]+)\\s*[,;]\\s*\"\\*([^*\"]+)\\*\"\\s*\\)")

    /** SEARCH/FIND et COUNTIF avec texte littéral ; les autres formules restent intactes. */
    private fun expression(formula: String, pos: CellPos, rows: List<List<String>>, anchor: CellRange): Boolean {
        val compact = formula.trim().removePrefix("=").replace(Regex("\\s+"), "").uppercase()
        searchPattern.find(formula)?.let { match ->
            val positive = compact.startsWith("ISNUMBER(SEARCH(") || compact.startsWith("ISNUMBER(FIND(") ||
                compact.startsWith("NOT(ISERROR(SEARCH(") || compact.startsWith("NOT(ISERROR(FIND(")
            val negative = compact.startsWith("ISERROR(SEARCH(") || compact.startsWith("ISERROR(FIND(") ||
                compact.startsWith("NOT(ISNUMBER(SEARCH(") || compact.startsWith("NOT(ISNUMBER(FIND(")
            if (!positive && !negative) return false
            val content = referencedValue(match.groupValues[3], pos, anchor, rows).orEmpty()
            val found = content.isNotEmpty() && content.contains(match.groupValues[2],
                ignoreCase = match.groupValues[1].equals("SEARCH", ignoreCase = true))
            return if (positive) found else !found
        }
        countIfPattern.find(formula)?.let { match ->
            if (!compact.contains(")>0") && !compact.contains(")>=1")) return false
            return referencedValue(match.groupValues[1], pos, anchor, rows)
                ?.contains(match.groupValues[2], ignoreCase = true) == true
        }
        return false
    }

    private data class ResolvedScale(val thresholds: List<Double>, val colors: List<Int>) {
        fun colorFor(number: Double?): Int? {
            val value = number ?: return null
            val segment = if (thresholds.size == 3 && value > thresholds[1]) 1 else 0
            val low = thresholds[segment]
            val high = thresholds[segment + 1]
            val fraction = if (high <= low) {
                if (value >= high) 1.0 else 0.0
            } else ((value - low) / (high - low)).coerceIn(0.0, 1.0)
            return mix(colors[segment], colors[segment + 1], fraction)
        }
    }

    private fun resolveScale(scale: ColorScale, ranges: List<CellRange>, rows: List<List<String>>): ResolvedScale? {
        val values = mutableListOf<Double>()
        ranges.forEach { range ->
            for (r in range.firstRow..minOf(range.lastRow, rows.lastIndex)) {
                val row = rows[r]
                for (c in range.firstCol..minOf(range.lastCol, row.lastIndex)) {
                    row[c].toDoubleOrNull()?.takeIf { it.isFinite() }?.let(values::add)
                }
            }
        }
        if (values.isEmpty()) return null
        values.sort()
        val low = values.first()
        val high = values.last()
        val points = scale.stops.map { stop ->
            when (stop.type) {
                "min" -> low
                "max" -> high
                "num" -> stop.value?.toDoubleOrNull()
                "percent" -> stop.value?.toDoubleOrNull()?.let { low + (high - low) * it.coerceIn(0.0, 100.0) / 100.0 }
                "percentile" -> stop.value?.toDoubleOrNull()?.let { percentile(values, it) }
                "formula" -> stop.value?.toDoubleOrNull() // référence ou formule complexe : non interprétée
                else -> null
            }?.takeIf { it.isFinite() } ?: return null
        }
        return ResolvedScale(points, scale.colors)
    }

    private fun percentile(sorted: List<Double>, percentage: Double): Double {
        val position = (sorted.lastIndex * percentage.coerceIn(0.0, 100.0) / 100.0)
        val index = position.toInt()
        val fraction = position - index
        return sorted[index] + (sorted[minOf(index + 1, sorted.lastIndex)] - sorted[index]) * fraction
    }

    private fun mix(from: Int, to: Int, fraction: Double): Int {
        fun channel(shift: Int): Int {
            val a = (from ushr shift) and 0xFF
            val b = (to ushr shift) and 0xFF
            return (a + (b - a) * fraction).roundToInt().coerceIn(0, 255)
        }
        return 0xFF000000.toInt() or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}

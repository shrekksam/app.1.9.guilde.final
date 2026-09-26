package com.manus.forgefp.document

import org.xmlpull.v1.XmlPullParser
import kotlin.math.abs
import kotlin.math.roundToInt

/** Styles de remplissage des cellules et styles différentiels des règles conditionnelles. */
internal class OoxmlStyles private constructor(
    private val fillColors: List<Int?>,
    private val cellFillIds: List<Int>,
    private val dxfColors: List<Int?>,
    private val palette: OoxmlColors,
) {
    fun colorForStyleIndex(index: Int?): Int? =
        index?.let { cellFillIds.getOrNull(it) }?.let { fillColors.getOrNull(it) }

    fun colorForDxf(index: Int?): Int? = index?.let { dxfColors.getOrNull(it) }
    fun resolveColor(parser: XmlPullParser): Int? = palette.resolve(parser)

    companion object {
        fun parse(styles: ByteArray?, theme: ByteArray?): OoxmlStyles {
            val colors = OoxmlColors.fromXml(styles, theme)
            if (styles == null) return OoxmlStyles(emptyList(), emptyList(), emptyList(), colors)

            val fills = mutableListOf<Int?>()
            val cellXfs = mutableListOf<Int>()
            val dxfs = mutableListOf<Int?>()
            var inFills = false
            var inCellXfs = false
            var inDxfs = false
            var inDxf = false
            var dxfColor: Int? = null
            var currentFill: Fill? = null
            val parser = newOoxmlParser(styles)
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val tag = localName(parser.name).orEmpty()
                when (event) {
                    XmlPullParser.START_TAG -> when (tag) {
                        "fills" -> inFills = true
                        "cellXfs" -> inCellXfs = true
                        "dxfs" -> inDxfs = true
                        "dxf" -> if (inDxfs) { inDxf = true; dxfColor = null }
                        "fill" -> if (inFills || inDxf) currentFill = Fill()
                        "patternFill" -> currentFill?.pattern = parser.localAttribute("patternType")
                        "fgColor" -> currentFill?.fg = colors.resolve(parser)
                        "bgColor" -> currentFill?.bg = colors.resolve(parser)
                        "xf" -> if (inCellXfs) cellXfs.add(parser.localAttribute("fillId")?.toIntOrNull() ?: 0)
                    }
                    XmlPullParser.END_TAG -> when (tag) {
                        "fill" -> {
                            val color = currentFill?.color()
                            if (inFills) fills.add(color) else if (inDxf) dxfColor = color
                            currentFill = null
                        }
                        "dxf" -> if (inDxf) { dxfs.add(dxfColor); inDxf = false }
                        "fills" -> inFills = false
                        "cellXfs" -> inCellXfs = false
                        "dxfs" -> inDxfs = false
                    }
                }
                event = parser.next()
            }
            return OoxmlStyles(fills, cellXfs, dxfs, colors)
        }
    }
}

private class Fill {
    var pattern: String? = null
    var fg: Int? = null
    var bg: Int? = null

    // Certaines bibliothèques n'écrivent que bgColor (ou omettent patternType).
    fun color(): Int? = if (pattern == null || pattern == "solid") fg ?: bg else null
}

/** Résolution des couleurs OOXML : RVB, thème + teinte, palette indexée, repli Office. */
internal class OoxmlColors private constructor(
    private val theme: List<Int>,
    private val indexed: List<Int>,
) {
    fun resolve(parser: XmlPullParser): Int? {
        val direct = parseHex(parser.localAttribute("rgb"))
        val themed = parser.localAttribute("theme")?.toIntOrNull()?.let { theme.getOrNull(it) }
        val legacy = parser.localAttribute("indexed")?.toIntOrNull()?.takeIf { it in 0..63 }
            ?.let { indexed.getOrNull(it) }
        val color = direct ?: themed ?: legacy ?: return null
        val tintAmount = parser.localAttribute("tint")?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
        return tint(color, tintAmount)
    }

    companion object {
        fun fromXml(styles: ByteArray?, theme: ByteArray?): OoxmlColors =
            OoxmlColors(parseTheme(theme), parseIndexed(styles))

        /** Excel n'utilise pas le premier octet ARGB comme transparence pour les remplissages. */
        private fun parseHex(hex: String?): Int? {
            val digits = hex?.removePrefix("#")?.takeIf { it.length == 6 || it.length == 8 } ?: return null
            return digits.takeLast(6).toIntOrNull(16)?.let { it or 0xFF000000.toInt() }
        }

        private val defaultTheme = listOf(
            "FFFFFF", "000000", "EEECE1", "1F497D", "4F81BD", "C0504D",
            "9BBB59", "8064A2", "4BACC6", "F79646", "0000FF", "800080",
        ).map { parseHex(it)!! }
        private val themeSlots = mapOf(
            "lt1" to 0, "dk1" to 1, "lt2" to 2, "dk2" to 3,
            "accent1" to 4, "accent2" to 5, "accent3" to 6,
            "accent4" to 7, "accent5" to 8, "accent6" to 9,
            "hlink" to 10, "folHlink" to 11,
        )

        private fun parseTheme(bytes: ByteArray?): List<Int> {
            if (bytes == null) return defaultTheme
            val result = defaultTheme.toMutableList()
            val parser = newOoxmlParser(bytes)
            var inScheme = false
            var slot: Int? = null
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val tag = localName(parser.name).orEmpty()
                when (event) {
                    XmlPullParser.START_TAG -> when {
                        tag == "clrScheme" -> inScheme = true
                        inScheme && themeSlots.containsKey(tag) -> slot = themeSlots[tag]
                        inScheme && slot != null && tag == "srgbClr" ->
                            parseHex(parser.localAttribute("val"))?.let { result[slot!!] = it }
                        inScheme && slot != null && tag == "sysClr" ->
                            parseHex(parser.localAttribute("lastClr") ?: parser.localAttribute("val"))
                                ?.let { result[slot!!] = it }
                    }
                    XmlPullParser.END_TAG -> {
                        if (inScheme && themeSlots.containsKey(tag)) slot = null
                        if (tag == "clrScheme") inScheme = false
                    }
                }
                event = parser.next()
            }
            return result
        }

        // Palette BIFF/OOXML héritée (indices 0..63). 64 et 65 sont des couleurs système.
        private val defaultIndexed = listOf(
            "000000", "FFFFFF", "FF0000", "00FF00", "0000FF", "FFFF00", "FF00FF", "00FFFF",
            "000000", "FFFFFF", "FF0000", "00FF00", "0000FF", "FFFF00", "FF00FF", "00FFFF",
            "800000", "008000", "000080", "808000", "800080", "008080", "C0C0C0", "808080",
            "9999FF", "993366", "FFFFCC", "CCFFFF", "660066", "FF8080", "0066CC", "CCCCFF",
            "000080", "FF00FF", "FFFF00", "00FFFF", "800080", "800000", "008080", "0000FF",
            "00CCFF", "CCFFFF", "CCFFCC", "FFFF99", "99CCFF", "FF99CC", "CC99FF", "FFCC99",
            "3366FF", "33CCCC", "99CC00", "FFCC00", "FF9900", "FF6600", "666699", "969696",
            "003366", "339966", "003300", "333300", "993300", "993366", "333399", "333333",
        ).map { parseHex(it)!! }

        private fun parseIndexed(bytes: ByteArray?): List<Int> {
            if (bytes == null) return defaultIndexed
            val result = defaultIndexed.toMutableList()
            val parser = newOoxmlParser(bytes)
            var inIndexed = false
            var index = 0
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val tag = localName(parser.name).orEmpty()
                when (event) {
                    XmlPullParser.START_TAG -> when (tag) {
                        "indexedColors" -> { inIndexed = true; index = 0 }
                        "rgbColor" -> if (inIndexed) {
                            if (index in result.indices) {
                                parseHex(parser.localAttribute("rgb"))?.let { result[index] = it }
                            }
                            index++
                        }
                    }
                    XmlPullParser.END_TAG -> if (tag == "indexedColors") inIndexed = false
                }
                event = parser.next()
            }
            return result
        }

        /** Teinte OOXML sur la luminance HSL : négatif = assombrir, positif = éclaircir. */
        internal fun tint(argb: Int, amount: Double): Int {
            val t = amount.coerceIn(-1.0, 1.0)
            if (t == 0.0) return argb
            val r = (argb ushr 16 and 0xFF) / 255.0
            val g = (argb ushr 8 and 0xFF) / 255.0
            val b = (argb and 0xFF) / 255.0
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min
            val lightness = (max + min) / 2.0
            val saturation = if (delta == 0.0) 0.0 else delta / (1.0 - abs(2 * lightness - 1))
            val hue = if (delta == 0.0) 0.0 else when (max) {
                r -> 60 * (((g - b) / delta) % 6)
                g -> 60 * ((b - r) / delta + 2)
                else -> 60 * ((r - g) / delta + 4)
            }.let { (it + 360) % 360 }
            val adjusted = if (t < 0) lightness * (1 + t) else lightness * (1 - t) + t
            val chroma = (1 - abs(2 * adjusted - 1)) * saturation
            val x = chroma * (1 - abs((hue / 60) % 2 - 1))
            val (rr, gg, bb) = when {
                hue < 60 -> Triple(chroma, x, 0.0)
                hue < 120 -> Triple(x, chroma, 0.0)
                hue < 180 -> Triple(0.0, chroma, x)
                hue < 240 -> Triple(0.0, x, chroma)
                hue < 300 -> Triple(x, 0.0, chroma)
                else -> Triple(chroma, 0.0, x)
            }
            val m = adjusted - chroma / 2
            fun byte(v: Double) = ((v + m) * 255).roundToInt().coerceIn(0, 255)
            return 0xFF000000.toInt() or (byte(rr) shl 16) or (byte(gg) shl 8) or byte(bb)
        }
    }
}

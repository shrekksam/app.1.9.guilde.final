package com.manus.forgefp.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Petites parties OOXML construites en mémoire : aucune dépendance à un fichier de l'utilisateur. */
class ExcelReaderTest {
    @Test
    fun sparseAndHiddenRowsKeepPhysicalCoordinatesForMergedColorsAndImages() {
        val sheet = readSheet(
            sheetXml = """
                <worksheet xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheetData>
                    <row r="1"><c r="A1"><v>Debut</v></c></row>
                    <row r="4" hidden="1"><c r="C4" s="1"><v>42</v></c></row>
                    <row r="7"><c r="A7"><v>Fin</v></c></row>
                  </sheetData>
                  <mergeCells><mergeCell ref="C4:D5"/></mergeCells>
                  <drawing r:id="rIdDrawing"/>
                </worksheet>
            """,
            stylesXml = styles(
                """<fill><patternFill patternType="solid"><fgColor rgb="FF336699"/></patternFill></fill>""",
                xfFillIds = listOf(0, 1),
            ),
            drawingXml = """
                <xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing"
                          xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <xdr:oneCellAnchor>
                    <xdr:from><xdr:col>3</xdr:col><xdr:row>5</xdr:row></xdr:from>
                    <xdr:ext cx="952500" cy="476250"/>
                    <xdr:pic><xdr:blipFill><a:blip r:embed="rIdPicture"/></xdr:blipFill></xdr:pic>
                  </xdr:oneCellAnchor>
                </xdr:wsDr>
            """,
        )
        assertEquals(7, sheet.rowCount)
        assertTrue(sheet.rows[1].isEmpty())
        assertTrue(sheet.rows[2].isEmpty())
        assertEquals("42", sheet.rows[3][2])
        assertEquals("Fin", sheet.rows[6][0])
        assertEquals(0xFF336699.toInt(), sheet.colorAt(3, 2))
        assertEquals(0xFF336699.toInt(), sheet.colorAt(4, 3)) // fusion sur une ligne absente
        assertEquals(1, sheet.images.size)
        assertEquals(5, sheet.images.single().anchorRow) // ligne 6 réelle, pas 3e ligne écrite
        assertEquals(3, sheet.images.single().anchorColumn)
        assertEquals(4, sheet.columnCount)
    }

    @Test
    fun imagePastLastWrittenRowKeepsItsAnchor() {
        val sheet = readSheet(
            sheetXml = """<worksheet><sheetData><row r="1"><c r="A1"><v>1</v></c></row></sheetData></worksheet>""",
            stylesXml = styles("", listOf(0)),
            drawingXml = """
                <xdr:wsDr xmlns:xdr="http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing"
                          xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <xdr:oneCellAnchor>
                    <xdr:from><xdr:col>1</xdr:col><xdr:row>9</xdr:row></xdr:from>
                    <xdr:pic><a:blip r:embed="rIdPicture"/></xdr:pic>
                  </xdr:oneCellAnchor>
                </xdr:wsDr>
            """,
        )
        assertEquals(10, sheet.rowCount)
        assertTrue(sheet.rows[8].isEmpty())
        assertEquals(9, sheet.images.single().anchorRow)
    }

    @Test
    fun themeTintIndexedCustomPaletteBgFallbackAndColumnStyles() {
        val sheet = readSheet(
            sheetXml = """
                <worksheet><cols><col min="6" max="6" style="5"/></cols><sheetData>
                  <row r="1">
                    <c r="A1" s="1"><v>A</v></c><c r="B1" s="2"><v>B</v></c>
                    <c r="C1" s="3"><v>C</v></c><c r="D1" s="4"><v>D</v></c>
                    <c r="E1" s="5"><v>E</v></c>
                  </row>
                  <row r="2" s="2" hidden="1"/>
                </sheetData></worksheet>
            """,
            stylesXml = styles(
                """
                    <fill><patternFill patternType="solid"><fgColor theme="4" tint="0.5"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor indexed="2"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor indexed="64"/><bgColor rgb="0000FF00"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor rgb="00FF0000"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><bgColor indexed="10"/></patternFill></fill>
                """,
                xfFillIds = listOf(0, 1, 2, 3, 4, 5),
                indexed = """
                    <indexedColors>
                      <rgbColor rgb="FF000000"/><rgbColor rgb="FFFFFFFF"/><rgbColor rgb="FF112233"/>
                    </indexedColors>
                """,
            ),
            themeXml = """
                <a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                  <a:themeElements><a:clrScheme name="Demo">
                    <a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1>
                    <a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1>
                    <a:accent1><a:srgbClr val="000000"/></a:accent1>
                  </a:clrScheme></a:themeElements>
                </a:theme>
            """,
        )
        assertEquals(0xFF808080.toInt(), sheet.colorAt(0, 0)) // noir du thème éclairci de 50 %
        assertEquals(0xFF112233.toInt(), sheet.colorAt(0, 1)) // palette personnalisée
        assertEquals(0xFF00FF00.toInt(), sheet.colorAt(0, 2)) // fgColor index 64 = auto => bgColor
        assertEquals(0xFFFF0000.toInt(), sheet.colorAt(0, 3)) // alpha OOXML 00 ignoré pour un fond
        assertEquals(0xFFFF0000.toInt(), sheet.colorAt(0, 4)) // palette standard index 10
        assertEquals(0xFF112233.toInt(), sheet.colorAt(1, 0)) // style de ligne sans <c>
        assertEquals(0xFFFF0000.toInt(), sheet.colorAt(0, 5)) // style de colonne sans <c>
        assertEquals(6, sheet.columnCount)
        assertEquals(2, sheet.rowCount)
    }

    @Test
    fun standardIndexedPaletteStartsWithBlackThenWhite() {
        val sheet = readSheet(
            sheetXml = """<worksheet><sheetData><row r="1">
                <c r="A1" s="1"><v>1</v></c><c r="B1" s="2"><v>2</v></c>
                </row></sheetData></worksheet>""",
            stylesXml = styles(
                """
                    <fill><patternFill patternType="solid"><fgColor indexed="0"/></patternFill></fill>
                    <fill><patternFill patternType="solid"><fgColor indexed="1"/></patternFill></fill>
                """, listOf(0, 1, 2),
            ),
        )
        assertEquals(0xFF000000.toInt(), sheet.colorAt(0, 0))
        assertEquals(0xFFFFFFFF.toInt(), sheet.colorAt(0, 1))
    }

    @Test
    fun decorativeEmptyMergeDoesNotCreateThousandsOfBlankRows() {
        val sheet = readSheet(
            sheetXml = """<worksheet><sheetData><row r="1"><c r="A1"><v>1</v></c></row></sheetData>
                <mergeCells><mergeCell ref="C5:D200000"/></mergeCells></worksheet>""",
            stylesXml = styles("", listOf(0)),
        )
        assertEquals(1, sheet.rowCount)
    }

    @Test
    fun conditionalColorsRespectPrioritiesContainsTextExpressionsAndTwoColorScale() {
        val sheet = readSheet(
            sheetXml = """
                <worksheet><sheetData>
                  <row r="1"><c r="A1"><v>0</v></c><c r="B1" t="inlineStr"><is><t>alpha</t></is></c></row>
                  <row r="2"><c r="A2"><v>10</v></c><c r="B2" t="inlineStr"><is><t>Xylophone</t></is></c></row>
                  <row r="3"><c r="A3"><v>20</v></c><c r="B3" t="inlineStr"><is><t>beta</t></is></c></row>
                  <row r="4"><c r="A4"><v>30</v></c><c r="B4" t="inlineStr"><is><t>x</t></is></c></row>
                </sheetData>
                <conditionalFormatting sqref="A1:A4">
                  <cfRule type="colorScale" priority="5"><colorScale>
                    <cfvo type="min"/><cfvo type="max"/>
                    <color rgb="FFFF0000"/><color rgb="FF00FF00"/>
                  </colorScale></cfRule>
                  <cfRule type="cellIs" operator="greaterThan" priority="1" dxfId="0"><formula>20</formula></cfRule>
                </conditionalFormatting>
                <conditionalFormatting sqref="B1:B4">
                  <cfRule type="containsText" text="x" priority="2" dxfId="1"/>
                </conditionalFormatting>
                <conditionalFormatting sqref="C1:C4">
                  <cfRule type="expression" priority="3" dxfId="2">
                    <formula>ISNUMBER(SEARCH("x",&#36;B1))</formula>
                  </cfRule>
                </conditionalFormatting>
                </worksheet>
            """,
            stylesXml = styles("", listOf(0), dxfs = """
                <dxf><fill><patternFill patternType="solid"><fgColor rgb="FF0000FF"/></patternFill></fill></dxf>
                <dxf><fill><patternFill patternType="solid"><bgColor rgb="FFFFCC00"/></patternFill></fill></dxf>
                <dxf><fill><patternFill patternType="solid"><fgColor rgb="FF66CCFF"/></patternFill></fill></dxf>
            """),
        )
        assertEquals(0xFFFF0000.toInt(), sheet.colorAt(0, 0))
        assertEquals(0xFFAA5500.toInt(), sheet.colorAt(1, 0))
        assertEquals(0xFF55AA00.toInt(), sheet.colorAt(2, 0))
        assertEquals(0xFF0000FF.toInt(), sheet.colorAt(3, 0)) // priorité 1 sur le dégradé
        assertEquals(0xFFFFCC00.toInt(), sheet.colorAt(1, 1))
        assertEquals(0xFFFFCC00.toInt(), sheet.colorAt(3, 1))
        assertEquals(0xFF66CCFF.toInt(), sheet.colorAt(1, 2)) // C2 est vide, dépend de $B2
        assertEquals(0xFF66CCFF.toInt(), sheet.colorAt(3, 2))
        assertEquals(3, sheet.columnCount)
        assertEquals(null, sheet.colorAt(0, 2))
        assertEquals(null, sheet.colorAt(2, 1))
    }

    @Test
    fun threeColorScaleUsesMiddleThresholdAndUnsupportedRulesAreIgnored() {
        val sheet = readSheet(
            sheetXml = """
                <worksheet><sheetData>
                  <row r="1"><c r="A1"><v>0</v></c></row>
                  <row r="2"><c r="A2"><v>50</v></c></row>
                  <row r="3"><c r="A3"><v>100</v></c></row>
                </sheetData><conditionalFormatting sqref="A1:A3">
                  <cfRule type="expression" priority="1" dxfId="0"><formula>MY_CUSTOM_FUNCTION()</formula></cfRule>
                  <cfRule type="colorScale" priority="2"><colorScale>
                    <cfvo type="min"/><cfvo type="percentile" val="50"/><cfvo type="max"/>
                    <color rgb="FFFF0000"/><color rgb="FFFFFF00"/><color rgb="FF00FF00"/>
                  </colorScale></cfRule>
                </conditionalFormatting></worksheet>
            """,
            stylesXml = styles("", listOf(0), dxfs = """
                <dxf><fill><patternFill patternType="solid"><fgColor rgb="FF0000FF"/></patternFill></fill></dxf>
            """),
        )
        assertEquals(0xFFFF0000.toInt(), sheet.colorAt(0, 0))
        assertEquals(0xFFFFFF00.toInt(), sheet.colorAt(1, 0))
        assertEquals(0xFF00FF00.toInt(), sheet.colorAt(2, 0))
        assertFalse(sheet.cellColors.values.contains(0xFF0000FF.toInt()))
    }

    @Test
    fun stopIfTrueBlocksLowerPriorityFillEvenWhenFirstRuleHasNoFill() {
        val sheet = readSheet(
            sheetXml = """
                <worksheet><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>X</t></is></c></row></sheetData>
                  <conditionalFormatting sqref="A1">
                    <cfRule type="containsText" text="x" priority="1" stopIfTrue="1"/>
                    <cfRule type="containsText" text="x" priority="2" dxfId="0"/>
                  </conditionalFormatting>
                </worksheet>
            """,
            stylesXml = styles("", listOf(0), dxfs = """
                <dxf><fill><patternFill patternType="solid"><fgColor rgb="FF00FF00"/></patternFill></fill></dxf>
            """),
        )
        assertEquals(null, sheet.colorAt(0, 0))
    }

    private fun readSheet(
        sheetXml: String,
        stylesXml: String,
        themeXml: String? = null,
        drawingXml: String? = null,
    ): SheetData {
        val workbook = """
            <workbook xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets><sheet name="Démo" sheetId="1" r:id="rIdSheet"/></sheets>
            </workbook>
        """.trimIndent()
        val relationships = """
            <Relationships>
              <Relationship Id="rIdSheet" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
              <Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
              <Relationship Id="rIdTheme" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml"/>
            </Relationships>
        """.trimIndent()
        val entries = mutableMapOf(
            "xl/workbook.xml" to workbook.toByteArray(),
            "xl/_rels/workbook.xml.rels" to relationships.toByteArray(),
            "xl/worksheets/sheet1.xml" to sheetXml.trimIndent().toByteArray(),
            "xl/styles.xml" to stylesXml.trimIndent().toByteArray(),
        )
        themeXml?.let { entries["xl/theme/theme1.xml"] = it.trimIndent().toByteArray() }
        drawingXml?.let {
            entries["xl/worksheets/_rels/sheet1.xml.rels"] = """
                <Relationships><Relationship Id="rIdDrawing" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing" Target="../drawings/drawing1.xml"/></Relationships>
            """.trimIndent().toByteArray()
            entries["xl/drawings/drawing1.xml"] = it.trimIndent().toByteArray()
            entries["xl/drawings/_rels/drawing1.xml.rels"] = """
                <Relationships><Relationship Id="rIdPicture" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/image1.png"/></Relationships>
            """.trimIndent().toByteArray()
            entries["xl/media/image1.png"] = byteArrayOf(1, 2, 3)
        }
        return ExcelReader.readXlsxEntries(entries).sheets.single()
    }

    private fun styles(fills: String, xfFillIds: List<Int>, indexed: String = "", dxfs: String = ""): String = """
        <styleSheet>
          <colors>$indexed</colors>
          <fills count="${xfFillIds.size}"><fill><patternFill patternType="none"/></fill>$fills</fills>
          <cellXfs count="${xfFillIds.size}">${xfFillIds.joinToString("") { "<xf fillId=\"$it\"/>" }}</cellXfs>
          <dxfs>$dxfs</dxfs>
        </styleSheet>
    """.trimIndent()
}

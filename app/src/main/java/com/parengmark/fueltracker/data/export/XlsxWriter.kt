package com.parengmark.fueltracker.data.export

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a minimal, valid single-sheet .xlsx (OOXML SpreadsheetML) by hand --
 * deliberately NOT using Apache POI. POI is heavy and has a long history of
 * runtime trouble on Android (missing javax.xml.stream / org.w3c.dom pieces
 * that desktop JVMs have and ART doesn't always provide the same way), and
 * this project's build can't be verified in the environment that generated
 * it -- a dependency that compiles but crashes at runtime on-device would be
 * a worse outcome than fewer features. A single-sheet workbook with inline
 * strings (no sharedStrings.xml, no styles.xml) is a well-defined, small
 * enough OOXML subset to get right without that verification.
 */
object XlsxWriter {

    class Sheet(val name: String, val headers: List<String>, val rows: List<List<Any?>>)

    fun write(
        outputStream: OutputStream,
        sheetName: String,
        headers: List<String>,
        rows: List<List<Any?>>
    ) = writeSheets(outputStream, listOf(Sheet(sheetName, headers, rows)))

    /** One workbook, one tab per [Sheet]. */
    fun writeSheets(outputStream: OutputStream, sheets: List<Sheet>) {
        require(sheets.isNotEmpty()) { "A workbook needs at least one sheet" }
        ZipOutputStream(outputStream).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            entry("[Content_Types].xml", contentTypesXml(sheets.size))
            entry("_rels/.rels", PACKAGE_RELS)
            entry("xl/workbook.xml", workbookXml(sheets.map { it.name }))
            entry("xl/_rels/workbook.xml.rels", workbookRelsXml(sheets.size))
            sheets.forEachIndexed { i, sheet ->
                entry("xl/worksheets/sheet${i + 1}.xml", sheetXml(sheet.headers, sheet.rows))
            }
        }
    }

    private fun workbookXml(sheetNames: List<String>): String {
        val sheetsXml = sheetNames.mapIndexed { i, name ->
            """<sheet name="${escapeXml(name.take(31))}" sheetId="${i + 1}" r:id="rId${i + 1}"/>"""
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
            """xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
            "<sheets>$sheetsXml</sheets></workbook>"
    }

    private fun contentTypesXml(sheetCount: Int): String {
        val overrides = (1..sheetCount).joinToString("") { i ->
            """<Override PartName="/xl/worksheets/sheet$i.xml" """ +
                """ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
            """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
            """<Default Extension="xml" ContentType="application/xml"/>""" +
            """<Override PartName="/xl/workbook.xml" """ +
            """ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
            overrides + "</Types>"
    }

    private fun workbookRelsXml(sheetCount: Int): String {
        val rels = (1..sheetCount).joinToString("") { i ->
            """<Relationship Id="rId$i" """ +
                """Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" """ +
                """Target="worksheets/sheet$i.xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            rels + "</Relationships>"
    }

    private fun sheetXml(headers: List<String>, rows: List<List<Any?>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")

        sb.append(rowXml(1, headers.map { it as Any? }))
        rows.forEachIndexed { index, row -> sb.append(rowXml(index + 2, row)) }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun rowXml(rowNumber: Int, values: List<Any?>): String {
        val sb = StringBuilder("""<row r="$rowNumber">""")
        values.forEachIndexed { colIndex, value ->
            val ref = "${columnLetter(colIndex)}$rowNumber"
            sb.append(cellXml(ref, value))
        }
        sb.append("</row>")
        return sb.toString()
    }

    private fun cellXml(ref: String, value: Any?): String = when (value) {
        null -> """<c r="$ref"/>"""
        is Number -> """<c r="$ref"><v>${value}</v></c>"""
        is Boolean -> """<c r="$ref" t="inlineStr"><is><t>${if (value) "yes" else "no"}</t></is></c>"""
        else -> """<c r="$ref" t="inlineStr"><is><t>${escapeXml(value.toString())}</t></is></c>"""
    }

    /** 0-based column index -> spreadsheet column letters (0 -> A, 25 -> Z, 26 -> AA, ...). */
    private fun columnLetter(index: Int): String {
        var i = index
        val sb = StringBuilder()
        do {
            sb.insert(0, ('A' + (i % 26)))
            i = i / 26 - 1
        } while (i >= 0)
        return sb.toString()
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")


    private val PACKAGE_RELS = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

}

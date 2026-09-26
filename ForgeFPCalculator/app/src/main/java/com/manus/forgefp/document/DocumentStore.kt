package com.manus.forgefp.document

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Métadonnées d'un document enregistré localement. */
data class StoredDocument(
    val id: String,
    val displayName: String,
    val kind: DocumentKind,
    val sizeBytes: Long,
    val savedAt: Long,
    val file: File,
)

/**
 * Stockage local des documents importés.
 *
 * Chaque document est copié dans `filesDir/documents/` et référencé dans un index
 * JSON (`index.json`). L'utilisateur peut ainsi retrouver, rouvrir ou supprimer
 * ses documents sans devoir les re-sélectionner depuis le système.
 */
object DocumentStore {

    private const val DIR_NAME = "documents"
    private const val INDEX_NAME = "index.json"

    private fun dir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    private fun indexFile(context: Context): File = File(dir(context), INDEX_NAME)

    /** Enregistre un document importé et renvoie ses métadonnées. */
    fun save(context: Context, uri: Uri, displayName: String, kind: DocumentKind): StoredDocument? {
        return runCatching {
            val id = "doc_" + System.currentTimeMillis() + "_" + (0..9999).random()
            val originalExtension = displayName.substringAfterLast('.', "").lowercase()
            val extension = when {
                kind == DocumentKind.PDF -> "pdf"
                originalExtension in setOf("xls", "xlsx", "ods") -> originalExtension
                runCatching { context.contentResolver.getType(uri)?.lowercase() }.getOrNull() == "application/vnd.ms-excel" -> "xls"
                else -> "xlsx"
            }
            val target = File(dir(context), "$id.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            val doc = StoredDocument(
                id = id,
                displayName = displayName.ifBlank { "Document" },
                kind = kind,
                sizeBytes = target.length(),
                savedAt = System.currentTimeMillis(),
                file = target,
            )
            val list = loadIndex(context).toMutableList()
            list.add(0, doc)
            writeIndex(context, list)
            doc
        }.getOrNull()
    }

    /** Liste les documents enregistrés, du plus récent au plus ancien. */
    fun list(context: Context): List<StoredDocument> {
        val list = loadIndex(context)
        // Nettoie les entrées dont le fichier a disparu.
        val existing = list.filter { it.file.exists() }
        if (existing.size != list.size) writeIndex(context, existing)
        return existing.sortedByDescending { it.savedAt }
    }

    /** Supprime un document enregistré (fichier + entrée d'index). */
    fun delete(context: Context, id: String) {
        val list = loadIndex(context)
        val target = list.firstOrNull { it.id == id }
        target?.file?.delete()
        writeIndex(context, list.filterNot { it.id == id })
    }

    // ------------------------------------------------------------- index JSON

    private fun loadIndex(context: Context): List<StoredDocument> {
        val file = indexFile(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("displayName")
                val kind = if (obj.getString("kind") == "PDF") DocumentKind.PDF else DocumentKind.EXCEL
                val size = obj.optLong("sizeBytes", 0L)
                val savedAt = obj.optLong("savedAt", 0L)
                val fileName = obj.getString("fileName")
                val f = File(dir(context), fileName)
                StoredDocument(id, name, kind, size, savedAt, f)
            }
        }.getOrElse { emptyList() }
    }

    private fun writeIndex(context: Context, documents: List<StoredDocument>) {
        val array = JSONArray()
        documents.forEach { doc ->
            array.put(
                JSONObject().apply {
                    put("id", doc.id)
                    put("displayName", doc.displayName)
                    put("kind", doc.kind.name)
                    put("sizeBytes", doc.sizeBytes)
                    put("savedAt", doc.savedAt)
                    put("fileName", doc.file.name)
                },
            )
        }
        runCatching { indexFile(context).writeText(array.toString()) }
    }
}

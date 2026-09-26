package com.manus.forgefp.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor

/**
 * Lecteur PDF basé sur le moteur natif Android [PdfRenderer].
 *
 * Le fichier importé est copié dans le cache de l'application afin d'obtenir un
 * descripteur de fichier exploitable par PdfRenderer, puis chaque page peut être
 * rendue en bitmap à la résolution demandée.
 */
class PdfReader(context: Context, uri: Uri) : AutoCloseable {

    private val pfd: ParcelFileDescriptor
    private val renderer: PdfRenderer

    val pageCount: Int

    init {
        val cacheFile = java.io.File(context.cacheDir, "imported_document.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Impossible d'ouvrir le fichier PDF.")

        pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(pfd)
        pageCount = renderer.pageCount
    }

    /** Rend une page (index base 0) en bitmap à la largeur cible donnée. */
    fun renderPage(index: Int, targetWidth: Int): Bitmap {
        renderer.openPage(index).use { page ->
            val ratio = page.height.toFloat() / page.width.toFloat()
            val width = targetWidth.coerceAtLeast(1)
            val height = (width * ratio).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }

    override fun close() {
        renderer.close()
        pfd.close()
    }
}

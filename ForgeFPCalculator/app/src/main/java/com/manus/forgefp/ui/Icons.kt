package com.manus.forgefp.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Icônes vectorielles Material Symbols définies localement (aucune dépendance
 * supplémentaire). Chaque chemin est au format SVG "pathData".
 */
private fun materialIcon(name: String, pathData: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    val node = PathParser().parsePathString(pathData).toNodes()
    builder.addPath(pathData = node, fill = SolidColor(Color.Black))
    return builder.build()
}

/** « file_open » : import de document. */
val ImportDocumentIcon: ImageVector by lazy {
    materialIcon(
        "ImportDocument",
        "M6 2c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6H6zm7 7V3.5L18.5 9H13z",
    )
}

/** « folder_open » : documents enregistrés. */
val FolderIcon: ImageVector by lazy {
    materialIcon(
        "Folder",
        "M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2z",
    )
}

/** « close ». */
val CloseIcon: ImageVector by lazy {
    materialIcon(
        "Close",
        "M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z",
    )
}

/** « delete ». */
val DeleteIcon: ImageVector by lazy {
    materialIcon(
        "Delete",
        "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
    )
}

/** « search ». */
val SearchIcon: ImageVector by lazy {
    materialIcon(
        "Search",
        "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
    )
}

/** « zoom_in ». */
val ZoomInIcon: ImageVector by lazy {
    materialIcon(
        "ZoomIn",
        "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14zm2.5-4h-2v2H9v-2H7V9h2V7h1v2h2v1z",
    )
}

/** « zoom_out ». */
val ZoomOutIcon: ImageVector by lazy {
    materialIcon(
        "ZoomOut",
        "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14zM7 9h5v1H7z",
    )
}

/** « description » : document générique. */
val DocumentIcon: ImageVector by lazy {
    materialIcon(
        "Document",
        "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z",
    )
}

/** « picture_as_pdf ». */
val PdfIcon: ImageVector by lazy {
    materialIcon(
        "Pdf",
        "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM9.5 9.5c0 .83-.67 1.5-1.5 1.5H7v2H5.5V7H8c.83 0 1.5.67 1.5 1.5v1zm5 2c0 .83-.67 1.5-1.5 1.5h-2.5V7H13c.83 0 1.5.67 1.5 1.5v3zm4-3H17v1h1.5V11H17v2h-1.5V7h3v1.5zM7 9.5h1v-1H7v1zm5 2.5h1V8.5h-1V12z",
    )
}

/** « table_chart » : feuille de calcul. */
val TableIcon: ImageVector by lazy {
    materialIcon(
        "Table",
        "M10 10.02h5V21h-5zM17 21h3c1.1 0 2-.9 2-2v-9h-5v11zm3-18H5c-1.1 0-2 .9-2 2v3h19V5c0-1.1-.9-2-2-2zM3 19c0 1.1.9 2 2 2h3V10H3v9z",
    )
}

/** « arrow_back ». */
val BackIcon: ImageVector by lazy {
    materialIcon(
        "Back",
        "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z",
    )
}

/** « menu » : liste des feuilles. */
val MenuIcon: ImageVector by lazy {
    materialIcon(
        "Menu",
        "M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z",
    )
}

/** « chevron_left » : cellule précédente. */
val ChevronLeftIcon: ImageVector by lazy {
    materialIcon(
        "ChevronLeft",
        "M15.41 7.41 14 6l-6 6 6 6 1.41-1.41L10.83 12z",
    )
}

/** « chevron_right » : cellule suivante. */
val ChevronRightIcon: ImageVector by lazy {
    materialIcon(
        "ChevronRight",
        "M8.59 16.59 13.17 12 8.59 7.41 10 6l6 6-6 6z",
    )
}

/** « expand_more » : ouvrir un sélecteur. */
val ExpandMoreIcon: ImageVector by lazy {
    materialIcon(
        "ExpandMore",
        "M16.59 8.59 12 13.17 7.41 8.59 6 10l6 6 6-6z",
    )
}

/** « check » : validation, indicateur positif. */
val CheckIcon: ImageVector by lazy {
    materialIcon(
        "Check",
        "M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z",
    )
}

/** « content_copy » : copier. */
val CopyIcon: ImageVector by lazy {
    materialIcon(
        "Copy",
        "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z",
    )
}

/** « share » : partager. */
val ShareIcon: ImageVector by lazy {
    materialIcon(
        "Share",
        "M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92s2.92-1.31 2.92-2.92-1.31-2.92-2.92-2.92z",
    )
}

/** « info » : information, aide. */
val InfoIcon: ImageVector by lazy {
    materialIcon(
        "Info",
        "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z",
    )
}

/** « warning » : avertissement (données manquantes). */
val WarningIcon: ImageVector by lazy {
    materialIcon(
        "Warning",
        "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z",
    )
}

/** « error » : état critique. */
val ErrorIcon: ImageVector by lazy {
    materialIcon(
        "Error",
        "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z",
    )
}

/** « refresh » : recalculer / actualiser. */
val RefreshIcon: ImageVector by lazy {
    materialIcon(
        "Refresh",
        "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z",
    )
}

/** « calculate » : calculateur. */
val CalculatorIcon: ImageVector by lazy {
    materialIcon(
        "Calculator",
        "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-6 16h-2v-2h2v2zm0-4h-2v-2h2v2zm0-4h-2V9h2v2zm4 8h-2v-2h2v2zm0-4h-2v-2h2v2zm0-4h-2V9h2v2zM7 7h10v2H7V7z",
    )
}

/** « apps » : grille / catalogue. */
val GridIcon: ImageVector by lazy {
    materialIcon(
        "Grid",
        "M4 8h4V4H4v4zm6 12h4v-4h-4v4zm-6 0h4v-4H4v4zm0-6h4v-4H4v4zm6 0h4v-4h-4v4zm6-10v4h4V4h-4zm-6 4h4V4h-4v4zm6 6h4v-4h-4v4zm0 6h4v-4h-4v4z",
    )
}

/** « apartment » : placeholder neutre pour un GB sans image. */
val BuildingIcon: ImageVector by lazy {
    materialIcon(
        "Building",
        "M17 11V3H7v4H3v14h8v-4h2v4h8V11h-4zM7 19H5v-2h2v2zm0-4H5v-2h2v2zm0-4H5V9h2v2zm4 4H9v-2h2v2zm0-4H9V9h2v2zm0-4H9V5h2v2zm4 8h-2v-2h2v2zm0-4h-2V9h2v2zm0-4h-2V5h2v2zm4 12h-2v-2h2v2zm0-4h-2v-2h2v2z",
    )
}

/** « auto_awesome » : assistant / suggestion. */
val SparkleIcon: ImageVector by lazy {
    materialIcon(
        "Sparkle",
        "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z",
    )
}

/** « verified_user » : protection anti-snipe. */
val ShieldIcon: ImageVector by lazy {
    materialIcon(
        "Shield",
        "M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z",
    )
}

/** « person » : joueur. */
val PersonIcon: ImageVector by lazy {
    materialIcon(
        "Person",
        "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z",
    )
}

/** « trending_up » : progression des niveaux. */
val TrendingUpIcon: ImageVector by lazy {
    materialIcon(
        "TrendingUp",
        "M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z",
    )
}

/** « history » : ordre recommandé. */
val HistoryIcon: ImageVector by lazy {
    materialIcon(
        "History",
        "M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z",
    )
}

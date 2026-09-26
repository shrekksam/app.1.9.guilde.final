package com.manus.forgefp.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GreatBuildingCatalog(
    val source: String,
    val buildingCount: Int,
    val buildings: List<GreatBuilding>,
)

@Serializable
data class GreatBuilding(
    val id: String,
    val name: String,
    val levels: List<GreatBuildingLevel>,
)

@Serializable
data class GreatBuildingLevel(
    val level: Int,
    val cost: Int,
    val rewards: List<String>,
) {
    fun rewardsAsDouble(): List<Double> = rewards.map { it.toDouble() }
}

/**
 * Mapping des images de Grands Bâtiments : identifiant du GB vers un chemin
 * d'asset (ex. « gb/The_Arc.webp »). Un GB absent de ce mapping n'a pas
 * d'image fournie : l'interface affiche alors un placeholder, jamais une
 * image inventée.
 */
@Serializable
data class GreatBuildingImageMap(
    val source: String = "",
    val imageCount: Int = 0,
    val placeholder: String? = null,
    val images: Map<String, String> = emptyMap(),
) {
    fun imageFor(buildingId: String): String? = images[buildingId]
}

class GreatBuildingRepository(private val context: Context) {
    private val parser = Json { ignoreUnknownKeys = true }

    fun loadCatalog(): GreatBuildingCatalog = context.assets.open("great_buildings.json")
        .bufferedReader()
        .use { parser.decodeFromString<GreatBuildingCatalog>(it.readText()) }

    fun loadImageMap(): GreatBuildingImageMap = runCatching {
        context.assets.open("gb_images.json").bufferedReader().use {
            parser.decodeFromString<GreatBuildingImageMap>(it.readText())
        }
    }.getOrElse { GreatBuildingImageMap() }
}

// Landmark.kt
package com.example.geographyquiz.data

data class Landmark(
    val name: String,
    val translatedName: String = name,
    val imagePath: String // e.g., "landmark/france/eiffel_tower.jpg"
)
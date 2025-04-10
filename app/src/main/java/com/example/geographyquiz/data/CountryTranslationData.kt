package com.example.geographyquiz.data

data class CountryTranslationData(
    val originalName: String,
    val translatedName: String,
    val translatedCapital: String,
    val translatedCity1: String,
    val translatedCity2: String?,
    val translatedCity3: String?,
    val translatedContinent: String,
    val translatedRegion: String,
    val translatedCurrency: String,
    val translatedLanguages: String
)
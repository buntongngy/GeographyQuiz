package com.example.geographyquiz.data

import com.example.geographyquiz.data.CountryDatabase.LandmarkData

data class CountryData(
    val name: String,
    val capital: String,
    val bigCity: String,
    val secondCity: String?,
    val thirdCity: String?,
    val continent: String,
    val region: String,
    val languages: List<String>?,
    val currency: String,
    val population: Long,
    val area: Long,
    val category: String,
    val countryCode: String,
    val landmarks: List<LandmarkData>? = null,
    val difficulty: Int,
)

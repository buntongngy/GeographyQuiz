// Country.kt
package com.example.geographyquiz.data

import android.database.Cursor
import androidx.core.database.getStringOrNull

data class Country(
    val id: Int,
    val name: String,
    val capital: String,
    val bigCity: String,
    val secondCity: String?,
    val thirdCity: String?,
    val continent: String,
    val region: String,
    val languages: List<String>,
    val currency: String,
    val population: Int,
    val area: Int,
    val category: String,
    val countryCode: String,
    val landmarks: List<Landmark> = emptyList(),
    val difficulty: Int,
    val flagColors: List<String> = emptyList(),
    val flagEmblem: List<String> = emptyList(),
    // Translation fields
    val translatedName: String = name,
    val translatedCapital: String = capital,
    val translatedBigCity: String = bigCity,
    val translatedSecondCity: String? = secondCity,
    val translatedThirdCity: String? = thirdCity,
    val translatedContinent: String = continent,
    val translatedRegion: String = region,
    val translatedCurrency: String = currency,
    val translatedLanguages: List<String> = languages,
    val translatedLandmarks: List<Landmark> = landmarks
) {
    companion object {
        fun fromCursor(cursor: Cursor): Country {
            val languagesString = cursor.getString(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_LANG))
            val languagesList = languagesString.split(",").map { it.trim() }
            val colorsString = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_FLAG_COLORS)) ?: ""
            val emblemsString = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_FLAG_EMBLEM)) ?: ""

            return Country(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_NAME)),
                capital = cursor.getString(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_CAPITAL)),
                bigCity = cursor.getString(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_BIGGEST_CITY)),
                secondCity = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_SECOND_CITY)),
                thirdCity = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_THIRD_CITY)),
                continent = cursor.getString(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_CONTINENT)),
                region = cursor.getString(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_REGION)),
                languages = languagesList,
                currency = cursor.getString(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_CURRENCY)),
                population = cursor.getInt(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_POPULATION)),
                area = cursor.getInt(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_AREA)),
                category = cursor.getString(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_CATEGORY)),
                countryCode = cursor.getString(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_CODE)),
                difficulty = cursor.getInt(cursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_DIFFICULTY)),
                flagColors = colorsString.split(",").map {it.trim()}.filter { it.isNotEmpty() },
                flagEmblem = emblemsString.split(",").map {it.trim()}.filter { it.isNotEmpty() }
            )
        }

        fun withTranslations(base: Country, translationCursor: Cursor): Country {
            val translatedLangsString = translationCursor.getString(
                translationCursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_TRANSLATED_LANG))
            val translatedLangsList = translatedLangsString.split(",").map { it.trim() }

            return base.copy(
                translatedName = translationCursor.getString(
                    translationCursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_TRANSLATED_NAME)),
                translatedCapital = translationCursor.getString(
                    translationCursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_TRANSLATED_CAPITAL)),
                translatedBigCity = translationCursor.getString(
                    translationCursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_TRANSLATED_CITY1)),
                translatedSecondCity = translationCursor.getStringOrNull(
                    translationCursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_TRANSLATED_CITY2)),
                translatedThirdCity = translationCursor.getStringOrNull(
                    translationCursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_TRANSLATED_CITY3)),
                translatedContinent = translationCursor.getString(
                    translationCursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_TRANSLATED_CONTINENT)),
                translatedRegion = translationCursor.getString(
                    translationCursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_TRANSLATED_REGION)),
                translatedCurrency = translationCursor.getString(
                    translationCursor.getColumnIndexOrThrow(CountryDatabase.COLUMN_TRANSLATED_CURRENCY)),
                translatedLanguages = translatedLangsList
            )
        }
    }

    // Helper methods
    fun getDisplayName(languageCode: String): String {
        return if (languageCode != "en" && translatedName.isNotEmpty()) translatedName else name
    }

    fun getDisplayCapital(languageCode: String): String {
        return if (languageCode != "en" && translatedCapital.isNotEmpty()) translatedCapital else capital
    }

    fun getDisplayLanguages(languageCode: String): String {
        return if (languageCode != "en" && translatedLanguages.isNotEmpty()) {
            translatedLanguages.joinToString(", ")
        } else {
            languages.joinToString(", ")
        }
    }

    fun getDisplayLandmarks(languageCode: String): List<Landmark> {
        return if (languageCode != "en" && translatedLandmarks.isNotEmpty()) {
            translatedLandmarks
        } else {
            landmarks
        }
    }
}
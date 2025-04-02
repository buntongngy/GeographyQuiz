package com.example.geographyquiz.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class CountryDatabase(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    private val appContext = context.applicationContext
    private val gson = Gson()

    companion object {
        // Database constants
        private const val TAG = "CountryDatabase"
        private const val DATABASE_NAME = "world_countries.db"
        private const val DATABASE_VERSION = 2

        // Countries table
        const val TABLE_COUNTRIES = "countries"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_CAPITAL = "capital"
        const val COLUMN_BIGGEST_CITY = "city1"
        const val COLUMN_SECOND_CITY = "city2"
        const val COLUMN_THIRD_CITY = "city3"
        const val COLUMN_CONTINENT = "continent"
        const val COLUMN_REGION = "region"
        const val COLUMN_LANG = "languages"
        const val COLUMN_CURRENCY = "currency"
        const val COLUMN_POPULATION = "population"
        const val COLUMN_AREA = "area"

        // Translation table
        const val TABLE_COUNTRY_TRANSLATION = "country_translation"
        const val COLUMN_COUNTRY_ID = "country_id"
        const val COLUMN_LANGUAGE_CODE = "language_code"
        const val COLUMN_TRANSLATED_NAME = "translated_name"
        const val COLUMN_TRANSLATED_CAPITAL = "translated_capital"
        const val COLUMN_TRANSLATED_CITY1 = "translated_city1"
        const val COLUMN_TRANSLATED_CITY2 = "translated_city2"
        const val COLUMN_TRANSLATED_CITY3 = "translated_city3"
        const val COLUMN_TRANSLATED_CONTINENT = "translated_continent"
        const val COLUMN_TRANSLATED_REGION = "translated_region"
        const val COLUMN_TRANSLATED_CURRENCY = "translated_currency"
        const val COLUMN_TRANSLATED_LANG = "translated_languages"
    }

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
        createIndexes(db)
        refreshAllCountries(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_COUNTRIES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_COUNTRY_TRANSLATION")
            onCreate(db)
        }
    }

    private fun createTables(db: SQLiteDatabase) {
        // Main countries table
        db.execSQL("""
            CREATE TABLE $TABLE_COUNTRIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL UNIQUE,
                $COLUMN_CAPITAL TEXT NOT NULL,
                $COLUMN_BIGGEST_CITY TEXT NOT NULL,
                $COLUMN_SECOND_CITY TEXT,
                $COLUMN_THIRD_CITY TEXT,
                $COLUMN_CONTINENT TEXT NOT NULL,
                $COLUMN_REGION TEXT NOT NULL,
                $COLUMN_LANG TEXT NOT NULL,
                $COLUMN_CURRENCY TEXT NOT NULL,
                $COLUMN_POPULATION INTEGER NOT NULL,
                $COLUMN_AREA INTEGER NOT NULL
            )
        """.trimIndent())

        // Translation table
        db.execSQL("""
            CREATE TABLE $TABLE_COUNTRY_TRANSLATION (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_COUNTRY_ID INTEGER NOT NULL,
                $COLUMN_LANGUAGE_CODE TEXT NOT NULL,
                $COLUMN_TRANSLATED_NAME TEXT NOT NULL,
                $COLUMN_TRANSLATED_CAPITAL TEXT NOT NULL,
                $COLUMN_TRANSLATED_CITY1 TEXT NOT NULL,
                $COLUMN_TRANSLATED_CITY2 TEXT,
                $COLUMN_TRANSLATED_CITY3 TEXT,
                $COLUMN_TRANSLATED_CONTINENT TEXT NOT NULL,
                $COLUMN_TRANSLATED_REGION TEXT NOT NULL,
                $COLUMN_TRANSLATED_CURRENCY TEXT NOT NULL,
                $COLUMN_TRANSLATED_LANG TEXT NOT NULL,
                FOREIGN KEY($COLUMN_COUNTRY_ID) REFERENCES $TABLE_COUNTRIES($COLUMN_ID),
                UNIQUE($COLUMN_COUNTRY_ID, $COLUMN_LANGUAGE_CODE)
            )
        """.trimIndent())
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_country_name ON $TABLE_COUNTRIES($COLUMN_NAME)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_country_continent ON $TABLE_COUNTRIES($COLUMN_CONTINENT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_country_region ON $TABLE_COUNTRIES($COLUMN_REGION)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_translation_country ON $TABLE_COUNTRY_TRANSLATION($COLUMN_COUNTRY_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_translation_language ON $TABLE_COUNTRY_TRANSLATION($COLUMN_LANGUAGE_CODE)")
    }

    private fun refreshAllCountries(db: SQLiteDatabase) {
        val countries = loadCountriesFromJson()
        val translations = loadAllTranslations()

        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM $TABLE_COUNTRIES")
            db.execSQL("DELETE FROM $TABLE_COUNTRY_TRANSLATION")

            for (country in countries) {
                val countryId = insertCountry(db, country)
                insertTranslations(db, countryId, country.name, translations)
            }
            db.setTransactionSuccessful()
            Log.d(TAG, "Loaded ${countries.size} countries with ${translations.size} language translations")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing countries", e)
        } finally {
            db.endTransaction()
        }
    }

    private fun insertCountry(db: SQLiteDatabase, country: CountryData): Long {
        val values = ContentValues().apply {
            put(COLUMN_NAME, country.name)
            put(COLUMN_CAPITAL, country.capital)
            put(COLUMN_BIGGEST_CITY, country.bigCity)
            put(COLUMN_SECOND_CITY, country.secondCity)
            put(COLUMN_THIRD_CITY, country.thirdCity)
            put(COLUMN_CONTINENT, country.continent)
            put(COLUMN_REGION, country.region)
            put(COLUMN_LANG, country.languages.joinToString(","))
            put(COLUMN_CURRENCY, country.currency)
            put(COLUMN_POPULATION, country.population)
            put(COLUMN_AREA, country.area)
        }
        return db.insertWithOnConflict(TABLE_COUNTRIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun insertTranslations(
        db: SQLiteDatabase,
        countryId: Long,
        countryName: String,
        translations: Map<String, List<TranslationData>>
    ) {
        translations.forEach { (languageCode, langTranslations) ->
            langTranslations.find { it.originalName == countryName }?.let { translation ->
                val values = ContentValues().apply {
                    put(COLUMN_COUNTRY_ID, countryId)
                    put(COLUMN_LANGUAGE_CODE, languageCode)
                    put(COLUMN_TRANSLATED_NAME, translation.translatedName)
                    put(COLUMN_TRANSLATED_CAPITAL, translation.translatedCapital)
                    put(COLUMN_TRANSLATED_CITY1, translation.translatedCity1)
                    put(COLUMN_TRANSLATED_CITY2, translation.translatedCity2)
                    put(COLUMN_TRANSLATED_CITY3, translation.translatedCity3)
                    put(COLUMN_TRANSLATED_CONTINENT, translation.translatedContinent)
                    put(COLUMN_TRANSLATED_REGION, translation.translatedRegion)
                    put(COLUMN_TRANSLATED_CURRENCY, translation.translatedCurrency)
                    put(COLUMN_TRANSLATED_LANG, translation.translatedLanguages)
                }
                db.insertWithOnConflict(TABLE_COUNTRY_TRANSLATION, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }
    }

    private fun loadCountriesFromJson(): List<CountryData> {
        return try {
            val json = appContext.assets.open("countries.json")
                .bufferedReader().use { it.readText() }
            gson.fromJson(json, object : TypeToken<List<CountryData>>() {}.type)
        } catch (e: IOException) {
            Log.e(TAG, "Error loading countries.json", e)
            emptyList()
        }
    }

    private fun loadAllTranslations(): Map<String, List<TranslationData>> {
        return try {
            val translationFiles = appContext.assets.list("translations")?.toList() ?: emptyList()
            translationFiles.associate { filename ->
                val languageCode = filename.removeSuffix(".json")
                val translations = loadTranslationsForLanguage(languageCode)
                languageCode to translations
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading translations", e)
            emptyMap()
        }
    }

    private fun loadTranslationsForLanguage(languageCode: String): List<TranslationData> {
        return try {
            val json = appContext.assets.open("translations/$languageCode.json")
                .bufferedReader().use { it.readText() }
            gson.fromJson(json, object : TypeToken<List<TranslationData>>() {}.type)
        } catch (e: IOException) {
            Log.e(TAG, "Error loading $languageCode translations", e)
            emptyList()
        }
    }

    // Data classes
    private data class CountryData(
        val name: String,
        val capital: String,
        val bigCity: String,
        val secondCity: String?,
        val thirdCity: String?,
        val continent: String,
        val region: String,
        val languages: List<String>,
        val currency: String,
        val population: Long,
        val area: Long
    )

    private data class TranslationData(
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

    // Public query methods
    fun getRandomCountries(count: Int): List<Country> {
        val db = readableDatabase
        return db.rawQuery(
            "SELECT * FROM $TABLE_COUNTRIES ORDER BY RANDOM() LIMIT $count", null
        ).use { cursor ->
            mutableListOf<Country>().apply {
                while (cursor.moveToNext()) {
                    add(Country.fromCursor(cursor))
                }
            }
        }
    }

    fun getTranslatedRandomCountries(count: Int, languageCode: String): List<Country> {
        val db = readableDatabase
        return db.rawQuery(
            """
            SELECT c.*, t.* 
            FROM $TABLE_COUNTRIES c
            LEFT JOIN $TABLE_COUNTRY_TRANSLATION t 
            ON c.$COLUMN_ID = t.$COLUMN_COUNTRY_ID AND t.$COLUMN_LANGUAGE_CODE = ?
            ORDER BY RANDOM() LIMIT $count
            """.trimIndent(),
            arrayOf(languageCode)
        ).use { cursor ->
            mutableListOf<Country>().apply {
                while (cursor.moveToNext()) {
                    val country = Country.fromCursor(cursor)
                    if (!cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_TRANSLATED_NAME))) {
                        add(Country.withTranslations(country, cursor))
                    } else {
                        add(country)
                    }
                }
            }
        }
    }

    fun getAvailableLanguages(): List<String> {
        return try {
            appContext.assets.list("translations")?.map {
                it.removeSuffix(".json")
            } ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }
    }

    // Add other query methods as needed...
}
package com.example.geographyquiz.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.getStringOrNull
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
        private const val DATABASE_VERSION = 3  // Incremented version for schema changes

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
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_CODE = "code"
        const val COLUMN_DIFFICULTY = "difficulty"

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

        // Landmark table
        const val TABLE_LANDMARKS = "landmarks"
        const val COLUMN_LANDMARK_ID = "landmark_id"
        const val COLUMN_LANDMARK_NAME = "landmark_name"
        const val COLUMN_IMAGE_PATH = "image_path"

        // Landmark translation table
        const val TABLE_LANDMARK_TRANSLATIONS = "landmark_translations"
        const val COLUMN_TRANSLATED_LANDMARK_NAME = "translated_landmark_name"
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            createTables(db)
            createIndexes(db)
            refreshAllCountries(db)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database", e)
            throw e
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            // Drop all tables and recreate them
            db.execSQL("DROP TABLE IF EXISTS $TABLE_COUNTRIES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_COUNTRY_TRANSLATION")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_LANDMARKS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_LANDMARK_TRANSLATIONS")
            onCreate(db)
        } catch (e: Exception) {
            Log.e(TAG, "Error upgrading database", e)
            throw e
        }
    }

    private fun createTables(db: SQLiteDatabase) {
        // Main countries table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_COUNTRIES (
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
                $COLUMN_AREA INTEGER NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_CODE TEXT NOT NULL,
                $COLUMN_DIFFICULTY INTEGER NOT NULL
            )
        """.trimIndent())

        // Translation table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_COUNTRY_TRANSLATION (
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

        // Landmarks table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_LANDMARKS (
                $COLUMN_LANDMARK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_COUNTRY_ID INTEGER NOT NULL,
                $COLUMN_LANDMARK_NAME TEXT NOT NULL,
                $COLUMN_IMAGE_PATH TEXT NOT NULL,
                FOREIGN KEY($COLUMN_COUNTRY_ID) REFERENCES $TABLE_COUNTRIES($COLUMN_ID)
            )
        """.trimIndent())

        // Landmark translations table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_LANDMARK_TRANSLATIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LANDMARK_ID INTEGER NOT NULL,
                $COLUMN_LANGUAGE_CODE TEXT NOT NULL,
                $COLUMN_TRANSLATED_LANDMARK_NAME TEXT NOT NULL,
                FOREIGN KEY($COLUMN_LANDMARK_ID) REFERENCES $TABLE_LANDMARKS($COLUMN_LANDMARK_ID),
                UNIQUE($COLUMN_LANDMARK_ID, $COLUMN_LANGUAGE_CODE)
            )
        """.trimIndent())
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_country_name ON $TABLE_COUNTRIES($COLUMN_NAME)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_country_continent ON $TABLE_COUNTRIES($COLUMN_CONTINENT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_country_region ON $TABLE_COUNTRIES($COLUMN_REGION)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_translation_country ON $TABLE_COUNTRY_TRANSLATION($COLUMN_COUNTRY_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_translation_language ON $TABLE_COUNTRY_TRANSLATION($COLUMN_LANGUAGE_CODE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_landmark_country ON $TABLE_LANDMARKS($COLUMN_COUNTRY_ID)")
    }

    private fun refreshAllCountries(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            // Clear all tables
            db.execSQL("DELETE FROM $TABLE_COUNTRIES")
            db.execSQL("DELETE FROM $TABLE_COUNTRY_TRANSLATION")
            db.execSQL("DELETE FROM $TABLE_LANDMARKS")
            db.execSQL("DELETE FROM $TABLE_LANDMARK_TRANSLATIONS")

            val countries = loadCountriesFromJson()
            val translations = loadAllTranslations()
            val landmarkTranslations = loadAllLandmarkTranslations()

            for (country in countries) {
                try {
                    val countryId = insertCountry(db, country)
                    insertTranslations(db, countryId, country.name, translations)
                    insertLandmarks(db, countryId, country, landmarkTranslations)
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting country ${country.name}", e)
                }
            }
            db.setTransactionSuccessful()
            Log.d(TAG, "Successfully loaded ${countries.size} countries with landmarks")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing countries", e)
            throw e
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
            put(COLUMN_LANG, country.languages?.joinToString(",") ?: "")
            put(COLUMN_CURRENCY, country.currency)
            put(COLUMN_POPULATION, country.population)
            put(COLUMN_AREA, country.area)
            put(COLUMN_CATEGORY, country.category)
            put(COLUMN_CODE, country.countryCode)
            put(COLUMN_DIFFICULTY, country.difficulty)
        }
        return db.insertWithOnConflict(TABLE_COUNTRIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun insertTranslations(
        db: SQLiteDatabase,
        countryId: Long,
        countryName: String,
        translations: Map<String, List<CountryTranslationData>>
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

    private fun insertLandmarks(
        db: SQLiteDatabase,
        countryId: Long,
        country: CountryData,
        translations: Map<String, List<LandmarkTranslationData>>
    ) {
        country.landmarks?.let { landmarks ->
            if (landmarks.isEmpty()) {
                Log.d(TAG, "No landmarks for ${country.name}")
                return
            }

            Log.d(TAG, "Inserting ${landmarks.size} landmarks for ${country.name}")

            landmarks.forEach { landmark ->
                try {
                    val values = ContentValues().apply {
                        put(COLUMN_COUNTRY_ID, countryId)
                        put(COLUMN_LANDMARK_NAME, landmark.name)
                        put(COLUMN_IMAGE_PATH, landmark.imagePath)
                    }

                    val landmarkId = db.insertWithOnConflict(
                        TABLE_LANDMARKS,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )

                    Log.d(TAG, "Inserted landmark ${landmark.name} with ID $landmarkId")

                    // Insert translations
                    translations.forEach { (languageCode, langTranslations) ->
                        langTranslations.find { it.originalName == landmark.name }?.let { translation ->
                            val transValues = ContentValues().apply {
                                put(COLUMN_LANDMARK_ID, landmarkId)
                                put(COLUMN_LANGUAGE_CODE, languageCode)
                                put(COLUMN_TRANSLATED_LANDMARK_NAME, translation.translatedName)
                            }
                            db.insertWithOnConflict(
                                TABLE_LANDMARK_TRANSLATIONS,
                                null,
                                transValues,
                                SQLiteDatabase.CONFLICT_REPLACE
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting landmark ${landmark.name}", e)
                }
            }
        } ?: Log.d(TAG, "Landmarks is null for ${country.name}")
    }

    private fun loadCountriesFromJson(): List<CountryData> {
        return try {
            val json = appContext.assets.open("countries.json")
                .bufferedReader().use { it.readText() }
            gson.fromJson(json, object : TypeToken<List<CountryData>>() {}.type) ?: emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "Error loading countries.json", e)
            emptyList()
        }
    }

    private fun loadAllTranslations(): Map<String, List<CountryTranslationData>> {
        return try {
            val translationFiles = appContext.assets.list("translations")?.toList() ?: emptyList()
            translationFiles.associate { filename ->
                val languageCode = filename.removeSuffix(".json")
                val translations = loadCountryTranslationsForLanguage(languageCode)
                languageCode to translations
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading translations", e)
            emptyMap()
        }
    }

    private fun loadAllLandmarkTranslations(): Map<String, List<LandmarkTranslationData>> {
        return try {
            val translationFiles = appContext.assets.list("landmark_translations")?.toList() ?: emptyList()
            translationFiles.associate { filename ->
                val languageCode = filename.removeSuffix(".json")
                val translations = loadLandmarkTranslationsForLanguage(languageCode)
                languageCode to translations
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading landmark translations", e)
            emptyMap()
        }
    }

    private fun loadCountryTranslationsForLanguage(languageCode: String): List<CountryTranslationData> {
        return try {
            val json = appContext.assets.open("translations/$languageCode.json")
                .bufferedReader().use { it.readText() }
            gson.fromJson(json, object : TypeToken<List<CountryTranslationData>>() {}.type) ?: emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "Error loading $languageCode translations", e)
            emptyList()
        }
    }

    private fun loadLandmarkTranslationsForLanguage(languageCode: String): List<LandmarkTranslationData> {
        return try {
            val json = appContext.assets.open("landmark_translations/$languageCode.json")
                .bufferedReader().use { it.readText() }
            gson.fromJson(json, object : TypeToken<List<LandmarkTranslationData>>() {}.type) ?: emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "Error loading $languageCode landmark translations", e)
            emptyList()
        }
    }

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

    fun getRandomCountriesWithLandmarks(count: Int, languageCode: String = "en"): List<Country> {
        val db = readableDatabase
        return db.rawQuery(
            """
        SELECT c.*, 
               l.$COLUMN_LANDMARK_NAME as landmark_name,
               l.$COLUMN_IMAGE_PATH as landmark_path,
               lt.$COLUMN_TRANSLATED_LANDMARK_NAME as translated_landmark_name
        FROM $TABLE_COUNTRIES c
        JOIN $TABLE_LANDMARKS l ON c.$COLUMN_ID = l.$COLUMN_COUNTRY_ID
        LEFT JOIN $TABLE_LANDMARK_TRANSLATIONS lt ON l.$COLUMN_LANDMARK_ID = lt.$COLUMN_LANDMARK_ID 
            AND lt.$COLUMN_LANGUAGE_CODE = ?
        ORDER BY RANDOM() LIMIT $count
        """.trimIndent(),
            arrayOf(languageCode)
        ).use { cursor ->
            mutableListOf<Country>().apply {
                while (cursor.moveToNext()) {
                    val country = Country.fromCursor(cursor)
                    val landmark = Landmark(
                        name = cursor.getString(cursor.getColumnIndexOrThrow("landmark_name")),
                        translatedName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("translated_landmark_name"))
                            ?: cursor.getString(cursor.getColumnIndexOrThrow("landmark_name")),
                        imagePath = cursor.getString(cursor.getColumnIndexOrThrow("landmark_path"))
                    )
                    add(country.copy(landmarks = listOf(landmark)))
                }
            }
        }
    }

    fun getCountriesByDifficulty(difficulty: Int, limit: Int = 3, languageCode: String = "en"): List<Country> {
        val db = readableDatabase
        return db.rawQuery(
            """
        SELECT c.*, t.* 
        FROM $TABLE_COUNTRIES c
        LEFT JOIN $TABLE_COUNTRY_TRANSLATION t 
        ON c.$COLUMN_ID = t.$COLUMN_COUNTRY_ID AND t.$COLUMN_LANGUAGE_CODE = ?
        WHERE c.$COLUMN_DIFFICULTY = ?
        ORDER BY RANDOM() LIMIT $limit
        """.trimIndent(),
            arrayOf(languageCode, difficulty.toString())
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

    fun getCountriesWithLandmarksByDifficulty(difficulty: Int, limit: Int = 3, languageCode: String = "en"): List<Country> {
        val db = readableDatabase
        return db.rawQuery(
            """
        SELECT c.*, 
               t.*,
               l.$COLUMN_LANDMARK_NAME as landmark_name,
               l.$COLUMN_IMAGE_PATH as landmark_path,
               lt.$COLUMN_TRANSLATED_LANDMARK_NAME as translated_landmark_name
        FROM $TABLE_COUNTRIES c
        LEFT JOIN $TABLE_COUNTRY_TRANSLATION t ON c.$COLUMN_ID = t.$COLUMN_COUNTRY_ID 
            AND t.$COLUMN_LANGUAGE_CODE = ?
        JOIN $TABLE_LANDMARKS l ON c.$COLUMN_ID = l.$COLUMN_COUNTRY_ID
        LEFT JOIN $TABLE_LANDMARK_TRANSLATIONS lt ON l.$COLUMN_LANDMARK_ID = lt.$COLUMN_LANDMARK_ID 
            AND lt.$COLUMN_LANGUAGE_CODE = ?
        WHERE c.$COLUMN_DIFFICULTY = ?
        ORDER BY RANDOM() LIMIT $limit
        """.trimIndent(),
            arrayOf(languageCode, languageCode, difficulty.toString())
        ).use { cursor ->
            mutableListOf<Country>().apply {
                while (cursor.moveToNext()) {
                    val country = if (!cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_TRANSLATED_NAME))) {
                        Country.withTranslations(Country.fromCursor(cursor), cursor)
                    } else {
                        Country.fromCursor(cursor)
                    }

                    val landmark = Landmark(
                        name = cursor.getString(cursor.getColumnIndexOrThrow("landmark_name")),
                        translatedName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("translated_landmark_name"))
                            ?: cursor.getString(cursor.getColumnIndexOrThrow("landmark_name")),
                        imagePath = cursor.getString(cursor.getColumnIndexOrThrow("landmark_path"))
                    )

                    // Check if we already have this country in our list
                    val existingCountry = find { it.id == country.id }
                    if (existingCountry != null) {
                        // Add landmark to existing country
                        val index = indexOf(existingCountry)
                        this[index] = existingCountry.copy(
                            landmarks = existingCountry.landmarks + landmark
                        )
                    } else {
                        // Add new country with landmark
                        add(country.copy(landmarks = listOf(landmark)))
                    }
                }
            }
        }
    }

    fun getAllCountriesWithLandmarks(languageCode: String = "en"): List<Country> {
        val db = readableDatabase
        return db.rawQuery(
            """
        SELECT c.*, 
               t.*,
               GROUP_CONCAT(l.$COLUMN_LANDMARK_NAME, '|') as landmark_names,
               GROUP_CONCAT(l.$COLUMN_IMAGE_PATH, '|') as landmark_paths,
               GROUP_CONCAT(lt.$COLUMN_TRANSLATED_LANDMARK_NAME, '|') as translated_landmark_names
        FROM $TABLE_COUNTRIES c
        LEFT JOIN $TABLE_COUNTRY_TRANSLATION t ON c.$COLUMN_ID = t.$COLUMN_COUNTRY_ID 
            AND t.$COLUMN_LANGUAGE_CODE = ?
        JOIN $TABLE_LANDMARKS l ON c.$COLUMN_ID = l.$COLUMN_COUNTRY_ID
        LEFT JOIN $TABLE_LANDMARK_TRANSLATIONS lt ON l.$COLUMN_LANDMARK_ID = lt.$COLUMN_LANDMARK_ID 
            AND lt.$COLUMN_LANGUAGE_CODE = ?
        GROUP BY c.$COLUMN_ID
        """, arrayOf(languageCode, languageCode)
        ).use { cursor ->
            mutableListOf<Country>().apply {
                while (cursor.moveToNext()) {
                    val country = if (!cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_TRANSLATED_NAME))) {
                        Country.withTranslations(Country.fromCursor(cursor), cursor)
                    } else {
                        Country.fromCursor(cursor)
                    }

                    val landmarkNames = cursor.getString(cursor.getColumnIndexOrThrow("landmark_names")).split("|")
                    val landmarkPaths = cursor.getString(cursor.getColumnIndexOrThrow("landmark_paths")).split("|")
                    val translatedLandmarkNames = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("translated_landmark_names"))
                        ?.split("|") ?: landmarkNames

                    val landmarks = landmarkNames.zip(landmarkPaths).zip(translatedLandmarkNames).map {
                        Landmark(
                            name = it.first.first,
                            translatedName = it.second,
                            imagePath = it.first.second
                        )
                    }
                    add(country.copy(landmarks = landmarks))
                }
            }
        }
    }

    fun getLandmarksForCountry(countryId: Int, languageCode: String = "en"): List<Landmark> {
        val db = readableDatabase
        return db.rawQuery(
            """
            SELECT l.$COLUMN_LANDMARK_NAME, l.$COLUMN_IMAGE_PATH, 
                   lt.$COLUMN_TRANSLATED_LANDMARK_NAME
            FROM $TABLE_LANDMARKS l
            LEFT JOIN $TABLE_LANDMARK_TRANSLATIONS lt ON l.$COLUMN_LANDMARK_ID = lt.$COLUMN_LANDMARK_ID 
                AND lt.$COLUMN_LANGUAGE_CODE = ?
            WHERE l.$COLUMN_COUNTRY_ID = ?
            """.trimIndent(),
            arrayOf(languageCode, countryId.toString())
        ).use { cursor ->
            mutableListOf<Landmark>().apply {
                while (cursor.moveToNext()) {
                    add(Landmark(
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LANDMARK_NAME)),
                        translatedName = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(COLUMN_TRANSLATED_LANDMARK_NAME))
                            ?: cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LANDMARK_NAME)),
                        imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH))
                    ))
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

    data class LandmarkData(
        val name: String,
        val imagePath: String,
        val difficulty: Int
    )

    private data class LandmarkTranslationData(
        val originalName: String,
        val translatedName: String
    )
}
// CountryDatabase.kt
package com.example.geographyquiz.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class CountryDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val TAG = "CountryDatabase"
        private const val DATABASE_NAME = "world_countries.db"
        private const val DATABASE_VERSION = 2

        // Main countries table
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
        Log.d(TAG, "Upgrading from v$oldVersion to v$newVersion")
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_COUNTRIES RENAME TO ${TABLE_COUNTRIES}_old")
            createTables(db)
            db.execSQL("""
                INSERT INTO $TABLE_COUNTRIES SELECT 
                    $COLUMN_ID, $COLUMN_NAME, $COLUMN_CAPITAL, $COLUMN_BIGGEST_CITY, 
                    $COLUMN_SECOND_CITY, $COLUMN_THIRD_CITY, $COLUMN_CONTINENT, 
                    $COLUMN_REGION, $COLUMN_LANG, $COLUMN_CURRENCY, 
                    $COLUMN_POPULATION, $COLUMN_AREA 
                FROM ${TABLE_COUNTRIES}_old
            """.trimIndent())
            db.execSQL("DROP TABLE ${TABLE_COUNTRIES}_old")
        }
        refreshAllCountries(db)
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
        db.execSQL("DELETE FROM $TABLE_COUNTRIES")
        db.execSQL("DELETE FROM $TABLE_COUNTRY_TRANSLATION")

        val allCountries = getAllCountryData()
        db.beginTransaction()
        try {
            for (country in allCountries) {
                // Insert base country data
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
                val countryId = db.insertWithOnConflict(TABLE_COUNTRIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)

                if (country.name == "Algeria") {
                    val translationValues = ContentValues().apply {
                        put(COLUMN_COUNTRY_ID, countryId)
                        put(COLUMN_LANGUAGE_CODE, "fr")
                        put(COLUMN_TRANSLATED_NAME, "Algérie")
                        put(COLUMN_TRANSLATED_CAPITAL, "Alger")
                        put(COLUMN_TRANSLATED_CITY1, "Alger")
                        put(COLUMN_TRANSLATED_CITY2, "Oran")
                        put(COLUMN_TRANSLATED_CITY3, "Constantine")
                        put(COLUMN_TRANSLATED_CONTINENT, "Afrique")
                        put(COLUMN_TRANSLATED_REGION, "Afrique du Nord")
                        put(COLUMN_TRANSLATED_CURRENCY, "Dinar algérien")
                        put(COLUMN_TRANSLATED_LANG, "Arabe, Berbère, Français")
                    }
                    db.insertWithOnConflict(TABLE_COUNTRY_TRANSLATION, null, translationValues, SQLiteDatabase.CONFLICT_REPLACE)
                }

                if (country.name == "Angola") {
                    val translationValues = ContentValues().apply {
                        put(COLUMN_COUNTRY_ID, countryId)
                        put(COLUMN_LANGUAGE_CODE, "fr")
                        put(COLUMN_TRANSLATED_NAME, "Angola")
                        put(COLUMN_TRANSLATED_CAPITAL, "Luanda")
                        put(COLUMN_TRANSLATED_CITY1, "Luanda")
                        put(COLUMN_TRANSLATED_CITY2, "Huambo")
                        put(COLUMN_TRANSLATED_CITY3, "Lobito")
                        put(COLUMN_TRANSLATED_CONTINENT, "Afrique")
                        put(COLUMN_TRANSLATED_REGION, "Afrique centrale")
                        put(COLUMN_TRANSLATED_CURRENCY, "Kwanza")
                        put(COLUMN_TRANSLATED_LANG, "Portugais")
                    }
                    db.insertWithOnConflict(TABLE_COUNTRY_TRANSLATION, null, translationValues, SQLiteDatabase.CONFLICT_REPLACE)
                }

                if (country.name == "Canada") {
                    val translationValues = ContentValues().apply {
                        put(COLUMN_COUNTRY_ID, countryId)
                        put(COLUMN_LANGUAGE_CODE, "fr")
                        put(COLUMN_TRANSLATED_NAME, "Canada")
                        put(COLUMN_TRANSLATED_CAPITAL, "Ottawa")
                        put(COLUMN_TRANSLATED_CITY1, "Toronto")
                        put(COLUMN_TRANSLATED_CITY2, "Montréal")
                        put(COLUMN_TRANSLATED_CITY3, "Vancouver")
                        put(COLUMN_TRANSLATED_CONTINENT, "Amériques")
                        put(COLUMN_TRANSLATED_REGION, "Amérique du Nord")
                        put(COLUMN_TRANSLATED_CURRENCY, "Dollar canadien")
                        put(COLUMN_TRANSLATED_LANG, "Anglais, Français")
                    }
                    db.insertWithOnConflict(TABLE_COUNTRY_TRANSLATION, null, translationValues, SQLiteDatabase.CONFLICT_REPLACE)
                }

                if (country.name == "United States") {
                    val translationValues = ContentValues().apply {
                        put(COLUMN_COUNTRY_ID, countryId)
                        put(COLUMN_LANGUAGE_CODE, "fr")
                        put(COLUMN_TRANSLATED_NAME, "États-Unis")
                        put(COLUMN_TRANSLATED_CAPITAL, "Washington D.C.")
                        put(COLUMN_TRANSLATED_CITY1, "New York")
                        put(COLUMN_TRANSLATED_CITY2, "Los Angeles")
                        put(COLUMN_TRANSLATED_CITY3, "Chicago")
                        put(COLUMN_TRANSLATED_CONTINENT, "Amériques")
                        put(COLUMN_TRANSLATED_REGION, "Amérique du Nord")
                        put(COLUMN_TRANSLATED_CURRENCY, "Dollar américain")
                        put(COLUMN_TRANSLATED_LANG, "Anglais, Espagnol")
                    }
                    db.insertWithOnConflict(TABLE_COUNTRY_TRANSLATION, null, translationValues, SQLiteDatabase.CONFLICT_REPLACE)
                }

                if (country.name == "China") {
                    val translationValues = ContentValues().apply {
                        put(COLUMN_COUNTRY_ID, countryId)
                        put(COLUMN_LANGUAGE_CODE, "fr")
                        put(COLUMN_TRANSLATED_NAME, "Chine")
                        put(COLUMN_TRANSLATED_CAPITAL, "Pékin")
                        put(COLUMN_TRANSLATED_CITY1, "Shanghai")
                        put(COLUMN_TRANSLATED_CITY2, "Pékin")
                        put(COLUMN_TRANSLATED_CITY3, "Canton")
                        put(COLUMN_TRANSLATED_CONTINENT, "Asie")
                        put(COLUMN_TRANSLATED_REGION, "Asie orientale")
                        put(COLUMN_TRANSLATED_CURRENCY, "Yuan")
                        put(COLUMN_TRANSLATED_LANG, "Mandarin, Cantonais, Wu, Min")
                    }
                    db.insertWithOnConflict(TABLE_COUNTRY_TRANSLATION, null, translationValues, SQLiteDatabase.CONFLICT_REPLACE)
                }

                if (country.name == "India") {
                    val translationValues = ContentValues().apply {
                        put(COLUMN_COUNTRY_ID, countryId)
                        put(COLUMN_LANGUAGE_CODE, "fr")
                        put(COLUMN_TRANSLATED_NAME, "Inde")
                        put(COLUMN_TRANSLATED_CAPITAL, "New Delhi")
                        put(COLUMN_TRANSLATED_CITY1, "Bombay")
                        put(COLUMN_TRANSLATED_CITY2, "Delhi")
                        put(COLUMN_TRANSLATED_CITY3, "Bangalore")
                        put(COLUMN_TRANSLATED_CONTINENT, "Asie")
                        put(COLUMN_TRANSLATED_REGION, "Asie du Sud")
                        put(COLUMN_TRANSLATED_CURRENCY, "Roupie indienne")
                        put(COLUMN_TRANSLATED_LANG, "Hindi, Anglais, Bengali, Telugu, Marathi")
                    }
                    db.insertWithOnConflict(TABLE_COUNTRY_TRANSLATION, null, translationValues, SQLiteDatabase.CONFLICT_REPLACE)
                }

                if (country.name == "France") {
                    val translationValues = ContentValues().apply {
                        put(COLUMN_COUNTRY_ID, countryId)
                        put(COLUMN_LANGUAGE_CODE, "fr")
                        put(COLUMN_TRANSLATED_NAME, "France")
                        put(COLUMN_TRANSLATED_CAPITAL, "Paris")
                        put(COLUMN_TRANSLATED_CITY1, "Paris")
                        put(COLUMN_TRANSLATED_CITY2, "Marseille")
                        put(COLUMN_TRANSLATED_CITY3, "Lyon")
                        put(COLUMN_TRANSLATED_CONTINENT, "Europe")
                        put(COLUMN_TRANSLATED_REGION, "Europe de l'Ouest")
                        put(COLUMN_TRANSLATED_CURRENCY, "Euro")
                        put(COLUMN_TRANSLATED_LANG, "Français")
                    }
                    db.insertWithOnConflict(TABLE_COUNTRY_TRANSLATION, null, translationValues, SQLiteDatabase.CONFLICT_REPLACE)
                }

                if (country.name == "Germany") {
                    val translationValues = ContentValues().apply {
                        put(COLUMN_COUNTRY_ID, countryId)
                        put(COLUMN_LANGUAGE_CODE, "fr")
                        put(COLUMN_TRANSLATED_NAME, "Allemagne")
                        put(COLUMN_TRANSLATED_CAPITAL, "Berlin")
                        put(COLUMN_TRANSLATED_CITY1, "Berlin")
                        put(COLUMN_TRANSLATED_CITY2, "Hambourg")
                        put(COLUMN_TRANSLATED_CITY3, "Munich")
                        put(COLUMN_TRANSLATED_CONTINENT, "Europe")
                        put(COLUMN_TRANSLATED_REGION, "Europe de l'Ouest")
                        put(COLUMN_TRANSLATED_CURRENCY, "Euro")
                        put(COLUMN_TRANSLATED_LANG, "Allemand, Bas allemand, Sorab, Danois")
                    }
                    db.insertWithOnConflict(TABLE_COUNTRY_TRANSLATION, null, translationValues, SQLiteDatabase.CONFLICT_REPLACE)
                }


                // Add more translations as needed
            }
            db.setTransactionSuccessful()
            Log.d(TAG, "Refreshed ${allCountries.size} countries with translations")
        } finally {
            db.endTransaction()
        }
    }

    // Country data methods
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

    fun getCountryById(id: Int): Country? {
        val db = readableDatabase
        return db.query(
            TABLE_COUNTRIES,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) Country.fromCursor(cursor) else null
        }
    }

    fun getTranslatedCountry(id: Int, languageCode: String): Country? {
        val country = getCountryById(id) ?: return null

        val db = readableDatabase
        db.query(
            TABLE_COUNTRY_TRANSLATION,
            null,
            "$COLUMN_COUNTRY_ID = ? AND $COLUMN_LANGUAGE_CODE = ?",
            arrayOf(id.toString(), languageCode),
            null, null, null
        ).use { cursor ->
            return if (cursor.moveToFirst()) {
                Country.withTranslations(country, cursor)
            } else {
                country
            }
        }
    }

    fun addCountryTranslation(
        countryId: Int,
        languageCode: String,
        translatedName: String,
        translatedCapital: String,
        translatedCity1: String,
        translatedCity2: String?,
        translatedCity3: String?,
        translatedContinent: String,
        translatedRegion: String,
        translatedCurrency: String,
        translatedLanguages: List<String>
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COUNTRY_ID, countryId)
            put(COLUMN_LANGUAGE_CODE, languageCode)
            put(COLUMN_TRANSLATED_NAME, translatedName)
            put(COLUMN_TRANSLATED_CAPITAL, translatedCapital)
            put(COLUMN_TRANSLATED_CITY1, translatedCity1)
            put(COLUMN_TRANSLATED_CITY2, translatedCity2)
            put(COLUMN_TRANSLATED_CITY3, translatedCity3)
            put(COLUMN_TRANSLATED_CONTINENT, translatedContinent)
            put(COLUMN_TRANSLATED_REGION, translatedRegion)
            put(COLUMN_TRANSLATED_CURRENCY, translatedCurrency)
            put(COLUMN_TRANSLATED_LANG, translatedLanguages.joinToString(","))
        }

        db.insertWithOnConflict(
            TABLE_COUNTRY_TRANSLATION,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getAvailableLanguages(): List<String> {
        val db = readableDatabase
        return db.rawQuery(
            "SELECT DISTINCT $COLUMN_LANGUAGE_CODE FROM $TABLE_COUNTRY_TRANSLATION",
            null
        ).use { cursor ->
            mutableListOf<String>().apply {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
                }
            }
        }
    }

    private fun getAllCountryData(): List<CountryData> {
        return listOf(
            CountryData("Algeria", "Algiers", "Algiers", "Oran", "Constantine", "Africa", "Northern Africa",
                listOf("Arabic", "Berber", "French"), "Algerian Dinar", 43851044, 2381741),
            CountryData("Angola", "Luanda", "Luanda", "Huambo", "Lobito", "Africa", "Middle Africa",
                listOf("Portuguese"), "Kwanza", 32866272, 1246700),

            // Americas
            CountryData("Canada", "Ottawa", "Toronto", "Montreal", "Vancouver", "Americas", "Northern America",
                listOf("English", "French"), "Canadian Dollar", 37742154, 9984670),
            CountryData("United States", "Washington D.C.", "New York", "Los Angeles", "Chicago", "Americas", "Northern America",
                listOf("English", "Spanish"), "US Dollar", 331002651, 9833517),

            // Asia
            CountryData("China", "Beijing", "Shanghai", "Beijing", "Guangzhou", "Asia", "Eastern Asia",
                listOf("Mandarin", "Cantonese", "Wu", "Min"), "Yuan", 1439323776, 9596961),
            CountryData("India", "New Delhi", "Mumbai", "Delhi", "Bangalore", "Asia", "Southern Asia",
                listOf("Hindi", "English", "Bengali", "Telugu", "Marathi"), "Indian Rupee", 1380004385, 3287263),

            // Europe
            CountryData("France", "Paris", "Paris", "Marseille", "Lyon", "Europe", "Western Europe",
                listOf("French"), "Euro", 65273511, 551695),
            CountryData("Germany", "Berlin", "Berlin", "Hamburg", "Munich", "Europe", "Western Europe",
                listOf("German", "Low German", "Sorbian", "Danish"), "Euro", 83783942, 357022),

        )
    }

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
        val population: Int,
        val area: Int
    )
}
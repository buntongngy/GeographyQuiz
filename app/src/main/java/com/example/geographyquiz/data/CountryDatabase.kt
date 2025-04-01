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
        private const val DATABASE_VERSION = 2 // Incremented version for multi-language support

        // Table and column names
        const val TABLE_COUNTRIES = "countries"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_CAPITAL = "capital"
        const val COLUMN_BIGGEST_CITY = "city1"
        const val COLUMN_SECOND_CITY = "city2"
        const val COLUMN_THIRD_CITY = "city3"
        const val COLUMN_CONTINENT = "continent"
        const val COLUMN_REGION = "region"
        const val COLUMN_LANG = "languages" // Changed to store multiple languages
        const val COLUMN_CURRENCY = "currency"
        const val COLUMN_POPULATION = "population"
        const val COLUMN_AREA = "area"
    }

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
        createIndexes(db)
        refreshAllCountries(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Upgrading from v$oldVersion to v$newVersion")
        if (oldVersion < 2) {
            // Migration to version 2 - changing language column to support multiple languages
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
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_country_name ON $TABLE_COUNTRIES($COLUMN_NAME)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_country_continent ON $TABLE_COUNTRIES($COLUMN_CONTINENT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_country_region ON $TABLE_COUNTRIES($COLUMN_REGION)")
    }

    private fun refreshAllCountries(db: SQLiteDatabase) {
        db.execSQL("DELETE FROM $TABLE_COUNTRIES") // Clear existing data

        val allCountries = getAllCountryData()
        db.beginTransaction()
        try {
            for (country in allCountries) {
                val values = ContentValues().apply {
                    put(COLUMN_NAME, country.name)
                    put(COLUMN_CAPITAL, country.capital)
                    put(COLUMN_BIGGEST_CITY, country.bigCity)
                    put(COLUMN_SECOND_CITY, country.secondCity)
                    put(COLUMN_THIRD_CITY, country.thirdCity)
                    put(COLUMN_CONTINENT, country.continent)
                    put(COLUMN_REGION, country.region)
                    put(COLUMN_LANG, country.languages.joinToString(",")) // Store as comma-separated
                    put(COLUMN_CURRENCY, country.currency)
                    put(COLUMN_POPULATION, country.population)
                    put(COLUMN_AREA, country.area)
                }
                db.insertWithOnConflict(TABLE_COUNTRIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            Log.d(TAG, "Refreshed ${allCountries.size} countries")
        } finally {
            db.endTransaction()
        }
    }

    private fun getAllCountryData(): List<Country> {
        return listOf(
            // Africa
            Country("Algeria", "Algiers", "Algiers", "Oran", "Constantine", "Africa", "Northern Africa",
                listOf("Arabic", "Berber", "French"), "Algerian Dinar", 43851044, 2381741),
            Country("Angola", "Luanda", "Luanda", "Huambo", "Lobito", "Africa", "Middle Africa",
                listOf("Portuguese"), "Kwanza", 32866272, 1246700),

            // Americas
            Country("Canada", "Ottawa", "Toronto", "Montreal", "Vancouver", "Americas", "Northern America",
                listOf("English", "French"), "Canadian Dollar", 37742154, 9984670),
            Country("United States", "Washington D.C.", "New York", "Los Angeles", "Chicago", "Americas", "Northern America",
                listOf("English", "Spanish"), "US Dollar", 331002651, 9833517),

            // Asia
            Country("China", "Beijing", "Shanghai", "Beijing", "Guangzhou", "Asia", "Eastern Asia",
                listOf("Mandarin", "Cantonese", "Wu", "Min"), "Yuan", 1439323776, 9596961),
            Country("India", "New Delhi", "Mumbai", "Delhi", "Bangalore", "Asia", "Southern Asia",
                listOf("Hindi", "English", "Bengali", "Telugu", "Marathi"), "Indian Rupee", 1380004385, 3287263),

            // Europe
            Country("France", "Paris", "Paris", "Marseille", "Lyon", "Europe", "Western Europe",
                listOf("French"), "Euro", 65273511, 551695),
            Country("Germany", "Berlin", "Berlin", "Hamburg", "Munich", "Europe", "Western Europe",
                listOf("German", "Low German", "Sorbian", "Danish"), "Euro", 83783942, 357022),
        )
    }

    // Query Methods
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

    fun getCountriesByContinent(continent: String): List<Country> {
        val db = readableDatabase
        return db.query(
            TABLE_COUNTRIES,
            null,
            "$COLUMN_CONTINENT = ?",
            arrayOf(continent),
            null, null, COLUMN_NAME
        ).use { cursor ->
            mutableListOf<Country>().apply {
                while (cursor.moveToNext()) {
                    add(Country.fromCursor(cursor))
                }
            }
        }
    }

    fun getCountryByName(name: String): Country? {
        val db = readableDatabase
        return db.query(
            TABLE_COUNTRIES,
            null,
            "$COLUMN_NAME = ?",
            arrayOf(name),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) Country.fromCursor(cursor) else null
        }
    }

    fun hasData(): Boolean {
        val db = readableDatabase
        return db.rawQuery("SELECT COUNT(*) FROM $TABLE_COUNTRIES", null).use { cursor ->
            cursor.moveToFirst() && cursor.getInt(0) > 0
        }
    }

    data class Country(
        val name: String,
        val capital: String,
        val bigCity: String,
        val secondCity: String?,
        val thirdCity: String?,
        val continent: String,
        val region: String,
        val languages: List<String>, // Changed from String to List<String>
        val currency: String,
        val population: Int,
        val area: Int
    ) {
        companion object {
            fun fromCursor(cursor: Cursor): Country {
                val languagesString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LANG))
                val languagesList = languagesString.split(",").map { it.trim() }

                return Country(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAPITAL)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIGGEST_CITY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SECOND_CITY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_THIRD_CITY)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTINENT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REGION)),
                    languagesList,
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_POPULATION)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AREA))
                )
            }
        }

        // Helper method to get primary language (first in the list)
        fun getPrimaryLanguage(): String = languages.firstOrNull() ?: ""

        // Helper method to get all languages as comma-separated string
        fun getLanguagesString(): String = languages.joinToString(", ")
    }
}
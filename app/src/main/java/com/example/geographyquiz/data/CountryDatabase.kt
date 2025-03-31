// CountryDatabaseHelper.kt
package com.example.geographyquiz.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
class CountryDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "CountryDatabase.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_COUNTRIES = "countries"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_CAPITAL = "capital"
        const val COLUMN_BIGGEST_CITY = "city1"
        const val COLUMN_SECOND_CITY = "city2"
        const val COLUMN_THIRD_CITY = "city3"
        const val COLUMN_CONTINENT = "continent"
        const val COLUMN_REGION = "region"
        const val COLUMN_LANG = "language"
        const val COLUMN_CURRENCY = "currency"
        const val COLUMN_POPULATION = "population"
        const val COLUMN_AREA = "area"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_COUNTRIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_CAPITAL TEXT NOT NULL,
                $COLUMN_BIGGEST_CITY TEXT NOT NULL,
                $COLUMN_SECOND_CITY TEXT,
                $COLUMN_THIRD_CITY TEXT,
                $COLUMN_CONTINENT TEXT NOT NULL,
                $COLUMN_REGION TEXT NOT NULL,
                $COLUMN_LANG TEXT NOT NULL,
                $COLUMN_CURRENCY TEXT NOT NULL,
                $COLUMN_POPULATION INTEGER,
                $COLUMN_AREA INTEGER
            )
        """.trimIndent()
        db.execSQL(createTableQuery)

        // Insert sample country data
        insertInitialCountries(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        db.execSQL("DROP TABLE IF EXISTS $TABLE_COUNTRIES")
        onCreate(db)
    }

    private fun insertInitialCountries(db: SQLiteDatabase) {
        val countries = listOf(
            Country("Canada", "Ottawa", "Toronto", "Montreal", "Vancouver", "North America", "North America", "English & French", "Canadian Dollar", 41528680, 9984670),
            Country("United States", "Washington, D.C.", "New York City", "Los Angeles", "Chicago", "North America", "North America", "English", "US Dollar", 331893745, 9833517),
            Country("Brazil", "Brasília", "São Paulo", "Rio de Janeiro", "Salvador", "South America", "South America", "Portuguese", "Brazilian Real", 216422446, 8515767),
            Country("Germany", "Berlin", "Berlin", "Hamburg", "Munich", "Europe", "Western Europe", "German", "Euro", 83830972, 357022),
            Country("France", "Paris", "Paris", "Marseille", "Lyon", "Europe", "Western Europe", "French", "Euro", 67081000, 643801)
        )

        db.beginTransaction()
        try {
            for (country in countries) {
                val values = ContentValues().apply {
                    put(COLUMN_NAME, country.name)
                    put(COLUMN_CAPITAL, country.capital)
                    put(COLUMN_BIGGEST_CITY, country.bigCity)
                    put(COLUMN_SECOND_CITY, country.secondCity)
                    put(COLUMN_THIRD_CITY, country.thirdCity)
                    put(COLUMN_CONTINENT, country.continent)
                    put(COLUMN_REGION, country.region)
                    put(COLUMN_LANG, country.language)
                    put(COLUMN_CURRENCY, country.currency)
                    put(COLUMN_POPULATION, country.population)
                    put(COLUMN_AREA, country.area)
                }
                db.insert(TABLE_COUNTRIES, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllCountries(): List<Country> {
        val countries = mutableListOf<Country>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_COUNTRIES,
            arrayOf(COLUMN_NAME, COLUMN_CAPITAL, COLUMN_BIGGEST_CITY, COLUMN_SECOND_CITY, COLUMN_THIRD_CITY,
                COLUMN_CONTINENT, COLUMN_REGION, COLUMN_LANG, COLUMN_CURRENCY, COLUMN_POPULATION, COLUMN_AREA),
            null, null, null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                countries.add(Country(
                    it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_CAPITAL)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_BIGGEST_CITY)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_SECOND_CITY)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_THIRD_CITY)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_CONTINENT)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_REGION)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_LANG)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_CURRENCY)),
                    it.getInt(it.getColumnIndexOrThrow(COLUMN_POPULATION)),
                    it.getInt(it.getColumnIndexOrThrow(COLUMN_AREA))
                ))
            }
        }
        return countries
    }

    fun getRandomCountries(count: Int): List<Country> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_COUNTRIES ORDER BY RANDOM() LIMIT $count", null
        )

        val countries = mutableListOf<Country>()
        cursor.use {
            while (it.moveToNext()) {
                countries.add(Country(
                    it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_CAPITAL)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_BIGGEST_CITY)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_SECOND_CITY)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_THIRD_CITY)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_CONTINENT)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_REGION)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_LANG)),
                    it.getString(it.getColumnIndexOrThrow(COLUMN_CURRENCY)),
                    it.getInt(it.getColumnIndexOrThrow(COLUMN_POPULATION)),
                    it.getInt(it.getColumnIndexOrThrow(COLUMN_AREA))
                ))
            }
        }
        return countries
    }

    fun addCountries(db: SQLiteDatabase, countries: List<Country>) {
        db.beginTransaction()
        try {
            for (country in countries) {
                val values = ContentValues().apply {
                    put(COLUMN_NAME, country.name)
                    put(COLUMN_CAPITAL, country.capital)
                    put(COLUMN_BIGGEST_CITY, country.bigCity)
                    put(COLUMN_SECOND_CITY, country.secondCity)
                    put(COLUMN_THIRD_CITY, country.thirdCity)
                    put(COLUMN_CONTINENT, country.continent)
                    put(COLUMN_REGION, country.region)
                    put(COLUMN_LANG, country.language)
                    put(COLUMN_CURRENCY, country.currency)
                    put(COLUMN_POPULATION, country.population)
                    put(COLUMN_AREA, country.area)
                }
                db.insert(TABLE_COUNTRIES, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
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
        val language: String,
        val currency: String,
        val population: Int,
        val area: Int
    )
}

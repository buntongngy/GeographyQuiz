package com.example.geographyquiz.quiz

import android.graphics.drawable.PictureDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.caverock.androidsvg.SVG
import com.example.geographyquiz.R
import com.example.geographyquiz.data.Country
import com.example.geographyquiz.data.CountryDatabase
import com.example.geographyquiz.utils.TranslationUtils
import java.io.InputStream
import java.util.*

class FlagQuiz : AppCompatActivity() {

    private var currentLanguage = "en";
    private lateinit var databaseHelper: CountryDatabase
    private lateinit var flagImageView: ImageView
    private var correctAnswerIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_img)

        val sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
        currentLanguage = sharedPref.getString("app_language", "en") ?: "en"
        flagImageView = findViewById(R.id.flagImageView)
        databaseHelper = CountryDatabase(this)
        loadFlagQuestion()
    }

    private fun loadFlagQuestion() {

        val questionText = findViewById<TextView>(R.id.questionText)
        questionText.text = TranslationUtils.getTranslatedString(this, R.string.flag_question, currentLanguage)

        val countries = if (currentLanguage != "en") {
            databaseHelper.getTranslatedRandomCountries(4, currentLanguage)
        } else {
            databaseHelper.getRandomCountries(4)
        }
        if (countries.size < 4) {
            showError()
            return
        }

        val targetCountry = countries.random()
        loadSvgFlag(targetCountry.countryCode)



        val options = listOf(
            findViewById<Button>(R.id.option1Button),
            findViewById<Button>(R.id.option2Button),
            findViewById<Button>(R.id.option3Button),
            findViewById<Button>(R.id.option4Button)
        )

        // Shuffle the countries for answer options
        val shuffledCountries = countries.shuffled()
        correctAnswerIndex = shuffledCountries.indexOf(targetCountry)

        shuffledCountries.forEachIndexed { index, country ->
            options[index].text = if (currentLanguage != "en") country.translatedName else country.name
            options[index].setOnClickListener { checkAnswer(index) }
        }

        findViewById<Button>(R.id.nextButton).setOnClickListener {
            loadFlagQuestion()
        }
    }

    private fun loadSvgFlag(countryCode: String) {
        try {
            val fileName = "${countryCode.lowercase()}.svg"
            Log.d("FlagQuiz", "Attempting to load: $fileName")

            val inputStream: InputStream = assets.open("flag/$fileName")
            val svg = SVG.getFromInputStream(inputStream)
            val drawable = PictureDrawable(svg.renderToPicture())
            flagImageView.setImageDrawable(drawable)
            Log.d("FlagQuiz", "Successfully loaded: $fileName")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FlagQuiz", "Error loading flag: ${e.message}")
            flagImageView.setImageResource(R.drawable.ic_flag)
        }
    }

    private fun checkAnswer(selectedIndex: Int) {
        val feedbackText = findViewById<TextView>(R.id.feedbackText)
        feedbackText.text = if (selectedIndex == correctAnswerIndex) {
            TranslationUtils.getTranslatedString(this, R.string.correct, currentLanguage)
        } else {
            TranslationUtils.getTranslatedString(this, R.string.wrong, currentLanguage)
        }
    }

    private fun showError() {
        findViewById<TextView>(R.id.questionText).text = getString(R.string.notEnoughCountry)
        listOf(R.id.option1Button, R.id.option2Button, R.id.option3Button, R.id.option4Button).forEach {
            findViewById<Button>(it).visibility = View.GONE
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}
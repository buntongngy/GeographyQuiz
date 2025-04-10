package com.example.geographyquiz.quiz

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.geographyquiz.R
import com.example.geographyquiz.data.Country
import com.example.geographyquiz.data.CountryDatabase
import com.example.geographyquiz.utils.TranslationUtils
import java.io.IOException
import java.io.InputStream

class LandmarkQuiz : AppCompatActivity() {

    private var currentLanguage = "en";
    private lateinit var databaseHelper: CountryDatabase
    private lateinit var landmarkImageView: ImageView
    private var correctAnswerIndex = 0
    private lateinit var currentLandmarkPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_landmark) // Reusing the same layout as FlagQuiz

        val sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
        currentLanguage = sharedPref.getString("app_language", "en") ?: "en"
        landmarkImageView = findViewById(R.id.landmarkImageView) // Reusing the same ImageView
        databaseHelper = CountryDatabase(this)
        loadLandmarkQuestion()
    }

    private fun loadLandmarkQuestion() {
        val allCountries = databaseHelper.getAllCountriesWithLandmarks(currentLanguage)
            .filter { it.landmarks.isNotEmpty() }

        if (allCountries.size < 4) {
            showError()
            return
        }

        val targetCountry = allCountries.random()
        val targetLandmark = targetCountry.landmarks.random()
        val otherCountries = allCountries
            .filter { it != targetCountry }
            .shuffled()
            .take(3)

        val answerOptions = (listOf(targetCountry) + otherCountries).shuffled()
        correctAnswerIndex = answerOptions.indexOf(targetCountry)

        currentLandmarkPath = targetLandmark.imagePath
        loadLandmarkImage(targetLandmark.imagePath)

        findViewById<TextView>(R.id.questionText).text = TranslationUtils.getTranslatedString(this,
            R.string.landmark_question, currentLanguage)

        val options = listOf(
            findViewById<Button>(R.id.option1Button),
            findViewById<Button>(R.id.option2Button),
            findViewById<Button>(R.id.option3Button),
            findViewById<Button>(R.id.option4Button)
        )

        answerOptions.forEachIndexed { index, country ->
            options[index].text = if (currentLanguage != "en") country.translatedName else country.name
            options[index].setOnClickListener { checkAnswer(index) }
            options[index].visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.nextButton).setOnClickListener {
            loadLandmarkQuestion()
        }

        findViewById<TextView>(R.id.feedbackText).text = ""
    }

    private fun loadLandmarkImage(imagePath: String) {
        try {
            val inputStream: InputStream = assets.open(imagePath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            landmarkImageView.setImageBitmap(bitmap)
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            landmarkImageView.setImageResource(R.drawable.ic_landmark)
        }
    }

    private fun checkAnswer(selectedIndex: Int) {
        val feedbackText = findViewById<TextView>(R.id.feedbackText)
        feedbackText.text = if (selectedIndex == correctAnswerIndex) {
            TranslationUtils.getTranslatedString(this,R.string.correct, currentLanguage)
        } else {
            TranslationUtils.getTranslatedString(this,R.string.wrong, currentLanguage)
        }
    }


    private fun showError() {
        findViewById<TextView>(R.id.questionText).text = getString(R.string.notLandmark)
        listOf(R.id.option1Button, R.id.option2Button, R.id.option3Button, R.id.option4Button).forEach {
            findViewById<Button>(it).visibility = View.GONE
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}
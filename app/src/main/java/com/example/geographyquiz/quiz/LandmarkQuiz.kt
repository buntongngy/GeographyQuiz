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
import java.io.IOException
import java.io.InputStream

class LandmarkQuiz : AppCompatActivity() {

    private lateinit var databaseHelper: CountryDatabase
    private lateinit var landmarkImageView: ImageView
    private var correctAnswerIndex = 0
    private lateinit var currentLandmarkPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_img) // Reusing the same layout as FlagQuiz

        landmarkImageView = findViewById(R.id.flagImageView) // Reusing the same ImageView
        databaseHelper = CountryDatabase(this)
        loadLandmarkQuestion()
    }

    private fun loadLandmarkQuestion() {
        // Get 4 random countries that have landmarks
        val countries = databaseHelper.getRandomCountriesWithLandmarks(4)
        if (countries.size < 4) {
            showError()
            return
        }

        // Select a random country from the 4
        val targetCountry = countries.random()

        // Select a random landmark from the country's landmarks
        val targetLandmark = targetCountry.landmarks.random()
        currentLandmarkPath = targetLandmark.imagePath

        // Load the landmark image
        loadLandmarkImage(targetLandmark.imagePath)

        val questionText = findViewById<TextView>(R.id.questionText)
        questionText.text = "Which country has this landmark?"

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
            options[index].text = country.name
            options[index].setOnClickListener { checkAnswer(index) }
        }

        findViewById<Button>(R.id.nextButton).setOnClickListener {
            loadLandmarkQuestion()
        }

        // Clear previous feedback
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
            getString(R.string.correct)
        } else {
            getString(R.string.wrong)
        }
    }

    private fun showError() {
        findViewById<TextView>(R.id.questionText).text = "not enough landmark"
        listOf(R.id.option1Button, R.id.option2Button, R.id.option3Button, R.id.option4Button).forEach {
            findViewById<Button>(it).visibility = View.GONE
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}
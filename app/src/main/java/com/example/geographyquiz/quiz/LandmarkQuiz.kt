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
        setContentView(R.layout.activity_quiz_landmark) // Reusing the same layout as FlagQuiz

        landmarkImageView = findViewById(R.id.landmarkImageView) // Reusing the same ImageView
        databaseHelper = CountryDatabase(this)
        loadLandmarkQuestion()
    }

    private fun loadLandmarkQuestion() {
        // 1. Get all countries with their landmarks (each country appears once with all its landmarks)
        val allCountries = databaseHelper.getAllCountriesWithLandmarks()
            .filter { it.landmarks.isNotEmpty() }

        // Need at least 4 unique countries with landmarks
        if (allCountries.size < 4) {
            showError()
            return
        }

        // 2. Select a random country that has at least one landmark
        val targetCountry = allCountries.random()

        // 3. Select a random landmark from this country
        val targetLandmark = targetCountry.landmarks.random()

        // 4. Prepare answer options:
        //    - Target country + 3 other random unique countries (excluding target country)
        val otherCountries = allCountries
            .filter { it != targetCountry }
            .shuffled()
            .take(3)

        val answerOptions = (listOf(targetCountry) + otherCountries).shuffled()
        correctAnswerIndex = answerOptions.indexOf(targetCountry)

        // 5. Load the landmark
        currentLandmarkPath = targetLandmark.imagePath
        loadLandmarkImage(targetLandmark.imagePath)

        // 6. Set up UI
        findViewById<TextView>(R.id.questionText).text = "Which country has this landmark?"

        val options = listOf(
            findViewById<Button>(R.id.option1Button),
            findViewById<Button>(R.id.option2Button),
            findViewById<Button>(R.id.option3Button),
            findViewById<Button>(R.id.option4Button)
        )

        answerOptions.forEachIndexed { index, country ->
            options[index].text = country.name
            options[index].setOnClickListener { checkAnswer(index) }
            options[index].visibility = View.VISIBLE
        }

        // 7. Set up Next button
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
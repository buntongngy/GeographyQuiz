package com.example.geographyquiz.quiz

import android.annotation.SuppressLint
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

    private var currentLanguage = "en"
    private lateinit var databaseHelper: CountryDatabase
    private lateinit var landmarkImageView: ImageView
    private var correctAnswerIndex = 0
    private lateinit var currentLandmarkPath: String
    private var score = 0
    private var totalQuestions = 0
    private var questionsRemaining = 0
    private lateinit var countriesWithLandmarks: List<Country>
    private val usedLandmarks = mutableSetOf<String>()

    // UI Components
    private lateinit var questionText: TextView
    private lateinit var option1Btn: Button
    private lateinit var option2Btn: Button
    private lateinit var option3Btn: Button
    private lateinit var option4Btn: Button
    private lateinit var feedbackText: TextView
    private lateinit var scoreText: TextView
    private lateinit var remainingText: TextView
    private lateinit var nextButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_img)

        // Initialize UI components
        questionText = findViewById(R.id.questionText)
        landmarkImageView = findViewById(R.id.flagImageView)
        option1Btn = findViewById(R.id.option1Button)
        option2Btn = findViewById(R.id.option2Button)
        option3Btn = findViewById(R.id.option3Button)
        option4Btn = findViewById(R.id.option4Button)
        feedbackText = findViewById(R.id.feedbackText)
        scoreText = findViewById(R.id.scoreText)
        remainingText = findViewById(R.id.remainingText)
        nextButton = findViewById(R.id.nextButton)

        val sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
        currentLanguage = sharedPref.getString("app_language", "en") ?: "en"
        databaseHelper = CountryDatabase(this)

        initializeQuiz()
    }

    private fun initializeQuiz() {
        score = 0
        usedLandmarks.clear()
        countriesWithLandmarks = databaseHelper.getAllCountriesWithLandmarks(currentLanguage)
            .filter { it.landmarks.isNotEmpty() }

        // Reset UI visibility
        option1Btn.visibility = View.VISIBLE
        option2Btn.visibility = View.VISIBLE
        option3Btn.visibility = View.VISIBLE
        option4Btn.visibility = View.VISIBLE
        nextButton.visibility = View.VISIBLE
        nextButton.text = getString(R.string.next_question)

        if (countriesWithLandmarks.size < 4) {
            showError()
            return
        }

        totalQuestions = countriesWithLandmarks.sumOf { it.landmarks.size }
        questionsRemaining = totalQuestions.coerceAtMost(20) // Limit to 20 questions max
        updateScoreAndRemaining()

        loadLandmarkQuestion()
    }

    private fun loadLandmarkQuestion() {
        feedbackText.text = ""

        if (questionsRemaining <= 0) {
            showQuizCompleted()
            return
        }

        // Get all available landmarks that haven't been used yet
        val availableLandmarks = countriesWithLandmarks.flatMap { country ->
            country.landmarks.filter { landmark ->
                !usedLandmarks.contains("${country.name}_${landmark.imagePath}")
            }.map { landmark -> country to landmark }
        }

        if (availableLandmarks.isEmpty()) {
            showQuizCompleted()
            return
        }

        // Select a random available landmark
        val (targetCountry, targetLandmark) = availableLandmarks.random()
        usedLandmarks.add("${targetCountry.name}_${targetLandmark.imagePath}")
        questionsRemaining--
        updateScoreAndRemaining()

        currentLandmarkPath = targetLandmark.imagePath
        loadLandmarkImage(targetLandmark.imagePath)

        // Get 3 other random countries (excluding target country)
        val otherCountries = countriesWithLandmarks
            .filter { it != targetCountry }
            .shuffled()
            .take(3)

        val answerOptions = (listOf(targetCountry) + otherCountries).shuffled()
        correctAnswerIndex = answerOptions.indexOf(targetCountry)

        // Update UI
        questionText.text = TranslationUtils.getTranslatedString(this, R.string.landmark_question, currentLanguage)
        option1Btn.text = if (currentLanguage != "en") answerOptions[0].translatedName else answerOptions[0].name
        option2Btn.text = if (currentLanguage != "en") answerOptions[1].translatedName else answerOptions[1].name
        option3Btn.text = if (currentLanguage != "en") answerOptions[2].translatedName else answerOptions[2].name
        option4Btn.text = if (currentLanguage != "en") answerOptions[3].translatedName else answerOptions[3].name

        option1Btn.setOnClickListener { checkAnswer(0) }
        option2Btn.setOnClickListener { checkAnswer(1) }
        option3Btn.setOnClickListener { checkAnswer(2) }
        option4Btn.setOnClickListener { checkAnswer(3) }

        nextButton.setOnClickListener {
            loadLandmarkQuestion()
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun updateScoreAndRemaining() {
        scoreText.text = getString(R.string.score_format, score, totalQuestions)
        remainingText.text = getString(R.string.remaining_format, questionsRemaining)
    }

    private fun loadLandmarkImage(imagePath: String) {
        try {
            val inputStream: InputStream = assets.open(imagePath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            landmarkImageView.setImageBitmap(bitmap)
            inputStream.close()
            Log.d("LandmarkQuiz", "Successfully loaded landmark: $imagePath")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("LandmarkQuiz", "Error loading landmark: ${e.message}")
            landmarkImageView.setImageResource(R.drawable.ic_landmark)
        }
    }

    private fun checkAnswer(selectedIndex: Int) {
        val isCorrect = selectedIndex == correctAnswerIndex
        feedbackText.text = if (isCorrect) {
            score++
            updateScoreAndRemaining()
            TranslationUtils.getTranslatedString(this, R.string.correct, currentLanguage)
        } else {
            TranslationUtils.getTranslatedString(this, R.string.wrong, currentLanguage)
        }
    }

    private fun showError() {
        questionText.text = TranslationUtils.getTranslatedString(this, R.string.notLandmark, currentLanguage)
        option1Btn.visibility = View.GONE
        option2Btn.visibility = View.GONE
        option3Btn.visibility = View.GONE
        option4Btn.visibility = View.GONE
        nextButton.visibility = View.GONE
    }

    private fun showQuizCompleted() {
        questionText.text = TranslationUtils.getTranslatedStringWithFormat(
            this,
            R.string.quiz_complete,
            currentLanguage,
            score,
            totalQuestions
        )
        option1Btn.visibility = View.GONE
        option2Btn.visibility = View.GONE
        option3Btn.visibility = View.GONE
        option4Btn.visibility = View.GONE
        nextButton.text = getString(R.string.restart_quiz)
        nextButton.setOnClickListener {
            initializeQuiz()
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}
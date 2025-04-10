package com.example.geographyquiz.quiz

import android.annotation.SuppressLint
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

    private var currentLanguage = "en"
    private lateinit var databaseHelper: CountryDatabase
    private lateinit var flagImageView: ImageView
    private var correctAnswerIndex = 0
    private var score = 0
    private var totalQuestions = 0
    private var questionsRemaining = 0
    private lateinit var countries: List<Country>
    private val usedCountries = mutableSetOf<String>()

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
        flagImageView = findViewById(R.id.flagImageView)
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
        usedCountries.clear()
        countries = if (currentLanguage != "en") {
            databaseHelper.getTranslatedRandomCountries(20, currentLanguage)
        } else {
            databaseHelper.getRandomCountries(20)
        }

        option1Btn.visibility = View.VISIBLE
        option2Btn.visibility = View.VISIBLE
        option3Btn.visibility = View.VISIBLE
        option4Btn.visibility = View.VISIBLE
        nextButton.visibility = View.VISIBLE
        nextButton.text = getString(R.string.next_question)

        if (countries.size < 4) {
            showError()
            return
        }

        totalQuestions = countries.size
        questionsRemaining = totalQuestions
        updateScoreAndRemaining()

        loadFlagQuestion()
    }

    private fun loadFlagQuestion() {
        feedbackText.text = ""

        if (questionsRemaining <= 0) {
            showQuizCompleted()
            return
        }

        // Get available countries that haven't been used yet
        val availableCountries = countries.filter { !usedCountries.contains(it.name) }
        if (availableCountries.isEmpty()) {
            showQuizCompleted()
            return
        }

        val targetCountry = availableCountries.random()
        usedCountries.add(targetCountry.name)
        questionsRemaining--
        updateScoreAndRemaining()

        loadSvgFlag(targetCountry.countryCode)

        // Get 3 other random countries (excluding target country)
        val otherCountries = countries
            .filter { it.name != targetCountry.name }
            .shuffled()
            .take(3)

        val answerOptions = (listOf(targetCountry) + otherCountries).shuffled()
        correctAnswerIndex = answerOptions.indexOf(targetCountry)

        // Update UI
        questionText.text = TranslationUtils.getTranslatedString(this, R.string.flag_question, currentLanguage)
        option1Btn.text = if (currentLanguage != "en") answerOptions[0].translatedName else answerOptions[0].name
        option2Btn.text = if (currentLanguage != "en") answerOptions[1].translatedName else answerOptions[1].name
        option3Btn.text = if (currentLanguage != "en") answerOptions[2].translatedName else answerOptions[2].name
        option4Btn.text = if (currentLanguage != "en") answerOptions[3].translatedName else answerOptions[3].name

        option1Btn.setOnClickListener { checkAnswer(0) }
        option2Btn.setOnClickListener { checkAnswer(1) }
        option3Btn.setOnClickListener { checkAnswer(2) }
        option4Btn.setOnClickListener { checkAnswer(3) }

        nextButton.setOnClickListener {
            loadFlagQuestion()
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun updateScoreAndRemaining() {
        scoreText.text = getString(R.string.score_format, score, totalQuestions)
        remainingText.text = getString(R.string.remaining_format, questionsRemaining)
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
        questionText.text = TranslationUtils.getTranslatedString(this, R.string.notEnoughCountry, currentLanguage)
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
            nextButton.text = getString(R.string.next_question)
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}
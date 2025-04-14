package com.example.geographyquiz.quiz

import android.annotation.SuppressLint
import android.graphics.drawable.PictureDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.max
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
    private lateinit var currentCountry: Country

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
        supportActionBar?.hide()
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
            databaseHelper.getTranslatedRandomCountries(28, currentLanguage)
        } else {
            databaseHelper.getRandomCountries(28)
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

        // Reset used countries if we're running low
        if (usedCountries.size >= countries.size - 3) {
            usedCountries.clear()
        }

        // Get available countries with null checks
        val availableCountries = countries.filter {
            !usedCountries.contains(it.name) &&
                    it.name.isNotBlank() &&
                    it.countryCode.isNotBlank()
        }

        if (availableCountries.isEmpty()) {
            showQuizCompleted()
            return
        }

        // Select current country
        currentCountry = availableCountries.random()
        usedCountries.add(currentCountry.name)
        questionsRemaining--
        updateScoreAndRemaining()

        // Load the flag image
        loadSvgFlag(currentCountry.countryCode)

        // Create answer options with weighted probabilities
        val answerOptions = mutableListOf<Country>().apply {
            add(currentCountry) // Correct answer

            // Get all other available countries
            val otherCountries = countries.filter {
                it.name != currentCountry.name &&
                        it.name.isNotBlank() &&
                        it.countryCode.isNotBlank()
            }

            // Categorize countries based on similarity to current country
            val categorizedCountries = otherCountries.map { country ->
                var similarityScore = 0

                // 1. Flag similarity (highest weight)
                val colorMatches = currentCountry.flagColors.intersect(country.flagColors).size
                val emblemMatches = currentCountry.flagEmblem.intersect(country.flagEmblem).size
                similarityScore += (colorMatches * 20) + (emblemMatches * 30)

                // 2. Geographic proximity (medium weight)
                if (country.continent == currentCountry.continent) similarityScore += 15
                if (country.region == currentCountry.region) similarityScore += 20
                if (country.category == currentCountry.category) similarityScore += 25

                // Categorize based on score
                when {
                    similarityScore >= 70 -> CountryWithScore(country, similarityScore, Category.HIGH)
                    similarityScore >= 40 -> CountryWithScore(country, similarityScore, Category.MEDIUM)
                    else -> CountryWithScore(country, similarityScore, Category.LOW)
                }
            }

            // Group by category
            val highScoreCountries = categorizedCountries.filter { it.category == Category.HIGH }
            val mediumScoreCountries = categorizedCountries.filter { it.category == Category.MEDIUM }
            val lowScoreCountries = categorizedCountries.filter { it.category == Category.LOW }

            Log.d("FlagQuiz", "High similarity options: ${highScoreCountries.size}")
            Log.d("FlagQuiz", "Medium similarity options: ${mediumScoreCountries.size}")
            Log.d("FlagQuiz", "Low similarity options: ${lowScoreCountries.size}")

            // Select 3 distractors with weighted probabilities
            val selectedDistractors = mutableSetOf<Country>()
            val random = Random()

            while (selectedDistractors.size < 3 && selectedDistractors.size < otherCountries.size) {
                val rand = random.nextDouble() // Random value between 0.0 and 1.0

                when {
                    // 80% chance to pick from high similarity
                    rand < 0.8 && highScoreCountries.isNotEmpty() -> {
                        val country = highScoreCountries.random().country
                        if (!selectedDistractors.contains(country)) {
                            selectedDistractors.add(country)
                        }
                    }
                    // 50% chance (of remaining 20%) = 10% total chance for medium
                    rand < 0.9 && mediumScoreCountries.isNotEmpty() -> {
                        val country = mediumScoreCountries.random().country
                        if (!selectedDistractors.contains(country)) {
                            selectedDistractors.add(country)
                        }
                    }
                    // 20% chance (of remaining 10%) = 2% total chance for low
                    rand < 1.0 && lowScoreCountries.isNotEmpty() -> {
                        val country = lowScoreCountries.random().country
                        if (!selectedDistractors.contains(country)) {
                            selectedDistractors.add(country)
                        }
                    }
                    // Fallback to random selection if categories are empty
                    else -> {
                        val remaining = otherCountries.filter { it !in selectedDistractors }
                        if (remaining.isNotEmpty()) {
                            selectedDistractors.add(remaining.random())
                        }
                    }
                }
            }

            addAll(selectedDistractors)

            // If we still don't have enough options, fill with random
            if (size < 4) {
                val remainingOptions = otherCountries
                    .filter { it !in this }
                    .shuffled()
                    .take(4 - size)
                addAll(remainingOptions)
            }
        }.shuffled()

        // Set correct answer index
        correctAnswerIndex = answerOptions.indexOfFirst { it.name == currentCountry.name }

        // Update UI
        questionText.text = getString(R.string.flag_question)
        option1Btn.text = answerOptions[0].name
        option2Btn.text = answerOptions[1].name
        option3Btn.text = answerOptions[2].name
        option4Btn.text = answerOptions[3].name

        // Reset button states
        listOf(option1Btn, option2Btn, option3Btn, option4Btn).forEach {
            it.isEnabled = true
            it.visibility = View.VISIBLE
        }

        // Set click listeners
        option1Btn.setOnClickListener { checkAnswer(0) }
        option2Btn.setOnClickListener { checkAnswer(1) }
        option3Btn.setOnClickListener { checkAnswer(2) }
        option4Btn.setOnClickListener { checkAnswer(3) }

        nextButton.setOnClickListener { loadFlagQuestion() }
    }

    // Supporting classes
    private enum class Category { HIGH, MEDIUM, LOW }
    private data class CountryWithScore(val country: Country, val score: Int, val category: Category)

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
            val correctCountryName = if (currentLanguage != "en")
                currentCountry.translatedName else currentCountry.name
            TranslationUtils.getTranslatedStringWithFormat(
                this,
                R.string.wrong,
                currentLanguage,
                correctCountryName
            )
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

    private data class WeightedCountry(val country: Country, val weight: Int)
}
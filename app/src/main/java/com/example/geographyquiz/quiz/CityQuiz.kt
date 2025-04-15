package com.example.geographyquiz.quiz

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.geographyquiz.R
import com.example.geographyquiz.data.Country
import com.example.geographyquiz.data.CountryDatabase
import com.example.geographyquiz.utils.TranslationUtils
import kotlin.random.Random

class CityQuiz : AppCompatActivity() {

    private var correctAnswerIndex = 0
    private lateinit var databaseHelper: CountryDatabase
    private var currentLanguage = "en"
    private var score = 0
    private var totalQuestions = 0
    private var questionsRemaining = 0
    private lateinit var countries: List<Country>
    private val usedCountryQuestions = mutableSetOf<Pair<String, QuestionType>>()
    private lateinit var currentCountry: Country
    private lateinit var currentQuestionType: QuestionType

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

    enum class QuestionType {
        CAPITAL,
        BIGGEST_CITY,
        CITY_IN_COUNTRY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        // Initialize UI components
        questionText = findViewById(R.id.questionText)
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

        // Initialize quiz
        initializeQuiz()
    }

    private fun initializeQuiz() {
        score = 0
        usedCountryQuestions.clear()
        countries = if (currentLanguage != "en") {
            databaseHelper.getTranslatedRandomCountries(85, currentLanguage)
        } else {
            databaseHelper.getRandomCountries(85)
        }

        if (countries.size < 4) {
            showNotEnoughCountriesError()
            return
        }

        totalQuestions = countries.size * QuestionType.values().size
        questionsRemaining = totalQuestions
        updateScoreAndRemaining()

        loadCityQuestion()
    }

    private fun loadCityQuestion() {
        feedbackText.text = ""

        if (questionsRemaining <= 0) {
            showQuizCompleted()
            return
        }

        // Get a country and question type that hasn't been used yet
        val availableQuestions = countries.flatMap { country ->
            QuestionType.values()
                .filter { type -> !usedCountryQuestions.contains(country.name to type) }
                .map { type -> country to type }
        }

        if (availableQuestions.isEmpty()) {
            showQuizCompleted()
            return
        }

        val (country, questionType) = availableQuestions.random()
        currentCountry = country
        currentQuestionType = questionType
        usedCountryQuestions.add(country.name to questionType)
        questionsRemaining--
        updateScoreAndRemaining()

        val (question, correctAnswer, allAnswers) = when (questionType) {
            QuestionType.CAPITAL -> generateCapitalQuestion(country)
            QuestionType.BIGGEST_CITY -> generateBiggestCityQuestion(country)
            QuestionType.CITY_IN_COUNTRY -> generateCityInCountryQuestion(country)
        }

        questionText.text = question
        option1Btn.text = allAnswers[0]
        option2Btn.text = allAnswers[1]
        option3Btn.text = allAnswers[2]
        option4Btn.text = allAnswers[3]

        // Make sure all buttons are visible
        option1Btn.visibility = View.VISIBLE
        option2Btn.visibility = View.VISIBLE
        option3Btn.visibility = View.VISIBLE
        option4Btn.visibility = View.VISIBLE

        correctAnswerIndex = allAnswers.indexOf(correctAnswer) + 1
        if (correctAnswerIndex == 0) { // Shouldn't happen as correct answer is always included
            correctAnswerIndex = 1
        }

        option1Btn.setOnClickListener { checkAnswer(1) }
        option2Btn.setOnClickListener { checkAnswer(2) }
        option3Btn.setOnClickListener { checkAnswer(3) }
        option4Btn.setOnClickListener { checkAnswer(4) }

        nextButton.setOnClickListener {
            loadCityQuestion()
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun updateScoreAndRemaining() {
        scoreText.text = getString(R.string.score_format, score, totalQuestions)
        remainingText.text = getString(R.string.remaining_format, questionsRemaining)
    }

    private fun getSimilarCountries(targetCountry: Country): List<Country> {
        return countries.filter { otherCountry ->
            otherCountry.name != targetCountry.name
        }.sortedByDescending { otherCountry ->
            when {
                otherCountry.category == targetCountry.category -> 3
                otherCountry.region == targetCountry.region -> 2
                otherCountry.continent == targetCountry.continent -> 1
                else -> 0
            }
        }
    }

    private fun getCitiesFromCountry(country: Country): List<String> {
        return listOfNotNull(
            if (currentLanguage != "en") country.translatedCapital else country.capital,
            if (currentLanguage != "en") country.translatedBigCity else country.bigCity,
            if (currentLanguage != "en") country.translatedSecondCity else country.secondCity,
            if (currentLanguage != "en") country.translatedThirdCity else country.thirdCity
        ).filter { it.isNotBlank() }.distinct()
    }

    private fun generateBiggestCityQuestion(
        targetCountry: Country
    ): Triple<String, String, List<String>> {
        val correctAnswer = if (currentLanguage != "en") targetCountry.translatedBigCity else targetCountry.bigCity
        val targetCities = getCitiesFromCountry(targetCountry)
        val similarCountries = getSimilarCountries(targetCountry)

        // Create answer options
        val answerOptions = mutableListOf<String>().apply {
            add(correctAnswer) // Always include correct answer

            // Get other cities from same country (excluding correct answer)
            val sameCountryOtherCities = targetCities.filter { it != correctAnswer }

            // Second option - 80% chance of being from same country
            if (sameCountryOtherCities.isNotEmpty() && Random.nextFloat() < 0.8f) {
                add(sameCountryOtherCities.random())
            } else {
                add(getRandomCityFromSimilarCountries(similarCountries, this))
            }

            // Third option - 50% chance of being from same country
            if (sameCountryOtherCities.isNotEmpty() && Random.nextFloat() < 0.5f) {
                add(sameCountryOtherCities.random())
            } else {
                add(getRandomCityFromSimilarCountries(similarCountries, this))
            }

            // Fourth option - 20% chance of being from same country (if we have enough cities)
            if (sameCountryOtherCities.size >= 3 && Random.nextFloat() < 0.2f) {
                add(sameCountryOtherCities.random())
            } else {
                add(getRandomCityFromSimilarCountries(similarCountries, this))
            }
        }

        return Triple(
            TranslationUtils.getTranslatedStringWithFormat(
                this,
                R.string.city_question,
                currentLanguage,
                if (currentLanguage != "en") targetCountry.translatedName else targetCountry.name
            ),
            correctAnswer,
            answerOptions.shuffled()
        )
    }

    private fun generateCapitalQuestion(
        targetCountry: Country
    ): Triple<String, String, List<String>> {
        val correctAnswer = if (currentLanguage != "en") targetCountry.translatedCapital else targetCountry.capital
        val targetCities = getCitiesFromCountry(targetCountry)
        val similarCountries = getSimilarCountries(targetCountry)

        // Create answer options
        val answerOptions = mutableListOf<String>().apply {
            add(correctAnswer) // Always include correct answer

            // Get other cities from same country (excluding correct answer)
            val sameCountryOtherCities = targetCities.filter { it != correctAnswer }

            // Second option - 80% chance of being from same country
            if (sameCountryOtherCities.isNotEmpty() && Random.nextFloat() < 0.8f) {
                add(sameCountryOtherCities.random())
            } else {
                add(getRandomCapitalFromSimilarCountries(similarCountries, this))
            }

            // Third option - 50% chance of being from same country
            if (sameCountryOtherCities.isNotEmpty() && Random.nextFloat() < 0.5f) {
                add(sameCountryOtherCities.random())
            } else {
                add(getRandomCapitalFromSimilarCountries(similarCountries, this))
            }

            // Fourth option - 20% chance of being from same country (if we have enough cities)
            if (sameCountryOtherCities.size >= 3 && Random.nextFloat() < 0.2f) {
                add(sameCountryOtherCities.random())
            } else {
                add(getRandomCapitalFromSimilarCountries(similarCountries, this))
            }
        }

        return Triple(
            TranslationUtils.getTranslatedStringWithFormat(
                this,
                R.string.capital_question,
                currentLanguage,
                if (currentLanguage != "en") targetCountry.translatedName else targetCountry.name
            ),
            correctAnswer,
            answerOptions.shuffled()
        )
    }

    private fun generateCityInCountryQuestion(
        targetCountry: Country
    ): Triple<String, String, List<String>> {
        val targetCities = getCitiesFromCountry(targetCountry)
        val correctCity = targetCities.random()
        val similarCountries = getSimilarCountries(targetCountry)

        // Create answer options
        val answerOptions = mutableListOf<String>().apply {
            add(correctCity) // Always include correct answer

            // Get other cities from same country (excluding correct answer)
            val sameCountryOtherCities = targetCities.filter { it != correctCity }

            // Second option - 80% chance of being from same country
            if (sameCountryOtherCities.isNotEmpty() && Random.nextFloat() < 0.8f) {
                add(sameCountryOtherCities.random())
            } else {
                add(getRandomCityFromSimilarCountries(similarCountries, this))
            }

            // Third option - 50% chance of being from same country
            if (sameCountryOtherCities.isNotEmpty() && Random.nextFloat() < 0.5f) {
                add(sameCountryOtherCities.random())
            } else {
                add(getRandomCityFromSimilarCountries(similarCountries, this))
            }

            // Fourth option - 20% chance of being from same country (if we have enough cities)
            if (sameCountryOtherCities.size >= 3 && Random.nextFloat() < 0.2f) {
                add(sameCountryOtherCities.random())
            } else {
                add(getRandomCityFromSimilarCountries(similarCountries, this))
            }
        }

        return Triple(
            TranslationUtils.getTranslatedStringWithFormat(
                this,
                R.string.city_in_country_question,
                currentLanguage,
                if (currentLanguage != "en") targetCountry.translatedName else targetCountry.name
            ),
            correctCity,
            answerOptions.shuffled()
        )
    }

    private fun getRandomCityFromSimilarCountries(
        similarCountries: List<Country>,
        exclude: List<String>
    ): String {
        // Try to get cities from similar countries (prioritizing more similar ones)
        val candidates = similarCountries.flatMap { country ->
            getCitiesFromCountry(country).filter { it !in exclude }
        }.distinct()

        return if (candidates.isNotEmpty()) {
            candidates.random()
        } else {
            // Fallback to any city not in exclude list
            countries.flatMap { getCitiesFromCountry(it) }
                .filter { it !in exclude }
                .randomOrNull() ?: "Unknown"
        }
    }

    private fun getRandomCapitalFromSimilarCountries(
        similarCountries: List<Country>,
        exclude: List<String>
    ): String {
        // Try to get capitals from similar countries (prioritizing more similar ones)
        val candidates = similarCountries.mapNotNull { country ->
            (if (currentLanguage != "en") country.translatedCapital else country.capital)
                .takeIf { it.isNotBlank() && it !in exclude }
        }.distinct()

        return if (candidates.isNotEmpty()) {
            candidates.random()
        } else {
            // Fallback to any capital not in exclude list
            countries.mapNotNull {
                (if (currentLanguage != "en") it.translatedCapital else it.capital)
                    .takeIf { it.isNotBlank() && it !in exclude }
            }.randomOrNull() ?: "Unknown"
        }
    }

    private fun showNotEnoughCountriesError() {
        questionText.text = TranslationUtils.getTranslatedString(
            this,
            R.string.notEnoughCountry,
            currentLanguage
        )
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

    private fun checkAnswer(selectedOption: Int) {
        val isCorrect = selectedOption == correctAnswerIndex
        feedbackText.text = if (isCorrect) {
            score++
            updateScoreAndRemaining()
            TranslationUtils.getTranslatedString(this, R.string.correct, currentLanguage)
        } else {
            TranslationUtils.getTranslatedString(this, R.string.wrong, currentLanguage)
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}
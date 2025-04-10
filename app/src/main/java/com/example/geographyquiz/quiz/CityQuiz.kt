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
import java.util.*
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
            databaseHelper.getTranslatedRandomCountries(10, currentLanguage)
        } else {
            databaseHelper.getRandomCountries(10)
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

        // Ensure we have exactly 4 answers (pad with empty strings if needed)
        val paddedAnswers = if (allAnswers.size < 4) {
            allAnswers + List(4 - allAnswers.size) { "" }
        } else {
            allAnswers.take(4)
        }

        questionText.text = question
        option1Btn.text = paddedAnswers[0].ifEmpty { getString(R.string.unknownAns) }
        option2Btn.text = paddedAnswers[1].ifEmpty { getString(R.string.unknownAns) }
        option3Btn.text = paddedAnswers[2].ifEmpty { getString(R.string.unknownAns) }
        option4Btn.text = paddedAnswers[3].ifEmpty { getString(R.string.unknownAns) }

        // Hide buttons with empty answers
        option1Btn.visibility = if (paddedAnswers[0].isNotEmpty()) View.VISIBLE else View.GONE
        option2Btn.visibility = if (paddedAnswers[1].isNotEmpty()) View.VISIBLE else View.GONE
        option3Btn.visibility = if (paddedAnswers[2].isNotEmpty()) View.VISIBLE else View.GONE
        option4Btn.visibility = if (paddedAnswers[3].isNotEmpty()) View.VISIBLE else View.GONE

        correctAnswerIndex = paddedAnswers.indexOf(correctAnswer) + 1
        if (correctAnswerIndex == 0) { // Correct answer not found in options
            correctAnswerIndex = 1 // Default to first option
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
            when {
                otherCountry.name == targetCountry.name -> false
                otherCountry.category == targetCountry.category -> true
                otherCountry.region == targetCountry.region -> true
                otherCountry.continent == targetCountry.continent -> true
                else -> false
            }
        }
    }

    private fun generateBiggestCityQuestion(
        targetCountry: Country
    ): Triple<String, String, List<String>> {
        val correctAnswer = if (currentLanguage != "en") targetCountry.translatedBigCity else targetCountry.bigCity
        val similarCountries = getSimilarCountries(targetCountry)

        // Get other cities from the same country
        val sameCountryCities = listOfNotNull(
            if (currentLanguage != "en") targetCountry.translatedCapital else targetCountry.capital,
            if (currentLanguage != "en") targetCountry.translatedSecondCity else targetCountry.secondCity,
            if (currentLanguage != "en") targetCountry.translatedThirdCity else targetCountry.thirdCity
        ).filter { it.isNotBlank() && it != correctAnswer }.distinct()

        // Create answer options
        val answerOptions = mutableListOf<String>().apply {
            add(correctAnswer) // Always include correct answer

            // Include other cities from same country if available
            if (sameCountryCities.isNotEmpty()) {
                add(sameCountryCities.random())
                if (sameCountryCities.size > 1) {
                    sameCountryCities.filterNot { it in this }.randomOrNull()?.let { add(it) }
                }
            }

            // Fill remaining slots with cities from similar countries
            val similarOptions = similarCountries
                .map { if (currentLanguage != "en") it.translatedBigCity else it.bigCity }
                .filter { it.isNotBlank() && it !in this }
                .distinct()
                .shuffled()

            addAll(similarOptions.take(4 - this.size))
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
        val similarCountries = getSimilarCountries(targetCountry)

        // Get other cities from the same country
        val sameCountryCities = listOfNotNull(
            if (currentLanguage != "en") targetCountry.translatedBigCity else targetCountry.bigCity,
            if (currentLanguage != "en") targetCountry.translatedSecondCity else targetCountry.secondCity,
            if (currentLanguage != "en") targetCountry.translatedThirdCity else targetCountry.thirdCity
        ).filter { it.isNotBlank() && it != correctAnswer }.distinct()

        // Create answer options
        val answerOptions = mutableListOf<String>().apply {
            add(correctAnswer) // Always include correct answer

            // Include other cities from same country if available
            if (sameCountryCities.isNotEmpty()) {
                add(sameCountryCities.random())
                if (sameCountryCities.size > 1) {
                    sameCountryCities.filterNot { it in this }.randomOrNull()?.let { add(it) }
                }
            }

            // Fill remaining slots with capitals from similar countries
            val similarOptions = similarCountries
                .map { if (currentLanguage != "en") it.translatedCapital else it.capital }
                .filter { it.isNotBlank() && it !in this }
                .distinct()
                .shuffled()

            addAll(similarOptions.take(4 - this.size))
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
        val similarCountries = getSimilarCountries(targetCountry)

        val targetCities = listOfNotNull(
            if (currentLanguage != "en") targetCountry.translatedCapital else targetCountry.capital,
            if (currentLanguage != "en") targetCountry.translatedBigCity else targetCountry.bigCity,
            if (currentLanguage != "en") targetCountry.translatedSecondCity else targetCountry.secondCity,
            if (currentLanguage != "en") targetCountry.translatedThirdCity else targetCountry.thirdCity
        ).filter { it.isNotBlank() }.distinct()

        val correctCity = targetCities.random()

        // Get cities from similar countries
        val similarOptions = similarCountries
            .flatMap { country ->
                listOfNotNull(
                    if (currentLanguage != "en") country.translatedCapital else country.capital,
                    if (currentLanguage != "en") country.translatedBigCity else country.bigCity,
                    if (currentLanguage != "en") country.translatedSecondCity else country.secondCity,
                    if (currentLanguage != "en") country.translatedThirdCity else country.thirdCity
                )
            }
            .filter { it.isNotBlank() && !targetCities.contains(it) }
            .distinct()
            .shuffled()
            .take(3)

        return Triple(
            TranslationUtils.getTranslatedStringWithFormat(
                this,
                R.string.city_in_country_question,
                currentLanguage,
                if (currentLanguage != "en") targetCountry.translatedName else targetCountry.name
            ),
            correctCity,
            (listOf(correctCity) + similarOptions).shuffled()
        )
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
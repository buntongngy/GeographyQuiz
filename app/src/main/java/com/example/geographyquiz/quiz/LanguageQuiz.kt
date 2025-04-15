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

class LanguageQuiz : AppCompatActivity() {

    private var correctAnswerIndex = 0
    private lateinit var databaseHelper: CountryDatabase
    private var currentLanguage = "en"
    private var score = 0
    private var totalQuestions = 0
    private var questionsRemaining = 0
    private lateinit var countries: List<Country>
    private val usedQuestions = mutableSetOf<Pair<String, QuestionType>>()

    enum class QuestionType {
        LANGUAGE,
        LANGUAGE_COUNT
    }

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
        setContentView(R.layout.activity_quiz)

        initializeUI()
        initializeQuiz()
    }

    private fun initializeUI() {
        questionText = findViewById(R.id.questionText)
        option1Btn = findViewById(R.id.option1Button)
        option2Btn = findViewById(R.id.option2Button)
        option3Btn = findViewById(R.id.option3Button)
        option4Btn = findViewById(R.id.option4Button)
        feedbackText = findViewById(R.id.feedbackText)
        scoreText = findViewById(R.id.scoreText)
        remainingText = findViewById(R.id.remainingText)
        nextButton = findViewById(R.id.nextButton)
    }

    private fun initializeQuiz() {
        score = 0
        usedQuestions.clear()

        val sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
        currentLanguage = sharedPref.getString("app_language", "en") ?: "en"
        databaseHelper = CountryDatabase(this)

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

        loadLanguageQuestion()
    }

    private fun loadLanguageQuestion() {
        feedbackText.text = ""

        if (questionsRemaining <= 0) {
            showQuizCompleted()
            return
        }

        // Get available questions (country + type combinations not yet used)
        val availableQuestions = countries.flatMap { country ->
            QuestionType.values()
                .filter { type -> !usedQuestions.contains(country.name to type) }
                .map { type -> country to type }
        }

        if (availableQuestions.isEmpty()) {
            showQuizCompleted()
            return
        }

        // Select a random available question
        val (targetCountry, questionType) = availableQuestions.random()
        usedQuestions.add(targetCountry.name to questionType)
        questionsRemaining--
        updateScoreAndRemaining()

        // Generate the question data
        val questionData = when (questionType) {
            QuestionType.LANGUAGE -> generateLanguageQuestion(targetCountry, countries)
            QuestionType.LANGUAGE_COUNT -> generateLanguageCountQuestion(targetCountry)
        }

        // Display the question and answers
        displayQuestion(questionData)
    }

    private fun displayQuestion(questionData: QuestionData) {
        // Ensure we have exactly 4 answers (pad with empty strings if needed)
        val paddedAnswers = if (questionData.answers.size < 4) {
            questionData.answers + List(4 - questionData.answers.size) { "" }
        } else {
            questionData.answers.take(4)
        }

        questionText.text = questionData.question
        option1Btn.text = paddedAnswers[0].ifEmpty { getString(R.string.unknownAns) }
        option2Btn.text = paddedAnswers[1].ifEmpty { getString(R.string.unknownAns) }
        option3Btn.text = paddedAnswers[2].ifEmpty { getString(R.string.unknownAns) }
        option4Btn.text = paddedAnswers[3].ifEmpty { getString(R.string.unknownAns) }

        // Hide buttons with empty answers
        option1Btn.visibility = if (paddedAnswers[0].isNotEmpty()) View.VISIBLE else View.GONE
        option2Btn.visibility = if (paddedAnswers[1].isNotEmpty()) View.VISIBLE else View.GONE
        option3Btn.visibility = if (paddedAnswers[2].isNotEmpty()) View.VISIBLE else View.GONE
        option4Btn.visibility = if (paddedAnswers[3].isNotEmpty()) View.VISIBLE else View.GONE

        correctAnswerIndex = paddedAnswers.indexOf(questionData.correctAnswer)
        if (correctAnswerIndex == -1) {
            correctAnswerIndex = 0
        }

        option1Btn.setOnClickListener { checkAnswer(0) }
        option2Btn.setOnClickListener { checkAnswer(1) }
        option3Btn.setOnClickListener { checkAnswer(2) }
        option4Btn.setOnClickListener { checkAnswer(3) }

        nextButton.setOnClickListener {
            loadLanguageQuestion()
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun updateScoreAndRemaining() {
        scoreText.text = getString(R.string.score_format, score, totalQuestions)
        remainingText.text = getString(R.string.remaining_format, questionsRemaining)
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

    private data class QuestionData(
        val question: String,
        val correctAnswer: String,
        val answers: List<String>
    )

    private fun generateLanguageQuestion(
        targetCountry: Country,
        allCountries: List<Country>
    ): QuestionData {
        val targetLanguages = if (currentLanguage != "en") {
            targetCountry.translatedLanguages.ifEmpty { targetCountry.languages }
        } else {
            targetCountry.languages
        }

        if (targetLanguages.isEmpty()) {
            return generateLanguageCountQuestion(targetCountry)
        }

        val correctLanguage = targetLanguages.random()
        val countryName = getCountryDisplayName(targetCountry)

        val similarCountries = allCountries.filter { it.name != targetCountry.name }
        val otherLanguages = similarCountries
            .flatMap { if (currentLanguage != "en") it.translatedLanguages else it.languages }
            .filter { it != correctLanguage }
            .distinct()
            .shuffled()
            .take(3)

        val answers = (listOf(correctLanguage) + otherLanguages).shuffled()

        return QuestionData(
            TranslationUtils.getTranslatedStringWithFormat(
                this,
                R.string.languageQuestion,
                currentLanguage,
                countryName
            ),
            correctLanguage,
            answers
        )
    }

    private fun generateLanguageCountQuestion(targetCountry: Country): QuestionData {
        val correctCount = targetCountry.languages.size
        val possibleCounts = mutableListOf<Int>()

        // Generate plausible nearby counts
        when {
            correctCount == 1 -> possibleCounts.addAll(listOf(1, 2, 4, 3))
            correctCount == 2 -> possibleCounts.addAll(listOf(2, 1, 3, 4))
            else -> possibleCounts.addAll(listOf(
                correctCount,
                correctCount - 1,
                correctCount + 1,
                if (correctCount > 2) correctCount - 2 else correctCount + 2
            ))
        }

        // Ensure all counts are positive and we have exactly 4 unique options
        val uniqueCounts = possibleCounts
            .filter { it >= (if (correctCount == 1)1 else 0) }
            .distinct()
            .take(4)
            .toMutableList()

        // If we somehow don't have enough, fill with reasonable numbers
        while (uniqueCounts.size < 4) {
            val nextNum = uniqueCounts.maxOrNull()?.plus(1) ?: 1
            if (!uniqueCounts.contains(nextNum)) {
                uniqueCounts.add(nextNum)
            } else {
                uniqueCounts.add((1..10).random())
            }
        }

        val answers = uniqueCounts.shuffled().map { it.toString() }

        return QuestionData(
            TranslationUtils.getTranslatedStringWithFormat(this,
                R.string.languageCountQuestion,
                currentLanguage,
                if (currentLanguage != "en") targetCountry.translatedName else targetCountry.name
            ),
            correctCount.toString(),
            answers
        )
    }

    private fun getCountryDisplayName(country: Country): String {
        return if (currentLanguage != "en" && country.translatedName.isNotEmpty()) {
            country.translatedName
        } else {
            country.name
        }
    }

    private fun checkAnswer(selectedIndex: Int) {
        val feedbackText = findViewById<TextView>(R.id.feedbackText)
        feedbackText.text = if (selectedIndex == correctAnswerIndex) {
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
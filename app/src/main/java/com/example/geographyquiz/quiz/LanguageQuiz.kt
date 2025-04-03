package com.example.geographyquiz.quiz

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.geographyquiz.R
import com.example.geographyquiz.data.Country
import com.example.geographyquiz.data.CountryDatabase

class LanguageQuiz : AppCompatActivity() {

    private var correctAnswerIndex = 0
    private lateinit var databaseHelper: CountryDatabase
    private var currentLanguage = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        // Get current language
        val sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
        currentLanguage = sharedPref.getString("app_language", "en") ?: "en"

        databaseHelper = CountryDatabase(this)
        loadLanguageQuestion()
    }

    private fun loadLanguageQuestion() {
        val questionText = findViewById<TextView>(R.id.questionText)
        val option1Btn = findViewById<Button>(R.id.option1Button)
        val option2Btn = findViewById<Button>(R.id.option2Button)
        val option3Btn = findViewById<Button>(R.id.option3Button)
        val option4Btn = findViewById<Button>(R.id.option4Button)
        val feedbackText = findViewById<TextView>(R.id.feedbackText)
        val nextButton = findViewById<Button>(R.id.nextButton)

        feedbackText.text = ""

        // Get countries with translations if needed
        val countries = if (currentLanguage != "en") {
            databaseHelper.getTranslatedRandomCountries(10, currentLanguage)
        } else {
            databaseHelper.getRandomCountries(10)
        }

        if (countries.isEmpty()) {
            showNotEnoughCountriesError(questionText, option1Btn, option2Btn, option3Btn, option4Btn)
            return
        }

        val targetCountry = countries.random()
        val questionData = when ((0..1).random()) {
            0 -> generateLanguageQuestion(targetCountry, countries)
            else -> generateLanguageCountQuestion(targetCountry)
        }

        // Update UI
        questionText.text = questionData.question
        option1Btn.text = questionData.answers[0]
        option2Btn.text = questionData.answers[1]
        option3Btn.text = questionData.answers[2]
        option4Btn.text = questionData.answers[3]

        correctAnswerIndex = questionData.answers.indexOf(questionData.correctAnswer)
        if (correctAnswerIndex == -1) {
            showNotEnoughCountriesError(questionText, option1Btn, option2Btn, option3Btn, option4Btn)
            return
        }

        option1Btn.setOnClickListener { checkAnswer(1) }
        option2Btn.setOnClickListener { checkAnswer(2) }
        option3Btn.setOnClickListener { checkAnswer(3) }
        option4Btn.setOnClickListener { checkAnswer(4) }

        nextButton.setOnClickListener {
            loadLanguageQuestion()
        }
    }

    private fun showNotEnoughCountriesError(
        questionText: TextView,
        vararg buttons: Button
    ) {
        questionText.text = getString(R.string.notEnoughCountry)
        buttons.forEach { it.visibility = View.GONE }
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
        // Get target country's languages
        val targetLanguages = if (currentLanguage != "en") {
            targetCountry.translatedLanguages
        } else {
            targetCountry.languages
        }

        // If country has no languages, fall back to count question
        if (targetLanguages.isEmpty()) {
            return generateLanguageCountQuestion(targetCountry)
        }

        // Select one random language from target country as correct answer
        val correctLanguage = targetLanguages.random()

        // Get all languages from other countries, excluding ALL languages from target country
        val otherLanguages = allCountries
            .filter { it.name != targetCountry.name }
            .flatMap { if (currentLanguage != "en") it.translatedLanguages else it.languages }
            .filter { !targetLanguages.contains(it) }
            .distinct()
            .toMutableList()

        // If we don't have enough distinct languages from other countries, fall back to count question
        if (otherLanguages.size < 3) {
            return generateLanguageCountQuestion(targetCountry)
        }

        // Select 3 incorrect answers from other countries' languages
        val incorrectAnswers = otherLanguages
            .shuffled()
            .take(3)

        val answers = (listOf(correctLanguage) + incorrectAnswers).shuffled()

        return QuestionData(
            getString(
                R.string.languageQuestion,
                if (currentLanguage != "en") targetCountry.translatedName else targetCountry.name
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
            getString(
                R.string.languageCountQuestion,
                if (currentLanguage != "en") targetCountry.translatedName else targetCountry.name
            ),
            correctCount.toString(),
            answers
        )
    }

    private fun checkAnswer(selectedOption: Int) {
        val isCorrect = (selectedOption - 1) == correctAnswerIndex
        val feedbackText = findViewById<TextView>(R.id.feedbackText)
        feedbackText.text = if (isCorrect) {
            getString(R.string.correct)
        } else {
            getString(R.string.wrong)
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}
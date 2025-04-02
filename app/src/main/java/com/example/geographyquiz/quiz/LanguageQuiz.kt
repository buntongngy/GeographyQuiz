package com.example.geographyquiz.quiz

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.geographyquiz.R
import com.example.geographyquiz.data.Country
import com.example.geographyquiz.data.CountryDatabase
import java.util.*

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

        feedbackText.text = ""

        // Get countries with translations if needed
        val countries = if (currentLanguage != "en") {
            databaseHelper.getTranslatedRandomCountries(4, currentLanguage)
        } else {
            databaseHelper.getRandomCountries(4)
        }

        if (countries.size < 4) {
            questionText.text = if (currentLanguage == "fr")
                "Pas assez de pays dans la base de données"
            else
                "Not enough countries in database"
            option1Btn.visibility = View.GONE
            option2Btn.visibility = View.GONE
            option3Btn.visibility = View.GONE
            option4Btn.visibility = View.GONE
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

        correctAnswerIndex = questionData.answers.indexOf(questionData.correctAnswer) + 1

        option1Btn.setOnClickListener { checkAnswer(1) }
        option2Btn.setOnClickListener { checkAnswer(2) }
        option3Btn.setOnClickListener { checkAnswer(3) }
        option4Btn.setOnClickListener { checkAnswer(4) }

        findViewById<Button>(R.id.nextButton).setOnClickListener {
            loadLanguageQuestion()
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
        val correctLanguage = if (currentLanguage != "en") {
            targetCountry.translatedLanguages.random()
        } else {
            targetCountry.languages.random()
        }

        val otherLanguages = allCountries
            .flatMap { if (currentLanguage != "en") it.translatedLanguages else it.languages }
            .filter { !targetCountry.languages.contains(it) }
            .distinct()
            .shuffled()
            .take(3)

        val answers = (listOf(correctLanguage) + otherLanguages).shuffled()

        return QuestionData(
            if (currentLanguage != "en")
                "Quelle langue est parlée en ${targetCountry.translatedName} ?"
            else
                "Which language is spoken in ${targetCountry.name}?",
            correctLanguage,
            answers
        )
    }

    private fun generateLanguageCountQuestion(targetCountry: Country): QuestionData {
        val correctCount = targetCountry.languages.size
        val possibleCounts = listOf(
            correctCount,
            (correctCount - 1).coerceAtLeast(1),
            (correctCount + 1).coerceAtMost(10),
            (correctCount + 2).coerceAtMost(10)
        ).distinct().shuffled().take(4)

        return QuestionData(
            if (currentLanguage != "en")
                "Combien de langues sont parlées en ${targetCountry.translatedName} ?"
            else
                "How many languages are spoken in ${targetCountry.name}?",
            correctCount.toString(),
            possibleCounts.map { it.toString() }
        )
    }

    private fun checkAnswer(selectedOption: Int) {
        val isCorrect = selectedOption == correctAnswerIndex
        val feedbackText = findViewById<TextView>(R.id.feedbackText)
        feedbackText.text = if (isCorrect) {
            if (currentLanguage != "en") "Correct! 🎉" else "Correct! 🎉"
        } else {
            if (currentLanguage != "en") "Faux! 😢" else "Wrong! 😢"
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}
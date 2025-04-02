package com.example.geographyquiz.quiz

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.geographyquiz.R
import com.example.geographyquiz.data.CountryDatabase

class QuizActivity : AppCompatActivity() {

    private var correctAnswerIndex = 0
    private lateinit var databaseHelper: CountryDatabase
    private var currentLanguage = "en"

    enum class QuestionType {
        CAPITAL,
        CONTINENT,
        POPULATION,
        AREA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        // Get current language
        val sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
        currentLanguage = sharedPref.getString("app_language", "en") ?: "en"

        databaseHelper = CountryDatabase(this)
        loadDynamicQuestion()
    }

    private fun loadDynamicQuestion() {
        val questionText = findViewById<TextView>(R.id.questionText)
        val option1Btn = findViewById<Button>(R.id.option1Button)
        val option2Btn = findViewById<Button>(R.id.option2Button)
        val option3Btn = findViewById<Button>(R.id.option3Button)
        val option4Btn = findViewById<Button>(R.id.option4Button)
        val feedbackText = findViewById<TextView>(R.id.feedbackText)

        feedbackText.text = ""

        // Reset all buttons to visible by default
        option1Btn.visibility = View.VISIBLE
        option2Btn.visibility = View.VISIBLE
        option3Btn.visibility = View.VISIBLE
        option4Btn.visibility = View.VISIBLE

        // Get countries with translations if needed
        val countries = if (currentLanguage != "en") {
            databaseHelper.getTranslatedRandomCountries(10, currentLanguage) // Get more countries to ensure unique options
        } else {
            databaseHelper.getRandomCountries(10)
        }

        if (countries.size < 4) {
            questionText.text = if (currentLanguage != "en")
                "Pas assez de pays dans la base de données"
            else
                "Not enough countries in database"
            option1Btn.visibility = View.GONE
            option2Btn.visibility = View.GONE
            option3Btn.visibility = View.GONE
            option4Btn.visibility = View.GONE
            return
        }

        val questionType = QuestionType.values().random()
        val (question, correctAnswer, allAnswers, isYesNoQuestion) = when (questionType) {
            QuestionType.CAPITAL -> {
                val correctCountry = countries[0]
                // Get unique capitals from other countries
                val otherCapitals = countries.subList(1, countries.size)
                    .asSequence()
                    .map { if (currentLanguage != "en") it.translatedCapital else it.capital }
                    .distinct()
                    .filter { it != (if (currentLanguage != "en") correctCountry.translatedCapital else correctCountry.capital) }
                    .take(3)
                    .toList()

                Quad(
                    if (currentLanguage != "en")
                        "Quelle est la capitale de ${correctCountry.translatedName} ?"
                    else
                        "What is the capital of ${correctCountry.name}?",
                    if (currentLanguage != "en") correctCountry.translatedCapital else correctCountry.capital,
                    listOf(
                        if (currentLanguage != "en") correctCountry.translatedCapital else correctCountry.capital
                    ) + otherCapitals,
                    false
                )
            }
            QuestionType.CONTINENT -> {
                val correctCountry = countries[0]
                // Get unique continents from other countries
                val otherContinents = countries.subList(1, countries.size)
                    .asSequence()
                    .map { if (currentLanguage != "en") it.translatedContinent else it.continent }
                    .distinct()
                    .filter { it != (if (currentLanguage != "en") correctCountry.translatedContinent else correctCountry.continent) }
                    .take(3)
                    .toList()

                Quad(
                    if (currentLanguage != "en")
                        "Sur quel continent se trouve ${correctCountry.translatedName} ?"
                    else
                        "Which continent is ${correctCountry.name} in?",
                    if (currentLanguage != "en") correctCountry.translatedContinent else correctCountry.continent,
                    listOf(
                        if (currentLanguage != "en") correctCountry.translatedContinent else correctCountry.continent
                    ) + otherContinents,
                    false
                )
            }
            QuestionType.POPULATION -> {
                val correctCountry = countries[0]
                val comparisonCountry = countries[1]
                val isLarger = correctCountry.population > comparisonCountry.population
                Quad(
                    if (currentLanguage != "en")
                        "Est-ce que ${correctCountry.translatedName} a une population plus grande que ${comparisonCountry.translatedName} ?"
                    else
                        "Does ${correctCountry.name} have a larger population than ${comparisonCountry.name}?",
                    if (currentLanguage != "en") (if (isLarger) "Oui" else "Non") else (if (isLarger) "Yes" else "No"),
                    if (currentLanguage != "en") listOf("Oui", "Non") else listOf("Yes", "No"),
                    true
                )
            }
            QuestionType.AREA -> {
                val largestCountry = countries.maxByOrNull { it.area } ?: countries[0]
                // Get unique country names for options
                val otherCountries = countries
                    .asSequence()
                    .filter { it.name != largestCountry.name }
                    .map { if (currentLanguage != "en") it.translatedName else it.name }
                    .distinct()
                    .take(3)
                    .toList()

                Quad(
                    if (currentLanguage != "en")
                        "Lequel de ces pays a la plus grande superficie ?"
                    else
                        "Which of these countries has the largest area?",
                    if (currentLanguage != "en") largestCountry.translatedName else largestCountry.name,
                    listOf(
                        if (currentLanguage != "en") largestCountry.translatedName else largestCountry.name
                    ) + otherCountries,
                    false
                )
            }
        }

        val shuffledAnswers = allAnswers.shuffled()

        questionText.text = question

        if (isYesNoQuestion) {
            // Only show two buttons for Yes/No questions
            option1Btn.text = shuffledAnswers[0]
            option2Btn.text = shuffledAnswers[1]
            option3Btn.visibility = View.GONE
            option4Btn.visibility = View.GONE

            correctAnswerIndex = shuffledAnswers.indexOf(correctAnswer) + 1
        } else {
            // Show all four buttons for other question types
            option1Btn.text = shuffledAnswers.getOrElse(0) { "" }
            option2Btn.text = shuffledAnswers.getOrElse(1) { "" }
            option3Btn.text = shuffledAnswers.getOrElse(2) { "" }
            option4Btn.text = shuffledAnswers.getOrElse(3) { "" }

            correctAnswerIndex = shuffledAnswers.indexOf(correctAnswer) + 1
        }

        option1Btn.setOnClickListener { checkAnswer(1) }
        option2Btn.setOnClickListener { checkAnswer(2) }
        option3Btn.setOnClickListener { checkAnswer(3) }
        option4Btn.setOnClickListener { checkAnswer(4) }

        findViewById<Button>(R.id.nextButton).setOnClickListener {
            loadDynamicQuestion()
        }
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

    // Helper data class for returning four values from when expression
    private data class Quad<T1, T2, T3, T4>(
        val first: T1,
        val second: T2,
        val third: T3,
        val fourth: T4
    )
}
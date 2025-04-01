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

        // Get translated countries if language is not English
        val countries = if (currentLanguage != "en") {
            databaseHelper.getTranslatedRandomCountries(4, currentLanguage)
        } else {
            databaseHelper.getRandomCountries(4)
        }

        if (countries.size < 4) {
            questionText.text = if (currentLanguage == "fr") "Pas assez de pays dans la base de données" else "Not enough countries in database"
            return
        }

        val questionType = QuestionType.values().random()

        val (question, correctAnswer, allAnswers) = when (questionType) {
            QuestionType.CAPITAL -> {
                val correctCountry = countries[0]
                Triple(
                    if (currentLanguage == "fr")
                        "Quelle est la capitale de ${correctCountry.translatedName} ?"
                    else
                        "What is the capital of ${correctCountry.name}?",
                    if (currentLanguage == "fr") correctCountry.translatedCapital else correctCountry.capital,
                    listOf(
                        if (currentLanguage == "fr") correctCountry.translatedCapital else correctCountry.capital
                    ) + countries.subList(1, 4).map {
                        if (currentLanguage == "fr") it.translatedCapital else it.capital
                    }
                )
            }
            QuestionType.CONTINENT -> {
                val correctCountry = countries[0]
                Triple(
                    if (currentLanguage == "fr")
                        "Sur quel continent se trouve ${correctCountry.translatedName} ?"
                    else
                        "Which continent is ${correctCountry.name} in?",
                    if (currentLanguage == "fr") correctCountry.translatedContinent else correctCountry.continent,
                    listOf(
                        if (currentLanguage == "fr") correctCountry.translatedContinent else correctCountry.continent
                    ) + countries.subList(1, 4).map {
                        if (currentLanguage == "fr") it.translatedContinent else it.continent
                    }
                )
            }
            QuestionType.POPULATION -> {
                val correctCountry = countries[0]
                val comparisonCountry = countries[1]
                val isLarger = correctCountry.population > comparisonCountry.population
                Triple(
                    if (currentLanguage == "fr")
                        "Est-ce que ${correctCountry.translatedName} a une population plus grande que ${comparisonCountry.translatedName} ?"
                    else
                        "Does ${correctCountry.name} have a larger population than ${comparisonCountry.name}?",
                    if (currentLanguage == "fr") (if (isLarger) "Oui" else "Non") else (if (isLarger) "Yes" else "No"),
                    if (currentLanguage == "fr") listOf("Oui", "Non") else listOf("Yes", "No")
                )
            }
            QuestionType.AREA -> {
                val largestCountry = countries.maxByOrNull { it.area } ?: countries[0]
                Triple(
                    if (currentLanguage == "fr")
                        "Lequel de ces pays a la plus grande superficie ?"
                    else
                        "Which of these countries has the largest area?",
                    if (currentLanguage == "fr") largestCountry.translatedName else largestCountry.name,
                    countries.map { if (currentLanguage == "fr") it.translatedName else it.name }
                )
            }
        }

        val shuffledAnswers = allAnswers.shuffled()

        questionText.text = question
        option1Btn.text = shuffledAnswers.getOrElse(0) { "" }
        option2Btn.text = shuffledAnswers.getOrElse(1) { "" }

        if (shuffledAnswers.size > 2) {
            option3Btn.text = shuffledAnswers[2]
            option4Btn.text = shuffledAnswers.getOrElse(3) { "" }
            option3Btn.visibility = View.VISIBLE
            option4Btn.visibility = View.VISIBLE
        } else {
            option3Btn.visibility = View.GONE
            option4Btn.visibility = View.GONE
        }

        correctAnswerIndex = shuffledAnswers.indexOf(correctAnswer) + 1

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
            if (currentLanguage == "fr") "Correct! 🎉" else "Correct! 🎉"
        } else {
            if (currentLanguage == "fr") "Faux! 😢" else "Wrong! 😢"
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}
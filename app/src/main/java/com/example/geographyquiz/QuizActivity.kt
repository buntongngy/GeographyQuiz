// QuizActivity.kt
package com.example.geographyquiz

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.geographyquiz.data.CountryDatabase

class QuizActivity : AppCompatActivity() {

    private var correctAnswerIndex = 0
    private lateinit var databaseHelper: CountryDatabase

    // Question types enum
    enum class QuestionType {
        CAPITAL,
        CONTINENT,
        POPULATION,
        AREA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        // Initialize database helper
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

        // Clear feedback
        feedbackText.text = ""

        // Get 4 random countries
        val countries = databaseHelper.getRandomCountries(4)
        if (countries.size < 4) {
            questionText.text = "Not enough countries in database"
            return
        }

        // Randomly select a question type
        val questionType = QuestionType.values().random()

        // Generate question and answers based on type
        val (question, correctAnswer, allAnswers) = when (questionType) {
            QuestionType.CAPITAL -> {
                val correctCountry = countries[0]
                Triple(
                    "What is the capital of ${correctCountry.name}?",
                    correctCountry.capital,
                    listOf(correctCountry.capital) + countries.subList(1, 4).map { it.capital }
                )
            }
            QuestionType.CONTINENT -> {
                val correctCountry = countries[0]
                Triple(
                    "Which continent is ${correctCountry.name} in?",
                    correctCountry.continent,
                    listOf(correctCountry.continent) + countries.subList(1, 4).map { it.continent }
                )
            }
            QuestionType.POPULATION -> {
                val correctCountry = countries[0]
                val comparisonCountry = countries[1]
                val isLarger = correctCountry.population > comparisonCountry.population
                Triple(
                    "Does ${correctCountry.name} have a larger population than ${comparisonCountry.name}?",
                    if (isLarger) "Yes" else "No",
                    listOf("Yes", "No")
                )
            }
            QuestionType.AREA -> {
                // Find the country with largest area
                val largestCountry = countries.maxByOrNull { it.area } ?: countries[0]
                Triple(
                    "Which of these countries has the largest area?",
                    largestCountry.name,
                    countries.map { it.name }
                )
            }
        }

        // Shuffle answers
        val shuffledAnswers = allAnswers.shuffled()

        // Update UI
        questionText.text = question
        option1Btn.text = shuffledAnswers.getOrElse(0) { "" }
        option2Btn.text = shuffledAnswers.getOrElse(1) { "" }

        // Only show buttons 3 and 4 if we have enough answers
        if (shuffledAnswers.size > 2) {
            option3Btn.text = shuffledAnswers[2]
            option4Btn.text = shuffledAnswers.getOrElse(3) { "" }
            option3Btn.visibility = View.VISIBLE
            option4Btn.visibility = View.VISIBLE
        } else {
            option3Btn.visibility = View.GONE
            option4Btn.visibility = View.GONE
        }

        // Track correct answer position (1-4)
        correctAnswerIndex = shuffledAnswers.indexOf(correctAnswer) + 1

        // Set click listeners
        option1Btn.setOnClickListener { checkAnswer(1) }
        option2Btn.setOnClickListener { checkAnswer(2) }
        option3Btn.setOnClickListener { checkAnswer(3) }
        option4Btn.setOnClickListener { checkAnswer(4) }

        // Next button loads new question
        findViewById<Button>(R.id.nextButton).setOnClickListener {
            loadDynamicQuestion()
        }
    }

    private fun checkAnswer(selectedOption: Int) {
        val isCorrect = selectedOption == correctAnswerIndex
        val feedbackText = findViewById<TextView>(R.id.feedbackText)
        feedbackText.text = if (isCorrect) "Correct! 🎉" else "Wrong! 😢"
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}
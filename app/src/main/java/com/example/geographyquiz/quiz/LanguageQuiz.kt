// QuizActivity.kt
package com.example.geographyquiz.quiz

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.geographyquiz.R
import com.example.geographyquiz.data.CountryDatabase

class LanguageQuiz : AppCompatActivity() {

    private var correctAnswerIndex = 0
    private lateinit var databaseHelper: CountryDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

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
        val countries = databaseHelper.getRandomCountries(4)
        if (countries.size < 4) {
            questionText.text = "Not enough countries in database"
            return
        }

        val targetCountry = countries.random()
        val questionData = when ((0..1).random()) {
            0 -> generateLanguageQuestion(targetCountry, countries)
            else -> generateLanguageCountQuestion(targetCountry)
        }

        val fullAnswers = when {
            questionData.answers.size >= 4 -> questionData.answers
            else -> {
                val additionalOptions = when (questionData.question.startsWith("How many")) {
                    true -> listOf("5", "6", "7").filter { !questionData.answers.contains(it) }
                    false -> listOf("English", "French", "Spanish", "Arabic", "Portuguese")
                        .filter { lang ->
                            !questionData.answers.contains(lang) &&
                                    !targetCountry.languages.contains(lang)
                        }
                }
                (questionData.answers + additionalOptions).distinct().take(4)
            }
        }.shuffled()

        // Update UI with question data
        questionText.text = questionData.question
        option1Btn.text = fullAnswers[0]
        option2Btn.text = fullAnswers[1]
        option3Btn.text = fullAnswers[2]
        option4Btn.text = fullAnswers[3]

        option3Btn.visibility = View.VISIBLE
        option4Btn.visibility = View.VISIBLE

        correctAnswerIndex = fullAnswers.indexOf(questionData.correctAnswer) + 1

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
        targetCountry: CountryDatabase.Country,
        allCountries: List<CountryDatabase.Country>
    ): QuestionData {
        val correctLanguage = targetCountry.languages.random()
        val otherLanguages = allCountries
            .flatMap { it.languages }
            .filter { !targetCountry.languages.contains(it) }
            .distinct()
            .shuffled()
            .take(3)

        val answers = (listOf(correctLanguage) + otherLanguages).shuffled()
        return QuestionData(
            "Which language is spoken in ${targetCountry.name}?",
            correctLanguage,
            answers
        )
    }

    private fun generateLanguageCountQuestion(
        targetCountry: CountryDatabase.Country
    ): QuestionData {
        val correctCount = targetCountry.languages.size
        val possibleCounts = listOf(
            correctCount,
            (correctCount - 1).coerceAtLeast(1),
            (correctCount + 1).coerceAtMost(10),
            (correctCount + 2).coerceAtMost(10)
        ).distinct().shuffled().take(4)

        return QuestionData(
            "How many languages are spoken in ${targetCountry.name}?",
            correctCount.toString(),
            possibleCounts.map { it.toString() }
        )
    }

    private fun checkAnswer(selectedOption: Int) {
        val isCorrect = selectedOption == correctAnswerIndex
        findViewById<TextView>(R.id.feedbackText).text = if (isCorrect) "Correct! 🎉" else "Wrong! 😢"
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}
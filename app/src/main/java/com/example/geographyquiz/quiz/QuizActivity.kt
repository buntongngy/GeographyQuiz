package com.example.geographyquiz.quiz

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.geographyquiz.R
import com.example.geographyquiz.data.Country
import com.example.geographyquiz.data.CountryDatabase

class QuizActivity : AppCompatActivity() {

    private var correctAnswerIndex = 0
    private lateinit var databaseHelper: CountryDatabase
    private var currentLanguage = "en"

    enum class QuestionType {
        CAPITAL,
        BIGGEST_CITY,
        CITY_IN_COUNTRY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        val sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
        currentLanguage = sharedPref.getString("app_language", "en") ?: "en"
        databaseHelper = CountryDatabase(this)
        loadCityQuestion()
    }

    private fun loadCityQuestion() {
        val questionText = findViewById<TextView>(R.id.questionText)
        val option1Btn = findViewById<Button>(R.id.option1Button)
        val option2Btn = findViewById<Button>(R.id.option2Button)
        val option3Btn = findViewById<Button>(R.id.option3Button)
        val option4Btn = findViewById<Button>(R.id.option4Button)
        val feedbackText = findViewById<TextView>(R.id.feedbackText)

        feedbackText.text = ""

        val countries = if (currentLanguage != "en") {
            databaseHelper.getTranslatedRandomCountries(20, currentLanguage)
        } else {
            databaseHelper.getRandomCountries(20)
        }

        if (countries.size < 4) {
            showNotEnoughCountriesError(questionText, option1Btn, option2Btn, option3Btn, option4Btn)
            return
        }

        val targetCountry = countries.random()
        val similarCountries = getSimilarCountries(targetCountry, countries)
        val questionType = QuestionType.values().random()

        val (question, correctAnswer, allAnswers) = when (questionType) {
            QuestionType.CAPITAL -> generateCapitalQuestion(targetCountry, similarCountries, countries)
            QuestionType.BIGGEST_CITY -> generateBiggestCityQuestion(targetCountry, similarCountries, countries)
            QuestionType.CITY_IN_COUNTRY -> generateCityInCountryQuestion(targetCountry, similarCountries, countries)
        }

        questionText.text = question
        option1Btn.text = allAnswers[0]
        option2Btn.text = allAnswers[1]
        option3Btn.text = allAnswers[2]
        option4Btn.text = allAnswers[3]
        correctAnswerIndex = allAnswers.indexOf(correctAnswer) + 1

        option1Btn.setOnClickListener { checkAnswer(1) }
        option2Btn.setOnClickListener { checkAnswer(2) }
        option3Btn.setOnClickListener { checkAnswer(3) }
        option4Btn.setOnClickListener { checkAnswer(4) }

        findViewById<Button>(R.id.nextButton).setOnClickListener {
            loadCityQuestion()
        }
    }

    private fun getSimilarCountries(targetCountry: Country, allCountries: List<Country>): List<Country> {
        return allCountries.filter { otherCountry ->
            when {
                otherCountry.name == targetCountry.name -> false
                otherCountry.category == targetCountry.category -> true
                otherCountry.region == targetCountry.region -> true
                otherCountry.continent == targetCountry.continent -> true
                else -> false
            }
        }
    }

    private fun generateCapitalQuestion(
        targetCountry: Country,
        similarCountries: List<Country>,
        allCountries: List<Country>
    ): Triple<String, String, List<String>> {
        val correctAnswer = if (currentLanguage != "en") targetCountry.translatedCapital else targetCountry.capital

        // First try to get options from similar countries
        val similarOptions = similarCountries
            .map { if (currentLanguage != "en") it.translatedCapital else it.capital }
            .filter { it != correctAnswer && it.isNotBlank() }
            .distinct()
            .shuffled()
            .take(3)

        // If not enough similar options, supplement with random countries
        val otherOptions = if (similarOptions.size < 3) {
            similarOptions + allCountries
                .filter { it.name != targetCountry.name && !similarCountries.contains(it) }
                .map { if (currentLanguage != "en") it.translatedCapital else it.capital }
                .filter { it != correctAnswer && it.isNotBlank() }
                .distinct()
                .shuffled()
                .take(3 - similarOptions.size)
        } else {
            similarOptions
        }

        return Triple(
            getString(R.string.capital_question,
                if (currentLanguage != "en") targetCountry.translatedName else targetCountry.name),
            correctAnswer,
            (listOf(correctAnswer) + otherOptions).shuffled()
        )
    }

    private fun generateBiggestCityQuestion(
        targetCountry: Country,
        similarCountries: List<Country>,
        allCountries: List<Country>
    ): Triple<String, String, List<String>> {
        val correctAnswer = if (currentLanguage != "en") targetCountry.translatedBigCity else targetCountry.bigCity

        val similarOptions = similarCountries
            .map { if (currentLanguage != "en") it.translatedBigCity else it.bigCity }
            .filter { it != correctAnswer && it.isNotBlank() }
            .distinct()
            .shuffled()
            .take(3)

        val otherOptions = if (similarOptions.size < 3) {
            similarOptions + allCountries
                .filter { it.name != targetCountry.name && !similarCountries.contains(it) }
                .map { if (currentLanguage != "en") it.translatedBigCity else it.bigCity }
                .filter { it != correctAnswer && it.isNotBlank() }
                .distinct()
                .shuffled()
                .take(3 - similarOptions.size)
        } else {
            similarOptions
        }

        return Triple(
            getString(R.string.city_question,
                if (currentLanguage != "en") targetCountry.translatedName else targetCountry.name),
            correctAnswer,
            (listOf(correctAnswer) + otherOptions).shuffled()
        )
    }

    private fun generateCityInCountryQuestion(
        targetCountry: Country,
        similarCountries: List<Country>,
        allCountries: List<Country>
    ): Triple<String, String, List<String>> {
        val targetCities = listOfNotNull(
            if (currentLanguage != "en") targetCountry.translatedCapital else targetCountry.capital,
            if (currentLanguage != "en") targetCountry.translatedBigCity else targetCountry.bigCity,
            if (currentLanguage != "en") targetCountry.translatedSecondCity else targetCountry.secondCity,
            if (currentLanguage != "en") targetCountry.translatedThirdCity else targetCountry.thirdCity
        ).filter { it.isNotBlank() }.distinct()

        val correctCity = targetCities.random()

        // Get cities from similar countries first
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

        // If not enough, supplement with other countries
        val otherOptions = if (similarOptions.size < 3) {
            similarOptions + allCountries
                .filter { it.name != targetCountry.name && !similarCountries.contains(it) }
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
                .take(3 - similarOptions.size)
        } else {
            similarOptions
        }

        return Triple(
            getString(R.string.city_in_country_question,
                if (currentLanguage != "en") targetCountry.translatedName else targetCountry.name),
            correctCity,
            (listOf(correctCity) + otherOptions).shuffled()
        )
    }

    private fun showNotEnoughCountriesError(
        questionText: TextView,
        vararg buttons: Button
    ) {
        questionText.text = getString(R.string.notEnoughCountry)
        buttons.forEach { it.visibility = View.GONE }
    }

    private fun checkAnswer(selectedOption: Int) {
        val isCorrect = selectedOption == correctAnswerIndex
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
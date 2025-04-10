package com.example.geographyquiz.ui.home

import ExplorerItem
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

import android.content.Intent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geographyquiz.ExplorerAdapter
import com.example.geographyquiz.quiz.CityQuiz
import com.example.geographyquiz.R
import com.example.geographyquiz.quiz.FlagQuiz
import com.example.geographyquiz.quiz.LandmarkQuiz
import com.example.geographyquiz.quiz.LanguageQuiz

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.explorerRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        recyclerView.adapter = ExplorerAdapter(getExplorerItems()) { item ->
            when (item.title) {
                "Capital City" -> {
                    val intent = Intent(requireContext(), CityQuiz::class.java)
                    startActivity(intent)
                }
                "Country Language" -> {
                    val intent = Intent(requireContext(), LanguageQuiz::class.java)
                    startActivity(intent)
                }
                "Flag Quiz" -> {
                    val intent = Intent(requireContext(), FlagQuiz::class.java)
                    startActivity(intent)
                }
                "Landmark" -> {
                    val intent = Intent(requireContext(), LandmarkQuiz::class.java)
                    startActivity(intent)
                }
            }
        }

        return view
    }

    private fun getExplorerItems(): List<ExplorerItem> {
        return listOf(
            ExplorerItem("Flag Quiz", R.drawable.ic_flag),
            ExplorerItem("Capital City", R.drawable.ic_city),
            ExplorerItem("Country Currency", R.drawable.ic_exchange),
            ExplorerItem("Country Language", R.drawable.ic_language),
            ExplorerItem("Country Shape", R.drawable.ic_shape),
            ExplorerItem("Landmark",R.drawable.ic_landmark )

        )
    }
}
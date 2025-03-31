package com.example.geographyquiz.ui.home

import ExplorerItem
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.geographyquiz.databinding.FragmentHomeBinding

import android.content.Intent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geographyquiz.ExplorerAdapter
import com.example.geographyquiz.QuizActivity
import com.example.geographyquiz.R

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
                    val intent = Intent(requireContext(), QuizActivity::class.java)
                    startActivity(intent)
                }
                "Flag Quiz" -> {
                    // Handle history category click
                }
            }
        }

        return view
    }

    private fun getExplorerItems(): List<ExplorerItem> {
        return listOf(
            ExplorerItem("Flag Quiz", R.drawable.ic_flag),
            ExplorerItem("Capital City", R.drawable.ic_city),
            ExplorerItem("Country Shape", R.drawable.ic_shape),
            ExplorerItem("Landmark",R.drawable.ic_landmark )

        )
    }
}
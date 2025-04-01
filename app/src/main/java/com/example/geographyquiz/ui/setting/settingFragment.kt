package com.example.geographyquiz.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.geographyquiz.databinding.FragmentSettingBinding


class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set current language (you would get this from your preferences)
        binding.currentLanguage.text = "English" // Default or get from shared prefs

        // Set up language selection
        binding.languageOption.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Spanish", "French", "German", "Japanese")
        val currentLanguage = binding.currentLanguage.text.toString()
        val checkedItem = languages.indexOf(currentLanguage).coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                // Handle language selection
                val selectedLanguage = languages[which]
                binding.currentLanguage.text = selectedLanguage

                // Here you would save the language preference and restart activity
                // or update the app locale
                // Example:
                // saveLanguagePreference(selectedLanguage)
                // updateAppLocale(selectedLanguage)

                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
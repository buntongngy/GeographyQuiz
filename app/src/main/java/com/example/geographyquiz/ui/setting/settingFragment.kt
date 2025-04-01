// SettingFragment.kt
package com.example.geographyquiz.ui.setting

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.geographyquiz.LocaleHelper
import com.example.geographyquiz.MainActivity
import com.example.geographyquiz.R
import com.example.geographyquiz.databinding.FragmentSettingBinding
import java.util.Locale

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

        // Load current language from preferences
        val sharedPref = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val currentLanguage = sharedPref.getString("app_language", "en") ?: "en"
        binding.currentLanguage.text = getLanguageDisplayName(currentLanguage)

        // Set up language selection
        binding.languageOption.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun showLanguageDialog() {
        val languages = mapOf(
            "English" to "en",
            "Français" to "fr",
            "Español" to "es",
            "Deutsch" to "de",
            "日本語" to "ja"
        )

        val sharedPref = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val currentLanguageCode = sharedPref.getString("app_language", "en") ?: "en"
        val currentLanguageName = languages.entries.find { it.value == currentLanguageCode }?.key ?: "English"
        val checkedItem = languages.keys.indexOf(currentLanguageName).coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setTitle("Select Language")
            .setSingleChoiceItems(
                languages.keys.toTypedArray(),
                checkedItem
            ) { dialog, which ->
                val selectedLanguageCode = languages.values.elementAt(which)
                setAppLanguage(selectedLanguageCode)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setAppLanguage(languageCode: String) {
        // Save preference
        val sharedPref = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("app_language", languageCode)
            apply()
        }

        // Update UI immediately
        binding.currentLanguage.text = getLanguageDisplayName(languageCode)

        // Change app locale
        updateLocale(languageCode)

        // Restart app to apply changes
        restartApp()
    }

    private fun updateLocale(languageCode: String) {
        // Update application context
        val appContext = requireContext().applicationContext
        LocaleHelper.setLocale(appContext, languageCode)

        // Update activity context
        activity?.let {
            val updatedContext = LocaleHelper.setLocale(it, languageCode)
            it.resources.updateConfiguration(
                updatedContext.resources.configuration,
                updatedContext.resources.displayMetrics
            )
        }
    }

    private fun restartApp() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        activity?.finish()
    }

    private fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            "en" -> "English"
            "fr" -> "Français"
            "es" -> "Español"
            "de" -> "Deutsch"
            "ja" -> "日本語"
            else -> "English"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
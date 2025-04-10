package com.example.geographyquiz.utils

import android.content.Context
import android.content.res.Configuration
import java.util.*

object TranslationUtils {

    fun getTranslatedString(context: Context, resId: Int, languageCode: String): String {
        val config = Configuration(context.resources.configuration)
        val locale = Locale(languageCode)
        config.setLocale(locale)
        val localizedContext = context.createConfigurationContext(config)
        return localizedContext.resources.getString(resId)
    }

    fun getTranslatedStringWithFormat(
        context: Context,
        resId: Int,
        languageCode: String,
        vararg args: Any
    ): String {
        val baseString = getTranslatedString(context, resId, languageCode)
        return baseString.format(*args)
    }
}
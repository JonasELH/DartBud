package com.group1.dartbud.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.group1.dartbud.ui.theme.GameColors
import com.group1.dartbud.ui.theme.gameColorsById
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holder på hvilket fargetema spillskjermen skal bruke, og husker valget mellom
 * omstarter.
 *
 * Lagres i SharedPreferences og ikke i Room/Firestore med vilje: dette er en
 * utseende-innstilling for denne enheten, ikke spilldata som hører hjemme i
 * historikken eller skal følge brukeren mellom telefoner.
 */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _gameColors = MutableStateFlow(gameColorsById(prefs.getString(KEY_THEME_ID, null)))
    val gameColors: StateFlow<GameColors> = _gameColors.asStateFlow()

    fun selectTheme(colors: GameColors) {
        _gameColors.value = colors
        prefs.edit().putString(KEY_THEME_ID, colors.id).apply()
    }

    private companion object {
        const val PREFS_NAME = "dartbud_settings"
        const val KEY_THEME_ID = "game_theme_id"
    }
}

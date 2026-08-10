package com.k.sekiro.musico.playmusic.presenation.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetStateKeys {
    val HasState = booleanPreferencesKey("has_state")
    val Title = stringPreferencesKey("title")
    val Artist = stringPreferencesKey("artist")
    val IsPlaying = booleanPreferencesKey("is_playing")
    val IsFavorite = booleanPreferencesKey("is_favorite")
    val CoverPath = stringPreferencesKey("cover_path")
    val CoverVersion = intPreferencesKey("cover_version")
}

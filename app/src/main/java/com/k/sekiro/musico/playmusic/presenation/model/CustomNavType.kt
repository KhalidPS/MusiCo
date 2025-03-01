package com.k.sekiro.musico.playmusic.presenation.model

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CustomNavType {
    val DisplayableDurationType = object : NavType<DisplayableDuration>(isNullableAllowed = false) {
        override fun get(
            bundle: Bundle,
            key: String
        ): DisplayableDuration? {
            return Json.decodeFromString(bundle.getString(key)?: return null)
        }

        override fun parseValue(value: String): DisplayableDuration {
            return Json.decodeFromString(Uri.decode(value))
        }

        override fun serializeAsValue(value: DisplayableDuration): String {
            return Uri.encode(Json.encodeToString(value))
        }

        override fun put(
            bundle: Bundle,
            key: String,
            value: DisplayableDuration
        ) {
            bundle.putString(key, Json.encodeToString(value))
        }

    }
}
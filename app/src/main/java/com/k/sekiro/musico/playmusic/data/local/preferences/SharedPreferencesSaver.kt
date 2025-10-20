package com.k.sekiro.musico.playmusic.data.local.preferences

import android.content.SharedPreferences
import androidx.core.content.edit
import com.k.sekiro.musico.playmusic.domain.SimpleDataSaver

class SharedPreferencesSaver(private val sharedPreferences: SharedPreferences) : SimpleDataSaver {

    @Throws(UnsupportedOperationException::class)
    override suspend fun <T> suspendSave(key: String, value: T) {
        throw UnsupportedOperationException("SharedPreferencesSaver does not support suspendSave use normal save instead or use PreferencesDataStore with suspendSave")
    }

    @Throws(UnsupportedOperationException::class)
    override suspend fun <T> suspendGet(key: String, default: T): T {
        throw UnsupportedOperationException("SharedPreferencesSaver does not support suspendGet use normal get instead or PreferencesDataStore with suspendGet")

    }

    @Throws(UnsupportedOperationException::class)
    override suspend fun <T : Any> suspendSave(vararg values: Pair<String, T>) {
        throw UnsupportedOperationException("this operation supported only for PreferencesDataStore")
    }

    @Throws(IllegalArgumentException::class)
    override fun <T> save(key: String, value: T) {
        sharedPreferences.edit {
            when(value){
                is Int -> { putInt(key,value) }

                is String -> { putString(key,value) }

                is Float -> { putFloat(key,value) }

                is Boolean -> { putBoolean(key,value) }

                is Long -> { putLong(key,value) }

                else -> throw  IllegalArgumentException("Unsupported type of value")
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun <T> get(key: String, default: T): T {
        return when(default){
            is Int -> { sharedPreferences.getInt(key,default) }

            is String -> { sharedPreferences.getString(key,default) }

            is Float -> { sharedPreferences.getFloat(key,default) }

            is Boolean -> { sharedPreferences.getBoolean(key,default) }

            is Long -> { sharedPreferences.getLong(key,default) }

            else -> throw  IllegalArgumentException("Unsupported type of value")
        } as T
    }

    @Throws(IllegalArgumentException::class)
    override fun <T : Any> save(vararg values: Pair<String, T>) {
        sharedPreferences.edit {
            for (value in values){
                when(value.second){
                    is Int -> { putInt(value.first,value.second as Int) }

                    is String -> { putString(value.first,value.second as String) }

                    is Float -> { putFloat(value.first,value.second as Float) }

                    is Boolean -> { putBoolean(value.first,value.second as Boolean) }

                    is Long -> { putLong(value.first,value.second as Long) }

                    else -> throw  IllegalArgumentException("Unsupported type of value")
                }
            }

        }
    }


}
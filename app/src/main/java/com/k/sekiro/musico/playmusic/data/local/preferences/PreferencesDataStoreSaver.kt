package com.k.sekiro.musico.playmusic.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.k.sekiro.musico.playmusic.domain.SimpleDataSaver
import kotlinx.coroutines.flow.first
import kotlin.jvm.Throws

class PreferencesDataStoreSaver(private val dataStore: DataStore<Preferences>): SimpleDataSaver {

    @Throws(IllegalArgumentException::class)
    override suspend fun <T> suspendSave(key: String, value: T) {
        when(value){
            is String -> {
                dataStore.edit {
                    it[stringPreferencesKey(key)] = value
                }
            }
            is Int -> {
                dataStore.edit {
                    it[intPreferencesKey(key)] = value
                }
            }
            is Boolean -> {
                dataStore.edit {
                    it[booleanPreferencesKey(key)] = value
                }
            }
            is Float -> {
                dataStore.edit {
                    it[floatPreferencesKey(key)] = value
                }
            }

            is Long -> {
                dataStore.edit {
                    it[longPreferencesKey(key)] = value
                }
            }

            is Double -> {
                dataStore.edit {
                    it[doublePreferencesKey(key)] = value
                }
            }
            else -> {
                throw IllegalArgumentException("Unsupported type of value")
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    override suspend fun <T> suspendGet(key: String, default: T): T {
        return when(default){
            is String -> {
                dataStore.data.first()[stringPreferencesKey(key)] ?: default
            }

            is Int -> {
                dataStore.data.first()[intPreferencesKey(key)] ?: default
            }

            is Boolean -> {
                dataStore.data.first()[booleanPreferencesKey(key)] ?: default
            }

            is Float -> {
                dataStore.data.first()[floatPreferencesKey(key)] ?: default
            }

            is Long -> {
                dataStore.data.first()[longPreferencesKey(key)] ?: default
            }

            is Double -> {
                dataStore.data.first()[doublePreferencesKey(key)] ?: default
            }

            else -> {
                throw IllegalArgumentException("Unsupported type of value")
            }
        } as T

    }

    @Throws(IllegalArgumentException::class)
    override suspend fun <T : Any> suspendSave(vararg values: Pair<String, T>) {
        dataStore.edit {
            for (value in values){
                when(value.second){
                    is String -> { it[stringPreferencesKey(value.first)] = value.second as String }

                    is Int -> { it[intPreferencesKey(value.first)] = value.second as Int }

                    is Long -> { it[longPreferencesKey(value.first)] = value.second as Long }

                    is Float -> { it[floatPreferencesKey(value.first)] = value.second as Float }

                    is Boolean -> { it[booleanPreferencesKey(value.first)] = value.second as Boolean}

                    is Double -> { it[doublePreferencesKey(value.first)] = value.second as Double }

                    else -> throw IllegalArgumentException("Unsupported type of value")
                }
            }
        }
    }

    @Throws(UnsupportedOperationException::class)
    override fun <T> save(key: String, value: T) {

        throw UnsupportedOperationException("PreferencesDataStore does not support normal save use suspendSave instead or SharedPreferences with save")
    }

    @Throws(UnsupportedOperationException::class)
    override fun <T> get(key: String, default: T): T {
        throw UnsupportedOperationException("PreferencesDataStore does not support normal get use suspendGet instead or SharedPreferences with get")
    }

    @Throws(UnsupportedOperationException::class)
    override fun <T : Any> save(vararg values: Pair<String, T>) {
        throw UnsupportedOperationException("this operation supported only for SharedPreferences")

    }


}
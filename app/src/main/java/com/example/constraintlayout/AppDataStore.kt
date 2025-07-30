package com.example.constraintlayout

import kotlinx.coroutines.flow.first
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppDataStore(private val context: Context) {
    companion object {
        val VALOR_CONTA = floatPreferencesKey("valor_conta")
        val NUMERO_PESSOAS = intPreferencesKey("numero_pessoas")
    }

    suspend fun saveContaValue(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[VALOR_CONTA] = value
        }
    }

    suspend fun savePessoasValue(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[NUMERO_PESSOAS] = value
        }
    }

    val contaValue: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[VALOR_CONTA] ?: 0f }

    val pessoasValue: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[NUMERO_PESSOAS] ?: 0 }

    suspend fun getContaValueOnce(): Float {
        return contaValue.first()
    }

    suspend fun getPessoasValueOnce(): Int {
        return pessoasValue.first()
    }
}
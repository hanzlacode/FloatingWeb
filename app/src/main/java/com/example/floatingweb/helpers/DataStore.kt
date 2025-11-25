package com.example.floatingweb.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
//import com.example.floatingweb.models.PriceAlert

@SuppressLint("StaticFieldLeak")
object DataStorage {

    private const val PREFS = "floating_web_storage"
    private const val KEY_ALERTS = "price_alerts"

    private lateinit var context: Context
    private val gson = Gson()

    // LiveData that observers (like your Service) can react to
    private val _alertsLiveData = MutableLiveData<List<PriceAlert>>(emptyList())
    val alertsLiveData: LiveData<List<PriceAlert>> get() = _alertsLiveData

    fun init(appContext: Context) {
        context = appContext.applicationContext
        // Load alerts from SharedPreferences when app starts
        val loaded = loadAlertsInternal()
        _alertsLiveData.value = loaded
    }

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** -------------------- ALERT STORAGE -------------------- */
    private fun loadAlertsInternal(): MutableList<PriceAlert> {
        val json = prefs.getString(KEY_ALERTS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<PriceAlert>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    private fun saveAlertsInternal(list: List<PriceAlert>) {
        prefs.edit(commit = true) { putString(KEY_ALERTS, gson.toJson(list)) }
    }

    fun addAlert(alert: PriceAlert) {
        val current = _alertsLiveData.value?.toMutableList() ?: mutableListOf()
        current.add(alert)
        _alertsLiveData.postValue(current)
        saveAlertsInternal(current)
        Log.d("PriceMonitor", "$alert")

    }

    fun updateAlert(updated: PriceAlert) {
        val current = _alertsLiveData.value?.toMutableList() ?: mutableListOf()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            current[index] = updated
            _alertsLiveData.postValue(current)
            saveAlertsInternal(current)
        }
        Log.d("PriceMonitor", "$updated $index")
    }

    fun removeAlert(alertId: String) {
        val current = _alertsLiveData.value?.toMutableList() ?: mutableListOf()
        val newList = current.filterNot { it.id == alertId }
        _alertsLiveData.postValue(newList)
        saveAlertsInternal(newList)
        Log.d("PriceMonitor", "$alertId")
    }

    fun clearAllAlerts() {
        _alertsLiveData.postValue(emptyList())
        saveAlertsInternal(emptyList())
    }

    /** -------------------- GENERIC STRING STORAGE -------------------- */
    fun saveString(key: String, value: String) {
        prefs.edit(commit = true) { putString(key, value) }
    }

    fun loadString(key: String): String? = prefs.getString(key, null)

    fun clearKey(key: String) {
        prefs.edit(commit = true) { remove(key) }
    }

    /** -------------------- OVERLAY POSITIONS (kept same) -------------------- */
    fun savePosition(index: Int, x: Int, y: Int) {
        prefs.edit(commit = true) {
            putInt("overlay_${index}_x", x)
            putInt("overlay_${index}_y", y)
        }
    }

    fun loadPosition(index: Int): Pair<Int, Int> {
        val x = prefs.getInt("overlay_${index}_x", 0)
        val y = prefs.getInt("overlay_${index}_y", 0)
        return x to y
    }
}

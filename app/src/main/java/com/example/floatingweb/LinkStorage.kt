package com.example.floatingweb

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit

class LinkStorageManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("floating_web_links", Context.MODE_PRIVATE)

    // Save links safely
    fun saveLinks(links: List<String>) {
        val encodedLinks = links.map { Uri.encode(it) }  // encode commas, slashes, etc.
        prefs.edit { putString("links", encodedLinks.joinToString(",")) }
    }

    // Load links safely
    fun loadLinks(): List<String> {
        val stored = prefs.getString("links", null) ?: return emptyList()
        return stored.split(",").map { Uri.decode(it) }.filter { it.isNotEmpty() }
    }


    // Save links safely
    fun saveTimeFrames(TimeFrames: List<String>) {
        val encodedLinks = TimeFrames.map { Uri.encode(it) }  // encode commas, slashes, etc.
        prefs.edit { putString("TimeFrames", encodedLinks.joinToString(",")) }
    }

    // Load links safely
    fun loadTimeFrames(): List<String> {
        val stored = prefs.getString("TimeFrames", null) ?: return emptyList()
        return stored.split(",").map { Uri.decode(it) }.filter { it.isNotEmpty() }
    }

    fun clearLinks() {
        prefs.edit { remove("links") }
    }
    fun saveSelectedWebMode(Mode:Int){
        prefs.edit { putInt("mode",Mode) }
        Log.d("ui/ux","mode from Web Storage ${Mode}")
    }
    fun loadSelectedWebMode():Int{
        val mode = prefs.getInt("mode",0)
        Log.d("ui/ux","mode loaded db storage ${mode}")
        return mode
    }
}

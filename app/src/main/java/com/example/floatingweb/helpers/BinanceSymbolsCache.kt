package com.example.floatingweb.helpers

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object BinanceSymbolsCache {

    private const val PREFS_NAME = "binance_symbols_cache"
    private const val CACHE_DURATION = 4 * 60 * 60 * 1000L // 4 hours

    // SharedPreferences keys per AlertFor
    private fun getKeyPrefix(alertFor: AlertFor): String {
        return when (alertFor) {
            AlertFor.SPOT -> "spot_"
            AlertFor.USDM -> "usdm_"
            AlertFor.COINM -> "coinm_"
        }
    }

    private fun prefs(context: Context, alertFor: AlertFor) =
        context.getSharedPreferences("${PREFS_NAME}_${alertFor.name}", Context.MODE_PRIVATE)

    // Per AlertFor cache
    private val cache = mutableMapOf<AlertFor, Set<String>>()
    private val lastUpdated = mutableMapOf<AlertFor, Long>()

    /**
     * Fetches symbols from the appropriate Binance API based on AlertFor.
     * Uses caching (in-memory + SharedPreferences) for 4 hours.
     */
    suspend fun getSymbols(context: Context, alertFor: AlertFor): Set<String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. Check in-memory cache
        if (cache.containsKey(alertFor) && cache[alertFor]!!.isNotEmpty() && now - lastUpdated[alertFor]!! < CACHE_DURATION) {
            Log.d("BinanceSymbolsCache", "Using in-memory cache for $alertFor")
            return@withContext cache[alertFor]!!
        }

        // 2. Load from SharedPreferences
        val prefs = prefs(context, alertFor)
        val storedSymbols = prefs.getStringSet(getKeyPrefix(alertFor) + "symbols", emptySet()) ?: emptySet()
        val storedTime = prefs.getLong(getKeyPrefix(alertFor) + "last_update", 0L)

        if (storedSymbols.isNotEmpty() && now - storedTime < CACHE_DURATION) {
            cache[alertFor] = storedSymbols
            lastUpdated[alertFor] = storedTime
            Log.d("BinanceSymbolsCache", "Using SharedPreferences cache for $alertFor")
            return@withContext storedSymbols
        }

        // 3. Fetch from network based on AlertFor
        val newSymbols = fetchSymbolsFromApi(alertFor)

        // 4. Save to SharedPreferences and update in-memory cache
        prefs.edit()
            .putStringSet(getKeyPrefix(alertFor) + "symbols", newSymbols)
            .putLong(getKeyPrefix(alertFor) + "last_update", now)
            .apply()

        cache[alertFor] = newSymbols
        lastUpdated[alertFor] = now

        Log.d("BinanceSymbolsCache", "Fetched fresh symbols from network for $alertFor")
        return@withContext newSymbols
    }

    /**
     * Fetches symbols from the correct Binance API endpoint for the given AlertFor.
     */
    private suspend fun fetchSymbolsFromApi(alertFor: AlertFor): Set<String> = withContext(Dispatchers.IO) {
        val url = when (alertFor) {
            AlertFor.SPOT -> "https://api.binance.com/api/v3/exchangeInfo"
            AlertFor.USDM -> "https://fapi.binance.com/fapi/v1/exchangeInfo"
            AlertFor.COINM -> "https://dapi.binance.com/dapi/v1/exchangeInfo"
        }

        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (body == null) {
                Log.e("BinanceSymbolsCache", "Empty response body for $alertFor")
                return@withContext emptySet()
            }

            val json = JSONObject(body)
            val symbolsArray = json.getJSONArray("symbols")
            val newSymbols = mutableSetOf<String>()

            for (i in 0 until symbolsArray.length()) {
                val symbolObj = symbolsArray.getJSONObject(i)
                val symbol = symbolObj.getString("symbol").uppercase()
                newSymbols.add(symbol)
            }

            Log.d("BinanceSymbolsCache", "Fetched ${newSymbols.size} symbols for $alertFor")
            return@withContext newSymbols

        } catch (e: Exception) {
            Log.e("BinanceSymbolsCache", "Failed to fetch symbols for $alertFor", e)
            return@withContext emptySet()
        }
    }

    /**
     * Checks if the given symbol is valid for the specified AlertFor market.
     */
    suspend fun isValidSymbol(context: Context, symbol: String, alertFor: AlertFor): Boolean {
        val symbols = getSymbols(context, alertFor)
        val normalizedSymbol = symbol.uppercase()

        val isValid = symbols.contains(normalizedSymbol)
        Log.d("BinanceSymbolsCache", "Symbol '$symbol' valid for ${alertFor}: $isValid (Total symbols: ${symbols.size})")

        return isValid
    }
}
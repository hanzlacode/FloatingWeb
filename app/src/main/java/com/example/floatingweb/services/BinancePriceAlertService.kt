package com.example.floatingweb.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import com.example.floatingweb.helpers.DataStorage
import com.example.floatingweb.R
import com.example.floatingweb.helpers.AlertStatus
import com.example.floatingweb.helpers.AlertType
import com.example.floatingweb.helpers.PriceAlert
import com.example.floatingweb.helpers.AlertFor
import com.example.floatingweb.screens.isServiceRunning
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.min

class PriceAlertService : Service() {

    companion object {
        private const val TAG = "PriceAlertService"
        private const val CHANNEL_ALERTS = "price_alert_channel"
        private const val CHANNEL_FOREGROUND = "price_alert_foreground_channel"
        private const val NOTIF_ID_FOREGROUND = 1001
        private const val ACTION_STOP_SOUND = "com.example.action.STOP_SOUND"
        private const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    // single shared OkHttpClient for the service (thread-safe, lazy)
    private val client: OkHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .pingInterval(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .dispatcher(okhttp3.Dispatcher().apply {
                // tune limits (optional)
                maxRequests = 64
                maxRequestsPerHost = 16
            })
            .build()
    }


    // one websocket per market
    private val sockets = mutableMapOf<AlertFor, WebSocket?>()

    // reconnect attempt counters per market
//    private val reconnectAttempts = mutableMapOf<AlertFor, Int>()

    // in-memory map symbol -> latest price (Double)
    private val latestPrices = mutableMapOf<String, Double>()

    // we will keep the live alerts list here and observe repository
    private var currentAlerts: MutableList<PriceAlert> = mutableListOf()

    // media player for alert sound
    private var mediaPlayer: MediaPlayer? = null

    private val maxBackoffMs = TimeUnit.SECONDS.toMillis(60)

    // observer token
    private val alertsObserver = Observer<List<PriceAlert>> { list ->
        synchronized(currentAlerts) {
            currentAlerts = list.toMutableList()
        }

        val markets = list.map { it.alertFor }.distinct()

        markets.forEach { market ->
            rebuildSubscriptionForMarket(market)
        }
    }


    private val priceExecutor = Executors.newSingleThreadExecutor()

    private val reconnectAttempts = mutableMapOf<AlertFor, Int>().apply {
        AlertFor.values().forEach { put(it, 0) }
    }

    private val reconnectRunnables = mutableMapOf<AlertFor, Runnable?>()
    private val debounceRunnable: Runnable? = null
    private var rebuildDebounceTask: Runnable? = null
    private val rebuildDebounceMs = 500L // adjust as needed

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        DataStorage.init(applicationContext)
        createNotificationChannels()
        startForegroundServiceNotification()

        try {
            DataStorage.alertsLiveData.observeForever(alertsObserver)
        } catch (t: Throwable) {
            Log.w(TAG, "observeForever failed: ${t.message}. Using fallback load.")
            val loaded = DataStorage.alertsLiveData.value ?: emptyList()
            currentAlerts = loaded.toMutableList()
        }

//        client = OkHttpClient.Builder()
//            .pingInterval(15, TimeUnit.SECONDS)
//            .readTimeout(0, TimeUnit.MILLISECONDS)
//            .build()
        // init reconnectAttempts
        AlertFor.values().forEach { reconnectAttempts[it] = 0 }

        // initial subscriptions
//        rebuildAllSubscriptions()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_STOP_SOUND -> {
                    if (intent.getBooleanExtra("FromUi", false)) {
                        stopAlertSound()
                    }else {
                    val alertId = intent.getStringExtra("alertId") ?: return@let
                    stopAlertSound()
                    NotificationManagerCompat.from(this).cancel(alertId.hashCode())
                    }
                }
                ACTION_STOP_SERVICE -> {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            DataStorage.alertsLiveData.removeObserver(alertsObserver)
        } catch (t: Throwable) {
            Log.w(TAG, "removeObserver failed: ${t.message}")
        }
        closeAllSockets()
        // DO NOT shutdown OkHttp dispatcher here
        stopAlertSound()
    }


    // ----------------------- Notifications & Sound -----------------------

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelAlerts = NotificationChannel(
                CHANNEL_ALERTS,
                "Price Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for price alerts"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setSound(null, null)
            }

            val channelForeground = NotificationChannel(
                CHANNEL_FOREGROUND,
                "Price Alert Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground notification for the running PriceAlertService"
            }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channelAlerts)
            nm.createNotificationChannel(channelForeground)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun startForegroundServiceNotification() {
        val stopServiceIntent = Intent(this, PriceAlertService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPending = PendingIntent.getService(
            this,
            0,
            stopServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_FOREGROUND)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Price Alerts Running")
            .setContentText("Monitoring price alerts")
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPending)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID_FOREGROUND, notif)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun showPriceAlertNotification(alert: PriceAlert, currentPrice: Double) {
        val stopSoundIntent = Intent(this, PriceAlertService::class.java).apply {
            action = ACTION_STOP_SOUND
            putExtra("alertId", alert.id)
            putExtra("FromUi",false)
        }
        val stopPending = PendingIntent.getService(
            this,
            alert.id.hashCode(),
            stopSoundIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = "${alert.type} ${alert.threshold}, Current: $currentPrice"

        val builder = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Price Alert: ${alert.symbol} (${alert.alertFor})")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(R.drawable.ic_launcher_foreground, "Stop Sound", stopPending)
            .setOngoing(true)

        toast("Alert Triggered: ${alert.symbol} ${alert.triggeredPrice}")


        try {
            NotificationManagerCompat.from(this).notify(alert.id.hashCode(), builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}")
        }
    }

    private fun playAlertSound() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            val prefs = getSharedPreferences("price_alert", Context.MODE_PRIVATE)
            val uriString = prefs.getString("custom_sound_uri", null)
            val alarmUri: Uri = uriString?.toUri() ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@PriceAlertService, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alert sound: ${e.message}")
        }
    }

    private fun stopAlertSound() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sound: ${e.message}")
        }
    }

    // ----------------------- WebSocket / Price Processing -----------------------

    private fun symbolsForMarket(market: AlertFor): List<String> {
        return synchronized(currentAlerts) {
            currentAlerts.filter { it.status == AlertStatus.ACTIVE && it.alertFor == market }
                .map { it.symbol.uppercase() }
                .distinct()
        }
    }

    private fun rebuildSubscriptionForMarket(market: AlertFor) {
        val symbols = symbolsForMarket(market)
        if (symbols.isEmpty()) {
            sockets[market]?.let {
                Log.d(TAG, "Closing socket for $market (no symbols)")
                it.close(1000, "no symbols")
                sockets[market] = null
            }
            return
        }

        val streamPath = symbols.joinToString("/") { "${it.lowercase()}@aggTrade" }
        val url = when (market) {
            AlertFor.SPOT -> "wss://stream.binance.com:9443/ws/$streamPath"
            AlertFor.USDM -> "wss://fstream.binance.com/ws/$streamPath"
            AlertFor.COINM -> "wss://dstream.binance.com/ws/$streamPath"
        }

        // close only if different or stale socket
        sockets[market]?.close(1000, "rebuild subscription")
        sockets[market] = null
        connectWebSocketForMarket(market, url)
    }

    private fun connectWebSocketForMarket(market: AlertFor, url: String) {
        try {
            val request = Request.Builder().url(url).build()

            val listener = object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket opened for $market: $url")
                    reconnectAttempts[market] = 0
                    reconnectRunnables[market]?.let { mainHandler.removeCallbacks(it) }
                    reconnectRunnables[market] = null
                    sockets[market] = ws
                }

                @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
                override fun onMessage(ws: WebSocket, text: String) {
                    priceExecutor.execute {
                        safelyHandleMessage(text, market)
                    }
                }

                @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure for $market: ${t.message}")
                    sockets[market] = null
                    scheduleReconnect(market)
                    notifyError("Price Alert service ($market): connection lost. Reconnecting...")
                }

                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                    Log.i(TAG, "WebSocket closing ($market): $code $reason")
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.i(TAG, "WebSocket closed ($market): $code $reason")
                    sockets[market] = null
                }
            }

            val ws = client.newWebSocket(request, listener)
            // set socket early to indicate 'connecting' status
            sockets[market] = ws
        } catch (e: Exception) {
            Log.e(TAG, "connectWebSocketForMarket error ($market): ${e.message}")
            sockets[market] = null
            scheduleReconnect(market)
        }
    }

    private fun scheduleReconnect(market: AlertFor) {
        // cancel any previous scheduled reconnect for this market
        reconnectRunnables[market]?.let { mainHandler.removeCallbacks(it) }

        val attempt = (reconnectAttempts[market] ?: 0) + 1
        reconnectAttempts[market] = attempt

        // exponential backoff (1s,2s,4s,8s...) capped
        val backoff = min(maxBackoffMs, 1000L * (1 shl (attempt.coerceAtMost(6))))

        val task = Runnable {
            // Try to rebuild *only* this market's subscription
            rebuildSubscriptionForMarket(market)
        }
        reconnectRunnables[market] = task
        Log.i(TAG, "Scheduling reconnect for $market in $backoff ms (attempt $attempt)")
        mainHandler.postDelayed(task, backoff)
    }


    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun safelyHandleMessage(text: String, market: AlertFor) {
        try {
            val json = JSONObject(text)
            // For trade stream, Binance uses fields: "s" (symbol) and "p" (price)
            // Some combined endpoints may wrap payloads, attempt both
            val symbol = when {
                json.has("s") -> json.optString("s")
                json.has("data") && JSONObject(json.optString("data")).has("s") -> JSONObject(json.optString("data")).optString("s")
                json.has("stream") -> json.optString("stream")
                else -> ""
            }
            val priceStr = when {
                json.has("p") -> json.optString("p", "")
                json.has("data") && JSONObject(json.optString("data")).has("p") -> JSONObject(json.optString("data")).optString("p", "")
                else -> ""
            }
            if (symbol.isNullOrEmpty() || priceStr.isNullOrEmpty()) return

            val price = try {
                val bd = BigDecimal(priceStr).setScale(8, RoundingMode.DOWN)
                if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                    Log.w(TAG, "Ignoring invalid price <= 0 for $symbol: $priceStr")
                    return
                }
                bd.toDouble()
            } catch (e: Exception) {
                Log.w(TAG, "Invalid price format for $symbol: $priceStr - $e")
                return
            }
            Log.d(TAG, "[$market] WebSocket $symbol $price")

            latestPrices[symbol.uppercase()] = price
            processPriceForSymbol(symbol.uppercase(), price)
        } catch (e: Exception) {
            Log.e(TAG, "Message parse error: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun processPriceForSymbol(symbol: String, price: Double) {
        val alertsToTrigger = mutableListOf<PriceAlert>()

        synchronized(currentAlerts) {
            for (a in currentAlerts) {
                if (a.status != AlertStatus.ACTIVE) continue
                if (!a.symbol.equals(symbol, ignoreCase = true)) continue

                try {
                    when (a.type) {
                        AlertType.ABOVE -> if (price >= a.threshold) alertsToTrigger.add(a)
                        AlertType.BELOW -> if (price <= a.threshold) alertsToTrigger.add(a)
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Error evaluating alert ${a.id}: ${t.message}")
                }
            }
        }

        if (alertsToTrigger.isEmpty()) return

        // Trigger all matched alerts (independently)
        for (alert in alertsToTrigger) {
            triggerAlert(alert, price)
        }
        if (this@PriceAlertService.isServiceRunning(FloatingBrowserService::class.java)) this@PriceAlertService.startService(Intent(this@PriceAlertService, FloatingBrowserService::class.java).apply { action = "com.example.action.SHOW_ALL_OVERLAY" })
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun triggerAlert(alert: PriceAlert, currentPrice: Double) {
        // Avoid duplicate triggers (double-check status)
        synchronized(currentAlerts) {
            val found = currentAlerts.firstOrNull { it.id == alert.id } ?: return
            if (found.status != AlertStatus.ACTIVE) return
            found.status = AlertStatus.TRIGGERED
            found.triggeredAt = System.currentTimeMillis()
            found.triggeredPrice = currentPrice
            DataStorage.updateAlert(found)
        }

        // show notification & play sound
        try {
            showPriceAlertNotification(alert, currentPrice)
            playAlertSound()
        } catch (e: Exception) {
            Log.e(TAG, "Error while triggering alert: ${e.message}")
        }
    }

    private fun closeAllSockets() {
        for (market in AlertFor.values()) {
            try {
                sockets[market]?.close(1000, "service destroyed")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to close socket for $market: ${t.message}")
            } finally {
                sockets[market] = null
                reconnectRunnables[market]?.let { mainHandler.removeCallbacks(it) }
                reconnectRunnables[market] = null
                reconnectAttempts[market] = 0
            }
        }
    }


    private fun toast(toast: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun notifyError(message: String) {
        val notif = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Price Alert Service")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(TAG.hashCode(), notif)
        } catch (e: Exception) {
            Log.e(TAG, "notifyError failed: ${e.message}")
        }
    }
}








































//package com.example.floatingweb.services
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.*
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import android.media.MediaPlayer
//import android.media.RingtoneManager
//import android.net.Uri
//import android.os.Build
//import android.os.Handler
//import android.os.IBinder
//import android.os.Looper
//import android.util.Log
//import android.widget.Toast
//import androidx.annotation.RequiresPermission
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import androidx.core.net.toUri
//import com.example.floatingweb.helpers.DataStorage
//import com.example.floatingweb.R
//import com.example.floatingweb.helpers.AlertStatus
//import com.example.floatingweb.helpers.AlertType
//import com.example.floatingweb.helpers.PriceAlert
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.Response
//import okhttp3.WebSocket
//import okhttp3.WebSocketListener
//import org.json.JSONObject
//import java.math.BigDecimal
//import java.math.RoundingMode
//import java.util.concurrent.TimeUnit
//import kotlin.math.min
//
//class PriceAlertService : Service() {
//
//    companion object {
//        private const val TAG = "PriceAlertService"
//        private const val CHANNEL_ALERTS = "price_alert_channel"
//        private const val CHANNEL_FOREGROUND = "price_alert_foreground_channel"
//        private const val NOTIF_ID_FOREGROUND = 1001
//        private const val ACTION_STOP_SOUND = "com.example.action.STOP_SOUND"
//        private const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
//    }
//
//    private val mainHandler = Handler(Looper.getMainLooper())
//    private var webSocket: WebSocket? = null
//    private var client: OkHttpClient? = null
//
//    // in-memory map symbol -> latest price (Double)
//    private val latestPrices = mutableMapOf<String, Double>()
//
//    // we will keep the live alerts list here and observe repository
//    private var currentAlerts: MutableList<PriceAlert> = mutableListOf()
//
//    // media player for alert sound
//    private var mediaPlayer: MediaPlayer? = null
//
//    // reconnect/backoff
//    private var reconnectAttempts = 0
//    private val maxBackoffMs = TimeUnit.SECONDS.toMillis(60)
//
//    // observer token
//    private val alertsObserver = androidx.lifecycle.Observer<List<PriceAlert>> { list ->
//        synchronized(currentAlerts) {
//            currentAlerts = list.toMutableList()
//        }
//        rebuildWebSocketSubscription()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    override fun onCreate() {
//        super.onCreate()
//        DataStorage.init(applicationContext) // no-op if already initialized
//        createNotificationChannels()
//        startForegroundServiceNotification()
//        // observe LiveData from CryptoRepository (assumes you created it)
//        try {
//            DataStorage.alertsLiveData.observeForever(alertsObserver)
//        } catch (t: Throwable) {
//            // If CryptoRepository.alerts isn't available, fallback to load from DataStorage once.
//            Log.w(TAG, "CryptoRepository.alerts observe failed: ${t.message}. Loading from DataStorage fallback.")
//            val loaded = DataStorage.alertsLiveData.value ?: emptyList()
//            currentAlerts = loaded.toMutableList()
//            rebuildWebSocketSubscription()
//        }
//
//        // init http client
//        client = OkHttpClient.Builder()
//            .pingInterval(15, TimeUnit.SECONDS)
//            .readTimeout(0, TimeUnit.MILLISECONDS)
//            .build()
//
//        // start websocket connection
//        rebuildWebSocketSubscription()
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        intent?.action?.let { action ->
//            when (action) {
//                ACTION_STOP_SOUND -> {
//                    val alertId = intent.getStringExtra("alertId") ?: return@let
//
//                    stopAlertSound()
//                    NotificationManagerCompat.from(this).cancel(alertId.hashCode())
//
//                }
//                ACTION_STOP_SERVICE -> {
//                    stopSelf()
//                }
//            }
//        }
//        return START_STICKY
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        try {
//            DataStorage.alertsLiveData.removeObserver(alertsObserver)
//        } catch (t: Throwable) {
//            Log.w(TAG, "removeObserver failed: ${t.message}")
//        }
//        webSocket?.close(1000, "service destroyed")
//        client?.dispatcher?.executorService?.shutdown()
//        stopAlertSound()
//    }
//
//    // ----------------------- Notifications & Sound -----------------------
//
//    private fun createNotificationChannels() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channelAlerts = NotificationChannel(
//                CHANNEL_ALERTS,
//                "Price Alerts",
//                NotificationManager.IMPORTANCE_HIGH
//            ).apply {
//                description = "Notifications for price alerts"
//                enableLights(true)
//                lightColor = Color.RED
//                enableVibration(true)
//                // Do not set a sound here so we control playback manually via MediaPlayer
//                setSound(null, null)
//            }
//
//            val channelForeground = NotificationChannel(
//                CHANNEL_FOREGROUND,
//                "Price Alert Service",
//                NotificationManager.IMPORTANCE_LOW
//            ).apply {
//                description = "Foreground notification for the running PriceAlertService"
//            }
//
//            val nm = getSystemService(NotificationManager::class.java)
//            nm.createNotificationChannel(channelAlerts)
//            nm.createNotificationChannel(channelForeground)
//        }
//    }
//
//    @SuppressLint("UnspecifiedImmutableFlag")
//    private fun startForegroundServiceNotification() {
//        val stopServiceIntent = Intent(this, PriceAlertService::class.java).apply {
//            action = ACTION_STOP_SERVICE
//        }
//        val stopPending = PendingIntent.getService(
//            this,
//            0,
//            stopServiceIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val notif = NotificationCompat.Builder(this, CHANNEL_FOREGROUND)
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentTitle("Price Alerts Running")
//            .setContentText("Monitoring price alerts")
//            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPending)
//            .setOngoing(true)
//            .build()
//
//        startForeground(NOTIF_ID_FOREGROUND, notif)
//    }
//
//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    @SuppressLint("UnspecifiedImmutableFlag")
//    private fun showPriceAlertNotification(alert: PriceAlert, currentPrice: Double) {
//        val stopSoundIntent = Intent(this, PriceAlertService::class.java).apply {
//            action = ACTION_STOP_SOUND
//            putExtra("alertId", alert.id)
//        }
//        val stopPending = PendingIntent.getService(
//            this,
//            alert.id.hashCode(),
//            stopSoundIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val contentText = "${alert.type} ${alert.threshold}, Current: $currentPrice"
//
//        val builder = NotificationCompat.Builder(this, CHANNEL_ALERTS)
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentTitle("Price Alert: ${alert.symbol}:WebSocket")
//            .setContentText(contentText)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setCategory(NotificationCompat.CATEGORY_ALARM)
//            .addAction(R.drawable.ic_launcher_foreground, "Stop Sound", stopPending)
//            .setOngoing(true)
////            .setAutoCancel(true)
//
//        toast("Alert Triggered: ${alert.symbol} ${alert.triggeredPrice}")
//
//        try {
//            NotificationManagerCompat.from(this).notify(alert.id.hashCode(), builder.build())
//        } catch (e: Exception) {
//            Log.e(TAG, "Error showing notification: ${e.message}")
//        }
//    }
//
//    private fun playAlertSound() {
//        try {
//            // release previous
//            mediaPlayer?.stop()
//            mediaPlayer?.release()
//            mediaPlayer = null
//
//            val prefs = getSharedPreferences("price_alert", Context.MODE_PRIVATE)
//            val uriString = prefs.getString("custom_sound_uri", null)
//            val alarmUri: Uri = uriString?.toUri() ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
//
//            mediaPlayer = MediaPlayer().apply {
//                setDataSource(this@PriceAlertService, alarmUri)
//                isLooping = true
//                prepare()
//                start()
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error playing alert sound: ${e.message}")
//        }
//    }
//
//    private fun stopAlertSound() {
//        try {
//            mediaPlayer?.stop()
//            mediaPlayer?.release()
//            mediaPlayer = null
//        } catch (e: Exception) {
//            Log.e(TAG, "Error stopping sound: ${e.message}")
//        }
//    }
//
//    // ----------------------- WebSocket / Price Processing -----------------------
//
//    private fun rebuildWebSocketSubscription() {
//        // Build unique symbol list from currentAlerts
//        Log.d(TAG, "WebSocket rebuild...")
//
//        val symbols = synchronized(currentAlerts) {
//            currentAlerts.filter { it.status == AlertStatus.ACTIVE }.map { it.symbol.uppercase() }.distinct()
//        }
//
//        if (symbols.isEmpty()) {
//            // No active alerts -> close socket if open
//            Log.d(TAG, "WebSocket opened symbols is empty")
//
//            toast("Alert Empty!")
//            webSocket?.close(1000, "no symbols")
//            webSocket = null
//            return
//        }
//
//        val streamPath = symbols.joinToString("/") { "${it.lowercase()}@trade" }
//        val url = "wss://stream.binance.com:9443/ws/$streamPath"
//
//        // Close existing
//        webSocket?.close(1000, "rebuild subscription")
//        webSocket = null
//
//        connectWebSocket(url)
//    }
//
//    private fun connectWebSocket(url: String) {
//        try {
//            val request = Request.Builder().url(url).build()
//            client ?: run {
//                client = OkHttpClient.Builder()
//                    .pingInterval(15, TimeUnit.SECONDS)
//                    .readTimeout(0, TimeUnit.MILLISECONDS)
//                    .build()
//            }
//
//            webSocket = client!!.newWebSocket(request, object : WebSocketListener() {
//                override fun onOpen(ws: WebSocket, response: Response) {
//                    Log.d(TAG, "WebSocket opened: $url")
//                    reconnectAttempts = 0
//                }
//
//                @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//                override fun onMessage(ws: WebSocket, text: String) {
//                    safelyHandleMessage(text)
//                }
//
//                @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
//                    Log.e(TAG, "WebSocket failure: ${t.message}")
//                    toast("WebSocket failure: ${t.message}")
//
//                    scheduleReconnect()
//                    notifyError("Price Alert service: connection lost. Reconnecting...")
//                }
//
//                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
//                    Log.i(TAG, "WebSocket closing: $code $reason")
//                    toast("WebSocket closing: $code $reason")
//                }
//
//                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
//                    Log.i(TAG, "WebSocket closed: $code $reason")
//                    toast("WebSocket closed: $code $reason")
//                }
//            })
//        } catch (e: Exception) {
//            Log.e(TAG, "connectWebSocket error: ${e.message}")
//            toast("connectWebSocket error: ${e.message}")
//            scheduleReconnect()
//        }
//    }
//
//    private fun scheduleReconnect() {
//        reconnectAttempts++
//        val backoff = min(maxBackoffMs, (1000L * (1 shl (reconnectAttempts.coerceAtMost(6))))) // exponential
//        mainHandler.postDelayed({ rebuildWebSocketSubscription() }, backoff)
//    }
//
//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    private fun safelyHandleMessage(text: String) {
//        try {
//            val json = JSONObject(text)
//            // For trade stream, Binance uses fields: "s" (symbol) and "p" (price)
//            val symbol = json.optString("s", json.optString("stream")) // fallback if needed
//            val priceStr = json.optString("p", "")
//            if (symbol.isNullOrEmpty() || priceStr.isNullOrEmpty()) return
//
//            val price = try {
//                BigDecimal(priceStr).setScale(8, RoundingMode.DOWN).toDouble()
//            } catch (e: Exception) {
//                return
//            }
//            Log.d(TAG, "WebSocket $symbol $price")
//
//            latestPrices[symbol.uppercase()] = price
//            processPriceForSymbol(symbol.uppercase(), price)
//        } catch (e: Exception) {
//            Log.e(TAG, "Message parse error: ${e.message}")
//        }
//    }
//
//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    private fun processPriceForSymbol(symbol: String, price: Double) {
//        val alertsToTrigger = mutableListOf<PriceAlert>()
//
//        synchronized(currentAlerts) {
//            for (a in currentAlerts) {
//                if (a.status != AlertStatus.ACTIVE) continue
//                if (!a.symbol.equals(symbol, ignoreCase = true)) continue
//
//                try {
//                    when (a.type) {
//                        AlertType.ABOVE -> if (price >= a.threshold) alertsToTrigger.add(a)
//                        AlertType.BELOW -> if (price <= a.threshold) alertsToTrigger.add(a)
//                    }
//                } catch (t: Throwable) {
//                    Log.w(TAG, "Error evaluating alert ${a.id}: ${t.message}")
//                }
//            }
//        }
//
//        if (alertsToTrigger.isEmpty()) return
//
//        // Trigger all matched alerts (independently)
//        for (alert in alertsToTrigger) {
//            triggerAlert(alert, price)
//        }
//    }
//
//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    private fun triggerAlert(alert: PriceAlert, currentPrice: Double) {
//        // Avoid duplicate triggers (double-check status)
//        synchronized(currentAlerts) {
//            val found = currentAlerts.firstOrNull { it.id == alert.id } ?: return
//            if (found.status != AlertStatus.ACTIVE) return
//            found.status = AlertStatus.TRIGGERED
//            found.triggeredAt = System.currentTimeMillis()
//            found.triggeredPrice = currentPrice
//            DataStorage.updateAlert(found)
//        }
//
//        // show notification & play sound
//        try {
//            showPriceAlertNotification(alert, currentPrice)
//            playAlertSound()
//        } catch (e: Exception) {
//            Log.e(TAG, "Error while triggering alert: ${e.message}")
//        }
//
//        // persist alerts back to storage if DataStorage available
////        try {
////            DataStorage.saveAlerts(this, currentAlerts)
////        } catch (e: Exception) {
////            Log.w(TAG, "Failed to save alerts to DataStorage: ${e.message}")
////        }
//    }
//    private fun toast(toast: String){
//        Handler(Looper.getMainLooper()).post {
//            Toast.makeText(this,toast , Toast.LENGTH_SHORT).show()
//        }
//    }
//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    private fun notifyError(message: String) {
//        val notif = NotificationCompat.Builder(this, CHANNEL_ALERTS)
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentTitle("Price Alert Service")
//            .setContentText(message)
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .build()
//        try {
//            NotificationManagerCompat.from(this).notify(TAG.hashCode(), notif)
//        } catch (e: Exception) {
//            Log.e(TAG, "notifyError failed: ${e.message}")
//        }
//    }
//}

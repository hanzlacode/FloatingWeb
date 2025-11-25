//package com.example.floatingweb.services
//
//import android.annotation.SuppressLint
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.Service
//import android.content.*
//import android.os.Build
//import android.os.IBinder
//import android.util.Log
//import android.widget.Toast
//import androidx.annotation.RequiresApi
//import androidx.core.app.NotificationCompat
////import com.example.floatingweb.helpers.DataStorage
//import com.example.floatingweb.helpers.PriceAlert
//import com.example.floatingweb.services.process.*
//import com.google.gson.Gson
//
//class FloatingControllerService : Service() {
//
//    // ---- Register up to 9 isolated processes ----
//    private val userLocationForProcesses = mapOf(
//        "webview0" to FloatingWebProcessA::class.java,
//        "webview1" to FloatingWebProcessB::class.java,
//        "webview2" to FloatingWebProcessC::class.java,
//        "webview3" to FloatingWebProcessD::class.java,
//        "webview4" to FloatingWebProcessE::class.java,
//        "webview5" to FloatingWebProcessF::class.java,
//        "webview6" to FloatingWebProcessG::class.java,
//        "webview7" to FloatingWebProcessH::class.java,
//        "webview8" to FloatingWebProcessI::class.java
//    )
//
//    // --- Receiver for commands from sub-processes ---
//    private val commandReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            if (context == null || intent == null) return
//            if (intent.action != "com.example.floatingweb.MAIN_COMMAND") return
//
//            val cmd = intent.getStringExtra("cmd") ?: return
//            val payload = intent.getStringExtra("payload")
//            Log.d("ControllerService", "Received command: $cmd payload=$payload")
//
//            when (cmd) {
//                "trigger_alert" -> handleTriggerAlert(context, payload)
//                "get_alerts" -> sendAlertsBack(context)
//                "save_alert" -> handleSaveAlert(context, payload)
//                "log_message" -> Log.d("ControllerService", "Sub-process log: $payload")
//                else -> Log.w("ControllerService", "Unknown command: $cmd")
//            }
//        }
//    }
//
//    @SuppressLint("UnspecifiedRegisterReceiverFlag")
//    override fun onCreate() {
//        super.onCreate()
////        DataStorage.init(applicationContext)
//        startForegroundService()
//
//        val filter = IntentFilter("com.example.floatingweb.MAIN_COMMAND")
//
//        // ✅ FIX for Android 13+ (API 33)
//        registerReceiver(commandReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
//        Log.d("ControllerService", "Controller created")
//    }
//
//    private fun startForegroundService() {
//        val channelId = "floating_web_controller"
//        val channel = NotificationChannel(
//            channelId,
//            "Floating Web Controller",
//            NotificationManager.IMPORTANCE_LOW
//        )
//        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
//
//        val notification = NotificationCompat.Builder(this, channelId)
//            .setContentTitle("Floating Web Active")
//            .setContentText("Managing web overlays")
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .setOngoing(true)
//            .build()
//
//        startForeground(1, notification)
//    }
//    override fun onDestroy() {
//        super.onDestroy()
//        try {
//            unregisterReceiver(commandReceiver)
//        } catch (e: Exception) {
//            Log.w("ControllerService", "Receiver already unregistered or missing: ${e.message}")
//        }
//        Log.d("ControllerService", "Controller destroyed")
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val action = intent?.getStringExtra("action") ?: "open"
//
//        when (action) {
//            "start" -> {
//                val links = intent?.getStringArrayListExtra("links") ?: arrayListOf()
//                Log.d("ControllerService", "Received ${links.size} links from UI: ${links.joinToString()}")
//
//                if (links.isEmpty()) {
//                    Log.w("ControllerService", "No links provided, nothing to start")
//                    return START_NOT_STICKY
//                }
//
//                if (links.size > userLocationForProcesses.size) {
//                    Toast.makeText(
//                        applicationContext,
//                        "Too many links! Only ${userLocationForProcesses.size} allowed.",
//                        Toast.LENGTH_LONG
//                    ).show()
//                    Log.w("ControllerService", "Received ${links.size} links, allowed max ${userLocationForProcesses.size}")
//                }
//
//                val limitedLinks = links.take(userLocationForProcesses.size)
//                startAllUsersWithLinks(limitedLinks)
//            }
//
//            "stop" -> {
//                Log.d("ControllerService", "Stopping all WebView processes")
//                val reply = Intent("com.example.floatingweb.DestroyALL")
//                sendBroadcast(reply)
//                stopSelf()
//            }
//
//            "open" -> startAllUsers()
//            "close" -> stopSelf()
//        }
//
//        return START_STICKY
//    }
//
//    // ---- Start Each WebView Process with 1 Link ----
//    private fun startAllUsersWithLinks(links: List<String>) {
//        userLocationForProcesses.entries.forEachIndexed { index, entry ->
//            val link = links.getOrNull(index)
//            if (link != null) {
//                startWebViewProcessWithLinks(entry.key, entry.value, link)
//            } else {
//                Log.d("ControllerService", "No link assigned for ${entry.key}, skipping")
//            }
//        }
//    }
//
//    private fun startWebViewProcessWithLinks(user: String, serviceClass: Class<*>, link: String) {
//        try {
//            val intent = Intent(this, serviceClass).apply {
//                putExtra("user", user)
//                putExtra("link", link)
//            }
//            startService(intent)
//            Log.d("ControllerService", "Started $serviceClass for $user with $link")
//        } catch (e: Exception) {
//            Log.e("ControllerService", "Error starting $user process: ${e.message}", e)
//        }
//    }
//
//    private fun startWebViewProcess(user: String) {
//        val serviceClass = userLocationForProcesses[user] ?: return
//        val intent = Intent(this, serviceClass).apply { putExtra("user", user) }
//        startService(intent)
//        Log.d("ControllerService", "Started WebView process for $user")
//    }
//
//    private fun startAllUsers() {
//        userLocationForProcesses.keys.forEach { user -> startWebViewProcess(user) }
//    }
//
//    private fun sendAlertsBack(context: Context) {
////        val alerts = DataStorage.loadAlerts(context)
////        val payload = Gson().toJson(alerts)
//        val reply = Intent("com.example.floatingweb.ALERTS_RESULT").apply {
////            putExtra("alerts", payload)
//        }
//        context.sendBroadcast(reply)
//    }
//
//    private fun handleTriggerAlert(context: Context, payload: String?) {
//        if (payload.isNullOrBlank()) return
//        try {
//            val trigAlert = Gson().fromJson(payload, PriceAlert::class.java)
////            val allAlerts = DataStorage.loadAlerts(context)
////            val updated = allAlerts.map { if (it.id == trigAlert.id) trigAlert else it }
////            DataStorage.saveAlerts(context, updated)
//            Log.d("ControllerService", "Marked alert as TRIGGERED: $trigAlert")
//        } catch (e: Exception) {
//            Log.e("ControllerService", "Error marking alert triggered: ${e.message}", e)
//        }
//    }
//
//    private fun handleSaveAlert(context: Context, payload: String?) {
//        if (payload.isNullOrBlank()) return
//        try {
//            val alert = Gson().fromJson(payload, PriceAlert::class.java)
////            val allAlerts = DataStorage.loadAlerts(context)
////            DataStorage.saveAlerts(context, allAlerts + alert)
//            Log.d("ControllerService", "Saved alert from WebView process: $alert")
//        } catch (e: Exception) {
//            Log.e("ControllerService", "Error saving alert: ${e.message}", e)
//        }
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//}

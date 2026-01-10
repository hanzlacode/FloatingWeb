//
//        holder.overlay.post { holder.webView.invalidate() }

//        js

//        val serviceRef = WeakReference(this)
//
//        var activeAlert = mutableSetOf<PriceAlert>()
//
//        activeAlertsState.observe { map ->
//            map[holder.coinSymbol]?.let { alerts ->
//                if (alerts != activeAlert) {
//                    activeAlert = alerts
//                    Log.d("Overlay", "Updated alerts for $id")
//                }
//            }
//        }
//
//        fun syncActiveAlerts(coinSymbol: String) {
//            val map = activeAlertsState.value
//            map[coinSymbol] = activeAlert
//            activeAlertsState.value = map
//        }
//
//
//        ensureWakeLock()
//        val lastTriggerTimes = mutableMapOf<String, Long>()
//        var lastPriceChange: Long = System.currentTimeMillis()
//        val FREEZE_THRESHOLD_MS = 120_000L
//
//        // 🔧 FIX: Use Handler with proper cleanup
//        val handler = Handler(Looper.getMainLooper())
//        handlersById[id] = handler // Track for cleanup
//
//        val priceFreezeRunnable = object : Runnable {
//            override fun run() {
//                if (isDestroying) return
//

//                val now = System.currentTimeMillis()
//                if ((now - lastPriceChange) > FREEZE_THRESHOLD_MS) {
//                    Log.w("PriceMonitor", "⚠️ Price frozen, reloading WebView $id")
////                    CrashLogger.log(this@FloatingBrowserService, "FloatingService", "⚠️ Price frozen, reloading WebView $id")
//
//                    safeReloadWebView(id)
//                    lastPriceChange = now
//                }
//                if (!isDestroying) {
//                    handler.postDelayed(this, 40_000L)
//                }
//            }
//        }
//
//        fun startPriceFreezeWatchdog() {
//            handler.removeCallbacks(priceFreezeRunnable)
//            if (!isDestroying) {
//                handler.postDelayed(priceFreezeRunnable, 15_000L)
//            }
//        }
//
//        fun stopPriceFreezeWatchdog() {
//            handler.removeCallbacks(priceFreezeRunnable)
//        }
//
//
//        // 🔧 FIX: Use WeakReference for context to prevent leaks
//
//        fun loadalerts(){
//            serviceScope.launch(Dispatchers.IO) {
//                val loaded = DataStorage.alertsLiveData.value ?: emptyList()
//                val alerts = loaded
//                withContext(Dispatchers.Main) {
//                    activeAlert = findAlertsForCoin(holder.coinSymbol ?: return@withContext, alerts, AlertStatus.ACTIVE).toMutableSet()
//                    syncActiveAlerts(holder.coinSymbol.toString())
//                }
//            }
//        }
//        loadalerts()


//        webView.addJavascriptInterface(object {
//            @SuppressLint("MissingPermission")
//            @JavascriptInterface
//            fun checkPrice(jsonString: String) {
//                val service = serviceRef.get() ?: return
//                if (service.isDestroying) return
//
//                try {
//                    val obj = JSONObject(jsonString)
//                    val symbol = obj.getString("symbol")
//                    val price = obj.getDouble("price")
//
//                    lastPriceChange = System.currentTimeMillis()
//                    if (activeAlert.isEmpty() ?: return) return
//
//                    latestPrices[id] = price
//                    latestSymbols[id] = symbol
//                    Log.d("PriceMonitor", "Symbol=$symbol | Price=$price from $id")
////                    CrashLogger.log(this@FloatingBrowserService, "FloatingService", "🟢 Running check price index=$id")
//
//                    val now = System.currentTimeMillis()
//                    val triggeredNow = mutableSetOf<PriceAlert>()
//
//                    activeAlert.removeAll { alert ->
//                        if (alert.status != AlertStatus.ACTIVE) return@removeAll true
//
//                        val lastTime = lastTriggerTimes[alert.id] ?: 0L
//                        if (now - lastTime < 5000) return@removeAll false
//
//                        val triggered = when (alert.type) {
//                            AlertType.ABOVE -> price > alert.threshold
//                            AlertType.BELOW -> price < alert.threshold
//                        }
//
//                        if (triggered) {
//                            triggeredNow += alert
//                            lastTriggerTimes[alert.id] = now
//                        }
//                        false
//                    }
//
//                    if (triggeredNow.isNotEmpty()) {
//                        triggeredNow.forEach { alert ->
//                            try {
//                                Log.d("PriceMonitor", "🎯 Triggered: ${alert.symbol} ${alert.type} ${alert.threshold} @ $price from $id")
////                                CrashLogger.log(this@FloatingBrowserService, "FloatingService", "🎯 Triggered: ${alert.symbol} ${alert.type} ${alert.threshold} @ $price")
//                                triggeredAndSaveAlert(alert, price, service)
//                                service.playAlertSound()
//                                service.showPriceAlertNotification(alert, price)
//
//                                var openPageId: String? = null
//
//                                AlertSavedOnPage.forEach { (key, value) ->
//                                    if (value.contains(alert.id)) {
//                                        openPageId = key
//                                        return@forEach
//                                    }
//                                }
//
//                                Handler(Looper.getMainLooper()).post {
//                                    if (!service.isDestroying) {
//                                        holder.overlay.visibility = View.VISIBLE
//                                        if (currentStates[id] != STATE_MAX && openPageId != null) {
//                                            switchToState(STATE_MAX, openPageId)
//                                        }
//                                    }
//                                }
//
//                            } catch (e: Exception) {
//                                Log.e("PriceMonitor", "Error: ${e.message}", e)
////                                CrashLogger.log(this@FloatingBrowserService, "FloatingService", "Error: ${e.message}")
//
//                            }
//                        }
//
//                        activeAlert.removeAll(triggeredNow)
//                        syncActiveAlerts(symbol)
//                        Log.d("PriceMonitor", "🧹 Removed ${triggeredNow.size} triggered alerts. from $id")
////                        CrashLogger.log(this@FloatingBrowserService, "FloatingService", "🧹 Removed ${triggeredNow.size} triggered alerts.")
//
//                    }
//
//                    lastTriggerTimes.entries.removeIf { (_, t) -> now - t > 30_000 }
//
//                    if (activeAlert.isEmpty() == true && service::wakeLock.isInitialized && service.wakeLock.isHeld) {
////                        service.releaseWakeLock()
//                        stopPriceFreezeWatchdog()
//                        Log.d("FloatingService", "WakeLock released (no active alerts).")
////                        CrashLogger.log(this@FloatingBrowserService, "FloatingService", "(no active alerts)..")
//
//                    }
//
//                } catch (e: Exception) {
//                    Log.e("PriceMonitor", "Invalid JSON: $jsonString", e)
//                }
//            }
//
//            @JavascriptInterface
//            fun onSymbolChange(newSymbol: String) {
//                val service = serviceRef.get() ?: return
//                if (service.isDestroying) return
//
//                val currentSymbol = latestSymbols[id]
//                if (newSymbol == currentSymbol) return
//
//                Handler(Looper.getMainLooper()).post {
//                    val loaded = DataStorage.alertsLiveData.value ?: emptyList()
//                    val alerts = loaded
//                    activeAlert = findAlertsForCoin(newSymbol, alerts, AlertStatus.ACTIVE).toMutableSet()
//                    syncActiveAlerts(newSymbol)
//                    lastTriggerTimes.clear()
//                    latestSymbols[id] = newSymbol
//                    holder.coinSymbol = newSymbol
//                    Log.d("PriceMonitor", "🔁 Symbol changed → ${activeAlert.size} active alerts loaded for $newSymbol from $id")
////                    CrashLogger.log(this@FloatingBrowserService, "FloatingService", "🔁 Symbol changed → ${activeAlert.size} active alerts loaded for $newSymbol")
//
//                }
//            }
//
//            @JavascriptInterface
//            fun onInjectedSpanClick(spanText: String) {
//                val service = serviceRef.get() ?: return
//                if (service.isDestroying) return
//
//                Log.d("InstantAlertWatcher", "✅ Injected span clicked: $spanText")
////                CrashLogger.log(this@FloatingBrowserService, "FloatingService", "✅ Injected span clicked: $spanText")
//
//                val regex = Regex("Add alert on ([A-Z0-9]+) at ([\\d,.]+)")
//                val match = regex.find(spanText)
//                if (match != null) {
//                    val symbol = match.groupValues[1]
//                    val spanPrice = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
//                    Log.d("InstantAlertWatcher", "Parsed symbol=$symbol price=$spanPrice")
//
//                    val handler = Handler(Looper.getMainLooper())
//                    handler.postDelayed({
//                        if (!service.isDestroying) {
//                            val latestPrice = latestPrices[id]
//                            if (latestPrice != null && latestPrice > 0) {
////                                service.createInstantAlert(id, symbol, spanPrice, latestPrice)
//                            } else {
//                                webView.evaluateJavascript("document.title") { title ->
//                                    val clean = title.trim('"').replace("\\n", "").trim()
//                                    val priceFromTitle = Regex("[\\d,.]+").find(clean)?.value?.replace(",", "")?.toDoubleOrNull() ?: spanPrice
////                                    service.createInstantAlert(id, symbol, spanPrice, priceFromTitle)
//                                }
//                            }
//                        }
//                    }, 100)
//                }
//            }
//        }, "Android")

//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.PendingIntent
//import android.content.Context
//import android.content.Intent
//import android.media.MediaPlayer
//import android.media.RingtoneManager
//import android.util.Log
//import androidx.annotation.RequiresPermission
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import androidx.core.net.toUri
//import com.example.floatingweb.R
//import com.example.floatingweb.helpers.PriceAlert
//import com.example.floatingweb.services.FloatingBrowserService
//

// Use a single adapter and update data inside it
//        val adapter = ArrayAdapter(
//            this,
//            android.R.layout.simple_dropdown_item_1line,
//            mutableListOf<String>()
//        )
//        nameInput.setAdapter(adapter)
//        nameInput.setOnClickListener {
//            val key = nameInput.text.toString().trim()
//            val suggestions = recommendationMap[key]
//            if (suggestions != null) {
//                val adapter = ArrayAdapter(
//                    this,
//                    android.R.layout.simple_dropdown_item_1line,
//                    suggestions
//                )
//                nameInput.setAdapter(adapter)
//                nameInput.showDropDown()
//            }
//        }

// Handle text input
//        nameInput.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {}
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                val key = s?.toString()?.trim()
//                val suggestions = recommendationMap[key]
//                if (suggestions != null) {
//                    adapter.clear()
//                    adapter.addAll(suggestions)
//                    adapter.notifyDataSetChanged()
//                    if (!nameInput.isPopupShowing) {
//                        nameInput.showDropDown()
//                    }
//                }
//            }
//        })

//@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//@SuppressLint("UnspecifiedImmutableFlag")
//private fun showPriceAlertNotification(alert: PriceAlert, currentPrice: Double) {
//    if (isDestroying) return
//
//    val stopIntent = Intent(this, FloatingBrowserService::class.java).apply {
//        action = "STOP_ALERT"
//        putExtra("alertId", alert.id)
//    }
//    val stopPendingIntent = PendingIntent.getService(
//        this, alert.id.hashCode(), stopIntent,
//        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//    )
//
//    val notification = NotificationCompat.Builder(this, "price_alert_channel")
//        .setSmallIcon(R.drawable.ic_launcher_foreground)
//        .setContentTitle("Price Alert: ${alert.symbol}")
//        .setContentText("${alert.type} ${alert.threshold}, Current Price: $currentPrice")
//        .setPriority(NotificationCompat.PRIORITY_HIGH)
//        .setCategory(NotificationCompat.CATEGORY_ALARM)
//        .addAction(R.drawable.ic_launcher_foreground, "Stop Sound", stopPendingIntent)
//        .setAutoCancel(false)
//        .build()
//
//    try {
//        NotificationManagerCompat.from(this).notify(alert.id.hashCode(), notification)
//    } catch (e: Exception) {
//        Log.e("FloatingService", "Error showing notification: ${e.message}")
////            CrashLogger.log(this, "FloatingService", "Error showing notification: ${e.message}")
//
//    }
//}

//private fun playAlertSound() {
//    if (isDestroying) return
//
//    val prefs = getSharedPreferences("price_alert", Context.MODE_PRIVATE)
//    val uriString = prefs.getString("custom_sound_uri", null)
//    val alarmUri = uriString?.toUri() ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
//    Log.d("sound", "playing alert sound")
//
//    try {
//        mediaPlayer?.stop()
//        mediaPlayer?.release()
//        mediaPlayer = null
//
//        mediaPlayer = MediaPlayer().apply {
//            setDataSource(this@FloatingBrowserService, alarmUri)
//            isLooping = true
//            prepare()
//            start()
//        }
//    } catch (e: Exception) {
//        Log.e("FloatingService", "Error playing alert sound: ${e.message}")
////            CrashLogger.log(this, "FloatingService", "Error showing notification: ${e.message}")
//
//    }
//}
//


import android.view.WindowManager

//private fun cloneLayoutParams(src: WindowManager.LayoutParams): WindowManager.LayoutParams {
//    return WindowManager.LayoutParams().apply {
//        width = src.width
//        height = src.height
//        x = src.x
//        y = src.y
//        gravity = src.gravity
//        flags = src.flags
//        type = src.type
//        format = src.format
//        windowAnimations = src.windowAnimations
//        token = src.token
//        packageName = src.packageName
//        alpha = src.alpha
//    }
//}



//import android.annotation.SuppressLint
//import android.graphics.Color
//import android.graphics.PixelFormat
//import android.graphics.Typeface
//import android.graphics.drawable.GradientDrawable
//import android.util.Log
//import android.view.Gravity
//import android.view.MotionEvent
//import android.view.View
//import android.view.ViewGroup
//import android.view.WindowManager
//import android.widget.Button
//import android.widget.FrameLayout
//import android.widget.LinearLayout
//import android.widget.ScrollView
//import android.widget.Space
//import android.widget.TextView
//import androidx.core.graphics.toColorInt
//import androidx.core.view.setPadding
//import kotlin.collections.component1
//import kotlin.collections.component2
//import kotlin.collections.set
//
//@SuppressLint("InflateParams", "ClickableViewAccessibility")
//private fun toggleOverlayBoxWindow(forceOpen: Boolean = false) {
//    if (isDestroying) return
////        CrashLogger.log(this, "FloatingService", "⚡ Entered [toggle overlayBoxWindow] ")
//
//
//    toggleHide(true)
//    if (overlayBoxWindow != null && !forceOpen) {
//        attachedInBox.forEach { id ->
//            val holder = overlaysById[id] ?: return@forEach
//            val overlay = holder.overlay
//
//            overlay.setOnTouchListener(null)
//            overlay.isClickable = true
//            holder.webView.setOnTouchListener { _, event ->
//                if (event.action == MotionEvent.ACTION_DOWN && !isDestroying) {
//                    requestOverlayFocus(id)
//                }
//                false
//            }
//
//            try { (overlay.parent as? ViewGroup)?.removeView(overlay) } catch (_: Exception) {}
//            val params = savedWindowParams[id] ?: defaultMiniParams(id)
//            safeAttachToWindow(id, params)
//        }
//        attachedInBox.clear()
//        savedBoxWrappers.clear()
//
//        try {
//            overlayBoxWindow?.let { windowManager?.removeViewImmediate(it) }
//        } catch (_: Exception) {}
//        overlayBoxWindow = null
//        overlayBoxWindowParams = null
//        return
//    }
//
//    if (overlayBoxWindow != null && forceOpen) {
//        try { windowManager?.updateViewLayout(overlayBoxWindow, overlayBoxWindowParams) } catch (_: Exception) {}
//        return
//    }
//
//    val box = FrameLayout(this).apply {
//        setBackgroundColor(Color.BLACK)
//        elevation = dpToPx(12).toFloat()
//    }
//
//    val params = WindowManager.LayoutParams(
//        WindowManager.LayoutParams.MATCH_PARENT,
//        WindowManager.LayoutParams.MATCH_PARENT,
//        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
//                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
//                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
//                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
//        PixelFormat.TRANSLUCENT
//    ).apply {
//        gravity = Gravity.END or Gravity.CENTER_VERTICAL
//        x = dpToPx(8)
//    }
//
//    val header = LinearLayout(this).apply {
//        orientation = LinearLayout.HORIZONTAL
//        setPadding(dpToPx(8))
//        setBackgroundColor("#FFBB86FC".toColorInt())
//        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(48))
//    }
//
//    val title = TextView(this).apply {
//        text = "Overlays"
//        textSize = 16f
//        setTypeface(null, Typeface.BOLD)
//        setTextColor(Color.WHITE)
//        setPadding(dpToPx(8), 6, dpToPx(8), 6)
//        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
//    }
//
//    val destroyBtn = Button(this).apply {
//        text = "Destroy 💣"
//        textSize = 16f
//        setTextColor(Color.RED)
//        setBackgroundColor("#FFBB86FC".toColorInt())
//        setPadding(dpToPx(8), 6, dpToPx(8), 6)
//        setOnClickListener { destroyServiceCompletely() }
//    }
//
//    val close = Button(this).apply {
//        text = "Hide All ● "
//        textSize = 16f
//        setTextColor(Color.BLACK)
//        setBackgroundColor(Color.TRANSPARENT)
//        setPadding(dpToPx(8), 6, dpToPx(8), 6)
//        setOnClickListener {
////                CrashLogger.log(this@FloatingBrowserService, "FloatingService", "⚡ Clicked [Hide All]")
//            restoreOverlayToMini()
////                if (overlayBoxWindow != null && !forceOpen) {
////                    attachedInBox.forEach { id ->
////                        val overlay = overlaysById[id]?.overlay ?: return@forEach
////
////                        overlay.setOnTouchListener(null)
////                        overlay.isClickable = true
////                        overlaysById[id]?.webView?.setOnTouchListener { _, event ->
////                            if (event.action == MotionEvent.ACTION_DOWN && !isDestroying) {
////                                requestOverlayFocus(id)
////                            }
////                            false
////                        }
////
////                        try { (overlay.parent as? ViewGroup)?.removeView(overlay) } catch (_: Exception) {}
////                        val miniParams = defaultMiniParams(id)
////                        safeAttachToWindow(id, miniParams)
////                        switchToState(STATE_MINI, id)
////                    }
////                    attachedInBox.clear()
////                    savedBoxWrappers.clear()
////                    toggleHide(false)
////
////                    try {
////                        overlayBoxWindow?.let { windowManager?.removeViewImmediate(it) }
////                    } catch (e: Exception) {
//////                        CrashLogger.log(this@FloatingBrowserService, "FloatingService", "Hide all err ${e.message}")
////                    }
////                    overlayBoxWindow = null
////                    overlayBoxWindowParams = null
////
////                }
//        }
//    }
//
//    val spacer = Space(this).apply {
//        layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
//    }
//
//    header.addView(title)
//    header.addView(destroyBtn)
//    header.addView(spacer)
//    header.addView(close)
//    box.addView(header)
//    // ... header setup as before ...
//    // header, destroyBtn, close, spacer, title (unchanged)
//
//    val scroll = ScrollView(this).apply {
//        layoutParams = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.MATCH_PARENT,
//            FrameLayout.LayoutParams.MATCH_PARENT
//        ).apply { topMargin = dpToPx(48) }
//        isVerticalScrollBarEnabled = true
//        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
//        isFocusable = true
//        isFocusableInTouchMode = true
//        isClickable = true
//    }
//
//    val inner = LinearLayout(this).apply {
//        orientation = LinearLayout.VERTICAL
//        setPadding(dpToPx(8))
//    }
//
//    overlaysById.forEach { (id, holder) ->
//        if (!savedWindowParams.containsKey(id)) {
//            (holder.overlay.layoutParams as? WindowManager.LayoutParams)?.let { orig ->
//                savedWindowParams[id] = cloneLayoutParams(orig)
//            }
//        }
//
//        safeDetachView(id)
//
//        val wrapper = savedBoxWrappers[id] ?: FrameLayout(this).apply {
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                dpToPx(220)
//            ).apply { bottomMargin = dpToPx(8) }
//            background = GradientDrawable().apply {
//                setColor(Color.WHITE)
//                setStroke(1, Color.LTGRAY)
//                cornerRadius = dpToPx(8).toFloat()
//            }
//            elevation = dpToPx(3).toFloat()
//        }.also { savedBoxWrappers[id] = it }
//
//        holder.overlay.setOnTouchListener { _, _ -> false }
//        holder.overlay.isClickable = false
//
//        holder.webView.apply {
//            setOnTouchListener { _, _ -> false }
//            isVerticalScrollBarEnabled = false
//            isHorizontalScrollBarEnabled = false
//        }
//
//        holder.overlay.layoutParams = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.MATCH_PARENT,
//            FrameLayout.LayoutParams.MATCH_PARENT
//        )
//
//        val touchInterceptor = View(this).apply {
//            layoutParams = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT
//            )
//            setBackgroundColor(Color.TRANSPARENT)
//            isClickable = true
//            setOnTouchListener { _, event ->
//                if (event.action == MotionEvent.ACTION_UP && !isDestroying) {
//                    holder.bubble.performClick()
//                }
//                false
//            }
//        }
//
//        try {
//            (wrapper.parent as? ViewGroup)?.removeView(wrapper)
//            if (wrapper.childCount > 0) wrapper.removeAllViews()
//            wrapper.addView(holder.overlay)
//            wrapper.addView(touchInterceptor)
//        } catch (e: Exception) {
//            try {
//                (holder.overlay.parent as? ViewGroup)?.removeView(holder.overlay)
//                wrapper.removeAllViews()
//                wrapper.addView(holder.overlay)
//                wrapper.addView(touchInterceptor)
//            } catch (_: Exception) {}
//        }
//
//        inner.addView(wrapper)
//        attachedInBox.add(id)
//    }
//
//    scroll.addView(inner)
//    box.addView(scroll)
//
//    try {
//        windowManager?.addView(box, params)
//        overlayBoxWindow = box
//        overlayBoxWindowParams = params
//        box.translationX = dpToPx(340).toFloat()
//        box.animate().translationX(0f).setDuration(200).start()
//    } catch (e: Exception) {
//        Log.e("FloatingService", "Failed to add sideBox window: ${e.message}")
////            CrashLogger.log(this, "FloatingService", "Failed to add sideBox window: ${e.message}")
//
//        attachedInBox.forEach { id ->
//            val holder = overlaysById[id] ?: return@forEach
//            try { (holder.overlay.parent as? ViewGroup)?.removeView(holder.overlay) } catch (_: Exception) {}
//            safeAttachToWindow(id, savedWindowParams[id] ?: defaultMiniParams(id))
//        }
//        attachedInBox.clear()
//    }
//}
//
//








//import android.annotation.SuppressLint
//import android.graphics.Color
//import android.util.Log
//
//// 🔧 FIX: Use WeakReference to prevent context leaks
//@SuppressLint("SetJavaScriptEnabled")
//private fun setupPriceMonitor(id: String) {
////        CrashLogger.log(this, "FloatingService", "⚡ Entered [setup Price Monitor] for id=$id")
//    Log.d("PriceMonitor", "1 step for $id")
//
//    val holder = overlaysById[id] ?: return
//    if (!holder.alertEnabled) {
//        Log.d("PriceMonitor", "🚫 Skipping setup for $id (alert disabled)")
//        holder.headerBar.setBackgroundColor(Color.BLUE)
//        return
//    }
//
//    if (holder.monitorInitialized) return
//    holder.monitorInitialized = true
//
//    val webView = holder.webView.apply {
//        settings.javaScriptEnabled = true
//        settings.domStorageEnabled = true
//    }
//
//    val jsPriceTracker = """
//        (function() {
//            const state = { observer: null, lastSymbol: "", lastPrice: NaN, debounceTimer: null };
////            console.log("priceMonitoor","js running ")
//            function cleanup() {
//                if (state.observer) state.observer.disconnect();
//                if (state.debounceTimer) clearTimeout(state.debounceTimer);
//            }
//            window.cleanupPriceObserver = cleanup;
//
//            function parseSymbol(txt) {
//    const m = txt.match(/^[A-Z0-9.:-]+/);
//    const symbol = m ? m[0].trim() : "";
//    return symbol;
//}
//
//function parsePrice(txt) {
//    const m = txt.match(/[\d,]+(\.\d+)?/); // integer or decimal
//    const price = m ? parseFloat(m[0].replace(/,/g, '')) : NaN;
//    return price;
//}
//
//
//            function sendUpdate(symbol, price) {
//                if (!symbol || !Number.isFinite(price)) return;
//                Android.checkPrice(JSON.stringify({ symbol, price }));
//                if (symbol !== state.lastSymbol) Android.onSymbolChange(symbol);
//                state.lastSymbol = symbol; state.lastPrice = price;
//            }
//
//            function handleTitleChange() {
//                const t = document.title || '';
//                const s = parseSymbol(t), p = parsePrice(t);
//                if (s && Number.isFinite(p)) {
//                sendUpdate(s, p);
//                }
//            }
//
//            const titleEl = document.querySelector('title');
//            if (titleEl) {
//                state.observer = new MutationObserver(handleTitleChange);
//                state.observer.observe(titleEl, { childList:true, characterData:true, subtree:true });
//                handleTitleChange();
//            }
//        })();
//    """.trimIndent()
//
////        webView.webViewClient = object : WebViewClient() {
////            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
////                super.onPageStarted(view, url, favicon)
////        webView.evaluateJavascript("window.cleanupPriceObserver?.();", null)
////                lastTriggerTimes.clear()
////            }
//
////            override fun onPageFinished(view: WebView?, url: String?) {
////                super.onPageFinished(view, url)
////                if (isDestroying) return
////        startPriceFreezeWatchdog()
//    webView.evaluateJavascript(jsPriceTracker, null)
//
////        webView.postDelayed({
////            if (!isDestroying) {
////                webView.evaluateJavascript("document.title") { title ->
////                    val clean = title.trim('"').replace("\\n", "").trim()
////                    val alerts = DataStorage.loadAlerts(this@FloatingBrowserService)
////                    activeAlert = findAlertsForCoin(clean, alerts, AlertStatus.ACTIVE).toMutableSet()
////                    syncActiveAlerts()
////                    startPriceFreezeWatchdog()
////                    Log.d("PriceMonitor", "✅ Page finished: '$clean' → ${activeAlert.size} active alerts. from $id")
////                    CrashLogger.log(this@FloatingBrowserService, "FloatingService", "✅ Page finished: '$clean' → ${activeAlert.size} active alerts.")
////
//////                    webView.evaluateJavascript(jsPriceTracker, null)
////
////    //                            injectAutoTimeframeScriptIfScreenOn(id)
////    //                            view.postDelayed({
////    //                                if (!isDestroying) {
////    //                                    val index = getindexofid(id)
////    //                                    val targetValue = when (index) {
////    //                                        0 -> "1"
////    //                                        1 -> "5"
////    //                                        2 -> "15"
////    //                                        3 -> "60"
////    //                                        4 -> "240"
////    //                                        5 -> "1D"
////    //                                        6 -> "1W"
////    //                                        7 -> "1M"
////    //                                        else -> null
////    //                                    }
////    //                                    val targetValuelabel = when (index) {
////    //                                        0 -> "1 minute"
////    //                                        1 -> "5 minutes"
////    //                                        2 -> "15 minutes"
////    //                                        3 -> "1 hour"
////    //                                        4 -> "4 hours"
////    //                                        5 -> "1 day"
////    //                                        6 -> "1 week"
////    //                                        7 -> "1 month"
////    //                                        else -> null
////    //                                    }
////    //                                    if (targetValue != null) {
////    //                                        val jsClick = """
////    //(function() {
////    //    try {
////    //        let attempts = 0;
////    //        const maxAttempts = 10;
////    //        const targetValue = "${targetValue}";
////    //        const targetLabel = "${targetValuelabel}";
////    //
////    //        function tryClick() {
////    //            const btn = document.querySelector(`button[data-value='${'$'}{targetValue}']`) ||
////    //                        document.querySelector(`button[aria-label='${'$'}{targetLabel}']`);
////    //            if (btn) {
////    //                btn.click();
////    //                console.log("✅ Clicked timeframe", targetValue);
////    //                setTimeout(() => {
////    //                    try {
////    //                        const resetBtn = document.querySelector(".js-btn-reset");
////    //                        if (resetBtn) {
////    //                            const evt2 = new MouseEvent("click", {
////    //                                bubbles: true,
////    //                                cancelable: true,
////    //                                view: window,
////    //                                isTrusted: true
////    //                            });
////    //                            resetBtn.dispatchEvent(evt2);
////    //                            console.log("✅ Clicked: Reset frame button");
////    //                        }
////    //                    } catch (err) {
////    //                        console.log("Reset button click error:", err);
////    //                    }
////    //                }, 2000);
////    //            } else if (attempts++ < maxAttempts) {
////    //                setTimeout(tryClick, 500);
////    //            }
////    //        }
////    //        tryClick();
////    //    } catch(e) {
////    //        console.log("Auto-click error:", e);
////    //    }
////    //
////    //function autoClickRestore() {
////    //    try {
////    //        const buttons = document.querySelectorAll("button");
////    //        for (const btn of buttons) {
////    //            const text = (btn.innerText || "").trim();
////    //            const tooltip = btn.getAttribute("data-overflow-tooltip-text") || "";
////    //            if (text.includes("Restore connection") || tooltip.includes("Restore connection")) {
////    //                const evt = new MouseEvent("click", {
////    //                    bubbles: true,
////    //                    cancelable: true,
////    //                    view: window,
////    //                    isTrusted: true
////    //                });
////    //                setTimeout(() => btn.dispatchEvent(evt), 2500);
////    //                console.log("✅ Dispatched real click: Restore connection");
////    //                return;
////    //            }
////    //        }
////    //    } catch (e) {
////    //        console.log("Restore connection watcher error:", e);
////    //    }
////    //}
////    //
////    //const observer = new MutationObserver(() => autoClickRestore());
////    //observer.observe(document.body, { childList: true, subtree: true });
////    //
////    //})();
////    //""".trimIndent()
////    //                                        view.evaluateJavascript(jsClick, null)
////    //                                    }
////    //                                }
////    //                            }, 500)
////
////    //                            auto alert set
//////                    webView.postDelayed({
//////                        if (!isDestroying) {
//////                            val jsSetupInstantAlert = """
//////    (function() {
//////        if (!window._instantAlertWatcherAdded) {
//////            window._instantAlertWatcherAdded = true;
//////            document.body.addEventListener('click', function(e) {
//////                let target = e.target;
//////                let text = (target.textContent || "").trim();
//////                if (text.startsWith("Add alert on")) {
//////                    if (window.Android && window.Android.onInjectedSpanClick) {
//////                        window.Android.onInjectedSpanClick(text);
//////                    }
//////                    setTimeout(() => {
//////                        const closeBtn = document.querySelector("button.overlayBtn-FvtqqqvS");
//////                        if (closeBtn) {
//////                            closeBtn.click();
//////                            console.log("✅ Close button clicked automatically");
//////                        }
//////                    }, 1000);
//////                }
//////            }, false);
//////        }
//////    })();
//////    """.trimIndent()
//////                            webView.evaluateJavascript(jsSetupInstantAlert, null)
//////                        }
//////                    }, 2000)
////                }
////            }
////        }, 500)
////            }
////        }
//}
//
//
















//    @RequiresApi(Build.VERSION_CODES.P)
//    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
//    private fun setupWebView(id: String, url: String) {
//        val holder = overlaysById[id] ?: return
//        val context = this
//
//        // 🚀 CRITICAL: Reset before recreation (prevents memory leaks)
//        holder.webView?.apply {
//            settings.javaScriptEnabled = false
//            loadDataWithBaseURL(null, "", "text/html", "UTF-8", null)
//            clearHistory()
////            onDestroy()
//        }
//
//        // 🔥 HIGH-PERFORMANCE WEBVIEW CREATION
//        // Inside setupWebView()
//        val webView = WebView(context).apply {
//            setBackgroundColor(Color.WHITE)
//            settings.apply {
//                javaScriptEnabled = true
//                domStorageEnabled = true
//                databaseEnabled = false
//                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
//                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
//                javaScriptCanOpenWindowsAutomatically = true
//                setSupportMultipleWindows(true)
//                userAgentString =
//                    "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 " +
//                            "TradingViewFloating/1.0 (compatible; TradingView)"
//                setSupportZoom(false)
//                displayZoomControls = false
//                textZoom = 100
//                mediaPlaybackRequiresUserGesture = true
//                blockNetworkImage = false
//            }
//        }
//
//// ✅ Correct cookie handling (no casting at all)
//        val cm = CookieManager.getInstance()
//        cm.setAcceptCookie(true)
//        cm.setAcceptThirdPartyCookies(webView, true)
//        cm.flush()
//
//        webView.webChromeClient = object : WebChromeClient() {
//
//            // Keep a reference so onCloseWindow can remove the correct container
//            private val popupContainers = mutableMapOf<WebView, ViewGroup>()
//
//            override fun onCreateWindow(
//                view: WebView,
//                isDialog: Boolean,
//                isUserGesture: Boolean,
//                resultMsg: Message
//            ): Boolean {
//                Log.d("WEBVIEW_DEBUG", "onCreateWindow triggered (userGesture=$isUserGesture)")
//
//                // Create new popup webview (must be fresh - not previously navigated)
//                val popupWebView = WebView(context).apply {
//                    // Keep same user agent / important settings
//                    settings.apply {
//                        javaScriptEnabled = true
//                        domStorageEnabled = true
//                        javaScriptCanOpenWindowsAutomatically = true
//                        setSupportMultipleWindows(true)
//                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
//                        // reuse outer userAgent if set
//                        userAgentString = webView.settings.userAgentString
//                    }
//
//                    // Console bridging so you can see JS logs from popup
////                    webChromeClient = this@object;
//                webViewClient = object : WebViewClient() {
//                    override fun onPageStarted(v: WebView, u: String?, f: Bitmap?) {
//                        Log.d("WEBVIEW_DEBUG", "Popup started: $u")
//                    }
//                    override fun onPageFinished(v: WebView, u: String?) {
//                        Log.d("WEBVIEW_DEBUG", "Popup finished: $u")
//                    }
//                    override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
//                        val url = r.url.toString()
//                        Log.d("WEBVIEW_DEBUG", "Popup navigation: $url")
//                        // broker callback handling
//                        if (url.contains("tradingview.com/broker-callback")) {
//                            Log.d("WEBVIEW_DEBUG", "Broker callback detected")
//                            Handler(Looper.getMainLooper()).post {
//                                if (!isDestroying) webView.reload()
//                            }
//                            return true
//                        }
//                        // Try launching binance intent if requested
//                        if (url.startsWith("binance://")) {
//                            try {
//                                Log.d("WEBVIEW_DEBUG", "Launching Binance intent: $url")
//                                context.startActivity(Intent.parseUri(url, Intent.URI_INTENT_SCHEME))
//                                return true
//                            } catch (e: Exception) {
//                                Log.e("WEBVIEW_DEBUG", "Binance intent failed", e)
//                            }
//                        }
//                        return false
//                    }
//                }
//                }
//
//                // Ensure cookies are allowed for popup too
//                try {
//                    val cm = CookieManager.getInstance()
//                    cm.setAcceptCookie(true)
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        cm.setAcceptThirdPartyCookies(popupWebView, true)
//                    }
//                    cm.flush()
//                } catch (e: Exception) {
//                    Log.w("WEBVIEW_DEBUG", "CookieManager popup setup failed", e)
//                }
//
//                // Build a container for the popup and attach it to the overlay
//                val popupContainer = FrameLayout(context).apply {
//                    isFocusable = true
//                    isClickable = true
//                    // optional: add a semi-transparent background or close button if you want
//                    // setBackgroundColor(Color.argb(200, 0, 0, 0))
//                    addView(
//                        popupWebView,
//                        FrameLayout.LayoutParams(
//                            FrameLayout.LayoutParams.MATCH_PARENT,
//                            FrameLayout.LayoutParams.MATCH_PARENT
//                        )
//                    )
//                }
//
//                // Attach to the same overlay holder so popup is visible to user
//                holder.overlay.addView(popupContainer)
//                popupContainers[popupWebView] = popupContainer
//
//                // Give focus to the popup
//                popupWebView.requestFocus()
//                popupContainer.requestFocus()
//
//                // Provide the new webview back to renderer
//                val transport = resultMsg.obj as WebView.WebViewTransport
//                transport.webView = popupWebView
//                resultMsg.sendToTarget()
//
//                Log.d("WEBVIEW_DEBUG", "Popup WebView attached to overlay")
//                return true
//            }
//
//            override fun onCloseWindow(window: WebView?) {
//                window ?: return
//                Log.d("WEBVIEW_DEBUG", "onCloseWindow called for popup")
//                val container = popupContainers.remove(window)
//                try {
//                    // remove popup safely
//                    container?.let {
//                        (it.parent as? ViewGroup)?.removeView(it)
//                    }
//                    window.stopLoading()
//                    window.webChromeClient = null
////                    window.webViewClient = null
//                    (window.parent as? ViewGroup)?.removeView(window)
//                    window.destroy()
//                } catch (e: Exception) {
//                    Log.w("WEBVIEW_DEBUG", "Error during popup destroy", e)
//                }
//            }
//
//            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
//                Log.d(
//                    "WEBVIEW_JS",
//                    "Line ${consoleMessage.lineNumber()} @${consoleMessage.sourceId()}: ${consoleMessage.message()}"
//                )
//                return true
//            }
//        }
//
//        // ✅ BROKER-SAFE WEBVIEW CLIENT
//        webView.webViewClient = object : WebViewClient() {
//            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
//                super.onPageStarted(view, url, favicon)
//
//                // 🛡️ Security: Reset broker state on new page
//                if (url?.contains("tradingview.com/chart/") == true) {
//                    view.evaluateJavascript("""
//                    (function() {
//                        if (typeof TradingView !== 'undefined') {
//                            // Force broker refresh
//                            TradingView.onready = TradingView.onready || [];
//                            TradingView.onready.push(() => {
//                                console.log("Broker fixed: TV ready");
//                                $('.js-broker-link[data-broker="binance"]').click();
//                            });
//                        }
//                    })();
//                """.trimIndent(), null)
//                }
//            }
//
//            override fun onPageFinished(view: WebView, url: String?) {
//                super.onPageFinished(view, url)
//                if (isDestroying) return
//
//                // 🌐 Critical: Fix cookie synchronization
//                CookieManager.getInstance().flush()
//
//                // 💡 Binance-specific tweaks
//                view.evaluateJavascript("""
//                (function() {
//                    // 1. Fix mixed content blocking
//                    if (window.location.protocol === 'https:') {
//                        document.querySelectorAll('iframe').forEach(iframe => {
//                            if (iframe.src.startsWith('http:')) {
//                                iframe.src = iframe.src.replace('http:', 'https:');
//                            }
//                        });
//                    }
//
//                    // 2. Auto-click broker connect if stuck
//                    if (/tradingview\.com\/chart\//.test(window.location.href)) {
//                        setTimeout(() => {
//                            const binanceBtn = document.querySelector('.js-broker-link[data-broker="binance"]');
//                            if (binanceBtn && !binanceBtn.closest('.js-broker-connected')) {
//                                binanceBtn.click();
//                                console.log("Auto-clicked Binance broker button");
//                            }
//                        }, 2000);
//                    }
//
//                    // 3. Override window.open for broker
//                    const originalOpen = window.open;
//                    window.open = function(url, target, features) {
//                        if (url.includes('accounts.binance.com')) {
//                            // Force same-window navigation (fixes popup crash)
//                            window.location.href = url;
//                            return null;
//                        }
//                        return originalOpen(url, target, features);
//                    };
//                })();
//            """.trimIndent(), null)
//            }
//
//            override fun onReceivedSslError(
//                view: WebView,
//                handler: SslErrorHandler,
//                error: SslError
//            ) {
//                // Binance uses valid SSL - this happens due to WebView's strict mode
//                if (error.primaryError == SslError.SSL_DATE_INVALID ||
//                    error.primaryError == SslError.SSL_INVALID) {
//                    handler.proceed() // Safe to proceed
//                } else {
//                    super.onReceivedSslError(view, handler, error)
//                }
//            }
//        }
//
//        // ✋ TOUCH HANDLER (Optimized for responsiveness)
//        webView.setOnTouchListener { _, event ->
//            if (event.action == MotionEvent.ACTION_DOWN && !isDestroying) {
//                requestOverlayFocus(id)
//                // Skip super call to reduce input lag
//                return@setOnTouchListener true
//            }
//            false
//        }
//
//        // 📦 LAYOUT CONFIGURATION
//        val lp = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.MATCH_PARENT,
//            FrameLayout.LayoutParams.MATCH_PARENT
//        ).apply {
//            topMargin = dpToPx(HEADER_HEIGHT_DP)
//        }
//
//        holder.overlay.apply {
//            removeAllViews() // Prevent view leaks
//            addView(webView, lp)
//        }
//
//        holder.webView = webView
//
//        // ⏳ SMART LOADING (Prevents jank)
//        Handler.createAsync(Looper.getMainLooper()).postDelayed({
//            if (isDestroying) return@postDelayed
//
//            // ✅ CRITICAL: Reset cookies before load (fixes auth issues)
//            val cm = CookieManager.getInstance()
//            cm.setCookie("tradingview.com", "broker_session=; Max-Age=0; path=/")
//            cm.setCookie("binance.com", "session=; Max-Age=0; path=/")
//            cm.flush()
//
//            webView.loadUrl(url, mutableMapOf(
//                "Sec-Fetch-Mode" to "navigate",
//                "Sec-Fetch-Site" to "same-origin",
//                "Origin" to "https://www.tradingview.com"
//            ))
//        }, 400L)
//    }


//     🔧 FIX: Proper WebView lifecycle management
//    @RequiresApi(Build.VERSION_CODES.P)
//    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
//    private fun setupWebView(id: String, url: String) {
////        CrashLogger.log(this, "FloatingService", "⚡ Entered [setup webview] for id=$id")
//        val holder = overlaysById[id] ?: return
//        val webView = holder.webView
////
//
//        // configure webView just like you do
//// In setupFloatingOverlay (after addView):
//        Log.d("HWAccel", "Overlay $id accelerated? ${holder.overlay.isHardwareAccelerated}")
//
//// In setupWebView (after addView):
//        holder.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
//        Log.d("HWAccel", "WebView $id accelerated? ${holder.webView.isHardwareAccelerated}")
//        if (!holder.webView.isHardwareAccelerated) {
//            Log.w("HWAccel", "WebView $id still not accelerated - device may not support overlays. Falling back to software.")
//            holder.webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)  // Fallback if hardware fails
//        }
////
//        webView.settings.javaScriptEnabled = true
//        webView.settings.domStorageEnabled = true
//        webView.setBackgroundColor(Color.WHITE)
//        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
////        webView.clearCache(true)
////        val cm = CookieManager.getInstance()
////        cm.setAcceptCookie(true)
////        cm.setAcceptThirdPartyCookies(webView, true)
////        cm.flush()
//
//
//        webView.setOnTouchListener { _, event ->
//            if (event.action == MotionEvent.ACTION_DOWN && !isDestroying) {
//                requestOverlayFocus(id)
//            }
//            false
//        }
//        val lp = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.MATCH_PARENT,
//            FrameLayout.LayoutParams.MATCH_PARENT
//        ).apply {
//            topMargin = dpToPx(HEADER_HEIGHT_DP)
//        }
//        holder.overlay.addView(webView, lp)
//        holder.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
//        Handler(Looper.getMainLooper()).postDelayed({
//            if (!isDestroying) {
//                webView.loadUrl(url)
//            }
//        }, 400L)
////
////        holder.overlay.post { holder.webView.invalidate() }
//
////        js
//
////        val serviceRef = WeakReference(this)
////
////        var activeAlert = mutableSetOf<PriceAlert>()
////
////        activeAlertsState.observe { map ->
////            map[holder.coinSymbol]?.let { alerts ->
////                if (alerts != activeAlert) {
////                    activeAlert = alerts
////                    Log.d("Overlay", "Updated alerts for $id")
////                }
////            }
////        }
////
////        fun syncActiveAlerts(coinSymbol: String) {
////            val map = activeAlertsState.value
////            map[coinSymbol] = activeAlert
////            activeAlertsState.value = map
////        }
////
////
////        ensureWakeLock()
////        val lastTriggerTimes = mutableMapOf<String, Long>()
////        var lastPriceChange: Long = System.currentTimeMillis()
////        val FREEZE_THRESHOLD_MS = 120_000L
////
////        // 🔧 FIX: Use Handler with proper cleanup
////        val handler = Handler(Looper.getMainLooper())
////        handlersById[id] = handler // Track for cleanup
////
////        val priceFreezeRunnable = object : Runnable {
////            override fun run() {
////                if (isDestroying) return
////
//
////                val now = System.currentTimeMillis()
////                if ((now - lastPriceChange) > FREEZE_THRESHOLD_MS) {
////                    Log.w("PriceMonitor", "⚠️ Price frozen, reloading WebView $id")
//////                    CrashLogger.log(this@FloatingBrowserService, "FloatingService", "⚠️ Price frozen, reloading WebView $id")
////
////                    safeReloadWebView(id)
////                    lastPriceChange = now
////                }
////                if (!isDestroying) {
////                    handler.postDelayed(this, 40_000L)
////                }
////            }
////        }
////
////        fun startPriceFreezeWatchdog() {
////            handler.removeCallbacks(priceFreezeRunnable)
////            if (!isDestroying) {
////                handler.postDelayed(priceFreezeRunnable, 15_000L)
////            }
////        }
////
////        fun stopPriceFreezeWatchdog() {
////            handler.removeCallbacks(priceFreezeRunnable)
////        }
////
////
////        // 🔧 FIX: Use WeakReference for context to prevent leaks
////
////        fun loadalerts(){
////            serviceScope.launch(Dispatchers.IO) {
////                val loaded = DataStorage.alertsLiveData.value ?: emptyList()
////                val alerts = loaded
////                withContext(Dispatchers.Main) {
////                    activeAlert = findAlertsForCoin(holder.coinSymbol ?: return@withContext, alerts, AlertStatus.ACTIVE).toMutableSet()
////                    syncActiveAlerts(holder.coinSymbol.toString())
////                }
////            }
////        }
////        loadalerts()
//
//
////        webView.addJavascriptInterface(object {
////            @SuppressLint("MissingPermission")
////            @JavascriptInterface
////            fun checkPrice(jsonString: String) {
////                val service = serviceRef.get() ?: return
////                if (service.isDestroying) return
////
////                try {
////                    val obj = JSONObject(jsonString)
////                    val symbol = obj.getString("symbol")
////                    val price = obj.getDouble("price")
////
////                    lastPriceChange = System.currentTimeMillis()
////                    if (activeAlert.isEmpty() ?: return) return
////
////                    latestPrices[id] = price
////                    latestSymbols[id] = symbol
////                    Log.d("PriceMonitor", "Symbol=$symbol | Price=$price from $id")
//////                    CrashLogger.log(this@FloatingBrowserService, "FloatingService", "🟢 Running check price index=$id")
////
////                    val now = System.currentTimeMillis()
////                    val triggeredNow = mutableSetOf<PriceAlert>()
////
////                    activeAlert.removeAll { alert ->
////                        if (alert.status != AlertStatus.ACTIVE) return@removeAll true
////
////                        val lastTime = lastTriggerTimes[alert.id] ?: 0L
////                        if (now - lastTime < 5000) return@removeAll false
////
////                        val triggered = when (alert.type) {
////                            AlertType.ABOVE -> price > alert.threshold
////                            AlertType.BELOW -> price < alert.threshold
////                        }
////
////                        if (triggered) {
////                            triggeredNow += alert
////                            lastTriggerTimes[alert.id] = now
////                        }
////                        false
////                    }
////
////                    if (triggeredNow.isNotEmpty()) {
////                        triggeredNow.forEach { alert ->
////                            try {
////                                Log.d("PriceMonitor", "🎯 Triggered: ${alert.symbol} ${alert.type} ${alert.threshold} @ $price from $id")
//////                                CrashLogger.log(this@FloatingBrowserService, "FloatingService", "🎯 Triggered: ${alert.symbol} ${alert.type} ${alert.threshold} @ $price")
////                                triggeredAndSaveAlert(alert, price, service)
////                                service.playAlertSound()
////                                service.showPriceAlertNotification(alert, price)
////
////                                var openPageId: String? = null
////
////                                AlertSavedOnPage.forEach { (key, value) ->
////                                    if (value.contains(alert.id)) {
////                                        openPageId = key
////                                        return@forEach
////                                    }
////                                }
////
////                                Handler(Looper.getMainLooper()).post {
////                                    if (!service.isDestroying) {
////                                        holder.overlay.visibility = View.VISIBLE
////                                        if (currentStates[id] != STATE_MAX && openPageId != null) {
////                                            switchToState(STATE_MAX, openPageId)
////                                        }
////                                    }
////                                }
////
////                            } catch (e: Exception) {
////                                Log.e("PriceMonitor", "Error: ${e.message}", e)
//////                                CrashLogger.log(this@FloatingBrowserService, "FloatingService", "Error: ${e.message}")
////
////                            }
////                        }
////
////                        activeAlert.removeAll(triggeredNow)
////                        syncActiveAlerts(symbol)
////                        Log.d("PriceMonitor", "🧹 Removed ${triggeredNow.size} triggered alerts. from $id")
//////                        CrashLogger.log(this@FloatingBrowserService, "FloatingService", "🧹 Removed ${triggeredNow.size} triggered alerts.")
////
////                    }
////
////                    lastTriggerTimes.entries.removeIf { (_, t) -> now - t > 30_000 }
////
////                    if (activeAlert.isEmpty() == true && service::wakeLock.isInitialized && service.wakeLock.isHeld) {
//////                        service.releaseWakeLock()
////                        stopPriceFreezeWatchdog()
////                        Log.d("FloatingService", "WakeLock released (no active alerts).")
//////                        CrashLogger.log(this@FloatingBrowserService, "FloatingService", "(no active alerts)..")
////
////                    }
////
////                } catch (e: Exception) {
////                    Log.e("PriceMonitor", "Invalid JSON: $jsonString", e)
////                }
////            }
////
////            @JavascriptInterface
////            fun onSymbolChange(newSymbol: String) {
////                val service = serviceRef.get() ?: return
////                if (service.isDestroying) return
////
////                val currentSymbol = latestSymbols[id]
////                if (newSymbol == currentSymbol) return
////
////                Handler(Looper.getMainLooper()).post {
////                    val loaded = DataStorage.alertsLiveData.value ?: emptyList()
////                    val alerts = loaded
////                    activeAlert = findAlertsForCoin(newSymbol, alerts, AlertStatus.ACTIVE).toMutableSet()
////                    syncActiveAlerts(newSymbol)
////                    lastTriggerTimes.clear()
////                    latestSymbols[id] = newSymbol
////                    holder.coinSymbol = newSymbol
////                    Log.d("PriceMonitor", "🔁 Symbol changed → ${activeAlert.size} active alerts loaded for $newSymbol from $id")
//////                    CrashLogger.log(this@FloatingBrowserService, "FloatingService", "🔁 Symbol changed → ${activeAlert.size} active alerts loaded for $newSymbol")
////
////                }
////            }
////
////            @JavascriptInterface
////            fun onInjectedSpanClick(spanText: String) {
////                val service = serviceRef.get() ?: return
////                if (service.isDestroying) return
////
////                Log.d("InstantAlertWatcher", "✅ Injected span clicked: $spanText")
//////                CrashLogger.log(this@FloatingBrowserService, "FloatingService", "✅ Injected span clicked: $spanText")
////
////                val regex = Regex("Add alert on ([A-Z0-9]+) at ([\\d,.]+)")
////                val match = regex.find(spanText)
////                if (match != null) {
////                    val symbol = match.groupValues[1]
////                    val spanPrice = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
////                    Log.d("InstantAlertWatcher", "Parsed symbol=$symbol price=$spanPrice")
////
////                    val handler = Handler(Looper.getMainLooper())
////                    handler.postDelayed({
////                        if (!service.isDestroying) {
////                            val latestPrice = latestPrices[id]
////                            if (latestPrice != null && latestPrice > 0) {
//////                                service.createInstantAlert(id, symbol, spanPrice, latestPrice)
////                            } else {
////                                webView.evaluateJavascript("document.title") { title ->
////                                    val clean = title.trim('"').replace("\\n", "").trim()
////                                    val priceFromTitle = Regex("[\\d,.]+").find(clean)?.value?.replace(",", "")?.toDoubleOrNull() ?: spanPrice
//////                                    service.createInstantAlert(id, symbol, spanPrice, priceFromTitle)
////                                }
////                            }
////                        }
////                    }, 100)
////                }
////            }
////        }, "Android")
//
//
//        webView.webViewClient = object : WebViewClient() {
//            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
//                super.onPageStarted(view, url, favicon)
////                view?.evaluateJavascript("window.cleanupPriceObserver?.();", null)
//            }
//
//            override fun onPageFinished(view: WebView?, url: String?) {
//                super.onPageFinished(view, url)
//                if(isDestroying) return
//                view?.postDelayed( {
////                    view.invalidate()  // Force redraw
//                    Log.d("RenderKick", "Forced render for $id after load")
////                    injectAutoTimeframeScriptIfScreenOn(id)
//                },1500L)
//
////                view?.postDelayed({setupPriceMonitor(id) ;if (holder.alertEnabled) startPriceFreezeWatchdog()},3000L)
//
//            }
//        }
//
//    }



//import android.annotation.SuppressLint
//import android.util.Log
//import android.widget.Toast
//import com.example.floatingweb.helpers.AlertType
//import com.example.floatingweb.helpers.PriceAlert
//
//@SuppressLint("SetJavaScriptEnabled")
//fun removeAllInjectedCode(id: String) {
//    val holder = overlaysById[id] ?: return
//    val webView = holder.webView
//
//    webView.post {
//        try {
//            // 🔹 1. Stop intervals, observers, event listeners, etc.
//            val cleanupScript = """
//                try {
//                    // Stop price monitoring loops
//                    if (window.priceMonitorInterval) {
//                        clearInterval(window.priceMonitorInterval);
//                        delete window.priceMonitorInterval;
//                    }
//
//                    // Stop mutation observers
//                    if (window.priceObserver && typeof window.priceObserver.disconnect === 'function') {
//                        window.priceObserver.disconnect();
//                        delete window.priceObserver;
//                    }
//
//                    // Stop any auto-click or DOM event loops
//                    if (window.autoClickInterval) {
//                        clearInterval(window.autoClickInterval);
//                        delete window.autoClickInterval;
//                    }
//
//                    if (window.autoClickHandler && typeof window.removeEventListener === 'function') {
//                        document.removeEventListener('click', window.autoClickHandler, true);
//                        delete window.autoClickHandler;
//                    }
//
//                    // Clean injected functions
//                    delete window.startPriceMonitor;
//                    delete window.startAutoClick;
//                    delete window.injectedScript;
//                    delete window.Android;
//
//                    console.log("🧹 All injected scripts stopped for $id");
//                } catch (e) {
//                    console.error("Cleanup failed for $id", e);
//                }
//            """.trimIndent()
//
//            webView.evaluateJavascript(cleanupScript, null)
//
//            // 🔹 2. Remove Android interface (API 30+)
//            try {
//                webView.removeJavascriptInterface("Android")
//            } catch (_: Exception) { /* older devices may not support */ }
//
//            // 🔹 3. Optional: Force reload if you want a clean state
//            // webView.reload()
//
//            Log.d("PriceMonitor", "🧼 JS injection cleanup complete for $id")
//
//        } catch (e: Exception) {
//            Log.e("PriceMonitor", "❌ removeAllInjectedCode failed for $id", e)
//        }
//    }
//}

//private fun createInstantAlert(id: String, symbol: String, price: Double, latestPrice: Double) {
//    if (isDestroying) return
//
//    val webView = overlaysById[id]?.webView ?: run {
//        Log.e("FloatingService", "WebView not found for id $id")
////            CrashLogger.log(this, "FloatingService", "WebView not found for id $id")
//
//        return
//    }
//
//    val safeCurrent = if (latestPrice.isFinite() && latestPrice > 0.0) latestPrice else 0.0
//    val type = if (safeCurrent < price) AlertType.ABOVE else AlertType.BELOW
//    val alertName = "AutoAlert-$symbol-$safeCurrent"
//
//    Log.d("FloatingService", "Creating alert: $alertName | $symbol | threshold=$price | type=$type | current=$safeCurrent")
////        CrashLogger.log(this@FloatingBrowserService, "FloatingService", "Creating alert: $alertName | $symbol | threshold=$price | type=$type | current=$safeCurrent")
//
//    val newAlert = PriceAlert(name = alertName, symbol = symbol, threshold = price, type = type)
////        val allAlerts = DataStorage.loadAlerts(this)
////        DataStorage.saveAlerts(this, allAlerts + newAlert)
//
//    val map = activeAlertsState.value
//    val set = map.getOrPut(id) { mutableSetOf() }
//    set.add(newAlert)
//    activeAlertsState.value = map
//
//
//    Log.i("FloatingService", "Alert set successfully: $symbol at $price ($type)")
//    Toast.makeText(this, "📈 Alert set: $symbol $type $price", Toast.LENGTH_SHORT).show()
//}













//package com.example.floatingweb.services
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.app.Service
//import android.content.Context
//import android.content.Intent
//import android.graphics.Bitmap
//import android.graphics.Color
//import android.graphics.PixelFormat
//import android.graphics.Typeface
//import android.graphics.drawable.GradientDrawable
//import android.media.MediaPlayer
//import android.media.RingtoneManager
//import android.net.Uri
//import android.os.Build
//import android.os.Handler
//import android.os.Looper
//import android.os.PowerManager
//import android.text.InputType
//import android.util.Log
//import android.view.*
//import android.view.inputmethod.EditorInfo
//import android.view.inputmethod.InputMethodManager
//import android.webkit.JavascriptInterface
//import android.webkit.WebView
//import android.webkit.WebViewClient
//import android.widget.*
//import androidx.annotation.RequiresApi
//import androidx.annotation.RequiresPermission
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import androidx.core.graphics.toColorInt
//import androidx.core.view.setPadding
//import kotlin.math.abs
//import androidx.core.net.toUri
//import androidx.core.widget.addTextChangedListener
//import com.example.floatingweb.helpers.AlertStatus
//import com.example.floatingweb.helpers.DataStorage
//import com.example.floatingweb.helpers.AlertType
//import com.example.floatingweb.helpers.PriceAlert
//import com.example.floatingweb.helpers.findAlertsForCoin
//import com.example.floatingweb.helpers.triggeredAndSaveAlert
//import com.example.floatingweb.R
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import org.json.JSONObject
//
//class FloatingBrowserService : Service() {
//
//    private var windowManager: WindowManager? = null
//    private var parentOverlay: FrameLayout? = null
//
//    var urls = mutableListOf<String>()
//    // Lists to hold multiple child containers
//    private val floatingOverlays = mutableListOf<FrameLayout>()
//    private val webViews = mutableListOf<WebView>()
//    private val headerBars = mutableListOf<LinearLayout>()
//    private val bubbles = mutableListOf<FrameLayout>()
//    private val currentStates = mutableListOf<Int>()
//
//    private val savedPositions = mutableMapOf<Int, Pair<Int, Int>>()
//
//    // Store the latest price per WebView index
//    private val latestPrices = mutableMapOf<Int, Double>()
//
//    // Optional: store the current symbol for reference
//    private val latestSymbols = mutableMapOf<Int, String>()
//
//    val accountsoftabs = mutableMapOf<Int, String>(Pair(0,"user0"),Pair(1,"user1"),Pair(2,"user2"),Pair(3,"user3"),Pair(4,"user4"),Pair(5,"user5"),Pair(6,"user6"),Pair(7,"user7"),Pair(8,"user8"))
//    companion object {
//        lateinit var appContext: Context
//
//        private const val STATE_MAX = 1
//        private const val STATE_MEDIUM = 2
//        private const val STATE_MINI = 3
//
//        private var SIZE_MEDIUM_WIDTH_DP = 300
//        private var SIZE_MEDIUM_HEIGHT_DP = 500
//        private var SIZE_MINI_WIDTH_DP = 130
//        private var SIZE_MINI_HEIGHT_DP = 200
//        private const val HEADER_HEIGHT_DP = 40
//        const val CLICK_THRESHOLD = 10
//    }
//
//    // Updated to store PriceAlert objects
//    private val activeAlerts = mutableMapOf<WebView, MutableSet<PriceAlert>>()
//    // Map to store latest price per WebView
//    private lateinit var wakeLock: PowerManager.WakeLock
//
//    // Helper constant for R.id equivalent in this standalone file context
//
//    override fun onBind(intent: Intent?) = null
//
//    @RequiresApi(Build.VERSION_CODES.P)
//    @SuppressLint("SetJavaScriptEnabled")
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        appContext = this
//        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
//        urls = intent?.getStringArrayListExtra("links") ?: arrayListOf("https://www.google.com")
//        Log.d("links", "$urls")
//
//        val containerSizes = intent?.getBundleExtra("ContainerSizes")
//        val miniSize = containerSizes?.getIntArray("mini") ?: intArrayOf(SIZE_MINI_WIDTH_DP, SIZE_MINI_HEIGHT_DP)
//        val mediumSize = containerSizes?.getIntArray("medium") ?: intArrayOf(SIZE_MEDIUM_WIDTH_DP, SIZE_MEDIUM_HEIGHT_DP)
//
//        SIZE_MEDIUM_WIDTH_DP = mediumSize[0]
//        SIZE_MEDIUM_HEIGHT_DP = mediumSize[1]
//        SIZE_MINI_WIDTH_DP = miniSize[0]
//        SIZE_MINI_HEIGHT_DP = miniSize[1]
//        DataStorage.init(this)
//        if (floatingOverlays.isEmpty()) {
//            setupParentOverlay()
//            setupControlPanel()
//            urls.forEachIndexed { index, url ->
//                setupFloatingOverlay()
//                setupWebView(url, index)
//                setupPriceMonitor(index)
//                setupHeader(index)
//                setupBubble(index)
//                switchToState(STATE_MINI, index)
//            }
//            startOverlayTimerCoroutine()
//        }
//
//        createNotificationChannel()
//        val action = intent?.action
//        if (action != null) {
//            intent.let {
//                when (it.action) {
//                    "STOP_ALERT" -> {
//                        stopAlertSound()
//                        NotificationManagerCompat.from(this).cancelAll()
//
//                    }
//                }
//            }
//        }
//        ensureWakeLock()
//        startForegroundService()
//        return START_STICKY
//    }
//
//    private var overlayJob: Job? = null
//
//    fun startOverlayTimerCoroutine() {
//        overlayJob = CoroutineScope(Dispatchers.Main).launch {
//            Log.d("timer", "running. timer.....")
//
//            while (isActive) {
//                delay(30 * 60 * 1000L)  // 30 minutes
//                toggleOverlayBoxWindow()
//            }
//        }
//    }
//
//
//    private fun ensureWakeLock() {
//        if (!this::wakeLock.isInitialized) {
//            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
//            wakeLock = pm.newWakeLock(
//                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
//                "FloatingWeb::PriceWatchLock"
//            )
//        }
//        if (!wakeLock.isHeld) wakeLock.acquire()
//    }
//
//    private fun releaseWakeLock() {
//        if (this::wakeLock.isInitialized && wakeLock.isHeld) {
//            wakeLock.release()
//        }
//    }
//    /** Parent overlay: full-screen, transparent, touch-through */
//    private fun setupParentOverlay() {
//        val params = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
//                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
//            PixelFormat.TRANSLUCENT
//        )
//
//        parentOverlay = FrameLayout(this).apply {
//            setBackgroundColor(Color.TRANSPARENT)
//        }
//
//        windowManager?.addView(parentOverlay, params)
//    }
//
//    /** Floating overlay: touchable, rounded, shadowed container */
//    private fun setupFloatingOverlay() {
//        val params = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
//                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//            PixelFormat.TRANSLUCENT
//        )
//
//        val floatingOverlay = FrameLayout(this).apply {
//            setBackgroundColor(Color.WHITE)
//            elevation = 16f
//            clipChildren = false
//            clipToOutline = false
//        }
//
//        windowManager?.addView(floatingOverlay, params)
//        floatingOverlays.add(floatingOverlay)
//        currentStates.add(STATE_MINI)
//    }
//
//    private fun requestOverlayFocus(index: Int) {
//        val params = floatingOverlays.getOrNull(index)?.layoutParams as? WindowManager.LayoutParams ?: return
//        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
//        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
//        windowManager?.updateViewLayout(floatingOverlays[index], params)
//    }
//
//    private fun releaseOverlayFocus(index: Int) {
//        val params = floatingOverlays.getOrNull(index)?.layoutParams as? WindowManager.LayoutParams ?: return
//        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//        windowManager?.updateViewLayout(floatingOverlays[index], params)
//    }
//
//    /** WebView inside floating overlay */
//    @RequiresApi(Build.VERSION_CODES.P)
//    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
//    private fun setupWebView(url: String, index: Int) {
//        val suffix = accountsoftabs[index] ?: "user_default"
//        Log.d("WebViewSetup", "Suffix already set: ${suffix}")
//
//
//        val webView = WebView(this).apply {
//            settings.javaScriptEnabled = true
//            settings.domStorageEnabled = true
//            setBackgroundColor(Color.WHITE)
//
//            setOnTouchListener { _, event ->
//                if (event.action == MotionEvent.ACTION_DOWN) {
//                    requestOverlayFocus(index)
//                }
//                false
//            }
//        }
//
//
//        val lp = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.MATCH_PARENT,
//            FrameLayout.LayoutParams.MATCH_PARENT
//        )
//        lp.topMargin = dpToPx(HEADER_HEIGHT_DP)
//        floatingOverlays.getOrNull(index)?.addView(webView, lp)
//        webViews.add(webView)
//
//
//        webView.loadUrl(url)
//    }
//
//    /** Sets up the JS price monitoring for a WebView */
//    @SuppressLint("SetJavaScriptEnabled")
//    private fun setupPriceMonitor(index: Int)   {
//        val webView = webViews.getOrNull(index)?.apply {
//            settings.javaScriptEnabled = true
//            settings.domStorageEnabled = true
//        }
//
//        // Keep local cache for performance
//        var activeAlert = mutableSetOf<PriceAlert>()
//
//        // Always sync local ↔ global (safe)
//        fun syncActiveAlerts() {
//            activeAlerts[webView as WebView] = activeAlert
//        }
//
//        ensureWakeLock() // Keeps CPU alive for monitoring
//        val lastTriggerTimes = mutableMapOf<String, Long>()
//
//        var lastPriceChange: Long = System.currentTimeMillis()
//        val FREEZE_THRESHOLD_MS = 60_000L // 30 seconds to detect freeze
//
//        val handler = Handler(Looper.getMainLooper())
//        val priceFreezeRunnable = object : Runnable {
//            override fun run() {
//                val now = System.currentTimeMillis()
//                if ((now - lastPriceChange) > FREEZE_THRESHOLD_MS) {
//                    Log.w("PriceMonitor", "⚠️ Price frozen, reloading WebView $index")
//                    safeReloadWebView(index)
//                    lastPriceChange = now // reset timer after reload
//                }
//                handler.postDelayed(this, 20_000L) // check every 10s
//            }
//        }
//
//        fun startPriceFreezeWatchdog() {
//            handler.removeCallbacks(priceFreezeRunnable)
//            handler.postDelayed(priceFreezeRunnable, 15_000L)
//        }
//
//        fun stopPriceFreezeWatchdog() {
//            handler.removeCallbacks(priceFreezeRunnable)
//        }
//
//        // -------------------- JS INTERFACE --------------------
//        webView?.addJavascriptInterface(object {
//            /** Called repeatedly as price updates */
//            @SuppressLint("MissingPermission")
//            @JavascriptInterface
//            fun checkPrice(jsonString: String) {
//                try {
//                    val obj = JSONObject(jsonString)
//                    val symbol = obj.getString("symbol")
//                    val price = obj.getDouble("price")
//
//                    lastPriceChange = System.currentTimeMillis()
//                    if (activeAlert.isEmpty()) return
//
//
//
//                    latestPrices[index] = price
//                    latestSymbols[index] = symbol
//                    Log.d("PriceMonitor", "Symbol=$symbol | Price=$price")
//
//                    val now = System.currentTimeMillis()
//                    val triggeredNow = mutableSetOf<PriceAlert>()
//
//                    activeAlert.removeAll { alert ->
//                        // Only ACTIVE alerts are valid
//                        if (alert.status != AlertStatus.ACTIVE) return@removeAll true
//
//                        // Debounce: Skip if fired <5s ago
//                        val lastTime = lastTriggerTimes[alert.id] ?: 0L
//                        if (now - lastTime < 5000) return@removeAll false
//
//                        val triggered = when (alert.type) {
//                            AlertType.ABOVE -> price > alert.threshold
//                            AlertType.BELOW -> price < alert.threshold
//                        }
//
//                        if (triggered) {
//                            triggeredNow += alert
//                            lastTriggerTimes[alert.id] = now
//                        }
//                        false // keep alert unless handled later
//                    }
//
//                    // Process triggered alerts
//                    if (triggeredNow.isNotEmpty()) {
//                        triggeredNow.forEach { alert ->
//                            try {
//                                Log.d("PriceMonitor", "🎯 Triggered: ${alert.symbol} ${alert.type} ${alert.threshold} @ $price")
//                                triggeredAndSaveAlert(alert, price, this@FloatingBrowserService)
//                                playAlertSound()
//                                showPriceAlertNotification(alert, price)
//
//                                Handler(Looper.getMainLooper()).post {
//                                    floatingOverlays.getOrNull(index)?.visibility = View.VISIBLE
//                                    if (currentStates[index] != STATE_MAX) switchToState(STATE_MAX, index)
//                                }
//
//                            } catch (e: Exception) {
//                                Log.e("PriceMonitor", "Error: ${e.message}", e)
//                            }
//                        }
//
//                        activeAlert.removeAll(triggeredNow)
//                        syncActiveAlerts() // Keep map updated
//                        Log.d("PriceMonitor", "🧹 Removed ${triggeredNow.size} triggered alerts.")
//                    }
//
//                    // Clean old debounce data (keep 30s)
//                    lastTriggerTimes.entries.removeIf { (_, t) -> now - t > 30_000 }
//
//                    if (activeAlert.isEmpty() && wakeLock.isHeld) {
//                        releaseWakeLock()
//                        stopPriceFreezeWatchdog()
//                        Log.d("FloatingService", "WakeLock released (no active alerts).")
//                    }
//
//                } catch (e: Exception) {
//                    Log.e("PriceMonitor", "Invalid JSON: $jsonString", e)
//                }
//            }
//
//            /** Called when symbol changes dynamically */
//            @JavascriptInterface
//            fun onSymbolChange(newSymbol: String) {
//                val currentSymbol = latestSymbols[index]
//                if (newSymbol == currentSymbol) return
//
//                Handler(Looper.getMainLooper()).post {
//                    val alerts = DataStorage.loadAlerts(this@FloatingBrowserService)
//                    activeAlert = findAlertsForCoin(newSymbol, alerts, AlertStatus.ACTIVE).toMutableSet()
//                    syncActiveAlerts()
//                    lastTriggerTimes.clear()
//                    latestSymbols[index] = newSymbol
//                    Log.d("PriceMonitor", "🔁 Symbol changed → ${activeAlert.size} active alerts loaded for $newSymbol")
//                }
//            }
//
//            @JavascriptInterface
//            fun onInjectedSpanClick(spanText: String) {
//                Log.d("InstantAlertWatcher", "✅ Injected span clicked: $spanText -> at ")
//
//                val regex = Regex("Add alert on ([A-Z0-9]+) at ([\\d,.]+)")
//                val match = regex.find(spanText)
//                if (match != null) {
//                    val symbol = match.groupValues[1]
//                    val spanPrice = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
//                    Log.d("InstantAlertWatcher", "Parsed symbol=$symbol price=$spanPrice")
//
//                    val handler = Handler(Looper.getMainLooper())
//                    handler.postDelayed(object : Runnable {
//                        override fun run() {
//                            val latestPrice = latestPrices[index]
//                            if (latestPrice != null && latestPrice > 0) {
//                                createInstantAlert(index, symbol, spanPrice, latestPrice)
//                            } else {
//                                // Use document.title price for first-time if available
//                                webViews.getOrNull(index)?.evaluateJavascript("document.title") { title ->
//                                    val clean = title.trim('"').replace("\\n", "").trim()
//                                    val priceFromTitle = Regex("[\\d,.]+").find(clean)?.value?.replace(",", "")?.toDoubleOrNull() ?: spanPrice
//                                    createInstantAlert(index, symbol, spanPrice, priceFromTitle)
//                                    Log.d("InstantAlertWatcher", "Used title price: $priceFromTitle")
//                                }
//                            }
//                        }
//                    }, 100)
//                } else {
//                    Log.w("InstantAlertWatcher", "Could not parse span text: $spanText")
//                }
//            }
//
//        }, "Android")
//
//        // -------------------- JS WATCHER --------------------
//        val js = """
//        (function() {
//            const state = { observer: null, lastSymbol: "", lastPrice: NaN, debounceTimer: null };
//
//            function cleanup() {
//                if (state.observer) state.observer.disconnect();
//                if (state.debounceTimer) clearTimeout(state.debounceTimer);
//            }
//            window.cleanupPriceObserver = cleanup;
//
//            function parseSymbol(txt) {
//                const m = txt.match(/^[A-Z0-9.:-]+/);
//                return m ? m[0].trim() : "";
//            }
//            function parsePrice(txt) {
//                const m = txt.match(/[\d,]+\.\d+/);
//                return m ? parseFloat(m[0].replace(/,/g,'')) : NaN;
//            }
//
//            function sendUpdate(symbol, price) {
//                if (!symbol || !Number.isFinite(price)) return;
//                Android.checkPrice(JSON.stringify({ symbol, price }));
//                if (symbol !== state.lastSymbol) Android.onSymbolChange(symbol);
//                state.lastSymbol = symbol; state.lastPrice = price;
//            }
//
//            function handleTitleChange() {
//                const t = document.title || '';
//                const s = parseSymbol(t), p = parsePrice(t);
//                if (s && Number.isFinite(p)) sendUpdate(s, p);
//            }
//
//            const titleEl = document.querySelector('title');
//            if (titleEl) {
//                state.observer = new MutationObserver(handleTitleChange);
//                state.observer.observe(titleEl, { childList:true, characterData:true, subtree:true });
//                handleTitleChange();
//            }
//        })();
//    """.trimIndent()
//
//        // -------------------- WEBVIEW CLIENT --------------------
//        webView?.webViewClient = object : WebViewClient() {
//            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
//                super.onPageStarted(view, url, favicon)
//                view?.evaluateJavascript("window.cleanupPriceObserver?.();", null)
//                lastTriggerTimes.clear()
//            }
//
//            override fun onPageFinished(view: WebView?, url: String?) {
//                super.onPageFinished(view, url)
//                view?.postDelayed({
//                    view.evaluateJavascript("document.title") { title ->
//                        val clean = title.trim('"').replace("\\n", "").trim()
//                        val alerts = DataStorage.loadAlerts(this@FloatingBrowserService)
//                        activeAlert = findAlertsForCoin(clean, alerts, AlertStatus.ACTIVE).toMutableSet()
//                        syncActiveAlerts()
//                        startPriceFreezeWatchdog()
//                        Log.d("PriceMonitor", "✅ Page finished: '$clean' → ${activeAlert.size} active alerts.")
//                        // your other injected JS (keeps original order)
//                        view.evaluateJavascript(js, null)
//
//                        // after injecting your main js, schedule the single auto-click (keeps order)
//                        view.postDelayed({
//                            // find which index this webview belongs to (handles -1 safely)
//                            val index = webViews.indexOfFirst { it === view }
//                            val targetValue = when (index) {
//                                0 -> "1"
//                                1 -> "5"
//                                2 -> "15"
//                                3 -> "60"
//                                4 -> "240"
//                                5 -> "1D"
//                                6 -> "1W"
//                                7 -> "1M"
//                                else -> null
//                            }
//                            val targetValuelabel = when (index) {
//                                0 -> "1 minute"
//                                1 -> "5 minutes"
//                                2 -> "15 minutes"
//                                3 -> "1 hour"
//                                4 -> "4 hours"
//                                5 -> "1 day"
//                                6 -> "1 week"
//                                7 -> "1 month"
//                                else -> null
//                            }
//                            if (targetValue != null) {
//                                val jsClick = """
//(function() {
//    try {
//        let attempts = 0;
//        const maxAttempts = 10; // 10 × 500 ms = 5 s total
//        const targetValue = "${targetValue}";
//        const targetLabel = "${targetValuelabel}";
//
//        function tryClick() {
//            const btn = document.querySelector(`button[data-value='${'$'}{targetValue}']`) ||
//                        document.querySelector(`button[aria-label='${'$'}{targetLabel}']`);
//            if (btn) {
//                btn.click();
//                console.log("✅ Clicked timeframe", targetValue);
//                setTimeout(() => {
//                    try {
//                        const resetBtn = document.querySelector(".js-btn-reset");
//                        if (resetBtn) {
//                            const evt2 = new MouseEvent("click", {
//                                bubbles: true,
//                                cancelable: true,
//                                view: window,
//                                isTrusted: true
//                            });
//                            resetBtn.dispatchEvent(evt2);
//                            console.log("✅ Clicked: Reset frame button");
//                        } else {
//                            console.log("⚠️ Reset button not found");
//                        }
//                    } catch (err) {
//                        console.log("Reset button click error:", err);
//                    }
//                }, 2000);
//            } else if (attempts++ < maxAttempts) {
//                setTimeout(tryClick, 500);
//            } else {
//                console.log("⚠️ Timeframe button not found after retries:", targetValue);
//            }
//        }
//        tryClick();
//    } catch(e) {
//        console.log("Auto-click error:", e);
//    }
//
//function autoClickRestore() {
//    try {
//        const buttons = document.querySelectorAll("button");
//        for (const btn of buttons) {
//            const text = (btn.innerText || "").trim();
//            const tooltip = btn.getAttribute("data-overflow-tooltip-text") || "";
//            if (text.includes("Restore connection") || tooltip.includes("Restore connection")) {
//                const evt = new MouseEvent("click", {
//                    bubbles: true,
//                    cancelable: true,
//                    view: window,
//                    isTrusted: true
//                });
//                setTimeout(() => btn.dispatchEvent(evt), 2500);
//                console.log("✅ Dispatched real click: Restore connection");
//                return;
//            }
//        }
//    } catch (e) {
//        console.log("Restore connection watcher error:", e);
//    }
//}
//
//const observer = new MutationObserver(() => autoClickRestore());
//observer.observe(document.body, { childList: true, subtree: true });
//
//})();
//""".trimIndent()
//
//
//                                view.evaluateJavascript(jsClick, null)
//                            } else {
//                                Log.w("PriceMonitor", "WebView not matched to an index (index=$index). Auto-click skipped.")
//                            }
//                        }, 500) // wait a bit after injecting your "js"
//
//                        view.postDelayed({
//                            val jsSetupInstantAlert = """
//                                    (function() {
//                                if (!window._instantAlertWatcherAdded) {
//                                    window._instantAlertWatcherAdded = true;
//
//                                        // --- Click listener ---
//                                        document.body.addEventListener('click', function(e) {
//                                            let target = e.target;
//                                            let text = (target.textContent || "").trim();
//                                            let tag = target.tagName;
//
//                                    console.log("Clicked:", tag, text);
//
//                                    // Example: access class, id, or data attributes directly
//                                    console.log("Clicked Class:", target.className);
//                                    console.log("Clicked ID:", target.id);
//
//                                            if (text.startsWith("Add alert on")) {
//                                                if (window.Android && window.Android.onInjectedSpanClick) {
//                                                    // send both the clicked text and the latest price
//                                                    window.Android.onInjectedSpanClick(text);
//                                                }
//
//                                                setTimeout(() => {
//                                            const closeBtn = document.querySelector(
//                                                "button.overlayBtn-FvtqqqvS"
//                                            );
//                                            if (closeBtn) {
//                                                closeBtn.click();
//                                                console.log("✅ Close button clicked automatically");
//                                            } else {
//                                                console.log("⚠️ Close button not found");
//                                            }
//                                        }, 1000);
//                                            }
//
//                                        }, false);
//                                        }
//
//                                    })();
//                                """.trimIndent()
//                            view.evaluateJavascript(jsSetupInstantAlert,null)
//                        },2000)
//
//                    }
//
//                }, 1000)
//
//            }
//        }
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    private fun showSetAlertOverlay(index: Int) {
//        val overlayContainer = floatingOverlays[index] ?: return
//        val webView = webViews.getOrNull(index)
//
//        // --- Default values ---
//        var symbol = "Loading..."
//        var currentPrice = 0.0
//
//        // --- Overlay layout as card ---
//        val cardLayout = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            setPadding(dpToPx(16))
//            background = GradientDrawable().apply {
//                cornerRadius = dpToPx(16).toFloat()
//                setColor(Color.WHITE)
//                setStroke(2, Color.LTGRAY)
//            }
//            elevation = dpToPx(10).toFloat()
//        }
//
//        // --- Header with title & cut button ---
//        val header = LinearLayout(this).apply {
//            orientation = LinearLayout.HORIZONTAL
//            gravity = Gravity.CENTER_VERTICAL or Gravity.END
//        }
//
//        val titleText = TextView(this).apply {
//            text = "Set Price Alert"
//            textSize = 16f
//            setTextColor(Color.DKGRAY)
//            setTypeface(null, Typeface.BOLD)
//            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
//        }
//
//        val cutBtn = Button(this).apply {
//            text = "✕"
//            textSize = 18f
//            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
//            setOnClickListener { overlayContainer.removeView(cardLayout) }
//        }
//
//        header.addView(titleText)
//        header.addView(cutBtn)
//        cardLayout.addView(header)
//
//        val nameInput = AutoCompleteTextView(this).apply {
//            hint = "Alert Name"
//            setText(symbol)
//            isSingleLine = true
//            imeOptions = EditorInfo.IME_ACTION_NEXT
//            setPadding(dpToPx(12))
//            background = GradientDrawable().apply {
//                cornerRadius = dpToPx(12).toFloat()
//                setStroke(2, Color.LTGRAY)
//                setColor(Color.parseColor("#F5F5F5"))
//            }
//            threshold = 1
//        }
//        nameInput.isFocusableInTouchMode = true
//        nameInput.isFocusable = true
//        nameInput.requestFocus()
//
//        val targetPriceInput = EditText(this).apply {
//            hint = "Target Price"
//            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
//            isSingleLine = true
//            imeOptions = EditorInfo.IME_ACTION_DONE
//            background = GradientDrawable().apply {
//                cornerRadius = dpToPx(12).toFloat()
//                setStroke(2, Color.LTGRAY)
//                setColor(Color.parseColor("#FFF5F5F5"))
//            }
//            setPadding(dpToPx(12))
//        }
//
//        val actionBtn = Button(this).apply {
//            text = "Loading..."
//            isEnabled = false
//            setTextColor(Color.WHITE)
//            background = GradientDrawable().apply {
//                cornerRadius = dpToPx(12).toFloat()
//                setColor(Color.GRAY)
//            }
//            setPadding(dpToPx(12))
//        }
//
//        cardLayout.addView(nameInput.apply { setMargins(dpToPx(0), dpToPx(8), dpToPx(0), dpToPx(8)) })
//        cardLayout.addView(targetPriceInput.apply { setMargins(0, 0, 0, dpToPx(12)) })
//        cardLayout.addView(actionBtn)
//
//        // --- Add card to existing overlay container ---
//        overlayContainer.addView(cardLayout, FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.WRAP_CONTENT,
//            FrameLayout.LayoutParams.WRAP_CONTENT,
//            Gravity.CENTER
//        ))
//
//        // --- Keyboard focus ---
//        cardLayout.postDelayed({
//            targetPriceInput.requestFocus()
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(targetPriceInput, InputMethodManager.SHOW_IMPLICIT)
//        }, 100)
//
//        // --- Helper function ---
//        fun updateSymbolPrice(symbolVal: String, priceVal: Double) {
//            symbol = symbolVal
//            currentPrice = priceVal
//            nameInput.setText(symbol)
//            targetPriceInput.setText(priceVal.toString())
//            actionBtn.text = "Set Alert Automatically"
//            actionBtn.isEnabled = true
//            actionBtn.background = GradientDrawable().apply {
//                cornerRadius = dpToPx(12).toFloat()
//                setColor(Color.parseColor("#4CAF50"))
//            }
//        }
//
//        // --- Load current symbol/price ---
//        val lastSymbol = latestSymbols[index]
//        val lastPrice = latestPrices[index]
//        if (!lastSymbol.isNullOrBlank() && lastPrice != null && lastPrice > 0) {
//            updateSymbolPrice(lastSymbol, lastPrice)
//        } else {
//            webView?.evaluateJavascript("document.title") { title ->
//                val clean = title.trim('"').replace("\\n", "").trim()
//                val matchSymbol = Regex("^[A-Z0-9.:-]+").find(clean)?.value
//                val matchPrice = Regex("[\\d,]+\\.\\d+").find(clean)?.value?.replace(",", "")?.toDoubleOrNull()
//                if (!matchSymbol.isNullOrBlank() && matchPrice != null) {
//                    updateSymbolPrice(matchSymbol, matchPrice)
//                }
//            }
//        }
//
//        // --- Action button logic ---
//        targetPriceInput.addTextChangedListener { text ->
//            val empty = text.isNullOrBlank()
//            actionBtn.text = if (empty) "Cancel" else "Set Alert Automatically"
//            actionBtn.background = GradientDrawable().apply {
//                cornerRadius = dpToPx(12).toFloat()
//                setColor(if (empty) Color.RED else Color.parseColor("#4CAF50"))
//            }
//        }
//
//        actionBtn.setOnClickListener {
//            val targetText = targetPriceInput.text.toString()
//            if (targetText.isBlank()) {
//                overlayContainer.removeView(cardLayout)
//                return@setOnClickListener
//            }
//
//            val target = targetText.toDoubleOrNull()
//            if (target == null) {
//                Toast.makeText(this, "Invalid target price", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            val name = nameInput.text.toString().ifEmpty { symbol }
//            val type = if (currentPrice < target) AlertType.ABOVE else AlertType.BELOW
//
//            val newAlert = PriceAlert(name = name, symbol = symbol, threshold = target, type = type)
//            val allAlerts = DataStorage.loadAlerts(this)
//            DataStorage.saveAlerts(this, allAlerts + newAlert)
//            activeAlerts[webView]?.add(newAlert)
//            Toast.makeText(this, "Alert set: $name $type $target", Toast.LENGTH_SHORT).show()
//            overlayContainer.removeView(cardLayout)
//        }
//
//        // --- Draggable overlay ---
//        var initialX = 0f
//        var initialY = 0f
//        var dX = 0f
//        var dY = 0f
//        cardLayout.setOnTouchListener { v, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    initialX = cardLayout.x
//                    initialY = cardLayout.y
//                    dX = event.rawX - initialX
//                    dY = event.rawY - initialY
//                    true
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    cardLayout.x = event.rawX - dX
//                    cardLayout.y = event.rawY - dY
//                    true
//                }
//                else -> false
//            }
//        }
//    }
//
//    private fun createInstantAlert(index: Int, symbol: String, price: Double,latestPrice: Double) {
//        val webView = webViews.getOrNull(index) ?: run {
//            Log.e("FloatingService", "WebView not found for index $index")
//            return
//        }
//
//            val safeCurrent = if (latestPrice.isFinite() && latestPrice > 0.0) latestPrice else 0.0
//            val type = if (safeCurrent < price) AlertType.ABOVE else AlertType.BELOW
//            val alertName = "AutoAlert-$symbol-$safeCurrent"
//
//            Log.d("FloatingService", "Creating alert: $alertName | $symbol | threshold=$price | type=$type | current=$safeCurrent")
//
//            val newAlert = PriceAlert(name = alertName, symbol = symbol, threshold = price, type = type)
//            val allAlerts = DataStorage.loadAlerts(this)
//            DataStorage.saveAlerts(this, allAlerts + newAlert)
//
//            val set = activeAlerts.getOrPut(webView) { mutableSetOf() }
//            set.add(newAlert)
//
//            Log.i("FloatingService", "Alert set successfully: $symbol at $price ($type)")
//            Toast.makeText(this, "📈 Alert set: $symbol $type $price", Toast.LENGTH_SHORT).show()
//    }
//    // --- Helper to set margins easily ---
//    private fun View.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
//        val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
//        params.setMargins(left, top, right, bottom)
//        layoutParams = params
//    }
//
//
//    /** Header bar with styled buttons (Unchanged) */
//    @SuppressLint("UseKtx")
//    private fun setupHeader(index: Int) {
//        val overlay = floatingOverlays[index]
//
//        // 🔹 Modern rounded header bar
//        val headerBar = LinearLayout(this).apply {
//            orientation = LinearLayout.HORIZONTAL
//            setBackgroundColor("#FFBB86FC".toColorInt())
//            gravity = Gravity.CENTER_VERTICAL
//            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
//            elevation = 8f
//        }
//
//        // 🔸 Define all buttons in one go (text → action)
//        val buttons = listOf(
//            "↺" to { safeReloadWebView(index) },   // Full
//            "🔇" to { if (mediaPlayer?.isPlaying == true) stopAlertSound() }, // 40%
//            "▭" to {
//                val overlay = floatingOverlays.getOrNull(index) ?: return@to
//                if (overlayBoxWindow != null) {
//                    overlay.post {
//                        try {
//                            // Detach from WindowManager
//                            safeDetachView(index)
//
//                            // Find box components
//                            val box = overlayBoxWindow ?: return@post
//                            val scroll = box.findViewById<ScrollView>(android.R.id.content)
//                            val inner = (scroll?.getChildAt(0) as? LinearLayout)
//                                ?: (box.getChildAt(1) as? ScrollView)?.getChildAt(0) as? LinearLayout
//                                ?: return@post
//
//                            // Get or create the wrapper for this overlay
//                            val wrapper = savedBoxWrappers[index] ?: FrameLayout(this).apply {
//                                layoutParams = LinearLayout.LayoutParams(
//                                    LinearLayout.LayoutParams.MATCH_PARENT,
//                                    dpToPx(220)
//                                ).apply { bottomMargin = dpToPx(8) }
//                                background = GradientDrawable().apply {
//                                    setColor(Color.WHITE)
//                                    setStroke(1, Color.LTGRAY)
//                                    cornerRadius = dpToPx(8).toFloat()
//                                }
//                                elevation = dpToPx(3).toFloat()
//                            }.also { savedBoxWrappers[index] = it }
//
//                            // If wrapper not already in the layout, re-add it in correct slot
//                            if (wrapper.parent == null) {
//                                // ensure correct visual order
//                                if (index < inner.childCount)
//                                    inner.addView(wrapper, index)
//                                else
//                                    inner.addView(wrapper)
//                            }
//
//                            // Clear wrapper content and re-add overlay
//                            wrapper.removeAllViews()
//                            overlay.layoutParams = FrameLayout.LayoutParams(
//                                FrameLayout.LayoutParams.MATCH_PARENT,
//                                FrameLayout.LayoutParams.MATCH_PARENT
//                            )
//                            wrapper.addView(overlay)
//
//                            headerBars.getOrNull(index)?.visibility = View.GONE
//                            bubbles.getOrNull(index)?.visibility = View.VISIBLE
//                            webViews.getOrNull(index)?.setInitialScale(100)
//                            (webViews.getOrNull(index)?.layoutParams as? FrameLayout.LayoutParams)?.let {
//                                it.topMargin = 0
//                                webViews.getOrNull(index)?.layoutParams = it
//                            }
//                            releaseOverlayFocus(index)
//
////                    windowManager?.updateViewLayout(floatingOverlays[index], params)
//                    overlay.requestLayout()
//
//                            if (!attachedInBox.contains(index)) attachedInBox.add(index)
//
//                            Log.d("FloatingService", "✅ Overlay $index reattached to side box at slot $index")
//                        } catch (e: Exception) {
//                            Log.e("FloatingService", "Error reattaching overlay $index to box: ${e.message}")
//                        }
//                    }
//                } else {
//                    switchToState(STATE_MINI, index)
//                }
//            },
//            "●" to { overlay.visibility = View.GONE  },      // Minimize
//            "🎯" to { showSetAlertOverlay(index) },       // Set alert
//            "✕" to { removeContainer(index) }               // Close
//        )
//
//        // 🔸 Add all buttons dynamically
//        buttons.forEachIndexed { _, (label, action) ->
//            headerBar.addView(createStyledHeaderButton(label, action as () -> Unit))
//        }
//
//        // 🔸 Layout params for header
//        val lp = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.MATCH_PARENT,
//            dpToPx(HEADER_HEIGHT_DP)
//        ).apply {
//            gravity = Gravity.TOP
//        }
//
//        overlay.addView(headerBar, lp)
//        headerBars.add(headerBar)
//    }
//
//    /** 🔹 Reusable header button creator (compact, modern) */
//    private fun createStyledHeaderButton(label: String, onClick: () -> Unit): Button {
//        return Button(this).apply {
//            text = label
//            setTextColor(Color.WHITE)
//            textSize = 20f
//            setBackgroundColor("#FFBB86FC".toColorInt())
//            setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
//            layoutParams = LinearLayout.LayoutParams(
//                0,
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                1f // evenly spaced
//            ).apply {
//                marginStart = dpToPx(3)
//                marginEnd = dpToPx(3)
//            }
//            setOnClickListener { onClick() }
//        }
//    }
//    /** Bubble icon to toggle overlay (Unchanged) */
//    @SuppressLint("ClickableViewAccessibility")
//    private fun setupBubble(index: Int) {
//        val overlay = floatingOverlays[index]
//
//        val bubble = FrameLayout(this).apply {
//            setBackgroundColor(Color.TRANSPARENT)
//            alpha = 0.9f
//            clipToOutline = true
//            outlineProvider = ViewOutlineProvider.BACKGROUND
//
//            var initialX = 0
//            var initialY = 0
//            var initialTouchX = 0f
//            var initialTouchY = 0f
//            var dragging = false
//
//            // Double-tap detection variables
//            var lastTapTime = 0L
//            val DOUBLE_TAP_TIMEOUT = 300L // milliseconds
//
//            setOnTouchListener { v, event ->
//                val lp = overlay.layoutParams
//
//                // ✅ Detect if overlay is floating or embedded
//                val isFloating = lp is WindowManager.LayoutParams
//
//                when (event.action) {
//                    MotionEvent.ACTION_DOWN -> {
//                        if (isFloating) {
//                            initialX = (lp as WindowManager.LayoutParams).x
//                            initialY = lp.y
//                        }
//                        initialTouchX = event.rawX
//                        initialTouchY = event.rawY
//                        dragging = false
//
//                        // Check for double-tap when in side box
//                        if (!isFloating) {
//                            val currentTime = System.currentTimeMillis()
//                            if (currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT) {
//                                // Double-tap detected - trigger fullscreen
//                                v.performClick()
//                                lastTapTime = 0L // Reset to prevent triple-tap
//                            } else {
//                                lastTapTime = currentTime
//                            }
//                        }
//                        true
//                    }
//
//                    MotionEvent.ACTION_MOVE -> {
//                        val dx = (event.rawX - initialTouchX).toInt()
//                        val dy = (event.rawY - initialTouchY).toInt()
//
//                        if (abs(dx) > CLICK_THRESHOLD || abs(dy) > CLICK_THRESHOLD) {
//                            dragging = true
//                            lastTapTime = 0L // Reset double-tap on drag
//
//                            if (isFloating) {
//                                // ✅ Move real floating overlay
//                                val params = lp as WindowManager.LayoutParams
//                                params.x = initialX + dx
//                                params.y = initialY + dy
//                                try {
//                                    windowManager?.updateViewLayout(overlay, params)
//                                } catch (_: Exception) {}
//                            }
//                        }
//                        true
//                    }
//
//                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                        if (!dragging && isFloating) {
//                            // Single tap when floating - toggle state
//                            v.performClick()
//                        }
//
//                        if (isFloating) {
//                            val params = lp as WindowManager.LayoutParams
//                            val lastX = params.x
//                            val lastY = params.y
//                            DataStorage.savePosition(index, lastX, lastY)
//                            savedPositions[index] = Pair(lastX, lastY)
//                        }
//                        dragging = false
//                        true
//                    }
//
//                    else -> false
//                }
//            }
//
//            setOnClickListener {
//                val isFloating = overlay.layoutParams is WindowManager.LayoutParams
//                if (isFloating) {
//                    // normal toggle when floating
//                    switchToState(
//                        if (currentStates[index] == STATE_MINI) STATE_MAX else STATE_MINI,
//                        index
//                    )
//                } else {
//                    // When in side box - this will be triggered by double-tap
//                    overlay.post {
//                        try {
//                            // 1️⃣ Detach from box
//                            (overlay.parent as? ViewGroup)?.removeView(overlay)
//
//                            // 2️⃣ Restore original or default layout params
//                            val params = savedWindowParams[index] ?: defaultMiniParams(index)
//
//                            // 3️⃣ Attach overlay back to WindowManager
//                            safeAttachToWindow(index, params)
//
//                            // 4️⃣ Expand to full-screen
//                            switchToState(STATE_MAX, index)
//
//                            Log.d("FloatingService", "✅ Double-tap → overlay $index fullscreen")
//
//                            // Optional: Show a toast to indicate double-tap worked
//                            Toast.makeText(this@FloatingBrowserService, "Opening fullscreen", Toast.LENGTH_SHORT).show()
//                        } catch (e: Exception) {
//                            Log.e("FloatingService", "Double-tap expand error: ${e.message}")
//                        }
//                    }
//                }
//            }
//        }
//
//        val lp = FrameLayout.LayoutParams(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.MATCH_PARENT
//        )
//        lp.gravity = Gravity.CENTER
//        overlay.addView(bubble, lp)
//        bubbles.add(bubble)
//    }
//    /** Handle mini / medium / max states for specific container (Unchanged) */
//    private fun switchToState(state: Int, index: Int) {
//        currentStates[index] = state
//        val params = floatingOverlays.getOrNull(index)?.layoutParams as WindowManager.LayoutParams
//
//        when (state) {
//            STATE_MAX -> {
//                bringOverlayToFront(index)
//                requestOverlayFocus(index)
//
//                params.width = WindowManager.LayoutParams.MATCH_PARENT
//                params.height = WindowManager.LayoutParams.MATCH_PARENT
//                params.gravity = Gravity.TOP or Gravity.START
//
//
//                headerBars.getOrNull(index)?.visibility = View.VISIBLE
//                bubbles.getOrNull(index)?.visibility = View.INVISIBLE
//                webViews.getOrNull(index)?.setInitialScale(0)
//                (webViews.getOrNull(index)?.layoutParams as? FrameLayout.LayoutParams)?.let {
//                    it.topMargin = dpToPx(HEADER_HEIGHT_DP)
//                    webViews.getOrNull(index)?.layoutParams = it
//                }
//            }
//
//            STATE_MEDIUM -> {
//                params.width = dpToPx(SIZE_MEDIUM_WIDTH_DP)
//                params.height = dpToPx(SIZE_MEDIUM_HEIGHT_DP)
//                params.gravity = Gravity.START or Gravity.TOP
//                val (x,y) = savedPositions[index] ?: Pair(0,0)
//                params.x = x
//                params.y = y
//
//                headerBars.getOrNull(index)?.visibility = View.VISIBLE
//                bubbles.getOrNull(index)?.visibility = View.INVISIBLE
//                webViews.getOrNull(index)?.setInitialScale(0)
//                (webViews.getOrNull(index)?.layoutParams as? FrameLayout.LayoutParams)?.let {
//                    it.topMargin = dpToPx(HEADER_HEIGHT_DP)
//                    webViews.getOrNull(index)?.layoutParams = it
//                }
//                requestOverlayFocus(index)
//            }
//
//            STATE_MINI -> {
//                params.width = dpToPx(SIZE_MINI_WIDTH_DP)
//                params.height = dpToPx(SIZE_MINI_HEIGHT_DP)
//                params.gravity = Gravity.TOP or Gravity.START
//                val (x,y) = savedPositions[index] ?: DataStorage.loadPosition(index)
//                params.x = x
//                params.y = y
//                headerBars.getOrNull(index)?.visibility = View.GONE
//                bubbles.getOrNull(index)?.visibility = View.VISIBLE
//                webViews.getOrNull(index)?.setInitialScale(100)
//                (webViews.getOrNull(index)?.layoutParams as? FrameLayout.LayoutParams)?.let {
//                    it.topMargin = 0
//                    webViews.getOrNull(index)?.layoutParams = it
//                }
//                releaseOverlayFocus(index)
//            }
//        }
//
//        windowManager?.updateViewLayout(floatingOverlays[index], params)
//        floatingOverlays.getOrNull(index)?.requestLayout()
//    }
//
//    private fun bringOverlayToFront(index: Int) {
//        val overlay = floatingOverlays[index]
//        val params = overlay.layoutParams as? WindowManager.LayoutParams ?: return
//
//        try {
//            windowManager?.removeViewImmediate(overlay)
//        } catch (_: IllegalArgumentException) {
//            // Already detached, nothing to remove.
//        }
//        windowManager?.addView(overlay, params)
//    }
//
//    /** Remove a specific container (Unchanged) */
//    private fun removeContainer(index: Int) {
//        webViews.getOrNull(index)?.destroy()
//        windowManager?.removeView(floatingOverlays[index])
//        stopAlertSound()
//        // Check if all containers are removed
//        val allRemoved = floatingOverlays.all {
//            it.parent == null
//        }
//
//        if (allRemoved) {
//            destroyServiceCompletely()
//            removeControlPanel()
//        }
//    }
//
//
//    //    Minimize btns
//    private var controlPanel: LinearLayout? = null
//    private var controlParams: WindowManager.LayoutParams? = null
//    private var isExpanded = false
//
//    @SuppressLint("ClickableViewAccessibility")
//    private fun setupControlPanel() {
//        if (controlPanel != null) return // prevent duplicates
//
//        // --- Initialize panel ---
//        controlPanel = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            gravity = Gravity.CENTER
//            clipToOutline = false
//            elevation = 12f
//            setPadding(dpToPx(6))
//            background = GradientDrawable().apply {
//                cornerRadius = dpToPx(16).toFloat()
//                setColor(Color.TRANSPARENT)
//            }
//        }
//
//        // --- WindowManager params ---
//        controlParams = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//            PixelFormat.TRANSLUCENT
//        ).apply {
//            gravity = Gravity.START or Gravity.TOP
//            val savedPos = DataStorage.loadPosition(999)
//            x = savedPos.first
//            y = savedPos.second
//        }
//
//        // --- Main gear button (created once) ---
//        val mainButton = createControlButton("⚙️", Color.TRANSPARENT, 24f) {
//            toggleControlPanel()
//        }
//        controlPanel!!.addView(mainButton)
//
//        // --- Dragging logic ---
//        var initialTouchX = 0f
//        var initialTouchY = 0f
//        var dragging = false
//        val CLICK_THRESHOLD = 10
//
//        mainButton.setOnTouchListener { v, event ->
//            val params = controlParams ?: return@setOnTouchListener false
//
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    initialTouchX = event.rawX
//                    initialTouchY = event.rawY
//                    dragging = false
//                    true
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    val dx = (event.rawX - initialTouchX).toInt()
//                    val dy = (event.rawY - initialTouchY).toInt()
//
//                    if (abs(dx) > CLICK_THRESHOLD || abs(dy) > CLICK_THRESHOLD) {
//                        dragging = true
//                        params.x += dx
//                        params.y += dy
//                        windowManager?.updateViewLayout(controlPanel, params)
//
//                        // Save new position
//                        DataStorage.savePosition(999, params.x, params.y)
//
//                        // Update touch for smooth dragging
//                        initialTouchX = event.rawX
//                        initialTouchY = event.rawY
//                    }
//                    true
//                }
//                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                    if (!dragging) v.performClick()
//                    dragging = false
//                    true
//                }
//                else -> false
//            }
//        }
//
//        // --- Add panel to window ---
//        windowManager?.addView(controlPanel, controlParams)
//    }
//
//    @SuppressLint("ClickableViewAccessibility", "UseKtx")
//    private fun toggleControlPanel() {
//        isExpanded = !isExpanded
//        controlPanel?.let { panel ->
//
//            // Remove old overlay buttons (keep mainButton at index 0)
//            if (panel.childCount > 1) {
//                panel.removeViews(1, panel.childCount - 1)
//            }
//
//            if (isExpanded) {
//                val scrollView = ScrollView(this).apply {
//                    layoutParams = LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        dpToPx(250)
//                    )
//                }
//
//                val innerLayout = LinearLayout(this).apply {
//                    orientation = LinearLayout.VERTICAL
//                    layoutParams = LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT
//                    )
//                }
//
//                // ------------------- Overlay Buttons (coin names) -------------------
//                webViews.forEachIndexed { index, webView ->
//                    if (webView.parent != null) {
//                        val url = webView.url ?: ""
//                        val coinName = try {
//                            Uri.parse(url).getQueryParameter("symbol")
//                                ?.substringAfter(":")
//                                ?.uppercase() ?: "Unknown"
//                        } catch (e: Exception) {
//                            "Unknown"
//                        }
//
//                        val btn = createControlButton(coinName, "#FFBB86FC".toColorInt(), 10f) {
//                            restoreOverlay(index)
//                        }
//                        innerLayout.addView(btn)
//                    }
//                }
//
//                // ------------------- SOUND BUTTON -------------------
//                val stopSoundBtn = createControlButton("🔇 Sound", Color.BLACK, 10f) {
//                    if (mediaPlayer?.isPlaying == true) stopAlertSound()
//                }
//
//                // ------------------- STOP OVERLAY BUTTON -------------------
//                val stopOverlayBtn = createControlButton("🛑 Stop Overlays", Color.BLACK, 10f) {
//                    // Remove and clear overlays + webviews
//                    floatingOverlays.forEach { overlay ->
//                        windowManager?.removeViewImmediate(overlay)
//                    }
//                    floatingOverlays.clear()
//                    webViews.clear()
//                    Toast.makeText(this, "All overlays destroyed", Toast.LENGTH_SHORT).show()
//                }
//
//
//                // ------------------- HIDE/SHOW ALL BUTTONS -------------------
//                val hideAllBtn = createControlButton("Hide ●", "#031956".toColorInt(), 10f) {
//                    floatingOverlays.forEach { it.visibility = View.GONE }
//                    Toast.makeText(this, "All overlays hidden", Toast.LENGTH_SHORT).show()
//                }
//
//                val showAllBtn = createControlButton("Show ⛶", "#031956".toColorInt(), 10f) {
//                    floatingOverlays.forEach { it.visibility = View.VISIBLE }
//                    Toast.makeText(this, "All overlays shown", Toast.LENGTH_SHORT).show()
//                }
//
//                // ------------------- OPEN APP BUTTON -------------------
//                val openAppBtn = createControlButton("📱 Open App", "#031956".toColorInt(), 10f) {
//                    try {
//                        val intent = packageManager.getLaunchIntentForPackage("com.example.floatingweb")
//                        if (intent != null) startActivity(intent)
//                        else Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
//                    } catch (e: Exception) {
//                        Toast.makeText(this, "Cannot open app", Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//                // ------------------- CLOSE PANEL BUTTON -------------------
//                val closeBtn = createControlButton("❌", Color.WHITE, 20f) {
//                    toggleControlPanel()
//                }
//                val showBox = createControlButton("📦", "#031956".toColorInt(), 10f) {
//                    toggleOverlayBoxWindow()
//                }
//                // ------------------- ADD BUTTONS IN ORDER -------------------
//                innerLayout.apply {
////                    addView(stopSoundBtn)
////                    addView(startOverlayBtn)
////                    addView(stopOverlayBtn)
//                    addView(showAllBtn)
//                    addView(hideAllBtn)
//                    addView(openAppBtn)
//                    addView(showBox)
//                    addView(closeBtn)
//                }
//
//                scrollView.addView(innerLayout)
//                panel.addView(scrollView)
//            }
//        }
//    }
//
//
//    private fun removeControlPanel() {
//        controlPanel?.let { panel ->
//            try {
//                windowManager?.removeView(panel)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//            controlPanel = null
//            controlParams = null
//        }
//    }
//
//
//
//    // Helper: create round control buttons with consistent style
//    private fun createControlButton(
//        text: String,
//        bgColor: Int,
//        textSizeSp: Float,
//        onClick: () -> Unit,
//    ): Button {
//        return Button(this).apply {
//            this.text = text
//            this.textSize = textSizeSp
//            setTextColor(Color.WHITE)
//            background = GradientDrawable().apply {
//                shape = GradientDrawable.OVAL
//                setColor(bgColor)
//            }
//            layoutParams = LinearLayout.LayoutParams(dpToPx(50), dpToPx(50)).apply {
//                topMargin = dpToPx(6)
//            }
//            setOnClickListener { onClick() }
//            elevation = dpToPx(4).toFloat()
//        }
//    }
//
//    // Fields (keep these in your class)
//    private var overlayBoxWindowParams: WindowManager.LayoutParams? = null
//    private var overlayBoxWindow: FrameLayout? = null
//    private val savedWindowParams = mutableMapOf<Int, WindowManager.LayoutParams>() // already present
//    private val attachedInBox = mutableListOf<Int>()
//    // Keep wrapper references so overlays return to the exact same slot
//    private val savedBoxWrappers = mutableMapOf<Int, FrameLayout>()
//
//    // Safe detach/attach helpers (re-use existing safeDetachView / safeAttachToWindow)
//    private fun safeDetachView(index: Int) {
//        val overlay = floatingOverlays.getOrNull(index) ?: return
//        try { (overlay.parent as? ViewGroup)?.removeView(overlay) } catch (_: Exception) {}
//        try { windowManager?.removeViewImmediate(overlay) } catch (_: Exception) {}
//    }
//
//    private fun safeAttachToWindow(index: Int, params: WindowManager.LayoutParams) {
//        val overlay = floatingOverlays.getOrNull(index) ?: return
//        try { (overlay.parent as? ViewGroup)?.removeView(overlay) } catch (_: Exception) {}
//        try {
//            windowManager?.addView(overlay, params)
//        } catch (ise: IllegalStateException) {
//            try { windowManager?.updateViewLayout(overlay, params) } catch (_: Exception) {}
//        } catch (_: Exception) {}
//    }
//    // Default mini overlay WindowManager params for restoring overlays
//    private fun defaultMiniParams(index: Int): WindowManager.LayoutParams {
//        return WindowManager.LayoutParams(
//            dpToPx(SIZE_MINI_WIDTH_DP),
//            dpToPx(SIZE_MINI_HEIGHT_DP),
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
//                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
//                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
//            PixelFormat.TRANSLUCENT
//        ).apply {
//            gravity = Gravity.TOP or Gravity.START
//            x = dpToPx(40 * (index % 3))   // small offset so multiple overlays don’t stack exactly
//            y = dpToPx(60 * (index % 4))
//        }
//    }
//
//    // Clone existing WindowManager.LayoutParams safely (used before moving overlays)
//    private fun cloneLayoutParams(src: WindowManager.LayoutParams): WindowManager.LayoutParams {
//        return WindowManager.LayoutParams().apply {
//            width = src.width
//            height = src.height
//            x = src.x
//            y = src.y
//            gravity = src.gravity
//            flags = src.flags
//            type = src.type
//            format = src.format
//            windowAnimations = src.windowAnimations
//            token = src.token
//            packageName = src.packageName
//            alpha = src.alpha
//        }
//    }
//
//    // Call toggleOverlayBoxWindow() to open/close the side box window
//    @SuppressLint("InflateParams", "ClickableViewAccessibility")
//    private fun toggleOverlayBoxWindow(forceOpen: Boolean = false) {
//        // If the window exists and we're not forcing open -> close it
//        floatingOverlays.forEach { it.visibility = View.VISIBLE }
//        if (overlayBoxWindow != null && !forceOpen) {
//            // restore overlays back to their saved WindowManager params
//            attachedInBox.forEach { index ->
//                val overlay = floatingOverlays.getOrNull(index) ?: return@forEach
//
//                // Re-enable touch events on the overlay when restoring
//                overlay.setOnTouchListener(null)
//                overlay.isClickable = true
//                webViews.getOrNull(index)?.setOnTouchListener { _, event ->
//                    if (event.action == MotionEvent.ACTION_DOWN) {
//                        requestOverlayFocus(index)
//                    }
//                    false
//                }
//
//                try { (overlay.parent as? ViewGroup)?.removeView(overlay) } catch (_: Exception) {}
//                val params = savedWindowParams[index] ?: defaultMiniParams(index)
//                safeAttachToWindow(index, params)
//            }
//            attachedInBox.clear()
//
//            // clear saved wrappers (we'll rebuild if box opened again)
//            savedBoxWrappers.clear()
//
//            // remove the side box window
//            try {
//                overlayBoxWindow?.let { windowManager?.removeViewImmediate(it) }
//            } catch (_: Exception) {}
//            overlayBoxWindow = null
//            overlayBoxWindowParams = null
//            return
//        }
//
//        // If it's already open and forceOpen -> bring to front (or animate)
//        if (overlayBoxWindow != null && forceOpen) {
//            try { windowManager?.updateViewLayout(overlayBoxWindow, overlayBoxWindowParams) } catch (_: Exception) {}
//            return
//        }
//
//        // --- CREATE the side box window THAT WILL BE TOUCHABLE INSIDE but allow touch-through outside ---
//        val box = FrameLayout(this).apply {
//            setBackgroundColor(Color.BLACK)
//            elevation = dpToPx(12).toFloat()
//        }
//
//        // Prepare WindowManager params for the side box window
//        val params = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//            // Accept touches inside the box; allow outside touches to pass through.
//            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
//                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
//                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
//            PixelFormat.TRANSLUCENT
//        ).apply {
//            gravity = Gravity.END or Gravity.CENTER_VERTICAL
//            x = dpToPx(8) // optional offset
//        }
//
//        // header + close button
//        val header = LinearLayout(this).apply {
//            orientation = LinearLayout.HORIZONTAL
//            setPadding(dpToPx(8))
//            setBackgroundColor("#FFBB86FC".toColorInt())
//            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(48))
//        }
//        val title = TextView(this).apply {
//            text = "Overlays"
//            textSize = 16f
//            setTypeface(null, Typeface.BOLD)
//            setTextColor(Color.WHITE)
//            setPadding(dpToPx(8), 6, dpToPx(8), 6)
//            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
//        }
//        val destroyBtn = Button(this).apply {
//            text = "Destroy 💣"
//            textSize = 16f
//            setTextColor(Color.RED)
//            setBackgroundColor(Color.TRANSPARENT)
//            setBackgroundColor("#FFBB86FC".toColorInt())
//            setPadding(dpToPx(8), 6, dpToPx(8), 6)
//            setOnClickListener {destroyServiceCompletely()}}
//
//        val close = Button(this).apply {
//            text = "Hide All ● "
//            textSize = 16f
//            setTextColor(Color.BLACK)
//            setBackgroundColor(Color.TRANSPARENT)
//            setPadding(dpToPx(8), 6, dpToPx(8), 6)
//            setOnClickListener {
//                if (overlayBoxWindow != null && !forceOpen) {
//                    // Restore overlays as mini windows
//                    attachedInBox.forEach { index ->
//                        val overlay = floatingOverlays.getOrNull(index) ?: return@forEach
//
//                        // Re-enable touch events when closing
//                        overlay.setOnTouchListener(null)
//                        overlay.isClickable = true
//                        webViews.getOrNull(index)?.setOnTouchListener { _, event ->
//                            if (event.action == MotionEvent.ACTION_DOWN) {
//                                requestOverlayFocus(index)
//                            }
//                            false
//                        }
//
//                        try { (overlay.parent as? ViewGroup)?.removeView(overlay) } catch (_: Exception) {}
//                        val miniParams = defaultMiniParams(index)
//                        safeAttachToWindow(index, miniParams)
//                        switchToState(STATE_MINI, index)
//                    }
//                    attachedInBox.clear()
//                    savedBoxWrappers.clear()
//                    floatingOverlays.forEach { it.visibility = View.GONE }
//
//                    try {
//                        overlayBoxWindow?.let { windowManager?.removeViewImmediate(it) }
//                    } catch (_: Exception) {}
//                    overlayBoxWindow = null
//                    overlayBoxWindowParams = null
//                }
//            }
//        }
//        val spacer = Space(this).apply {
//            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
//        }
//
//        header.addView(title);header.addView(destroyBtn) ;header.addView(spacer);header.addView(close);
//        box.addView(header)
//
//        // ScrollView area - Make it properly scrollable
//        val scroll = ScrollView(this).apply {
//            layoutParams = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT
//            ).apply { topMargin = dpToPx(48) }
//            isVerticalScrollBarEnabled = true
//            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
//            // Ensure scroll view can receive touch events
//            isFocusable = true
//            isFocusableInTouchMode = true
//            isClickable = true
//        }
//
//        val inner = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            setPadding(dpToPx(8))
//        }
//
//        // Move live overlays into the side box window
//        floatingOverlays.forEachIndexed { index, overlay ->
//            // save original params if not saved
//            if (!savedWindowParams.containsKey(index)) {
//                (overlay.layoutParams as? WindowManager.LayoutParams)?.let { orig ->
//                    savedWindowParams[index] = cloneLayoutParams(orig)
//                }
//            }
//
//            // detach from any parent/window
//            safeDetachView(index)
//
//            // If wrapper already exists (reopen), reuse it; otherwise create and save it
//            val wrapper = savedBoxWrappers[index] ?: FrameLayout(this).apply {
//                layoutParams = LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    dpToPx(220)
//                ).apply { bottomMargin = dpToPx(8) }
//                background = GradientDrawable().apply {
//                    setColor(Color.WHITE)
//                    setStroke(1, Color.LTGRAY)
//                    cornerRadius = dpToPx(8).toFloat()
//                }
//                elevation = dpToPx(3).toFloat()
//            }.also { savedBoxWrappers[index] = it }
//
//            // IMPORTANT: Disable touch events on overlays when in the box
//            overlay.setOnTouchListener { _, event ->
//                // Only handle bubble clicks, block all other touches
//                false
//            }
//            overlay.isClickable = false
//
//            // Disable WebView touch events when in side box
//            webViews.getOrNull(index)?.apply {
//                setOnTouchListener { _, _ -> false }
//                isVerticalScrollBarEnabled = false
//                isHorizontalScrollBarEnabled = false
//            }
//
//            // make overlay match wrapper
//            overlay.layoutParams = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT
//            )
//
//            // Add a transparent overlay on top to intercept touches and allow scrolling
//            val touchInterceptor = View(this).apply {
//                layoutParams = FrameLayout.LayoutParams(
//                    FrameLayout.LayoutParams.MATCH_PARENT,
//                    FrameLayout.LayoutParams.MATCH_PARENT
//                )
//                setBackgroundColor(Color.TRANSPARENT)
//                isClickable = true
//                setOnTouchListener { _, event ->
//                    // Allow the bubble to be clicked
//                    if (event.action == MotionEvent.ACTION_UP) {
//                        val bubble = bubbles.getOrNull(index)
//                        bubble?.performClick()
//                    }
//                    // Return false to let the ScrollView handle scrolling
//                    false
//                }
//            }
//
//            // ensure wrapper is empty before adding (avoid duplicates)
//            try {
//                (wrapper.parent as? ViewGroup)?.removeView(wrapper)
//                if (wrapper.childCount > 0) wrapper.removeAllViews()
//                wrapper.addView(overlay)
//                wrapper.addView(touchInterceptor) // Add touch interceptor on top
//            } catch (e: Exception) {
//                try {
//                    (overlay.parent as? ViewGroup)?.removeView(overlay)
//                    wrapper.removeAllViews()
//                    wrapper.addView(overlay)
//                    wrapper.addView(touchInterceptor)
//                } catch (_: Exception) {}
//            }
//
//            inner.addView(wrapper)
//            if (!attachedInBox.contains(index)) attachedInBox.add(index)
//        }
//
//        scroll.addView(inner)
//        box.addView(scroll)
//
//        // Add the side box window to WindowManager
//        try {
//            windowManager?.addView(box, params)
//            overlayBoxWindow = box
//            overlayBoxWindowParams = params
//            // slide-in animation
//            box.translationX = dpToPx(340).toFloat()
//            box.animate().translationX(0f).setDuration(200).start()
//        } catch (e: Exception) {
//            Log.e("FloatingService", "Failed to add sideBox window: ${e.message}")
//            // fallback: cleanup any partially attached overlays
//            attachedInBox.forEach { idx ->
//                val overlay = floatingOverlays.getOrNull(idx) ?: return@forEach
//                try { (overlay.parent as? ViewGroup)?.removeView(overlay) } catch (_: Exception) {}
//                safeAttachToWindow(idx, savedWindowParams[idx] ?: defaultMiniParams(idx))
//            }
//            attachedInBox.clear()
//        }
//    }
//
//    private fun restoreOverlay(index: Int) {
//        val overlay = floatingOverlays.getOrNull(index) ?: return
//        switchToState(STATE_MAX,index)
//        floatingOverlays.getOrNull(index)?.visibility = View.VISIBLE
//        if (overlay.parent == null) {
//            val params = WindowManager.LayoutParams(
//                WindowManager.LayoutParams.MATCH_PARENT,
//                WindowManager.LayoutParams.MATCH_PARENT,
//                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                PixelFormat.TRANSLUCENT
//            ).apply {
//                gravity = Gravity.TOP or Gravity.START
//            }
//            windowManager?.addView(overlay, params)
//        }
//    }
//
//    private fun safeReloadWebView(index: Int) {
//        val webView = webViews.getOrNull(index) ?: return
//        if (webView.context == null) return  // make sure context exists
//
//        if (webView.isAttachedToWindow) {
//            // WebView is fully in the view hierarchy → safe to reload immediately
//            webView.reload()
//        } else {
//            // WebView not attached yet (side-box or temporarily detached)
//            // post to its own message queue; executes on UI thread after attachment
//            webView.post {
//                try {
//                    webView.reload()
//                } catch (e: Exception) {
//                    Log.e("FloatingService", "Failed reload for index $index: ${e.message}")
//                }
//            }
//        }
//    }
//
//
//    override fun onDestroy() {
//        super.onDestroy()
//        if (!isDestroying) {
//            destroyServiceCompletely()
//        }
//    }
//
//    private var isDestroying = false
//
//    private fun destroyServiceCompletely() {
//        if (isDestroying) {
//            Log.w("FloatingService", "Already destroying service, skipping duplicate call")
//            return
//        }
//        isDestroying = true
//
//        try {
//            // 1. Cancel any running coroutines first
//            overlayJob?.cancel()
//            overlayJob = null  // Clear reference
//
//            // 2. Stop alert sounds
//            stopAlertSound()
//
//            // 3. Release wake lock
//            try {
//                if (this::wakeLock.isInitialized && wakeLock.isHeld) {
//                    wakeLock.release()
//                }
//            } catch (e: Exception) {
//                Log.e("FloatingService", "Error releasing wake lock: ${e.message}")
//            }
//
//            // 4. Destroy all WebViews (must be done before removing views)
//            webViews.forEach {
//                try {
//                    it.stopLoading()
//                    it.clearHistory()
//                    it.clearCache(true)
//                    it.loadUrl("about:blank")
//                    it.onPause()
//                    it.removeAllViews()
//                    it.destroy()
//                } catch (e: Exception) {
//                    Log.e("FloatingService", "Error destroying WebView: ${e.message}")
//                }
//            }
//            webViews.clear()
//
//            // 5. Remove all floating overlays from WindowManager
//            floatingOverlays.forEach { overlay ->
//                try {
//                    // First try to remove from parent if it's in the box
//                    (overlay.parent as? ViewGroup)?.removeView(overlay)
//                } catch (_: Exception) {}
//
//                try {
//                    // Then remove from WindowManager
//                    windowManager?.removeViewImmediate(overlay)
//                } catch (e: Exception) {
//                    Log.e("FloatingService", "Error removing overlay: ${e.message}")
//                }
//            }
//            floatingOverlays.clear()
//
//            // 6. Remove the side box window if it exists
//            overlayBoxWindow?.let {
//                try {
//                    windowManager?.removeViewImmediate(it)
//                } catch (e: Exception) {
//                    Log.e("FloatingService", "Error removing box window: ${e.message}")
//                }
//            }
//            overlayBoxWindow = null
//            overlayBoxWindowParams = null
//
//            // 7. Remove control panel
//            removeControlPanel()
//
//            // 8. Remove parent overlay
//            parentOverlay?.let {
//                try {
//                    windowManager?.removeViewImmediate(it)
//                } catch (e: Exception) {
//                    Log.e("FloatingService", "Error removing parent overlay: ${e.message}")
//                }
//            }
//            parentOverlay = null
//
//            // 9. Clear all collections
//            headerBars.clear()
//            bubbles.clear()
//            currentStates.clear()
//            savedPositions.clear()
//            latestPrices.clear()
//            latestSymbols.clear()
//            activeAlerts.clear()
//            attachedInBox.clear()
//            savedBoxWrappers.clear()
//            savedWindowParams.clear()
//
//            // 10. Stop foreground and notifications
//            stopForeground(true)
//            NotificationManagerCompat.from(this).cancelAll()
//
//            Log.d("FloatingService", "Service destruction completed successfully")
//            val intent = Intent("com.example.floatingweb.SERVICE_STOPPED").apply {
//                // Ensure only your app receives it
//                setPackage(packageName)
//            }
//            sendBroadcast(intent)
//
//
//            // 11. Finally stop the service - NO RECURSION!
//            stopSelf()  // ✅ Just call stopSelf() directly
//
//
//        } catch (e: Exception) {
//            Log.e("FloatingService", "Error during service destruction: ${e.message}", e)
//            // ✅ NO RECURSIVE CALL HERE - just try to stop the service
//            try {
//                stopForeground(true)
//                stopSelf()
//            } catch (stopError: Exception) {
//                Log.e("FloatingService", "Critical: Could not stop service: ${stopError.message}")
//            }
//        }
//        // ✅ DON'T reset isDestroying - leave it true so it can't be called again
//    }
//    private fun startForegroundService() {
//        val notificationChannelId = "floating_web_channel"
//        val channel = NotificationChannel(
//            notificationChannelId,
//            "Floating Web Service",
//            NotificationManager.IMPORTANCE_LOW
//        )
//        val manager = getSystemService(NotificationManager::class.java)
//        manager.createNotificationChannel(channel)
//
//        val notification = NotificationCompat.Builder(this, "floating_web_channel")
//            .setContentTitle("Floating Browser Running")
//            .setContentText("Tap to manage your overlays")
//            .setSmallIcon(com.example.floatingweb.R.mipmap.ic_launcher)
//            .setOngoing(true)
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .build()
//
//
//        startForeground(1, notification)
//    }
//
//    private fun dpToPx(dp: Int): Int {
//        val density = resources.displayMetrics.density
//        return (dp * density).toInt()
//    }
//    private var mediaPlayer: MediaPlayer? = null
//
//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    @SuppressLint("UnspecifiedImmutableFlag")
//    private fun showPriceAlertNotification(alert: PriceAlert, currentPrice: Double) {
//        // 1️⃣ Stop intent
//        val stopIntent = Intent(this, FloatingBrowserService::class.java).apply {
//            action = "STOP_ALERT"
//            putExtra("alertId", alert.id)
//        }
//        val stopPendingIntent = PendingIntent.getService(
//            this, alert.id.hashCode(), stopIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        // 2️⃣ Notification builder
//        val notification = NotificationCompat.Builder(this, "price_alert_channel")
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentTitle("Price Alert: ${alert.symbol}")
//            .setContentText("${alert.type} ${alert.threshold}, Current Price: $currentPrice")
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setCategory(NotificationCompat.CATEGORY_ALARM)
//            .addAction(R.drawable.ic_launcher_foreground, "Stop Sound", stopPendingIntent)
//            .setAutoCancel(false)
//            .build()
//
//        // 3️⃣ Show notification
//        NotificationManagerCompat.from(this).notify(alert.id.hashCode(), notification)
//
//        // 4️⃣ Play sound
////        playAlertSound()
//    }
//
//    // ---------------------
//// Sound handler
//    private fun playAlertSound() {
//        val prefs = getSharedPreferences("price_alert", Context.MODE_PRIVATE)
//        val uriString = prefs.getString("custom_sound_uri", null)
//        val alarmUri = uriString?.toUri() ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
//        Log.d("sound","running.")
//        try {
//            // Stop old sound
//            mediaPlayer?.stop()
//            mediaPlayer?.release()
//            mediaPlayer = null
//
//            // Create new MediaPlayer
//            mediaPlayer = MediaPlayer().apply {
//                setDataSource(this@FloatingBrowserService, alarmUri)
//                isLooping = true // keeps playing until user stops
//                prepare()
//                start()
//            }
//        } catch (e: Exception) {
//            Log.e("FloatingService", "Error playing alert sound: ${e.message}")
//        }
//    }
//
//    // ---------------------
//// Stop sound
//    fun stopAlertSound() {
//        try {
//            mediaPlayer?.stop()
//            mediaPlayer?.release()
//            mediaPlayer = null
//        } catch (e: Exception) {
//            Log.e("FloatingService", "Error stopping sound: ${e.message}")
//        }
//    }
//
//    // ---------------------
//// Notification channel (call once in onCreate)
//    private fun createNotificationChannel() {
//        val channel = NotificationChannel(
//            "price_alert_channel",
//            "Price Alerts",
//            NotificationManager.IMPORTANCE_HIGH
//        ).apply {
//            description = "Notifications for price alerts"
//            enableLights(true)
//            lightColor = Color.RED
//            enableVibration(true)
//            setSound(null, null) // avoid default sound; handled via MediaPlayer
//        }
//        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
//    }
//
//
//
//}
















//package com.example.floatingweb.services
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.app.Service
//import android.content.Context
//import android.content.Intent
//import android.graphics.Bitmap
//import android.graphics.Color
//import android.graphics.PixelFormat
//import android.graphics.Typeface
//import android.graphics.drawable.GradientDrawable
//import android.media.MediaPlayer
//import android.media.RingtoneManager
//import android.net.Uri
//import android.os.Build
//import android.os.Handler
//import android.os.Looper
//import android.os.PowerManager
//import android.text.InputType
//import android.util.Log
//import android.view.*
//import android.view.inputmethod.EditorInfo
//import android.view.inputmethod.InputMethodManager
//import android.webkit.JavascriptInterface
//import android.webkit.WebView
//import android.webkit.WebViewClient
//import android.widget.*
//import androidx.annotation.RequiresApi
//import androidx.annotation.RequiresPermission
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import androidx.core.graphics.toColorInt
//import androidx.core.view.setPadding
//import kotlin.math.abs
//import androidx.core.net.toUri
//import androidx.core.widget.addTextChangedListener
//import com.example.floatingweb.helpers.AlertStatus
//import com.example.floatingweb.helpers.DataStorage
//import com.example.floatingweb.helpers.AlertType
//import com.example.floatingweb.helpers.PriceAlert
//import com.example.floatingweb.helpers.findAlertsForCoin
//import com.example.floatingweb.helpers.triggeredAndSaveAlert
//import com.example.floatingweb.R
//import org.json.JSONObject
//import androidx.core.view.isVisible
//import java.util.Timer
//import kotlin.concurrent.fixedRateTimer
//
//class FloatingBrowserService : Service() {
//
//    private var windowManager: WindowManager? = null
//    private var parentOverlay: FrameLayout? = null
//
//    var urls = mutableListOf<String>()
//    // Lists to hold multiple child containers
//    private val floatingOverlays = mutableListOf<FrameLayout>()
//    private val webViews = mutableListOf<WebView>()
//    private val headerBars = mutableListOf<LinearLayout>()
//    private val bubbles = mutableListOf<FrameLayout>()
//    private val currentStates = mutableListOf<Int>()
//
//    private val savedPositions = mutableMapOf<Int, Pair<Int, Int>>()
//
//    // Store the latest price per WebView index
//    private val latestPrices = mutableMapOf<Int, Double>()
//
//    // Optional: store the current symbol for reference
//    private val latestSymbols = mutableMapOf<Int, String>()
//
//     val accountsoftabs = mutableMapOf<Int, String>(Pair(0,"user0"),Pair(1,"user1"),Pair(2,"user2"),Pair(3,"user3"),Pair(4,"user4"),Pair(5,"user5"),Pair(6,"user6"),Pair(7,"user7"),Pair(8,"user8"))
//    companion object {
//        lateinit var appContext: Context
//
//        private const val STATE_MAX = 1
//        private const val STATE_MEDIUM = 2
//        private const val STATE_MINI = 3
//
//        private var SIZE_MEDIUM_WIDTH_DP = 300
//        private var SIZE_MEDIUM_HEIGHT_DP = 500
//        private var SIZE_MINI_WIDTH_DP = 130
//        private var SIZE_MINI_HEIGHT_DP = 200
//        private const val HEADER_HEIGHT_DP = 40
//        const val CLICK_THRESHOLD = 10
//    }
//
//    // Updated to store PriceAlert objects
//    private val activeAlerts = mutableMapOf<WebView, MutableSet<PriceAlert>>()
//    // Map to store latest price per WebView
//    private lateinit var wakeLock: PowerManager.WakeLock
//
//    // Helper constant for R.id equivalent in this standalone file context
//
//    override fun onBind(intent: Intent?) = null
//
//    @RequiresApi(Build.VERSION_CODES.P)
//    @SuppressLint("SetJavaScriptEnabled")
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        appContext = this
//        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
//         urls = intent?.getStringArrayListExtra("links") ?: arrayListOf("https://www.google.com")
//        Log.d("links", "$urls")
//
//        val containerSizes = intent?.getBundleExtra("ContainerSizes")
//        val miniSize = containerSizes?.getIntArray("mini") ?: intArrayOf(SIZE_MINI_WIDTH_DP, SIZE_MINI_HEIGHT_DP)
//        val mediumSize = containerSizes?.getIntArray("medium") ?: intArrayOf(SIZE_MEDIUM_WIDTH_DP, SIZE_MEDIUM_HEIGHT_DP)
//
//        SIZE_MEDIUM_WIDTH_DP = mediumSize[0]
//        SIZE_MEDIUM_HEIGHT_DP = mediumSize[1]
//        SIZE_MINI_WIDTH_DP = miniSize[0]
//        SIZE_MINI_HEIGHT_DP = miniSize[1]
//
//        if (floatingOverlays.isEmpty()) {
//            setupParentOverlay()
//            setupControlPanel()
//            urls.forEachIndexed { index, url ->
//                setupFloatingOverlay()
//                setupWebView(url, index)
//                setupHeader(index)
//                setupBubble(index)
//                switchToState(STATE_MINI, index)
//            }
//        }
//
//        createNotificationChannel()
//        val action = intent?.action
//        if (action != null) {
//            intent.let {
//                when (it.action) {
//                    "STOP_ALERT" -> {
////                        val index = it.getIntExtra("index", -1)
////                        if (index >= 0)
//                            stopAlertSound()
//                        NotificationManagerCompat.from(this).cancelAll()
//
//                    }
////                    "MARK_TRIGGERED" -> {
////                        val alertId = it.getStringExtra("alertId")
////                            markAlertTriggered(alertId)
////                    }
//                }
//            }
//        }
//        ensureWakeLock()
////        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
////        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FloatingWeb::AlertLock")
////        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
//        startForegroundService()
//        return START_STICKY
//    }
//
//
//    private fun ensureWakeLock() {
//        if (!this::wakeLock.isInitialized) {
//            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
//            wakeLock = pm.newWakeLock(
//                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
//                "FloatingWeb::PriceWatchLock"
//            )
//        }
//        if (!wakeLock.isHeld) wakeLock.acquire()
//    }
//
//    private fun releaseWakeLock() {
//        if (this::wakeLock.isInitialized && wakeLock.isHeld) {
//            wakeLock.release()
//        }
//    }
//    /** Parent overlay: full-screen, transparent, touch-through */
//    private fun setupParentOverlay() {
//        val params = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
//                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
//            PixelFormat.TRANSLUCENT
//        )
//
//        parentOverlay = FrameLayout(this).apply {
//            setBackgroundColor(Color.TRANSPARENT)
//        }
//
//        windowManager?.addView(parentOverlay, params)
//    }
//
//    /** Floating overlay: touchable, rounded, shadowed container */
//    private fun setupFloatingOverlay() {
//        val params = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
//                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//            PixelFormat.TRANSLUCENT
//        )
//
//        val floatingOverlay = FrameLayout(this).apply {
//            setBackgroundColor(Color.WHITE)
//            elevation = 16f
//            clipChildren = false
//            clipToOutline = false
//        }
//
//        windowManager?.addView(floatingOverlay, params)
//        floatingOverlays.add(floatingOverlay)
//        currentStates.add(STATE_MINI)
//    }
//
//    private fun requestOverlayFocus(index: Int) {
//        val params = floatingOverlays[index].layoutParams as? WindowManager.LayoutParams ?: return
//        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
//        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
//        windowManager?.updateViewLayout(floatingOverlays[index], params)
//    }
//
//    private fun releaseOverlayFocus(index: Int) {
//        val params = floatingOverlays[index].layoutParams as? WindowManager.LayoutParams ?: return
//        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//        windowManager?.updateViewLayout(floatingOverlays[index], params)
//    }
//
//    /** WebView inside floating overlay */
//    @RequiresApi(Build.VERSION_CODES.P)
//    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
//    private fun setupWebView(url: String, index: Int) {
//        val suffix = accountsoftabs[index] ?: "user_default"
//        Log.d("WebViewSetup", "Suffix already set: ${suffix}")
//
//
//        val webView = WebView(this).apply {
//            settings.javaScriptEnabled = true
//            settings.domStorageEnabled = true
//            setBackgroundColor(Color.WHITE)
//
//            setOnTouchListener { _, event ->
//                if (event.action == MotionEvent.ACTION_DOWN) {
//                    requestOverlayFocus(index)
//                }
//                false
//            }
//        }
//
//        // JS interface as inner class
//        webView.addJavascriptInterface(ClickLogger(), "ClickLogger")
//
//        val lp = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.MATCH_PARENT,
//            FrameLayout.LayoutParams.MATCH_PARENT
//        )
//        lp.topMargin = dpToPx(HEADER_HEIGHT_DP)
//        floatingOverlays[index].addView(webView, lp)
//        webViews.add(webView)
//
//
//        webView.loadUrl(url)
//
//        webView.webViewClient = object : WebViewClient() {
//            override fun onPageFinished(view: WebView?, url: String?) {
//                super.onPageFinished(view, url)
//
//                val jsCode = """
//        (function() {
//            // ---- Auto Reload every 7 minutes ----
//            setTimeout(function() {
//                location.reload();
//            }, 10 * 60 * 1000);
//
//            // ---- Create or reuse popup container ----
//            let popupContainer = document.createElement('div');
//            popupContainer.style.position = 'fixed';
//            popupContainer.style.bottom = '20px';
//            popupContainer.style.left = '50%';
//            popupContainer.style.transform = 'translateX(-50%)';
//            popupContainer.style.backgroundColor = 'rgba(0,0,0,0.85)';
//            popupContainer.style.color = '#fff';
//            popupContainer.style.padding = '10px 16px';
//            popupContainer.style.borderRadius = '8px';
//            popupContainer.style.fontFamily = 'sans-serif';
//            popupContainer.style.fontSize = '14px';
//            popupContainer.style.zIndex = 999999;
//            popupContainer.style.display = 'none';
//            document.body.appendChild(popupContainer);
//
//            function showPopup(info) {
//                popupContainer.innerText = info;
//                popupContainer.style.display = 'block';
//                clearTimeout(popupContainer.hideTimer);
//                popupContainer.hideTimer = setTimeout(() => {
//                    popupContainer.style.display = 'none';
//                }, 3000);
//            }
//
//            // ---- Click listener for buttons ----
//            document.addEventListener('click', function(e) {
//                let el = e.target;
//
//                // Trigger only for BUTTON elements
//                if (el.tagName.toLowerCase() === 'button') {
//                    let elementTag = el.tagName;
//                    let elementId = el.id || '';
//                    let elementClass = el.className || '';
//                    let elementText = el.innerText || '';
//                    let info = 'Tag: ' + elementTag +
//                               '\\nID: ' + elementId +
//                               '\\nClass: ' + elementClass +
//                               '\\nText: ' + elementText;
//
//                    showPopup(info);
//
//                    // Call Android interface
//                    if (window.ClickLogger) {
//                        window.ClickLogger.logClick(elementTag, elementId, elementClass, elementText);
//                    }
//                }
//            }, true);
//        })();
//    """.trimIndent()
//
//                view?.evaluateJavascript(jsCode, null)
//            }
//        }
//        setupPriceMonitor(index)
//
//    }
//
//    // JS interface inner class
//    inner class ClickLogger {
//        @JavascriptInterface
//        fun logClick(tagName: String, id: String, className: String, innerText: String) {
////            this@FloatingBrowserService.runOnUiThread {
////                Log.d("ClickInfo", "Tag: $tagName, ID: $id, Class: $className, Text: $innerText")
////            }
//        }
//    }
//
//
//
//    /** Sets up the JS price monitoring for a WebView */
//    @SuppressLint("SetJavaScriptEnabled")
//    private fun setupPriceMonitor(index: Int) {
//        val webView = webViews[index].apply {
//            settings.javaScriptEnabled = true
//            settings.domStorageEnabled = true
//        }
//        ensureWakeLock()
//        val STALE_TIMEOUT_MS = 60_000L   // 60s of inactivity = reload
//        val CHECK_INTERVAL_MS = 10_000L  // every 10s check
//
//        var lastPriceUpdateTime = System.currentTimeMillis()
//        var lastJsPingTime = System.currentTimeMillis()
//        var staleCheckHandler: Handler? = null
//        var staleCheckRunnable: Runnable? = null
//
//        var activeAlert = mutableSetOf<PriceAlert>()
//        fun syncActiveAlerts() { activeAlerts[webView] = activeAlert }
//
//        val lastTriggerTimes = mutableMapOf<String, Long>()
//
//        // 🟩 Initial load of alerts for the page title
//        Handler(Looper.getMainLooper()).post {
//            webView.evaluateJavascript("document.title") { title ->
//                val clean = title.trim('"').replace("\\n", "").trim()
//                val alerts = DataStorage.loadAlerts(this@FloatingBrowserService)
//                activeAlert = findAlertsForCoin(clean, alerts, AlertStatus.ACTIVE).toMutableSet()
//                syncActiveAlerts()
//                if (activeAlert.isNotEmpty()) ensureWakeLock()
//                Log.d("PriceMonitor", "📋 Loaded ${activeAlert.size} alerts for '$clean'")
//            }
//        }
//
//        // -------------------- JS INTERFACE --------------------
//        webView.addJavascriptInterface(object {
//            @SuppressLint("MissingPermission")
//            @JavascriptInterface
//            fun checkPrice(jsonString: String) {
//                try {
//                    val obj = JSONObject(jsonString)
//                    val symbol = obj.getString("symbol")
//                    val price = obj.getDouble("price")
//
//                    if (activeAlert.isEmpty()) return
//
//                    latestPrices[index] = price
//                    latestSymbols[index] = symbol
//                    lastPriceUpdateTime = System.currentTimeMillis()
//                    Log.d("PriceMonitor", "✅ $symbol = $$price")
//
//                    val now = System.currentTimeMillis()
//                    val triggeredNow = mutableSetOf<PriceAlert>()
//
//                    activeAlert.forEach { alert ->
//                        if (alert.status != AlertStatus.ACTIVE) return@forEach
//                        val lastTime = lastTriggerTimes[alert.id] ?: 0L
//                        if (now - lastTime < 5000) return@forEach
//
//                        val triggered = when (alert.type) {
//                            AlertType.ABOVE -> price > alert.threshold
//                            AlertType.BELOW -> price < alert.threshold
//                        }
//                        if (triggered) {
//                            triggeredNow += alert
//                            lastTriggerTimes[alert.id] = now
//                        }
//                    }
//
//                    if (triggeredNow.isNotEmpty()) {
//                        triggeredNow.forEach { alert ->
//                            try {
//                                Log.d("PriceMonitor", "🎯 ${alert.symbol} ${alert.type} ${alert.threshold} @ $$price")
//                                triggeredAndSaveAlert(alert, price, this@FloatingBrowserService)
//                                playAlertSound()
//                                showPriceAlertNotification(alert, price)
//                                Handler(Looper.getMainLooper()).post {
//                                    floatingOverlays[index].visibility = View.VISIBLE
//                                    if (currentStates[index] != STATE_MAX) switchToState(STATE_MAX, index)
//                                }
//                            } catch (e: Exception) {
//                                Log.e("PriceMonitor", "Error alert trigger: ${e.message}", e)
//                            }
//                        }
//                        activeAlert.removeAll(triggeredNow)
//                        syncActiveAlerts()
//                    }
//
//                    lastTriggerTimes.entries.removeIf { (_, t) -> now - t > 30_000 }
//
//                    // 🛑 No active alerts left → stop JS + wake lock
//                    if (activeAlert.isEmpty()) {
//                        Log.d("PriceMonitor", "🛑 All alerts done — stopping monitor")
//                        Handler(Looper.getMainLooper()).post {
//                            try {
//                                webView.evaluateJavascript("window.cleanupPriceObserver?.();", null)
//                                staleCheckHandler?.removeCallbacksAndMessages(null)
//                            } catch (_: Exception) {}
//                        }
//                        if (wakeLock.isHeld) releaseWakeLock()
//                    }
//
//                } catch (e: Exception) {
//                    Log.e("PriceMonitor", "Invalid JSON: $jsonString", e)
//                }
//            }
//
//            @JavascriptInterface
//            fun markActive() {
//                lastJsPingTime = System.currentTimeMillis()
//            }
//
//            @JavascriptInterface
//            fun onSymbolChange(newSymbol: String) {
//                val currentSymbol = latestSymbols[index]
//                if (newSymbol == currentSymbol) return
//                Handler(Looper.getMainLooper()).post {
//                    val alerts = DataStorage.loadAlerts(this@FloatingBrowserService)
//                    activeAlert = findAlertsForCoin(newSymbol, alerts, AlertStatus.ACTIVE).toMutableSet()
//                    syncActiveAlerts()
//                    lastTriggerTimes.clear()
//                    latestSymbols[index] = newSymbol
//                    Log.d("PriceMonitor", "🔁 Symbol changed to $newSymbol (${activeAlert.size} alerts)")
//                }
//            }
//        }, "Android")
//
//        // -------------------- SMART JS CODE --------------------
//        val js = """
//(function() {
//  const STALE_MS = 30000;
//  const CHECK_MS = 15000;
//  const RELOAD_COOLDOWN_MS = 40000;
//
//  if (window.cleanupPriceObserver) { try { window.cleanupPriceObserver(); } catch(e) {} }
//
//  const state = { observer:null, lastSymbol:"", lastPrice:NaN, lastChangeAt:Date.now(), lastReloadAt:0, intervalId:null };
//
//  function cleanup() {
//    try { state.observer?.disconnect(); } catch(e) {}
//    if (state.intervalId) { clearInterval(state.intervalId); state.intervalId=null; }
//  }
//  window.cleanupPriceObserver = cleanup;
//
//  function parseSymbol(t){ const m=t.match(/^[A-Z0-9.:-]+/); return m?m[0].trim():""; }
//  function parsePrice(t){ const m=t.match(/[\d,]+\.\d+/); return m?parseFloat(m[0].replace(/,/g,'')):NaN; }
//
//  function sendUpdate(s,p){
//    if(!s||!Number.isFinite(p))return;
//    try{Android.markActive();}catch(e){}
//    const priceChanged = !Number.isFinite(state.lastPrice) || Math.abs(p - state.lastPrice) > 0.00001 * p;
//    const symbolChanged = s !== state.lastSymbol;
//    if(priceChanged||symbolChanged) state.lastChangeAt = Date.now();
//    if(symbolChanged) try{Android.onSymbolChange(s);}catch(e){}
//    state.lastSymbol=s; state.lastPrice=p;
//    try{Android.checkPrice(JSON.stringify({symbol:s,price:p}));}catch(e){}
//  }
//
//  function handleTitleChange(){
//    const t=document.title||"", s=parseSymbol(t), p=parsePrice(t);
//    if(s&&Number.isFinite(p))sendUpdate(s,p);
//  }
//
//  const titleEl=document.querySelector('title');
//  if(titleEl){ state.observer=new MutationObserver(handleTitleChange); state.observer.observe(titleEl,{childList:true,characterData:true,subtree:true}); handleTitleChange(); }
//
//  state.intervalId=setInterval(()=>{
//    try{Android.markActive();}catch(e){}
//    const now=Date.now(), staleFor=now-state.lastChangeAt, sinceReload=now-state.lastReloadAt;
//    if(staleFor>=STALE_MS && sinceReload>=RELOAD_COOLDOWN_MS){
//      console.log("📄 JS reload due to stale data ("+Math.round(staleFor/1000)+"s)");
//      state.lastReloadAt=now;
//      state.lastChangeAt=now;
//      try{location.reload();}catch(e){location.href=location.href;}
//    }
//  },CHECK_MS);
//})();
//""".trimIndent()
//
//
//        // -------------------- WEBVIEW CLIENT --------------------
//        webView.webViewClient = object : WebViewClient() {
//            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
//                super.onPageStarted(view, url, favicon)
//                staleCheckHandler?.removeCallbacksAndMessages(null)
//                view?.evaluateJavascript("window.cleanupPriceObserver?.();", null)
//                lastTriggerTimes.clear()
//            }
//
//            override fun onPageFinished(view: WebView?, url: String?) {
//                super.onPageFinished(view, url)
//                staleCheckHandler?.removeCallbacksAndMessages(null)
//
//                staleCheckHandler = Handler(Looper.getMainLooper())
//                // --- Hybrid watchdog for stuck price updates ---
//                staleCheckRunnable = object : Runnable {
//                    override fun run() {
//                        try {
//                            if (activeAlerts.isNotEmpty()) {
//                                // 🧠 Only reload if WebView is still alive and attached
//                                if (System.currentTimeMillis() - lastJsPingTime > 30_000) {
//                                    if (webView.isAttachedToWindow && webView.handler != null) {
//                                        Log.d("Watchdog", "🔁 JS inactive >90s — forcing reload")
//                                        ensureWakeLock()
//                                        webView.reload()
//                                    }
//                                } else {
//                                    Log.d("Watchdog", "✅ JS active, skipping reload")
//                                }
//
//                                staleCheckHandler?.postDelayed(this, 60000L) // repeat every 60s
//                            } else {
//                                Log.d("Watchdog", "🛑 No alerts left — stopping watchdog")
//                                staleCheckHandler?.removeCallbacksAndMessages(null)
//                                staleCheckHandler = null
//                                staleCheckRunnable = null
//
//                                // Stop JS monitoring and release wakelock
//                                webView.evaluateJavascript("window.cleanupPriceObserver?.();", null)
//                                releaseWakeLock()
//                            }
//                        } catch (e: Exception) {
//                            Log.e("Watchdog", "❌ Error in reload loop: ${e.message}")
//                        }
//                    }
//                }
//                staleCheckHandler?.postDelayed(staleCheckRunnable!!, 60000L)
//
//
//                view?.postDelayed({
//                    view.evaluateJavascript(js, null)
//                    Log.d("PriceMonitor", "💉 JS injected (hybrid mode)")
//                }, 1500)
//            }
//        }
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    private fun showSetAlertOverlay(index: Int) {
//        val overlay = floatingOverlays.getOrNull(index) ?: return
//        val webView = webViews.getOrNull(index)
//
//        // --- Default values ---
//        var symbol = "Loading..."
//        var currentPrice = 0.0
//
//        // --- Overlay layout as card ---
//        val cardLayout = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            setPadding(dpToPx(16))
//            background = GradientDrawable().apply {
//                cornerRadius = dpToPx(16).toFloat()
//                setColor(Color.WHITE)
//                setStroke(2, Color.LTGRAY)
////                setShadowLayer(8f, 0f, 4f, Color.LTGRAY)
//            }
//            elevation = dpToPx(10).toFloat()
//        }
//
//        // --- Header with title & cut button ---
//        val header = LinearLayout(this).apply {
//            orientation = LinearLayout.HORIZONTAL
//            gravity = Gravity.CENTER_VERTICAL or Gravity.END
//        }
//
//        val titleText = TextView(this).apply {
//            text = "Set Price Alert"
//            textSize = 16f
//            setTextColor(Color.DKGRAY)
//            setTypeface(null, Typeface.BOLD)
//            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
//        }
//
//        val cutBtn = Button(this).apply {
//            text = "✕"
//            textSize = 18f
////            setTextColor(Color.WHITE)
////            setBackgroundColor(Color.RED)
//            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
////            background = GradientDrawable().apply {
////                cornerRadius = dpToPx(12).toFloat()
////                setColor(Color.RED)
////            }
//            setOnClickListener { windowManager?.removeView(cardLayout) }
//        }
//
//        header.addView(titleText)
//        header.addView(cutBtn)
//        cardLayout.addView(header)
//
//        // --- Name input ---
//        // List of recommended alerts
//        val alertRecommendations = listOf("D-1-OB",
//            "D-1-PB","D-1-RB","Fractal Point -D-1",
//            "Fractal Point -H-1","Fractal Point -H-4",
//            "Fractal Point -M-5","Fractal Point -M-15",
//            "Fractal Point -MT-1","Fractal Point -W-1",
//            "H-1-OB","H-1-PB",    "H-1-RB",
//            "H-4-OB","H-4-PB",
//            "H-4-RB","M-5-OB",
//            "M-5-PB","M-5-RB",
//            "M-15-OB","M-15-PB",
//            "M-15-RB","FVG-D-1",
//            "FVG-H-1","FVG-H-4",
//            "FVG-M-5","FVG-M-15",
//            "FVG-M-30","FVG-MT-1",
//            "FVG-MT-3","FVG-W-1",
//            "M-15-RB","MT-1-OB",
//            "MT-1-PB","MT-1-RB",
//            "W-1-OB","W-1-PB",
//            "W-1-RB")
//
//
//
//// AutoCompleteTextView
//        val nameInput = AutoCompleteTextView(this).apply {
//            hint = "Alert Name"
//            setText(symbol)
//            isSingleLine = true
//            imeOptions = EditorInfo.IME_ACTION_NEXT
//            setPadding(dpToPx(12))
//            background = GradientDrawable().apply {
//                cornerRadius = dpToPx(12).toFloat()
//                setStroke(2, Color.LTGRAY)
//                setColor(Color.parseColor("#F5F5F5"))
//            }
//
//            // Adapter for autocomplete suggestions
//            val adapter = ArrayAdapter(
//                this@FloatingBrowserService,
//                android.R.layout.simple_dropdown_item_1line,
//                alertRecommendations
//            )
//            setAdapter(adapter)
//
//            // Show dropdown when typing
//            threshold = 1  // start showing suggestions after 1 character
//        }
//
//
//        // --- Target price input ---
//        val targetPriceInput = EditText(this).apply {
//            hint = "Target Price"
//            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
//            isSingleLine = true
//            imeOptions = EditorInfo.IME_ACTION_DONE
//            background = GradientDrawable().apply {
//                cornerRadius = dpToPx(12).toFloat()
//                setStroke(2, Color.LTGRAY)
//                setColor(Color.parseColor("#FFF5F5F5"))
//            }
//            setPadding(dpToPx(12))
//        }
//
//        // --- Action button ---
//        val actionBtn = Button(this).apply {
//            text = "Loading..."
//            isEnabled = false
//            setTextColor(Color.WHITE)
//            setBackgroundColor(Color.GRAY)
//            background = GradientDrawable().apply {
//                cornerRadius = dpToPx(12).toFloat()
//                setColor(Color.GRAY)
//            }
//            setPadding(dpToPx(12))
//        }
//
//        cardLayout.addView(nameInput.apply { setMargins(dpToPx(0), dpToPx(8), dpToPx(0), dpToPx(8)) })
//        cardLayout.addView(targetPriceInput.apply { setMargins(0, 0, 0, dpToPx(12)) })
//        cardLayout.addView(actionBtn)
//
//        // --- Window params ---
//        val params = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
//            PixelFormat.TRANSLUCENT
//        ).apply { gravity = Gravity.CENTER }
//
//        windowManager?.addView(cardLayout, params)
//
//        // --- Keyboard focus ---
//        cardLayout.postDelayed({
//            targetPriceInput.requestFocus()
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.showSoftInput(targetPriceInput, InputMethodManager.SHOW_IMPLICIT)
//        }, 100)
//
//        // --- Helper: update symbol/price ---
//        fun updateSymbolPrice(symbolVal: String, priceVal: Double) {
//            symbol = symbolVal
//            currentPrice = priceVal
//            nameInput.setText(symbol)
//            targetPriceInput.setText(priceVal.toString()) // Auto-fill target input
//            actionBtn.text = "Set Alert Automatically"
//            actionBtn.isEnabled = true
//            actionBtn.setBackgroundColor(Color.parseColor("#4CAF50"))
//        }
//
//        // --- Load current symbol/price ---
//        val lastSymbol = latestSymbols[index]
//        val lastPrice = latestPrices[index]
//        if (!lastSymbol.isNullOrBlank() && lastPrice != null && lastPrice > 0) {
//            updateSymbolPrice(lastSymbol, lastPrice)
//        } else {
//            webView?.evaluateJavascript("document.title") { title ->
//                val clean = title.trim('"').replace("\\n", "").trim()
//                val matchSymbol = Regex("^[A-Z0-9.:-]+").find(clean)?.value
//                val matchPrice = Regex("[\\d,]+\\.\\d+").find(clean)?.value?.replace(",", "")?.toDoubleOrNull()
//                if (!matchSymbol.isNullOrBlank() && matchPrice != null) {
//                    updateSymbolPrice(matchSymbol, matchPrice)
//                }
//            }
//        }
//
//        // --- Toggle Cancel when target empty ---
//        targetPriceInput.addTextChangedListener { text ->
//            val empty = text.isNullOrBlank()
//            actionBtn.text = if (empty) "Cancel" else "Set Alert Automatically"
//            actionBtn.setBackgroundColor(if (empty) Color.RED else Color.parseColor("#4CAF50"))
//        }
//
//        // --- Button click ---
//        actionBtn.setOnClickListener {
//            val targetText = targetPriceInput.text.toString()
//            if (targetText.isBlank()) {
//                windowManager?.removeView(cardLayout)
//                return@setOnClickListener
//            }
//
//            val target = targetText.toDoubleOrNull()
//            if (target == null) {
//                Toast.makeText(this, "Invalid target price", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            val name = nameInput.text.toString().ifEmpty { symbol }
//            val type = if (currentPrice < target) AlertType.ABOVE else AlertType.BELOW
//
//            val newAlert = PriceAlert(name = name, symbol = symbol, threshold = target, type = type)
//            val allAlerts = DataStorage.loadAlerts(this)
//            DataStorage.saveAlerts(this, allAlerts + newAlert)
//            activeAlerts[webView]?.add(newAlert)
//            Toast.makeText(this, "Alert set: $name $type $target", Toast.LENGTH_SHORT).show()
//            windowManager?.removeView(cardLayout)
//        }
//
//        // --- Draggable overlay ---
//        var initialX = 0
//        var initialY = 0
//        var initialTouchX = 0f
//        var initialTouchY = 0f
//        cardLayout.setOnTouchListener { _, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    initialX = params.x
//                    initialY = params.y
//                    initialTouchX = event.rawX
//                    initialTouchY = event.rawY
//                    true
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    params.x = initialX + (event.rawX - initialTouchX).toInt()
//                    params.y = initialY + (event.rawY - initialTouchY).toInt()
//                    windowManager?.updateViewLayout(cardLayout, params)
//                    true
//                }
//                else -> false
//            }
//        }
//    }
//
//    // --- Helper to set margins easily ---
//    private fun View.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
//        val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
//        params.setMargins(left, top, right, bottom)
//        layoutParams = params
//    }
//
//
//    /** Header bar with styled buttons (Unchanged) */
//    @SuppressLint("UseKtx")
//    private fun setupHeader(index: Int) {
//        val overlay = floatingOverlays[index]
//
//        // 🔹 Modern rounded header bar
//        val headerBar = LinearLayout(this).apply {
//            orientation = LinearLayout.HORIZONTAL
//            setBackgroundColor("#FFBB86FC".toColorInt())
//            gravity = Gravity.CENTER_VERTICAL
//            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
//            elevation = 8f
//        }
//
//        // 🔸 Define all buttons in one go (text → action)
//        val buttons = listOf(
//            "↺" to { webViews[index].reload() },   // Full
//            "🔇" to { if (mediaPlayer?.isPlaying == true) stopAlertSound() }, // 40%
//            "▭" to { switchToState(STATE_MINI, index) },   // Mini
//            "●" to { overlay.visibility = View.GONE  },      // Minimize
//            "🎯" to { showSetAlertOverlay(index) },       // Set alert
//            "✕" to { removeContainer(index) }               // Close
//        )
//
//        // 🔸 Add all buttons dynamically
//        buttons.forEachIndexed { _, (label, action) ->
//            headerBar.addView(createStyledHeaderButton(label, action))
//        }
//
//        // 🔸 Layout params for header
//        val lp = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.MATCH_PARENT,
//            dpToPx(HEADER_HEIGHT_DP)
//        ).apply {
//            gravity = Gravity.TOP
//        }
//
//        overlay.addView(headerBar, lp)
//        headerBars.add(headerBar)
//    }
//
//    /** 🔹 Reusable header button creator (compact, modern) */
//    private fun createStyledHeaderButton(label: String, onClick: () -> Unit): Button {
//        return Button(this).apply {
//            text = label
//            setTextColor(Color.WHITE)
//            textSize = 20f
//            setBackgroundColor("#FFBB86FC".toColorInt())
//            setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
//            layoutParams = LinearLayout.LayoutParams(
//                0,
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                1f // evenly spaced
//            ).apply {
//                marginStart = dpToPx(3)
//                marginEnd = dpToPx(3)
//            }
//            setOnClickListener { onClick() }
//        }
//    }
//    /** Bubble icon to toggle overlay (Unchanged) */
//    @SuppressLint("ClickableViewAccessibility")
//    private fun setupBubble(index: Int) {
//        val bubble = FrameLayout(this).apply {
//            setBackgroundColor(Color.TRANSPARENT)
//            alpha = 0.9f
//            clipToOutline = true
//            outlineProvider = ViewOutlineProvider.BACKGROUND
//
//            var initialX = 0
//            var initialY = 0
//            var initialTouchX = 0f
//            var initialTouchY = 0f
//            var dragging = false
//
//            setOnTouchListener { v, event ->
//                val params = floatingOverlays[index].layoutParams as WindowManager.LayoutParams
//
//                when (event.action) {
//                    MotionEvent.ACTION_DOWN -> {
//                        initialX = params.x
//                        initialY = params.y
//                        initialTouchX = event.rawX
//                        initialTouchY = event.rawY
//                        dragging = false
//                        false
//                    }
//
//                    MotionEvent.ACTION_MOVE -> {
//                        val dx = (event.rawX - initialTouchX).toInt()
//                        val dy = (event.rawY - initialTouchY).toInt()
//
//                        if (abs(dx) > CLICK_THRESHOLD || abs(dy) > CLICK_THRESHOLD) {
//                            dragging = true
//                            params.x = initialX + dx
//                            params.y = initialY + dy
//                            windowManager?.updateViewLayout(floatingOverlays[index], params)
//                        }
//                        true
//                    }
//
//                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                        if (!dragging) {
//                            v.performClick()
//                        }
//                        val lastX = params.x
//                        val lastY = params.y
//                        DataStorage.savePosition(index,lastX,lastY)
//                        savedPositions[index] = Pair(lastX, lastY)
//                        dragging = false
//                        true
//                    }
//
//                    else -> false
//                }
//            }
//
//            setOnClickListener {
//                switchToState(
//                    if (currentStates[index] == STATE_MINI) STATE_MAX else STATE_MINI,
//                    index
//                )
//            }
//        }
//
//        val lp = FrameLayout.LayoutParams(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.MATCH_PARENT
//        )
//        lp.gravity = Gravity.CENTER
//        floatingOverlays[index].addView(bubble, lp)
//        bubbles.add(bubble)
//    }
//
//    /** Handle mini / medium / max states for specific container (Unchanged) */
//    private fun switchToState(state: Int, index: Int) {
//        currentStates[index] = state
//        val params = floatingOverlays[index].layoutParams as WindowManager.LayoutParams
//
//        when (state) {
//            STATE_MAX -> {
//                bringOverlayToFront(index)
//                requestOverlayFocus(index)
//
//                params.width = WindowManager.LayoutParams.MATCH_PARENT
//                params.height = WindowManager.LayoutParams.MATCH_PARENT
//                params.gravity = Gravity.TOP or Gravity.START
//
//
//                headerBars[index].visibility = View.VISIBLE
//                bubbles[index].visibility = View.INVISIBLE
//                webViews[index].setInitialScale(0)
//                (webViews[index].layoutParams as? FrameLayout.LayoutParams)?.let {
//                    it.topMargin = dpToPx(HEADER_HEIGHT_DP)
//                    webViews[index].layoutParams = it
//                }
//            }
//
//            STATE_MEDIUM -> {
//                params.width = dpToPx(SIZE_MEDIUM_WIDTH_DP)
//                params.height = dpToPx(SIZE_MEDIUM_HEIGHT_DP)
//                params.gravity = Gravity.START or Gravity.TOP
//                val (x,y) = savedPositions[index] ?: Pair(0,0)
//                params.x = x
//                params.y = y
//
//                headerBars[index].visibility = View.VISIBLE
//                bubbles[index].visibility = View.INVISIBLE
//                webViews[index].setInitialScale(0)
//                (webViews[index].layoutParams as? FrameLayout.LayoutParams)?.let {
//                    it.topMargin = dpToPx(HEADER_HEIGHT_DP)
//                    webViews[index].layoutParams = it
//                }
//                requestOverlayFocus(index)
//            }
//
//            STATE_MINI -> {
//                params.width = dpToPx(SIZE_MINI_WIDTH_DP)
//                params.height = dpToPx(SIZE_MINI_HEIGHT_DP)
//                params.gravity = Gravity.TOP or Gravity.START
//                val (x,y) = savedPositions[index] ?: DataStorage.loadPosition(index)
//                params.x = x
//                params.y = y
//                headerBars[index].visibility = View.GONE
//                bubbles[index].visibility = View.VISIBLE
//                webViews[index].setInitialScale(100)
//                (webViews[index].layoutParams as? FrameLayout.LayoutParams)?.let {
//                    it.topMargin = 0
//                    webViews[index].layoutParams = it
//                }
//                releaseOverlayFocus(index)
//            }
//        }
//
//        windowManager?.updateViewLayout(floatingOverlays[index], params)
//        floatingOverlays[index].requestLayout()
//    }
//
//    private fun bringOverlayToFront(index: Int) {
//        val overlay = floatingOverlays[index]
//        val params = overlay.layoutParams as? WindowManager.LayoutParams ?: return
//
//        try {
//            windowManager?.removeViewImmediate(overlay)
//        } catch (_: IllegalArgumentException) {
//            // Already detached, nothing to remove.
//        }
//        windowManager?.addView(overlay, params)
//    }
//
//    /** Remove a specific container (Unchanged) */
//    private fun removeContainer(index: Int) {
//        webViews[index].destroy()
//        windowManager?.removeView(floatingOverlays[index])
//        stopAlertSound()
//        // Check if all containers are removed
//        val allRemoved = floatingOverlays.all {
//            it.parent == null
//        }
//
//        if (allRemoved) {
//            onDestroy()
//            removeControlPanel()
//        }
//    }
//
//
////    Minimize btns
//    private var controlPanel: LinearLayout? = null
//    private var controlParams: WindowManager.LayoutParams? = null
//    private var isExpanded = false
//
//    @SuppressLint("ClickableViewAccessibility")
//    private fun setupControlPanel() {
//        if (controlPanel != null) return // prevent duplicates
//
//        // --- Initialize panel ---
//        controlPanel = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            gravity = Gravity.CENTER
//            clipToOutline = false
//            elevation = 12f
//            setPadding(dpToPx(6))
//            background = GradientDrawable().apply {
//                cornerRadius = dpToPx(16).toFloat()
//                setColor(Color.TRANSPARENT)
//            }
//        }
//
//        // --- WindowManager params ---
//        controlParams = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//            PixelFormat.TRANSLUCENT
//        ).apply {
//            gravity = Gravity.START or Gravity.TOP
//            val savedPos = DataStorage.loadPosition(999)
//            x = savedPos.first
//            y = savedPos.second
//        }
//
//        // --- Main gear button (created once) ---
//        val mainButton = createControlButton("⚙️", Color.TRANSPARENT, 24f) {
//            toggleControlPanel()
//        }
//        controlPanel!!.addView(mainButton)
//
//        // --- Dragging logic ---
//        var initialTouchX = 0f
//        var initialTouchY = 0f
//        var dragging = false
//        val CLICK_THRESHOLD = 10
//
//        mainButton.setOnTouchListener { v, event ->
//            val params = controlParams ?: return@setOnTouchListener false
//
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    initialTouchX = event.rawX
//                    initialTouchY = event.rawY
//                    dragging = false
//                    true
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    val dx = (event.rawX - initialTouchX).toInt()
//                    val dy = (event.rawY - initialTouchY).toInt()
//
//                    if (abs(dx) > CLICK_THRESHOLD || abs(dy) > CLICK_THRESHOLD) {
//                        dragging = true
//                        params.x += dx
//                        params.y += dy
//                        windowManager?.updateViewLayout(controlPanel, params)
//
//                        // Save new position
//                        DataStorage.savePosition(999, params.x, params.y)
//
//                        // Update touch for smooth dragging
//                        initialTouchX = event.rawX
//                        initialTouchY = event.rawY
//                    }
//                    true
//                }
//                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                    if (!dragging) v.performClick()
//                    dragging = false
//                    true
//                }
//                else -> false
//            }
//        }
//
//        // --- Add panel to window ---
//        windowManager?.addView(controlPanel, controlParams)
//    }
//
//    @SuppressLint("ClickableViewAccessibility", "UseKtx")
//    private fun toggleControlPanel() {
//        isExpanded = !isExpanded
//        controlPanel?.let { panel ->
//
//            // Remove old overlay buttons (keep mainButton at index 0)
//            if (panel.childCount > 1) {
//                panel.removeViews(1, panel.childCount - 1)
//            }
//
//            if (isExpanded) {
//                val scrollView = ScrollView(this).apply {
//                    layoutParams = LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        dpToPx(250)
//                    )
//                }
//
//                val innerLayout = LinearLayout(this).apply {
//                    orientation = LinearLayout.VERTICAL
//                    layoutParams = LinearLayout.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT
//                    )
//                }
//
//                // ------------------- Overlay Buttons (coin names) -------------------
//                webViews.forEachIndexed { index, webView ->
//                    if (webView.parent != null) {
//                        val url = webView.url ?: ""
//                        val coinName = try {
//                            Uri.parse(url).getQueryParameter("symbol")
//                                ?.substringAfter(":")
//                                ?.uppercase() ?: "Unknown"
//                        } catch (e: Exception) {
//                            "Unknown"
//                        }
//
//                        val btn = createControlButton("$coinName ${index + 1}", "#FFBB86FC".toColorInt(), 10f) {
//                            restoreOverlay(index)
//                        }
//                        innerLayout.addView(btn)
//                    }
//                }
//
//                // ------------------- SOUND BUTTON -------------------
//                val stopSoundBtn = createControlButton("🔇 Sound", Color.BLACK, 10f) {
//                    if (mediaPlayer?.isPlaying == true) stopAlertSound()
//                }
//
//                // ------------------- STOP OVERLAY BUTTON -------------------
//                val stopOverlayBtn = createControlButton("🛑 Stop Overlays", Color.BLACK, 10f) {
//                    // Remove and clear overlays + webviews
//                    floatingOverlays.forEach { overlay ->
//                        windowManager?.removeViewImmediate(overlay)
//                    }
//                    floatingOverlays.clear()
//                    webViews.clear()
//                    Toast.makeText(this, "All overlays destroyed", Toast.LENGTH_SHORT).show()
//                }
//
//                // ------------------- START OVERLAY BUTTON -------------------
////                val startOverlayBtn = createControlButton("▶️ Start Overlays", Color.BLACK, 10f) {
////                    if (floatingOverlays.isEmpty()) {
////                        val urlsToRestore = listOf(
////                            "https://www.tradingview.com/chart/?symbol=BINANCE:DOGEUSDT",
////                            "https://www.tradingview.com/chart/?symbol=BINANCE:BTCUSDT"
////                            // add more default URLs here if needed
////                        )
////
////                        urls.forEachIndexed { index, url ->
////                            setupFloatingOverlay()
////                            setupWebView(url, index) // setupWebView handles loadUrl
////                            setupHeader(index)
////                            setupBubble(index)
////                            switchToState(STATE_MINI, index)
////                        }
////
////                        Toast.makeText(this, "Overlays restarted", Toast.LENGTH_SHORT).show()
////                    } else {
////                        Toast.makeText(this, "Overlays already running", Toast.LENGTH_SHORT).show()
////                    }
////                }
//
//                // ------------------- HIDE/SHOW ALL BUTTONS -------------------
//                val hideAllBtn = createControlButton("Hide ●", "#031956".toColorInt(), 10f) {
//                    floatingOverlays.forEach { it.visibility = View.GONE }
//                    Toast.makeText(this, "All overlays hidden", Toast.LENGTH_SHORT).show()
//                }
//
//                val showAllBtn = createControlButton("Show ⛶", "#031956".toColorInt(), 10f) {
//                    floatingOverlays.forEach { it.visibility = View.VISIBLE }
//                    Toast.makeText(this, "All overlays shown", Toast.LENGTH_SHORT).show()
//                }
//
//                // ------------------- OPEN APP BUTTON -------------------
//                val openAppBtn = createControlButton("📱 Open App", "#031956".toColorInt(), 10f) {
//                    try {
//                        val intent = packageManager.getLaunchIntentForPackage("com.example.floatingweb")
//                        if (intent != null) startActivity(intent)
//                        else Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
//                    } catch (e: Exception) {
//                        Toast.makeText(this, "Cannot open app", Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//                // ------------------- CLOSE PANEL BUTTON -------------------
//                val closeBtn = createControlButton("❌", Color.WHITE, 20f) {
//                    toggleControlPanel()
//                }
//
//                // ------------------- ADD BUTTONS IN ORDER -------------------
//                innerLayout.apply {
////                    addView(stopSoundBtn)
////                    addView(startOverlayBtn)
////                    addView(stopOverlayBtn)
//                    addView(showAllBtn)
//                    addView(hideAllBtn)
//                    addView(openAppBtn)
//                    addView(closeBtn)
//                }
//
//                scrollView.addView(innerLayout)
//                panel.addView(scrollView)
//            }
//        }
//    }
//
//
//    private fun removeControlPanel() {
//        controlPanel?.let { panel ->
//            try {
//                windowManager?.removeView(panel)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//            controlPanel = null
//            controlParams = null
//        }
//    }
//
//
//    // Helper: create round control buttons with consistent style
//    private fun createControlButton(
//        text: String,
//        bgColor: Int,
//        textSizeSp: Float,
//        onClick: () -> Unit,
//    ): Button {
//        return Button(this).apply {
//            this.text = text
//            this.textSize = textSizeSp
//            setTextColor(Color.WHITE)
//            background = GradientDrawable().apply {
//                shape = GradientDrawable.OVAL
//                setColor(bgColor)
//            }
//            layoutParams = LinearLayout.LayoutParams(dpToPx(50), dpToPx(50)).apply {
//                topMargin = dpToPx(6)
//            }
//            setOnClickListener { onClick() }
//            elevation = dpToPx(4).toFloat()
//        }
//    }
//
//
//    private fun restoreOverlay(index: Int) {
//        val overlay = floatingOverlays.getOrNull(index) ?: return
//        switchToState(STATE_MAX,index)
//        floatingOverlays[index].visibility = View.VISIBLE
//        if (overlay.parent == null) {
//            val params = WindowManager.LayoutParams(
//                WindowManager.LayoutParams.MATCH_PARENT,
//                WindowManager.LayoutParams.MATCH_PARENT,
//                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//                PixelFormat.TRANSLUCENT
//            ).apply {
//                gravity = Gravity.TOP or Gravity.START
//            }
//            windowManager?.addView(overlay, params)
//        }
//    }
//
//
//
//    override fun onDestroy() {
//        super.onDestroy()
//        webViews.forEach { it.destroy() }
//        floatingOverlays.forEach {
//            try {
//                windowManager?.removeView(it)
//            } catch (_: Exception) {
//                // View already removed
//            }
//        }
//
//        try {
//            if (this::wakeLock.isInitialized && wakeLock.isHeld) {
//                wakeLock.release()
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//       stopAlertSound()
//        removeControlPanel()
//        parentOverlay?.let { windowManager?.removeView(it) }
//    }
//
//    private fun startForegroundService() {
//        val notificationChannelId = "floating_web_channel"
//        val channel = NotificationChannel(
//            notificationChannelId,
//            "Floating Web Service",
//            NotificationManager.IMPORTANCE_LOW
//        )
//        val manager = getSystemService(NotificationManager::class.java)
//        manager.createNotificationChannel(channel)
//
//        val notification = NotificationCompat.Builder(this, "floating_web_channel")
//            .setContentTitle("Floating Browser Running")
//            .setContentText("Tap to manage your overlays")
//            .setSmallIcon(com.example.floatingweb.R.mipmap.ic_launcher)
//            .setOngoing(true)
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .build()
//
//
//        startForeground(1, notification)
//    }
//
//    private fun dpToPx(dp: Int): Int {
//        val density = resources.displayMetrics.density
//        return (dp * density).toInt()
//    }
//    private var mediaPlayer: MediaPlayer? = null
//
//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    @SuppressLint("UnspecifiedImmutableFlag")
//    private fun showPriceAlertNotification(alert: PriceAlert, currentPrice: Double) {
//        // 1️⃣ Stop intent
//        val stopIntent = Intent(this, FloatingBrowserService::class.java).apply {
//            action = "STOP_ALERT"
//            putExtra("alertId", alert.id)
//        }
//        val stopPendingIntent = PendingIntent.getService(
//            this, alert.id.hashCode(), stopIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        // 2️⃣ Notification builder
//        val notification = NotificationCompat.Builder(this, "price_alert_channel")
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setContentTitle("Price Alert: ${alert.symbol}")
//            .setContentText("${alert.type} ${alert.threshold}, Current Price: $currentPrice")
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setCategory(NotificationCompat.CATEGORY_ALARM)
//            .addAction(R.drawable.ic_launcher_foreground, "Stop Sound", stopPendingIntent)
//            .setAutoCancel(false)
//            .build()
//
//        // 3️⃣ Show notification
//        NotificationManagerCompat.from(this).notify(alert.id.hashCode(), notification)
//
//        // 4️⃣ Play sound
////        playAlertSound()
//    }
//
//    // ---------------------
//// Sound handler
//    private fun playAlertSound() {
//        val prefs = getSharedPreferences("price_alert", Context.MODE_PRIVATE)
//        val uriString = prefs.getString("custom_sound_uri", null)
//        val alarmUri = uriString?.toUri() ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
//Log.d("sound","running.")
//        try {
//            // Stop old sound
//            mediaPlayer?.stop()
//            mediaPlayer?.release()
//            mediaPlayer = null
//
//            // Create new MediaPlayer
//            mediaPlayer = MediaPlayer().apply {
//                setDataSource(this@FloatingBrowserService, alarmUri)
//                isLooping = true // keeps playing until user stops
//                prepare()
//                start()
//            }
//        } catch (e: Exception) {
//            Log.e("FloatingService", "Error playing alert sound: ${e.message}")
//        }
//    }
//
//    // ---------------------
//// Stop sound
//    fun stopAlertSound() {
//        try {
//            mediaPlayer?.stop()
//            mediaPlayer?.release()
//            mediaPlayer = null
//        } catch (e: Exception) {
//            Log.e("FloatingService", "Error stopping sound: ${e.message}")
//        }
//    }
//
//    // ---------------------
//// Notification channel (call once in onCreate)
//    private fun createNotificationChannel() {
//        val channel = NotificationChannel(
//            "price_alert_channel",
//            "Price Alerts",
//            NotificationManager.IMPORTANCE_HIGH
//        ).apply {
//            description = "Notifications for price alerts"
//            enableLights(true)
//            lightColor = Color.RED
//            enableVibration(true)
//            setSound(null, null) // avoid default sound; handled via MediaPlayer
//        }
//        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
//    }
//
//
//

//package com.example.floatingweb.services.process
//
//import android.annotation.SuppressLint
//import android.app.Application
//import android.app.Service
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.graphics.Bitmap
//import android.graphics.Color
//import android.graphics.PixelFormat
//import android.graphics.Typeface
//import android.graphics.drawable.GradientDrawable
//import android.media.MediaPlayer
//import android.media.RingtoneManager
//import android.os.Build
//import android.os.Handler
//import android.os.IBinder
//import android.os.Looper
//import android.text.InputType
//import android.util.Log
//import android.view.*
//import android.view.inputmethod.EditorInfo
//import android.view.inputmethod.InputMethodManager
//import android.webkit.JavascriptInterface
//import android.webkit.WebView
//import android.webkit.WebViewClient
//import android.widget.ArrayAdapter
//import android.widget.AutoCompleteTextView
//import android.widget.Button
//import android.widget.EditText
//import android.widget.FrameLayout
//import android.widget.LinearLayout
//import android.widget.ScrollView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.annotation.RequiresApi
//import androidx.core.graphics.toColorInt
//import androidx.core.net.toUri
//import androidx.core.view.setPadding
//import androidx.core.widget.addTextChangedListener
//import com.example.floatingweb.helpers.AlertStatus
//import com.example.floatingweb.helpers.AlertType
//import com.example.floatingweb.helpers.DataStorage
//import com.example.floatingweb.helpers.PriceAlert
//import com.example.floatingweb.helpers.findAlertsForCoin
//import com.example.floatingweb.services.FloatingBrowserService.Companion.CLICK_THRESHOLD
//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
//import org.json.JSONObject
//import kotlin.collections.get
//import kotlin.collections.set
//import kotlin.math.abs
//
//open class FloatingWebProcess : Service() {
//
//    private lateinit var windowManager: WindowManager
//    private lateinit var container: FrameLayout
//    private lateinit var webView: WebView
//    private lateinit var headerBar: LinearLayout
//
//    private val draggableLayerId = View.generateViewId()
//    private var windowLayoutParams: WindowManager.LayoutParams? = null
//    private var isReceiverRegistered = false
//    private var isOverlayAttached = false
//    private var latestPrice = 0.00
//    private var latestSymbol = null
//    private var activeAlerts = mutableListOf<PriceAlert>()
//    companion object {
//        const val HEADER_HEIGHT_DP = 42
//        private const val MINIMIZED_WIDTH_DP = 140
//        private const val MINIMIZED_HEIGHT_DP = 190
//        private const val MAXIMIZED_SCALE = 250
//        private const val MINIMIZED_SCALE = 60
//        private const val DEFAULT_WEBVIEW_SUFFIX = "webview0"
//        private var DEFAULT_URL =
//            "https://www.google.com"
//        private const val ACTION_WEBVIEW_COMMAND = "com.example.floatingweb.WEBVIEW_COMMAND"
//    }
//
//    @SuppressLint("UnspecifiedRegisterReceiverFlag")
//    @RequiresApi(Build.VERSION_CODES.P)
//    override fun onCreate() {
//        super.onCreate()
//        if (!::windowManager.isInitialized) {
//            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
//        }
//        DataStorage.init(applicationContext)
//
//        if (!::container.isInitialized) {
//            val processName = Application.getProcessName()
//            if (processName != null && processName != packageName) {
//                WebView.setDataDirectorySuffix(processName.substringAfterLast(":"))
//            }
//            Log.d("webview process 001", "processName:$processName")
//
////            val suffix = intent?.getStringExtra("user") ?: DEFAULT_WEBVIEW_SUFFIX
////            WebView.setDataDirectorySuffix(suffix)
//            setupOverlay()
//            addDraggableLayer()
//        }
//
//        registerCommandReceiverIfNeeded()
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val action = intent?.getStringExtra("link")
//        webView.loadUrl(action.toString())
//        Log.d("ControllerService","Webview is started")
//
//        if (action != null) {
//            DEFAULT_URL = action
//        }
//
//        return START_STICKY
//    }
//    private val commandReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            if (intent?.action == "com.example.floatingweb.ALERTS_RESULT") {
//                try {
//                    val data = intent.getStringExtra("alerts") ?: "[]"
//                    val alertsList: List<PriceAlert> =
//                        Gson().fromJson(data, object : TypeToken<List<PriceAlert>>() {}.type)
//                    Log.d("Price001","$alertsList")
//
//                    // Now build overlay, e.g.:
////                    showAlertListOverlayWith(alertsList)
//                } catch (e: Exception) {
//                    Log.e("Subprocess", "Failed to parse alerts result", e)
//                }
//            }else if (intent?.action == "com.example.floatingweb.DestroyALL"){
//                stopSelf()
//            }
//        }
//    }
//
//    /** ---------------- Overlay construction ---------------- */
//    @SuppressLint("UseKtx", "SetJavaScriptEnabled")
//    private fun setupOverlay() {
//        container = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
//
//        setupHeader()
//
//        webView = WebView(this).apply {
//            settings.javaScriptEnabled = true
//            settings.domStorageEnabled = true
//            loadUrl(DEFAULT_URL)
//            setInitialScale(MINIMIZED_SCALE)
//        }
//
//        container.addView(
//            webView,
//            FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT
//            ).apply { topMargin = dpToPx(HEADER_HEIGHT_DP) }
//        )
//
//        windowLayoutParams = createWindowLayoutParams().also { params ->
//            windowManager.addView(container, params)
//            isOverlayAttached = true
//        }
//        minimize()
//
//        setupPriceMonitor(applicationContext)
//    }
//
//    @SuppressLint("SetJavaScriptEnabled")
//    private fun setupPriceMonitor(context: Context) {
//        val webView = webView.apply {
//            settings.javaScriptEnabled = true
//            settings.domStorageEnabled = true
//        }
//
//        // Runtime cache for active alerts
//        var activeAlert = mutableSetOf<PriceAlert>()
//        val lastTriggerTimes = mutableMapOf<String, Long>()
//
//        webView.addJavascriptInterface(object {
//            @JavascriptInterface
//            fun checkPrice(jsonString: String) {
//                try {
//                    val obj = JSONObject(jsonString)
//                    val symbol = obj.getString("symbol")
//                    val price = obj.getDouble("price")
//                    if (activeAlert.isEmpty()) {
//                        Log.i("PriceMonitor", "No active alerts for $symbol; skipping update.")
//                        return
//                    }
//
//                    Log.d("PriceMonitor", "Checking $symbol @$price with ${activeAlert.size} active alerts")
//
//                    val now = System.currentTimeMillis()
//                    val triggeredNow = mutableSetOf<PriceAlert>()
//
//                    // Evaluate all alerts, debounce per alert id
//                    activeAlert.removeAll { alert ->
//                        if (alert.status != AlertStatus.ACTIVE) {
//                            Log.v("PriceMonitor", "Alert ${alert.id} is not ACTIVE, removed.")
//                            return@removeAll true
//                        }
//                        val lastTime = lastTriggerTimes[alert.id] ?: 0L
//                        if (now - lastTime < 5000) return@removeAll false
//
//                        val shouldTrigger = when (alert.type) {
//                            AlertType.ABOVE -> price > alert.threshold
//                            AlertType.BELOW -> price < alert.threshold
//                        }
//                        if (shouldTrigger) {
//                            triggeredNow += alert
//                            lastTriggerTimes[alert.id] = now
//                            Log.w("PriceMonitor", "TRIGGER: ${alert.symbol} ${alert.type} ${alert.threshold} at $price")
//                        }
//                        false
//                    }
//
//                    if (triggeredNow.isNotEmpty()) {
//                        triggeredNow.forEach { alert ->
//                            try {
//                                playAlertSound()
//                                Log.w("PriceMonitor", "Triggered alert: ${alert.threshold} ")
//
//                                val intent = Intent("com.example.floatingweb.MAIN_COMMAND").apply {
//                                    putExtra("cmd", "trigger_alert")
//                                    putExtra("payload", Gson().toJson(alert.copy(
//                                        status = AlertStatus.TRIGGERED, // Or whatever status name you use
//                                        triggeredAt = System.currentTimeMillis(),
//                                        triggeredPrice = price
//                                    )))
//                                }
//                                refreshAlertsOverlayIfVisible()
//                                context.sendBroadcast(intent)
//                            } catch (e: Exception) {
//                                Log.e("PriceMonitor", "Failed to trigger alert: ${alert.id} - ${e.message}", e)
//                            }
//                        }
//                        // Optionally bring overlay to front, show popup, or maximize
//                        val intent = Intent("com.example.floatingweb.PRICE_TRIGGER")
//                        intent.putExtra("alerts", ArrayList(triggeredNow))
//                        context.sendBroadcast(intent)
//
//                        activeAlert.removeAll(triggeredNow)
//                        Log.d("PriceMonitor", "Handled and cleared ${triggeredNow.size} triggered alerts.")
//                    }
//
//                    // Cleanup: Remove debounce data aged over 30s
//                    lastTriggerTimes.entries.removeIf { (_, t) -> now - t > 30_000 }
//
//                } catch (e: Exception) {
//                    Log.e("PriceMonitor", "JSON Exception: $jsonString", e)
//                }
//            }
//
//            @JavascriptInterface
//            fun onSymbolChange(newSymbol: String) {
//                // Reload alerts on symbol change
//                val alerts = DataStorage.loadAlerts(context)
//                activeAlert = findAlertsForCoin(newSymbol, alerts, AlertStatus.ACTIVE).toMutableSet()
//                lastTriggerTimes.clear()
//                Log.i("PriceMonitor", "Symbol changed to $newSymbol; loaded ${activeAlert.size} active alerts.")
//            }
//        }, "Android")
//
//        // JS price and symbol monitor
//        val js = """
//        (function() {
//            const state = { observer: null, lastSymbol: "", lastPrice: NaN, debounceTimer: null };
//            function cleanup() { if (state.observer) state.observer.disconnect(); if (state.debounceTimer) clearTimeout(state.debounceTimer); }
//            window.cleanupPriceObserver = cleanup;
//            function parseSymbol(txt) { const m = txt.match(/^[A-Z0-9.:-]+/); return m ? m[0].trim() : ""; }
//            function parsePrice(txt) { const m = txt.match(/[\d,]+\.\d+/); return m ? parseFloat(m[0].replace(/,/g,'')) : NaN; }
//            function sendUpdate(symbol, price) { if (!symbol || !Number.isFinite(price)) return; Android.checkPrice(JSON.stringify({ symbol, price })); if (symbol !== state.lastSymbol) Android.onSymbolChange(symbol); state.lastSymbol = symbol; state.lastPrice = price; }
//            function handleTitleChange() { const t = document.title || ''; const s = parseSymbol(t), p = parsePrice(t); if (s && Number.isFinite(p)) sendUpdate(s, p); }
//            const titleEl = document.querySelector('title');
//            if (titleEl) { state.observer = new MutationObserver(handleTitleChange); state.observer.observe(titleEl, { childList:true, characterData:true, subtree:true }); handleTitleChange(); }
//        })();
//    """.trimIndent()
//
//        webView.webViewClient = object : WebViewClient() {
//            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
//                super.onPageStarted(view, url, favicon)
//                view?.evaluateJavascript("window.cleanupPriceObserver?.();", null)
//                lastTriggerTimes.clear()
//                Log.d("PriceMonitor", "Page loading started; old debounce cleared.")
//            }
//
//            override fun onPageFinished(view: WebView?, url: String?) {
//                super.onPageFinished(view, url)
//                view?.postDelayed({
//                    view.evaluateJavascript("document.title") { title ->
//                        val clean = title.trim('"').replace("\\n", "").trim()
//                        val alerts = DataStorage.loadAlerts(context)
//                        activeAlert = findAlertsForCoin(clean, alerts, AlertStatus.ACTIVE).toMutableSet()
//                        Log.i("PriceMonitor", "Page finished: '$clean' -> ${activeAlert.size} alerts loaded. Injecting JS watcher.")
//                        view.evaluateJavascript(js, null)
//                    }
//                }, 2000)
//            }
//        }
//    }
//
//    private fun createWindowLayoutParams() =
//        WindowManager.LayoutParams(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//            PixelFormat.TRANSLUCENT
//        ).apply { gravity = Gravity.TOP or Gravity.START }
//
//    /** ---------------- Header bar ---------------- */
//    @SuppressLint("UseKtx")
//    private fun setupHeader() {
//        headerBar = LinearLayout(this).apply {
//            orientation = LinearLayout.HORIZONTAL
//            setBackgroundColor("#FFBB86FC".toColorInt())
//            gravity = Gravity.CENTER_VERTICAL
//            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
//            elevation = 2f
//        }
//
//        listOf(
////            "⛶" to ::maximize,
//            "🚨" to ::showAlertListOverlay,
//            "🔇" to ::stopAlertSound,
//            "▭" to ::minimize,
//            "●" to { container.visibility = View.GONE },
//            "🎯" to ::showSetAlertOverlay,
//            "✕" to ::onDestroy
//        ).forEach { (label, action) ->
//            headerBar.addView(createHeaderButton(label, action))
//        }
//
//        container.addView(
//            headerBar,
//            FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                dpToPx(HEADER_HEIGHT_DP)
//            ).apply { gravity = Gravity.TOP }
//        )
//    }
//
//    private fun createHeaderButton(label: String, onClick: () -> Unit) =
//        Button(this).apply {
//            text = label
//            textSize = 18f
//            setBackgroundColor(Color.TRANSPARENT)
//            setTextColor(Color.WHITE)
//            setPadding(dpToPx(1), 0, dpToPx(1), 0)
//            layoutParams = LinearLayout.LayoutParams(
//                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f // key line (weight = 1)
//            ).apply {
//                marginStart = dpToPx(3)
//                marginEnd = dpToPx(3)
//            }
//
//            setOnClickListener { onClick() }
//        }
//
//    /** ---------------- Draggable layer ---------------- */
//    @SuppressLint("ClickableViewAccessibility", "ResourceType")
//    private fun addDraggableLayer() {
//        val draggableLayer = FrameLayout(this).apply layer@{
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
//            setOnTouchListener { _, event ->
//                val params = windowLayoutParams ?: return@setOnTouchListener false
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
//                            windowManager.updateViewLayout(container, params)
//                        }
//                        true
//                    }
//
//                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                        if (!dragging) performClick()
//                        dragging = false
//                        true
//                    }
//
//                    else -> false
//                }
//            }
//
//            setOnClickListener { maximize() }
//        }
//
//        container.addView(
//            draggableLayer.apply { id = draggableLayerId },
//            FrameLayout.LayoutParams(
//                WindowManager.LayoutParams.MATCH_PARENT,
//                WindowManager.LayoutParams.MATCH_PARENT
//            )
//        )
//    }
//
//    /** ---------------- Window state handling ---------------- */
//    @SuppressLint("ResourceType")
//    private fun maximize() {
//        val params = windowLayoutParams ?: return
//        bringOverlayToFront()
//
//        params.width = WindowManager.LayoutParams.MATCH_PARENT
//        params.height = WindowManager.LayoutParams.MATCH_PARENT
//        windowManager.updateViewLayout(container, params)
//
//        updateWebViewLayout(dpToPx(HEADER_HEIGHT_DP))
//        updateHeaderLayout(FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(HEADER_HEIGHT_DP))
//
//        requestOverlayFocus()
//        webView.setInitialScale(MAXIMIZED_SCALE)
//        headerBar.visibility = View.VISIBLE
//        toggleDraggableLayer(show = false)
//
//        showToast("Maximized")
//    }
//
//    @SuppressLint("ResourceType")
//    private fun minimize() {
//        val params = windowLayoutParams ?: return
//
//        params.width = dpToPx(MINIMIZED_WIDTH_DP)
//        params.height = dpToPx(MINIMIZED_HEIGHT_DP)
//        windowManager.updateViewLayout(container, params)
//
//        updateWebViewLayout(topMargin = 0)
//        webView.setInitialScale(MINIMIZED_SCALE)
//        headerBar.visibility = View.GONE
//        toggleDraggableLayer(show = true)
//        releaseOverlayFocus()
//
//        showToast("Minimized")
//    }
//
//    private fun updateWebViewLayout(topMargin: Int) {
//        (webView.layoutParams as? FrameLayout.LayoutParams)?.apply {
//            this.topMargin = topMargin
//            webView.layoutParams = this
//        }
//    }
//
//    private fun updateHeaderLayout(width: Int, height: Int) {
//        (headerBar.layoutParams as? FrameLayout.LayoutParams)?.apply {
//            this.width = width
//            this.height = height
//            this.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
//            headerBar.layoutParams = this
//        }
//    }
//
//    private fun toggleDraggableLayer(show: Boolean) {
//        container.findViewById<View>(draggableLayerId)?.apply {
//            visibility = if (show) View.VISIBLE else View.GONE
//            isClickable = show
//            isFocusable = show
//        }
//    }
//
//    private fun bringOverlayToFront() {
//        if (!isOverlayAttached) return
//        val params = windowLayoutParams ?: return
//        runCatching {
//            windowManager.removeViewImmediate(container)
//            windowManager.addView(container, params)
//        }.onFailure { it.printStackTrace() }
//    }
//
//    private fun requestOverlayFocus() {
//        val params = windowLayoutParams ?: return
//        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
//        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
//        windowManager.updateViewLayout(container, params)
//    }
//
//    private fun releaseOverlayFocus() {
//        val params = windowLayoutParams ?: return
//        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//        windowManager.updateViewLayout(container, params)
//    }
//
//    /** ---------------- Utilities ---------------- */
//    private fun registerCommandReceiverIfNeeded() {
//        if (isReceiverRegistered) return
//
//        val filter = IntentFilter().apply {
//            addAction("com.example.floatingweb.ALERTS_RESULT")
//            addAction("com.example.floatingweb.DestroyALL")
//        }
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
//            // Android 13+ requires specifying whether receiver is exported
//            registerReceiver(commandReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
//        } else {
//            @Suppress("UnspecifiedRegisterReceiverFlag")
//            registerReceiver(commandReceiver, filter)
//        }
//
//        isReceiverRegistered = true
//    }
//
////    private fun showSetAlertOverlay() = showToast("Set alert clicked")
//
//    @SuppressLint("ClickableViewAccessibility")
//    private fun showSetAlertOverlay() {
//        val overlay = container
//        val webView = webView
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
//            setOnClickListener { windowManager.removeView(cardLayout) }
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
//                setColor("#F5F5F5".toColorInt())
//            }
//
//            // Adapter for autocomplete suggestions
//            val adapter = ArrayAdapter(
//                this@FloatingWebProcess,
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
//        cardLayout.addView(nameInput)
//        cardLayout.addView(targetPriceInput)
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
//        windowManager.addView(cardLayout, params)
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
//        webView.evaluateJavascript("document.title") { title ->
//            val clean = title.trim('"').replace("\\n", "").trim()
//            val matchSymbol = Regex("^[A-Z0-9.:-]+").find(clean)?.value
//            val matchPrice = Regex("[\\d,]+\\.\\d+").find(clean)?.value?.replace(",", "")?.toDoubleOrNull()
//            if (!matchSymbol.isNullOrBlank() && matchPrice != null) {
//                updateSymbolPrice(matchSymbol, matchPrice)
//            }
//        }
//
//        // --- Toggle Cancel when target empty ---
//        targetPriceInput.addTextChangedListener { text ->
//            val empty = text.isNullOrBlank()
//            actionBtn.text = if (empty) "Cancel" else "Set Alert Automatically"
//            actionBtn.setBackgroundColor(if (empty) Color.RED else "#4CAF50".toColorInt())
//        }
//
//        // --- Button click ---
//        actionBtn.setOnClickListener {
//            val targetText = targetPriceInput.text.toString()
//            Log.d("target set 000", "targettext:$targetText")
//            if (targetText.isBlank()) {
//                windowManager.removeView(cardLayout)
//                return@setOnClickListener
//            }
//
//            val target = targetText.toDoubleOrNull()
//            if (target == null) {
//                Toast.makeText(this@FloatingWebProcess, "Invalid target price", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            val name = nameInput.text.toString().ifEmpty { symbol }
//            val type = if (currentPrice < target) AlertType.ABOVE else AlertType.BELOW
//            Log.d("target set 000", "$name")
//
//            val newAlert = PriceAlert(name = name, symbol = symbol, threshold = target, type = type)
//            val intent = Intent("com.example.floatingweb.MAIN_COMMAND").apply {
//                putExtra("cmd", "save_alert")
//                putExtra("payload", Gson().toJson(newAlert))
//            }
//            sendBroadcast(intent)
////            val allAlerts = DataStorage.loadAlerts()
////            Log.d("target set", "$newAlert $allAlerts")
//
////            DataStorage.saveAlerts(this@FloatingWebProcess, allAlerts + newAlert)
//            activeAlerts.add(newAlert)
//            refreshAlertsOverlayIfVisible()
//            Toast.makeText(this@FloatingWebProcess, "Alert set: $name $type $target", Toast.LENGTH_SHORT).show()
//            windowManager.removeView(cardLayout)
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
//    @SuppressLint("InflateParams")
//    private fun showAlertListOverlay() {
//        // Remove any existing view first
//        refreshAlertsOverlayIfVisible()
//        val existing = container.findViewWithTag<View>("alerts_overlay")
//        existing?.let { windowManager.removeView(it) }
//
//        // Load all targets/alerts
//
//        val intent = Intent("com.example.floatingweb.MAIN_COMMAND").apply {
//            putExtra("cmd", "get_alerts")
//        }
//        applicationContext.sendBroadcast(intent)
//
//        val alerts = DataStorage.loadAlerts(applicationContext).asReversed()
//        // Full-screen overlay parent
//        val overlay = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            setBackgroundColor("#CC222222".toColorInt())
//            elevation = 12f
//            setPadding(dpToPx(18), dpToPx(38), dpToPx(18), dpToPx(18))
//            layoutParams = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT
//            )
//            tag = "alerts_overlay"
//        }
//
//        // Header with title & close button
//        val header = LinearLayout(this).apply {
//            orientation = LinearLayout.HORIZONTAL
//            setPadding(0, 0, 0, dpToPx(12))
//        }
//        header.addView(TextView(this).apply {
//            text = "All Targets"
//            textSize = 22f
//            setTextColor(Color.WHITE)
//            setTypeface(null, Typeface.BOLD)
//            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
//        })
//        header.addView(Button(this).apply {
//            text = "✕"
//            textSize = 20f
//            setOnClickListener { windowManager.removeView(overlay) }
//        })
//        overlay.addView(header)
//
//        // Results area in a ScrollView
//        val scroll = ScrollView(this).apply {
//            setBackgroundColor(Color.TRANSPARENT)
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
//            )
//        }
//        val content = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            setBackgroundColor(Color.TRANSPARENT)
//        }
//        if (alerts.isEmpty()) {
//            content.addView(TextView(this).apply {
//                text = "No alerts/targets set."
//                textSize = 18f
//                setTextColor(Color.LTGRAY)
//                gravity = Gravity.CENTER_HORIZONTAL
//                setPadding(0, dpToPx(42), 0, 0)
//            })
//        } else {
//            alerts.forEach { alert ->
//                val statusColor = when(alert.status) {
//                    AlertStatus.TRIGGERED -> "#FF9100"
//                    AlertStatus.ACTIVE -> "#4CAF50"
//                    else -> "#AAAAAA"
//                }
//                content.addView(TextView(this).apply {
//                    text = "${alert.symbol}  •  ${alert.type}  ${alert.threshold}  (${alert.status})"
//                    textSize = 18f
//                    setTextColor(Color.parseColor(statusColor))
//                    setPadding(0, dpToPx(10), 0, dpToPx(10))
//                })
//            }
//        }
//        scroll.addView(content)
//        overlay.addView(scroll)
//
//        // Add view to overlay
//        val params = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
//            PixelFormat.TRANSLUCENT,
//        )
//        windowManager.addView(overlay, params)
//    }
//
//    // Example utility for update/refresh pattern:
//    fun refreshAlertsOverlayIfVisible() {
//        // This searches all direct children in your root overlay container
//        // Use the SAME container in which you add the alerts overlay!
//        val parent = container // should be your overlay container/frame
//        val overlay = parent.findViewWithTag<View>("alerts_overlay")
//        if (overlay != null) {
//            windowManager.removeViewImmediate(overlay)
//            showAlertListOverlay()
//        }
//    }
//
//
//    private var mediaPlayer: MediaPlayer? = null
//
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
//                setDataSource(this@FloatingWebProcess, alarmUri)
//                isLooping = true // keeps playing until user stops
//                prepare()
//                start()
//            }
//        } catch (e: Exception) {
//            Log.e("FloatingService", "Error playing alert sound: ${e.message}")
//        }
//    }
//
//    private fun stopAlertSound() {
//        try {
//            mediaPlayer?.stop()
//            mediaPlayer?.release()
//            mediaPlayer = null
//        } catch (e: Exception) {
//            Log.e("FloatingService", "Error stopping sound: ${e.message}")
//        }
//    }
//
//    private fun showToast(message: String) =
//        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//
//    private fun dpToPx(dp: Int): Int =
//        (dp * resources.displayMetrics.density).toInt()
//
//    override fun onDestroy() {
//        super.onDestroy()
//        if (isReceiverRegistered) {
//            unregisterReceiver(commandReceiver)
//            isReceiverRegistered = false
//        }
//        if (isOverlayAttached) {
//            runCatching { windowManager.removeViewImmediate(container) }
//            isOverlayAttached = false
//        }
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//}
//
//class FloatingWebProcessA : FloatingWebProcess()
//class FloatingWebProcessB : FloatingWebProcess()
//class FloatingWebProcessC : FloatingWebProcess()
//
//class FloatingWebProcessD : FloatingWebProcess()
//class FloatingWebProcessE : FloatingWebProcess()
//class FloatingWebProcessF : FloatingWebProcess()
//
//class FloatingWebProcessG : FloatingWebProcess()
//class FloatingWebProcessH : FloatingWebProcess()
//class FloatingWebProcessI : FloatingWebProcess()
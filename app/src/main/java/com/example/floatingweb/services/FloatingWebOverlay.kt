package com.example.floatingweb.services

import android.Manifest
import android.R.attr.orientation
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.text.Editable
import android.text.InputType
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.setPadding
import kotlin.math.abs
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import com.android.identity.util.UUID
import com.example.floatingweb.LinkStorageManager
import com.example.floatingweb.helpers.DataStorage
import com.example.floatingweb.helpers.AlertType
import com.example.floatingweb.helpers.PriceAlert
import com.example.floatingweb.R
import com.example.floatingweb.helpers.AlertFor
import kotlinx.coroutines.*
import java.net.URLDecoder
import android.text.TextWatcher
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import com.example.floatingweb.screens.isServiceRunning
import kotlin.collections.forEachIndexed

private fun getSelectedLinks(context: Context): ArrayList<LinkItemData> {
    val storage = LinkStorageManager(context)

    val allLinks = storage.loadLinks()              // List<String>
    val allTimeFrames = storage.loadTimeFrames()    // List<String>

    val prefs = context.getSharedPreferences("price_alert", Context.MODE_PRIVATE)
    val selectedSet = prefs.getStringSet("selected_indices", emptySet())
    val selectedIndices = selectedSet?.mapNotNull { it.toIntOrNull() } ?: emptyList()

    return ArrayList(
        selectedIndices.mapNotNull { idx ->
            val url = allLinks.getOrNull(idx)
            val time = allTimeFrames.getOrNull(idx) ?: "1"

            if (url != null) LinkItemData(url, time) else null
        }
    )
}


//private fun getTimeFrames(context: Context): ArrayList<String>{
//    val timeFrams = LinkStorageManager(context).loadTimeFrames()
//    return ArrayList(timeFrams)
//}
fun extractPairFromUrl(url: String): String? {
    return try {
        val uri = Uri.parse(url)
        val encodedSymbol = uri.getQueryParameter("symbol") ?: return null
        val decodedSymbol = URLDecoder.decode(encodedSymbol, "UTF-8")  // e.g. BINANCE:BTCUSD
        decodedSymbol.substringAfter(":").uppercase()  // -> BTCUSD
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

data class OverlayHolder(
    val id: String,
    val overlay: FrameLayout,
    var webView: WebView,
    var headerBar: LinearLayout,
    val bubble: FrameLayout,
    val wrapper: FrameLayout,
    var coinSymbol: String?,
    val alertEnabled: Boolean,
    var monitorInitialized: Boolean,
    val mode: Int = 0,
    val timeFrame:String = "1"
)

data class LinkItemData (
    val url:String,
    val timeFrame:String = "1"
)
// prev version find 9:15 pm 11/3/2025
class FloatingBrowserService : Service() {

    private var windowManager: WindowManager? = null
    private var parentOverlay: FrameLayout? = null
//    var urls = mutableListOf<links>()

    // --- ID-based registry
    private val overlaysById = mutableMapOf<String, OverlayHolder>()
    private val currentStates = mutableMapOf<String, Int>()
    private val savedPositions = mutableMapOf<String, Pair<Int, Int>>()
    private val latestPrices = mutableMapOf<String, Double>()
    private val latestSymbols = mutableMapOf<String, String>()
    private val handlersById = mutableMapOf<String, Handler>()
    private val attachedInBox = mutableSetOf<String>()
    private val savedBoxWrappers = mutableMapOf<String, FrameLayout>()

    var AlertSavedOnPage = mutableMapOf<String, MutableSet<String>>()

    companion object {
        lateinit var appContext: Context

        private const val STATE_MAX = 1
        private const val STATE_MEDIUM = 2
        private const val STATE_MINI = 3

        private var SIZE_MEDIUM_WIDTH_DP = 300
        private var SIZE_MEDIUM_HEIGHT_DP = 500
        private var SIZE_MINI_WIDTH_DP = 130
        private var SIZE_MINI_HEIGHT_DP = 200
        private const val HEADER_HEIGHT_DP = 40
        const val CLICK_THRESHOLD = 10
    }

    private val activeAlertsState = ReactiveState<MutableMap<String, MutableSet<PriceAlert>>>(mutableMapOf())

    class ReactiveState<T>(initial: T) {
        private val listeners = mutableListOf<(T) -> Unit>()
        var value: T = initial
            set(v) {
                field = v
                listeners.forEach { it(v) }
            }

        fun observe(listener: (T) -> Unit) {
            listeners.add(listener)
            listener(value) // trigger once immediately
        }
    }

    private lateinit var wakeLock: PowerManager.WakeLock

    // 🔧 FIX: Use CoroutineScope with lifecycle
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var overlayJob: Job? = null

    // 🔧 FIX: Handlers that need cleanup
    private val handlers = mutableListOf<Handler>()

    // 🔧 FIX: Track destruction state atomically
    @Volatile
    private var isDestroying = false
    override fun onBind(intent: Intent?) = null

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.example.action.SHOW_ALL_OVERLAY") {
            val symbol = intent.getStringExtra("symbol")
            Log.d("showall","from webOverlay intent show all")
            for (holder in overlaysById.values) {
                Log.d("showall","from webOverlay ${holder.coinSymbol} ${symbol}intent show all")


                if (holder.coinSymbol?.replace(".P","") == symbol) {
                    switchToState(STATE_MINI, holder.id)
                    holder.overlay.visibility = View.VISIBLE
                    break   // stop checking after first match
                }
            }

        }

        appContext = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val urlsData = getSelectedLinks(this@FloatingBrowserService)
        Log.d("links", "$urlsData ")

        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
            screenOn = false
        }

        val containerSizes = intent?.getBundleExtra("ContainerSizes")
        val miniSize = containerSizes?.getIntArray("mini") ?: intArrayOf(SIZE_MINI_WIDTH_DP, SIZE_MINI_HEIGHT_DP)
        val mediumSize = containerSizes?.getIntArray("medium") ?: intArrayOf(SIZE_MEDIUM_WIDTH_DP, SIZE_MEDIUM_HEIGHT_DP)

        SIZE_MEDIUM_WIDTH_DP = mediumSize[0]
        SIZE_MEDIUM_HEIGHT_DP = mediumSize[1]
        SIZE_MINI_WIDTH_DP = miniSize[0]
        SIZE_MINI_HEIGHT_DP = miniSize[1]

        // 🔧 FIX: Create notification channel ONCE at creation
        createNotificationChannel()

        DataStorage.init(this)


        if (overlaysById.isEmpty()) {
            setupControlPanel()

            // Keep a set to track which symbols already have an alert
            val activeSymbols = mutableSetOf<String>()


            val mode = intent?.getIntExtra("Mode",0)

            urlsData.forEachIndexed { index, data ->

                val id = UUID.randomUUID().toString()
                val floatingOverlay = FrameLayout(this)
                val webView = WebView(this)
                val headerBar = LinearLayout(this)
                val bubble = FrameLayout(this)
                val wrapper = FrameLayout(this)

                val symbol = extractPairFromUrl(data.url) ?: "UNKNOWN"

                // If this symbol already exists, disable alert for duplicates
                val alertEnabled = if (activeSymbols.contains(symbol)) {
                    false
                } else {
                    activeSymbols.add(symbol)
                    true
                }

                val holder = OverlayHolder(
                    id = id,
                    overlay = floatingOverlay,
                    webView = webView,
                    headerBar = headerBar,
                    bubble = bubble,
                    wrapper = wrapper,
                    coinSymbol = symbol,
                    alertEnabled = alertEnabled,
                    monitorInitialized = false,
                    mode = mode ?: 0,
                    timeFrame = data.timeFrame
                )

//                holder.overlay.visibility = View.GONE
                overlaysById[id] = holder

                setupFloatingOverlay(id)
                when (mode) {
                    0 -> setupWebView(id, data.url, false)
                    1 -> setupWebView(id, data.url, true)
                    2 -> setupWebViewWithLocalCache(id, data.url) // 👈 Note: fixed typo "Chece" → "Cache"
                    else -> setupWebView(id, data.url, false) // default fallback
                }
                setupHeader(id)
                setupBubble(id)
                switchToState(STATE_MINI, id)

            }

            startOverlayTimerCoroutine()
        }

        val action = intent?.action
        if (action != null) {
            when (action) {
                "STOP_ALERT" -> {
                    stopAlertSound()
                    NotificationManagerCompat.from(this).cancelAll()
                }
            }
        }
        registerScreenReceiver()
//        ensureWakeLock()
        startForegroundService()
        return START_STICKY
    }


    private var screenOn = true

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true

                    overlaysById.values.forEach { holder ->
                        resumeWebView(holder.id)
                        // Use the stored mode from the holder
                        injectAutoTimeframeScriptIfScreenOn(
                            holder.id,
                            if (holder.mode == 0 || holder.mode == 2) false else true
                        )
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    overlaysById.keys.forEach { id ->
                        pauseWebViewIfNoAlert(id)
                    }
                    toggleHide(false)
                    restoreOverlayToMini()
                }
            }
        }
    }

    private var isScreenReceiverRegistered = false

    private fun registerScreenReceiver() {
        if (isScreenReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
        isScreenReceiverRegistered = true
    }


    // 🔧 FIX: Proper coroutine with lifecycle management
    private fun startOverlayTimerCoroutine() {
        overlayJob?.cancel() // Cancel any existing job
        overlayJob = serviceScope.launch {
            Log.d("timer", "running timer...")
//            CrashLogger.log(this@FloatingBrowserService, "FloatingService", "⚡ Entered [StartOverlayTimerCortine]")
            try {
                while (isActive && !isDestroying) {
                    delay(120 * 60 * 1000L)  // 120 minutes
                    if (!isDestroying) {
                        withContext(Dispatchers.Main) {
                            overlaysById.keys.forEach { id ->
                                switchToState(STATE_MINI,id)
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d("timer", "Timer coroutine cancelled")
//                CrashLogger.log(this@FloatingBrowserService, "FloatingService", "Timer coroutine cancelled")

            } catch (e: Exception) {
                Log.e("timer", "Timer error: ${e.message}")
//                CrashLogger.log(this@FloatingBrowserService, "FloatingService", "Timer error: ${e.message}")
            }
        }
    }

    // 🔧 FIX: Proper wake lock management with timeout
    private fun ensureWakeLock() {
//        CrashLogger.log(this, "FloatingService", "⚡ Entered [ensure wakeLock]")
        if (!this::wakeLock.isInitialized) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "FloatingWeb::PriceWatchLock"
            )
            // 🔧 ADD: Set timeout to prevent eternal locks
            wakeLock.setReferenceCounted(false)
        }
        if (!wakeLock.isHeld) {
            // 🔧 FIX: Add timeout (8 hours max)
            wakeLock.acquire(8 * 60 * 60 * 1000L)
//            CrashLogger.log(this, "FloatingService", "Add timeout (4 hours max)")

        }
    }

    private fun releaseWakeLock() {
        try {
            if (this::wakeLock.isInitialized && wakeLock.isHeld) {
                wakeLock.release()
//                CrashLogger.log(this, "FloatingService", "⚡ Entered [release wakeLock]")
            }
        } catch (e: Exception) {
            Log.e("FloatingService", "Error releasing wake lock: ${e.message}")
//            CrashLogger.log(this, "FloatingService", "Error releasing wake lock: ${e.message}")

        }
    }

    private fun setupParentOverlay() {
//        CrashLogger.log(this, "FloatingService", "⚡ Entered [setup Parent Overly]")
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )

        parentOverlay = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        try {
            windowManager?.addView(parentOverlay, params)
        } catch (e: Exception) {
            Log.e("FloatingService", "Failed to add parent overlay: ${e.message}")
//            CrashLogger.log(this, "FloatingService", "Failed to add parent overlay: ${e.message}")

        }
    }

    private fun setupFloatingOverlay(id: String) {
//        CrashLogger.log(this, "FloatingService", "⚡ Entered [setup Floating Overlay] for id=$id")

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )

        val floatingOverlay = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            elevation = 16f
            clipChildren = false
            clipToOutline = false
        }

        try {
            windowManager?.addView(floatingOverlay, params)
            floatingOverlay.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            // --- Register everything by ID! ---
            val holder = overlaysById[id] ?: OverlayHolder(
                id = id,
                overlay = floatingOverlay,
                webView = WebView(this),
                headerBar = LinearLayout(this),
                bubble = FrameLayout(this),
                wrapper = FrameLayout(this),
                coinSymbol = null,
                false,
                false,
                timeFrame = "0"
            )
            Log.d("HWAccel", "Overlay $id accelerated? ${floatingOverlay.isHardwareAccelerated}")

            overlaysById[id] = holder.copy(overlay = floatingOverlay) // Replace or set
            currentStates[id] = STATE_MINI

            if (!floatingOverlay.isHardwareAccelerated) {
                windowManager?.removeView(floatingOverlay)
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                windowManager?.addView(floatingOverlay, params)
                floatingOverlay.setLayerType(View.LAYER_TYPE_HARDWARE, null)  // Double-force
                Log.d("HWAccel", "Re-added overlay $id for accel fix. Now accelerated? ${floatingOverlay.isHardwareAccelerated}")
            }
        } catch (e: Exception) {
            Log.e("FloatingService", "Failed to add floating overlay: ${e.message}")
//            CrashLogger.log(this, "FloatingService", "Failed to add floating overlay: ${e.message}")
        }
    }

    private fun requestOverlayFocus(id: String) {
        val holder = overlaysById[id] ?: return
        val params = holder.overlay.layoutParams as? WindowManager.LayoutParams ?: return
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
        try {
            windowManager?.updateViewLayout(holder.overlay, params)
        } catch (e: Exception) {
            Log.e("FloatingService", "Error requesting focus: ${e.message}")
//            CrashLogger.log(this, "FloatingService", "Error requesting focus:${e.message}")
        }
    }

    private fun releaseOverlayFocus(id: String) {
        val holder = overlaysById[id] ?: return
        val params = holder.overlay.layoutParams as? WindowManager.LayoutParams ?: return
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        try {
            windowManager?.updateViewLayout(holder.overlay, params)
        } catch (e: Exception) {
            Log.e("FloatingService", "Error releasing focus: ${e.message}")
//            CrashLogger.log(this, "FloatingService", "Error releasing focus: ${e.message}")
        }
    }


//     🔧 FIX: Proper WebView lifecycle management
    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setupWebView(id: String, url: String,inject: Boolean) {
//        CrashLogger.log(this, "FloatingService", "⚡ Entered [setup webview] for id=$id")
        val holder = overlaysById[id] ?: return
        val webView = holder.webView
//
    Log.d("inject ", "Overlay $id inject  $inject                               ")

        // configure webView just like you do
// In setupFloatingOverlay (after addView):
        Log.d("HWAccel", "Overlay $id accelerated? ${holder.overlay.isHardwareAccelerated}")

// In setupWebView (after addView):
        holder.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        Log.d("HWAccel", "WebView $id accelerated? ${holder.webView.isHardwareAccelerated}")
        if (!holder.webView.isHardwareAccelerated) {
            Log.w("HWAccel", "WebView $id still not accelerated - device may not support overlays. Falling back to software.")
            holder.webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)  // Fallback if hardware fails
        }
//
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.setBackgroundColor(Color.WHITE)
        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
//        webView.clearCache(true)
//        val cm = CookieManager.getInstance()
//        cm.setAcceptCookie(true)
//        cm.setAcceptThirdPartyCookies(webView, true)
//        cm.flush()


        webView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && !isDestroying) {
                requestOverlayFocus(id)
            }
            false
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            topMargin = dpToPx(HEADER_HEIGHT_DP)
        }
        holder.overlay.addView(webView, lp)
        holder.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isDestroying) {
                webView.loadUrl(url)
            }
        }, 400L)


    webView.addJavascriptInterface(object {

        @JavascriptInterface
        fun onInjectedSpanClick(spanText: String) {

            // Example:
            // "Add alert on BTCUSDT at 43250.5"

            val regex = Regex("Add alert on ([A-Z0-9.]+) at ([\\d,.]+)")
            val match = regex.find(spanText) ?: return

            val symbolRaw = match.groupValues[1]
            val price = match.groupValues[2]
                .replace(",", "")
                .toDoubleOrNull() ?: return

            Handler(Looper.getMainLooper()).post {

                val holder = overlaysById[id] ?: return@post

                // Use latest tracked price if available
                val latestPrice = latestPrices[id] ?: price

                val type = if (latestPrice < price)
                    AlertType.ABOVE
                else
                    AlertType.BELOW

                val newAlertId = UUID.randomUUID().toString()
                val alertFor = if (symbolRaw.contains(".P"))
                    AlertFor.USDM
                else
                    AlertFor.SPOT

                val symbol = symbolRaw.replace(".P","")

                val newAlert = PriceAlert(
                    id = newAlertId,
                    name = "Auto-$symbol-$price",
                    symbol = symbol,
                    threshold = price,
                    type = type,
                    alertFor = alertFor
                )

                // Save using your real storage API
                DataStorage.addAlert(newAlert)

                // Mark alert saved on this page
                AlertSavedOnPage.getOrPut(id) { mutableSetOf() }
                    .add(newAlertId)

                // Update reactive active state
                val map = activeAlertsState.value
                val set = map.getOrPut(symbol) { mutableSetOf() }
                set.add(newAlert)
                activeAlertsState.value = map

                Log.d("InstantAlert", "✅ Alert saved: $symbol at $price")
            }
        }

    }, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
//                view?.evaluateJavascript("window.cleanupPriceObserver?.();", null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if(isDestroying) return
                view?.postDelayed( {
//                    view.invalidate()  // Force redraw
                    Log.d("RenderKick", "Forced render for $id after load")
                    injectAutoTimeframeScriptIfScreenOn(id,inject)
                    injectInstantAlertWatcher(id)
                },1500L)

//                view?.postDelayed({setupPriceMonitor(id) ;if (holder.alertEnabled) startPriceFreezeWatchdog()},3000L)

            }
        }

    }

    private fun injectInstantAlertWatcher(id: String) {
        val webView = overlaysById[id]?.webView ?: return

        val js = """
        (function() {
            if (window._instantAlertWatcherAdded) return;
            window._instantAlertWatcherAdded = true;

            document.body.addEventListener('click', function(e) {
                let target = e.target;
                let text = (target.textContent || "").trim();

                // TradingView alert button text:
                // "Add alert on BTCUSDT at 43250.5"
                if (text.startsWith("Add alert on")) {
                    if (window.Android && window.Android.onInjectedSpanClick) {
                        window.Android.onInjectedSpanClick(text);
                    }

                    // Auto close popup after click
                    setTimeout(() => {
                        const closeBtn = document.querySelector("button.overlayBtn-FvtqqqvS");
                        if (closeBtn) closeBtn.click();
                    }, 800);
                }
            }, false);
        })();
    """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    //   Binance connect using this webview
    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setupWebViewWithLocalCache(id: String, url: String) {
        val holder = overlaysById[id] ?: return

        // 🔧 Create fresh WebView with optimized settings
        val webView = holder.webView.apply {
            setBackgroundColor(Color.WHITE)

            settings.apply {
                // ✅ Core JavaScript settings
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true

                // ✅ CRITICAL: Enable popup windows for OAuth
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)

                // ✅ Cache settings
                cacheMode = WebSettings.LOAD_DEFAULT
//                setAppCacheEnabled(true)

                // ✅ Content access
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                // ✅ User agent (important for TradingView)
                userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                // ✅ Display settings
//                useWideViewPort = true
//                loadWithOverviewMode = true
                builtInZoomControls = false
                displayZoomControls = false

                // ✅ Media settings
                mediaPlaybackRequiresUserGesture = false

            }

            // ✅ Hardware acceleration
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        // ✅ CRITICAL: Cookie Manager Setup
        setupCookieManager(webView)

        // ✅ WebChromeClient for popup handling
        webView.webChromeClient = createBrokerCompatibleChromeClient(holder, id)

        // ✅ WebViewClient for navigation handling
        webView.webViewClient = createBrokerCompatibleWebClient(id)

        // ✅ Touch handling
        webView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && !isDestroying) {
                requestOverlayFocus(id)
            }
            false
        }

        // ✅ Layout setup
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            topMargin = dpToPx(HEADER_HEIGHT_DP)
        }

        holder.overlay.removeAllViews()
        holder.overlay.addView(webView, lp)
        holder.webView = webView

        // ✅ Load URL with delay to ensure setup completes
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isDestroying) {
                webView.loadUrl(url)
            }
        }, 300L)
    }

    // ✅ Proper Cookie Manager Configuration
    private fun setupCookieManager(webView: WebView) {
        val cookieManager = CookieManager.getInstance()

        // Enable cookies globally
        cookieManager.setAcceptCookie(true)

        // Enable third-party cookies (CRITICAL for OAuth)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }

        // Clear old session cookies that might conflict
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeSessionCookies(null)
        }

        // Force flush to disk
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.flush()
        } else {
            @Suppress("DEPRECATION")
            CookieSyncManager.getInstance().sync()
        }
    }

    // ✅ WebChromeClient that properly handles OAuth popups
    private fun createBrokerCompatibleChromeClient(holder: OverlayHolder, id: String): WebChromeClient {
        return object : WebChromeClient() {

            override fun onPermissionRequest(request: PermissionRequest) {
                // Grant all permissions for the request
                request.grant(request.resources)
            }

            // Track popup -> container (full view), and popup -> minimized button
            private val popupWindows = mutableMapOf<WebView, ViewGroup>()
            private val popupMinimizedButton = mutableMapOf<WebView, View>()
            private val originalLayoutParams = mutableMapOf<WebView, ViewGroup.LayoutParams>()

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                Log.d("BrokerAuth", "onCreateWindow called - userGesture: $isUserGesture")

                // Create popup WebView
                val popupWebView = WebView(view.context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                        userAgentString = view.settings.userAgentString
                    }
                }

                // Setup cookie manager for popup (reuse your existing function)
                setupCookieManager(popupWebView)

                // Popup WebViewClient - handles redirects
                popupWebView.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(v: WebView, url: String?, favicon: Bitmap?) {
                        Log.d("BrokerAuth", "Popup loading: $url")
                        super.onPageStarted(v, url, favicon)
                    }

                    override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url.toString()
                        Log.d("BrokerAuth", "Popup navigation: $url")

                        // Handle OAuth callback
                        if (url.contains("tradingview.com/broker-callback", ignoreCase = true)) {
                            Log.d("BrokerAuth", "✅ OAuth callback detected in popup")
                            val code = request.url.getQueryParameter("code")
                            val state = request.url.getQueryParameter("state")
                            Log.d("BrokerAuth", "Auth code: $code, state: $state")

                            // Close popup and reload main view after a short delay
                            Handler(Looper.getMainLooper()).postDelayed({
                                closePopup(popupWebView)
                                holder.webView.reload()
                            }, 500)
                            return true
                        }

                        // OAuth redirect
                        if (url.contains("tradingview.com/trading/oauth-redirect", ignoreCase = true)) {
                            Log.d("BrokerAuth", "OAuth redirect in popup")
                            v.loadUrl(url)
                            return true
                        }

                        return false
                    }
                }

                // Create popup container (full-size)
                val popupContainer = FrameLayout(view.context).apply {
                    setBackgroundColor(Color.WHITE)
                    elevation = dpToPx(16).toFloat()

                    // Add the popup WebView filling the container
                    addView(
                        popupWebView,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )

                    // Close button (top-right)
                    val closeBtn = Button(view.context).apply {
                        text = "✕"
                        setOnClickListener { closePopup(popupWebView) }
                        layoutParams = FrameLayout.LayoutParams(
                            dpToPx(48),
                            dpToPx(48),
                            Gravity.TOP or Gravity.END
                        ).apply { setMargins(dpToPx(8), dpToPx(8), dpToPx(8), 0) }
                    }

                    // Minimize button (top-left)
                    val minimizeBtn = Button(view.context).apply {
                        text = "-"
                        setOnClickListener { minimizePopup(popupWebView) }
                        layoutParams = FrameLayout.LayoutParams(
                            dpToPx(48),
                            dpToPx(48),
                            Gravity.TOP or Gravity.START
                        )
                    }


                    addView(minimizeBtn)
                    addView(closeBtn)
                }

                // Add popup to overlay and save references
                holder.overlay.addView(
                    popupContainer,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                popupWindows[popupWebView] = popupContainer
                originalLayoutParams[popupWebView] = popupContainer.layoutParams

                // Return the popup WebView via the transport
                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = popupWebView
                resultMsg.sendToTarget()

                Log.d("BrokerAuth", "Popup window created successfully")
                return true
            }

            override fun onCloseWindow(window: WebView) {
                Log.d("BrokerAuth", "onCloseWindow called")
                closePopup(window)
            }

            /**
             * Minimizes the popup: hides the full container and adds a small draggable restore button.
             */
//            private fun minimizePopup(popupWebView: WebView) {
//                // Ensure this runs on UI thread
//                popupWebView.post {
//                    try {
//                        val container = popupWindows[popupWebView] ?: return@post
//                        // If already minimized, do nothing
//                        if (popupMinimizedButton.containsKey(popupWebView)) return@post
//
//                        // Hide the container (keep it attached so we can restore quickly)
//                        container.visibility = View.GONE
//
//                        // Create a small floating restore button
//                        val miniSize = dpToPx(64)
//                        val miniBtn = FrameLayout(popupWebView.context).apply {
//                            setBackgroundResource(android.R.drawable.btn_default) // platform look
//                            layoutParams = FrameLayout.LayoutParams(miniSize, miniSize).apply {
//                                // start near top-left; user can drag it later
//                                leftMargin = dpToPx(16)
//                                topMargin = dpToPx(100)
//                            }
//                            val arrow = TextView(context).apply {
//                                text = "▶"
//                                textSize = 20f
//                                gravity = Gravity.CENTER
//                                layoutParams = FrameLayout.LayoutParams(
//                                    FrameLayout.LayoutParams.MATCH_PARENT,
//                                    FrameLayout.LayoutParams.MATCH_PARENT
//                                )
//                            }
//                            addView(arrow)
//                            // restore on click
//                            setOnClickListener {
//                                restorePopup(popupWebView)
//                            }
//                            // allow dragging
//                            var dX = 0f
//                            var dY = 0f
//                            setOnTouchListener { v, event ->
//                                when (event.actionMasked) {
//                                    MotionEvent.ACTION_DOWN -> {
//                                        dX = v.x - event.rawX
//                                        dY = v.y - event.rawY
//                                        true
//                                    }
//                                    MotionEvent.ACTION_MOVE -> {
//                                        val newX = event.rawX + dX
//                                        val newY = event.rawY + dY
//                                        v.animate().x(newX).y(newY).setDuration(0).start()
//                                        true
//                                    }
//                                    else -> false
//                                }
//                            }
//                        }
//
//                        // Add mini button to overlay
//                        holder.overlay.addView(
//                            miniBtn,
//                            FrameLayout.LayoutParams(
//                                miniSize,
//                                miniSize
//                            )
//                        )
//
//                        popupMinimizedButton[popupWebView] = miniBtn
//
//                        Log.d("BrokerAuth", "Popup minimized (miniBtn added)")
//                    } catch (e: Exception) {
//                        Log.e("BrokerAuth", "Error minimizing popup: ${e.message}", e)
//                    }
//                }
//            }
//
//            /**
//             * Restores a minimized popup: removes the mini button and shows the full container again.
//             */
//            private fun restorePopup(popupWebView: WebView) {
//                popupWebView.post {
//                    try {
//                        val container = popupWindows[popupWebView] ?: return@post
//                        // Remove mini button if exists
//                        val miniBtn = popupMinimizedButton.remove(popupWebView)
//                        miniBtn?.let { (it.parent as? ViewGroup)?.removeView(it) }
//
//                        // Show container again
//                        container.visibility = View.VISIBLE
//
//                        Log.d("BrokerAuth", "Popup restored")
//                    } catch (e: Exception) {
//                        Log.e("BrokerAuth", "Error restoring popup: ${e.message}", e)
//                    }
//                }
//            }
//
            private fun showRestoreButton(holder: OverlayHolder, popupWebView: WebView) {
                val restoreBtn = Button(holder.overlay.context).apply {
                    text = "↗"
                    layoutParams = FrameLayout.LayoutParams(
                        dpToPx(48),
                        dpToPx(48),
                        Gravity.BOTTOM or Gravity.END
                    ).apply {
                        marginEnd = dpToPx(16)
                        bottomMargin = dpToPx(16)
                    }
                    setOnClickListener {
                        restorePopup(popupWebView)
                        holder.overlay.removeView(this) // remove restore button once restored
                    }
                }
                holder.overlay.addView(restoreBtn)
            }

            private fun minimizePopup(popupWebView: WebView) {
                popupWindows[popupWebView]?.visibility = View.GONE
                showRestoreButton(holder, popupWebView)
            }

            private fun restorePopup(popupWebView: WebView) {
                popupWindows[popupWebView]?.visibility = View.VISIBLE
            }

            /**
             * Fully close and cleanup popup
             */
            private fun closePopup(popupWebView: WebView) {
                popupWebView.post {
                    try {
                        // Remove minimized button if present
                        popupMinimizedButton.remove(popupWebView)?.let { btn ->
                            (btn.parent as? ViewGroup)?.removeView(btn)
                        }

                        val container = popupWindows.remove(popupWebView)
                        container?.let {
                            (it.parent as? ViewGroup)?.removeView(it)
                        }

                        // Destroy the WebView safely
                        try {
                            popupWebView.stopLoading()
                        } catch (_: Exception) {}
                        try {
                            popupWebView.loadUrl("about:blank")
                        } catch (_: Exception) {}
                        try {
                            popupWebView.clearHistory()
                            popupWebView.removeAllViews()
                            popupWebView.destroy()
                        } catch (_: Exception) {}

                        Log.d("BrokerAuth", "Popup closed and cleaned up")
                    } catch (e: Exception) {
                        Log.e("BrokerAuth", "Error closing popup: ${e.message}", e)
                    }
                }
            }

            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                Log.d("WebViewJS", "${msg.message()} -- Line ${msg.lineNumber()}")
                return true
            }
        }
    }

    // ✅ WebViewClient that handles broker authentication flow
    private fun createBrokerCompatibleWebClient(id: String): WebViewClient {
        return object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                Log.d("BrokerAuth", "Main navigation: $url")

                // ✅ Handle broker signin redirects
                if (url.contains("/trading/signin/binance", ignoreCase = true)) {
                    Log.d("BrokerAuth", "Broker signin detected - loading in same view")
                    view.loadUrl(url)
                    return true
                }

                // ✅ Handle broker-connect URLs
                if (url.contains("tradingview.com/broker-connect/binance", ignoreCase = true)) {
                    Log.d("BrokerAuth", "Broker connect URL - loading normally")
                    return false // Let WebView handle it (will trigger onCreateWindow if needed)
                }

                // ✅ Don't intercept OAuth redirects - let them complete naturally
                if (url.contains("tradingview.com/trading/oauth-redirect", ignoreCase = true)) {
                    Log.d("BrokerAuth", "OAuth redirect - allowing default handling")
                    return false
                }

                // ✅ Handle final callback (only if in main view)
                if (url.contains("tradingview.com/broker-callback", ignoreCase = true)) {
                    Log.d("BrokerAuth", "Broker callback in main view")

                    // Let TradingView's JS handle the auth code
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!isDestroying) {
                            view.reload()
                        }
                    }, 1000)

                    return false // Let page load normally
                }

                return false // Default: let WebView handle
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                if (isDestroying) return

                Log.d("BrokerAuth", "Page finished: $url")

                // ✅ Force cookie sync after each page load
                CookieManager.getInstance().flush()

                // ✅ Inject helper scripts only on chart page
                if (url?.contains("tradingview.com/chart") == true) {
                    view.postDelayed({
                        if (!isDestroying) {
                            injectBrokerHelperScripts(view)
                        }
                    }, 1000)
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                // ⚠️ For production, implement proper SSL verification
                // For testing broker connection, proceed carefully
                if (error.primaryError == SslError.SSL_DATE_INVALID) {
                    handler.proceed()
                } else {
                    handler.cancel()
                }
            }
        }
    }

    // ✅ Helper scripts for broker connection (optional enhancement)
    private fun injectBrokerHelperScripts(webView: WebView) {
        val script = """
        (function() {
            console.log('[BrokerHelper] Injecting broker helper scripts');

            // Override window.open to handle it in same window if popup blocked
            const originalOpen = window.open;
            window.open = function(url, target, features) {
                console.log('[BrokerHelper] window.open called:', url);

                // Try original first
                const popup = originalOpen.call(window, url, target, features);

                // If popup blocked, navigate in same window
                if (!popup || popup.closed || typeof popup.closed === 'undefined') {
                    console.log('[BrokerHelper] Popup blocked, using same window');
                    window.location.href = url;
                    return { closed: false };
                }

                return popup;
            };

            // Monitor broker connection status
            const observer = new MutationObserver(() => {
                const brokerBtn = document.querySelector('[data-broker="binance"]');
                if (brokerBtn) {
                    const connected = brokerBtn.classList.contains('connected') ||
                                    brokerBtn.closest('.js-broker-connected');
                    console.log('[BrokerHelper] Binance status:', connected ? 'Connected' : 'Disconnected');
                }
            });

            observer.observe(document.body, { childList: true, subtree: true });

            console.log('[BrokerHelper] Scripts injected successfully');
        })();
    """.trimIndent()

        webView.evaluateJavascript(script, null)
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun injectAutoTimeframeScriptIfScreenOn(id: String,inject: Boolean) {
        Log.w("Overlay", "⚠️ injectAutoTimeframeScriptIfScreenOn: before return for id=$id")
        if (isDestroying) return

        val holder = overlaysById[id]
        if (holder == null) {
            Log.w("Overlay", "⚠️ injectAutoTimeframeScriptIfScreenOn: No holder found for id=$id")
            return
        }

        val view = holder.webView
        if (view == null) {
            Log.w("Overlay", "⚠️ injectAutoTimeframeScriptIfScreenOn: WebView null for id=$id")
            return
        }

        if (!screenOn) {
            Log.d("Overlay", "⏭️ Skipping auto-timeframe injection (alert enabled) for id=$id")
            return
        }
//        if(!screenOn && !holder.alertEnabled){
//            return
//        }

        val index = getindexofid(id)
//        if (holder.alertEnabled) return

//        val targetValue = when (index) {
//            0 -> "1"
//            1 -> "3"
//            2 -> "5"
//            3 -> "15"
//            4 -> "30"
//            5 -> "60"
//            6 -> "120"
//            7 -> "180"
//            8 -> "240"
//            9 -> "1D"
//           10 -> "1W"
//           11 -> "1M"
//            else -> null
//        }

        val targetValuelabel = when (holder.timeFrame) {
           "1"  -> "1 minute"
           "3"  -> "3 minutes"
           "5"  -> "5 minutes"
           "15" -> "15 minutes"
           "30" -> "30 minutes"
           "60" -> "1 hour"
           "120"-> "2 hours"
           "180"-> "3 hours"
           "240"-> "4 hours"
           "1D" -> "1 day"
           "1W"  -> "1 week"
           "1M"  -> "1 month"
            else -> null
        }

        Log.w("Overlay", "⚠️ injectAutoTimeframeScript: for id=$id $targetValuelabel ${holder.timeFrame}")
//        if (targetValue == null) return

        val jsClick = """
(function() {
    try {
        let attempts = 0;
        const maxAttempts = 10;
        const targetValue = "${holder.timeFrame}";
        const targetLabel = "${targetValuelabel}";

        function tryClick() {
            const btn = document.querySelector(`button[data-value='${'$'}{targetValue}']`) ||
                        document.querySelector(`button[aria-label='${'$'}{targetLabel}']`);
            if (btn) {
                btn.click();
                console.log("✅ Clicked timeframe", targetValue);
                setTimeout(() => {
                    try {
                        const resetBtn = document.querySelector(".js-btn-reset");
                        if (resetBtn) {
                            const evt2 = new MouseEvent("click", {
                                bubbles: true,
                                cancelable: true,
                                view: window,
                                isTrusted: true
                            });
                            resetBtn.dispatchEvent(evt2);
                            console.log("✅ Clicked: Reset frame button");
                        }
                    } catch (err) {
                        console.log("Reset button click error:", err);
                    }
                }, 2000);
            } else if (attempts++ < maxAttempts) {
                setTimeout(tryClick, 500);
            }
        }
        tryClick();
  // run again after 5 seconds
    setTimeout(() => {
        console.log("🔁 Retrying click after 5 seconds...");
        attempts = 0;  // reset attempts
        tryClick();
    }, 5000);
    } catch(e) {
        console.log("Auto-click error:", e);
    }

    

})();
""".trimIndent()

        val restoreConnectionjs = """// Restore connection watcher
    function autoClickRestore() {
        try {
            const buttons = document.querySelectorAll("button");
            for (const btn of buttons) {
                const text = (btn.innerText || "").trim();
                const tooltip = btn.getAttribute("data-overflow-tooltip-text") || "";
                if (text.includes("Restore connection") || tooltip.includes("Restore connection")) {
                    const evt = new MouseEvent("click", {
                        bubbles: true,
                        cancelable: true,
                        view: window,
                        isTrusted: true
                    });
                    setTimeout(() => btn.dispatchEvent(evt), 2500);
                    console.log("✅ Dispatched real click: Restore connection");
                    return;
                }
            }
        } catch (e) {
            console.log("Restore connection watcher error:", e);
        }
    }

    if (window.autoClickObserver) {
        window.autoClickObserver.disconnect();
    }
    window.autoClickObserver = new MutationObserver(() => autoClickRestore());
    window.autoClickObserver.observe(document.body, { childList: true, subtree: true });""".trimIndent()

        view.postDelayed({
            if (!isDestroying && screenOn) {
               if (inject) {
                   if (holder.timeFrame != null) view.evaluateJavascript(jsClick, null)
                   view.evaluateJavascript(restoreConnectionjs, null)
               }else{
                   view.evaluateJavascript(restoreConnectionjs,null)
                   Log.d("restore", "✅ JS restore injected after delay for $id")

               }
                Log.d("AutoFrame", "✅ JS injected after delay for $id")
            }
        }, 700L)
    }

    private fun pauseWebViewIfNoAlert(id: String) {
        val holder = overlaysById[id] ?: return
        if (!holder.alertEnabled) {
            holder.webView.onPause()
            holder.webView.pauseTimers()
            holder.webView.evaluateJavascript("if(window.autoClickObserver){window.autoClickObserver.disconnect();}", null)
            Log.d("Overlay", "⏸️ WebView paused for $id (no alerts)")
        }
    }
    private fun resumeWebView(id: String) {
        val holder = overlaysById[id] ?: return
        if (!holder.alertEnabled) {
            holder.webView.onResume()
            holder.webView.resumeTimers()
            holder.webView.evaluateJavascript("window.startAutoClickObserver();", null)
            Log.d("Overlay", "▶️ WebView resumed for $id")
        }
    }

    var isRunningAlertOverlay = false
    @SuppressLint("ClickableViewAccessibility")
    private fun showSetAlertOverlay(id: String) {
        if (isDestroying) return
        if (isRunningAlertOverlay) return else isRunningAlertOverlay = true

        val holder = overlaysById[id] ?: return
        val overlayContainer = holder.overlay
        val webView = holder.webView

        var symbol = "Loading..."
        var currentPrice = 0.0

        // Create a RelativeLayout as the container for absolute positioning
        val cardContainer = RelativeLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(120), // Tiny width like original
                dpToPx(130), // Compact height
                Gravity.START
            )
        }
        cardContainer.x = 40f
        cardContainer.y = 260f
        // Your existing card layout (now a child of cardContainer)
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(6)) // Minimal padding
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.WHITE)
                setStroke(1, Color.LTGRAY)
            }
            elevation = dpToPx(4).toFloat()
            // Set to match parent so it fills the cardContainer
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.START or Gravity.TOP

            }
        }

        // Create the close button with absolute positioning
        val cutBtn = Button(this).apply {
            text = "✕"
            textSize = 12f // Smaller for compact design
            background = null
            includeFontPadding = false
            minimumHeight = 0
            minimumWidth = 0
            setTextColor(Color.GRAY)
            setOnClickListener { overlayContainer.removeView(cardContainer); isRunningAlertOverlay = false }

            // Set layout parameters for absolute positioning
            layoutParams = RelativeLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                // Add small margins to position it nicely in the corner
                setMargins(0, dpToPx(-4), dpToPx(2), 0)
            }
        }

        // Add the card layout to the container
        cardContainer.addView(cardLayout)
        // Add the close button to the container (it will be positioned absolutely)
        cardContainer.addView(cutBtn)

        // Title text - small and compact
        val titleText = TextView(this).apply {
            text = "Alert"
            textSize = 11f // Tiny font
            setTextColor(Color.DKGRAY)
            setTypeface(null, Typeface.BOLD)
            setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(80),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(2))
            }
        }
        cardLayout.addView(titleText)

        val recommendationMap = mapOf(
            "8" to listOf(
                "MT-1-RB", "MT-1-OB", "FVG-MT-1", "MT-1-FP",
                "MT-1-X", "MT-1-BOS", "MT-1-CHOCH"
            ),
            "7" to listOf(
                "W-1-RB", "W-1-OB", "FVG-W-1", "W-1-FP",
                "W-1-X", "W-1-BOS", "W-1-CHOCH"
            ),
            "6" to listOf(
                "D-1-RB", "D-1-OB", "FVG-D-1", "D-1-FP",
                "D-1-X", "D-1-BOS", "D-1-CHOCH"
            ),
            "5" to listOf(
                "H-4-RB", "H-4-OB", "FVG-H-4", "H-4-FP",
                "H-4-X", "H-4-BOS", "H-4-CHOCH"
            ),
            "4" to listOf(
                "H-1-RB", "H-1-OB", "FVG-H-1", "H-1-FP",
                "H-1-X", "H-1-BOS", "H-1-CHOCH"
            ),
            "3" to listOf(
                "M-15-RB", "M-15-OB", "FVG-M-15", "M-15-FP",
                "M-15-X", "M-15-BOS", "M-15-CHOCH"
            ),
            "2" to listOf(
                "M-5-RB", "M-5-OB", "FVG-M-5", "M-5-FP",
                "M-5-X", "M-5-BOS", "M-5-CHOCH"
            ),
            "1" to listOf(
                "M-1-RB", "M-1-OB", "FVG-M-1", "M-1-FP",
                "M-1-X", "M-1-BOS", "M-1-CHOCH"
            ),
            "." to listOf("SL","Entry","TP 1","TP 2"),
            "000" to listOf("M-1-CISD","M-3-CISD","M-5-CISD","M-15-CISD","H-1-CISD","H-4-CISD"),
            "0" to listOf(
                "M-15-BOS",
                "M-15-BOS-SWEEP",
                "M-15-CHOCH",
                "M-15-CHOCH-SWEEP",
                "M-15-Fake-BOS",
                "M-15-Fake-CHOCH",
                "M-15-MMS",
                "M-15-X",
                "H-4-BOS","H-4-BOS-SWEEP","H-4-CHOCH",
                "H-4-CHOCH-SWEEP",
                "H-4-Fake-BOS",
                "H-4-Fake-CHOCH",
                "H-4-MMS",
                "H-4-X",
            ),
            "00" to listOf(
                "1-BUY-ZONE-H-4",
                "1-BUY-ZONE-M-15",
                "1-SELL-ZONE-H-4",
                "1-SELL-ZONE-M-15",
                "2-BUY-ZONE-H-4",
                "2-BUY-ZONE-M-15",
                "2-SELL-ZONE-H-4",
                "2-SELL-ZONE-M-15",
                "3-BUY-ZONE-H-4",
                "3-BUY-ZONE-M-15",
                "3-SELL-ZONE-H-4",
                "3-SELL-ZONE-M-15"
            )
        )

        val nameInput = AutoCompleteTextView(this).apply {
            hint = "Name"
            textSize = 11f
            setText(symbol)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setPadding(dpToPx(4))
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(4).toFloat()
                setStroke(1, Color.LTGRAY)
                setColor(Color.parseColor("#F8F8F8"))
            }
            threshold = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
        }
        cardContainer.id = View.generateViewId()

// Force dropdown to attach to the same window as overlay
        nameInput.dropDownAnchor = cardContainer.id
        nameInput.dropDownWidth = dpToPx(110) // match overlay width
        nameInput.dropDownVerticalOffset = dpToPx(2)

// Create manual dropdown ListView for overlay-safe suggestion display
        val suggestionList = ListView(this).apply {
            visibility = View.GONE
            divider = ColorDrawable(Color.LTGRAY)
            dividerHeight = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(80)
            ).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
        }
        cardLayout.addView(suggestionList)

// Adapter for suggestions
        val suggestionAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )
        suggestionList.adapter = suggestionAdapter

// Show list when typing key 1–8
        nameInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val key = s?.toString()?.trim()
                val list = recommendationMap[key]
                if (list != null) {
                    suggestionAdapter.clear()
                    suggestionAdapter.addAll(list)
                    suggestionAdapter.notifyDataSetChanged()
                    suggestionList.visibility = View.VISIBLE
                } else {
                    suggestionList.visibility = View.GONE
                }
            }
        })

// When user selects one
        suggestionList.setOnItemClickListener { _, _, position, _ ->
            val selected = suggestionAdapter.getItem(position)
            nameInput.setText(selected)
            suggestionList.visibility = View.GONE
        }

        // Target Price Input – made tiny
        val targetPriceInput = EditText(this).apply {
            hint = "Price"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            textSize = 11f  // Tiny font
            setPadding(dpToPx(4)) // Minimal padding
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(4).toFloat()
                setStroke(1, Color.LTGRAY)
                setColor(Color.parseColor("#F8F8F8"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
        }

        // Action Button – made tiny
        val actionBtn = Button(this).apply {
            text = "Set"
            textSize = 11f // Tiny font
            isEnabled = false
            setTextColor(Color.WHITE)
//            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2)) // Minimal padding
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(4).toFloat()
                setColor(Color.GRAY)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(32)
            ).apply {
                setMargins(0, dpToPx(8), 0, 0)
            }
        }

        // Add views to cardLayout
        cardLayout.addView(nameInput)
        cardLayout.addView(targetPriceInput)
        cardLayout.addView(actionBtn)

        // Add the container to the overlay
        overlayContainer.addView(cardContainer)

        cardLayout.postDelayed({
            targetPriceInput.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(targetPriceInput, InputMethodManager.SHOW_IMPLICIT)
        }, 100)

        fun updateSymbolPrice(symbolVal: String, priceVal: Double) {
            symbol = symbolVal
            currentPrice = priceVal
            nameInput.setText(symbol)
            targetPriceInput.setText(priceVal.toString())
            actionBtn.text = "Set"
            actionBtn.isEnabled = true
            actionBtn.background = GradientDrawable().apply {
                cornerRadius = dpToPx(4).toFloat()
                setColor(Color.parseColor("#4CAF50"))
            }
        }

        val lastSymbol = latestSymbols[id]
        val lastPrice = latestPrices[id]
        if (!lastSymbol.isNullOrBlank() && lastPrice != null && lastPrice > 0) {
            updateSymbolPrice(lastSymbol, lastPrice)
        } else {
            webView.evaluateJavascript("document.title") { title ->
                val clean = title.trim('"').replace("\\n", "").trim()
                val matchSymbol = Regex("^[A-Z0-9.:-]+").find(clean)?.value
                val matchPrice = Regex("[\\d,]+\\.\\d+").find(clean)?.value?.replace(",", "")?.toDoubleOrNull()
                if (!matchSymbol.isNullOrBlank() && matchPrice != null) {
                    updateSymbolPrice(matchSymbol, matchPrice)
                }
            }
        }

        targetPriceInput.addTextChangedListener { text ->
            val empty = text.isNullOrBlank()
            actionBtn.text = if (empty) "Canc" else "Set"
            actionBtn.background = GradientDrawable().apply {
                cornerRadius = dpToPx(4).toFloat()
                setColor(if (empty) Color.RED else Color.parseColor("#4CAF50"))
            }
        }

        actionBtn.setOnClickListener {
            val targetText = targetPriceInput.text.toString()
            if (targetText.isBlank()) {
                overlayContainer.removeView(cardContainer)
                return@setOnClickListener
            }

            val target = targetText.toDoubleOrNull()
            if (target == null) {
                Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = nameInput.text.toString().ifEmpty { symbol }
            val type = if (currentPrice < target) AlertType.ABOVE else AlertType.BELOW
            val newAlertId = UUID.randomUUID().toString()
            val AlertFor = if (symbol.contains(".P")) AlertFor.USDM else AlertFor.SPOT
            val newAlert = PriceAlert(
                newAlertId,
                name = name,
                symbol = symbol.replace(".P", ""),
                threshold = target,
                type = type,
                alertFor = AlertFor
            )

            DataStorage.addAlert(newAlert)
            AlertSavedOnPage.getOrPut(id) { mutableSetOf() }.add(newAlertId)

            val map = activeAlertsState.value
            val set = map.getOrPut(symbol) { mutableSetOf() }
            set.add(newAlert)
            activeAlertsState.value = map

            Toast.makeText(this, "$name $type $target", Toast.LENGTH_SHORT).show()
            isRunningAlertOverlay = false
            overlayContainer.removeView(cardContainer)
        }

        // Dragging functionality (updated to use cardContainer)
        var dX = 0f
        var dY = 0f
        cardContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = event.rawX - cardContainer.x
                    dY = event.rawY - cardContainer.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    cardContainer.x = event.rawX - dX
                    cardContainer.y = event.rawY - dY
                    true
                }
                else -> false
            }
        }
    }

    var riskammount: Double = 0.0
    var riskpersantage: Double = 0.0

    var amount: Double = 0.0
    var percentage: Double = 0.0
    var isRuninigCalsRisk = false
    var isRuninigCalsPer = false

    // Function Definition (Should also be aligned at the class level)
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n", "SuspiciousIndentation")
    private fun showSimpleCalcOverlay(id: String, Cals:Int) {
        if (isDestroying) return

        when(Cals) {
            0 -> if (isRuninigCalsRisk) return else isRuninigCalsRisk = true
            1 -> if (isRuninigCalsPer) return else isRuninigCalsPer = true
        }

        val holder = overlaysById[id] ?: return
        val overlayContainer = holder.overlay

        // Main container with shadow
        val cardContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(90),
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
            }
            setPadding(dpToPx(4))
            background = createCardBackground()
            elevation = dpToPx(2).toFloat()
        }
        cardContainer.x = 40f
        cardContainer.y = 260f
        // Content layout
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1))
            }
        }

        // Header with close button
        val headerView = RelativeLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(26)
            )
            setBackgroundColor(Color.parseColor("#F5F5F5"))

            // Drag indicator
            View(this@FloatingBrowserService).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    dpToPx(24),
                    dpToPx(4)
                ).apply {
                    addRule(RelativeLayout.CENTER_VERTICAL)
                    setMargins(dpToPx(12), 0, 0, 0)
                }
                background = createRoundedDrawable(Color.DKGRAY, dpToPx(2).toFloat())
            }.let { addView(it) }
        }

        // Close button (using text instead of icon)
        val closeBtn = TextView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                dpToPx(36),
                dpToPx(36)
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            text = "×"
            gravity = Gravity.CENTER
            textSize = 24f
            setTextColor(Color.GRAY)
            background = createSelectorDrawable()
            setOnClickListener { overlayContainer.removeView(cardContainer)
                when(Cals) {
                    0 -> isRuninigCalsRisk = false
                    1 -> isRuninigCalsPer = false
                }
            }
        }
        headerView.addView(closeBtn)

        // Input fields container
        val inputContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
        }

        // Input field helper
        fun createInputField(hint: String, value: Double): EditText {
            return EditText(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(20)
                ).apply {
                    setMargins(0, dpToPx(2), 0, 0)
                }
                this.hint = hint
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or
                        InputType.TYPE_NUMBER_FLAG_SIGNED
                textSize = 12f
                setText(String.format("%.2f", value))
                background = createInputBackground()
                setPadding(dpToPx(2), 0, dpToPx(2), 0)
                gravity = Gravity.CENTER_VERTICAL
            }
        }
//        val textCal = when(Cals){
//            0 -> "CALCULATE Risk"
//            1 -> "CALCULATE %"
//            else -> {null}
//        }
        val AmountInput = createInputField(if (Cals == 0) "Risk Amount" else "Amount",  if(Cals == 0 ) riskammount else amount)
        val PercentInput = createInputField("%", if(Cals == 0 ) riskpersantage else percentage)

        // Result view
        val resultView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(36)
            ).apply {
                setMargins(0, dpToPx(8), 0, 0)
            }
            text = ""
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.BLACK)
            background = createResultBackground()
        }
        val textCal = when(Cals){
            0 -> "CALCULATE Risk"
            1 -> "CALCULATE %"
            else -> {null}
        }
        // Calculate button
        val calcButton = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(32)
            ).apply {
                setMargins(0, dpToPx(8), 0, 0)
            }
            text = textCal
            textSize = 8f
            setTextColor(Color.WHITE)
            background = createButtonBackground()
            setOnClickListener {
                fun EditText.toDoubleOrZero(): Double = text.toString().toDoubleOrNull() ?: 0.0

                when(Cals) {
                   0 -> {
                       riskammount = AmountInput.toDoubleOrZero(); riskpersantage =
                           PercentInput.toDoubleOrZero()
                   }
                   1 -> {amount = AmountInput.toDoubleOrZero();  percentage = PercentInput.toDoubleOrZero()}
                }

                val result = if (riskpersantage != 0.0 || amount != 0.0) {
                    when (Cals) {
                        0 -> riskammount * 100 / riskpersantage
                        1 -> amount * (percentage / 100)
                        else -> {}
                    }

                } else 0.0
                resultView.text = "Result: ${"%.2f".format(result)}\$"
            }
        }

        // Add views to containers
        inputContainer.addView(AmountInput)
        inputContainer.addView(PercentInput)
        inputContainer.addView(resultView)
        inputContainer.addView(calcButton)

        contentLayout.addView(headerView)
        contentLayout.addView(inputContainer)
        cardContainer.addView(contentLayout)
        overlayContainer.addView(cardContainer)

        // Dragging functionality
        var dX = 0f
        var dY = 0f

        headerView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = cardContainer.x - event.rawX
                    dY = cardContainer.y - event.rawY
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    cardContainer.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick()
                    true
                }
                else -> false
            }
        }
    }


    // Helper functions for creating drawables
    private fun createCardBackground(): Drawable {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = dpToPx(8).toFloat()
        shape.setColor(Color.WHITE)
        shape.setStroke(dpToPx(1), Color.LTGRAY)
        return shape
    }

    private fun createRoundedDrawable(color: Int, cornerRadius: Float): Drawable {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = cornerRadius
        shape.setColor(color)
        return shape
    }

    private fun createInputBackground(): Drawable {
        val normal = GradientDrawable()
        normal.shape = GradientDrawable.RECTANGLE
        normal.cornerRadius = dpToPx(4).toFloat()
        normal.setStroke(dpToPx(1), Color.LTGRAY)
        normal.setColor(Color.WHITE)

        val focused = GradientDrawable()
        focused.shape = GradientDrawable.RECTANGLE
        focused.cornerRadius = dpToPx(4).toFloat()
        focused.setStroke(dpToPx(1), Color.BLUE)
        focused.setColor(Color.WHITE)

        val states = StateListDrawable()
        states.addState(intArrayOf(android.R.attr.state_focused), focused)
        states.addState(intArrayOf(), normal)
        return states
    }

    private fun createButtonBackground(): Drawable {
        val normal = GradientDrawable()
        normal.shape = GradientDrawable.RECTANGLE
        normal.cornerRadius = dpToPx(4).toFloat()
        normal.setColor(Color.parseColor("#4CAF50"))

        val pressed = GradientDrawable()
        pressed.shape = GradientDrawable.RECTANGLE
        pressed.cornerRadius = dpToPx(4).toFloat()
        pressed.setColor(Color.parseColor("#388E3C"))

        val states = StateListDrawable()
        states.addState(intArrayOf(android.R.attr.state_pressed), pressed)
        states.addState(intArrayOf(), normal)
        return states
    }

    private fun createResultBackground(): Drawable {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = dpToPx(4).toFloat()
        shape.setStroke(dpToPx(1), Color.LTGRAY)
        shape.setColor(Color.parseColor("#F0FFF0"))
        return shape
    }

    private fun createSelectorDrawable(): Drawable {
        val normal = ColorDrawable(Color.TRANSPARENT)
        val pressed = ColorDrawable(Color.parseColor("#20000000"))

        val states = StateListDrawable()
        states.addState(intArrayOf(android.R.attr.state_pressed), pressed)
        states.addState(intArrayOf(), normal)
        return states
    }





    private fun View.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
        val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
        params.setMargins(left, top, right, bottom)
        layoutParams = params
    }

    @SuppressLint("UseKtx")
    private fun setupHeader(id: String) {
        val holder = overlaysById[id] ?: return
        val overlay = holder.overlay
//        CrashLogger.log(this, "FloatingService", "[setup header] for id=$id")

        val headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor("#FFBB86FC".toColorInt())
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            elevation = 8f
        }

        // All button actions by lambda using id!
        val buttons = listOf(
            "✕" to { removeContainer(id) },
            "🔄️" to {safeReloadWebView(id)},
            "🔇" to {
              if (this@FloatingBrowserService.isServiceRunning(PriceAlertService::class.java)) this@FloatingBrowserService.startService(Intent(this@FloatingBrowserService, PriceAlertService::class.java).apply { action = "com.example.action.STOP_SOUND"; putExtra("FromUi",true) })
//                if (mediaPlayer?.isPlaying == true) stopAlertSound()
                    },
            "●" to { if (overlayBoxWindow == null && attachedInBox.isEmpty()) {
                overlay.visibility = View.GONE; switchToState(STATE_MINI, id)
            } else restoreToListBox(id) },
            "🎯" to { showSetAlertOverlay(id) },
            "🗓️" to { showSimpleCalcOverlay(id,1)},
            "🧮" to { showSimpleCalcOverlay(id,0)},
            "💹" to {
                try {
                val intent = packageManager.getLaunchIntentForPackage("com.binance.dev")
                if (intent != null) {startActivity(intent);restoreToListBox(id)}else Toast.makeText(this, "Binance app not found", Toast.LENGTH_SHORT).show()
              } catch (e: Exception) {
                Toast.makeText(this, "Cannot open Binance app", Toast.LENGTH_SHORT).show()
             } },
            "5️⃣" to {
                try {
                    val intent = packageManager.getLaunchIntentForPackage("net.metaquotes.metatrader5")
                    if (intent != null) {startActivity(intent);restoreToListBox(id)}else Toast.makeText(this, "meta trader 5 app not found", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot open meta trader 5 app", Toast.LENGTH_SHORT).show()
                } },
            "▭" to {
                restoreToListBox(id)
            }
        )

        buttons.forEach { (label, action) ->
            headerBar.addView(createStyledHeaderButton(label) { if (!isDestroying) action() })
        }

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dpToPx(HEADER_HEIGHT_DP)
        ).apply {
            gravity = Gravity.TOP
        }

        overlay.addView(headerBar, lp)
        holder.headerBar = headerBar // If you want, reassign to OverlayHolder for direct lookup.
    }
    private fun restoreToListBox(id: String){
        val holder = overlaysById[id]
        val overlay = holder?.overlay


        if (overlayBoxWindow != null) {
            overlay?.post {
                try {
                    safeDetachView(id)
                    val box = overlayBoxWindow ?: return@post
                    val scroll = box.findViewById<ScrollView>(android.R.id.content)
                    val inner = (scroll?.getChildAt(0) as? LinearLayout)
                        ?: (box.getChildAt(1) as? ScrollView)?.getChildAt(0) as? LinearLayout
                        ?: return@post

                    val wrapper = savedBoxWrappers[id] ?: FrameLayout(this).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(220)
                        ).apply { bottomMargin = dpToPx(8) }
                        background = GradientDrawable().apply {
                            setColor(Color.WHITE)
                            setStroke(1, Color.LTGRAY)
                            cornerRadius = dpToPx(8).toFloat()
                        }
                        elevation = dpToPx(3).toFloat()
                    }.also { savedBoxWrappers[id] = it }

                    if (wrapper.parent == null) {
                        inner.addView(wrapper)
                    }

                    wrapper.removeAllViews()
                    overlay.layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    wrapper.addView(overlay)
                    setupBubble(id)

                    holder.headerBar.visibility = View.GONE
                    holder.bubble.visibility = View.VISIBLE
                    holder.webView.setInitialScale(100)
                    (holder.webView.layoutParams as? FrameLayout.LayoutParams)?.let {
                        it.topMargin = 0
                        holder.webView.layoutParams = it
                    }
                    releaseOverlayFocus(id)
                    overlay.requestLayout()

                    attachedInBox.add(id)

                    Log.d("FloatingService", "✅ Overlay $id reattached to side box")
        //                            CrashLogger.log(this, "FloatingService", "✅ Overlay $id reattached to side box")
                } catch (e: Exception) {
                    Log.e("FloatingService", "Error reattaching overlay $id to box: ${e.message}")
        //                            CrashLogger.log(this, "FloatingService", "Error reattaching overlay $id to box: ${e.message}")
                }
            }
        } else {
            switchToState(STATE_MINI, id)
        }
    }
    private fun createStyledHeaderButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 20f
            setBackgroundColor("#FFBB86FC".toColorInt())
            setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dpToPx(3)
                marginEnd = dpToPx(3)
            }
            setOnClickListener {
                if (!isDestroying) onClick()
            }
        }
    }

    private fun getindexofid(id:String) : Int {
        val keys = overlaysById.keys.toList()
        val index = keys.indexOf(id)
        return index
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun setupBubble(id: String) {
        val holder = overlaysById[id] ?: return
        val overlay = holder.overlay

        val bubble = holder.bubble.apply {
            setBackgroundColor(Color.TRANSPARENT)
            alpha = 0.9f
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND

            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var dragging = false

            var lastTapTime = 0L
            val DOUBLE_TAP_TIMEOUT = 300L

            setOnTouchListener { v, event ->
                if (isDestroying) return@setOnTouchListener false

                val lp = overlay.layoutParams
                val isFloating = lp is WindowManager.LayoutParams

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (isFloating) {
                            initialX = (lp as WindowManager.LayoutParams).x
                            initialY = lp.y
                        }
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        dragging = false

                        if (!isFloating) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT) {
                                v.performClick()
                                lastTapTime = 0L
                            } else {
                                lastTapTime = currentTime
                            }
                        }
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()

                        if (abs(dx) > CLICK_THRESHOLD || abs(dy) > CLICK_THRESHOLD) {
                            dragging = true
                            lastTapTime = 0L

                            if (isFloating && !isDestroying) {
                                val params = lp as WindowManager.LayoutParams
                                params.x = initialX + dx
                                params.y = initialY + dy
                                try {
                                    windowManager?.updateViewLayout(overlay, params)
                                } catch (_: Exception) {}
                            }
                        }
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (!dragging && isFloating && !isDestroying) {
                            v.performClick()
                        }

                        if (isFloating && !isDestroying) {
                            val params = lp as WindowManager.LayoutParams
                            val lastX = params.x
                            val lastY = params.y

                            DataStorage.savePosition(getindexofid(id), lastX, lastY)
                            savedPositions[id] = Pair(lastX, lastY)
                        }
                        dragging = false
                        true
                    }

                    else -> false
                }
            }

            setOnClickListener {
                if (isDestroying) return@setOnClickListener

                val isFloating = overlay.layoutParams is WindowManager.LayoutParams
                if (isFloating) {
                    switchToState(
                        if (currentStates[id] == STATE_MINI) STATE_MAX else STATE_MINI,
                        id
                    )
                } else {
                    overlay.post {
                        try {
                            (overlay.parent as? ViewGroup)?.removeView(overlay)
                            val params = savedWindowParams[id] ?: defaultMiniParams(id)
                            safeAttachToWindow(id, params)
                            switchToState(STATE_MAX, id)
//                            restoreOverlay(id)
                            Log.d("FloatingService", "✅ Double-tap → overlay $id fullscreen")
//                            CrashLogger.log(this@FloatingBrowserService, "FloatingService", "✅ Double-tap → overlay $id fullscreen")
                            Toast.makeText(this@FloatingBrowserService, "Opening fullscreen", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e("FloatingService", "Double-tap expand error: ${e.message}")
//                            CrashLogger.log(this@FloatingBrowserService, "FloatingService", "Double-tap expand error: ${e.message}")
                        }
                    }
                }
            }
        }

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.CENTER
        }

        if (bubble.parent != overlay) {
            (bubble.parent as? ViewGroup)?.removeView(bubble)
            overlay.addView(bubble, lp)
        }
    }
    @SuppressLint("UseKtx")
    private fun switchToState(state: Int, id: String) {
        if (isDestroying) return
        Log.d("showall","from webOverlay state mini ${state}${id} intent show all")


        currentStates[id] = state
        val holder = overlaysById[id]
        val params = holder?.overlay?.layoutParams as? WindowManager.LayoutParams ?: return

        when (state) {
            STATE_MAX -> {
                bringOverlayToFront(id)
                requestOverlayFocus(id)
                Log.e("FloatingService", "Max runing..")

                params.width = WindowManager.LayoutParams.MATCH_PARENT
                params.height = WindowManager.LayoutParams.MATCH_PARENT
                params.gravity = Gravity.TOP or Gravity.START

                holder.headerBar.visibility = View.VISIBLE
                if (holder.overlay.indexOfChild(holder.bubble) != -1) {
                    holder.overlay.removeView(holder.bubble)
                }
                holder.overlay.visibility = View.VISIBLE
                holder.webView.setInitialScale(0)
                (holder.webView.layoutParams as? FrameLayout.LayoutParams)?.let {
                    it.topMargin = dpToPx(HEADER_HEIGHT_DP)
                    holder.webView.layoutParams = it
                }
            }

//            STATE_MEDIUM -> {
//                params.width = dpToPx(SIZE_MEDIUM_WIDTH_DP)
//                params.height = dpToPx(SIZE_MEDIUM_HEIGHT_DP)
//                params.gravity = Gravity.START or Gravity.TOP
//                val (x, y) = savedPositions[index] ?: Pair(0, 0)
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

            STATE_MINI -> {
                params.width = dpToPx(SIZE_MINI_WIDTH_DP)
                params.height = dpToPx(SIZE_MINI_HEIGHT_DP)
                params.gravity = Gravity.TOP or Gravity.START
                val (x, y) = savedPositions[id] ?: DataStorage.loadPosition(getindexofid(id))
                params.x = x
                params.y = y
                holder.headerBar.visibility = View.GONE


                if (holder.bubble.parent != holder.overlay) {
                    (holder.bubble.parent as? ViewGroup)?.removeView(holder.bubble)
                    holder.overlay.addView(holder.bubble)
                }
                holder.bubble.visibility = View.VISIBLE
                Log.d("showall","from webOverlay running mini intent show all")



                holder.webView.setInitialScale(100)
                (holder.webView.layoutParams as? FrameLayout.LayoutParams)?.let {
                    it.topMargin = 0
                    holder.webView.layoutParams = it
                }
                releaseOverlayFocus(id)
            }
        }
//        CrashLogger.log(this, "FloatingService", "🔄 Switching to state $state for id=$id")

        try {
            windowManager?.updateViewLayout(holder.overlay, params)
            holder.overlay.requestLayout()
        } catch (e: Exception) {
            Log.e("FloatingService", "Error switching state: ${e.message}")
//            CrashLogger.log(this, "FloatingService", "Error switching state: ${e.message} for id=$id")

        }
    }

    private fun bringOverlayToFront(id: String) {
        if (isDestroying) return

        val overlay = overlaysById[id]?.overlay
        val params = overlay?.layoutParams as? WindowManager.LayoutParams ?: return

        try {
            windowManager?.removeViewImmediate(overlay)
        } catch (_: IllegalArgumentException) {}

        try {
            windowManager?.addView(overlay, params)
        } catch (e: Exception) {
            Log.e("FloatingService", "Error bringing overlay to front: ${e.message}")
//            CrashLogger.log(this, "FloatingService", "Error bringing overlay to front: ${e.message} for id=$id")

        }
    }

    private fun removeContainer(id: String) {
        try {
            val holder = overlaysById.remove(id)
            holder?.webView?.let { webView ->
                try {
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.clearHistory()
                    webView.clearCache(true)
                    webView.onPause()
                    webView.removeAllViews()
                    webView.destroy()
                } catch (e: Exception) {
                    Log.e("FloatingService", "Error destroying WebView: ${e.message}")
//                    CrashLogger.log(this, "FloatingService", "Error destroying WebView: ${e.message}")
                }
            }

            holder?.overlay?.let { overlay ->
                try {
                    windowManager?.removeViewImmediate(overlay)
                } catch (e: Exception) {
                    Log.e("FloatingService", "Error removing overlay: ${e.message}")
//                    CrashLogger.log(this, "FloatingService", "Error removing overlay: ${e.message}")
                }
            }

            // Clean up header, bubble, wrapper
            (holder?.headerBar?.parent as? ViewGroup)?.removeView(holder.headerBar)
            (holder?.bubble?.parent as? ViewGroup)?.removeView(holder.bubble)
            (holder?.wrapper?.parent as? ViewGroup)?.removeView(holder.wrapper)

            // Clean up state/handlers
            handlersById.remove(id)?.removeCallbacksAndMessages(null)
            attachedInBox.remove(id)
            savedBoxWrappers.remove(id)
            savedWindowParams.remove(id)
            currentStates.remove(id)
            latestPrices.remove(id)
            latestSymbols.remove(id)

            stopAlertSound()

            // Optionally: check if ALL overlays are removed using overlaysById
            if (overlaysById.isEmpty()) {
                destroyServiceCompletely()
                removeControlPanel()
            }
        } catch (e: Exception) {
            Log.e("FloatingService", "Error in removeContainer: ${e.message}")
//            CrashLogger.log(this, "FloatingService", "Error in removeContainer: ${e.message}")
        }
    }

    private var controlPanel: LinearLayout? = null
    private var controlParams: WindowManager.LayoutParams? = null
    private var isExpanded = false

    @SuppressLint("ClickableViewAccessibility")
    private fun setupControlPanel() {
        if (controlPanel != null) return

        controlPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            clipToOutline = false
            elevation = 12f
            setPadding(dpToPx(6))
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(16).toFloat()
                setColor(Color.TRANSPARENT)
            }
        }

        controlParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            val savedPos = DataStorage.loadPosition(999)
            x = savedPos.first
            y = savedPos.second
        }

        val mainButton = createControlButton("⚙️", Color.TRANSPARENT, 24f) {
            toggleControlPanel()
        }
        controlPanel!!.addView(mainButton)

        var initialTouchX = 0f
        var initialTouchY = 0f
        var dragging = false

        mainButton.setOnTouchListener { v, event ->
            if (isDestroying) return@setOnTouchListener false

            val params = controlParams ?: return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (abs(dx) > CLICK_THRESHOLD || abs(dy) > CLICK_THRESHOLD) {
                        dragging = true
                        params.x += dx
                        params.y += dy
                        try {
                            windowManager?.updateViewLayout(controlPanel, params)
                        } catch (_: Exception) {}

                        DataStorage.savePosition(999, params.x, params.y)

                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!dragging) v.performClick()
                    dragging = false
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(controlPanel, controlParams)
        } catch (e: Exception) {
            Log.e("FloatingService", "Error adding control panel: ${e.message}")
//            CrashLogger.log(this, "FloatingService", "Error adding control panel: ${e.message}")

        }
    }

    @SuppressLint("ClickableViewAccessibility", "UseKtx")
    private fun toggleControlPanel() {
        if (isDestroying) return
        isExpanded = !isExpanded

        val panel = controlPanel ?: run {
            Log.e("FloatingBrowserService", "controlPanel is null!")
            return
        }

        // Clear old items
        if (panel.childCount > 1) {
            panel.removeViews(1, panel.childCount - 1)
        }

        if (!isExpanded) return

        val margin = dpToPx(2)

        // Scroll container
        val scrollView = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(360), ViewGroup.LayoutParams.MATCH_PARENT)
            setPadding(margin, margin, margin, margin)
            isHorizontalScrollBarEnabled = false
        }

        // Inner horizontal layout
        val innerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(margin, margin, margin, margin)

        }


        // Control button color
        val blue = "#031956".toColorInt()

        // List of static control buttons
        val controls = listOf(
//            Triple("📦 List", blue) { toggleOverlayBoxWindow() },
            Triple("Show ⛶", blue) { toggleHide(true) },
            Triple("Hide ●", blue) { toggleHide(false) },
            Triple("📱 Open App", blue) { launchAppSafe("com.example.floatingweb") },
            Triple("❌", Color.WHITE) { toggleControlPanel() }
        )

        // Add control buttons
        controls.forEach { (label, color, action) ->
            innerLayout.addView(createControlButton(label, color, 10f, action))
        }
        // --- Coin Buttons ---
        overlaysById.entries.forEachIndexed { index, (id, holder) ->
            val url = holder.webView.url ?: ""
            val coin = try {
                Uri.parse(url).getQueryParameter("symbol")
                    ?.substringAfter(":")
                    ?.uppercase() ?: "Unknown"
            } catch (_: Exception) {
                "Unknown"
            }

            innerLayout.addView(
                createControlButton(
                    text = "${index + 1}: $coin",
                    bgColor = "#FFBB86FC".toColorInt(),
//                    radius = 10f,
                    textSizeSp = 11f,

                    ) {
                    switchToState(STATE_MAX, id)
                }
            )
        }

        scrollView.addView(innerLayout)
        panel.addView(scrollView)
    }
    private fun launchAppSafe(pkg: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) startActivity(intent)
            else Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Cannot open app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeControlPanel() {
        controlPanel?.let { panel ->
            try {
                windowManager?.removeViewImmediate(panel)
            } catch (e: Exception) {
                Log.e("FloatingService", "Error removing control panel: ${e.message}")
//                CrashLogger.log(this, "FloatingService", "Error removing control panel: ${e.message}")

            }
            controlPanel = null
            controlParams = null
        }
    }

    private fun createControlButton(
        text: String,
        bgColor: Int,
        textSizeSp: Float,
        onClick: () -> Unit,
    ): Button {
        return Button(this).apply {
            this.text = text
            this.textSize = textSizeSp
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
            }
            layoutParams = LinearLayout.LayoutParams(dpToPx(50), dpToPx(50)).apply {
                topMargin = dpToPx(6)
            }
            setOnClickListener {
                if (!isDestroying) onClick()
            }
            elevation = dpToPx(4).toFloat()
        }
    }

    private fun toggleHide(show: Boolean){
        overlaysById.values.forEach { it.overlay.visibility = if (show) View.VISIBLE else View.GONE   }
    }
    private var overlayBoxWindowParams: WindowManager.LayoutParams? = null
    private var overlayBoxWindow: FrameLayout? = null
    private val savedWindowParams = mutableMapOf<String, WindowManager.LayoutParams>()
//    private val attachedInBox = mutableListOf<Int>()
//    private val savedBoxWrappers = mutableMapOf<Int, FrameLayout>()

    private fun safeDetachView(id: String) {
        val overlay = overlaysById[id]?.overlay ?: return
        try { (overlay.parent as? ViewGroup)?.removeView(overlay) } catch (_: Exception) {}
        try { windowManager?.removeViewImmediate(overlay) } catch (_: Exception) {}
    }

    private fun safeAttachToWindow(id: String, params: WindowManager.LayoutParams) {
        if (isDestroying) return

        val overlay = overlaysById[id]?.overlay ?: return
        try { (overlay.parent as? ViewGroup)?.removeView(overlay) } catch (e: Exception) {
//            CrashLogger.log(this, "FloatingService", "Error safe attach removing overlay : ${e.message}")
        }
        try {
            windowManager?.addView(overlay, params)
        } catch (ise: IllegalStateException) {
            try { windowManager?.updateViewLayout(overlay, params) } catch (_: Exception) {}
        } catch (e: Exception) {
//            CrashLogger.log(this, "FloatingService", "Error tried to add overlay to window manager : ${e.message}")

        }
    }

    private fun defaultMiniParams(id: String): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            dpToPx(SIZE_MINI_WIDTH_DP),
            dpToPx(SIZE_MINI_HEIGHT_DP),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(40 * (getindexofid(id) % 3))
            y = dpToPx(60 * (getindexofid(id) % 4))
        }
    }



    private fun restoreOverlayToMini(){
        if (overlayBoxWindow != null && attachedInBox.isNotEmpty()) {
            attachedInBox.forEach { id ->
                val overlay = overlaysById[id]?.overlay ?: return@forEach

                overlay.setOnTouchListener(null)
                overlay.isClickable = true
                overlaysById[id]?.webView?.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN && !isDestroying) {
                        requestOverlayFocus(id)
                    }
                    false
                }

                try { (overlay.parent as? ViewGroup)?.removeView(overlay) } catch (_: Exception) {}
                val miniParams = defaultMiniParams(id)
                safeAttachToWindow(id, miniParams)
                switchToState(STATE_MINI, id)
            }
            attachedInBox.clear()
            savedBoxWrappers.clear()
            toggleHide(false)

            try {
                overlayBoxWindow?.let { windowManager?.removeViewImmediate(it) }
            } catch (e: Exception) {
//                        CrashLogger.log(this@FloatingBrowserService, "FloatingService", "Hide all err ${e.message}")
            }
            overlayBoxWindow = null
            overlayBoxWindowParams = null

        }
    }

    private fun restoreOverlay(id: String) {
        if (isDestroying) return

        val overlay = overlaysById[id]?.overlay ?: return
//        switchToState(STATE_MAX, id)
        overlay.visibility = View.VISIBLE

        if (overlay.parent == null) {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            try {
                windowManager?.addView(overlay, params)
            } catch (e: Exception) {
                Log.e("FloatingService", "Error restoring overlay: ${e.message}")
//                CrashLogger.log(this, "FloatingService", "Error restoring overlay: ${e.message}")

            }
        }
    }

    private fun safeReloadWebView(id: String) {
        if (isDestroying) {
            Log.d("FloatingService", "Reload aborted: Service is destroying")
            return
        }

        val webView = overlaysById[id]?.webView ?: run {
            Log.d("FloatingService", "Reload aborted: WebView not found for ID: $id")
            return
        }

        if (webView.context == null) {
            Log.d("FloatingService", "Reload aborted: WebView context is null for ID: $id")
            return
        }

        if (webView.isAttachedToWindow) {
            webView.reload()
        } else {
            Log.d("FloatingService", "WebView is not attached to window, posting reload for ID: $id")
            webView.post {
                try {
                    if (!isDestroying) {
                        Log.d("FloatingService", "Executing posted reload for ID: $id")
                        webView.reload()
                    } else {
                        Log.d("FloatingService", "Posted reload aborted: Service is destroying for ID: $id")
                    }
                } catch (e: Exception) {
                    Log.e("FloatingService", "Failed to reload WebView in post for ID: $id", e)
                }
            }
        }
    }

    // 🔧 FIX: Proper destruction with safety checks
    override fun onDestroy() {
        super.onDestroy()
        if (!isDestroying) {
            destroyServiceCompletely()
        }
        if (isScreenReceiverRegistered) {
            unregisterReceiver(screenReceiver)
            isScreenReceiverRegistered = false
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun destroyServiceCompletely() {
        if (isDestroying) {
            Log.w("FloatingService", "Already destroying service, skipping duplicate call")
//            CrashLogger.log(this, "FloatingService", "Already destroying service, skipping duplicate call")

            return
        }
        if (isScreenReceiverRegistered) {
            unregisterReceiver(screenReceiver)
            isScreenReceiverRegistered = false
        }
        isDestroying = true

        Log.d("FloatingService", "Starting service destruction...")
//        CrashLogger.log(this, "FloatingService", "Starting service destruction...")

        try {
            // 1. Cancel coroutines FIRST
            overlayJob?.cancel()
            overlayJob = null
            serviceScope.cancel()

            // 2. Stop all handlers
            handlers.forEach { handler ->
                handler.removeCallbacksAndMessages(null)
            }
            handlers.clear()

            // 3. Stop alert sounds
            stopAlertSound()

            // 4. Release wake lock
            releaseWakeLock()

            // 5. Destroy WebViews properly
            overlaysById.forEach { (id,holder) ->
                val webView = holder.webView
                    try {
                        webView.stopLoading()
                        webView.clearHistory()
                        webView.clearCache(true)
                        webView.loadUrl("about:blank")
                        webView.removeJavascriptInterface("Android")
                        webView.webViewClient = object : WebViewClient() {}
                        webView.webChromeClient = null
                        webView.onPause()
                        webView.removeAllViews()
                        webView.destroy()
                    } catch (e: Exception) {
                        Log.e("FloatingService", "Error destroying WebView: ${e.message}")
//                        CrashLogger.log(
//                            this,
//                            "FloatingService",
//                            "Error destroying WebView: ${e.message}"
//                        )

                    }

//                webView.clear()

                // 6. Remove all floating overlays
                    try {
                        (holder.overlay.parent as? ViewGroup)?.removeView(holder.overlay)
                    } catch (_: Exception) {
                    }

                    try {
                        windowManager?.removeViewImmediate(holder.overlay)
                    } catch (e: Exception) {
                        Log.e("FloatingService", "Error destroy removing overlay: ${e.message}")
//                        CrashLogger.log(
//                            this,
//                            "FloatingService",
//                            "Error destroy removing overlay: ${e.message}"
//                        )

                    }

//                holder.overlay.clear()
//                holder.headerBars.clear()
//                holder.bubbles.clear()
            }
            // 7. Remove side box window
            overlayBoxWindow?.let {
                try {
                    windowManager?.removeViewImmediate(it)
                } catch (e: Exception) {
                    Log.e("FloatingService", "Error removing box window: ${e.message}")
//                    CrashLogger.log(this, "FloatingService", "Error removing box window: ${e.message}")

                }
            }
            overlayBoxWindow = null
            overlayBoxWindowParams = null

            // 8. Remove control panel
            removeControlPanel()

            // 9. Remove parent overlay
            parentOverlay?.let {
                try {
                    windowManager?.removeViewImmediate(it)
                } catch (e: Exception) {
                    Log.e("FloatingService", "Error removing parent overlay: ${e.message}")
//                    CrashLogger.log(this, "FloatingService", "Error removing parent overlay: ${e.message}")

                }
            }
            parentOverlay = null

            // 10. Clear all collections
            currentStates.clear()
            savedPositions.clear()
            latestPrices.clear()
            latestSymbols.clear()
//            activeAlerts.clear()
            attachedInBox.clear()
            savedBoxWrappers.clear()
            savedWindowParams.clear()

            // 11. Stop foreground and notifications
            try {
                stopForeground(true)
                NotificationManagerCompat.from(this).cancelAll()
            } catch (e: Exception) {
                Log.e("FloatingService", "Error stopping foreground: ${e.message}")
//                CrashLogger.log(this, "FloatingService", "Error stopping foreground: ${e.message}")

            }

            Log.d("FloatingService", "Service destruction completed successfully")

            val intent = Intent("com.example.floatingweb.SERVICE_STOPPED").apply {
                putExtra("manual_stop", true)
                setPackage(packageName)
            }
            sendBroadcast(intent)

            stopSelf()

        } catch (e: Exception) {
            Log.e("FloatingService", "Critical error during service destruction: ${e.message}", e)
            try {
                stopForeground(true)
                stopSelf()
            } catch (stopError: Exception) {
                Log.e("FloatingService", "Could not stop service: ${stopError.message}")
            }
        }
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "floating_web_channel")
            .setContentTitle("Floating Browser Running")
            .setContentText("Tap to manage your overlays")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private var mediaPlayer: MediaPlayer? = null


    fun stopAlertSound() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("FloatingService", "Error stopping sound: ${e.message}")
//            CrashLogger.log(this, "FloatingService", "Error stopping sound: ${e.message}")

        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "price_alert_channel",
            "Price Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for price alerts"
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
            setSound(null, null)
        }

        val channel2 = NotificationChannel(
            "floating_web_channel",
            "Floating Web Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Foreground service notification"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(channel2)
    }
}


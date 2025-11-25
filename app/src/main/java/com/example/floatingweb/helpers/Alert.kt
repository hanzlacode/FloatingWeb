package com.example.floatingweb.helpers
import android.content.Context
import android.util.Log
import com.example.floatingweb.services.FloatingBrowserService
import java.io.Serializable
import java.util.UUID

enum class AlertType { ABOVE, BELOW }
enum class AlertStatus { ACTIVE, TRIGGERED, MISSED }
enum class AlertFor {USDM,COINM,SPOT}
data class PriceAlert(
    val id: String = UUID.randomUUID().toString(),
    val symbol: String,         // extracted symbol/coin name (best-effort)
    val threshold: Double,
    val name:String,
    val type: AlertType,
    val createdAt: Long = System.currentTimeMillis(),
    var status: AlertStatus = AlertStatus.ACTIVE,
    var triggeredAt: Long? = null,
    var triggeredPrice: Double? = null,
    var alertFor:AlertFor = AlertFor.SPOT
): Serializable









private fun extractCoinNameFromTitle(title: String): String {
    // Typically, the first token is the coin symbol, e.g. "DOGEUSDT 0.18934 ▲ +2.21%"
    val match = Regex("^[A-Z0-9.:/-]+").find(title)
    return match?.value ?: title.split(" ").firstOrNull()?.uppercase() ?: "UNKNOWN"
}


fun extractCoinName(input: String): String {
    val parts = input.trim().split("\\s+".toRegex())
    if (parts.isEmpty()) return ""

    val candidate = parts.firstOrNull { it.matches(Regex("^[A-Z0-9/._:-]+$")) }
    return candidate ?: parts.first()
}

fun hasAlertForCoin(input: String, alerts: List<PriceAlert>,alertStatus:AlertStatus): Boolean {
    val coin = extractCoinNameFromTitle(input).uppercase()
    return alerts.any { alert ->
        alert.status == alertStatus && (
                alert.symbol.uppercase().contains(coin) || coin.contains(alert.symbol.uppercase())
                )
    }
}

fun findAlertsForCoin(input: String,alerts: List<PriceAlert>,alertStatus:AlertStatus): List<PriceAlert> {
    val coin = extractCoinNameFromTitle(input).uppercase()

    return alerts.filter { alert ->
        alert.status == alertStatus &&
                ( alert.symbol.uppercase().contains(coin) || coin.contains(alert.symbol.uppercase()))
    }
}






//triggered

fun triggeredAndSaveAlert(alert: PriceAlert, currentPrice: Double, context: Context) {
    // Update the alert
    alert.apply {
        triggeredAt = System.currentTimeMillis()
        triggeredPrice = currentPrice
        status = AlertStatus.TRIGGERED
    }

    // Load existing alerts from storage
//    val alerts = DataStorage.loadAlerts(context as FloatingBrowserService).toMutableList()
//    val index = alerts.indexOfFirst { it.id == alert.id }

    // Update existing alert or add if missing
//    if (index >= 0) alerts[index] = alert else alerts.add(alert)

    // Save back to storage
//    DataStorage.saveAlerts(context, alerts)

    Log.d("PriceAlert", "Triggered alert saved: $alert")
}















//Use Later


/** Displays the UI for setting a new price alert, now including alert type selection. */
//    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
//    private fun showPriceInputOverlay(index: Int) {
//        Log.d("PriceLive", "Showing price overlay for WebView index $index")
//        val webView = webViews[index]
//        val existingOverlay = floatingOverlays[index].findViewById<LinearLayout>(R.Id.price_input_overlay)
//        if (existingOverlay != null) return
//
//        // Main overlay container (rounded card look)
//        val overlay = LinearLayout(this).apply {
//            id = R.Id.price_input_overlay
//            orientation = LinearLayout.VERTICAL
//            setBackgroundColor("#3534345e".toColorInt())
//            setPadding(dpToPx(16))
//            gravity = Gravity.TOP
//            background = GradientDrawable().apply {
//                setColor("#3534345e".toColorInt())
//                cornerRadius = dpToPx(16).toFloat()
//            }
//            elevation = 16f
//        }
//
//        // Title
//        val targetTitle = TextView(this).apply {
//            text = "🎯 Set Price Target"
//            textSize = 20f
//            setTypeface(typeface, Typeface.BOLD)
//            setTextColor(Color.WHITE)
//            gravity = Gravity.CENTER
//            setPadding(0, 0, 0, dpToPx(12))
//        }
//
//        // Input Row
//        val inputRow = LinearLayout(this).apply {
//            orientation = LinearLayout.HORIZONTAL
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                bottomMargin = dpToPx(10)
//            }
//        }
//
//        // Price EditText
//        val editText = EditText(this).apply {
//            hint = "Enter target price"
//            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
//            setBackgroundResource(android.R.drawable.edit_text)
//            setBackgroundColor(Color.WHITE)
//            setTextColor(Color.BLACK)
//            textSize = 16f
//            setPadding(dpToPx(10))
//            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
//        }
//        inputRow.addView(editText)
//
//        // Spinner for alert type
//        val alertTypes = arrayOf("Up (↑)", "Down (↓)")
//        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, alertTypes)
//        val spinner = Spinner(this).apply {
//            adapter = spinnerAdapter
//            setBackgroundColor(Color.WHITE)
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                marginStart = dpToPx(8)
//            }
//        }
//        inputRow.addView(spinner)
//
//        // Buttons Row
//        val buttonsRow = LinearLayout(this).apply {
//            orientation = LinearLayout.HORIZONTAL
//            gravity = Gravity.CENTER
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                topMargin = dpToPx(8)
//                bottomMargin = dpToPx(12)
//            }
//        }
//
//        fun makeModernButton(text: String, color: String): Button {
//            return Button(this).apply {
//                this.text = text
//                setTextColor(Color.WHITE)
//                textSize = 15f
//                setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
//                background = GradientDrawable().apply {
//                    setColor(color.toColorInt())
//                    cornerRadius = dpToPx(12).toFloat()
//                }
//            }
//        }
//
//        val buttonSet = makeModernButton("✅ Set Alert", "#4CAF50")
//        val buttonCancel = makeModernButton("❌ Cancel", "#E53935").apply {
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply { marginStart = dpToPx(8) }
//        }
//
//        buttonsRow.addView(buttonSet)
//        buttonsRow.addView(buttonCancel)
//
//        // Active Targets label
//        val activeLabel = TextView(this).apply {
//            text = "📋 Active Targets"
//            textSize = 17f
//            setTypeface(typeface, Typeface.BOLD)
//            setTextColor(Color.WHITE)
//            gravity = Gravity.START
//            setPadding(0, dpToPx(6), 0, dpToPx(6))
//        }
//
//        // ScrollView for active list
//        val scrollView = ScrollView(this).apply {
//            isFillViewport = true
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                dpToPx(150)
//            )
//            background = GradientDrawable().apply {
//                setColor("#22111111".toColorInt())
//                cornerRadius = dpToPx(12).toFloat()
//            }
//            setPadding(dpToPx(6))
//        }
//
//        val targetsContainer = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            )
//        }
//        scrollView.addView(targetsContainer)
//
//        // --- Refresh Target List ---
//        fun refreshTargetList() {
//            android.os.Handler(Looper.getMainLooper()).post {
//                targetsContainer.removeAllViews()
//                val targets = activeAlerts[index]?.toList()?.sortedBy { it.threshold } ?: emptyList()
//
//                Log.d("Floating","$targets ${activeAlerts[index] } || $activeAlerts ")
//
//                if (targets.isEmpty()) {
//                    targetsContainer.addView(TextView(this).apply {
//                        text = "No active targets"
//                        setTextColor(Color.LTGRAY)
//                        gravity = Gravity.CENTER
//                        textSize = 14f
//                        setPadding(dpToPx(4))
//                    })
//                    return@post
//                }
//
//                targets.forEach { alert ->
//                    val row = LinearLayout(this).apply {
//                        orientation = LinearLayout.HORIZONTAL
//                        setPadding(dpToPx(6))
//                        background = GradientDrawable().apply {
//                            setColor("#33111111".toColorInt())
//                            cornerRadius = dpToPx(10).toFloat()
//                        }
//                        layoutParams = LinearLayout.LayoutParams(
//                            LinearLayout.LayoutParams.MATCH_PARENT,
//                            LinearLayout.LayoutParams.WRAP_CONTENT
//                        ).apply { topMargin = dpToPx(6) }
//                    }
//
//                    val label = TextView(this).apply {
//                        text = when (alert.type) {
//                            AlertType.ABOVE -> "↑ Up ${alert.threshold}"
//                            AlertType.BELOW -> "↓ Down ${alert.threshold}"
//                        }
//                        textSize = 14f
//                        setTextColor(Color.WHITE)
//                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
//                    }
//
//                    val removeBtn = makeModernButton("🗑", "#666666").apply {
//                        textSize = 12f
//                        layoutParams = LinearLayout.LayoutParams(
//                            LinearLayout.LayoutParams.WRAP_CONTENT,
//                            LinearLayout.LayoutParams.WRAP_CONTENT
//                        ).apply { marginStart = dpToPx(8) }
//                        setOnClickListener {
//                            activeAlerts[index]?.remove(alert)
//                            Toast.makeText(applicationContext, "Removed ₹${alert.threshold}", Toast.LENGTH_SHORT).show()
//                            refreshTargetList()
//                        }
//                    }
//
//                    row.addView(label)
//                    row.addView(removeBtn)
//                    targetsContainer.addView(row)
//                }
//            }
//        }
//
////         --- Button listeners ---
////        buttonSet.setOnClickListener {
////            val target = editText.text.toString().trim().toDoubleOrNull()
////            if (target != null && target > 0) {
////                val selectedType = if (spinner.selectedItemPosition == 0)
////                    AlertType.ABOVE else AlertType.BELOW
////
////                val alert = PriceAlert(threshold = target, type = selectedType)
////                addAlertTarget(webView, alert,index)
////                Toast.makeText(applicationContext, "Alert set for ₹$target", Toast.LENGTH_SHORT).show()
////                setupPriceMonitor(index)
////                refreshTargetList()
////                editText.setText("")
////            } else {
////                Toast.makeText(applicationContext, "Invalid price", Toast.LENGTH_SHORT).show()
////            }
////        }
//
//        buttonCancel.setOnClickListener {
//            floatingOverlays[index].removeView(overlay)
//        }
//
//        // --- Add all to overlay ---
//        overlay.addView(targetTitle)
//        overlay.addView(inputRow)
//        overlay.addView(buttonsRow)
//        overlay.addView(activeLabel)
//        overlay.addView(scrollView)
//
//        floatingOverlays[index].addView(
//            overlay,
//            FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.WRAP_CONTENT,
//                Gravity.TOP
//            ).apply {
//                leftMargin = dpToPx(16)
//                rightMargin = dpToPx(16)
//            }
//        )
//
//        refreshTargetList()
//    }

/** Adds a PriceAlert object for a WebView */
//    private fun addAlertTarget(webView: WebView, alert: PriceAlert,index) {
//        val targets = activeAlerts.getOrPut(index) { mutableSetOf() }
//        targets.add(alert)
//        Log.d("PriceAlert", "Added target: ${alert.type} ${alert.threshold} (Total: ${targets.size})")
//
//        // Immediate check after adding new alert
//        checkPriceInWebView(webView)
//    }

/** Calls the JS function in WebView to check current price */
//private fun checkPriceInWebView(webView: WebView) {
//    webView.evaluateJavascript("window.getPriceAndCheck();", null)
//}

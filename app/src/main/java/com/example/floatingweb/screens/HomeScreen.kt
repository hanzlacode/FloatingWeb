@file:Suppress("DEPRECATION")
package com.example.floatingweb.screens

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.floatingweb.LinkStorageManager
import com.example.floatingweb.services.FloatingBrowserService
import androidx.core.net.toUri
import androidx.core.content.edit
import androidx.navigation.NavBackStackEntry
import com.example.floatingweb.helpers.BinanceSymbolsCache
import com.example.floatingweb.helpers.DataStorage
import com.example.floatingweb.helpers.PriceAlert
import com.example.floatingweb.services.PriceAlertService
import kotlinx.coroutines.launch

//import com.example.floatingweb.services.FloatingControllerService

@SuppressLint("UnspecifiedRegisterReceiverFlag")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingWebScreen(navController: NavBackStackEntry) {
    val context = LocalContext.current
    val storage = remember { LinkStorageManager(context) }

    val links = remember { mutableStateListOf<String>().apply { addAll(storage.loadLinks()) } }
    val timeFrames = remember {
        mutableStateListOf<String>().apply {
            val saved = storage.loadTimeFrames()

            if (saved.isNullOrEmpty()) {
                // load defaults
                addAll(
                    listOf(
                        "1",
                        "3",
                        "5",
                        "15",
                        "30",
                        "60",
                        "120",
                        "180",
                        "240",
                        "1D",
                        "1W",
                        "1M",
                    )
                )
            } else {
                addAll(saved)
            }
        }
    }

    val selectedIndices = remember { mutableStateListOf<Int>() }

    var selectedMode by remember { mutableIntStateOf(storage.loadSelectedWebMode()) }
    var isServiceRunning by remember { mutableStateOf(context.isServiceRunning(
        FloatingBrowserService::class.java)) }
    var isServiceRunningForAlertService by remember { mutableStateOf(context.isServiceRunning(
        PriceAlertService::class.java)) }

    // Save links on change
    LaunchedEffect(links) {
        snapshotFlow { links.toList() }.collect { storage.saveLinks(it) }
    }
    LaunchedEffect(timeFrames) {
        snapshotFlow { timeFrames.toList() }.collect { storage.saveTimeFrames(it) }
    }


// Restore saved selections
    LaunchedEffect(Unit) {
        val savedSelected = context.getSharedPreferences("price_alert", Context.MODE_PRIVATE)
            .getStringSet("selected_indices", emptySet())
            ?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        selectedIndices.addAll(savedSelected.filter { it in links.indices })
    }

// Save selected indices whenever they change
    LaunchedEffect(selectedIndices) {
        snapshotFlow { selectedIndices.toSet() }.collect { indices ->
            context.getSharedPreferences("price_alert", Context.MODE_PRIVATE)
                .edit { putStringSet("selected_indices", indices.map { it.toString() }.toSet()) }
        }
    }

    // Sound picker
    val selectedSoundUri = remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(Unit) {
        context.getSharedPreferences("price_alert", Context.MODE_PRIVATE)
            .getString("custom_sound_uri", null)?.let { selectedSoundUri.value = it.toUri() }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isServiceRunning = false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter("com.example.floatingweb.SERVICE_STOPPED"),
                Context.RECEIVER_NOT_EXPORTED
            )
//            context.registerReceiver(
//                receiver,
//                IntentFilter("com.example.floatingweb.STOPSOUND"),
//                Context.RECEIVER_NOT_EXPORTED
//            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter("com.example.floatingweb.SERVICE_STOPPED")
            )
//            context.registerReceiver(
//                receiver,
//                IntentFilter("com.example.floatingweb.STOPSOUND")
//            )
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    var isDarkMode by remember { mutableStateOf(true) }
    val colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()

    Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HeaderSection(isDarkMode) {  newMode ->
                isDarkMode = newMode }

            LinksManagerSection(
                links = links,
                selectedIndices = selectedIndices,
                timeFrames = timeFrames,
                onEdit = { index, newLink -> links[index] = newLink },
                onDelete = { index ->
                    selectedIndices.remove(index)
                    links.removeAt(index)
                },
                onTimeFrameSelected ={ index,timeframe ->
                    Log.d("timeframe","$index $timeframe")
                    timeFrames[index] = timeframe
//                    storage.saveTimeFrames(it.)
                }
            )

//        Button(modifier = Modifier.padding(3.dp), onClick = {
//            val controllerIntent = Intent(context, FloatingControllerService::class.java)
//            if(!context.isServiceRunning(FloatingControllerService::class.java)){
//                context.startService(controllerIntent)
//            }
//        }
//        ) {
//            Text("StartWeb")
//        }
//        Button(modifier = Modifier.padding(3.dp), onClick = {
//            val stopIntent = Intent(context, FloatingControllerService::class.java)
//            stopIntent.putExtra("action", "stop")
//            context.startService(stopIntent)
//        }
//        ) {
//            Text("StopWeb")
//        }

//        ServiceControlButtons(
//            links = links,
//            selectedIndices = selectedIndices,
//            isServiceRunning = isServiceRunningS,
//            onServiceStateChange = { newState ->
//                isServiceRunningS = newState
//            }
//        )

//        ServiceControlSection(
//            selectedIndices = selectedIndices,
//            isServiceRunning = isServiceRunningS,
//            onServiceStateChange = {
//                val controllerIntent = Intent(context, FloatingControllerService::class.java)
//                if(!context.isServiceRunning(FloatingControllerService::class.java)){
//                    context.startService(controllerIntent)
//                }else{
//                    controllerIntent.putExtra("action", "stop")
//                    context.startService(controllerIntent)
//                }
//                isServiceRunningS = context.isServiceRunning(FloatingControllerService::class.java)
//            }
//        )

        AdvancedWebViewSelector(modifier = Modifier.fillMaxSize(),listOf("Simple (No Injection) WebView Fastest","Normal (With Injection) WebView Faster","Advanced WebView With Added Feature"),initMode=selectedMode,{
           selectedMode = it
            storage.saveSelectedWebMode(it)
            Log.d("ui/ux","mode ${it}")
        })

        ServiceControlSection(
                selectedIndices = selectedIndices,
                isServiceRunning = isServiceRunning,
                onServiceStateChange = {
                    val intent = Intent(context, FloatingBrowserService::class.java)
                    if (!context.isServiceRunning(FloatingBrowserService::class.java)) {
                        if (selectedIndices.isEmpty()) {
                            Toast.makeText(context, "Select at least one link", Toast.LENGTH_SHORT).show()
                            return@ServiceControlSection
                        }
                        intent.putExtra("Mode",selectedMode)
                        intent.putExtra(
                            "links",
                            ArrayList(selectedIndices.map { links[it] })
                        )
                        val bundle = Bundle()
                        val sizes = getOverlaySize(context)
                        bundle.putIntArray("mini", intArrayOf(sizes["mini"]!!.first, sizes["mini"]!!.second))
                        bundle.putIntArray("medium", intArrayOf(sizes["medium"]!!.first, sizes["medium"]!!.second))
                        intent.putExtra("ContainerSizes", bundle)
                        context.startService(intent)
                    } else context.stopService(intent)
                    isServiceRunning = context.isServiceRunning(FloatingBrowserService::class.java)
                }
            )

        val scope = rememberCoroutineScope() // only in a @Composable

        Button(
            onClick = {
                scope.launch {
                    // Get alerts list
                    val alertList = DataStorage.alertsLiveData.value ?: emptyList()

                    // Check invalid alerts one by one
                    val invalidAlerts = mutableListOf<PriceAlert>()
                    for (alert in alertList) {
                        if (!BinanceSymbolsCache.isValidSymbol(context, alert.symbol, alert.alertFor)) {
                            invalidAlerts.add(alert)
                        }
                    }

                    if (invalidAlerts.isNotEmpty()) {
                        // Show Toast and navigate
                        Toast.makeText(
                            context,
                            "Some alerts have invalid coin symbols. Please fix them.",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@launch
                    }

                    // Start or stop the service
                    val intent = Intent(context, PriceAlertService::class.java)
                    if (!isServiceRunningForAlertService) context.startService(intent)
                    else context.stopService(intent)

                    isServiceRunningForAlertService = context.isServiceRunning(PriceAlertService::class.java)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isServiceRunningForAlertService)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isServiceRunningForAlertService) "Stop Binance Socket Alert" else "Start Binance Socket Alert",
                style = MaterialTheme.typography.labelLarge
            )
        }

        OverlaySizePicker()
            SoundPicker(selectedSoundUri,isServiceRunningForAlertService)
        }
}

@Composable
fun SoundPicker(selectedSoundUri: MutableState<Uri?>, isServiceRunningForAlertService: Boolean) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    val pickSoundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // Persist permission and save URI
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            context.getSharedPreferences("price_alert", Context.MODE_PRIVATE)
                .edit { putString("custom_sound_uri", it.toString()) }
            selectedSoundUri.value = it

            // Stop currently playing audio if new one is chosen
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        }
    }

    // UI layout
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Sound",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Custom Alert Sound",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Select sound
            Button(
                onClick = { pickSoundLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Select")
                Spacer(Modifier.width(6.dp))
                Text("Choose Sound")
            }

            // Selected sound details
            selectedSoundUri.value?.let { uri ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Selected: ${uri.lastPathSegment ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Play / Stop controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    mediaPlayer?.release()
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(context, uri)
                                        prepare()
                                        start()
                                        isPlaying = true
                                        setOnCompletionListener {
                                            it.release()
                                            isPlaying = false
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(
                                        context,
                                        "Error playing sound",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isPlaying = false
                                }
                            },
                            enabled = !isPlaying,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                            Spacer(Modifier.width(6.dp))
                            Text("Play")
                        }

                        OutlinedButton(
                            onClick = {
                                if (isServiceRunningForAlertService) context.startService(Intent(context, PriceAlertService::class.java).apply { action = "com.example.action.STOP_SOUND"; putExtra("FromUi",true) })
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                isPlaying = false
                            },
                            enabled = isPlaying,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Stop")
                            Spacer(Modifier.width(6.dp))
                            Text("Stop")
                        }
                    }
                }
            } ?: Text(
                "No sound selected",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.outline
                )
            )
        }
    }

    // Cleanup when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }
}

// Add this in your service file or a separate file
fun getOverlaySize(context: Context): HashMap<String, Pair<Int, Int>> {
    val prefs = context.getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE)
    val miniWidth = prefs.getInt("mini_width", 200)
    val miniHeight = prefs.getInt("mini_height", 200)
    val mediumWidth = prefs.getInt("medium_width", 400)
    val mediumHeight = prefs.getInt("medium_height", 400)

    return hashMapOf(
        "mini" to Pair(miniWidth, miniHeight),
        "medium" to Pair(mediumWidth, mediumHeight)
    )
}

fun Context.saveOverlaySize(state: String, width: Int, height: Int) {
    getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE).edit {
        putInt("${state}_width", width)
        putInt("${state}_height", height)
    }
}

fun Context.getOverlaySize(state: String): Pair<Int, Int> {
    val prefs = getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE)
    val width = prefs.getInt("${state}_width", if (state == "mini") 200 else 600)  // default values
    val height = prefs.getInt("${state}_height", if (state == "mini") 150 else 400)
    return width to height
}

@Composable
fun OverlaySizePicker() {
    val context = LocalContext.current

    // Load saved dimensions
    var miniWidth by remember { mutableStateOf(context.getOverlaySize("mini").first.toString()) }
    var miniHeight by remember { mutableStateOf(context.getOverlaySize("mini").second.toString()) }
    var mediumWidth by remember { mutableStateOf(context.getOverlaySize("medium").first.toString()) }
    var mediumHeight by remember { mutableStateOf(context.getOverlaySize("medium").second.toString()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Customize Overlay Size", style = MaterialTheme.typography.titleMedium)

            // Mini state
            Text("Mini State", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = miniWidth,
                    onValueChange = { miniWidth = it.filter { c -> c.isDigit() } },
                    label = { Text("Width px") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = miniHeight,
                    onValueChange = { miniHeight = it.filter { c -> c.isDigit() } },
                    label = { Text("Height px") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Medium state
//            Text("Medium State", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
//            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                OutlinedTextField(
//                    value = mediumWidth,
//                    onValueChange = { mediumWidth = it.filter { c -> c.isDigit() } },
//                    label = { Text("Width px") },
//                    modifier = Modifier.weight(1f)
//                )
//                OutlinedTextField(
//                    value = mediumHeight,
//                    onValueChange = { mediumHeight = it.filter { c -> c.isDigit() } },
//                    label = { Text("Height px") },
//                    modifier = Modifier.weight(1f)
//                )
//            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // Reset dimensions
                        context.saveOverlaySize("mini", 130, 200)
                        context.saveOverlaySize("medium", 300, 500)

                        miniWidth = 130.toString()
                        miniHeight = 200.toString()
                        mediumWidth = 300.toString()
                        mediumHeight = 500.toString()

                        Toast.makeText(context, "Overlay size restored", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset Sizes")
                }

                Spacer(modifier = Modifier.weight(1f)) // Pushes the next button to the end

                Button(
                    onClick = {
                        // Save dimensions
                        context.saveOverlaySize(
                            "mini",
                            miniWidth.toIntOrNull() ?: 200,
                            miniHeight.toIntOrNull() ?: 150
                        )
                        context.saveOverlaySize(
                            "medium",
                            mediumWidth.toIntOrNull() ?: 600,
                            mediumHeight.toIntOrNull() ?: 400
                        )
                        Toast.makeText(context, "Overlay size saved", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Sizes")
                }
            }


        }
    }
}

// Check if service is running
fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return manager.getRunningServices(Int.MAX_VALUE)
        .any { it.service.className == serviceClass.name }
}

@Composable
private fun HeaderSection(isDarkMode: Boolean, onThemeChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Floating Web Browser", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(8.dp))
//            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
//                Text("Manage your links", style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
//                Switch(
//                    checked = isDarkMode,
//                    onCheckedChange = onThemeChange,
//                    colors = SwitchDefaults.colors(
//                        checkedTrackColor = MaterialTheme.colorScheme.primary,
//                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
//                    )
//                )
//                Text(if (isDarkMode) "Dark" else "Light", style = MaterialTheme.typography.bodySmall)
//            }
        }
    }
}

@Composable
private fun LinksManagerSection(
    links: SnapshotStateList<String>,
    selectedIndices: SnapshotStateList<Int>,
    onEdit: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    onTimeFrameSelected: (Int,String) -> Unit,
    timeFrames: SnapshotStateList<String>
) {
    val context = LocalContext.current

    var newLink by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp),colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Saved Links", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Badge { Text("${links.size}") }
            }

            if (links.isEmpty()) EmptyLinksPlaceholder()
            else links.forEachIndexed { index, link ->
                val tf = timeFrames.getOrNull(index) ?: ""

                LinkItem(
                    link = link,
                    timeFrame = tf,
                    isSelected = selectedIndices.contains(index),
                    onSelectionChange = { selected ->
                        if (selected) selectedIndices.add(index) else selectedIndices.remove(index)
                    },
                    onEdit = { newLink -> onEdit(index, newLink) },
                    onDelete = { onDelete(index) },
                    onCopy = { copiedLink ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Link", copiedLink))
                        Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                    },
                    onClone = {
                            if (it.isNotBlank()) { links.add(it.trim()); newLink = "" }
                    },
                    onTimeFrameSelected = { onTimeFrameSelected(index,it) }
                )
            }

            AddLinkSection(newLink, onNewLinkChange = { newLink = it }, onAddLink = {
                if (it.isNotBlank()) { links.add(it.trim()); newLink = "" }
            })
        }
    }
}

@Composable
private fun EmptyLinksPlaceholder() {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(32.dp), contentAlignment = Alignment.Center) {
        Text("No links added yet. Add your first link below",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun LinkItem(
    link: String,
    isSelected: Boolean,
    timeFrame:String,
    onSelectionChange: (Boolean) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    onCopy: (String) -> Unit,
    onClone: (String) -> Unit,
    onTimeFrameSelected: (String)-> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(link) { mutableStateOf(link) }

    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isEditing) {
                OutlinedTextField(value = editText, onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Enter URL") })
            } else {
                Text(link, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {

                Checkbox(checked = isSelected, onCheckedChange = onSelectionChange)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        icon = Icons.Default.Autorenew,
                        contentDescription = "Clone",
                        onClick = {
                            onClone(link)
                        },
                                colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                                )
                    )
                    ActionButton(
                        icon = if(isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = "Edit",
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (isEditing) Color.Green else MaterialTheme.colorScheme.primary
                        ),
                        onClick = {
                            if (editText.isNotBlank()) {
                                onEdit(editText.trim())
                                isEditing = !isEditing
                            }
                        }
                    )

                    ActionButton(
                        icon = Icons.Default.CopyAll,
                        contentDescription = "Copy",
                        onClick = { onCopy(link) },
                        colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    ActionButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "Delete",
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    )

                }

            }
            // Define a data class for time frame options
            data class TimeFrameOption(
                val value: String,
                val label: String
            )

// Create the list of time frame options
            val timeFrameOptions = listOf(
                TimeFrameOption("1", "1 minute"  ),
                TimeFrameOption("3", "3 minutes"  ),
                TimeFrameOption("5", "5 minutes"  ),
                TimeFrameOption("15", "15 minutes"),
                TimeFrameOption("30", "30 minutes"),
                TimeFrameOption("60", "1 hour"    ),
                TimeFrameOption("120", "2 hours"  ),
                TimeFrameOption("180", "3 hours"  ),
                TimeFrameOption("240", "4 hours"  ),
                TimeFrameOption("1D", "1 day"      ),
                TimeFrameOption("1W", "1 week"    ),
                TimeFrameOption("1M", "1 month"   )
            )

// State variables
            var expanded by remember { mutableStateOf(false) }
            var selectedTimeFrame by remember(timeFrame) {
                mutableStateOf(
                    when (timeFrame) {
                        "1" -> TimeFrameOption("1", "1 minute")
                        "3" -> TimeFrameOption("3", "3 minutes")
                        "5" -> TimeFrameOption("5", "5 minutes")
                        "15" -> TimeFrameOption("15", "15 minutes")
                        "30" -> TimeFrameOption("30", "30 minutes")
                        "60" -> TimeFrameOption("1 hour", "1 hour")
                        "120" -> TimeFrameOption("2 hours", "2 hours")
                        "180" -> TimeFrameOption("3 hours", "3 hours")
                        "240" -> TimeFrameOption("4 hours", "4 hours")
                        "1D" -> TimeFrameOption("1D", "1 day")
                        "1W" -> TimeFrameOption("1W", "1 week")
                        "1M" -> TimeFrameOption("1M", "1 month")
                        else -> TimeFrameOption("1", "1 minute")
                    }
                )
            }

          selectedTimeFrame =  when(timeFrame){
                "1"   ->  TimeFrameOption("1", "1 minute" )
                "3"   ->TimeFrameOption("3", "3 minutes" )
                "5"   -> TimeFrameOption("5", "5 minutes" )
                "15"  ->TimeFrameOption("15", "15 minutes")
                "30"  ->TimeFrameOption("30", "30 minutes")
                "60"  ->TimeFrameOption("60", "1 hour" )
                "120"  ->TimeFrameOption("120", "2 hours" )
                "180"  ->TimeFrameOption("180", "3 hours" )
                "240"  ->TimeFrameOption("240", "4 hours" )
                "1D"  ->TimeFrameOption("1D", "1 day" )
                "1W"  ->TimeFrameOption("1W", "1 week" )
                "1M"  ->TimeFrameOption("1M", "1 month" )
              else -> {TimeFrameOption("1", "1 minute" )}
          }

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedTimeFrame?.label ?: "Select a Mode",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown indicator",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(250.dp) // Set appropriate width
                ) {
                    timeFrameOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,     // show human-readable label
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                selectedTimeFrame = option
                                expanded = false
                                // Handle selection here - e.g., pass option.value to your logic
                                 onTimeFrameSelected(option.value)
                            },
                            modifier = Modifier.fillMaxWidth(),

                        )
//                        {
//                            Text(
//                                text = option.label,
//                                maxLines = 1,
//                                overflow = TextOverflow.Ellipsis
//                            )
//                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun AddLinkSection(newLink: String, onNewLinkChange: (String) -> Unit, onAddLink: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Add New Link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(newLink, onValueChange = onNewLinkChange, modifier = Modifier.weight(1f),
                placeholder = { Text("Enter URL (e.g., https://example.com)") }, singleLine = true)
            FloatingActionButton(onClick = { onAddLink(newLink) }, modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Default.Add, contentDescription = "Add Link") }
        }
    }
}

@Composable
private fun ServiceControlSection(
    selectedIndices: SnapshotStateList<Int>,
    isServiceRunning: Boolean,
    onServiceStateChange: () -> Unit,
) {
    LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isServiceRunning) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (isServiceRunning) "Service Running" else "Service Stopped",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = if (isServiceRunning) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer)

            Button(onClick = onServiceStateChange, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isServiceRunning) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary)) {
                Text(if (isServiceRunning) "Stop Floating Browser" else "Start Floating Browser",
                    style = MaterialTheme.typography.labelLarge)
            }

            if (!isServiceRunning && selectedIndices.isNotEmpty()) {
                Text("Ready to launch with ${selectedIndices.size} link${if (selectedIndices.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun ServiceControlButtons(
    links: List<String>,
    selectedIndices: List<Int>,
    isServiceRunning: Boolean,
    onServiceStateChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        onClick = {
//            val controllerIntent = Intent(context, FloatingControllerService::class.java)

            if (!isServiceRunning) {
                // Starting service - send selected links
                val selectedLinks = selectedIndices.map { links.getOrNull(it) ?: "" }
                    .filter { it.isNotBlank() }

                if (selectedLinks.isEmpty()) {
                    Toast.makeText(context, "Please select at least one link", Toast.LENGTH_SHORT).show()
                    return@Button
                }

//                controllerIntent.apply {
//                    putExtra("action", "start")
//                    putStringArrayListExtra("links", ArrayList(selectedLinks))
//                    putIntegerArrayListExtra("selected_indices", ArrayList(selectedIndices))
//                }
//                context.startService(controllerIntent)
                onServiceStateChange(true)
            } else {
                // Stopping service
//                controllerIntent.putExtra("action", "stop")
//                context.startService(controllerIntent)
                onServiceStateChange(false)
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isServiceRunning)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = if (isServiceRunning) Icons.Default.Warning else Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isServiceRunning) "Stop Floating Web" else "Start Floating Web",
            style = MaterialTheme.typography.labelLarge
        )
    }

    // Show status info
    if (!isServiceRunning && selectedIndices.isNotEmpty()) {
        Text(
            text = "Ready to launch ${selectedIndices.size} link${if (selectedIndices.size == 1) "" else "s"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}


@Composable
fun AdvancedWebViewSelector(
    modifier: Modifier = Modifier,
    mode: List<String>,
    initMode:Int,
    onClicked: (Int) -> Unit
) {
    var selectedMode by remember { mutableStateOf<String>(mode[initMode]) }
    val context = LocalContext.current

    Column(
        modifier = modifier.padding(12.dp)
    ) {
        Text(
            text = "🔗 Choose a Mode to Start Floating Web",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Simple dropdown to pick which link to open in the WebView
        var expanded by remember { mutableStateOf(false) }

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedMode ?: "Select a Mode")
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                mode.forEachIndexed { index, mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = mode,  // Show index for demonstration
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            selectedMode = mode
                            expanded = false
                            onClicked(index)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live WebView preview area
//        selectedLink?.let { link ->
//            Surface(
//                tonalElevation = 4.dp,
//                shape = RoundedCornerShape(8.dp),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(300.dp)
//            ) {
//                AndroidView(
//                    factory = {
//                        WebView(context).apply {
//                            settings.javaScriptEnabled = true
//                            settings.domStorageEnabled = true
//                            settings.useWideViewPort = true
//                            settings.loadWithOverviewMode = true
//                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
//                            setBackgroundColor(android.graphics.Color.WHITE)
//
//                            // camera/mic permission handler
//                            webChromeClient = object : WebChromeClient() {
//                                override fun onPermissionRequest(request: PermissionRequest) {
//                                    request.grant(request.resources)
//                                }
//                            }
//
//                            webViewClient = object : WebViewClient() {
//                                override fun onPageFinished(view: WebView?, url: String?) {
//                                    super.onPageFinished(view, url)
//                                    Toast.makeText(context, "WebView loaded!", Toast.LENGTH_SHORT).show()
//                                }
//                            }
//
//                            loadUrl(link)
//                        }
//                    },
//                    update = { webView ->
//                        if (webView.url != link) {
//                            webView.loadUrl(link)
//                        }
//                    }
//                )
//            }
//        }
    }
}
@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
) {
    IconButton(onClick = onClick, colors = colors) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

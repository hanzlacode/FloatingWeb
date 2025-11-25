package com.example.floatingweb.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.floatingweb.helpers.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertManagerScreen(context: Context) {
    val alerts by DataStorage.alertsLiveData.observeAsState(emptyList())

// Form state
    var symbol by remember { mutableStateOf("") }
    var threshold by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AlertType.ABOVE) }
    var alertfor by remember { mutableStateOf(AlertFor.USDM) }
    var editId by remember { mutableStateOf<String?>(null) }

//    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }


    // Save or update alert
    fun saveAlert() {
        if (symbol.isBlank() || threshold.isBlank()) return
        val alert = PriceAlert(
            id = editId ?: UUID.randomUUID().toString(),
            symbol = symbol.trim().uppercase(),
            threshold = threshold.toDoubleOrNull() ?: 0.0,
            name = name.trim(),
            type = type,
            alertFor = alertfor
        )

        // Update or add in DataStorage
        if (alerts.any { it.id == alert.id }) {
            DataStorage.updateAlert(alert)
        } else {
            DataStorage.addAlert(alert)
        }

        // Reset form
        symbol = ""
        threshold = ""
        name = ""
        editId = null
    }

    // Delete alert
    fun deleteAlert(id: String) {
        DataStorage.removeAlert(id)
    }


    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Input Form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (editId == null) "Add New Alert" else "Edit Alert", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Symbol (e.g., BTCUSDT)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

            Row ( modifier = Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween){
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Type: ")
                    Spacer(Modifier.width(8.dp))
                    TypeDropdown(selected = type, onSelect = { type = it }) // for AlertType
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("For: ")
                    Spacer(Modifier.width(8.dp))
                    TypeDropdown(selected = alertfor, onSelect = { alertfor = it }) // for AlertFor
                }
            }
            val scope = rememberCoroutineScope() // rememberCoroutineScope can only be called in a @Composable

                Button(onClick = {
                    scope.launch {
                        val isValid = BinanceSymbolsCache.isValidSymbol(context, symbol,alertfor)
                        if (!isValid) {
                            Toast.makeText(context, "Please enter a valid coin symbol from Binance", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        saveAlert() // a new non-suspend internal function for actual save
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save Alert")
                }

            }
        }

        // Alerts List
        Text("Saved Alerts (${alerts.size})", style = MaterialTheme.typography.titleMedium)
        if (alerts.isEmpty()) {
            Text("No alerts yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            alerts.forEach { alert ->
                var isValidSymbol by remember(alert.symbol) { mutableStateOf(true) }
                val isChanged = alert.symbol.isNotEmpty() || (alert.triggeredPrice != null)
                LaunchedEffect(isChanged) {
                    isValidSymbol = BinanceSymbolsCache.isValidSymbol(
                        context,
                        alert.symbol,
                        alertfor
                    )
                }

                AlertCard(
                    alert = alert,
//                    dateFormat = dateFormat,
                    onEdit = {
                        editId = it.id
                        symbol = it.symbol
                        threshold = it.threshold.toString()
                        name = it.name
                        type = it.type
                    },
                    onDelete = { deleteAlert(it.id) },
                    isValidSymbol = isValidSymbol
                )
            }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> TypeDropdown(
    selected: T,
    crossinline onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected.name)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            enumValues<T>().forEach {
                DropdownMenuItem(
                    text = { Text(it.name) },
                    onClick = {
                        onSelect(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AlertCard(
    alert: PriceAlert,
    onEdit: (PriceAlert) -> Unit,
    onDelete: (PriceAlert) -> Unit,
    isValidSymbol: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (!isValidSymbol) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header: title and status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${alert.name} (${alert.symbol})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                val statusConfig = getStatusConfig(alert.status)
                if (isValidSymbol) Card(
                    colors = CardDefaults.cardColors(containerColor = statusConfig.backgroundColor),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Text(
                        text = statusConfig.label,
                        color = statusConfig.textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                } else Icon(Icons.Default.Warning,"Warning", tint = MaterialTheme.colorScheme.error,modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main content
            Column(modifier = Modifier.fillMaxWidth(),) {
                Text(
                    text = "Target: ${alert.threshold} ${alert.type}",
//                    style = MaterialTheme.typography.,
                    maxLines = 1
                )

                Text(
                    text = "Created: ${alert.createdAt.toFormattedDate()}",
//                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                alert.triggeredAt?.let {
                    Text(
                        text = "Triggered: ${it.toFormattedDate()}",
//                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                alert.triggeredPrice?.let {
                    Text(
                        text = "Triggered Price: $it",
//                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alert For: ${alert.alertFor}",
//                    style = MaterialTheme.typography.,
                    maxLines = 1
                )
                Row {
                TextButton(onClick = { onEdit(alert) }) {
                    Text(
                        "Edit",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                TextButton(onClick = { onDelete(alert) }) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                }
            }
        }
    }
}

// Status config remains unchanged
@Composable
private fun getStatusConfig(status: AlertStatus): StatusConfig {
    return when (status) {
        AlertStatus.ACTIVE -> StatusConfig(
            "ACTIVE",
            Color.Green,
            Color.Black
        )
        AlertStatus.TRIGGERED -> StatusConfig(
            "TRIGGERED",
            Color.Black,
            Color.Yellow
        )
        AlertStatus.MISSED -> StatusConfig(
            "MISSED",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// Helper functions and data classes
@Immutable
data class StatusConfig(
    val label: String,
    val backgroundColor: Color,
    val textColor: Color
)

//@Composable
//private fun getStatusConfig(status: AlertStatus): StatusConfig {
//    return when (status) {
//        AlertStatus.ACTIVE -> StatusConfig(
//            "ACTIVE",
//            MaterialTheme.colorScheme.tertiaryContainer,
//            MaterialTheme.colorScheme.onTertiaryContainer
//        )
//        AlertStatus.TRIGGERED -> StatusConfig(
//            "TRIGGERED",
//            MaterialTheme.colorScheme.secondaryContainer,
//            MaterialTheme.colorScheme.onSecondaryContainer
//        )
//        AlertStatus.MISSED -> StatusConfig(
//            "MISSED",
//            MaterialTheme.colorScheme.surfaceVariant,
//            MaterialTheme.colorScheme.onSurfaceVariant
//        )
//    }
//}


fun Long?.toFormattedDate(): String {
    return this?.let { SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(it)) }
        ?: "N/A"
}
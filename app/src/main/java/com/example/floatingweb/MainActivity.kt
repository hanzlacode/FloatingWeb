package com.example.floatingweb

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.floatingweb.helpers.DataStorage
import com.example.floatingweb.ui.theme.FloatingWebTheme

class MainActivity : ComponentActivity() {

    private val overlayRequestCode = 1001
    private val batteryRequestCode = 1002
    private val permissionRequestCode = 1003
    private val cameraPermissionRequestCode = 1004

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataStorage.init(this)

        // Begin the auto permission check sequence
        checkAndRequestAllPermissions()
    }

    // ✅ Checks all required permissions in sequence
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestAllPermissions() {
        when {
            // 1️⃣ Overlay Permission
            !Settings.canDrawOverlays(this) -> {
                Toast.makeText(this, "Grant overlay permission", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, overlayRequestCode)
                return
            }

            // 2️⃣ Battery Optimization Ignore
            !isIgnoringBatteryOptimizations() -> {
                requestBatteryOptimizationIgnore()
                return
            }

            // 3️⃣ Notification Permission (Android 13+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), permissionRequestCode)
                return
            }

            // 4️⃣ Foreground Service Permission (Android 14+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.FOREGROUND_SERVICE
                    ) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissions(arrayOf(Manifest.permission.FOREGROUND_SERVICE), permissionRequestCode)
                return
            }

            !hasCameraAndAudioPermissions() -> {
                requestCameraAndAudioPermissions()
                return
            }

            // ✅ All permissions granted
            else -> {
                launchAppUI()
            }
        }
    }

    private fun hasCameraAndAudioPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraAndAudioPermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ),
            cameraPermissionRequestCode
        )
    }
    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationIgnore() {
        Toast.makeText(this, "Grant battery optimization ignore", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, batteryRequestCode)
    }

    // ✅ Launch your main UI only after all permissions are granted
    private fun launchAppUI() {
        setContent {
            FloatingWebTheme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(navController,this)
                }
            }
        }
        Toast.makeText(this, "✅ All permissions granted", Toast.LENGTH_SHORT).show()
    }

    // ✅ Recheck after returning from system permission screens
    @Deprecated("Used for backward compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Handler(Looper.getMainLooper()).postDelayed({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                checkAndRequestAllPermissions()
        }, 800)
    }

    // ✅ Recheck after runtime permission results
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Delay to ensure system settings apply
        Handler(Looper.getMainLooper()).postDelayed({
            checkAndRequestAllPermissions()
        }, 800)
    }

}

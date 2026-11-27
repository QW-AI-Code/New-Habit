package com.duck.twominute

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.duck.twominute.alarms.BootReceiver
import com.duck.twominute.ui.TwoMinuteRoot

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** Tab requested by a notification tap ("reminders"), consumed by the UI once shown. */
    private val openTabRequest = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        if (savedInstanceState == null) openTabRequest.value = intent?.getStringExtra(EXTRA_OPEN_TAB)
        setContent {
            TwoMinuteRoot(
                openTabRequest = openTabRequest.value,
                onOpenTabHandled = { openTabRequest.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_TAB)?.let { openTabRequest.value = it }
    }

    override fun onResume() {
        super.onResume()
        // The user may have just allowed exact alarms or notifications in system
        // settings: re-arm everything so the change takes effect immediately.
        BootReceiver.rescheduleAsync(this)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_OPEN_TAB = "open_tab"
        const val TAB_REMINDERS = "reminders"
    }
}

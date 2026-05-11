package com.shreshtagaurd.app

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Parent-facing onboarding screen.
 * Guides the parent through the two special permissions required, then starts monitoring.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var btnUsageAccess: Button
    private lateinit var btnOverlayAccess: Button
    private lateinit var btnStart: Button
    private lateinit var imgUsageTick: ImageView
    private lateinit var imgOverlayTick: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnUsageAccess = findViewById(R.id.btn_usage_access)
        btnOverlayAccess = findViewById(R.id.btn_overlay_access)
        btnStart = findViewById(R.id.btn_start_monitoring)
        imgUsageTick = findViewById(R.id.img_usage_tick)
        imgOverlayTick = findViewById(R.id.img_overlay_tick)

        btnUsageAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        btnOverlayAccess.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        btnStart.setOnClickListener {
            startMonitoringService()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
    }

    private fun refreshPermissionStatus() {
        val usageGranted = hasUsageStatsPermission()
        val overlayGranted = Settings.canDrawOverlays(this)

        imgUsageTick.setImageResource(
            if (usageGranted) R.drawable.ic_check else R.drawable.ic_cross
        )
        imgOverlayTick.setImageResource(
            if (overlayGranted) R.drawable.ic_check else R.drawable.ic_cross
        )

        btnStart.isEnabled = usageGranted && overlayGranted
        btnStart.alpha = if (btnStart.isEnabled) 1f else 0.4f
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun startMonitoringService() {
        val intent = Intent(this, UsageMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
        // Visual feedback — button text changes to indicate active monitoring
        btnStart.text = getString(R.string.monitoring_active)
        btnStart.isEnabled = false
        btnStart.alpha = 0.6f
    }
}

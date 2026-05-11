package com.shreshtagaurd.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class UsageMonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "shreshta_guard_channel"
        private const val NOTIFICATION_ID = 1
        private const val POLL_INTERVAL_MS = 10_000L      // 10 seconds
        private const val WARNING_THRESHOLD_MS = 5 * 60 * 1000L  // 5 minutes

        private val MONITORED_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.kids"
        )

        // Broadcast action sent to WarningOverlayActivity when overlay should close and reset
        const val ACTION_RESET_TIMER = "com.shreshtagaurd.app.RESET_TIMER"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var youtubeActiveMs = 0L
    private var lastPollTimeMs = 0L
    private var overlayShowing = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESET_TIMER) {
            resetTimer()
        } else {
            handler.removeCallbacks(pollRunnable)
            handler.post(pollRunnable)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
    }

    // ---------------------------------------------------------------------------
    // Core logic
    // ---------------------------------------------------------------------------

    private fun checkForegroundApp() {
        if (overlayShowing) return

        val foregroundPackage = getForegroundPackage() ?: return
        val nowMs = System.currentTimeMillis()

        if (foregroundPackage in MONITORED_PACKAGES) {
            if (lastPollTimeMs > 0) {
                val elapsed = nowMs - lastPollTimeMs
                // Only credit time if polling interval is close to expected (avoids crediting
                // large gaps caused by the device sleeping with the screen off).
                if (elapsed < POLL_INTERVAL_MS * 3) {
                    youtubeActiveMs += elapsed
                }
            }
            lastPollTimeMs = nowMs

            if (youtubeActiveMs >= WARNING_THRESHOLD_MS) {
                triggerWarning()
            }
        } else {
            // YouTube not in foreground — pause the timer
            lastPollTimeMs = 0L
        }
    }

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10_000L  // look back 10 seconds

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun triggerWarning() {
        overlayShowing = true
        val intent = Intent(this, WarningOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    fun resetTimer() {
        youtubeActiveMs = 0L
        lastPollTimeMs = 0L
        overlayShowing = false
    }

    // ---------------------------------------------------------------------------
    // Notification
    // ---------------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ShreshtaGuard Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps ShreshtaGuard running in the background"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}

package com.shreshtagaurd.app

import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Full-screen overlay activity that plays the warning video.
 *
 * Design constraints from PRD:
 * - Covers entire screen (no status bar, no nav bar)
 * - Back and home are consumed during playback
 * - Volume forced to maximum
 * - Dismisses automatically when video ends; timer is then reset
 * - No visible close/dismiss button
 */
class WarningOverlayActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyFullscreenFlags()
        forceMaxVolume()

        setContentView(R.layout.activity_warning_overlay)
        hideSystemUI()

        initPlayer()
    }

    // ---------------------------------------------------------------------------
    // Window / display flags
    // ---------------------------------------------------------------------------

    private fun applyFullscreenFlags() {
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    private fun hideSystemUI() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    // ---------------------------------------------------------------------------
    // Volume
    // ---------------------------------------------------------------------------

    private fun forceMaxVolume() {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
    }

    // ---------------------------------------------------------------------------
    // ExoPlayer
    // ---------------------------------------------------------------------------

    private fun initPlayer() {
        val videoView = findViewById<androidx.media3.ui.PlayerView>(R.id.player_view)

        player = ExoPlayer.Builder(this).build().also { exo ->
            videoView.player = exo
            videoView.useController = false  // hide all controls

            val rawUri = Uri.parse("android.resource://$packageName/${R.raw.warning_video}")
            exo.setMediaItem(MediaItem.fromUri(rawUri))
            exo.repeatMode = Player.REPEAT_MODE_OFF
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        onVideoFinished()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    // Missing/corrupt video — fail safe: close overlay immediately
                    onVideoFinished()
                }
            })
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    private fun onVideoFinished() {
        // Tell the service to reset its timer
        val resetIntent = Intent(this, UsageMonitorService::class.java).apply {
            action = UsageMonitorService.ACTION_RESET_TIMER
        }
        startService(resetIntent)
        finish()
    }

    // ---------------------------------------------------------------------------
    // Block hardware keys during playback
    // ---------------------------------------------------------------------------

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Consume back, volume-down, and all other keys so 3-year-old can't dismiss
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_VOLUME_DOWN -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onBackPressed() {
        // Intentionally do nothing — consumed
    }

    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        player?.play()
    }

    override fun onPause() {
        super.onPause()
        // Keep playing even if something tries to push us to background
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}

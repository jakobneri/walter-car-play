package dev.walter.carplay.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.*
import androidx.core.app.NotificationCompat
import dev.walter.carplay.LauncherActivity
import dev.walter.carplay.MainActivity
import dev.walter.carplay.R
import dev.walter.carplay.data.AppPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class CarModeService : Service() {

    private lateinit var audioManager: AudioManager
    private lateinit var prefs: AppPreferences
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var launchJob: Job? = null

    private val audioCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<AudioDeviceInfo>) {
            if (added.any { it.isAudioOutput() }) onCableConnected()
        }

        override fun onAudioDevicesRemoved(removed: Array<AudioDeviceInfo>) {
            if (removed.any { it.isAudioOutput() }) launchJob?.cancel()
        }
    }

    private fun AudioDeviceInfo.isAudioOutput() = isSink && type in AUDIO_OUTPUT_TYPES

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        prefs = AppPreferences(this)
        createNotificationChannels()
        startForeground(NOTIF_ID, buildMonitorNotification())
        audioManager.registerAudioDeviceCallback(audioCallback, Handler(Looper.getMainLooper()))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        audioManager.unregisterAudioDeviceCallback(audioCallback)
        scope.cancel()
        super.onDestroy()
    }

    private fun onCableConnected() {
        launchJob?.cancel()
        launchJob = scope.launch {
            if (!prefs.serviceEnabled.first()) return@launch

            delay(prefs.delaySeconds.first() * 1000L)

            val packages = buildList {
                if (prefs.launchYoutubeMusic.first()) add(YT_MUSIC_PKG)
                if (prefs.launchBlitzer.first()) add(prefs.blitzerPackage.first())
            }
            if (packages.isEmpty()) return@launch

            val launchIntent = Intent(this@CarModeService, LauncherActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putStringArrayListExtra(LauncherActivity.EXTRA_PACKAGES, ArrayList(packages))
            }

            // fullScreenIntent zeigt die Activity auch bei gesperrtem Bildschirm
            val pi = PendingIntent.getActivity(
                this@CarModeService, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alertNotif = NotificationCompat.Builder(this@CarModeService, ALERT_CHANNEL_ID)
                .setContentTitle("Kabel erkannt – starte Apps…")
                .setSmallIcon(R.drawable.ic_car)
                .setFullScreenIntent(pi, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .notify(ALERT_NOTIF_ID, alertNotif)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(MONITOR_CHANNEL_ID, "CarPlay Überwachung", NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL_ID, "CarPlay Start", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun buildMonitorNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, MONITOR_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_car)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val YT_MUSIC_PKG = "com.google.android.apps.youtube.music"
        private const val MONITOR_CHANNEL_ID = "car_monitor"
        private const val ALERT_CHANNEL_ID = "car_alert"
        private const val NOTIF_ID = 1
        private const val ALERT_NOTIF_ID = 2

        private val AUDIO_OUTPUT_TYPES = setOf(
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        )

        fun start(ctx: Context) {
            val intent = Intent(ctx, CarModeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(intent)
            else ctx.startService(intent)
        }

        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, CarModeService::class.java))
    }
}

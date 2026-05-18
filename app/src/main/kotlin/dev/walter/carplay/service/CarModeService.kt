package dev.walter.carplay.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.*
import android.view.WindowManager
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
    private var wakeLock: PowerManager.WakeLock? = null

    private val audioCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<AudioDeviceInfo>) {
            if (added.any { it.isSink && it.type in AUDIO_OUTPUT_TYPES }) onCableConnected()
        }
        override fun onAudioDevicesRemoved(removed: Array<AudioDeviceInfo>) {
            if (removed.any { it.isSink && it.type in AUDIO_OUTPUT_TYPES }) {
                launchJob?.cancel()
                releaseWakeLock()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        prefs = AppPreferences(this)
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        audioManager.registerAudioDeviceCallback(audioCallback, Handler(Looper.getMainLooper()))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TEST) onCableConnected()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        audioManager.unregisterAudioDeviceCallback(audioCallback)
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    private fun onCableConnected() {
        launchJob?.cancel()
        launchJob = scope.launch {
            if (!prefs.serviceEnabled.first()) return@launch

            delay(prefs.delaySeconds.first() * 1000L)

            if (prefs.keepScreenOn.first()) acquireWakeLock()

            if (prefs.setVolume.first()) {
                val pct = prefs.volumeLevel.first() / 100f
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (max * pct).toInt(), 0)
            }

            val packages = prefs.appList.first().filter { it.isNotBlank() }
            if (packages.isEmpty()) return@launch

            val launchIntent = Intent(this@CarModeService, LauncherActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putStringArrayListExtra(LauncherActivity.EXTRA_PACKAGES, ArrayList(packages))
            }
            val pi = PendingIntent.getActivity(
                this@CarModeService, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alert = NotificationCompat.Builder(this@CarModeService, ALERT_CHANNEL_ID)
                .setContentTitle("Kabel erkannt – starte Apps…")
                .setSmallIcon(R.drawable.ic_car)
                .setFullScreenIntent(pi, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(ALERT_NOTIF_ID, alert)
        }
    }

    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "walter:carmode"
        ).apply { acquire(4 * 60 * 60 * 1000L) }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            // IMPORTANCE_MIN = kein Icon in der Statusleiste, unsichtbar im Alltag
            nm.createNotificationChannel(
                NotificationChannel(MONITOR_CHANNEL_ID, "CarPlay", NotificationManager.IMPORTANCE_MIN)
                    .apply { setShowBadge(false) }
            )
            nm.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL_ID, "CarPlay Start", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, MONITOR_CHANNEL_ID)
            .setContentTitle("Walter CarPlay")
            .setSmallIcon(R.drawable.ic_car)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
    }

    companion object {
        const val ACTION_TEST = "dev.walter.carplay.TEST"
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
            val i = Intent(ctx, CarModeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, CarModeService::class.java))

        fun testLaunch(ctx: Context) {
            ctx.startService(Intent(ctx, CarModeService::class.java).apply { action = ACTION_TEST })
        }
    }
}

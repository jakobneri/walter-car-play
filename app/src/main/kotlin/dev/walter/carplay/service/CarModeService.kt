package dev.walter.carplay.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.*
import android.util.Log
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
            Log.d(TAG, "onAudioDevicesAdded: ${added.map { "${it.type}(${it.productName})" }}")
            val match = added.firstOrNull { it.isSink && it.type in AUDIO_OUTPUT_TYPES }
            if (match != null) {
                Log.d(TAG, "Cable connected – matched device type ${match.type}")
                onCableConnected()
            }
        }
        override fun onAudioDevicesRemoved(removed: Array<AudioDeviceInfo>) {
            Log.d(TAG, "onAudioDevicesRemoved: ${removed.map { "${it.type}(${it.productName})" }}")
            if (removed.any { it.isSink && it.type in AUDIO_OUTPUT_TYPES }) {
                Log.d(TAG, "Cable removed – cancelling launch job")
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
        Log.d(TAG, "onCableConnected called")
        launchJob?.cancel()
        launchJob = scope.launch {
            val enabled = prefs.serviceEnabled.first()
            Log.d(TAG, "serviceEnabled=$enabled")
            if (!enabled) return@launch

            val delaySec = prefs.delaySeconds.first()
            Log.d(TAG, "Waiting ${delaySec}s before launching apps")
            delay(delaySec * 1000L)

            if (prefs.keepScreenOn.first()) acquireWakeLock()

            if (prefs.setVolume.first()) {
                val pct = prefs.volumeLevel.first() / 100f
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val target = (max * pct).toInt()
                Log.d(TAG, "Setting volume to $target/$max")
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            }

            val packages = prefs.appList.first().filter { it.isNotBlank() }
            Log.d(TAG, "App list from prefs: $packages")
            if (packages.isEmpty()) {
                Log.w(TAG, "App list is empty – nothing to launch")
                return@launch
            }

            val launchIntent = Intent(this@CarModeService, LauncherActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putStringArrayListExtra(LauncherActivity.EXTRA_PACKAGES, ArrayList(packages))
            }
            val pi = PendingIntent.getActivity(
                this@CarModeService, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val canFullScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                    nm.canUseFullScreenIntent()
            Log.d(TAG, "canUseFullScreenIntent=$canFullScreen – firing notification")
            val alert = NotificationCompat.Builder(this@CarModeService, ALERT_CHANNEL_ID)
                .setContentTitle("Kabel erkannt – Apps starten")
                .setContentText(if (canFullScreen) "Wird geöffnet…" else "Tippen zum Öffnen")
                .setSmallIcon(R.drawable.ic_car)
                .setContentIntent(pi)
                .apply { if (canFullScreen) setFullScreenIntent(pi, true) }
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            nm.notify(ALERT_NOTIF_ID, alert)
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
        private const val TAG = "WalterService"
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

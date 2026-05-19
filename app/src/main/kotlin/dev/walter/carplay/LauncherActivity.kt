package dev.walter.carplay

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity

class LauncherActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packages = intent.getStringArrayListExtra(EXTRA_PACKAGES)
            ?.filter { it.isNotBlank() }
        Log.d(TAG, "onCreate – packages: $packages")

        if (packages.isNullOrEmpty()) {
            Log.e(TAG, "No packages – finishing")
            finish()
            return
        }

        launchNext(packages, 0)
    }

    private fun launchNext(packages: List<String>, index: Int) {
        if (index >= packages.size) {
            Log.d(TAG, "All packages processed – finishing")
            finish()
            return
        }

        val pkg = packages[index]
        Log.d(TAG, "Launching $pkg (${index + 1}/${packages.size})")
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent == null) {
            Log.w(TAG, "No launch intent for $pkg – skipping")
        } else {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(launchIntent)
                Log.d(TAG, "startActivity ok: $pkg")
            } catch (e: Exception) {
                Log.e(TAG, "startActivity failed for $pkg: ${e.message}")
            }
        }

        // Give each app (except the last) 1.5s to reach onResume before the
        // next app takes the foreground.
        if (index < packages.size - 1) {
            handler.postDelayed({ launchNext(packages, index + 1) }, 1500)
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PACKAGES = "packages"
        private const val TAG = "WalterLauncher"
    }
}

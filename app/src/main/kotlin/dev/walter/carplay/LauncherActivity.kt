package dev.walter.carplay

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packages = intent.getStringArrayListExtra(EXTRA_PACKAGES)
        Log.d(TAG, "onCreate – packages from intent: $packages")

        if (packages == null) {
            Log.e(TAG, "No packages extra found – finishing immediately")
            finish()
            return
        }

        // Launch all apps while still in foreground – Android queues the starts,
        // last app in list ends up on top. Delaying between launches causes
        // background-activity-launch blocks on Android 10+.
        packages.forEach { pkg ->
            Log.d(TAG, "Trying to launch: $pkg")
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent == null) {
                Log.w(TAG, "getLaunchIntentForPackage returned null for: $pkg (not installed?)")
            } else {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(launchIntent)
                    Log.d(TAG, "startActivity succeeded for $pkg")
                } catch (e: Exception) {
                    Log.e(TAG, "startActivity failed for $pkg: ${e.message}")
                }
            }
        }

        Log.d(TAG, "All packages processed – finishing")
        finish()
    }

    companion object {
        const val EXTRA_PACKAGES = "packages"
        private const val TAG = "WalterLauncher"
    }
}

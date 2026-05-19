package dev.walter.carplay

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packages = intent.getStringArrayListExtra(EXTRA_PACKAGES)?.filter { it.isNotBlank() }
        Log.d(TAG, "onCreate – packages: $packages")

        if (packages.isNullOrEmpty()) {
            Log.e(TAG, "No packages – finishing")
            finish()
            return
        }

        packages.forEach { pkg ->
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent == null) {
                Log.w(TAG, "No launch intent for $pkg – skipping")
            } else {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(launchIntent)
                    Log.d(TAG, "Launched: $pkg")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch $pkg: ${e.message}")
                }
            }
        }

        finish()
    }

    companion object {
        const val EXTRA_PACKAGES = "packages"
        private const val TAG = "WalterLauncher"
    }
}

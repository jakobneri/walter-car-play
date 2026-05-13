package dev.walter.carplay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Transparente Trampolin-Activity: öffnet Ziel-Apps und schließt sich sofort wieder.
 * Wird über einen fullScreenIntent aus CarModeService gestartet, was auch auf
 * gesperrten Bildschirmen und Android 10+ funktioniert.
 */
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packages = intent.getStringArrayListExtra(EXTRA_PACKAGES) ?: return finish()

        for (pkg in packages) {
            packageManager.getLaunchIntentForPackage(pkg)
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                ?.let {
                    startActivity(it)
                    Thread.sleep(400) // kurze Pause damit erste App in den Vordergrund kommt
                }
        }

        finish()
    }

    companion object {
        const val EXTRA_PACKAGES = "packages"
    }
}

package dev.walter.carplay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packages = intent.getStringArrayListExtra(EXTRA_PACKAGES) ?: run { finish(); return }

        lifecycleScope.launch {
            packages.forEach { pkg ->
                packageManager.getLaunchIntentForPackage(pkg)
                    ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    ?.let { startActivity(it) }
                delay(600)
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_PACKAGES = "packages"
    }
}

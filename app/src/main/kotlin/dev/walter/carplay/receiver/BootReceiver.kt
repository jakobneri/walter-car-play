package dev.walter.carplay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.walter.carplay.data.AppPreferences
import dev.walter.carplay.service.CarModeService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val validActions = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
        if (intent.action !in validActions) return

        val enabled = runBlocking { AppPreferences(ctx).serviceEnabled.first() }
        if (enabled) CarModeService.start(ctx)
    }
}

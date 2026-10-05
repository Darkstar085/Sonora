package com.sipun.sonora.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sipun.sonora.data.preferences.SonoraPreferences

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SonoraPreferences(context).clearLastPlayed()
        }
    }
}

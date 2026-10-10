package com.sipun.sonora.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class SonoraWidgetActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.action?.let { action ->
            sendBroadcast(
                Intent(this, SonoraWidgetActionReceiver::class.java)
                    .setAction(action),
            )
        }
        finish()
    }
}

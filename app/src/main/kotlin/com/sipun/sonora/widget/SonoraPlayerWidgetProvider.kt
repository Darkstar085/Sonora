package com.sipun.sonora.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent

abstract class SonoraPlayerWidgetProvider : AppWidgetProvider() {
    protected abstract val layoutResId: Int
    protected abstract val fullPlayer: Boolean

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        SonoraWidgetRenderer.update(
            context,
            appWidgetManager,
            appWidgetIds,
            layoutResId,
            fullPlayer,
        )
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action != SonoraWidgetActions.ACTION_REFRESH) return

        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, javaClass))
        if (ids.isNotEmpty()) {
            onUpdate(context, manager, ids)
        }
    }
}

class SonoraMiniPlayerWidgetProvider : SonoraPlayerWidgetProvider() {
    override val layoutResId: Int = com.sipun.sonora.R.layout.widget_mini_player
    override val fullPlayer: Boolean = false
}

class SonoraFullPlayerWidgetProvider : SonoraPlayerWidgetProvider() {
    override val layoutResId: Int = com.sipun.sonora.R.layout.widget_full_player
    override val fullPlayer: Boolean = true
}
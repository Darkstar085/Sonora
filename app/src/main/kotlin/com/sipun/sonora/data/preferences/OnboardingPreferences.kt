package com.sipun.sonora.data.preferences

import android.content.Context

class OnboardingPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE,
    )

    fun isWelcomeComplete(): Boolean =
        preferences.getBoolean(WELCOME_COMPLETE, false)

    fun setWelcomeComplete() {
        preferences.edit()
            .putBoolean(WELCOME_COMPLETE, true)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "sonora_onboarding"
        const val WELCOME_COMPLETE = "welcome_complete"
    }
}

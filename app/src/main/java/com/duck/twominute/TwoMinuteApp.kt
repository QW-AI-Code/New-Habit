package com.duck.twominute

import android.app.Application
import com.duck.twominute.ai.network.GeminiUsageHub
import com.duck.twominute.ai.usage.TokenUsageRepository
import com.duck.twominute.alarms.BootReceiver

class TwoMinuteApp : Application() {

    val store: AppStore by lazy { AppStore(this) }

    override fun onCreate() {
        super.onCreate()
        // Exact token and request counts of every Gemini answer (v1.0.1 AI planner).
        GeminiUsageHub.listener = TokenUsageRepository.get(this)
        // Make sure reminders survive process death or a missed boot broadcast.
        BootReceiver.rescheduleAsync(this)
    }
}

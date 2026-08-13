package com.duck.twominute

import android.app.Application
import com.duck.twominute.alarms.BootReceiver

class TwoMinuteApp : Application() {

    val store: AppStore by lazy { AppStore(this) }

    override fun onCreate() {
        super.onCreate()
        // Make sure reminders survive process death or a missed boot broadcast.
        BootReceiver.rescheduleAsync(this)
    }
}

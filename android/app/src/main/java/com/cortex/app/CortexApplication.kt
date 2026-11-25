package com.cortex.app

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.cortex.app.background.SyncScheduler
import com.cortex.app.database.CortexDatabase

class CortexApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Chaquopy Python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // 2. Pre-initialize Room Database & Seed Permanent Denylist
        CortexDatabase.getInstance(this)

        // 3. Register Deferrable Background WorkManager Sync
        SyncScheduler.schedulePeriodicSync(this)
    }
}

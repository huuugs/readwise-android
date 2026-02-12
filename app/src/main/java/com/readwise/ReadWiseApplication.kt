package com.readwise

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReadWiseApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize app-wide components
    }
}

package com.google.codelabs.appauth

import android.app.Application

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }

    companion object {
        public const val LOG_TAG = "AppAuthSample"
    }
}

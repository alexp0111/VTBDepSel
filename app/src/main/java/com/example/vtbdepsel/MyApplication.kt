package com.example.vtbdepsel

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.HiltAndroidApp


/**
 * Application class for dagger hilt & MapKit init
 * */
@HiltAndroidApp
class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
    }
}
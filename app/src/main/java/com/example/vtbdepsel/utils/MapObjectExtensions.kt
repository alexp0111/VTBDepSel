package com.example.vtbdepsel.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.vtbdepsel.R
import com.yandex.mapkit.map.PolylineMapObject

/**
 * Extensions for working with polyline styling
 * */

// Main route
fun PolylineMapObject.styleMainRoute(context: Context) {
    zIndex = 10f
    setStrokeColor(ContextCompat.getColor(context, R.color.md_theme_dark_primary))
    strokeWidth = 5f
    outlineColor = ContextCompat.getColor(context, R.color.black)
    outlineWidth = 3f
}

// Alternative route
fun PolylineMapObject.styleAlternativeRoute(context: Context) {
    zIndex = 5f
    setStrokeColor(ContextCompat.getColor(context, R.color.md_theme_dark_outlineVariant))
    strokeWidth = 4f
    outlineColor = ContextCompat.getColor(context, R.color.black)
    outlineWidth = 2f
}
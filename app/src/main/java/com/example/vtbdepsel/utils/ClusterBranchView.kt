package com.example.vtbdepsel.utils

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.vtbdepsel.R

/**
 * Custom cluster view for collapsable collection of ATM's
 * */
class ClusterBranchView(context: Context) : LinearLayout(context) {

    private val lowText by lazy { findViewById<TextView>(R.id.text_low_pins) }
    private val midText by lazy { findViewById<TextView>(R.id.text_mid_pins) }
    private val highText by lazy { findViewById<TextView>(R.id.text_high_pins) }
    private val prText by lazy { findViewById<TextView>(R.id.text_premium_pins) }

    private val lowLayout by lazy { findViewById<View>(R.id.layout_low_group) }
    private val midLayout by lazy { findViewById<View>(R.id.layout_mid_group) }
    private val highLayout by lazy { findViewById<View>(R.id.layout_high_group) }
    private val prLayout by lazy { findViewById<View>(R.id.layout_premium_group) }

    init {
        inflate(context, R.layout.cluster_branch_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        orientation = HORIZONTAL
        setBackgroundResource(R.drawable.cluster_view_background)
    }

    fun setData(placemarkTypes: List<PlacemarkType>) {
        PlacemarkType.values().forEach {
            updateViews(placemarkTypes, it)
        }
    }

    private fun updateViews(
        placemarkTypes: List<PlacemarkType>,
        type: PlacemarkType
    ) {
        val (textView, layoutView) = when (type) {
            PlacemarkType.LOW -> lowText to lowLayout
            PlacemarkType.MEDIUM -> midText to midLayout
            PlacemarkType.HIGH -> highText to highLayout
            PlacemarkType.PREMIUM -> prText to prLayout
        }
        val value = placemarkTypes.countTypes(type)

        textView.text = value.toString()
        layoutView.isVisible = value != 0
    }

    private fun List<PlacemarkType>.countTypes(type: PlacemarkType) = count { it == type }
}
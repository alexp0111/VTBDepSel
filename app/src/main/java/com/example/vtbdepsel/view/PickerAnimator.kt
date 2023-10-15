package com.example.vtbdepsel.view

import android.animation.ObjectAnimator
import android.content.res.Resources
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.example.vtbdepsel.R

/**
 * Special animator for picker
 * */
object PickerAnimator {

    fun animate(
        resources: Resources,
        picker: MutableMap<ConstraintLayout, ImageView>,
        pickerCircle: ConstraintLayout
    ) {
        for (mutableEntry in picker) {
            mutableEntry.key.setOnClickListener {
                setUpPickerTransition(resources, mutableEntry, picker, pickerCircle)
            }
        }
    }

    private fun setUpPickerTransition(
        resources: Resources,
        entry: MutableMap.MutableEntry<ConstraintLayout, ImageView>,
        picker: MutableMap<ConstraintLayout, ImageView>,
        cursor: ConstraintLayout
    ) {
        val anim = ObjectAnimator.ofFloat(
            cursor,
            "translationX",
            entry.key.x - (8.0f * resources.displayMetrics.density + 0.5f)
        )
        anim.doOnStart {
            picker.forEach { i ->
                when (i.key.transitionName) {
                    "cl_picker_1" -> i.value.setImageResource(R.drawable.auto_unpick)
                    "cl_picker_2" -> i.value.setImageResource(R.drawable.ped_unpick)
                    "cl_picker_3" -> i.value.setImageResource(R.drawable.bus_unpick)
                }
                i.key.elevation = 0f
            }
        }
        anim.doOnEnd {
            picker.forEach { i ->
                if (i != entry) {
                    when (i.key.transitionName) {
                        "cl_picker_1" -> i.value.setImageResource(R.drawable.auto_unpick)
                        "cl_picker_2" -> i.value.setImageResource(R.drawable.ped_unpick)
                        "cl_picker_3" -> i.value.setImageResource(R.drawable.bus_unpick)
                    }
                }
            }
            when (entry.key.transitionName) {
                "cl_picker_1" -> entry.value.setImageResource(R.drawable.auto_pick)
                "cl_picker_2" -> entry.value.setImageResource(R.drawable.ped_pick)
                "cl_picker_3" -> entry.value.setImageResource(R.drawable.bus_pick)
            }
            entry.key.elevation = resources.getDimension(R.dimen.cl_elevation) + 1f
        }
        anim.start()
    }
}

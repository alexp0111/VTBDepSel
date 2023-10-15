package com.example.vtbdepsel.view

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.vtbdepsel.R

/**
 * Adapter for showing workig hours
 * */
class MyListAdapter(
    private val context: Activity,
    private val dayList: Array<String>,
    private val hourList: Array<String>,
) : ArrayAdapter<String>(context, R.layout.list_item, dayList) {

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.list_item, null, true)

        val dayText = rowView.findViewById(R.id.item_day) as TextView
        val hourText = rowView.findViewById(R.id.item_hours) as TextView

        dayText.text = dayList[position]
        hourText.text = hourList[position]

        return rowView
    }
}
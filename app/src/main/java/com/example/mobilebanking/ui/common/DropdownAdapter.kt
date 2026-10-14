package com.example.mobilebanking.ui.common

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.example.mobilebanking.R

class DropdownAdapter(
    context: Context,
    private val items: List<String>,
) : ArrayAdapter<String>(context, R.layout.item_dropdown, items) {

    private val showAllFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?) =
            FilterResults().apply {
                values = items
                count = items.size
            }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) = Unit
    }

    override fun getFilter(): Filter = showAllFilter
}

package com.akeem.mobilebanking.ui.common

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.akeem.mobilebanking.R

/**
 * Adapter for read-only exposed dropdowns. The default ArrayAdapter filters its list by the text
 * currently in the field, so once an item is selected the menu shows only that item (or nothing
 * after the field is cleared). This adapter always shows every option.
 */
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

package com.akeem.mobilebanking.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.res.use
import com.akeem.mobilebanking.R
import com.akeem.mobilebanking.databinding.ViewDetailRowBinding

/** Reusable "label ........ value" row used on the confirmation, result and details screens. */
class DetailRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewDetailRowBinding

    init {
        orientation = HORIZONTAL
        binding = ViewDetailRowBinding.inflate(LayoutInflater.from(context), this)
        context.obtainStyledAttributes(attrs, R.styleable.DetailRowView).use { a ->
            a.getString(R.styleable.DetailRowView_rowLabel)?.let { binding.rowLabel.text = it }
            a.getString(R.styleable.DetailRowView_rowValue)?.let { binding.rowValue.text = it }
        }
    }

    fun setValue(value: CharSequence?) {
        binding.rowValue.text = value
    }
}

package com.example.mobilebanking.ui.common

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import com.example.mobilebanking.R
import com.example.mobilebanking.databinding.ViewDetailRowBinding

@RequiresApi(Build.VERSION_CODES.S)
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

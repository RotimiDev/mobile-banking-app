package com.akeem.mobilebanking.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.akeem.mobilebanking.R
import com.akeem.mobilebanking.databinding.ItemTransactionBinding
import com.akeem.mobilebanking.domain.model.Transaction
import com.akeem.mobilebanking.domain.model.TransactionType
import com.akeem.mobilebanking.ui.common.bindStatusBadge
import com.akeem.mobilebanking.ui.common.colorRes
import com.akeem.mobilebanking.ui.common.formattedAmount
import com.akeem.mobilebanking.ui.common.iconRes
import com.akeem.mobilebanking.ui.common.labelRes

class TransactionAdapter(
    private val onClick: (Transaction) -> Unit,
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) = with(binding) {
            val context = root.context
            title.text = transaction.title
            date.text = transaction.date
            amount.text = transaction.formattedAmount()
            amount.setTextColor(ContextCompat.getColor(context, transaction.type.colorRes()))
            status.bindStatusBadge(transaction.status)

            typeIcon.setImageResource(transaction.type.iconRes())
            ImageViewCompat.setImageTintList(
                typeIcon,
                ColorStateList.valueOf(ContextCompat.getColor(context, transaction.type.colorRes())),
            )

            // One coherent announcement for screen readers instead of five fragments.
            root.contentDescription = context.getString(
                R.string.transaction_row_description,
                transaction.title,
                context.getString(transaction.type.labelRes()) + " " + transaction.formattedAmount(),
                context.getString(transaction.status.labelRes()),
                transaction.date,
            )
            root.setOnClickListener { onClick(transaction) }
        }

    }

    private object DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem == newItem
    }
}

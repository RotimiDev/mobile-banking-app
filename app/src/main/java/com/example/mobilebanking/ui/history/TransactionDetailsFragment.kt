package com.example.mobilebanking.ui.history

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mobilebanking.R
import com.example.mobilebanking.databinding.FragmentTransactionDetailsBinding
import com.example.mobilebanking.domain.model.Transaction
import com.example.mobilebanking.domain.money.MoneyFormatter
import com.example.mobilebanking.ui.common.bindStatusBadge
import com.example.mobilebanking.ui.common.formattedAmount
import com.example.mobilebanking.ui.common.labelRes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransactionDetailsFragment : Fragment(R.layout.fragment_transaction_details) {

    private val viewModel: TransactionDetailsViewModel by viewModels()

    private var _binding: FragmentTransactionDetailsBinding? = null
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTransactionDetailsBinding.bind(view)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transaction.collect(::render)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun render(transaction: Transaction?) = with(binding) {
        content.isVisible = transaction != null
        notFound.isVisible = transaction == null
        if (transaction == null) return@with

        heroAmount.text = transaction.formattedAmount()
        heroStatus.bindStatusBadge(transaction.status)
        rowReference.setValue(transaction.reference)
        rowDescription.setValue(transaction.title)
        rowAmount.setValue(MoneyFormatter.format(transaction.amount))
        rowDate.setValue(transaction.date)
        rowType.setValue(getString(transaction.type.labelRes()))
        rowStatus.setValue(getString(transaction.status.labelRes()))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

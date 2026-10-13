package com.akeem.mobilebanking.ui.history

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.akeem.mobilebanking.R
import com.akeem.mobilebanking.databinding.FragmentTransactionDetailsBinding
import com.akeem.mobilebanking.domain.model.Transaction
import com.akeem.mobilebanking.domain.money.MoneyFormatter
import com.akeem.mobilebanking.ui.common.bindStatusBadge
import com.akeem.mobilebanking.ui.common.formattedAmount
import com.akeem.mobilebanking.ui.common.labelRes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransactionDetailsFragment : Fragment(R.layout.fragment_transaction_details) {

    private val viewModel: TransactionDetailsViewModel by viewModels()

    private var _binding: FragmentTransactionDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTransactionDetailsBinding.bind(view)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.transaction.collect(::render)
            }
        }
    }

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

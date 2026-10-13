package com.akeem.mobilebanking.ui.transfer

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.akeem.mobilebanking.R
import com.akeem.mobilebanking.databinding.FragmentConfirmationBinding
import com.akeem.mobilebanking.domain.money.MoneyFormatter
import com.akeem.mobilebanking.ui.common.displayName
import com.akeem.mobilebanking.ui.common.navigateSafely
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConfirmationFragment : Fragment(R.layout.fragment_confirmation) {

    private val viewModel: TransferViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private var _binding: FragmentConfirmationBinding? = null
    private val binding get() = _binding!!

    /** While a transfer is in flight, back is swallowed so the customer can't leave mid-request. */
    private val blockBackWhileBusy = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = Unit
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentConfirmationBinding.bind(view)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, blockBackWhileBusy)

        binding.confirmButton.setOnClickListener { viewModel.confirmTransfer() }
        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: TransferUiState) = with(binding) {
        val context = requireContext()
        val draft = state.draft
        if (draft == null) {
            findNavController().popBackStack(R.id.transferFormFragment, false)
            return@with
        }

        rowDebitAccount.setValue(draft.debitAccount.displayName(context))
        rowBank.setValue(draft.bank.name)
        rowBeneficiaryName.setValue(draft.beneficiaryName)
        rowBeneficiaryAccount.setValue(draft.accountNumber)
        rowAmount.setValue(MoneyFormatter.format(draft.amounts.amountKobo))
        rowFee.setValue(MoneyFormatter.format(draft.amounts.feeKobo))
        rowTotal.setValue(MoneyFormatter.format(draft.amounts.totalDebitKobo))
        rowNarration.setValue(draft.narration.ifEmpty { getString(R.string.narration_none) })

        processingOverlay.isVisible = state.isBusy
        confirmButton.isEnabled = !state.isBusy
        cancelButton.isEnabled = !state.isBusy
        blockBackWhileBusy.isEnabled = state.isBusy

        if (state.process is TransferProcessState.Finished) {
            navigateSafely(R.id.confirmationFragment, R.id.action_confirmation_to_result)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

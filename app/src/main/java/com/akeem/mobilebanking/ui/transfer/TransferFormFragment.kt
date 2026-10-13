package com.akeem.mobilebanking.ui.transfer

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.akeem.mobilebanking.R
import com.akeem.mobilebanking.databinding.FragmentTransferFormBinding
import com.akeem.mobilebanking.domain.model.Account
import com.akeem.mobilebanking.domain.model.ResolutionFailure
import com.akeem.mobilebanking.domain.model.ResolutionState
import com.akeem.mobilebanking.domain.money.MoneyFormatter
import com.akeem.mobilebanking.ui.common.navigateSafely
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransferFormFragment : Fragment(R.layout.fragment_transfer_form) {

    private val viewModel: TransferViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private var _binding: FragmentTransferFormBinding? = null
    private val binding get() = _binding!!

    private var renderedAccountLabels: List<String> = emptyList()
    private var renderedBankNames: List<String> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTransferFormBinding.bind(view)
        // A new view has no adapters yet, so forget what the previous view was given.
        renderedAccountLabels = emptyList()
        renderedBankNames = emptyList()

        // The ViewModel owns the form state: seed the views from it (inputs have saveEnabled=false),
        // then start listening so we never echo our own writes back.
        val initial = viewModel.uiState.value
        binding.accountNumberInput.setText(initial.accountNumber)
        binding.amountInput.setText(initial.amountInput)
        binding.narrationInput.setText(initial.narration)

        binding.accountNumberInput.doAfterTextChanged { viewModel.onAccountNumberChanged(it?.toString().orEmpty()) }
        binding.amountInput.doAfterTextChanged { viewModel.onAmountChanged(it?.toString().orEmpty()) }
        binding.narrationInput.doAfterTextChanged { viewModel.onNarrationChanged(it?.toString().orEmpty()) }

        binding.debitAccountInput.setOnItemClickListener { _, _, position, _ ->
            viewModel.uiState.value.accounts.getOrNull(position)?.let { viewModel.onAccountSelected(it.id) }
        }
        binding.bankInput.setOnItemClickListener { _, _, position, _ ->
            viewModel.uiState.value.banks.getOrNull(position)?.let { viewModel.onBankSelected(it.code) }
        }
        binding.retryResolutionButton.setOnClickListener { viewModel.onRetryResolution() }
        binding.continueButton.setOnClickListener { viewModel.onContinue() }
        binding.checkStatusButton.setOnClickListener {
            viewModel.checkStatus()
            navigateSafely(R.id.transferFormFragment, R.id.action_transferForm_to_result)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun handleEvent(event: TransferEvent) {
        when (event) {
            TransferEvent.NavigateToConfirmation ->
                navigateSafely(R.id.transferFormFragment, R.id.action_transferForm_to_confirmation)
            TransferEvent.PreviousTransferUnresolved ->
                Snackbar.make(binding.root, R.string.snackbar_unresolved_transfer, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun render(state: TransferUiState) = with(binding) {
        val context = requireContext()

        // Debit account
        val accountLabels = state.accounts.map { it.displayName(context) }
        if (accountLabels != renderedAccountLabels) {
            renderedAccountLabels = accountLabels
            debitAccountInput.setAdapter(DropdownAdapter(context, accountLabels))
        }
        val selectedAccount = state.selectedAccount
        val accountText = selectedAccount?.displayName(context).orEmpty()
        if (debitAccountInput.text.toString() != accountText) debitAccountInput.setText(accountText, false)
        availableBalance.text = selectedAccount
            ?.let { getString(R.string.available_balance, MoneyFormatter.format(it.balanceKobo)) }
            ?: getString(R.string.available_balance_none)
        debitAccountLayout.error = state.validation.account?.toMessage(context)

        // Destination bank
        val bankNames = state.banks.map { it.name }
        if (bankNames != renderedBankNames) {
            renderedBankNames = bankNames
            bankInput.setAdapter(DropdownAdapter(context, bankNames))
        }
        val bankText = state.selectedBank?.name.orEmpty()
        if (bankInput.text.toString() != bankText) bankInput.setText(bankText, false)
        bankLayout.error = state.validation.bank?.toMessage(context)

        // Account number, amount
        accountNumberLayout.error = state.validation.accountNumber?.toMessage(context)
        amountLayout.error = state.validation.amount?.toMessage(context)

        renderBeneficiary(state.resolution, state.validation.beneficiary != null && state.showAllErrors)

        // Fee / total preview
        val amounts = state.validation.amounts
        summaryCard.isVisible = amounts != null
        if (amounts != null) {
            summaryAmount.setValue(MoneyFormatter.format(amounts.amountKobo))
            summaryFee.setValue(MoneyFormatter.format(amounts.feeKobo))
            summaryTotal.setValue(MoneyFormatter.format(amounts.totalDebitKobo))
        }

        // Unconfirmed previous transfer
        val unresolved = state.unresolved
        unresolvedBanner.isVisible = unresolved != null
        if (unresolved != null) {
            bannerMessage.text = getString(R.string.banner_message, unresolved.draft.reference)
        }

        continueButton.isEnabled = !state.isBusy
        checkStatusButton.isEnabled = !state.isBusy
    }

    private fun renderBeneficiary(resolution: ResolutionState, flagMissing: Boolean) = with(binding) {
        beneficiaryCard.isVisible = resolution !is ResolutionState.Idle
        beneficiaryProgress.isVisible = resolution is ResolutionState.Loading
        beneficiaryCheck.isVisible = resolution is ResolutionState.Resolved
        retryResolutionButton.isVisible =
            resolution is ResolutionState.Failed && resolution.reason == ResolutionFailure.SERVICE_ERROR
        beneficiaryLabel.isVisible = resolution is ResolutionState.Resolved

        val errorColor = com.google.android.material.color.MaterialColors
            .getColor(beneficiaryText, com.google.android.material.R.attr.colorError)
        val normalColor = com.google.android.material.color.MaterialColors
            .getColor(beneficiaryText, com.google.android.material.R.attr.colorOnSurface)

        when (resolution) {
            ResolutionState.Idle -> beneficiaryText.text = ""
            is ResolutionState.Loading -> {
                beneficiaryText.text = getString(R.string.beneficiary_verifying)
                beneficiaryText.setTextColor(normalColor)
            }
            is ResolutionState.Resolved -> {
                beneficiaryText.text = resolution.accountName
                beneficiaryText.setTextColor(normalColor)
            }
            is ResolutionState.Failed -> {
                beneficiaryText.text = getString(
                    if (resolution.reason == ResolutionFailure.ACCOUNT_NOT_FOUND) R.string.beneficiary_not_found
                    else R.string.beneficiary_service_error,
                )
                beneficiaryText.setTextColor(errorColor)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

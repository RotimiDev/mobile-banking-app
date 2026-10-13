package com.akeem.mobilebanking.ui.transfer

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.akeem.mobilebanking.R
import com.akeem.mobilebanking.databinding.FragmentTransferResultBinding
import com.akeem.mobilebanking.domain.model.FailureReason
import com.akeem.mobilebanking.domain.model.TransferDraft
import com.akeem.mobilebanking.domain.model.TransferOutcome
import com.akeem.mobilebanking.domain.money.MoneyFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransferResultFragment : Fragment(R.layout.fragment_transfer_result) {

    private val viewModel: TransferViewModel by hiltNavGraphViewModels(R.id.nav_graph)

    private var _binding: FragmentTransferResultBinding? = null
    private val binding get() = _binding!!

    private var leaving = false

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = leaveToForm()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTransferResultBinding.bind(view)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: TransferUiState) {
        if (leaving) return
        val draft = state.draft
        when (val process = state.process) {
            TransferProcessState.CheckingStatus, TransferProcessState.Submitting -> showChecking()
            is TransferProcessState.Finished -> if (draft != null) showOutcome(process.outcome, draft) else leaveToForm()
            TransferProcessState.Idle -> leaveToForm()
        }
    }

    private fun showChecking() = with(binding) {
        checkingProgress.isVisible = true
        statusIcon.isVisible = false
        statusTitle.setText(R.string.result_checking)
        statusMessage.text = ""
        detailsCard.isVisible = false
        primaryButton.isVisible = false
        secondaryButton.isVisible = false
        backCallback.isEnabled = true
    }

    private fun showOutcome(outcome: TransferOutcome, draft: TransferDraft) = with(binding) {
        checkingProgress.isVisible = false
        statusIcon.isVisible = true
        detailsCard.isVisible = true
        primaryButton.isVisible = true
        rowReference.setValue(draft.reference)
        rowBeneficiary.setValue(draft.beneficiaryName)
        rowAmount.setValue(MoneyFormatter.format(draft.amounts.amountKobo))
        rowTotal.setValue(MoneyFormatter.format(draft.amounts.totalDebitKobo))

        when (outcome) {
            is TransferOutcome.Successful -> {
                bindStatus(R.drawable.ic_check_circle, R.color.success, R.string.result_success_title)
                statusMessage.text = getString(
                    R.string.result_success_message,
                    MoneyFormatter.format(draft.amounts.amountKobo),
                    draft.beneficiaryName,
                )
                primaryButton.setText(R.string.action_done)
                primaryButton.setOnClickListener { leaveToForm() }
                secondaryButton.isVisible = true
                secondaryButton.setText(R.string.action_view_history)
                secondaryButton.setOnClickListener { leaveToHistory() }
            }
            is TransferOutcome.Failed -> {
                bindStatus(R.drawable.ic_error, R.color.failed, R.string.result_failed_title)
                statusMessage.setText(
                    when (outcome.reason) {
                        FailureReason.INSUFFICIENT_FUNDS -> R.string.result_failed_insufficient
                        FailureReason.REJECTED_BY_BANK -> R.string.result_failed_rejected
                        FailureReason.NOT_PROCESSED -> R.string.result_failed_not_processed
                    },
                )
                primaryButton.setText(R.string.action_try_again)
                primaryButton.setOnClickListener { leaveToForm() }
                secondaryButton.isVisible = false
            }
            TransferOutcome.Pending -> showUnsettled(
                R.drawable.ic_schedule, R.string.result_pending_title, R.string.result_pending_message,
            )
            TransferOutcome.Unknown -> showUnsettled(
                R.drawable.ic_warning, R.string.result_unknown_title, R.string.result_unknown_message,
            )
        }
    }

    /** Pending / unknown: the only way forward is to check the ORIGINAL transfer; never resubmit. */
    private fun showUnsettled(@DrawableRes icon: Int, @StringRes title: Int, @StringRes message: Int) = with(binding) {
        bindStatus(icon, R.color.pending, title)
        statusMessage.setText(message)
        primaryButton.setText(R.string.action_check_status)
        primaryButton.setOnClickListener { viewModel.checkStatus() }
        secondaryButton.isVisible = true
        secondaryButton.setText(R.string.action_done)
        secondaryButton.setOnClickListener { leaveToForm() }
    }

    private fun bindStatus(@DrawableRes icon: Int, @ColorRes color: Int, @StringRes title: Int) = with(binding) {
        statusIcon.setImageResource(icon)
        ImageViewCompat.setImageTintList(
            statusIcon,
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), color)),
        )
        statusTitle.setText(title)
    }

    private fun leaveToForm() {
        if (viewModel.uiState.value.isBusy) return // can't leave while a status check is running
        leaving = true
        viewModel.onResultDismissed()
        findNavController().popBackStack(R.id.transferFormFragment, false)
    }

    private fun leaveToHistory() {
        if (viewModel.uiState.value.isBusy) return
        leaving = true
        viewModel.onResultDismissed()
        findNavController().navigate(
            R.id.historyFragment,
            null,
            navOptions {
                popUpTo(R.id.transferFormFragment) { saveState = true }
                launchSingleTop = true
                restoreState = true
            },
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

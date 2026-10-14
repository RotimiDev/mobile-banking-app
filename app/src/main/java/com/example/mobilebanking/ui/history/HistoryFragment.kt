package com.example.mobilebanking.ui.history

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mobilebanking.R
import com.example.mobilebanking.databinding.FragmentHistoryBinding
import com.example.mobilebanking.domain.transactions.TransactionFilter
import com.example.mobilebanking.ui.common.navigateSafely
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryFragment : Fragment(R.layout.fragment_history) {

    private val viewModel: HistoryViewModel by viewModels()

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val adapter = TransactionAdapter { transaction ->
        navigateSafely(
            from = R.id.historyFragment,
            action = R.id.action_history_to_details,
            args = bundleOf("transactionId" to transaction.id),
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHistoryBinding.bind(view)

        binding.transactionsList.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.retryButton.setOnClickListener { viewModel.load() }

        val initial = viewModel.uiState.value
        binding.searchInput.setText(initial.query)
        binding.filterGroup.check(initial.filter.chipId())
        binding.searchInput.doAfterTextChanged {
            viewModel.onQueryChanged(
                it?.toString().orEmpty()
            )
        }
        binding.filterGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            checkedIds.firstOrNull()?.let { viewModel.onFilterSelected(it.toFilter()) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.messages.collect {
                        Snackbar.make(
                            binding.root,
                            R.string.snackbar_refresh_failed,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun render(state: HistoryUiState) = with(binding) {
        swipeRefresh.isRefreshing = state.isRefreshing
        val content = state.content

        loadingIndicator.isVisible = content is HistoryContent.Loading
        swipeRefresh.isVisible =
            content is HistoryContent.Items || content is HistoryContent.NoResults || content is HistoryContent.Empty
        retryButton.isVisible = content is HistoryContent.Error
        stateContainer.isVisible = content is HistoryContent.Error ||
                content is HistoryContent.Empty || content is HistoryContent.NoResults

        when (content) {
            is HistoryContent.Items -> adapter.submitList(content.transactions)
            HistoryContent.Loading -> Unit
            HistoryContent.Error -> showState(
                R.string.state_error_title,
                R.string.state_error_message
            )

            HistoryContent.Empty -> {
                adapter.submitList(emptyList())
                showState(R.string.state_empty_title, R.string.state_empty_message)
            }

            HistoryContent.NoResults -> {
                adapter.submitList(emptyList())
                showState(R.string.state_no_results_title, R.string.state_no_results_message)
            }
        }
    }

    private fun showState(title: Int, message: Int) {
        binding.stateTitle.setText(title)
        binding.stateMessage.setText(message)
    }

    private fun TransactionFilter.chipId() = when (this) {
        TransactionFilter.ALL -> R.id.chipAll
        TransactionFilter.CREDIT -> R.id.chipCredit
        TransactionFilter.DEBIT -> R.id.chipDebit
    }

    private fun Int.toFilter() = when (this) {
        R.id.chipCredit -> TransactionFilter.CREDIT
        R.id.chipDebit -> TransactionFilter.DEBIT
        else -> TransactionFilter.ALL
    }

    override fun onDestroyView() {
        binding.transactionsList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}

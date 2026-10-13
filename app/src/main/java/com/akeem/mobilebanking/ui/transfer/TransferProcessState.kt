package com.akeem.mobilebanking.ui.transfer

import com.akeem.mobilebanking.domain.model.TransferOutcome

sealed interface TransferProcessState {
    data object Idle : TransferProcessState
    data object Submitting : TransferProcessState
    data object CheckingStatus : TransferProcessState
    data class Finished(val outcome: TransferOutcome) : TransferProcessState
}

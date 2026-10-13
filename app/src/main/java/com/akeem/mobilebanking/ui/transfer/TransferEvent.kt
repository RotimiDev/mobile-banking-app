package com.akeem.mobilebanking.ui.transfer

sealed interface TransferEvent {
    data object NavigateToConfirmation : TransferEvent
    data object PreviousTransferUnresolved : TransferEvent
}

package com.akeem.mobilebanking.domain.transfer

import com.akeem.mobilebanking.domain.model.Account
import com.akeem.mobilebanking.domain.model.Bank
import com.akeem.mobilebanking.domain.model.ResolutionState

data class TransferFormInput(
    val account: Account?,
    val bank: Bank?,
    val accountNumber: String,
    val amountInput: String,
    val resolution: ResolutionState,
)

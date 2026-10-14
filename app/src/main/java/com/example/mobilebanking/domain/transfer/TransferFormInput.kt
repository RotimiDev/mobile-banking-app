package com.example.mobilebanking.domain.transfer

import com.example.mobilebanking.domain.model.Account
import com.example.mobilebanking.domain.model.Bank
import com.example.mobilebanking.domain.model.ResolutionState

data class TransferFormInput(
    val account: Account?,
    val bank: Bank?,
    val accountNumber: String,
    val amountInput: String,
    val resolution: ResolutionState,
)

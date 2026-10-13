package com.akeem.mobilebanking.ui.transfer

import com.akeem.mobilebanking.domain.model.TransferDraft
import com.akeem.mobilebanking.domain.model.TransferOutcome

/** A transfer whose outcome is Pending/Unknown. It blocks new transfers until its status is settled. */
data class UnresolvedTransfer(val draft: TransferDraft, val outcome: TransferOutcome)

package com.akeem.mobilebanking.data.demo

import javax.inject.Inject
import javax.inject.Singleton

/** Demo-only switchboard so every required state can be demonstrated without a real backend. */
@Singleton
class DemoSettings @Inject constructor() {
    @Volatile var transfer: DemoTransferResult = DemoTransferResult.SUCCESS
    @Volatile var history: DemoHistoryResult = DemoHistoryResult.NORMAL
}

package com.akeem.mobilebanking.data

import javax.inject.Inject
import javax.inject.Singleton

/** Which outcome the simulated transfer backend should produce next (chosen from the toolbar menu). */
enum class TransferSimulation { SUCCESS, FAILED, PENDING, TIMEOUT }

/** What the simulated history endpoint returns on the next load/refresh. */
enum class HistorySimulation { NORMAL, EMPTY, ERROR }

/** Demo-only switchboard so every required state can be demonstrated without a real backend. */
@Singleton
class SimulationSettings @Inject constructor() {
    @Volatile var transfer: TransferSimulation = TransferSimulation.SUCCESS
    @Volatile var history: HistorySimulation = HistorySimulation.NORMAL
}

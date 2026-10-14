package com.example.mobilebanking.data.simulation

import javax.inject.Inject
import javax.inject.Singleton

enum class TransferSimulation { SUCCESS, FAILED, PENDING, TIMEOUT }
enum class HistorySimulation { NORMAL, EMPTY, ERROR }

@Singleton
class SimulationSettings @Inject constructor() {
    @Volatile
    var transfer: TransferSimulation = TransferSimulation.SUCCESS
    @Volatile
    var history: HistorySimulation = HistorySimulation.NORMAL
}

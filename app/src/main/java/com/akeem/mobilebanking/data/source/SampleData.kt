package com.akeem.mobilebanking.data.source

import com.akeem.mobilebanking.domain.model.Account
import com.akeem.mobilebanking.domain.model.Bank
import com.akeem.mobilebanking.domain.model.Transaction
import com.akeem.mobilebanking.domain.model.TransactionStatus.FAILED
import com.akeem.mobilebanking.domain.model.TransactionStatus.PENDING
import com.akeem.mobilebanking.domain.model.TransactionStatus.SUCCESSFUL
import com.akeem.mobilebanking.domain.model.TransactionType.CREDIT
import com.akeem.mobilebanking.domain.model.TransactionType.DEBIT

internal object SampleData {

    val accounts = listOf(
        Account("acc_savings", "Savings Account", "4821", balanceKobo = 15_000_000L),      // ₦150,000.00
        Account("acc_current", "Current Account", "7730", balanceKobo = 235_050_075L),     // ₦2,350,500.75
        Account("acc_salary", "Salary Account", "1096", balanceKobo = 842_050L),           // ₦8,420.50
    )

    val banks = listOf(
        Bank("044", "Access Bank"),
        Bank("214", "First City Monument Bank (FCMB)"),
        Bank("070", "Fidelity Bank"),
        Bank("011", "First Bank of Nigeria"),
        Bank("058", "Guaranty Trust Bank"),
        Bank("076", "Polaris Bank"),
        Bank("221", "Stanbic IBTC Bank"),
        Bank("232", "Sterling Bank"),
        Bank("033", "United Bank for Africa"),
        Bank("032", "Union Bank of Nigeria"),
        Bank("035", "Wema Bank"),
        Bank("057", "Zenith Bank"),
    )

    val beneficiaryNames = listOf(
        "JOHN CHUKWUEMEKA DOE",
        "ADEBAYO TUNDE OLUWASEUN",
        "AMINA YUSUF BELLO",
        "CHINEDU OKAFOR",
        "FATIMA ABDULLAHI",
        "EMEKA NWANKWO",
        "BLESSING EKPO",
        "OLUWAFEMI ADEYEMI",
        "NGOZI EZE",
        "IBRAHIM MUSA SANI",
    )

    val transactions = listOf(
        Transaction("txn_seed_01", "TRF20261012091402", "Transfer to John Doe", 2_500_000L, "12 Oct 2026, 09:14 AM", DEBIT, SUCCESSFUL),
        Transaction("txn_seed_02", "SAL20261009080000", "Salary Payment", 45_000_000L, "09 Oct 2026, 08:00 AM", CREDIT, SUCCESSFUL),
        Transaction("txn_seed_03", "TRF20261008174511", "Transfer to Jane Smith", 1_000_000L, "08 Oct 2026, 05:45 PM", DEBIT, PENDING),
        Transaction("txn_seed_04", "BIL20261007120930", "Electricity Bill (IKEDC)", 1_500_000L, "07 Oct 2026, 12:09 PM", DEBIT, FAILED),
        Transaction("txn_seed_05", "TRF20261006101015", "Transfer from Chinedu Okafor", 7_500_000L, "06 Oct 2026, 10:10 AM", CREDIT, SUCCESSFUL),
        Transaction("txn_seed_06", "AIR20261005193322", "MTN Airtime Purchase", 200_000L, "05 Oct 2026, 07:33 PM", DEBIT, SUCCESSFUL),
        Transaction("txn_seed_07", "POS20261004145501", "Shoprite POS Purchase", 3_825_050L, "04 Oct 2026, 02:55 PM", DEBIT, SUCCESSFUL),
        Transaction("txn_seed_08", "TV20261003090012", "DStv Subscription", 2_450_000L, "03 Oct 2026, 09:00 AM", DEBIT, SUCCESSFUL),
        Transaction("txn_seed_09", "TRF20261002161247", "Transfer from Amina Bello", 12_000_000L, "02 Oct 2026, 04:12 PM", CREDIT, SUCCESSFUL),
        Transaction("txn_seed_10", "TRF20261001113005", "Transfer to Emeka Nwankwo", 5_000_000L, "01 Oct 2026, 11:30 AM", DEBIT, SUCCESSFUL),
    )
}

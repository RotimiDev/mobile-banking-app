package com.akeem.mobilebanking.testutil

import com.akeem.mobilebanking.domain.model.Account
import com.akeem.mobilebanking.domain.model.Bank

val SAVINGS = Account("acc_savings", "Savings Account", "4821", balanceKobo = 15_000_000L) // ₦150,000.00

val SALARY = Account("acc_salary", "Salary Account", "1096", balanceKobo = 842_050L)       // ₦8,420.50

val ACCESS = Bank("044", "Access Bank")

val GTB = Bank("058", "Guaranty Trust Bank")

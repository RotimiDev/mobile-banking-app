package com.akeem.mobilebanking.di

import com.akeem.mobilebanking.data.repository.AccountRepositoryImpl
import com.akeem.mobilebanking.data.repository.BankRepositoryImpl
import com.akeem.mobilebanking.data.repository.BeneficiaryRepositoryImpl
import com.akeem.mobilebanking.data.repository.ReferenceGeneratorImpl
import com.akeem.mobilebanking.data.repository.TransactionRepositoryImpl
import com.akeem.mobilebanking.data.repository.TransferRepositoryImpl
import com.akeem.mobilebanking.domain.repository.AccountRepository
import com.akeem.mobilebanking.domain.repository.BankRepository
import com.akeem.mobilebanking.domain.repository.BeneficiaryRepository
import com.akeem.mobilebanking.domain.repository.ReferenceGenerator
import com.akeem.mobilebanking.domain.repository.TransactionRepository
import com.akeem.mobilebanking.domain.repository.TransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Swap these bindings for Retrofit-backed implementations to go from simulated to real. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun accounts(impl: AccountRepositoryImpl): AccountRepository
    @Binds abstract fun banks(impl: BankRepositoryImpl): BankRepository
    @Binds abstract fun beneficiaries(impl: BeneficiaryRepositoryImpl): BeneficiaryRepository
    @Binds abstract fun transfers(impl: TransferRepositoryImpl): TransferRepository
    @Binds abstract fun transactions(impl: TransactionRepositoryImpl): TransactionRepository
    @Binds abstract fun references(impl: ReferenceGeneratorImpl): ReferenceGenerator
}

package com.akeem.mobilebanking.di

import com.akeem.mobilebanking.data.InMemoryAccountRepository
import com.akeem.mobilebanking.data.InMemoryBankRepository
import com.akeem.mobilebanking.data.InMemoryBeneficiaryRepository
import com.akeem.mobilebanking.data.InMemoryTransactionRepository
import com.akeem.mobilebanking.data.InMemoryTransferRepository
import com.akeem.mobilebanking.data.UuidReferenceGenerator
import com.akeem.mobilebanking.domain.repository.AccountRepository
import com.akeem.mobilebanking.domain.repository.BankRepository
import com.akeem.mobilebanking.domain.repository.BeneficiaryRepository
import com.akeem.mobilebanking.domain.repository.ReferenceGenerator
import com.akeem.mobilebanking.domain.repository.TransactionRepository
import com.akeem.mobilebanking.domain.repository.TransferRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/** Swap these bindings for Retrofit-backed implementations to go from simulated to real. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun accounts(impl: InMemoryAccountRepository): AccountRepository
    @Binds abstract fun banks(impl: InMemoryBankRepository): BankRepository
    @Binds abstract fun beneficiaries(impl: InMemoryBeneficiaryRepository): BeneficiaryRepository
    @Binds abstract fun transfers(impl: InMemoryTransferRepository): TransferRepository
    @Binds abstract fun transactions(impl: InMemoryTransactionRepository): TransactionRepository
    @Binds abstract fun references(impl: UuidReferenceGenerator): ReferenceGenerator
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

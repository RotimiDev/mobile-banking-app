package com.example.mobilebanking.di

import com.example.mobilebanking.data.repository.AccountRepositoryImpl
import com.example.mobilebanking.data.repository.BankRepositoryImpl
import com.example.mobilebanking.data.repository.BeneficiaryRepositoryImpl
import com.example.mobilebanking.data.repository.ReferenceGeneratorImpl
import com.example.mobilebanking.data.repository.TransactionRepositoryImpl
import com.example.mobilebanking.data.repository.TransferRepositoryImpl
import com.example.mobilebanking.domain.repository.AccountRepository
import com.example.mobilebanking.domain.repository.BankRepository
import com.example.mobilebanking.domain.repository.BeneficiaryRepository
import com.example.mobilebanking.domain.repository.ReferenceGenerator
import com.example.mobilebanking.domain.repository.TransactionRepository
import com.example.mobilebanking.domain.repository.TransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun accounts(impl: AccountRepositoryImpl): AccountRepository
    @Binds
    abstract fun banks(impl: BankRepositoryImpl): BankRepository
    @Binds
    abstract fun beneficiaries(impl: BeneficiaryRepositoryImpl): BeneficiaryRepository
    @Binds
    abstract fun transfers(impl: TransferRepositoryImpl): TransferRepository
    @Binds
    abstract fun transactions(impl: TransactionRepositoryImpl): TransactionRepository
    @Binds
    abstract fun references(impl: ReferenceGeneratorImpl): ReferenceGenerator
}

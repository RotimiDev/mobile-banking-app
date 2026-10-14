package com.example.mobilebanking.domain.repository

fun interface ReferenceGenerator {
    fun next(): String
}

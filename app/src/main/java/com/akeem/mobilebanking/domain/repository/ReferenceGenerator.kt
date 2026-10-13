package com.akeem.mobilebanking.domain.repository

fun interface ReferenceGenerator {
    fun next(): String
}

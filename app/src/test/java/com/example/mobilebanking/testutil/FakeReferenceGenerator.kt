package com.example.mobilebanking.testutil

import com.example.mobilebanking.domain.repository.ReferenceGenerator

class FakeReferenceGenerator : ReferenceGenerator {
    private var n = 0
    override fun next() = "REF${++n}"
}

package com.akeem.mobilebanking.testutil

import com.akeem.mobilebanking.domain.repository.ReferenceGenerator

class FakeReferenceGenerator : ReferenceGenerator {
    private var n = 0
    override fun next() = "REF${++n}"
}

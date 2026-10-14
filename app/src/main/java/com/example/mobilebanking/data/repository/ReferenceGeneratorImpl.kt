package com.example.mobilebanking.data.repository

import com.example.mobilebanking.domain.repository.ReferenceGenerator
import java.util.UUID
import javax.inject.Inject

class ReferenceGeneratorImpl @Inject constructor() : ReferenceGenerator {
    override fun next(): String =
        "TRF" + UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
}

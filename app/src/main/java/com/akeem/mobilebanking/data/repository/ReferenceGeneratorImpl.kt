package com.akeem.mobilebanking.data.repository

import com.akeem.mobilebanking.domain.repository.ReferenceGenerator
import java.util.UUID
import javax.inject.Inject

class ReferenceGeneratorImpl @Inject constructor() : ReferenceGenerator {
    override fun next(): String = "TRF" + UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
}

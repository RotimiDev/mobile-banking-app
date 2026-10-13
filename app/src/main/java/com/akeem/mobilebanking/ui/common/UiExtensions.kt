package com.akeem.mobilebanking.ui.common

import android.content.Context
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.akeem.mobilebanking.R
import com.akeem.mobilebanking.domain.model.Account

fun Account.displayName(context: Context): String =
    context.getString(R.string.account_display_name, productName, last4)

/** Navigates only if [from] is still the current destination, so double taps can't navigate twice. */
fun Fragment.navigateSafely(@IdRes from: Int, @IdRes action: Int, args: Bundle? = null) {
    val controller = findNavController()
    if (controller.currentDestination?.id == from) controller.navigate(action, args)
}

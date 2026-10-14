package com.example.mobilebanking.ui.common

import android.content.Context
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mobilebanking.R
import com.example.mobilebanking.domain.model.Account

fun Account.displayName(context: Context): String =
    context.getString(R.string.account_display_name, productName, last4)

fun Fragment.navigateSafely(@IdRes from: Int, @IdRes action: Int, args: Bundle? = null) {
    val controller = findNavController()
    if (controller.currentDestination?.id == from) controller.navigate(action, args)
}

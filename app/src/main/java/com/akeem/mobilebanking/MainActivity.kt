package com.akeem.mobilebanking

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.akeem.mobilebanking.data.demo.DemoHistoryResult
import com.akeem.mobilebanking.data.demo.DemoSettings
import com.akeem.mobilebanking.data.demo.DemoTransferResult
import com.akeem.mobilebanking.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var demoSettings: DemoSettings

    private lateinit var binding: ActivityMainBinding

    private val topLevelDestinations = setOf(R.id.transferFormFragment, R.id.historyFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, max(bars.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        binding.toolbar.setupWithNavController(navController, AppBarConfiguration(topLevelDestinations))
        binding.bottomNav.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.isVisible = destination.id in topLevelDestinations
        }

        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sim_transfer -> showTransferSimulationDialog()
                R.id.action_sim_history -> showHistorySimulationDialog()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
    }

    private fun showTransferSimulationDialog() {
        val options = DemoTransferResult.entries
        val labels = arrayOf(
            getString(R.string.sim_transfer_success),
            getString(R.string.sim_transfer_failed),
            getString(R.string.sim_transfer_pending),
            getString(R.string.sim_transfer_timeout),
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sim_transfer_title)
            .setSingleChoiceItems(labels, options.indexOf(demoSettings.transfer)) { dialog, which ->
                demoSettings.transfer = options[which]
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_close, null)
            .show()
    }

    private fun showHistorySimulationDialog() {
        val options = DemoHistoryResult.entries
        val labels = arrayOf(
            getString(R.string.sim_history_normal),
            getString(R.string.sim_history_empty),
            getString(R.string.sim_history_error),
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sim_history_title)
            .setSingleChoiceItems(labels, options.indexOf(demoSettings.history)) { dialog, which ->
                demoSettings.history = options[which]
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_close, null)
            .show()
    }
}

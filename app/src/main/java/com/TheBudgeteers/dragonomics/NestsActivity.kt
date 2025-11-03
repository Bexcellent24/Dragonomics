package com.TheBudgeteers.dragonomics

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.TheBudgeteers.dragonomics.data.NestLayoutType
import com.TheBudgeteers.dragonomics.data.NestType
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.databinding.ActivityNestsBinding
import com.TheBudgeteers.dragonomics.ui.NestFragment
import com.TheBudgeteers.dragonomics.ui.NewNestDialogFragment
import com.TheBudgeteers.dragonomics.utils.openIntent
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import com.TheBudgeteers.dragonomics.viewmodel.factories.NestViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// NestsActivity is the nest management screen
// Allows users to view and edit their budget categories (nests)
// Shows two lists: income sources at the top and expense categories at the bottom
// Users can add new nests using the FAB button
// Accessed from the ExpensesActivity via the "Edit Nests" button

class NestsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {


    private lateinit var binding: ActivityNestsBinding
    private lateinit var sessionStore: SessionStore
    private var currentUserId: String? = null

    // Initialize ViewModel with factory
    private val nestVm: NestViewModel by lazy {
        ViewModelProvider(
            this,
            NestViewModelFactory(Repository())
        )[NestViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionStore = SessionStore(this)
        // Get current user ID
        lifecycleScope.launch {
            currentUserId = sessionStore.userId.first()
        }

        // Setup bottom navigation with expenses tab selected
        // (since this activity is accessed from the expenses screen)
        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.selectedItemId = R.id.nav_expenses

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            onNavigationItemSelected(item)
        }

        binding.btnResetNests.setOnClickListener {
            Log.d("NestActivity", "Reset Button Clicked")
            showResetConfirmationDialog() }

        // begin code attribution
        // Fragment transactions adapted from:
        // Android Developers: Fragment transactions

        // Setup expense nests fragment (bottom section)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentOutgoingNests, NestFragment.newInstance(NestType.EXPENSE, NestLayoutType.LIST))
            .commit()

        // Setup income nests fragment (top section)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentIncomingNests, NestFragment.newInstance(NestType.INCOME, NestLayoutType.LIST))
            .commit()

        // end code attribution (Android Developers, 2020)

        // FAB opens dialog to create a new nest
        binding.fabAddNest.setOnClickListener {
            val dlg = NewNestDialogFragment()
            dlg.show(supportFragmentManager, "new_nest")
        }
    }

    // Show confirmation dialog before resetting for new month
    private fun showResetConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Start New Budget Period?")
            .setMessage(
                "This will start a fresh budget period:\n\n" +
                        "• Expense budgets reset to R0 spent\n" +
                        "• Income nests reset to R0\n" +
                        "• Your transaction history remains intact\n\n" +
                        "You can still view all past transactions in History.\n\n" +
                        "Continue?"
            )
            .setPositiveButton("Start New Period") { _, _ ->
                performMonthReset()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Perform the actual month reset operation
    private fun performMonthReset() {
        val userId = currentUserId

        if (userId == null) {
            Toast.makeText(this, "Error: No user logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        Toast.makeText(this, "Resetting budgets...", Toast.LENGTH_SHORT).show()

        nestVm.resetForNewMonth(
            userId = userId,
            onSuccess = {
                Toast.makeText(
                    this,
                    "Successfully reset for new month!",
                    Toast.LENGTH_LONG
                ).show()

                // Optionally refresh the fragments
                recreate()
            },
            onError = { errorMessage ->
                Toast.makeText(
                    this,
                    "Error resetting: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }


    // Handle bottom navigation item clicks
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> openIntent(this, "", HomeActivity::class.java)
            R.id.nav_expenses -> openIntent(this, "", ExpensesActivity::class.java)
            R.id.nav_history -> openIntent(this, "", HistoryActivity::class.java)
            R.id.nav_profile -> openIntent(this, "", ProfileActivity::class.java)
        }
        return true
    }
}

// reference list
// Android Developers, 2020. Fragment Transactions. [online] Available at: <https://developer.android.com/guide/fragments/transactions> [Accessed 5 October 2025].
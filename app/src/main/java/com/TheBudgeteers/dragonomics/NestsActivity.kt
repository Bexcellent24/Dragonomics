package com.TheBudgeteers.dragonomics

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.TheBudgeteers.dragonomics.data.NestLayoutType
import com.TheBudgeteers.dragonomics.databinding.ActivityExpensesBinding
import com.TheBudgeteers.dragonomics.databinding.ActivityNestsBinding
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.ui.NestFragment
import com.TheBudgeteers.dragonomics.ui.NewNestDialogFragment
import com.google.android.material.navigation.NavigationView

// NestsActivity is the nest management screen
// Allows users to view and edit their budget categories (nests)
// Shows two lists: income sources at the top and expense categories at the bottom
// Users can add new nests using the FAB button
// Accessed from the ExpensesActivity via the "Edit Nests" button

class NestsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {


    private lateinit var binding: ActivityNestsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup bottom navigation with expenses tab selected
        // (since this activity is accessed from the expenses screen)
        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.selectedItemId = R.id.nav_expenses

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            onNavigationItemSelected(item)
        }

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
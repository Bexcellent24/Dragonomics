package com.TheBudgeteers.dragonomics

import android.os.Bundle
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.TheBudgeteers.dragonomics.data.NestLayoutType
import com.TheBudgeteers.dragonomics.data.RepositoryProvider
import com.TheBudgeteers.dragonomics.databinding.ActivityExpensesBinding
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.testing.TestDataSeeder
import com.TheBudgeteers.dragonomics.ui.NestFragment
import com.TheBudgeteers.dragonomics.ui.NewTransactionFragment
import com.TheBudgeteers.dragonomics.ui.StatsFragment
import com.TheBudgeteers.dragonomics.ui.TransactionFragment
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class ExpensesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityExpensesBinding

    private lateinit var btnNestIn: LinearLayout
    private lateinit var btnNestOut: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = RepositoryProvider.getRepository(this)

        lifecycleScope.launch {
            val seeder = TestDataSeeder(repository)
            seeder.seedDummyData()
        }

        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.selectedItemId = R.id.nav_expenses

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            onNavigationItemSelected(item)
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.month_summary, StatsFragment.newInstance(toggleEnabled = false))
            .commit()

        binding.btnEditNests.setOnClickListener {
            openIntent(this, "", NestsActivity::class.java)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerTransactions, TransactionFragment())
                .commit()

            loadNestFragment(NestType.EXPENSE) // Only one call
        }

        btnNestIn = binding.btnNestsIn
        btnNestOut = binding.btnNestsOut

        btnNestIn.setOnClickListener {
            updateNestToggleSelection(R.id.btnNestsIn)
            loadNestFragment(NestType.INCOME)
        }

        btnNestOut.setOnClickListener {
            updateNestToggleSelection(R.id.btnNestsOut)
            loadNestFragment(NestType.EXPENSE)
        }

        binding.fabAddTransaction.setOnClickListener {
            NewTransactionFragment().show(supportFragmentManager, "NewTransaction")
        }

        updateNestToggleSelection(R.id.btnNestsOut)
    }


    private fun loadNestFragment(nestType: NestType) {
        val fragment = NestFragment.newInstance(nestType, NestLayoutType.GRID)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerNests, fragment)
            .commit()
    }

    private fun updateNestToggleSelection(selectedId: Int) {
        btnNestIn.isSelected = selectedId == R.id.btnNestsIn
        btnNestOut.isSelected = selectedId == R.id.btnNestsOut
    }

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

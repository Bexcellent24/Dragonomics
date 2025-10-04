package com.TheBudgeteers.dragonomics

import android.os.Bundle
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.setFragmentResultListener
import com.TheBudgeteers.dragonomics.data.NestLayoutType
import com.TheBudgeteers.dragonomics.databinding.ActivityExpensesBinding
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.testing.TestDataSeeder
import com.TheBudgeteers.dragonomics.ui.NestFragment
import com.TheBudgeteers.dragonomics.ui.NewTransactionFragment
import com.TheBudgeteers.dragonomics.ui.StatsFragment
import com.TheBudgeteers.dragonomics.ui.TransactionFragment
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

import com.TheBudgeteers.dragonomics.gamify.DragonGameProvider
import com.TheBudgeteers.dragonomics.gamify.DragonGameEvents

import com.TheBudgeteers.dragonomics.gamify.DragonMoodManager
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import com.TheBudgeteers.dragonomics.gamify.DragonRules
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider


class ExpensesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityExpensesBinding

    private lateinit var btnNestIn: LinearLayout
    private lateinit var btnNestOut: LinearLayout

    private lateinit var nestVm: NestViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = RepositoryProvider.getRepository(this)
        nestVm = NestViewModel(repository)

        lifecycleScope.launch {
            //val seeder = TestDataSeeder(repository)
            //seeder.seedDummyData()
            // After seeding, refresh overall mood once (based on EXPENSE nests)
            updateOverallMoodFromVm(NestType.EXPENSE)
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

        // --- LISTEN FOR TRANSACTION SAVE RESULT (from NewTransactionFragment) ---
        // NewTransactionFragment should call:
        // parentFragmentManager.setFragmentResult("tx_saved", bundleOf("addedPhoto" to true/false))
        supportFragmentManager.setFragmentResultListener("tx_saved", this) { _, bundle ->
            val addedPhoto = bundle.getBoolean("addedPhoto", false)
            // Award XP (mood affects final XP internally)
            val game = DragonGameProvider.get(this)
            game.onExpenseLogged(addedPhoto)
            // Tell HomeActivity (and any observers) to refresh UI
            DragonGameEvents.notifyChanged(game.state)


            // Recompute & persist overall mood (based on EXPENSE nests)
            updateOverallMoodFromVm(NestType.EXPENSE)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerTransactions, TransactionFragment())
                .commit()

            loadNestFragment(NestType.EXPENSE)
        }

        btnNestIn = binding.btnNestsIn
        btnNestOut = binding.btnNestsOut

        btnNestIn.setOnClickListener {
            updateNestToggleSelection(R.id.btnNestsIn)
            loadNestFragment(NestType.INCOME)
            // If you want only EXPENSE nests to drive dragon mood, keep EXPENSE here:
            updateOverallMoodFromVm(NestType.EXPENSE)
        }

        btnNestOut.setOnClickListener {
            updateNestToggleSelection(R.id.btnNestsOut)
            loadNestFragment(NestType.EXPENSE)
            updateOverallMoodFromVm(NestType.EXPENSE)
        }

        binding.fabAddTransaction.setOnClickListener {
            NewTransactionFragment().show(supportFragmentManager, "NewTransaction")
        }

        updateNestToggleSelection(R.id.btnNestsOut)
    }

    override fun onResume() {
        super.onResume()
        // Keep Home/XP in sync if anything changed while away
        updateOverallMoodFromVm(NestType.EXPENSE)
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

    // Map app Nest mood -> Dragon mood
    private fun com.TheBudgeteers.dragonomics.models.Mood.toDragonMood(): DragonRules.Mood =
        when (this) {
            com.TheBudgeteers.dragonomics.models.Mood.POSITIVE -> DragonRules.Mood.HAPPY
            com.TheBudgeteers.dragonomics.models.Mood.NEUTRAL  -> DragonRules.Mood.NEUTRAL
            com.TheBudgeteers.dragonomics.models.Mood.NEGATIVE -> DragonRules.Mood.ANGRY
        }

    private fun updateOverallMoodFromVm(type: NestType) {
        lifecycleScope.launch {
            val (mood, _) = nestVm.getOverallMood(
                type = type,
                weighting = NestViewModel.Weighting.BUDGET
            )
            DragonMoodManager.setOverallMood(this@ExpensesActivity, mood)

            val game = DragonGameProvider.get(this@ExpensesActivity)
            game.setOverallMood(mood.toDragonMood())
        }
    }


}

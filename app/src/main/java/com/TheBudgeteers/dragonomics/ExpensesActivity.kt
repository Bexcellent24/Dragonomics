package com.TheBudgeteers.dragonomics

import android.os.Bundle
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.TheBudgeteers.dragonomics.data.NestLayoutType
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.databinding.ActivityExpensesBinding
import com.TheBudgeteers.dragonomics.gamify.DragonGameEvents
import com.TheBudgeteers.dragonomics.gamify.DragonGameProvider
import com.TheBudgeteers.dragonomics.gamify.DragonMoodManager
import com.TheBudgeteers.dragonomics.gamify.DragonRules
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.ui.NestFragment
import com.TheBudgeteers.dragonomics.ui.NewTransactionFragment
import com.TheBudgeteers.dragonomics.ui.StatsFragment
import com.TheBudgeteers.dragonomics.ui.TransactionFragment
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ExpensesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityExpensesBinding
    private lateinit var btnNestIn: LinearLayout
    private lateinit var btnNestOut: LinearLayout
    private lateinit var nestVm: NestViewModel
    private lateinit var sessionStore: SessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize dependencies
        val repository = RepositoryProvider.getRepository(this)
        nestVm = NestViewModel(repository)
        sessionStore = SessionStore(this)

        // Setup UI components
        setupBottomNavigation()
        setupNestButtons()
        setupFragments(savedInstanceState)
        setupTransactionListener()

        // Initial mood update
        lifecycleScope.launch {
            updateOverallMoodFromVm(NestType.EXPENSE)
        }
    }

    override fun onResume() {
        super.onResume()
        // Keep Home/XP in sync if anything changed while away
        updateOverallMoodFromVm(NestType.EXPENSE)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.apply {
            itemIconTintList = null
            selectedItemId = R.id.nav_expenses
            setOnItemSelectedListener { item ->
                onNavigationItemSelected(item)
            }
        }
    }

    private fun setupNestButtons() {
        btnNestIn = binding.btnNestsIn
        btnNestOut = binding.btnNestsOut

        binding.btnEditNests.setOnClickListener {
            openIntent(this, "", NestsActivity::class.java)
        }

        btnNestIn.setOnClickListener {
            updateNestToggleSelection(R.id.btnNestsIn)
            loadNestFragment(NestType.INCOME)
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

    private fun setupFragments(savedInstanceState: Bundle?) {
        // Setup month summary fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.month_summary, StatsFragment.newInstance(toggleEnabled = false))
            .commit()

        // Setup initial fragments on first launch
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerTransactions, TransactionFragment())
                .commit()

            loadNestFragment(NestType.EXPENSE)
        }
    }

    private fun setupTransactionListener() {
        // Listen for transaction save events from NewTransactionFragment
        supportFragmentManager.setFragmentResultListener("tx_saved", this) { _, bundle ->
            val addedPhoto = bundle.getBoolean("addedPhoto", false)

            // Award XP for logging expense
            val game = DragonGameProvider.get(this)
            game.onExpenseLogged(addedPhoto)

            // Notify listeners of game state change
            DragonGameEvents.notifyChanged(game.state)

            // Update mood based on new transaction
            updateOverallMoodFromVm(NestType.EXPENSE)
        }
    }

    private fun loadNestFragment(nestType: NestType) {
        val fragment = NestFragment.newInstance(nestType, NestLayoutType.GRID)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerNests, fragment)
            .commit()
    }

    private fun updateNestToggleSelection(selectedId: Int) {
        btnNestIn.isSelected = (selectedId == R.id.btnNestsIn)
        btnNestOut.isSelected = (selectedId == R.id.btnNestsOut)
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

    private fun updateOverallMoodFromVm(type: NestType) {
        lifecycleScope.launch {
            // Get current user ID from session
            val userId = sessionStore.userId.firstOrNull()

            if (userId == null) {
                // No user logged in - could redirect to login or show error
                return@launch
            }

            // Calculate overall mood based on user's nests
            val (mood, _) = nestVm.getOverallMood(
                userId = userId,
                type = type,
                weighting = NestViewModel.Weighting.BUDGET
            )

            // Update dragon mood managers
            DragonMoodManager.setOverallMood(this@ExpensesActivity, mood)

            val game = DragonGameProvider.get(this@ExpensesActivity)
            game.setOverallMood(mood.toDragonMood())
        }
    }

    private fun com.TheBudgeteers.dragonomics.models.Mood.toDragonMood(): DragonRules.Mood =
        when (this) {
            com.TheBudgeteers.dragonomics.models.Mood.POSITIVE -> DragonRules.Mood.HAPPY
            com.TheBudgeteers.dragonomics.models.Mood.NEUTRAL -> DragonRules.Mood.NEUTRAL
            com.TheBudgeteers.dragonomics.models.Mood.NEGATIVE -> DragonRules.Mood.ANGRY
        }
}
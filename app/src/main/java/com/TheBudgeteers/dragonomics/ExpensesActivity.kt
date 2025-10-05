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

// ExpensesActivity is the main screen for managing income and expense categories (nests)
// Shows monthly stats at the top, transaction list in middle, and nest cards at bottom
// Users can toggle between income and expense views
// Integrates with the dragon gamification system to award XP and update mood
// Part of the bottom navigation flow (Home -> Expenses -> History -> Profile)

class ExpensesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {


    private lateinit var binding: ActivityExpensesBinding
    private lateinit var btnNestIn: LinearLayout        // Button to show income nests
    private lateinit var btnNestOut: LinearLayout       // Button to show expense nests


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

        // Calculate and display initial dragon mood based on expenses
        lifecycleScope.launch {
            updateOverallMoodFromVm(NestType.EXPENSE)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh mood when returning to this screen
        // Important if user added transactions elsewhere
        updateOverallMoodFromVm(NestType.EXPENSE)
    }


    // begin code attribution
    // Bottom navigation setup adapted from:
    // Android Developers: Material Design bottom navigation

    // Setup bottom navigation bar with proper selection state
    private fun setupBottomNavigation() {
        binding.bottomNavigationView.apply {
            itemIconTintList = null  // Use original icon colors
            selectedItemId = R.id.nav_expenses  // Mark this tab as selected
            setOnItemSelectedListener { item ->
                onNavigationItemSelected(item)
            }
        }
    }

    // end code attribution (Android Developers, 2021)

    // Setup nest toggle buttons and FAB for adding transactions
    private fun setupNestButtons() {
        btnNestIn = binding.btnNestsIn
        btnNestOut = binding.btnNestsOut

        // Edit nests button opens the nests management screen
        binding.btnEditNests.setOnClickListener {
            openIntent(this, "", NestsActivity::class.java)
        }

        // Toggle to show income nests
        btnNestIn.setOnClickListener {
            updateNestToggleSelection(R.id.btnNestsIn)
            loadNestFragment(NestType.INCOME)
            updateOverallMoodFromVm(NestType.EXPENSE)
        }

        // Toggle to show expense nests
        btnNestOut.setOnClickListener {
            updateNestToggleSelection(R.id.btnNestsOut)
            loadNestFragment(NestType.EXPENSE)
            updateOverallMoodFromVm(NestType.EXPENSE)
        }

        // FAB opens dialog to add new transaction
        binding.fabAddTransaction.setOnClickListener {
            NewTransactionFragment().show(supportFragmentManager, "NewTransaction")
        }

        // Default to expense view
        updateNestToggleSelection(R.id.btnNestsOut)
    }

    // Setup all fragments that make up this screen
    private fun setupFragments(savedInstanceState: Bundle?) {
        // Setup month summary fragment at the top (shows income/expense/balance)
        supportFragmentManager.beginTransaction()
            .replace(R.id.month_summary, StatsFragment.newInstance(toggleEnabled = false))
            .commit()

        // Setup initial fragments only on first launch (not on rotation)
        if (savedInstanceState == null) {
            // Transaction list in the middle
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerTransactions, TransactionFragment())
                .commit()

            // Nest cards at the bottom (start with expenses)
            loadNestFragment(NestType.EXPENSE)
        }
    }

    // begin code attribution
    // Fragment result listener pattern adapted from:
    // Android Developers: Fragment result API

    // Listen for when user saves a new transaction
    // Award XP and update dragon mood
    private fun setupTransactionListener() {
        supportFragmentManager.setFragmentResultListener("tx_saved", this) { _, bundle ->
            val addedPhoto = bundle.getBoolean("addedPhoto", false)

            // Award XP for logging an expense
            val game = DragonGameProvider.get(this)
            game.onExpenseLogged(addedPhoto)

            // Notify any listeners that game state changed
            DragonGameEvents.notifyChanged(game.state)

            // Recalculate dragon mood based on new spending
            updateOverallMoodFromVm(NestType.EXPENSE)
        }
    }

    // end code attribution (Android Developers, 2020)


    // Load the nest fragment for a specific type (income or expense)
    // Shows nest cards in grid layout
    private fun loadNestFragment(nestType: NestType) {
        val fragment = NestFragment.newInstance(nestType, NestLayoutType.GRID)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerNests, fragment)
            .commit()
    }

    // Update visual state of nest toggle buttons
    // Selected button gets highlighted styling
    private fun updateNestToggleSelection(selectedId: Int) {
        btnNestIn.isSelected = (selectedId == R.id.btnNestsIn)
        btnNestOut.isSelected = (selectedId == R.id.btnNestsOut)
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


    // Calculate overall budget health and update dragon's mood
    // This is called whenever spending changes or view is opened
    private fun updateOverallMoodFromVm(type: NestType) {
        lifecycleScope.launch {
            // Get current user ID from session
            val userId = sessionStore.userId.firstOrNull()

            if (userId == null) {
                // No user logged in - skip mood update
                return@launch
            }

            // Calculate overall mood based on user's budget progress
            // Uses budget weighting (bigger budgets matter more)
            val (mood, _) = nestVm.getOverallMood(
                userId = userId,
                type = type,
                weighting = NestViewModel.Weighting.BUDGET
            )

            // Update both mood managers so dragon appears correct everywhere
            DragonMoodManager.setOverallMood(this@ExpensesActivity, mood)

            val game = DragonGameProvider.get(this@ExpensesActivity)
            game.setOverallMood(mood.toDragonMood())
        }
    }

    // Convert nest mood to dragon mood format
    private fun com.TheBudgeteers.dragonomics.models.Mood.toDragonMood(): DragonRules.Mood =
        when (this) {
            com.TheBudgeteers.dragonomics.models.Mood.POSITIVE -> DragonRules.Mood.HAPPY
            com.TheBudgeteers.dragonomics.models.Mood.NEUTRAL -> DragonRules.Mood.NEUTRAL
            com.TheBudgeteers.dragonomics.models.Mood.NEGATIVE -> DragonRules.Mood.ANGRY
        }
}

// reference list
// Android Developers, 2021. Bottom Navigation. [online] Available at: <https://developer.android.com/develop/ui/views/components/bottom-navigation> [Accessed 1 October 2025].
// Android Developers, 2020. Fragment Result API. [online] Available at: <https://developer.android.com/guide/fragments/communicate> [Accessed 1 October 2025].
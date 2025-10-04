package com.TheBudgeteers.dragonomics

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.TheBudgeteers.dragonomics.databinding.ActivityHomeBinding
import com.TheBudgeteers.dragonomics.gamify.DragonMoodManager
import com.TheBudgeteers.dragonomics.ui.AchievementsDialogFragment
import com.TheBudgeteers.dragonomics.ui.ShopDialogFragment
import com.TheBudgeteers.dragonomics.viewmodel.AchievementsViewModel
import com.TheBudgeteers.dragonomics.viewmodel.DragonViewModel
import com.TheBudgeteers.dragonomics.viewmodel.ShopViewModel
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityHomeBinding

    // ViewModels
    private lateinit var dragonViewModel: DragonViewModel
    private lateinit var shopViewModel: ShopViewModel
    private lateinit var achievementsViewModel: AchievementsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeViewModels()
        initializeDragonDisplay()
        setupButtons()
        setupBottomNavigation()
        setupBackButton()
    }

    private fun initializeViewModels() {
        val dragonFactory = DragonViewModel.Factory(this)
        dragonViewModel = ViewModelProvider(this, dragonFactory)[DragonViewModel::class.java]

        shopViewModel = ViewModelProvider(this)[ShopViewModel::class.java]
        achievementsViewModel = ViewModelProvider(this)[AchievementsViewModel::class.java]
    }

    private fun initializeDragonDisplay() {
        lifecycleScope.launch {
            dragonViewModel.uiState.collect { state ->
                binding.dragon.setImageResource(state.dragonImageRes)
                binding.MoodImg.setImageResource(state.moodIconRes)
                binding.xpTxt.text = "XP  L${state.level}  ${state.xpIntoLevel}/${com.TheBudgeteers.dragonomics.gamify.DragonRules.XP_PER_LEVEL}"
                binding.xpProgress.setProgress(state.xpProgress, true)
            }
        }

        lifecycleScope.launch {
            shopViewModel.state.collect { state ->
                binding.currencyTxt.text = state.currency.toString()
            }
        }
    }

    private fun setupButtons() {
        binding.achievementsImg.setOnClickListener {
            AchievementsDialogFragment().show(supportFragmentManager, AchievementsDialogFragment.TAG)
        }

        binding.shopImg.setOnClickListener {
            ShopDialogFragment().show(supportFragmentManager, ShopDialogFragment.TAG)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.apply {
            setOnItemSelectedListener { item -> onNavigationItemSelected(item) }
            itemIconTintList = null
        }
    }

    private fun setupBackButton() {
        onBackPressedDispatcher.addCallback(this) {
            // Check if any dialog is showing
            val shopDialog = supportFragmentManager.findFragmentByTag(ShopDialogFragment.TAG)
            val achDialog = supportFragmentManager.findFragmentByTag(AchievementsDialogFragment.TAG)

            when {
                shopDialog?.isVisible == true -> (shopDialog as ShopDialogFragment).dismiss()
                achDialog?.isVisible == true -> (achDialog as AchievementsDialogFragment).dismiss()
                else -> finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        DragonMoodManager.getOverallMood(this)?.let { nestMood ->
            dragonViewModel.setOverallMood(nestMood.toDragonMood())
        }
    }

    private fun com.TheBudgeteers.dragonomics.models.Mood.toDragonMood(): com.TheBudgeteers.dragonomics.gamify.DragonRules.Mood {
        return when (this) {
            com.TheBudgeteers.dragonomics.models.Mood.POSITIVE -> com.TheBudgeteers.dragonomics.gamify.DragonRules.Mood.HAPPY
            com.TheBudgeteers.dragonomics.models.Mood.NEUTRAL -> com.TheBudgeteers.dragonomics.gamify.DragonRules.Mood.NEUTRAL
            com.TheBudgeteers.dragonomics.models.Mood.NEGATIVE -> com.TheBudgeteers.dragonomics.gamify.DragonRules.Mood.ANGRY
        }
    }

    // ==================== Public API for Dragon Events ====================

    fun onUserLoggedExpense(addedPhoto: Boolean) {
        dragonViewModel.onExpenseLogged(addedPhoto)
    }

    fun onBudgetRecalculated(
        under80: Boolean,
        between80And100: Boolean,
        over: Boolean,
        betweenMinAndMax: Boolean,
        aboveMax: Boolean
    ) {
        dragonViewModel.onBudgetEvaluated(
            under80Percent = under80,
            between80And100 = between80And100,
            overBudget = over,
            betweenMinAndMaxGoal = betweenMinAndMax,
            aboveMaxGoal = aboveMax
        )
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
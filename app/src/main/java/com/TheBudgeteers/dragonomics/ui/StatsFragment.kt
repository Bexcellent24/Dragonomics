package com.TheBudgeteers.dragonomics.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.TheBudgeteers.dragonomics.R
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.Fragment
import com.TheBudgeteers.dragonomics.data.MonthlyStats
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import com.TheBudgeteers.dragonomics.utils.DateUtils
import com.TheBudgeteers.dragonomics.viewmodel.StatsViewModel
import com.TheBudgeteers.dragonomics.viewmodel.StatsViewModelFactory
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.models.UserEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

// Displays monthly financial statistics for the user.
// Shows income, expenses, remaining balance, and progress toward savings goals.
// Can expand/collapse to show/hide goal information.
// Loads data for the current month from the database.

class StatsFragment : Fragment(R.layout.fragment_stats) {

    private lateinit var viewModel: StatsViewModel
    private lateinit var session: SessionStore
    private var toggleEnabled = true
    private var expanded = true

    // UI element references
    private lateinit var incomeAmount: TextView
    private lateinit var expensesAmount: TextView
    private lateinit var remainingAmount: TextView
    private lateinit var minGoal: TextView
    private lateinit var maxGoal: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var toggleArrow: ImageView
    private lateinit var goalsLayout: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupViewModel()
        loadUserData()
        observeData()
        setupToggle()
    }

    // Initialisation
    private fun initializeViews(view: View) {
        incomeAmount = view.findViewById(R.id.incomeAmount)
        expensesAmount = view.findViewById(R.id.expensesAmount)
        remainingAmount = view.findViewById(R.id.remainingAmount)
        minGoal = view.findViewById(R.id.minGoal)
        maxGoal = view.findViewById(R.id.maxGoal)
        progressBar = view.findViewById(R.id.progressBar)
        toggleArrow = view.findViewById(R.id.toggleArrow)
        goalsLayout = view.findViewById(R.id.goalsAndProgressLayout)
    }

    private fun setupViewModel() {
        session = SessionStore(requireContext())
        val repository = RepositoryProvider.getRepository(requireContext())
        val factory = StatsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[StatsViewModel::class.java]
    }


    // Data loading
    private fun loadUserData() {
        lifecycleScope.launch {
            val userId = session.userId.firstOrNull() ?: return@launch

            // Get start and end timestamps for current month
            val (start, end) = DateUtils.getMonthRange()
            viewModel.loadMonthlyStats(userId, start, end)
            viewModel.loadUser(userId)
        }
    }

    // begin code attribution
    // Data observing adapted from Android Developers guide to LifecycleScope

    // Data Observing
    private fun observeData() {
        observeMonthlyStats()
        observeUserGoals()
    }

    // Watch for changes in monthly statistics
    private fun observeMonthlyStats() {
        lifecycleScope.launch {
            viewModel.monthlyStats.collect { stats ->
                stats?.let { updateStatsDisplay(it) }
            }
        }
    }

    // Watch for changes in user's savings goals
    private fun observeUserGoals() {
        lifecycleScope.launch {
            viewModel.userEntity.collect { user ->
                user?.let { updateGoalsDisplay(it) }
            }
        }
    }
    // end code attribution (Android Developers, 2020)

    private fun updateStatsDisplay(stats: MonthlyStats) {
        incomeAmount.text = "R${stats.income.toInt()}"
        expensesAmount.text = "R${stats.expenses.toInt()}"
        remainingAmount.text = "R${stats.remaining.toInt()}"

        // Calculate and show percentage of income spent
        val percent = if (stats.income > 0) {
            (stats.expenses / stats.income * 100).toInt()
        } else {
            0
        }
        progressBar.progress = percent
    }

    private fun updateGoalsDisplay(user: UserEntity) {
        minGoal.text = if (user.minGoal != null) {
            "Min Goal: R${user.minGoal.toInt()}"
        } else {
            "Min Goal: Not Set"
        }

        maxGoal.text = if (user.maxGoal != null) {
            "Max Goal: R${user.maxGoal.toInt()}"
        } else {
            "Max Goal: Not Set"
        }
    }

    // Toggle functionality
    private fun setupToggle() {
        if (toggleEnabled) {
            toggleArrow.setOnClickListener {
                expanded = !expanded
                updateToggleState()
            }
        } else {
            toggleArrow.visibility = View.GONE
        }
    }

    private fun updateToggleState() {
        goalsLayout.visibility = if (expanded) View.VISIBLE else View.GONE
        toggleArrow.setImageResource(
            if (expanded) R.drawable.collapse_arrow_up else R.drawable.collapse_arrow_down
        )
    }

    // Factory
    companion object {
        fun newInstance(toggleEnabled: Boolean): StatsFragment {
            val f = StatsFragment()
            f.toggleEnabled = toggleEnabled
            return f
        }
    }
}

// Android Developers, 2020. LifecycleScope. [online] Available at: <https://developer.android.com/topic/libraries/architecture/coroutines> [Accessed 3 October 2025].
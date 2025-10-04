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
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import com.TheBudgeteers.dragonomics.utils.DateUtils
import com.TheBudgeteers.dragonomics.viewmodel.StatsViewModel
import com.TheBudgeteers.dragonomics.viewmodel.StatsViewModelFactory
import com.TheBudgeteers.dragonomics.data.SessionStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch


class StatsFragment : Fragment(R.layout.fragment_stats) {

    private lateinit var viewModel: StatsViewModel
    private lateinit var session: SessionStore
    private var toggleEnabled = true
    private var expanded = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SessionStore
        session = SessionStore(requireContext())

        val repository = RepositoryProvider.getRepository(requireContext())
        val factory = StatsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[StatsViewModel::class.java]

        val (start, end) = DateUtils.getMonthRange()
        viewModel.loadMonthlyStats(start, end)

        // ✅ FIX: Load the actual logged-in user ID
        lifecycleScope.launch {
            val userId = session.userId.firstOrNull()
            if (userId != null) {
                viewModel.loadUser(userId)
            }
        }

        val incomeAmount = view.findViewById<TextView>(R.id.incomeAmount)
        val expensesAmount = view.findViewById<TextView>(R.id.expensesAmount)
        val remainingAmount = view.findViewById<TextView>(R.id.remainingAmount)
        val minGoal = view.findViewById<TextView>(R.id.minGoal)
        val maxGoal = view.findViewById<TextView>(R.id.maxGoal)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val goalsRow = view.findViewById<LinearLayout>(R.id.goalsRow)
        val toggleArrow = view.findViewById<ImageView>(R.id.toggleArrow)

        // collect stats
        lifecycleScope.launchWhenStarted {
            viewModel.monthlyStats.collect { stats ->
                stats?.let {
                    incomeAmount.text = "R${it.income.toInt()}"
                    expensesAmount.text = "R${it.expenses.toInt()}"
                    remainingAmount.text = "R${it.remaining.toInt()}"

                    // example: progress bar percent
                    val percent = if (it.income > 0) (it.expenses / it.income * 100).toInt() else 0
                    progressBar.progress = percent
                }
            }
        }

        // ✅ FIX: Display goals with proper formatting
        lifecycleScope.launchWhenStarted {
            viewModel.userEntity.collect { user ->
                user?.let {
                    minGoal.text = if (it.minGoal != null) {
                        "Min Goal: R${it.minGoal.toInt()}"
                    } else {
                        "Min Goal: Not Set"
                    }

                    maxGoal.text = if (it.maxGoal != null) {
                        "Max Goal: R${it.maxGoal.toInt()}"
                    } else {
                        "Max Goal: Not Set"
                    }
                }
            }
        }

        // toggle expand/compact
        if (toggleEnabled) {
            toggleArrow.setOnClickListener {
                expanded = !expanded
                val goalsAndProgressLayout = view.findViewById<LinearLayout>(R.id.goalsAndProgressLayout)
                goalsAndProgressLayout.visibility = if (expanded) View.VISIBLE else View.GONE

                toggleArrow.setImageResource(
                    if (expanded) R.drawable.collapse_arrow_up else R.drawable.collapse_arrow_down
                )
            }
        } else {
            toggleArrow.visibility = View.GONE
        }
    }

    companion object {
        fun newInstance(toggleEnabled: Boolean): StatsFragment {
            val f = StatsFragment()
            f.toggleEnabled = toggleEnabled
            return f
        }
    }
}
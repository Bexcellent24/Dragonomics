package com.TheBudgeteers.dragonomics.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.TheBudgeteers.dragonomics.R
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.Fragment
import com.TheBudgeteers.dragonomics.data.RepositoryProvider
import com.TheBudgeteers.dragonomics.utils.DateUtils
import com.TheBudgeteers.dragonomics.viewmodel.StatsViewModel
import com.TheBudgeteers.dragonomics.viewmodel.StatsViewModelFactory


class StatsFragment : Fragment(R.layout.fragment_stats) {

    private lateinit var viewModel: StatsViewModel
    private var toggleEnabled = true
    private var expanded = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = RepositoryProvider.getRepository(requireContext())
        val factory = StatsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[StatsViewModel::class.java]

        val (start, end) = DateUtils.getMonthRange()
        viewModel.loadMonthlyStats(start, end)

        viewModel.loadUser(1L)

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

        // collect goals (if in UserEntity, expose via ViewModel too)
        lifecycleScope.launchWhenStarted {
            viewModel.userEntity.collect { user ->
                user?.let {
                    minGoal.text = "Min Goal: R${it.minGoal ?: 0}"
                    maxGoal.text = "Max Goal: R${it.maxGoal ?: 0}"
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

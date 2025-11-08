package com.TheBudgeteers.dragonomics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.data.NestType
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.ui.NestUiMapper
import com.TheBudgeteers.dragonomics.ui.widgets.GoalBarView
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import android.graphics.Typeface
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat

class StatsFragment : Fragment() {

    companion object {
        private const val ARG_TOGGLE = "toggleEnabled"
        fun newInstance(toggleEnabled: Boolean = false): StatsFragment =
            StatsFragment().apply {
                arguments = Bundle().apply { putBoolean(ARG_TOGGLE, toggleEnabled) }
            }
    }

    private var toggleEnabled: Boolean = false
    private lateinit var goalBar: GoalBarView
    private lateinit var sessionStore: SessionStore

    private lateinit var valueIncome: android.widget.TextView
    private lateinit var valueExpenses: android.widget.TextView
    private lateinit var valueRemaining: android.widget.TextView
    private lateinit var minGoalText: android.widget.TextView
    private lateinit var maxGoalText: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toggleEnabled = arguments?.getBoolean(ARG_TOGGLE) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        val root = inflater.inflate(R.layout.fragment_stats, container, false)
        goalBar = root.findViewById(R.id.goalBar)
        valueIncome = root.findViewById(R.id.valueIncome)
        valueExpenses = root.findViewById(R.id.valueExpenses)
        valueRemaining = root.findViewById(R.id.valueRemaining)
        minGoalText = root.findViewById(R.id.minGoalText)
        maxGoalText = root.findViewById(R.id.maxGoalText)
        sessionStore = SessionStore(requireContext())

        val aref = ResourcesCompat.getFont(requireContext(), R.font.aref_ruqaa)
        minGoalText.typeface = Typeface.create(aref, Typeface.BOLD)
        maxGoalText.typeface = Typeface.create(aref, Typeface.BOLD)
        minGoalText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        maxGoalText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)

        val title = root.findViewById<android.widget.TextView>(R.id.titleThisMonth)
        val monthName = java.time.format.DateTimeFormatter
            .ofPattern("MMMM", Locale("en", "ZA"))
            .format(LocalDate.now())
        title.text = monthName.uppercase(Locale("en", "ZA"))


        // Theme the bar (gold border, light track, gold labels/end cap)
        val gold = ContextCompat.getColor(requireContext(), R.color.GoldenEmber)
        val track = 0xFFE0E0E0.toInt()
        goalBar.setThemeColors(
            trackColor = track,
            borderColor = gold,
            labelColor = gold,
            endCapColor = gold
        )
        goalBar.setTrackMatchesParentBackground(true)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val repo = RepositoryProvider.getRepository(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            val userId = sessionStore.userId.firstOrNull() ?: return@launch

            // Goals
            val minGoal = repo.getUserMinGoal(userId) ?: 0.0
            val maxGoal = repo.getUserMaxGoal(userId) ?: 0.0

            // Current month range
            val today = LocalDate.now()
            val startOfMonth = today.withDayOfMonth(1)
            val startMs = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMs = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            // Totals (income/expenses/remaining)
            val tx = repo.getTransactionsBetweenFlow(userId, startMs, endMs).firstOrNull().orEmpty()
            val nestsById = repo.getNests(userId).associateBy { it.id }
            var incomeTotal = 0.0
            var expensesTotal = 0.0
            tx.forEach { t ->
                when (nestsById[t.categoryId]?.type) {
                    NestType.INCOME  -> incomeTotal += t.amount
                    NestType.EXPENSE -> expensesTotal += t.amount
                    else -> {}
                }
            }
            val remainingTotal = incomeTotal - expensesTotal

            // Expense nests (for stacked colors)
            val expenseNests = nestsById.values.filter { it.type == NestType.EXPENSE }

            // Amount per nest in range
            val amountsByNestId: Map<String, Double> =
                repo.getSpentAmountsInRangeOnce(userId, startMs, endMs)

            // Segments (oldest→newest) then reverse so NEWEST appears further LEFT in the bar
            val segments: List<GoalBarView.Segment> =
                expenseNests.map { nest ->
                    val amt = amountsByNestId[nest.id] ?: 0.0
                    val color = NestUiMapper.parseColorSafe(nest.colour)
                    GoalBarView.Segment(amt, color)
                }.filter { seg -> seg.amount > 0.0 }

            val segmentsNewestLeft = segments.asReversed()

            // Display numbers
            val nf = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
                maximumFractionDigits = 0
            }
            valueIncome.text = nf.format(incomeTotal)
            valueExpenses.text = nf.format(expensesTotal)
            valueRemaining.text = nf.format(remainingTotal)

            minGoalText.text = "Min Goal: " + nf.format(minGoal)
            maxGoalText.text = "Max Goal: " + nf.format(maxGoal)

            // Draw bar (expenses start from LEFT, newest further LEFT)
            goalBar.setData(
                maxGoal = maxGoal,
                minGoal = minGoal,
                expenseSegments = segmentsNewestLeft,
                totalIncome = incomeTotal
            )
        }
    }
}

package com.TheBudgeteers.dragonomics

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.TheBudgeteers.dragonomics.data.NestLayoutType
import com.TheBudgeteers.dragonomics.data.NestType
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.databinding.ActivityHistoryBinding
import com.TheBudgeteers.dragonomics.ui.NestFragment
import com.TheBudgeteers.dragonomics.ui.adapters.HistoryTransactionsAdapter
import com.TheBudgeteers.dragonomics.ui.widgets.PieChartView
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import com.TheBudgeteers.dragonomics.utils.openIntent
import com.TheBudgeteers.dragonomics.viewmodel.HistoryViewModel
import com.TheBudgeteers.dragonomics.viewmodel.factories.HistoryViewModelFactory
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.data.HistoryListItem

class HistoryActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var viewModel: HistoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.selectedItemId = R.id.nav_history
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            onNavigationItemSelected(item)
        }

        val repository = RepositoryProvider.getRepository(this)
        val session = SessionStore(this)

        lifecycleScope.launch {
            val userId = session.userId.firstOrNull()
            if (userId == null) {
                navigateToLogin()
                return@launch
            }

            viewModel = ViewModelProvider(
                this@HistoryActivity,
                HistoryViewModelFactory(repository, userId)
            )[HistoryViewModel::class.java]

            setupUI()
        }
    }

    private fun setupUI() {
        val prevMonthButton = binding.prevMonthButton
        val nextMonthButton = binding.nextMonthButton
        val currentMonthText = binding.currentMonthText

        val startDateText = binding.startDateButton.startDateText
        val endDateText = binding.endDateButton.endDateText

        val incomeText = binding.incomeText
        val expensesText = binding.expensesText

        val pieChart: PieChartView = binding.pieChart
        pieChart.holeRadiusPercent = 0f

        val dateFormat = SimpleDateFormat("d MMM yy", Locale.ENGLISH)

        prevMonthButton.setOnClickListener { viewModel.prevMonth() }
        nextMonthButton.setOnClickListener { viewModel.nextMonth() }

        lifecycleScope.launchWhenStarted {
            viewModel.startDate.collect { start ->
                startDateText.text = if (start != 0L) dateFormat.format(Date(start)) else "Start"
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.endDate.collect { end ->
                endDateText.text = if (end != 0L) dateFormat.format(Date(end)) else "End"
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.startDate.collect {
                currentMonthText.text = viewModel.getMonthDisplayName()
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.monthlyStats.collect { stats ->
                incomeText.text = "R${stats.income.toInt()}"
                expensesText.text = "R${stats.expenses.toInt()}"
            }
        }

        binding.startDateButton.root.setOnClickListener {
            showDatePicker { date -> viewModel.setCustomRange(date, viewModel.endDate.value) }
        }
        binding.endDateButton.root.setOnClickListener {
            showDatePicker { date -> viewModel.setCustomRange(viewModel.startDate.value, date) }
        }

        // begin code attribution
        // Fragment transaction pattern adapted from:
        // Android Developers, 2023. Fragments: create and add. [online]
        // Available at: <https://developer.android.com/guide/fragments> [Accessed 6 October 2025].
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.history_fragment_container,
                NestFragment.newInstance(NestType.EXPENSE, NestLayoutType.HISTORY)
            )
            .commit()
        // end code attribution (Android Developers, 2023)

        val adapter = HistoryTransactionsAdapter(emptyList()) { photoPath ->
            openPhotoViewer(photoPath)
        }
        binding.transactionsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.transactionsRecyclerView.adapter = adapter


        lifecycleScope.launchWhenStarted {
            viewModel.groupedTransactions.collect { grouped ->
                adapter.updateData(grouped)


                val items = grouped.filterIsInstance<HistoryListItem.TransactionItem>()


                val expenseItems = items.filter {
                    getNestFrom(it)?.type == NestType.EXPENSE
                }

                val byNest: Map<Nest, List<HistoryListItem.TransactionItem>> =
                    expenseItems.groupBy { getNestFrom(it)!! }

                val slices = byNest.map { (category, list) ->
                    val total = list.sumOf { row -> row.transactionWithNest.transaction.amount.toDouble() }
                    val color = parseColorOrFallback(category.colour)
                    PieChartView.Slice(category.name, total.toFloat(), color)
                }.sortedByDescending { it.value }


                pieChart.setData(slices)
            }
        }
    }

    private fun getNestFrom(item: HistoryListItem.TransactionItem): Nest? {
        return item.transactionWithNest.categoryNest
    }



    private fun parseColorOrFallback(hex: String?): Int {
        return try {
            if (hex.isNullOrBlank()) Color.parseColor("#F2994A")
            else Color.parseColor(hex)
        } catch (_: Throwable) {
            Color.parseColor("#F2994A")
        }
    }

    // begin code attribution
    // DatePickerDialog creation and callback usage adapted from:
    // Android Developers, 2023. Pickers. [online]
    // Available at: <https://developer.android.com/develop/ui/views/components/pickers> [Accessed 6 October 2025].
    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
    // end code attribution (Android Developers, 2023)

    private fun openPhotoViewer(photoPath: String) {
        try {
            val photoFile = File(photoPath)
            if (photoFile.exists()) {
                val photoUri: Uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    photoFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(photoUri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "View Photo"))
            } else {
                Toast.makeText(this, "Photo not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening photo: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
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
// reference list
// Android Developers, 2023. Fragments: create and add. [online]
// Available at: <https://developer.android.com/guide/fragments> [Accessed 6 October 2025].
// Android Developers, 2020. Create a list with RecyclerView. [online]
// Available at: <https://developer.android.com/develop/ui/views/layout/recyclerview#kotlin> [Accessed 6 October 2025].
// Android Developers, 2022. Kotlin coroutines on Android (lifecycle-aware collection). [online]
// Available at: <https://developer.android.com/kotlin/coroutines> [Accessed 6 October 2025].
// Oracle, 2024. SimpleDateFormat (Java SE). [online]
// Available at: <https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html> [Accessed 6 October 2025].
// Android Developers, 2023. Pickers (DatePickerDialog). [online]
// Available at: <https://developer.android.com/develop/ui/views/components/pickers> [Accessed 6 October 2025].
// Android Developers, 2023. Share files securely with FileProvider. [online]
// Available at: <https://developer.android.com/reference/androidx/core/content/FileProvider> [Accessed 6 October 2025].
// Android Developers, 2023. Common intents (ACTION_VIEW). [online]
// Available at: <https://developer.android.com/guide/components/intents-common> [Accessed 6 October 2025].

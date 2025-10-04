package com.TheBudgeteers.dragonomics

import android.app.DatePickerDialog
import android.content.Intent
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
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import java.text.SimpleDateFormat
import java.util.Locale
import com.TheBudgeteers.dragonomics.databinding.ActivityHistoryBinding
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.ui.HistoryTransactionsAdapter
import com.TheBudgeteers.dragonomics.ui.NestFragment
import com.TheBudgeteers.dragonomics.viewmodel.HistoryViewModel
import com.TheBudgeteers.dragonomics.viewmodel.HistoryViewModelFactory
import com.google.android.material.navigation.NavigationView
import java.io.File
import java.util.Calendar
import java.util.Date

class HistoryActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityHistoryBinding

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

        // ViewModel
        val repository = RepositoryProvider.getRepository(this)
        val viewModel = ViewModelProvider(this, HistoryViewModelFactory(repository))[HistoryViewModel::class.java]

        val prevMonthButton = binding.prevMonthButton
        val nextMonthButton = binding.nextMonthButton
        val currentMonthText = binding.currentMonthText

        val startDateText = binding.startDateButton.startDateText
        val endDateText = binding.endDateButton.endDateText

        val incomeText = binding.incomeText
        val expensesText = binding.expensesText

        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)

        prevMonthButton.setOnClickListener {
            viewModel.prevMonth()
        }

        nextMonthButton.setOnClickListener {
            viewModel.nextMonth()
        }

        lifecycleScope.launchWhenStarted {
            viewModel.startDate.collect { start ->
                startDateText.text = if (start != 0L) "Start: ${dateFormat.format(Date(start))}" else "Start Date"
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.endDate.collect { end ->
                endDateText.text = if (end != 0L) "End: ${dateFormat.format(Date(end))}" else "End Date"
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
            showDatePicker { date ->
                viewModel.setCustomRange(date, viewModel.endDate.value)
            }
        }

        binding.endDateButton.root.setOnClickListener {
            showDatePicker { date ->
                viewModel.setCustomRange(viewModel.startDate.value, date)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.history_fragment_container,
                NestFragment.newInstance(NestType.EXPENSE, NestLayoutType.HISTORY)
            )
            .commit()

        val adapter = HistoryTransactionsAdapter(emptyList()) { photoPath ->
            // Open photo viewer
            openPhotoViewer(photoPath)
        }
        binding.transactionsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.transactionsRecyclerView.adapter = adapter

        // Observe grouped transactions
        lifecycleScope.launchWhenStarted {
            viewModel.groupedTransactions.collect { grouped ->
                adapter.updateData(grouped)
            }
        }
    }

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
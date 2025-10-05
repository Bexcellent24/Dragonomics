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
import com.TheBudgeteers.dragonomics.data.SessionStore
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.Date

// HistoryActivity displays transaction history with filtering options
// Shows transactions grouped by date with monthly income/expense summary
// Users can navigate between months or set custom date ranges
// Displays attached receipt photos and allows viewing them in full screen
// Part of the bottom navigation flow

class HistoryActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {


    private lateinit var binding: ActivityHistoryBinding
    private lateinit var viewModel: HistoryViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup bottom navigation
        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.selectedItemId = R.id.nav_history

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            onNavigationItemSelected(item)
        }

        // Initialize repository and session
        val repository = RepositoryProvider.getRepository(this)
        val session = SessionStore(this)

        // Get user ID and setup ViewModel
        lifecycleScope.launch {
            val userId = session.userId.firstOrNull()
            if (userId == null) {
                // No user logged in - redirect to login
                navigateToLogin()
                return@launch
            }

            // Create ViewModel with user-specific factory
            viewModel = ViewModelProvider(
                this@HistoryActivity,
                HistoryViewModelFactory(repository, userId)
            )[HistoryViewModel::class.java]

            setupUI()
        }
    }


    // begin code attribution
    // Flow collection in lifecycle scope adapted from:
    // Android Developers: Collect flows safely

    // Setup all UI components and their data bindings
    private fun setupUI() {
        val prevMonthButton = binding.prevMonthButton
        val nextMonthButton = binding.nextMonthButton
        val currentMonthText = binding.currentMonthText

        val startDateText = binding.startDateButton.startDateText
        val endDateText = binding.endDateButton.endDateText

        val incomeText = binding.incomeText
        val expensesText = binding.expensesText

        val dateFormat = SimpleDateFormat("d MMM yy", Locale.ENGLISH)

        // Month navigation buttons
        prevMonthButton.setOnClickListener {
            viewModel.prevMonth()
        }

        nextMonthButton.setOnClickListener {
            viewModel.nextMonth()
        }

        // Display start date (updates automatically when date range changes)
        lifecycleScope.launchWhenStarted {
            viewModel.startDate.collect { start ->
                startDateText.text = if (start != 0L) dateFormat.format(Date(start)) else "Start"
            }
        }

        // Display end date (updates automatically when date range changes)
        lifecycleScope.launchWhenStarted {
            viewModel.endDate.collect { end ->
                endDateText.text = if (end != 0L) dateFormat.format(Date(end)) else "End"
            }
        }

        // Display current month name
        lifecycleScope.launchWhenStarted {
            viewModel.startDate.collect {
                currentMonthText.text = viewModel.getMonthDisplayName()
            }
        }

        // Display monthly income and expense totals
        lifecycleScope.launchWhenStarted {
            viewModel.monthlyStats.collect { stats ->
                incomeText.text = "R${stats.income.toInt()}"
                expensesText.text = "R${stats.expenses.toInt()}"
            }
        }

        // end code attribution (Android Developers, 2021)

        // Custom date range pickers
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

        // Setup nest spending summary fragment
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.history_fragment_container,
                NestFragment.newInstance(NestType.EXPENSE, NestLayoutType.HISTORY)
            )
            .commit()

        // Setup transaction list with photo viewing capability
        val adapter = HistoryTransactionsAdapter(emptyList()) { photoPath ->
            openPhotoViewer(photoPath)
        }
        binding.transactionsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.transactionsRecyclerView.adapter = adapter

        // Collect grouped transactions (automatically updates when date range changes)
        lifecycleScope.launchWhenStarted {
            viewModel.groupedTransactions.collect { grouped ->
                adapter.updateData(grouped)
            }
        }
    }


    // begin code attribution
    // DatePickerDialog usage adapted from:
    // Android Developers: Pickers guide

    // Show date picker dialog and return selected timestamp
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

    // end code attribution (Android Developers, 2020)


    // begin code attribution
    // FileProvider usage for viewing images adapted from:
    // Android Developers: Sharing files with FileProvider

    // Open receipt photo in external image viewer
    private fun openPhotoViewer(photoPath: String) {
        try {
            val photoFile = File(photoPath)
            if (photoFile.exists()) {
                // Use FileProvider to securely share the file
                val photoUri: Uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    photoFile
                )

                // Create intent to view image
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

    // end code attribution (Android Developers, 2020)


    // Redirect to login screen if no user is logged in
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
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
}

// reference list
// Android Developers, 2021. Collect Flows Safely. [online] Available at: <https://developer.android.com/kotlin/flow/collect> [Accessed 5 October 2025].
// Android Developers, 2020. Pickers. [online] Available at: <https://developer.android.com/develop/ui/views/components/pickers> [Accessed 5 October 2025].
// Android Developers, 2020. Sharing Files with FileProvider. [online] Available at: <https://developer.android.com/training/secure-file-sharing/setup-sharing> [Accessed 5 October 2025].
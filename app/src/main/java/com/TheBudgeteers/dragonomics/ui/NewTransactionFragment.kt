package com.TheBudgeteers.dragonomics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.data.AppDatabase
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.models.Transaction
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModelFactory
import com.TheBudgeteers.dragonomics.viewmodel.TransactionViewModelFactory
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import com.TheBudgeteers.dragonomics.viewmodel.TransactionViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.app.DatePickerDialog
import java.util.Calendar
import java.util.Date
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.os.bundleOf
import com.TheBudgeteers.dragonomics.data.SessionStore
import kotlinx.coroutines.flow.firstOrNull

// DialogFragment for creating a new transaction.
// Lets the user input transaction details such as title, amount, description, date, category, and optionally attach a photo.
// Handles form validation, category selection, expense/income toggling, date picking, camera permission handling,
// transaction creation, and notifies the parent fragment when the transaction is saved.

class NewTransactionFragment : DialogFragment() {

    // UI elements
    private lateinit var edtTitle: EditText
    private lateinit var edtAmount: EditText
    private lateinit var edtDescription: EditText
    private lateinit var btnDate: ImageButton
    private lateinit var btnPhoto: ImageButton
    private lateinit var btnExpense: TextView
    private lateinit var btnIncome: TextView
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var fromNestOptions: LinearLayout
    private lateinit var txtSelectedCategory: TextView
    private lateinit var txtFromSelectedCategory: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnCreate: Button
    private lateinit var recyclerFromCategories: RecyclerView

    // ViewModels and session data
    private lateinit var nestViewModel: NestViewModel
    private lateinit var session: SessionStore

    // Selected category and transaction details
    private var selectedCategory: Nest? = null
    private var selectedFromCategory: Nest? = null
    private var isExpense = true
    private var selectedDate = Date()
    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var currentUserId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this dialog
        val view = inflater.inflate(R.layout.dialog_new_transaction, container, false)

        // Initialise session store and UI components
        session = SessionStore(requireContext())
        edtTitle = view.findViewById(R.id.edtTransactionTitle)
        edtAmount = view.findViewById(R.id.edtTransactionAmount)
        edtDescription = view.findViewById(R.id.edtTransactionDescription)
        btnDate = view.findViewById(R.id.btnDate)
        btnPhoto = view.findViewById(R.id.btnPhoto)
        btnExpense = view.findViewById(R.id.btnExpense)
        btnIncome = view.findViewById(R.id.btnIncome)
        recyclerCategories = view.findViewById(R.id.recyclerTransactionCategories)
        fromNestOptions = view.findViewById(R.id.fromNestOptions)
        txtSelectedCategory = view.findViewById(R.id.txtSelectedCategory)
        txtFromSelectedCategory = view.findViewById(R.id.txtSelectedFromCategory)
        btnCancel = view.findViewById(R.id.btnCancelTransaction)
        btnCreate = view.findViewById(R.id.btnCreateTransaction)
        recyclerFromCategories = view.findViewById(R.id.recyclerFromCategories)

        // Setup buttons, category grids, and toggle functionality
        setupToggleButtons()
        setupCategoryGrid()

        // Button click listeners
        btnCancel.setOnClickListener { dismiss() }
        btnCreate.setOnClickListener { createTransaction() }
        btnDate.setOnClickListener { showDatePicker() }
        btnPhoto.setOnClickListener { takePhotoWithPermissionCheck() }

        // Initialise ViewModel for categories
        val repository = Repository(AppDatabase.getDatabase(requireContext()))
        nestViewModel = ViewModelProvider(this, NestViewModelFactory(repository))
            .get(NestViewModel::class.java)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load current logged-in user
        lifecycleScope.launch {
            currentUserId = session.userId.firstOrNull()
            if (currentUserId != null) {
                toggleType(true) // Default to expense transactions
            } else {
                Toast.makeText(requireContext(), "Error: No user logged in", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Ensure dialog takes full width of screen
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    // Sets up toggle buttons for Expense and Income
    private fun setupToggleButtons() {
        btnExpense.setOnClickListener { toggleType(true) }
        btnIncome.setOnClickListener { toggleType(false) }
    }

    // Toggles between expense and income transaction mode
    private fun toggleType(expense: Boolean) {
        isExpense = expense
        btnExpense.isSelected = expense
        btnIncome.isSelected = !expense
        fromNestOptions.visibility = if (expense) View.VISIBLE else View.GONE
        if (expense) loadFromCategories() else selectedFromCategory = null
        loadCategories(expense)
    }

    // Loads available categories for the transaction type
    private fun loadCategories(expense: Boolean) {
        val userId = currentUserId ?: return
        val type = if (expense) NestType.EXPENSE else NestType.INCOME

        viewLifecycleOwner.lifecycleScope.launch {
            nestViewModel.getNestsByTypeLive(userId, type).collect { categories ->
                recyclerCategories.layoutManager = GridLayoutManager(requireContext(), 6)
                recyclerCategories.adapter = NewTransactionNestAdapter(categories) {
                    selectedCategory = it
                    txtSelectedCategory.text = it.name
                }
            }
        }
    }

    // Loads available "From" categories when creating expense transactions
    private fun loadFromCategories() {
        val userId = currentUserId ?: return
        recyclerFromCategories.layoutManager = GridLayoutManager(requireContext(), 6)

        viewLifecycleOwner.lifecycleScope.launch {
            nestViewModel.getNestsByTypeLive(userId, NestType.INCOME).collect { categories ->
                recyclerFromCategories.adapter = NewTransactionNestAdapter(categories) {
                    selectedFromCategory = it
                    txtFromSelectedCategory.text = it.name
                }
            }
        }
    }

    // begin code attribution
    // DatePickerDialog usage adapted from:
    // Android Developers guide to pickers

    // Displays a date picker dialog for selecting the transaction date
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    // end code attribution (Android Developers, 2020)


    // begin code attribution
    // Runtime permissions handling adapted from:
    // Android Developers guide to permissions

    // Requests camera permission and launches photo capture if granted
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) takePhoto() else
                Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
        }

    // end code attribution (Android Developers, 2020)


    // begin code attribution
    // Camera capture using TakePicture contract adapted from:
    // Android Developers guide to camera intents

    //Handles captured photo result
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Photo captured!", Toast.LENGTH_SHORT).show()
            } else {
                currentPhotoPath = null
                photoUri = null
            }
        }

    // end code attribution (Android Developers, 2020)

    // begin code attribution
    // Temporary image file creation adapted from:
    // Android Developers guide to saving photos

    // Creates a temporary file to store a photo taken
    @Throws(IOException::class)
    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                currentPhotoPath = absolutePath
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            Toast.makeText(requireContext(), "Error creating image file", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // end code attribution (Android Developers, 2020)

    // Checks camera permission before taking a photo
    private fun takePhotoWithPermissionCheck() {
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    // begin code attribution
    // Camera capture with FileProvider adapted from:
    // Android Developers guide to sharing files

    // Initiates photo capture
    private fun takePhoto() {
        val photoFile = createImageFile()
        photoFile?.let {
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                it
            )
            takePictureLauncher.launch(photoUri)
        } ?: run {
            Toast.makeText(requireContext(), "Failed to create photo file", Toast.LENGTH_SHORT).show()
        }
    }

    // end code attribution (Android Developers, 2020)

    // Sets up the category selection grid
    private fun setupCategoryGrid() {
        recyclerCategories.layoutManager = GridLayoutManager(requireContext(), 6)
    }

    // Validates inputs and creates a new transaction
    private fun createTransaction() {
        val title = edtTitle.text.toString().trim()
        val amount = edtAmount.text.toString().toDoubleOrNull() ?: 0.0
        val userId = currentUserId

        if (userId == null) {
            Toast.makeText(requireContext(), "Error: No user logged in", Toast.LENGTH_SHORT).show()
            return
        }
        if (title.isBlank()) {
            edtTitle.error = "Title is required"
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }
        if (isExpense && selectedFromCategory == null) {
            Toast.makeText(requireContext(), "Please select a 'From' category", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount <= 0) {
            edtAmount.error = "Amount must be greater than zero"
            Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Create transaction object
        val transaction = Transaction(
            userId = userId,
            title = title,
            amount = amount,
            date = selectedDate,
            photoPath = currentPhotoPath,
            description = edtDescription.text.toString().trim(),
            categoryId = selectedCategory!!.id,
            fromCategoryId = if (isExpense) selectedFromCategory!!.id else null
        )

        // Save transaction using ViewModel
        val repository = Repository(AppDatabase.getDatabase(requireContext()))
        val vm = ViewModelProvider(this, TransactionViewModelFactory(repository))
            .get(TransactionViewModel::class.java)
        vm.addTransaction(transaction)

        // Notify parent fragment and close dialog
        val addedPhoto = currentPhotoPath != null
        parentFragmentManager.setFragmentResult("tx_saved", bundleOf("addedPhoto" to addedPhoto))
        Toast.makeText(requireContext(), "Transaction created successfully", Toast.LENGTH_SHORT).show()
        dismiss()
    }
}

// Android Developers, 2020. DatePickerDialog guide. [online] Available at: <https://developer.android.com/guide/topics/ui/controls/pickers> [Accessed 1 October 2025]
// Android Developers, 2020. Permissions guide. [online] Available at: <https://developer.android.com/guide/topics/permissions> [Accessed 1 October 2025]
// Android Developers, 2020. Camera intents guide. [online] Available at: <https://developer.android.com/training/camera> [Accessed 1 October 2025]
// Android Developers, 2020. Saving photos guide. [online] Available at: <https://developer.android.com/training/camera/photobasics> [Accessed 1 October 2025]

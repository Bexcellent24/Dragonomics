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
import androidx.core.os.bundleOf // <-- ADDED for setFragmentResult payload

class NewTransactionFragment : DialogFragment() {

    private lateinit var edtTitle: EditText
    private lateinit var edtAmount: EditText
    private lateinit var edtDescription: EditText
    private lateinit var btnDate: ImageButton
    private lateinit var btnPhoto: ImageButton
    private lateinit var btnExpense: TextView
    private lateinit var btnIncome: TextView
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var nestViewModel: NestViewModel
    private lateinit var fromNestOptions: LinearLayout
    private lateinit var txtSelectedCategory: TextView
    private lateinit var txtFromSelectedCategory: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnCreate: Button
    private lateinit var recyclerFromCategories: RecyclerView
    private var selectedCategory: Nest? = null
    private var selectedFromCategory: Nest? = null
    private var isExpense = true
    private var selectedDate = Date()
    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_new_transaction, container, false)

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

        setupToggleButtons()
        setupCategoryGrid()

        btnCancel.setOnClickListener { dismiss() }
        btnCreate.setOnClickListener { createTransaction() }

        val repository = Repository(AppDatabase.getDatabase(requireContext()))
        nestViewModel = ViewModelProvider(this, NestViewModelFactory(repository))
            .get(NestViewModel::class.java)

        btnDate.setOnClickListener { showDatePicker() }
        btnPhoto.setOnClickListener { takePhotoWithPermissionCheck() }

        btnCreate.isEnabled = true

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toggleType(true) // default to Expense
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupToggleButtons() {
        btnExpense.setOnClickListener { toggleType(true) }
        btnIncome.setOnClickListener { toggleType(false) }
    }

    private fun toggleType(expense: Boolean) {
        isExpense = expense

        btnExpense.isSelected = expense
        btnIncome.isSelected = !expense

        fromNestOptions.visibility = if (expense) View.VISIBLE else View.GONE
        if (expense) loadFromCategories() else selectedFromCategory = null

        loadCategories(expense)
    }

    private fun loadCategories(expense: Boolean) {
        val type = if (expense) NestType.EXPENSE else NestType.INCOME
        viewLifecycleOwner.lifecycleScope.launch {
            nestViewModel.getNestsByTypeLive(type).collect { categories ->
                recyclerCategories.layoutManager = GridLayoutManager(requireContext(), 6)
                recyclerCategories.adapter = NewTransactionNestAdapter(categories) {
                    selectedCategory = it
                    txtSelectedCategory.text = it.name
                }
            }
        }
    }

    private fun loadFromCategories() {
        recyclerFromCategories.layoutManager = GridLayoutManager(requireContext(), 6)
        viewLifecycleOwner.lifecycleScope.launch {
            nestViewModel.getNestsByTypeLive(NestType.INCOME).collect { categories ->
                recyclerFromCategories.adapter = NewTransactionNestAdapter(categories) {
                    selectedFromCategory = it
                    txtFromSelectedCategory.text = it.name
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                takePhoto()
            } else {
                Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
<<<<<<< Updated upstream
                // Photo was taken successfully
                // currentPhotoPath is already set in createImageFile()
                Toast.makeText(requireContext(), "Photo captured!", Toast.LENGTH_SHORT).show()
            } else {
                // Photo capture was cancelled or failed
                currentPhotoPath = null
                photoUri = null
=======
                photoUri?.let { currentPhotoPath = it.toString() }
>>>>>>> Stashed changes
            }
        }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        return try {
            val timeStamp: String =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
<<<<<<< Updated upstream
            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            ).apply {
                // FIXED: Save the actual file path, not the URI
=======
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
>>>>>>> Stashed changes
                currentPhotoPath = absolutePath
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            Toast.makeText(requireContext(), "Error creating image file", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun takePhotoWithPermissionCheck() {
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun takePhoto() {
        val photoFile: File? = createImageFile()
        photoFile?.also {
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

    private fun setupCategoryGrid() {
        recyclerCategories.layoutManager = GridLayoutManager(requireContext(), 6)
    }

    private fun checkCreateButtonState() {
        val hasTitle = edtTitle.text.toString().isNotBlank()
        val hasCategory = selectedCategory != null
        val hasFromCategory = !isExpense || (isExpense && selectedFromCategory != null)
        btnCreate.isEnabled = hasTitle && hasCategory && hasFromCategory
    }

    private fun createTransaction() {
        val title = edtTitle.text.toString().trim()
        val amount = edtAmount.text.toString().toDoubleOrNull() ?: 0.0

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

        val transaction = Transaction(
            title = title,
            amount = amount,
            date = selectedDate,
            photoPath = currentPhotoPath,  // This now contains the actual file path
            description = edtDescription.text.toString().trim(),
            categoryId = selectedCategory!!.id,
            fromCategoryId = if (isExpense) selectedFromCategory!!.id else null
        )

        val repository = Repository(AppDatabase.getDatabase(requireContext()))
        val vm = ViewModelProvider(this, TransactionViewModelFactory(repository))
            .get(TransactionViewModel::class.java)

        vm.addTransaction(transaction)

        // ===== NEW: tell host activity we saved and whether a photo was attached =====
        val addedPhoto = currentPhotoPath != null
        parentFragmentManager.setFragmentResult(
            "tx_saved",
            bundleOf("addedPhoto" to addedPhoto)
        )
        // ============================================================================

        Toast.makeText(requireContext(), "Transaction created successfully", Toast.LENGTH_SHORT).show()
        dismiss()
    }
<<<<<<< Updated upstream
}
=======
}
>>>>>>> Stashed changes

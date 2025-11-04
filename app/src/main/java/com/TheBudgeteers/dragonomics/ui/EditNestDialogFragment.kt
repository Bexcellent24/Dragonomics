package com.TheBudgeteers.dragonomics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.data.NestType
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.ui.adapters.ColourAdapter
import com.TheBudgeteers.dragonomics.ui.adapters.IconAdapter
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// DialogFragment for editing an existing nest.

class EditNestDialogFragment : androidx.fragment.app.DialogFragment() {

    private lateinit var edtName: EditText
    private lateinit var edtAmount: EditText
    private lateinit var txtDialogTitle: TextView
    private lateinit var recyclerIcons: RecyclerView
    private lateinit var recyclerColours: RecyclerView
    private lateinit var btnCancel: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnDelete: Button
    private lateinit var session: SessionStore

    private var selectedIcon: String? = null
    private var selectedColour: String? = null
    private var nestToEdit: Nest? = null

    private val iconList = listOf(
        "ci_airplane", "ci_apartment", "ci_apple", "ci_ball", "ci_bear", "ci_bus", "ci_car",
        "ci_coffee", "ci_coin_stack", "ci_computer", "ci_dumbbells", "ci_fashion", "ci_fuel",
        "ci_gift", "ci_graduate_hat", "ci_heart", "ci_home", "ci_iphone", "ci_make_up", "ci_music",
        "ci_open_book", "ci_paint", "ci_paw", "ci_piggy_bank", "ci_restaurant", "ci_ribbon",
        "ci_scale", "ci_seed", "ci_setting", "ci_shopping_bag", "ci_shopping_cart", "ci_stroller",
        "ci_tent", "ci_tool", "ci_umbrella", "ci_wine-glass"
    )
    private val colourList = listOf("#53171c", "#9b252c", "#523295", "#a44e24", "#8b98ad", "#231c2a")

    companion object {
        private const val ARG_NEST_ID = "nest_id"
        private const val ARG_USER_ID = "user_id"

        fun newInstance(userId: String, nestId: String): EditNestDialogFragment {
            val fragment = EditNestDialogFragment()
            val args = Bundle().apply {
                putString(ARG_USER_ID, userId)
                putString(ARG_NEST_ID, nestId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_edit_nest, container, false)

        session = SessionStore(requireContext())
        edtName = view.findViewById(R.id.edtName)
        edtAmount = view.findViewById(R.id.edtAmount)
        txtDialogTitle = view.findViewById(R.id.txtDialogTitle)
        recyclerIcons = view.findViewById(R.id.recyclerIcons)
        recyclerColours = view.findViewById(R.id.recyclerColours)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnUpdate = view.findViewById(R.id.btnUpdate)
        btnDelete = view.findViewById(R.id.btnDelete)

        setupIconGrid()
        setupColourRow()
        setupButtons()
        loadNestData()

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun loadNestData() {
        val userId = arguments?.getString(ARG_USER_ID) ?: return
        val nestId = arguments?.getString(ARG_NEST_ID) ?: return

        lifecycleScope.launch {
            val repository = RepositoryProvider.getRepository(requireContext())
            val nest = repository.getNestById(userId, nestId)

            if (nest != null) {
                nestToEdit = nest
                populateFields(nest)
            } else {
                Toast.makeText(requireContext(), "Error loading nest", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun populateFields(nest: Nest) {
        edtName.setText(nest.name)

        // Show/hide budget field based on nest type
        // Income nests don't have a budget field
        if (nest.type == NestType.EXPENSE) {
            edtAmount.visibility = View.VISIBLE
            edtAmount.setText(nest.budget?.toString() ?: "")
        } else {
            edtAmount.visibility = View.GONE
        }

        selectedIcon = nest.icon
        selectedColour = nest.colour

        // Highlight selected icon and colour in adapters
        (recyclerIcons.adapter as? IconAdapter)?.setSelectedIcon(nest.icon)
        (recyclerColours.adapter as? ColourAdapter)?.setSelectedColour(nest.colour)
    }

    private fun setupIconGrid() {
        val adapter = IconAdapter(iconList) { iconName ->
            selectedIcon = iconName
        }
        recyclerIcons.layoutManager = GridLayoutManager(requireContext(), 6)
        recyclerIcons.setHasFixedSize(true)
        recyclerIcons.adapter = adapter
    }

    private fun setupColourRow() {
        val adapter = ColourAdapter(colourList) { colourHex ->
            selectedColour = colourHex
        }
        recyclerColours.layoutManager = GridLayoutManager(requireContext(), 6)
        recyclerColours.adapter = adapter
        recyclerColours.setHasFixedSize(true)
    }

    private fun setupButtons() {
        btnCancel.setOnClickListener { dismiss() }
        btnUpdate.setOnClickListener { updateNest() }
        btnDelete.setOnClickListener { showDeleteConfirmation() }
    }

    private fun updateNest() {
        val nest = nestToEdit ?: return
        val name = edtName.text.toString().trim()
        val icon = selectedIcon
        val colour = selectedColour

        edtName.error = null
        edtAmount.error = null

        // Validation
        if (name.isEmpty()) {
            edtName.error = "Please enter a name"
            Toast.makeText(requireContext(), "Please enter a name for your nest", Toast.LENGTH_SHORT).show()
            return
        }

        if (icon.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select an icon", Toast.LENGTH_SHORT).show()
            return
        }

        if (colour.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please select a colour", Toast.LENGTH_SHORT).show()
            return
        }

        // Keep the original nest type (no changing between income/expense)
        val type = nest.type
        var amount: Double? = null

        // Only validate budget for expense nests
        if (type == NestType.EXPENSE) {
            val amountText = edtAmount.text.toString().trim()
            if (amountText.isEmpty()) {
                edtAmount.error = "Please enter a budget amount"
                Toast.makeText(requireContext(), "Please enter a budget amount", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                amount = amountText.toDouble()
                if (amount < 0) {
                    edtAmount.error = "Amount cannot be negative"
                    Toast.makeText(requireContext(), "Amount cannot be negative", Toast.LENGTH_SHORT).show()
                    return
                }
            } catch (e: NumberFormatException) {
                edtAmount.error = "Invalid number format"
                Toast.makeText(requireContext(), "Please enter a valid number for the amount", Toast.LENGTH_SHORT).show()
                return
            }
        }

        lifecycleScope.launch {
            val userId = session.userId.first()
            if (userId == null) {
                Toast.makeText(requireContext(), "Error: No user logged in", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val repository = RepositoryProvider.getRepository(requireContext())

            // Build updates map
            val updates = mutableMapOf<String, Any?>(
                "name" to name,
                "icon" to icon,
                "colour" to colour
            )

            // Only update budget for expense nests
            if (type == NestType.EXPENSE) {
                updates["budget"] = amount
            }

            // Update nest
            val result = repository.updateNest(userId, nest.id, updates)

            if (result.isSuccess) {
                parentFragmentManager.setFragmentResult("nest_updated", Bundle.EMPTY)
                Toast.makeText(requireContext(), "Nest updated successfully", Toast.LENGTH_SHORT).show()
                view?.postDelayed({ dismiss() }, 200)
            } else {
                Toast.makeText(requireContext(), "Error updating nest", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        val nest = nestToEdit ?: return

        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Delete Nest?")
            .setMessage(
                "Are you sure you want to delete '${nest.name}'?\n\n" +
                        "All transactions in this nest will be moved to an 'Uncategorized' nest.\n\n" +
                        "This action cannot be undone."
            )
            .setPositiveButton("Delete") { _, _ ->
                deleteNest()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteNest() {
        val nest = nestToEdit ?: return

        lifecycleScope.launch {
            val userId = session.userId.first()
            if (userId == null) {
                Toast.makeText(requireContext(), "Error: No user logged in", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val repository = RepositoryProvider.getRepository(requireContext())

            // Delete nest (this will handle transaction reassignment in the repository)
            val result = repository.deleteNest(userId, nest.id)

            if (result.isSuccess) {
                parentFragmentManager.setFragmentResult("nest_deleted", Bundle.EMPTY)
                Toast.makeText(requireContext(), "Nest deleted successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Error deleting nest: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
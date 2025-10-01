package com.TheBudgeteers.dragonomics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.data.AppDatabase
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModelFactory

class NewNestDialogFragment : androidx.fragment.app.DialogFragment() {

    private lateinit var edtName: EditText
    private lateinit var edtAmount: EditText
    private lateinit var btnIncoming: TextView
    private lateinit var btnOutgoing: TextView
    private lateinit var recyclerIcons: RecyclerView
    private lateinit var recyclerColours: RecyclerView
    private lateinit var btnCancel: Button
    private lateinit var btnCreate: Button

    private var selectedIcon: String? = null
    private var selectedColour: String? = null
    private var isIncome = false

    // small predefined lists, add or change as you need
    private val iconList = listOf("ci_airplane", "ci_apartment", "ci_apple", "ci_ball", "ci_bear", "ci_bus", "ci_car", "ci_coffee", "ci_coin_stack", "ci_computer", "ci_dumbbells", "ci_fashion", "ci_fuel", "ci_gift", "ci_graduate_hat", "ci_heart", "ci_home", "ci_iphone", "ci_make_up", "ci_music", "ci_open_book", "ci_paint", "ci_paw", "ci_piggy_bank", "ci_restaurant", "ci_ribbon", "ci_scale", "ci_seed", "ci_setting", "ci_shopping_bag", "ci_shopping_cart", "ci_stroller", "ci_tent", "ci_tool", "ci_umbrella", "ci_wine-glass")
    private val colourList = listOf("#53171c", "#9b252c", "#523295", "#a44e24", "#8b98ad", "#231c2a")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_new_nest, container, false)
        edtName = view.findViewById(R.id.edtName)
        edtAmount = view.findViewById(R.id.edtAmount)
        btnIncoming = view.findViewById(R.id.btnIncoming)
        btnOutgoing = view.findViewById(R.id.btnOutgoing)
        recyclerIcons = view.findViewById(R.id.recyclerIcons)
        recyclerColours = view.findViewById(R.id.recyclerColours)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnCreate = view.findViewById(R.id.btnCreate)

        setupIconGrid()
        setupColourRow()
        setupToggleButtons()
        setupButtons()
        setupValidationTriggers()

        return view
    }

    private fun setupIconGrid() {
        val adapter = IconAdapter(iconList) { iconName ->
            selectedIcon = iconName
            updateCreateButtonState()
        }
        recyclerIcons.layoutManager = GridLayoutManager(requireContext(), 6)
        recyclerIcons.setHasFixedSize(true)

        recyclerIcons.adapter = adapter
    }

    private fun setupColourRow() {
        val adapter = ColourAdapter(colourList) { colourHex ->
            selectedColour = colourHex
            updateCreateButtonState()
        }
        val layoutManager = GridLayoutManager(requireContext(), 6)
        recyclerColours.layoutManager = layoutManager
        recyclerColours.adapter = adapter
        recyclerColours.setHasFixedSize(true)
    }

    private fun setupToggleButtons() {
        // default outgoing selected
        setIncome(false)
        btnIncoming.setOnClickListener { setIncome(true) }
        btnOutgoing.setOnClickListener { setIncome(false) }
    }

    private fun setIncome(income: Boolean) {
        isIncome = income

        btnIncoming.isSelected = income
        btnOutgoing.isSelected = !income

        edtAmount.isEnabled = !income
        if (income) edtAmount.setText("")
        updateCreateButtonState()
    }

    private fun setupButtons() {
        btnCancel.setOnClickListener { dismiss() }
        btnCreate.setOnClickListener { createNestAndDismiss() }
    }

    private fun setupValidationTriggers() {
        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { updateCreateButtonState() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        edtName.addTextChangedListener(watcher)
        edtAmount.addTextChangedListener(watcher)
    }

    private fun updateCreateButtonState() {
        val hasName = edtName.text.toString().trim().isNotEmpty()
        val hasColour = !selectedColour.isNullOrEmpty()
        val hasIcon = !selectedIcon.isNullOrEmpty()
        val amountOk = if (isIncome) true else {
            val amt = edtAmount.text.toString().trim()
            amt.isNotEmpty() && try { amt.toDouble() >= 0.0 } catch (e: Exception) { false }
        }
        btnCreate.isEnabled = hasName && hasColour && hasIcon && amountOk
    }

    private fun createNestAndDismiss() {
        val name = edtName.text.toString().trim()
        val amount = edtAmount.text.toString().trim().toDoubleOrNull()
        val icon = selectedIcon ?: return
        val colour = selectedColour ?: return
        val type = if (isIncome) NestType.INCOME else NestType.EXPENSE

        val nest = Nest(
            name = name,
            budget = if (type == NestType.EXPENSE) amount else null,
            icon = icon,
            colour = colour,
            type = type
        )

        val repository = Repository(AppDatabase.getDatabase(requireContext()))
        val factory = NestViewModelFactory(repository)
        val vm = ViewModelProvider(this, factory).get(NestViewModel::class.java)

        vm.addNest(nest) {
            // Use FragmentResult so NestFragment knows to refresh
            parentFragmentManager.setFragmentResult("new_nest_created", Bundle.EMPTY)
            dismiss()
        }
    }

}

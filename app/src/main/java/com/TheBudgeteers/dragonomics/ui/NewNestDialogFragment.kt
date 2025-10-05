package com.TheBudgeteers.dragonomics.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.data.AppDatabase
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.data.SessionStore
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModelFactory
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class NewNestDialogFragment : androidx.fragment.app.DialogFragment() {

    private lateinit var edtName: EditText
    private lateinit var edtAmount: EditText
    private lateinit var btnIncoming: TextView
    private lateinit var btnOutgoing: TextView
    private lateinit var recyclerIcons: RecyclerView
    private lateinit var recyclerColours: RecyclerView
    private lateinit var btnCancel: Button
    private lateinit var btnCreate: Button
    private lateinit var session: SessionStore

    private var selectedIcon: String? = null
    private var selectedColour: String? = null
    private var isIncome = false

    // small predefined lists, add or change as you need
    private val iconList = listOf("ci_airplane", "ci_apartment", "ci_apple", "ci_ball", "ci_bear", "ci_bus", "ci_car", "ci_coffee", "ci_coin_stack", "ci_computer", "ci_dumbbells", "ci_fashion", "ci_fuel", "ci_gift", "ci_graduate_hat", "ci_heart", "ci_home", "ci_iphone", "ci_make_up", "ci_music", "ci_open_book", "ci_paint", "ci_paw", "ci_piggy_bank", "ci_restaurant", "ci_ribbon", "ci_scale", "ci_seed", "ci_setting", "ci_shopping_bag", "ci_shopping_cart", "ci_stroller", "ci_tent", "ci_tool", "ci_umbrella", "ci_wine-glass")
    private val colourList = listOf("#53171c", "#9b252c", "#523295", "#a44e24", "#8b98ad", "#231c2a")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_new_nest, container, false)

        session = SessionStore(requireContext())
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


        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
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


        edtAmount.visibility = if (income) View.GONE else View.VISIBLE
        if (income) edtAmount.text.clear()


    }

    private fun setupButtons() {
        btnCancel.setOnClickListener { dismiss() }
        btnCreate.setOnClickListener { createNestAndDismiss() }
    }



    private fun createNestAndDismiss() {
        val name = edtName.text.toString().trim()
        val icon = selectedIcon
        val colour = selectedColour

        // Reset previous errors
        edtName.error = null
        edtAmount.error = null

        // Validation checks with field errors and Toasts
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

        val type = if (isIncome) NestType.INCOME else NestType.EXPENSE
        var amount: Double? = null

        if (!isIncome) {
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
            val userId = session.userId.firstOrNull()
            if (userId == null) {
                Toast.makeText(requireContext(), "Error: No user logged in", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

            val nest = Nest(
                userId = userId,  // ✅ NEW: Add userId
                name = name,
                budget = if (type == NestType.EXPENSE) amount else null,
                icon = icon,
                colour = colour,
                type = type
            )

            val repository = Repository(AppDatabase.getDatabase(requireContext()))
            val factory = NestViewModelFactory(repository)
            val vm = ViewModelProvider(
                this@NewNestDialogFragment,
                factory
            ).get(NestViewModel::class.java)

            vm.addNest(nest) {
                parentFragmentManager.setFragmentResult("new_nest_created", Bundle.EMPTY)
                Toast.makeText(requireContext(), "Nest created successfully", Toast.LENGTH_SHORT)
                    .show()
                view?.postDelayed({ dismiss() }, 200)
            }
        }
    }
}

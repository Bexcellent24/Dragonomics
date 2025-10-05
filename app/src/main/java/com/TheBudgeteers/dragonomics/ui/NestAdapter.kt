package com.TheBudgeteers.dragonomics.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.data.NestLayoutType
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.viewmodel.NestUiState
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch



// NestAdapter is a RecyclerView adapter for displaying Nests in different layouts:
// GRID, LIST, and HISTORY.

// - GRID: shows nest name, icon, mood, budget progress, spent amount, and remaining budget.
// - LIST: shows nest name, icon, budget, and coloured progress bar.
// - HISTORY: shows nest name, icon, and amount spent within a given date range.
//  In this class we tried to manage all variations of nest layouts used on different pages, helping to avoid repeating large code blocks.


class NestAdapter(
    private val nestViewModel: NestViewModel,
    private val userId: Long,
    private val layoutType: NestLayoutType,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val startDateFlow: Flow<Long>? = null,
    private val endDateFlow: Flow<Long>? = null,
    private val onClick: (Nest) -> Unit
) : RecyclerView.Adapter<NestAdapter.NestViewHolder>() {

    private val nests = mutableListOf<Nest>()
    private val nestSpentMap = mutableMapOf<Long, Double>()

    // begin code attribution
    // combine() and flatMapLatest() usage adapted from:
    // Kotlin Coroutines documentation: Combining flows
    init {
        // For HISTORY layout: collect spent amounts in date range
        if (startDateFlow != null && endDateFlow != null) {
            lifecycleScope.launch {
                combine(startDateFlow, endDateFlow) { start, end -> start to end }
                    .flatMapLatest { (start, end) ->
                        nestViewModel.getSpentAmountsInRange(userId, start, end)
                    }
                    .collect { spentMap ->
                        nestSpentMap.clear()
                        nestSpentMap.putAll(spentMap)
                        notifyDataSetChanged() // Refresh UI when spent data changes
                    }
            }
        }
    }
    // end code attribution (Kotlin Documentation, 2020)

    // Updates the nest list displayed in the adapter.
    fun setNests(newNests: List<Nest>) {
        nests.clear()
        nests.addAll(newNests)
        notifyDataSetChanged()
    }

    //ViewHolder for nest items.
    //Contains references to UI elements and tracks coroutine jobs for reactive updates.
    inner class NestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNestName: TextView = view.findViewById(R.id.txtNestName)
        val imgMood: ImageView? = view.findViewById(R.id.imgMood)
        val progressBar: ProgressBar? = view.findViewById(R.id.progressBar)
        val txtSpent: TextView? = view.findViewById(R.id.amountSpent)
        val txtBudget: TextView? = view.findViewById(R.id.txtBudget)
        val txtRemaining: TextView? = view.findViewById(R.id.amountRemaining)
        val layoutNestBar: View? = view.findViewById(R.id.layoutNestBar)
        val imgNestIcon: ImageView? = view.findViewById(R.id.imgNestIcon)
        val txtSpentInRange: TextView? = view.findViewById(R.id.txtSpentInRange)

        var bindJob: Job? = null // Track coroutine job to cancel on rebind
    }

    // begin code attribution
    // Inflating different layouts based on view type adapted from:
    // Android Developers guide to RecyclerView

    // Choose layout based on selected layout type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NestViewHolder {
        val layoutRes = when (layoutType) {
            NestLayoutType.GRID -> R.layout.item_nest
            NestLayoutType.LIST -> R.layout.item_nest_list
            NestLayoutType.HISTORY -> R.layout.item_nest_history
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return NestViewHolder(view)
    }
    // end code attribution (Android Developers, 2020)

    override fun onBindViewHolder(holder: NestViewHolder, position: Int) {
        // Cancel previous bind job to prevent multiple collectors for recycled views
        holder.bindJob?.cancel()

        val nest = nests[position]
        holder.txtNestName.text = nest.name

        when (layoutType) {
            NestLayoutType.GRID -> bindGrid(holder, nest)
            NestLayoutType.LIST -> bindList(holder, nest)
            NestLayoutType.HISTORY -> bindHistory(holder, nest)
        }

        holder.itemView.setOnClickListener { onClick(nest) }
    }

    //Bind data for GRID layout with reactive UI updates.
    private fun bindGrid(holder: NestViewHolder, nest: Nest) {
        // Set icon
        val iconRes = NestUiMapper.getIconResource(holder.itemView.context, nest.icon)
        if (iconRes != 0) {
            holder.imgNestIcon?.setImageResource(iconRes)
        }

        // Collect UI state and update views reactively
        holder.bindJob = lifecycleScope.launch {
            nestViewModel.getNestUiStateFlow(userId,nest.id).collect { uiState ->
                updateGridViews(holder, uiState)
            }
        }
    }

    // Update UI for GRID layout based on nest UI state.
    private fun updateGridViews(holder: NestViewHolder, state: NestUiState) {
        // Progress bar shows remaining/budget percentage
        holder.progressBar?.progress = if (state.budget > 0) {
            ((state.remaining / state.budget) * 100).toInt()
        } else {
            0
        }

        holder.txtSpent?.text = NestUiMapper.formatCurrency(state.spent)
        holder.txtBudget?.text = NestUiMapper.formatCurrency(state.budget)
        holder.txtRemaining?.text = NestUiMapper.formatCurrency(state.remaining)
        holder.imgMood?.setImageResource(NestUiMapper.getMoodDrawable(state.mood))
    }

    // Bind data for LIST layout.
    private fun bindList(holder: NestViewHolder, nest: Nest) {
        // Set icon
        val iconRes = NestUiMapper.getIconResource(holder.itemView.context, nest.icon)
        if (iconRes != 0) {
            holder.imgNestIcon?.setImageResource(iconRes)
        }

        // Set color bar
        holder.layoutNestBar?.background?.mutate()?.let { bg ->
            if (bg is GradientDrawable) {
                bg.setColor(NestUiMapper.parseColorSafe(nest.colour))
            }
        }

        // Collect UI state to get the correct budget (important for income nests)
        holder.bindJob = lifecycleScope.launch {
            nestViewModel.getNestUiStateFlow(userId,nest.id).collect { uiState ->
                holder.txtBudget?.text = NestUiMapper.formatCurrency(uiState.budget)
            }
        }
    }

    // Bind data for HISTORY layout.
    private fun bindHistory(holder: NestViewHolder, nest: Nest) {
        // Set icon
        val iconRes = NestUiMapper.getIconResource(holder.itemView.context, nest.icon)
        if (iconRes != 0) {
            holder.imgNestIcon?.setImageResource(iconRes)
        }

        // Set spent amount from map
        val spent = nestSpentMap[nest.id] ?: 0.0
        holder.txtSpentInRange?.text = NestUiMapper.formatCurrency(spent)

        // Set background color
        holder.txtSpentInRange?.setBackgroundColor(
            NestUiMapper.parseColorSafe(nest.colour)
        )
    }

    override fun onViewRecycled(holder: NestViewHolder) {
        super.onViewRecycled(holder)
        // Cancel any ongoing collection when view is recycled
        holder.bindJob?.cancel()
        holder.bindJob = null
    }

    override fun getItemCount() = nests.size
}

// reference list
// Kotlin Documentation, 2020. Combining Flows. [online] Available at: <https://kotlinlang.org/docs/flow.html#combine> [Accessed 29 September 2025].
// Android Developers, 2020. Create a List with RecyclerView. [online] Available at: <https://developer.android.com/develop/ui/views/layout/recyclerview> [Accessed 29 September 2025].
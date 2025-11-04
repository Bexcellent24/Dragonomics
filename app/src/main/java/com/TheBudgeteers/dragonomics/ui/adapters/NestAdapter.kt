package com.TheBudgeteers.dragonomics.ui.adapters

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
import com.TheBudgeteers.dragonomics.ui.NestUiMapper
import com.TheBudgeteers.dragonomics.viewmodel.NestUiState
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch


// NestAdapter is a RecyclerView adapter for displaying Nests in different layouts.
// UPDATED FOR FIREBASE: Uses String userId and nestId instead of Long.

class NestAdapter(
    private val nestViewModel: NestViewModel,
    private val userId: String,
    private val layoutType: NestLayoutType,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val startDateFlow: Flow<Long>? = null,
    private val endDateFlow: Flow<Long>? = null,
    private val onClick: (Nest) -> Unit
) : RecyclerView.Adapter<NestAdapter.NestViewHolder>() {

    private val nests = mutableListOf<Nest>()
    private val nestSpentMap = mutableMapOf<String, Double>()

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

    // Updates the nest list displayed in the adapter.
    fun setNests(newNests: List<Nest>) {
        nests.clear()
        nests.addAll(newNests)
        notifyDataSetChanged()
    }

    // ViewHolder for nest items.
    // Contains references to UI elements and tracks coroutine jobs for reactive updates.
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
    // Standard RecyclerView.Adapter pattern (onCreateViewHolder, onBindViewHolder, getItemCount) adapted from:
    // “Create dynamic lists with RecyclerView” — Android Developers guide

    // Choose layout based on selected layout type.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NestViewHolder {
        val layoutRes = when (layoutType) {
            NestLayoutType.GRID -> R.layout.item_nest
            NestLayoutType.LIST -> R.layout.item_nest_list
            NestLayoutType.HISTORY -> R.layout.item_nest_history
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return NestViewHolder(view)
    }

    // end code attribution (Android Developers, 2025)

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

    // begin code attribution
    // Using Kotlin coroutines + Flow to collect UI state for each ViewHolder adapted from:
    // StackOverflow discussion “Combine Flow<List> and string in Kotlin” — demonstrates combining flows and reacting to updates

    // Bind data for GRID layout with reactive UI updates.
    private fun bindGrid(holder: NestViewHolder, nest: Nest) {
        // Set icon
        val iconRes = NestUiMapper.getIconResource(holder.itemView.context, nest.icon)
        if (iconRes != 0) {
            holder.imgNestIcon?.setImageResource(iconRes)
        }

        // Collect UI state and update views reactively
        // UPDATED: Now passes String userId and nestId
        holder.bindJob = lifecycleScope.launch {
            nestViewModel.getNestUiStateFlow(userId, nest.id).collect { uiState ->
                updateGridViews(holder, uiState)
            }
        }
    }

    // end code attribution (user18958467, 2022)

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
        // UPDATED: Now passes String userId and nestId
        holder.bindJob = lifecycleScope.launch {
            nestViewModel.getNestUiStateFlow(userId, nest.id).collect { uiState ->
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

        // Set spent amount from map (key is now String)
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

// Android Developers. 2025. Create dynamic lists with RecyclerView. [online] Available at: <https://developer.android.com/guide/topics/ui/layout/recyclerview> [Accessed 4 November 2025]
// user18958467. 2022. Combine Flow<List> and string in Kotlin. [online] Available at: <https://stackoverflow.com/questions/72601167/combine-flowlist-and-string-in-kotlin> [Accessed 4 November 2025]

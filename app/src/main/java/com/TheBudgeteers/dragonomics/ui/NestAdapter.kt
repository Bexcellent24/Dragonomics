package com.TheBudgeteers.dragonomics.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import android.graphics.Color
import androidx.lifecycle.LifecycleCoroutineScope
import com.TheBudgeteers.dragonomics.data.NestLayoutType
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.viewmodel.NestViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class NestAdapter(
    private val nestViewModel: NestViewModel,
    private val layoutType: NestLayoutType,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val startDateFlow: Flow<Long>? = null,
    private val endDateFlow: Flow<Long>? = null,
    private val onClick: (Nest) -> Unit
) : RecyclerView.Adapter<NestAdapter.NestViewHolder>() {

    private val nests = mutableListOf<Nest>()
    private val nestSpentMap = mutableMapOf<Long, Double>()


    init {
        // Collect spent amounts once
        if (startDateFlow != null && endDateFlow != null) {
            lifecycleScope.launch {
                combine(startDateFlow, endDateFlow) { start, end -> start to end }
                    .flatMapLatest { (start, end) ->
                        nestViewModel.getSpentAmountsInRange(start, end)
                    }
                    .collect { spentMap ->
                        nestSpentMap.clear()
                        nestSpentMap.putAll(spentMap)
                        notifyDataSetChanged()
                    }
            }
        }
    }
    fun setNests(newNests: List<Nest>) {
        nests.clear()
        nests.addAll(newNests)
        notifyDataSetChanged()
    }

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
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NestViewHolder {
        val layoutRes = when (layoutType) {
            NestLayoutType.GRID -> R.layout.item_nest
            NestLayoutType.LIST -> R.layout.item_nest_list
            NestLayoutType.HISTORY -> R.layout.item_nest_history
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return NestViewHolder(view)
    }


    override fun onBindViewHolder(holder: NestViewHolder, position: Int) {
        val nest = nests[position]
        holder.txtNestName.text = nest.name

        when (layoutType) {
            NestLayoutType.GRID -> bindGrid(holder, nest)
            NestLayoutType.LIST -> bindList(holder, nest)
            NestLayoutType.HISTORY -> bindHistory(holder, nest)
        }

        holder.itemView.setOnClickListener { onClick(nest) }
    }

    private fun bindGrid(holder: NestViewHolder, nest: Nest) {
        // Icon
        holder.imgNestIcon?.let {
            val iconResId = holder.itemView.context.resources.getIdentifier(
                nest.icon, "drawable", holder.itemView.context.packageName
            )
            if (iconResId != 0) it.setImageResource(iconResId)
        }

        if (holder.progressBar != null) {
            if (nest.type == NestType.INCOME) {
                lifecycleScope.launchWhenStarted {
                    val totalIn = suspendCancellableCoroutine<Double> { cont ->
                        nestViewModel.getNestProgressAndMoodWithSpent(nest.id) { _, _, spent ->
                            cont.resume(spent, onCancellation = null)
                        }
                    }

                    nestViewModel.getSpentAmountFlow(nest.id).collect { spent ->
                        val displayedSpent = spent ?: 0.0
                        val remaining = totalIn - displayedSpent

                        updateHolderUI(holder, totalIn, displayedSpent, remaining)
                    }
                }
            }
            else {

                val budget = nest.budget ?: 0.0
                lifecycleScope.launchWhenStarted {
                    nestViewModel.getNestProgressAndMoodWithSpent(nest.id) { _, _, spent ->
                        val displayedSpent = spent
                        val remaining = budget - displayedSpent

                        updateHolderUI(holder, budget, displayedSpent, remaining)
                    }
                }
            }
        }
    }

    private fun bindList(holder: NestViewHolder, nest: Nest) {
        holder.txtBudget?.text = "R${nest.budget ?: 0.0}"

        holder.imgNestIcon?.let {
            val iconResId = holder.itemView.context.resources.getIdentifier(
                nest.icon, "drawable", holder.itemView.context.packageName
            )
            if (iconResId != 0) it.setImageResource(iconResId)
        }

        holder.layoutNestBar?.background?.mutate()?.let { bg ->
            if (bg is android.graphics.drawable.GradientDrawable) {
                try {
                    bg.setColor(Color.parseColor(nest.colour))
                } catch (e: IllegalArgumentException) {
                    bg.setColor(Color.GRAY)
                }
            }
        }
    }

    private fun bindHistory(holder: NestViewHolder, nest: Nest) {
        holder.imgNestIcon?.let {
            val iconResId = holder.itemView.context.resources.getIdentifier(
                nest.icon, "drawable", holder.itemView.context.packageName
            )
            if (iconResId != 0) it.setImageResource(iconResId)
        }

        val spent = nestSpentMap[nest.id] ?: 0.0
        holder.txtSpentInRange?.text = "R${spent.toInt()}"

        nest.colour?.takeIf { it.isNotBlank() }?.let {
            try {
                holder.txtSpentInRange?.setBackgroundColor(Color.parseColor(it))
            } catch (e: Exception) {
                holder.txtSpentInRange?.setBackgroundColor(Color.GRAY)
            }
        } ?: holder.txtSpentInRange?.setBackgroundColor(Color.GRAY)
    }


    private fun updateHolderUI(holder: NestViewHolder, budget: Double, spent: Double, remaining: Double) {
        holder.progressBar?.progress = if (budget > 0) ((remaining / budget) * 100).toInt() else 0
        holder.txtSpent?.text = "R${spent.toInt()}"
        holder.txtBudget?.text = "R${budget.toInt()}"
        holder.txtRemaining?.text = "R${remaining.toInt()}"

        val moodDrawable = when {
            remaining >= budget -> R.drawable.mood_happy
            remaining > 0 -> R.drawable.mood_neutral
            else -> R.drawable.mood_angry
        }
        holder.imgMood?.setImageResource(moodDrawable)
    }


    override fun getItemCount() = nests.size
}



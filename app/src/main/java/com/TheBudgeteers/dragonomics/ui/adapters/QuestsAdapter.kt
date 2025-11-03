package com.TheBudgeteers.dragonomics.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.viewmodel.AchievementDisplay
import com.google.android.material.card.MaterialCardView

// Adapter for displaying quests in profile page.

class QuestsAdapter(
    private val onClick: (AchievementDisplay) -> Unit = {}
) : ListAdapter<AchievementDisplay, QuestsAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<AchievementDisplay>() {
        override fun areItemsTheSame(oldItem: AchievementDisplay, newItem: AchievementDisplay) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AchievementDisplay, newItem: AchievementDisplay) =
            oldItem == newItem
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view as MaterialCardView
        val icon: ImageView = view.findViewById(R.id.icon)
        val title: TextView = view.findViewById(R.id.title)
        val description: TextView = view.findViewById(R.id.description)
        val reward: TextView = view.findViewById(R.id.reward)
        val tick: ImageView = view.findViewById(R.id.tick)
        val layoutProgress: LinearLayout = view.findViewById(R.id.layoutProgress)
        val txtProgress: TextView = view.findViewById(R.id.txtProgress)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_quest, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val ctx = holder.itemView.context

        // Set quest icon (use medal as icon)
        holder.icon.setImageResource(item.medalRes)

        // Set quest title
        holder.title.text = item.title

        // Set quest description
        holder.description.text = item.description
        holder.description.visibility = View.VISIBLE

        if (item.achieved) {
            // Quest completed
            val bg = ContextCompat.getColor(ctx, R.color.QuestDone)
            holder.card.setCardBackgroundColor(bg)

            holder.reward.visibility = View.GONE
            holder.tick.visibility = View.VISIBLE
            holder.layoutProgress.visibility = View.GONE

        } else {
            // Quest active/incomplete
            val bg = ContextCompat.getColor(ctx, R.color.QuestTodo)
            holder.card.setCardBackgroundColor(bg)

            // Show gold reward
            holder.reward.text = "${item.goldReward}g"
            holder.reward.visibility = View.VISIBLE
            holder.tick.visibility = View.GONE

            // Show progress bar if this is a cumulative quest
            if (item.targetValue > 1) {
                holder.layoutProgress.visibility = View.VISIBLE

                // Progress text
                holder.txtProgress.text = "${item.progress} of ${item.targetValue} completed"

                // Progress bar percentage
                val percentage = ((item.progress.toFloat() / item.targetValue.toFloat()) * 100).toInt()
                holder.progressBar.progress = percentage

            } else {
                // One-time quest, no progress bar needed
                holder.layoutProgress.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }
}
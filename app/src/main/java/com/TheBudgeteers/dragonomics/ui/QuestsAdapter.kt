package com.TheBudgeteers.dragonomics.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.models.Quest
import com.google.android.material.card.MaterialCardView

class QuestsAdapter(
    private val onClick: (Quest) -> Unit = {}
) : ListAdapter<Quest, QuestsAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Quest>() {
        override fun areItemsTheSame(oldItem: Quest, newItem: Quest) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Quest, newItem: Quest) = oldItem == newItem
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view as MaterialCardView
        val icon: ImageView = view.findViewById(R.id.icon)
        val title: TextView = view.findViewById(R.id.title)
        val reward: TextView = view.findViewById(R.id.reward)
        val tick: ImageView = view.findViewById(R.id.tick)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_quest, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val ctx = holder.itemView.context

        // card tint by state
        val bg = ContextCompat.getColor(ctx, if (item.completed) R.color.QuestDone else R.color.QuestTodo)
        holder.card.setCardBackgroundColor(bg)

        // content
        holder.icon.setImageResource(item.iconRes)
        holder.title.text = item.title

        if (item.completed) {
            holder.reward.visibility = View.GONE
            holder.tick.visibility = View.VISIBLE
        } else {
            holder.reward.text = item.rewardText ?: ""
            holder.reward.visibility = View.VISIBLE
            holder.tick.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }
}
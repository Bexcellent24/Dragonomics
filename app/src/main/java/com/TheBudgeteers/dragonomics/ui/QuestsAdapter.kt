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


//Placeholder class, functionality won't be implemented until part 3
// Adapter for displaying quest items in a RecyclerView.
// Shows quest title, icon, reward, and completion status.
// Uses DiffUtil for efficient list updates when quest data changes.
// Completed quests show a checkmark, active quests show their reward.

class QuestsAdapter(
    private val onClick: (Quest) -> Unit = {}
) : ListAdapter<Quest, QuestsAdapter.VH>(Diff) {


    // begin code attribution
    // Data binding and DiffUtil pattern adapted from:
    // Android Developers guide to DiffUtil

    // DiffUtil helps RecyclerView know which items changed
    // so it only updates those items instead of the whole list
    object Diff : DiffUtil.ItemCallback<Quest>() {
        override fun areItemsTheSame(oldItem: Quest, newItem: Quest) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Quest, newItem: Quest) = oldItem == newItem
    }

    // end code attribution (Android Developers, 2020)


    // ViewHolder holds references to all the views in one quest card
    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view as MaterialCardView
        val icon: ImageView = view.findViewById(R.id.icon)
        val title: TextView = view.findViewById(R.id.title)
        val reward: TextView = view.findViewById(R.id.reward)
        val tick: ImageView = view.findViewById(R.id.tick)
    }

    // begin code attribution
    // RecyclerView.Adapter and ViewHolder pattern adapted from:
    // Android Developers guide to RecyclerView Adapters

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_quest, parent, false)
        return VH(v)
    }

    // end code attribution (Android Developers, 2020)


    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val ctx = holder.itemView.context

        // Change card background color based on completion status
        val bg = ContextCompat.getColor(ctx, if (item.completed) R.color.QuestDone else R.color.QuestTodo)
        holder.card.setCardBackgroundColor(bg)

        // Set quest icon and title
        holder.icon.setImageResource(item.iconRes)
        holder.title.text = item.title

        // Show checkmark for completed quests, show reward for active quests
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

// Android Developers, 2020. Create a List with RecyclerView. [online] Available at: <https://developer.android.com/guide/topics/ui/layout/recyclerview> [Accessed 3 October 2025]
// Android Developers, 2020. DiffUtil. [online] Available at: <https://developer.android.com/reference/androidx/recyclerview/widget/DiffUtil> [Accessed 3 October 2025]

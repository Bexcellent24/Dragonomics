package com.TheBudgeteers.dragonomics.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.viewmodel.AchievementDisplay


// Adapter for displaying achievements with progress tracking.
// Shows medal, title, description, progress, gold reward, and completion status.

class AchievementsAdapter : ListAdapter<AchievementDisplay, AchievementsAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<AchievementDisplay>() {
        override fun areItemsTheSame(old: AchievementDisplay, new: AchievementDisplay) =
            old.id == new.id

        override fun areContentsTheSame(old: AchievementDisplay, new: AchievementDisplay) =
            old == new
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imgMedal: ImageView = v.findViewById(R.id.imgMedal)
        val txtTitle: TextView = v.findViewById(R.id.txtTitle)
        val txtDesc: TextView = v.findViewById(R.id.txtDesc)
        val imgTick: ImageView = v.findViewById(R.id.imgTick)
    }

    // begin code attribution
    // Use of ListAdapter and DiffUtil.ItemCallback to optimise RecyclerView updates adapted from:
    // “Adapting to ListAdapter”
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return VH(v)
    }

    // end code attribution (Android Developers, 2020)

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = getItem(position)

        h.imgMedal.setImageResource(item.medalRes)
        h.txtTitle.text = item.title
        h.txtDesc.text = item.description

        h.imgTick.setImageResource(R.drawable.tick)
        h.imgTick.alpha = 1f

        h.itemView.alpha = 1f
    }
}

// Android Developers, 2020. Adapting to ListAdapter. [online] Available at: <https://medium.com/androiddevelopers/adapting-to-listadapter-341da4218f5b> [Accessed 4 November 2025]

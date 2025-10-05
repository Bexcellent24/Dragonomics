package com.TheBudgeteers.dragonomics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

//Placeholder class, functionality won't be implemented until part 3

class AchievementsAdapter(
    private var items: List<Achievement>
) : RecyclerView.Adapter<AchievementsAdapter.VH>() {


    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imgMedal: ImageView = v.findViewById(R.id.imgMedal)
        val txtTitle: TextView  = v.findViewById(R.id.txtTitle)
        val txtDesc: TextView   = v.findViewById(R.id.txtDesc)
        val imgTick: ImageView  = v.findViewById(R.id.imgTick)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val it = items[position]
        h.imgMedal.setImageResource(it.medalRes)
        h.txtTitle.text = it.title
        h.txtDesc.text  = it.description
        h.imgTick.setImageResource(
            if (it.achieved) R.drawable.tick else R.drawable.tick_dim
        )
        h.imgTick.alpha = if (it.achieved) 1f else 0.5f
    }

    override fun getItemCount() = items.size

    fun submit(newItems: List<Achievement>) {
        items = newItems
        notifyDataSetChanged()
    }
}

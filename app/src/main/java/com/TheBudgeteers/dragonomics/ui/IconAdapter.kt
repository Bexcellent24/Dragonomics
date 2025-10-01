package com.TheBudgeteers.dragonomics.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R

class IconAdapter(
    private val icons: List<String>,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<IconAdapter.IconVH>() {

    private var selectedPos = RecyclerView.NO_POSITION

    inner class IconVH(val view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_icon, parent, false)
        v.setBackgroundResource(R.drawable.icon_background)
        return IconVH(v)
    }

    override fun onBindViewHolder(holder: IconVH, position: Int) {
        val name = icons[position]
        val ctx = holder.itemView.context
        val resId = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        holder.img.setImageResource(if (resId != 0) resId else android.R.drawable.ic_menu_help)

        holder.view.isSelected = position == selectedPos

        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val previous = selectedPos
            selectedPos = currentPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPos)

            val clickedName = icons[currentPosition]
            onSelect(clickedName)
        }
    }


    override fun getItemCount() = icons.size
}

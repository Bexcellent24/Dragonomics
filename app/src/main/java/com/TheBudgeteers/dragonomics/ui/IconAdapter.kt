package com.TheBudgeteers.dragonomics.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R

// Adapter for displaying a list of icons in a RecyclerView.
// Handles selection highlighting and click callbacks.

class IconAdapter(
    private val icons: List<String>,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<IconAdapter.IconVH>() {

    private var selectedPos = RecyclerView.NO_POSITION

    // ViewHolder for icon items
    inner class IconVH(val view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon, parent, false)

        // set default background for icon container
        view.setBackgroundResource(R.drawable.icon_background)

        return IconVH(view)
    }

    override fun onBindViewHolder(holder: IconVH, position: Int) {
        val iconName = icons[position]
        val context = holder.itemView.context

        // Get drawable resource ID by name
        val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)

        // Set icon image or fallback icon
        holder.img.setImageResource(if (resId != 0) resId else android.R.drawable.ic_menu_help)

        // Highlight selection
        holder.view.isSelected = position == selectedPos

        // Handle icon click
        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val previousPosition = selectedPos
            selectedPos = currentPosition

            // Refresh previous and current selection states
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPos)

            val selectedIconName = icons[currentPosition]
            onSelect(selectedIconName)
        }
    }

    override fun getItemCount(): Int = icons.size
}

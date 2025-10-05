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

    // begin code attribution
    // Adapter and ViewHolder pattern adapted from:
    // Android Developers guide to RecyclerView usage
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon, parent, false)

        // set default background for icon container
        view.setBackgroundResource(R.drawable.icon_background)

        return IconVH(view)
    }
    // end code attribution (Android Developers, 2020)

    override fun onBindViewHolder(holder: IconVH, position: Int) {
        val iconName = icons[position]
        val context = holder.itemView.context

        // begin code attribution
        // Use of getIdentifier() for dynamic resource loading adapted from:
        // Android Developers Resources class documentation
        val resId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
        // end code attribution (Android Developers, 2020)

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

// reference list
// Android Developers, 2020. Create a List with RecyclerView. [online] Available at: <https://developer.android.com/develop/ui/views/layout/recyclerview> [Accessed 21 September 2025].
// Android Developers, 2020. Resources Class Documentation. [online] Available at: <https://developer.android.com/reference/android/content/res/Resources#getIdentifier(java.lang.String,%20java.lang.String,%20java.lang.String)> [Accessed 21 September 2025].

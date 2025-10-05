package com.TheBudgeteers.dragonomics.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R

// Adapter for displaying a list of colours as selectable items.
// Highlights the selected colour with a distinct stroke.
// Calls onSelect callback with the chosen colour hex.

class ColourAdapter(
    private val colours: List<String>,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<ColourAdapter.ColourVH>() {

    private var selectedPos = RecyclerView.NO_POSITION

    // ViewHolder for colour item
    inner class ColourVH(val view: View) : RecyclerView.ViewHolder(view)

    // begin code attribution
    // Adapter pattern and ViewHolder implementation adapted from:
    // Android Developers guide to RecyclerView Adapters and ViewHolders
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColourVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_colour, parent, false)
        return ColourVH(v)
    }
    // end code attribution (Android Developers, 2020)

    override fun onBindViewHolder(holder: ColourVH, position: Int) {
        val hex = colours[position]

        // begin code attribution
        // Use of GradientDrawable for dynamic colour UI adapted from:
        // Android Developers guide to Drawables overview
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f
            try {
                setColor(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {
                setColor(android.graphics.Color.GRAY)
            }

            setStroke(
                3,
                if (position == selectedPos)
                    holder.view.context.getColor(R.color.GoldenEmber)
                else
                    holder.view.context.getColor(R.color.FireBurst)
            )
        }
        // end code attribution (Android Developers, 2020)

        holder.view.background = drawable

        holder.view.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val previous = selectedPos
            selectedPos = currentPosition

            notifyItemChanged(previous)
            notifyItemChanged(selectedPos)

            onSelect(colours[currentPosition])
        }
    }

    override fun getItemCount() = colours.size
}

// reference list
// Android Developers, 2020. Create a List with RecyclerView. [online] Available at: <https://developer.android.com/develop/ui/views/layout/recyclerview> [Accessed 21 September 2025].
// Android Developers, 2020. Drawables overview. [online] Available at: <https://developer.android.com/guide/topics/graphics/drawables> [Accessed 21 September 2025].

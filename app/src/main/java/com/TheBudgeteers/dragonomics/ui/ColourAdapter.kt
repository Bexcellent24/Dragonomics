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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColourVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_colour, parent, false)
        return ColourVH(v)
    }

    override fun onBindViewHolder(holder: ColourVH, position: Int) {
        val hex = colours[position]

        // Create a rounded rectangle background for colour item
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f // rounded corners
            try {
                setColor(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {
                setColor(android.graphics.Color.GRAY) // fallback colour
            }

            // Stroke: golden if selected, FireBurst if not
            setStroke(
                3, // stroke width in px
                if (position == selectedPos)
                    holder.view.context.getColor(R.color.GoldenEmber)
                else
                    holder.view.context.getColor(R.color.FireBurst)
            )
        }

        // Apply background drawable
        holder.view.background = drawable

        // Handle click selection
        holder.view.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val previous = selectedPos
            selectedPos = currentPosition

            // Refresh previous and current selection visuals
            notifyItemChanged(previous)
            notifyItemChanged(selectedPos)

            // Trigger callback with selected colour hex
            onSelect(colours[currentPosition])
        }
    }

    override fun getItemCount() = colours.size
}

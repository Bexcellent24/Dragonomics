package com.TheBudgeteers.dragonomics.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R

class ColourAdapter(
    private val colours: List<String>,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<ColourAdapter.ColourVH>() {

    private var selectedPos = RecyclerView.NO_POSITION

    inner class ColourVH(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColourVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_colour, parent, false)
        return ColourVH(v)
    }

    override fun onBindViewHolder(holder: ColourVH, position: Int) {
        val hex = colours[position]

        // Create a rounded rectangle background
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f // rounded corners
            try {
                setColor(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {
                setColor(android.graphics.Color.GRAY)
            }
            // Stroke: golden if selected, FireBurst if not
            setStroke(
                3, // stroke width in px
                if (position == selectedPos) holder.view.context.getColor(R.color.GoldenEmber)
                else holder.view.context.getColor(R.color.FireBurst)
            )
        }

        // Apply to background
        holder.view.background = drawable

        holder.view.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val previous = selectedPos
            selectedPos = currentPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPos)

            val clickedColour = colours[currentPosition]
            onSelect(clickedColour)
        }
    }




    override fun getItemCount() = colours.size
}

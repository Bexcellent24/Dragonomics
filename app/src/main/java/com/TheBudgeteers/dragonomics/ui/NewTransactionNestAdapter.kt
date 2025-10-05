package com.TheBudgeteers.dragonomics.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.models.Nest


// Adapter for displaying a grid of nests (categories) when creating a new transaction.
// Allows the user to select one category and highlights the selected one.
// categories List of available categories (nests) to display.
// onSelect Callback triggered when a category is selected.


class NewTransactionNestAdapter(
    private val categories: List<Nest>,
    private val onSelect: (Nest) -> Unit
) : RecyclerView.Adapter<NewTransactionNestAdapter.CategoryVH>() {

    // Tracks currently selected position
    private var selectedPos = RecyclerView.NO_POSITION


     // ViewHolder for category item.
     // Holds reference to icon ImageView.
    inner class CategoryVH(val view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgIcon)
    }

    // Inflates the category item view
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_nest, parent, false)
        return CategoryVH(v)
    }

    // begin code attribution
    // Data binding and click handling adapted from:
    // Android Developers guide to RecyclerView Adapters

    //Binds category data to the ViewHolder
    override fun onBindViewHolder(holder: CategoryVH, position: Int) {
        val category = categories[position]

        // Load icon drawable by name
        holder.imgIcon.setImageResource(
            holder.itemView.context.resources.getIdentifier(
                category.icon, "drawable", holder.itemView.context.packageName
            )
        )

        // Highlight selected item
        holder.view.isSelected = position == selectedPos

        // Handle selection click
        holder.view.setOnClickListener {
            val prev = selectedPos
            selectedPos = holder.adapterPosition

            // Refresh previous and new selected positions
            notifyItemChanged(prev)
            notifyItemChanged(selectedPos)

            onSelect(category) // Trigger callback with selected category
        }
    }

    // end code attribution (Android Developers, 2020)

    override fun getItemCount() = categories.size
}

// Android Developers, 2020. Create a List with RecyclerView. [online] Available at: <https://developer.android.com/guide/topics/ui/layout/recyclerview> [Accessed 28 September 2025]


package com.TheBudgeteers.dragonomics.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.models.Nest


class NewTransactionNestAdapter(
    private val categories: List<Nest>,
    private val onSelect: (Nest) -> Unit
) : RecyclerView.Adapter<NewTransactionNestAdapter.CategoryVH>() {

    private var selectedPos = RecyclerView.NO_POSITION

    inner class CategoryVH(val view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction_nest, parent, false)
        return CategoryVH(v)
    }

    override fun onBindViewHolder(holder: CategoryVH, position: Int) {
        val category = categories[position]
        holder.imgIcon.setImageResource(holder.itemView.context.resources.getIdentifier(
            category.icon, "drawable", holder.itemView.context.packageName))

        holder.view.isSelected = position == selectedPos
        holder.view.setOnClickListener {
            val prev = selectedPos
            selectedPos = holder.adapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedPos)
            onSelect(category)
        }
    }

    override fun getItemCount() = categories.size
}

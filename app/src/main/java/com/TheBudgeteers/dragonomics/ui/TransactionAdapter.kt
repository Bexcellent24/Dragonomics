package com.TheBudgeteers.dragonomics.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.models.TransactionWithNest
import java.text.SimpleDateFormat
import java.util.Locale

// Adapter that displays transaction items in a RecyclerView list.
// Shows transaction title, date, amount, and category icon.
// Colors the amount box green for income, red for expenses.
// Updates the entire list when new transaction data comes in.

class TransactionAdapter(
    private var transactions: List<TransactionWithNest>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    companion object {
        private const val INCOME_COLOR = "#3E8644"
        private const val EXPENSE_COLOR = "#9C2124"
    }

    // ViewHolder stores references to views for one transaction item
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.imgCategoryIcon)
        val txtTitle: TextView = itemView.findViewById(R.id.txtTransactionTitle)
        val txtDate: TextView = itemView.findViewById(R.id.txtTransactionDate)
        val txtAmount: TextView = itemView.findViewById(R.id.txtTransactionAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = transactions[position]

        bindTransactionDetails(holder, item)
        bindCategoryIcon(holder, item)
        bindAmountColor(holder, item)
    }


    private fun bindTransactionDetails(holder: TransactionViewHolder, item: TransactionWithNest) {
        holder.txtTitle.text = item.transaction.title
        holder.txtAmount.text = "R%.2f".format(item.transaction.amount)
        holder.txtDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(item.transaction.date)
    }

    private fun bindCategoryIcon(holder: TransactionViewHolder, item: TransactionWithNest) {
        val iconRes = getIconResource(holder.itemView.context, item.categoryNest.icon)
        holder.imgIcon.setImageResource(iconRes)
    }

    private fun bindAmountColor(holder: TransactionViewHolder, item: TransactionWithNest) {
        // Color code: green for income, red for expenses
        val bg = holder.txtAmount.background as GradientDrawable
        val color = if (item.categoryNest.type == NestType.INCOME) {
            Color.parseColor(INCOME_COLOR)
        } else {
            Color.parseColor(EXPENSE_COLOR)
        }
        bg.setColor(color)
    }


    // Find drawable resource by name, or use default icon if not found
    private fun getIconResource(context: Context, iconName: String): Int {
        return context.resources.getIdentifier(iconName, "drawable", context.packageName)
            .takeIf { it != 0 } ?: R.drawable.ic_default
    }

    // Called when new data arrives - refreshes the entire list
    fun updateData(newTransactions: List<TransactionWithNest>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun getItemCount() = transactions.size
}
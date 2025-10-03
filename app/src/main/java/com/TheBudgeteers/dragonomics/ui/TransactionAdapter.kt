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
import com.TheBudgeteers.dragonomics.models.Transaction
import com.TheBudgeteers.dragonomics.models.TransactionWithNest
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private var transactions: List<TransactionWithNest>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

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

        holder.txtTitle.text = item.transaction.title
        holder.txtAmount.text = "R%.2f".format(item.transaction.amount)
        holder.txtDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(item.transaction.date)

        // Load category icon
        holder.imgIcon.setImageResource(getIconResource(holder.itemView.context, item.categoryNest.icon))

        // Colour amount box: green for income, red for expense
        val bg = holder.txtAmount.background as GradientDrawable
        bg.setColor(if (item.categoryNest.type == NestType.INCOME) Color.parseColor("#3E8644") else Color.parseColor("#9C2124"))

    }


    override fun getItemCount() = transactions.size

    private fun getIconResource(context: Context, iconName: String): Int {
        return context.resources.getIdentifier(iconName, "drawable", context.packageName).takeIf { it != 0 }
            ?: R.drawable.ic_default // fallback icon
    }

    fun updateData(newTransactions: List<TransactionWithNest>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}

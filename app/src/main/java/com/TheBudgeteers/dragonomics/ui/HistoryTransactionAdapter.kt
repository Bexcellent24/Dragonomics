package com.TheBudgeteers.dragonomics.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.data.HistoryListItem
import com.TheBudgeteers.dragonomics.models.NestType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Adapter for transaction history list with date headers.
// Handles two view types, date headers and transactions.
// Allows clicking on transaction photos to open them.

class HistoryTransactionsAdapter(
    private var items: List<HistoryListItem>,
    private val onPhotoClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TRANSACTION = 1
    }

    // ViewHolder for date headers
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtHeader: TextView = itemView.findViewById(R.id.txtDateHeader)
    }

    // ViewHolder for transaction items
    inner class HistoryTransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFromCategory: ImageView = itemView.findViewById(R.id.imgFromCategory)
        val imgCategoryIcon: ImageView = itemView.findViewById(R.id.imgCategoryIcon)
        val arrow: ImageView = itemView.findViewById(R.id.imgArrow)
        val txtTitle: TextView = itemView.findViewById(R.id.txtTransactionTitle)
        val txtDescription: TextView = itemView.findViewById(R.id.txtTransactionDescription)
        val imgPhotoIcon: ImageView = itemView.findViewById(R.id.imgPhotoIcon)
        val txtAmount: TextView = itemView.findViewById(R.id.txtTransactionAmount)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HistoryListItem.Header -> TYPE_HEADER
            is HistoryListItem.TransactionItem -> TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_date_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_transaction, parent, false)
            HistoryTransactionViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {

            // Bind date header
            is HistoryListItem.Header -> {
                val headerHolder = holder as HeaderViewHolder
                val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                headerHolder.txtHeader.text = dateFormat.format(Date(item.dateMillis))
            }

            // Bind transaction item
            is HistoryListItem.TransactionItem -> {
                val transactionHolder = holder as HistoryTransactionViewHolder
                val transaction = item.transactionWithNest

                transactionHolder.txtTitle.text = transaction.transaction.title
                transactionHolder.txtDescription.text = transaction.transaction.description ?: ""

                // Load category icon
                transactionHolder.imgCategoryIcon.setImageResource(
                    getIconResource(transactionHolder.itemView.context, transaction.categoryNest.icon)
                )

                // Show or hide "from category" icon and arrow based on fromCategoryId
                if (transaction.transaction.fromCategoryId != null && transaction.fromNest != null) {
                    transactionHolder.imgFromCategory.visibility = View.VISIBLE
                    transactionHolder.imgFromCategory.setImageResource(
                        getIconResource(transactionHolder.itemView.context, transaction.fromNest.icon)
                    )
                    transactionHolder.arrow.visibility = View.VISIBLE
                } else {
                    transactionHolder.imgFromCategory.visibility = View.GONE
                    transactionHolder.arrow.visibility = View.GONE
                }

                // Show photo icon if transaction has a photo
                if (!transaction.transaction.photoPath.isNullOrEmpty()) {
                    transactionHolder.imgPhotoIcon.visibility = View.VISIBLE
                    transactionHolder.imgPhotoIcon.setOnClickListener {
                        onPhotoClick(transaction.transaction.photoPath!!)
                    }
                } else {
                    transactionHolder.imgPhotoIcon.visibility = View.GONE
                }

                // Format amount with + for income, - for expense
                val sign = if (transaction.categoryNest.type == NestType.INCOME) "+" else "-"
                transactionHolder.txtAmount.text = "$sign R${transaction.transaction.amount.toInt()}"
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // Utility to get drawable resource ID from icon name
    private fun getIconResource(context: Context, categoryId: String): Int {
        return context.resources.getIdentifier(categoryId, "drawable", context.packageName)
            .takeIf { it != 0 } ?: R.drawable.ic_default
    }

    // Replace adapter data and refresh list
    fun updateData(newItems: List<HistoryListItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

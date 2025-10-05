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

    // begin code attribution
    // ViewHolder pattern and multiple view type usage adapted from:
    // Android Developers guide to RecyclerView
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HistoryListItem.Header -> TYPE_HEADER
            is HistoryListItem.TransactionItem -> TYPE_TRANSACTION
        }
    }
    // end code attribution (Android Developers, 2020)

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

            is HistoryListItem.Header -> {
                val headerHolder = holder as HeaderViewHolder

                // begin code attribution
                // SimpleDateFormat usage adapted from:
                // Android Developers guide to formatting dates in Java/Kotlin
                val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                headerHolder.txtHeader.text = dateFormat.format(Date(item.dateMillis))
                // end code attribution (Android Developers, 2020)
            }

            is HistoryListItem.TransactionItem -> {
                val transactionHolder = holder as HistoryTransactionViewHolder
                val transaction = item.transactionWithNest

                transactionHolder.txtTitle.text = transaction.transaction.title
                transactionHolder.txtDescription.text = transaction.transaction.description ?: ""

                transactionHolder.imgCategoryIcon.setImageResource(
                    getIconResource(transactionHolder.itemView.context, transaction.categoryNest.icon)
                )

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

                if (!transaction.transaction.photoPath.isNullOrEmpty()) {
                    transactionHolder.imgPhotoIcon.visibility = View.VISIBLE
                    transactionHolder.imgPhotoIcon.setOnClickListener {
                        onPhotoClick(transaction.transaction.photoPath!!)
                    }
                } else {
                    transactionHolder.imgPhotoIcon.visibility = View.GONE
                }

                val sign = if (transaction.categoryNest.type == NestType.INCOME) "+" else "-"
                transactionHolder.txtAmount.text = "$sign R${transaction.transaction.amount.toInt()}"
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // begin code attribution
    // Dynamic resource loading with getIdentifier() adapted from:
    // Android Developers Resources class documentation
    private fun getIconResource(context: Context, categoryId: String): Int {
        return context.resources.getIdentifier(categoryId, "drawable", context.packageName)
            .takeIf { it != 0 } ?: R.drawable.ic_default
    }
    // end code attribution (Android Developers, 2020)

    fun updateData(newItems: List<HistoryListItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

// reference list
// Android Developers, 2020. Create a List with RecyclerView. [online] Available at: <https://developer.android.com/develop/ui/views/layout/recyclerview> [Accessed 23 September 2025].
// Android Developers, 2020. Formatting Dates with SimpleDateFormat. [online] Available at: <https://developer.android.com/reference/java/text/SimpleDateFormat> [Accessed 23 September 2025]
// Android Developers, 2020. Resources Class Documentation. [online] Available at: <https://developer.android.com/reference/android/content/res/Resources#getIdentifier(java.lang.String,%20java.lang.String,%20java.lang.String)> [Accessed 23 September 2025].

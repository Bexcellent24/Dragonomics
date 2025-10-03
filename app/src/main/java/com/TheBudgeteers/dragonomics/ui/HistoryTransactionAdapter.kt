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

class HistoryTransactionsAdapter(
    private var items: List<HistoryListItem>,
    private val onPhotoClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TRANSACTION = 1
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtHeader: TextView = itemView.findViewById(R.id.txtDateHeader)
    }

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
            is HistoryListItem.Header -> {
                val headerHolder = holder as HeaderViewHolder
                val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
                headerHolder.txtHeader.text = dateFormat.format(Date(item.dateMillis))
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

    private fun getIconResource(context: Context, categoryId: String): Int {
        return context.resources.getIdentifier(categoryId, "drawable", context.packageName)
            .takeIf { it != 0 } ?: R.drawable.ic_default
    }

    fun updateData(newItems: List<HistoryListItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}


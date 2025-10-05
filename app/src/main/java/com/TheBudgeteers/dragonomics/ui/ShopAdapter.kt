package com.TheBudgeteers.dragonomics.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.models.ShopItem
import com.google.android.material.button.MaterialButton

// Adapter for the shop items in the dragon customization shop.
// Shows preview images, prices, and buy/equip buttons.
// Handles three states: not owned (buy), owned (equip), equipped (already wearing).
// Uses DiffUtil to efficiently update the list when items change.

class ShopAdapter(
    private val onAction: (ShopItem) -> Unit
) : ListAdapter<ShopItem, ShopAdapter.VH>(DIFF) {

    companion object {
        // DiffUtil compares old and new lists to figure out what changed
        private val DIFF = object : DiffUtil.ItemCallback<ShopItem>() {
            override fun areItemsTheSame(a: ShopItem, b: ShopItem) = a.id == b.id
            override fun areContentsTheSame(a: ShopItem, b: ShopItem) = a == b
        }
    }

    // ViewHolder stores references to all views in one shop item card
    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val price: TextView = v.findViewById(R.id.price)
        val priceChip: View = v.findViewById(R.id.priceChip)
        val btn: MaterialButton = v.findViewById(R.id.actionBtn)
        val preview: ImageView = v.findViewById(R.id.preview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = getItem(position)
        h.title.text = item.name
        h.preview.setImageResource(item.previewRes)

        // Hide price if user already owns or has equipped this item
        val owned = item.owned || item.equipped
        h.priceChip.visibility = if (owned) View.GONE else View.VISIBLE
        h.price.text = item.price.toString()

        // Button text changes based on item state:
        // "Buy" if not owned, "Equip" if owned, "Equipped" if currently wearing
        h.btn.apply {
            text = when {
                item.equipped -> "Equipped"
                item.owned    -> "Equip"
                else          -> "Buy"
            }
            // Can't click if already equipped
            isEnabled = !item.equipped
            setOnClickListener { onAction(item) }
        }
    }
}

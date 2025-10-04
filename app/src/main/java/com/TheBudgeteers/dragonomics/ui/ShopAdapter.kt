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

class ShopAdapter(
    private val onAction: (ShopItem) -> Unit
) : ListAdapter<ShopItem, ShopAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ShopItem>() {
            override fun areItemsTheSame(a: ShopItem, b: ShopItem) = a.id == b.id
            override fun areContentsTheSame(a: ShopItem, b: ShopItem) = a == b
        }
    }

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

        val owned = item.owned || item.equipped
        h.priceChip.visibility = if (owned) View.INVISIBLE else View.VISIBLE
        h.price.text = item.price.toString()

        h.btn.apply {
            text = when {
                item.equipped -> "Equipped"
                item.owned    -> "Equip"
                else          -> "Buy"
            }
            isEnabled = !item.equipped  // can't click "Equipped"
            setOnClickListener { onAction(item) }
        }
    }
}
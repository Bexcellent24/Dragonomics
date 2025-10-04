package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.models.ShopItem
import com.TheBudgeteers.dragonomics.models.ShopTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShopState(
    val currency: Int = 0,
    val hornsItems: List<ShopItem> = emptyList(),
    val wingsItems: List<ShopItem> = emptyList(),
    val paletteItems: List<ShopItem> = emptyList(),
    val currentTab: ShopTab = ShopTab.PALETTE,
    val purchaseResult: PurchaseResult? = null
)

sealed class PurchaseResult {
    object Success : PurchaseResult()
    object InsufficientFunds : PurchaseResult()
}

class ShopViewModel : ViewModel() {

    private val _state = MutableStateFlow(ShopState())
    val state: StateFlow<ShopState> = _state.asStateFlow()

    init {
        loadShopItems()
        loadCurrency()
    }

    private fun loadShopItems() {
        viewModelScope.launch {
            val horns = listOf(
                ShopItem("horns_twisted", "Twisted Horns", 90, previewRes = R.drawable.placeholder_item),
                ShopItem("horns_curly", "Curly Horns", 90, previewRes = R.drawable.placeholder_item),
                ShopItem("horns_chipped", "Chipped Horns", 0, owned = true, previewRes = R.drawable.placeholder_item),
                ShopItem("horns_straight", "Straight Horns", 90, previewRes = R.drawable.placeholder_item)
            )

            val wings = listOf(
                ShopItem("wings_bat", "Bat Wings", 120, previewRes = R.drawable.placeholder_item),
                ShopItem("wings_feather", "Feathered", 150, previewRes = R.drawable.placeholder_item),
                ShopItem("wings_ragged", "Ragged", 60, owned = true, previewRes = R.drawable.placeholder_item),
                ShopItem("wings_royal", "Royal Wings", 200, previewRes = R.drawable.placeholder_item)
            )

            val palette = listOf(
                ShopItem("pal_forest", "Forest Scheme", 40, previewRes = R.drawable.placeholder_item),
                ShopItem("pal_crimson", "Crimson Scheme", 60, previewRes = R.drawable.placeholder_item),
                ShopItem("pal_ember", "Ember Scheme", 0, owned = true, previewRes = R.drawable.placeholder_item),
                ShopItem("pal_ice", "Ice Scheme", 50, previewRes = R.drawable.placeholder_item)
            )

            _state.value = _state.value.copy(
                hornsItems = horns,
                wingsItems = wings,
                paletteItems = palette
            )
        }
    }

    private fun loadCurrency() {
        viewModelScope.launch {
            // TODO: Load from Repository/DataStore instead of hard-coding
            _state.value = _state.value.copy(currency = 0)
        }
    }

    fun setCurrentTab(tab: ShopTab) {
        _state.value = _state.value.copy(currentTab = tab)
    }

    fun getCurrentItems(): List<ShopItem> {
        return when (_state.value.currentTab) {
            ShopTab.PALETTE -> _state.value.paletteItems
            ShopTab.HORNS -> _state.value.hornsItems
            ShopTab.WINGS -> _state.value.wingsItems
        }
    }

    fun handleItemAction(item: ShopItem) {
        val currentList = getCurrentItems().toMutableList()
        val idx = currentList.indexOfFirst { it.id == item.id }
        if (idx == -1) return

        val current = currentList[idx]

        when {
            current.equipped -> return // Already equipped, do nothing

            current.owned -> {
                // Equip this item, unequip others in the same category
                equipItem(currentList, idx)
            }

            else -> {
                // Try to purchase
                purchaseItem(currentList, idx, current)
            }
        }
    }

    private fun equipItem(list: MutableList<ShopItem>, index: Int) {
        // Unequip all items in this category
        for (i in list.indices) {
            list[i] = list[i].copy(equipped = false)
        }
        // Equip the selected item
        list[index] = list[index].copy(equipped = true)

        updateItemList(list)
    }

    private fun purchaseItem(list: MutableList<ShopItem>, index: Int, item: ShopItem) {
        if (_state.value.currency >= item.price) {
            // Purchase successful
            val newCurrency = _state.value.currency - item.price
            list[index] = item.copy(owned = true)

            _state.value = _state.value.copy(
                currency = newCurrency,
                purchaseResult = PurchaseResult.Success
            )
            updateItemList(list)

            // TODO: Persist currency to Repository/DataStore
        } else {
            // Not enough funds
            _state.value = _state.value.copy(
                purchaseResult = PurchaseResult.InsufficientFunds
            )
        }
    }

    private fun updateItemList(list: List<ShopItem>) {
        val tab = _state.value.currentTab
        _state.value = when (tab) {
            ShopTab.PALETTE -> _state.value.copy(paletteItems = list)
            ShopTab.HORNS -> _state.value.copy(hornsItems = list)
            ShopTab.WINGS -> _state.value.copy(wingsItems = list)
        }
    }

    fun clearPurchaseResult() {
        _state.value = _state.value.copy(purchaseResult = null)
    }

    fun addCurrency(amount: Int) {
        val newAmount = (_state.value.currency + amount).coerceAtLeast(0)
        _state.value = _state.value.copy(currency = newAmount)
        // TODO: Persist to Repository/DataStore
    }
}
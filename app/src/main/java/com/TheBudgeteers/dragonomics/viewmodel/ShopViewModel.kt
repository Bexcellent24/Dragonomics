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

// interface for the home kt. this is to get the string and Id
interface AccessoryEquipListener {
    // accessoryType will be like  "horns", "wings", or "palette"
    // itemId will be the unique ID, like "horns_chipped"
    fun onAccessoryEquipped(accessoryType: String, itemId: String)
}

// Data class defining the structure of the Shop's UI state.
data class ShopState(
    val currency: Int = 0,
    val hornsItems: List<ShopItem> = emptyList(),
    val wingsItems: List<ShopItem> = emptyList(),
    val paletteItems: List<ShopItem> = emptyList(),
    val currentTab: ShopTab = ShopTab.PALETTE,
    val purchaseResult: PurchaseResult? = null
)

// Sealed class to represent the outcome of a purchase attempt.
sealed class PurchaseResult {
    object Success : PurchaseResult()
    object InsufficientFunds : PurchaseResult()
}

class ShopViewModel : ViewModel() {

    // Listener will be set by HomeActivity in initializeViewModels
    private var equipListener: AccessoryEquipListener? = null

    private val _state = MutableStateFlow(ShopState())
    val state: StateFlow<ShopState> = _state.asStateFlow()

    init {
        loadShopItems()
        loadCurrency()
    }

    // Setter for the listener (called by HomeActivity)
    public fun setEquipListener(listener: AccessoryEquipListener) {
        this.equipListener = listener
    }

    private fun loadShopItems() {
        viewModelScope.launch {
            // NOTE: Using placeholder items and resources
            // IMPORTANT: Equipped status needs to be tracked.
            val horns = listOf(
                ShopItem("horns_twisted", "Twisted Horns", 90, previewRes = R.drawable.placeholder_item, equipped = false),
                ShopItem("horns_curly", "Curly Horns", 90, previewRes = R.drawable.placeholder_item, equipped = false),
                ShopItem("horns_chipped", "Chipped Horns", 0, owned = true, equipped = true, previewRes = R.drawable.placeholder_item), // Default equipped

            )

            val wings = listOf(
                ShopItem("wings_bat", "Bat Wings", 120, previewRes = R.drawable.placeholder_item, equipped = false),
                ShopItem("wings_feather", "Feathered", 150, previewRes = R.drawable.placeholder_item, equipped = false),
                ShopItem("wings_ragged", "Ragged", 60, owned = true, equipped = true, previewRes = R.drawable.placeholder_item), // Default equipped

            )

            val palette = listOf(
                ShopItem("pal_forest", "Forest Scheme", 40, previewRes = R.drawable.placeholder_item, equipped = false),
                ShopItem("pal_crimson", "Crimson Scheme", 60, previewRes = R.drawable.placeholder_item, equipped = false),
                ShopItem("pal_ember", "Ember Scheme", 0, owned = true, equipped = true, previewRes = R.drawable.placeholder_item), // Default equipped
                ShopItem("pal_ice", "Ice Scheme", 50, previewRes = R.drawable.placeholder_item, equipped = false)
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
            _state.value = _state.value.copy(currency = 500) // Reset currency to 500 for easier testing
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
        val itemToEquip = list[index]

        // Unequip all items in this category
        for (i in list.indices) {
            list[i] = list[i].copy(equipped = false)
        }
        // Equip the selected item
        list[index] = itemToEquip.copy(equipped = true)

        updateItemList(list)

        // NOTIFY DRAGON VIEW: Tell the activity which item was equipped
        val accessoryType = when(_state.value.currentTab) {
            ShopTab.HORNS -> "horns"
            ShopTab.WINGS -> "wings"
            else -> "palette" // Palette will change the dragonImageRes, but we still notify
        }
        // This call updates the DragonViewModel which in turn updates HomeActivity
        equipListener?.onAccessoryEquipped(accessoryType, itemToEquip.id)
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
    }
}
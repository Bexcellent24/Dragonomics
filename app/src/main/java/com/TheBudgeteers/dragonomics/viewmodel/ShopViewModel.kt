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


// ShopViewModel manages the in-app shop where users buy dragon accessories
// Handles three categories: horns, wings, and color palettes
// Manages currency, purchases, and equipped items
// Communicates with HomeActivity to update the dragon's appearance

//------------CODE ATTRIBUTION------------
//Title: Interfaces
//Author: JetBrains
//Date: 05/10/2025
//Code Version:(N/A)
//Availability: https://kotlinlang.org/docs/interfaces.html#jvm-default-method-generation-for-interface-functions
interface AccessoryEquipListener {
    // Listener interface for notifying when accessories are equipped
// HomeActivity implements this to update the dragon view
    fun onAccessoryEquipped(accessoryType: String, itemId: String)
}
//---------END OF CODE ATTRIBUTION--------- ( Jetbrains, 2025)

// Complete shop state container
data class ShopState(
    val currency: Int = 0,                              // User's current currency
    val hornsItems: List<ShopItem> = emptyList(),       // Available horn styles
    val wingsItems: List<ShopItem> = emptyList(),       // Available wing styles
    val paletteItems: List<ShopItem> = emptyList(),     // Available color schemes
    val currentTab: ShopTab = ShopTab.PALETTE,          // Active shop tab
    val purchaseResult: PurchaseResult? = null          // Result of last purchase attempt
)



// Outcome of a purchase attempt
sealed class PurchaseResult {
    object Success : PurchaseResult()
    object InsufficientFunds : PurchaseResult()
}

class ShopViewModel : ViewModel() {


    // Listener for communicating equipped items to the dragon view
    private var equipListener: AccessoryEquipListener? = null

    private val _state = MutableStateFlow(ShopState())
    val state: StateFlow<ShopState> = _state.asStateFlow()

    init {
        loadShopItems()
        loadCurrency()
    }

    // Set the listener (called by HomeActivity during initialization)
    fun setEquipListener(listener: AccessoryEquipListener) {
        this.equipListener = listener
    }


    // Load all available shop items
    // TODO: This should eventually come from a database or config file
    private fun loadShopItems() {
        viewModelScope.launch {
            // Horn items - different horn styles for the dragon
            val horns = listOf(
                ShopItem(
                    "horns_twisted",
                    "Twisted Horns",
                    90,
                    previewRes = R.drawable.placeholder_item,
                    equipped = false
                ),
                ShopItem(
                    "horns_curly",
                    "Curly Horns",
                    90,
                    previewRes = R.drawable.placeholder_item,
                    equipped = false
                ),
                ShopItem(
                    "horns_chipped",
                    "Chipped Horns",
                    0,
                    owned = true,
                    equipped = true,
                    previewRes = R.drawable.placeholder_item
                ) // Default equipped item
            )

            // Wing items - different wing styles for the dragon
            val wings = listOf(
                ShopItem(
                    "wings_bat",
                    "Bat Wings",
                    120,
                    previewRes = R.drawable.placeholder_item,
                    equipped = false
                ),
                ShopItem(
                    "wings_feather",
                    "Feathered",
                    150,
                    previewRes = R.drawable.placeholder_item,
                    equipped = false
                ),
                ShopItem(
                    "wings_ragged",
                    "Ragged",
                    60,
                    owned = true,
                    equipped = true,
                    previewRes = R.drawable.placeholder_item
                ) // Default equipped item
            )

            // Palette items - different color schemes for the dragon
            val palette = listOf(
                ShopItem(
                    "pal_forest",
                    "Forest Scheme",
                    40,
                    previewRes = R.drawable.placeholder_item,
                    equipped = false
                ),
                ShopItem(
                    "pal_crimson",
                    "Crimson Scheme",
                    60,
                    previewRes = R.drawable.placeholder_item,
                    equipped = false
                ),
                ShopItem(
                    "pal_ember",
                    "Ember Scheme",
                    0,
                    owned = true,
                    equipped = true,
                    previewRes = R.drawable.placeholder_item
                ), // Default equipped item
                ShopItem(
                    "pal_ice",
                    "Ice Scheme",
                    50,
                    previewRes = R.drawable.placeholder_item,
                    equipped = false
                )
            )

            _state.value = _state.value.copy(
                hornsItems = horns,
                wingsItems = wings,
                paletteItems = palette
            )
        }
    }

    // Load user's currency balance
    private fun loadCurrency() {
        viewModelScope.launch {
            _state.value = _state.value.copy(currency = 500)
        }
    }


    // Switch between shop categories
    fun setCurrentTab(tab: ShopTab) {
        _state.value = _state.value.copy(currentTab = tab)
    }

    // Get items for the currently active tab
    fun getCurrentItems(): List<ShopItem> {
        return when (_state.value.currentTab) {
            ShopTab.PALETTE -> _state.value.paletteItems
            ShopTab.HORNS -> _state.value.hornsItems
            ShopTab.WINGS -> _state.value.wingsItems
        }
    }


    // begin code attribution
    // When expression pattern adapted from:
    // Kotlin Documentation: When expression

    // Handle user clicking on a shop item
    // Logic: Already equipped -> do nothing
    //        Owned -> equip it
    //        Not owned -> try to purchase
    fun handleItemAction(item: ShopItem) {
        val currentList = getCurrentItems().toMutableList()
        val idx = currentList.indexOfFirst { it.id == item.id }
        if (idx == -1) return

        val current = currentList[idx]

        when {
            current.equipped -> return // Already wearing it

            current.owned -> {
                // User owns it, so equip it
                equipItem(currentList, idx)
            }

            else -> {
                // User doesn't own it, try to buy it
                purchaseItem(currentList, idx, current)
            }
        }
    }

    // end code attribution (Kotlin Documentation, 2020)

    // Equip an item and unequip all others in the same category
    private fun equipItem(list: MutableList<ShopItem>, index: Int) {
        val itemToEquip = list[index]

        // Unequip everything else in this category
        for (i in list.indices) {
            list[i] = list[i].copy(equipped = false)
        }
        // Equip the selected item
        list[index] = itemToEquip.copy(equipped = true)

        updateItemList(list)

        // Notify the dragon view to update its appearance
        val accessoryType = when(_state.value.currentTab) {
            ShopTab.HORNS -> "horns"
            ShopTab.WINGS -> "wings"
            else -> "palette"
        }
        equipListener?.onAccessoryEquipped(accessoryType, itemToEquip.id)
    }


    // Attempt to purchase an item
    private fun purchaseItem(list: MutableList<ShopItem>, index: Int, item: ShopItem) {
        if (_state.value.currency >= item.price) {
            // User has enough money
            val newCurrency = _state.value.currency - item.price
            list[index] = item.copy(owned = true)

            _state.value = _state.value.copy(
                currency = newCurrency,
                purchaseResult = PurchaseResult.Success
            )
            updateItemList(list)

        } else {
            // Not enough funds
            _state.value = _state.value.copy(
                purchaseResult = PurchaseResult.InsufficientFunds
            )
        }
    }

    // Update the item list for the current tab
    private fun updateItemList(list: List<ShopItem>) {
        val tab = _state.value.currentTab
        _state.value = when (tab) {
            ShopTab.PALETTE -> _state.value.copy(paletteItems = list)
            ShopTab.HORNS -> _state.value.copy(hornsItems = list)
            ShopTab.WINGS -> _state.value.copy(wingsItems = list)
        }
    }


    // Clear purchase result (after showing a message to user)
    fun clearPurchaseResult() {
        _state.value = _state.value.copy(purchaseResult = null)
    }

    // Add currency to the user's balance (for testing or rewards)
    fun addCurrency(amount: Int) {
        val newAmount = (_state.value.currency + amount).coerceAtLeast(0)
        _state.value = _state.value.copy(currency = newAmount)
    }
}

// reference list
// Kotlin Documentation, 2020. When Expression. [online] Available at: <https://kotlinlang.org/docs/control-flow.html#when-expression> [Accessed 3 October 2025].
//Kotlin Documentation, 2025. Interfaces | Kotlin. [online] Available at: <https://kotlinlang.org/docs/interfaces.html> [Accessed 3 October 2025].
//


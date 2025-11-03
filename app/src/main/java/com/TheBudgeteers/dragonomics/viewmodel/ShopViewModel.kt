package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.R
import com.TheBudgeteers.dragonomics.data.FirebaseCosmeticsRepository
import com.TheBudgeteers.dragonomics.models.ShopItem
import com.TheBudgeteers.dragonomics.models.ShopTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


// ShopViewModel manages the in-app shop where users buy dragon accessories.
// UPDATED FOR FIREBASE: Now persists purchases and equipped items to Firestore.


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
    val currency: Int = 0,
    val hornsItems: List<ShopItem> = emptyList(),
    val wingsItems: List<ShopItem> = emptyList(),
    val paletteItems: List<ShopItem> = emptyList(),
    val currentTab: ShopTab = ShopTab.PALETTE,
    val purchaseResult: PurchaseResult? = null,
    val isLoading: Boolean = true
)

// Outcome of a purchase attempt
sealed class PurchaseResult {
    object Success : PurchaseResult()
    object InsufficientFunds : PurchaseResult()
}

class ShopViewModel : ViewModel() {

    private val cosmeticsRepo = FirebaseCosmeticsRepository()
    private var equipListener: AccessoryEquipListener? = null
    private var currentUserId: String? = null

    private val _state = MutableStateFlow(ShopState())
    val state: StateFlow<ShopState> = _state.asStateFlow()

    // Initialize shop data for a specific user.
    fun initialize(userId: String) {
        android.util.Log.d("ShopViewModel", "🔵 initialize() called with userId: $userId")
        currentUserId = userId
        loadShopData(userId)
    }

    // Set the listener for notifying equipped items
    fun setEquipListener(listener: AccessoryEquipListener) {
        this.equipListener = listener
    }

    // Load shop data from Firebase and merge with owned/equipped items
    private fun loadShopData(userId: String) {

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Get user's cosmetics data from Firestore
            cosmeticsRepo.getCosmeticsDataFlow(userId).collect { data ->

                if (data == null) {
                    android.util.Log.e("ShopViewModel", "data is null!")
                    return@collect
                }

                android.util.Log.d("ShopViewModel", "Got data - currency: ${data.currency}, owned: ${data.ownedItems}")


                // Define all available shop items
                val allHorns = listOf(
                    ShopItem("horns_twisted", "Twisted Horns", 90, previewRes = R.drawable.placeholder_item),
                    ShopItem("horns_curly", "Curly Horns", 90, previewRes = R.drawable.placeholder_item),
                    ShopItem("horns_chipped", "Chipped Horns", 0, previewRes = R.drawable.placeholder_item)
                )

                val allWings = listOf(
                    ShopItem("wings_bat", "Bat Wings", 120, previewRes = R.drawable.placeholder_item),
                    ShopItem("wings_feather", "Feathered", 150, previewRes = R.drawable.placeholder_item),
                    ShopItem("wings_ragged", "Ragged", 60, previewRes = R.drawable.placeholder_item)
                )

                val allPalettes = listOf(
                    ShopItem("pal_forest", "Forest Scheme", 40, previewRes = R.drawable.placeholder_item),
                    ShopItem("pal_crimson", "Crimson Scheme", 60, previewRes = R.drawable.placeholder_item),
                    ShopItem("pal_ember", "Ember Scheme", 0, previewRes = R.drawable.placeholder_item),
                    ShopItem("pal_ice", "Ice Scheme", 50, previewRes = R.drawable.placeholder_item)
                )

                // Mark items as owned/equipped based on Firestore data
                val hornsWithState = allHorns.map { item ->
                    item.copy(
                        owned = data.ownedItems.contains(item.id),
                        equipped = item.id == data.equippedHorns
                    )
                }

                val wingsWithState = allWings.map { item ->
                    item.copy(
                        owned = data.ownedItems.contains(item.id),
                        equipped = item.id == data.equippedWings
                    )
                }

                val palettesWithState = allPalettes.map { item ->
                    item.copy(
                        owned = data.ownedItems.contains(item.id),
                        equipped = item.id == data.equippedPalette
                    )
                }

                _state.value = _state.value.copy(
                    currency = data.currency,
                    hornsItems = hornsWithState,
                    wingsItems = wingsWithState,
                    paletteItems = palettesWithState,
                    isLoading = false
                )
            }
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

    // Handle user clicking on a shop item
    fun handleItemAction(item: ShopItem) {
        val userId = currentUserId ?: return

        when {
            item.equipped -> return // Already wearing it
            item.owned -> equipItem(userId, item) // User owns it, so equip it
            else -> purchaseItem(userId, item) // User doesn't own it, try to buy it
        }
    }

    // Equip an item (user already owns it)
    private fun equipItem(userId: String, item: ShopItem) {
        viewModelScope.launch {
            val itemType = when (_state.value.currentTab) {
                ShopTab.HORNS -> "horns"
                ShopTab.WINGS -> "wings"
                ShopTab.PALETTE -> "palette"
            }

            val result = cosmeticsRepo.equipItem(userId, itemType, item.id)

            if (result.isSuccess) {
                // Notify the dragon view to update appearance
                equipListener?.onAccessoryEquipped(itemType, item.id)
            }
        }
    }

    // Attempt to purchase an item
    private fun purchaseItem(userId: String, item: ShopItem) {
        viewModelScope.launch {
            val result = cosmeticsRepo.purchaseItem(userId, item.id, item.price)

            _state.value = _state.value.copy(
                purchaseResult = if (result.isSuccess) {
                    PurchaseResult.Success
                } else {
                    PurchaseResult.InsufficientFunds
                }
            )
        }
    }

   // Clear purchase result (after showing message)
    fun clearPurchaseResult() {
        _state.value = _state.value.copy(purchaseResult = null)
    }
}
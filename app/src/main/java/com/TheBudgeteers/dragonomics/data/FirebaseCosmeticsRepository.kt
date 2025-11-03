package com.TheBudgeteers.dragonomics.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


// Firebase repository for user cosmetics and shop data.
// Stores purchased items and equipped items.
class FirebaseCosmeticsRepository {

    private val firestore = FirebaseFirestore.getInstance()

    // Data class for cosmetics state
    // Currency is now fetched from the main user document
    data class CosmeticsData(
        val currency: Int = 0,  // This comes from users gold
        val ownedItems: List<String> = emptyList(),
        val equippedHorns: String? = "horns_chipped",
        val equippedWings: String? = "wings_ragged",
        val equippedPalette: String = "pal_ember"
    )

    // Get reference to user's cosmetics document
    private fun cosmeticsDoc(userId: String) =
        firestore.collection("users")
            .document(userId)
            .collection("cosmetics")
            .document("data")

    // Get reference to user's main document (where gold is stored)
    private fun userDoc(userId: String) =
        firestore.collection("users").document(userId)

    // Initialize cosmetics data for a new user with defaults
    suspend fun initializeDefaults(userId: String): Result<Unit> {
        return try {
            android.util.Log.d("CosmeticsRepo", "Creating default cosmetics document...")

            val defaultData = hashMapOf(
                "ownedItems" to listOf("horns_chipped", "wings_ragged", "pal_ember"),
                "equippedHorns" to "horns_chipped",
                "equippedWings" to "wings_ragged",
                "equippedPalette" to "pal_ember"
            )

            cosmeticsDoc(userId).set(defaultData).await()

            // Initialize gold in main user document if it doesn't exist
            val userSnapshot = userDoc(userId).get().await()
            if (!userSnapshot.contains("gold")) {
                userDoc(userId).update("gold", 500).await()
            }

            android.util.Log.d("CosmeticsRepo", "Default cosmetics created successfully!")

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CosmeticsRepo", "Failed to create defaults", e)
            Result.failure(e)
        }
    }

    // Get cosmetics data as a one-time fetch
    suspend fun getCosmeticsData(userId: String): CosmeticsData? {
        return try {
            android.util.Log.d("CosmeticsRepo", "Getting cosmetics for userId: $userId")

            val cosmeticsSnapshot = cosmeticsDoc(userId).get().await()

            android.util.Log.d("CosmeticsRepo", "Document exists: ${cosmeticsSnapshot.exists()}")

            if (!cosmeticsSnapshot.exists()) {
                android.util.Log.d("CosmeticsRepo", "Initializing defaults...")
                initializeDefaults(userId)
                return CosmeticsData()
            }

            // Get gold from main user document
            val userSnapshot = userDoc(userId).get().await()
            val currency = userSnapshot.getLong("gold")?.toInt() ?: 0

            val data = CosmeticsData(
                currency = currency,  // Now from main user doc!
                ownedItems = (cosmeticsSnapshot.get("ownedItems") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                equippedHorns = cosmeticsSnapshot.getString("equippedHorns"),
                equippedWings = cosmeticsSnapshot.getString("equippedWings"),
                equippedPalette = cosmeticsSnapshot.getString("equippedPalette") ?: "pal_ember"
            )

            android.util.Log.d("CosmeticsRepo", "Loaded data - currency: ${data.currency}, owned: ${data.ownedItems.size}")

            data
        } catch (e: Exception) {
            android.util.Log.e("CosmeticsRepo", "Error getting cosmetics", e)
            null
        }
    }

     // Get cosmetics data as a reactive Flow
     // Listens to BOTH user document (for gold) and cosmetics document
    fun getCosmeticsDataFlow(userId: String): Flow<CosmeticsData?> = callbackFlow {
        android.util.Log.d("CosmeticsRepo", "getCosmeticsDataFlow() called for userId: $userId")

        // Initialize defaults if document doesn't exist
        val initialDoc = cosmeticsDoc(userId).get().await()
        if (!initialDoc.exists()) {
            android.util.Log.d("CosmeticsRepo", "Document doesn't exist, initializing...")
            initializeDefaults(userId)
        }

        var latestGold = 0
        var latestCosmetics: Map<String, Any?>? = null

        // Listen to user document for gold changes
        val userListener = userDoc(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CosmeticsRepo", "User listener error: ${error.message}", error)
                    return@addSnapshotListener
                }

                latestGold = snapshot?.getLong("gold")?.toInt() ?: 0
                android.util.Log.d("CosmeticsRepo", "Gold updated: $latestGold")

                // Emit combined data if we have cosmetics data
                if (latestCosmetics != null) {
                    emitCombinedData(latestGold, latestCosmetics)
                }
            }

        // Listen to cosmetics document for item changes
        val cosmeticsListener = cosmeticsDoc(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CosmeticsRepo", "Cosmetics listener error: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot?.exists() == true) {
                    latestCosmetics = snapshot.data
                    android.util.Log.d("CosmeticsRepo", "Cosmetics updated")

                    // Emit combined data
                    emitCombinedData(latestGold, latestCosmetics)
                }
            }

        awaitClose {
            android.util.Log.d("CosmeticsRepo", "Flow closed")
            userListener.remove()
            cosmeticsListener.remove()
        }
    }

    private fun kotlinx.coroutines.channels.ProducerScope<CosmeticsData?>.emitCombinedData(gold: Int, cosmeticsData: Map<String, Any?>?) {
        if (cosmeticsData == null) {
            trySend(CosmeticsData(currency = gold))
            return
        }

        val data = CosmeticsData(
            currency = gold,
            ownedItems = (cosmeticsData["ownedItems"] as? List<*>)?.mapNotNull { it as? String }
                ?: listOf("horns_chipped", "wings_ragged", "pal_ember"),
            equippedHorns = cosmeticsData["equippedHorns"] as? String ?: "horns_chipped",
            equippedWings = cosmeticsData["equippedWings"] as? String ?: "wings_ragged",
            equippedPalette = cosmeticsData["equippedPalette"] as? String ?: "pal_ember"
        )

        android.util.Log.d("CosmeticsRepo", "Emitting combined data - currency: ${data.currency}, owned: ${data.ownedItems.size}")
        trySend(data)
    }

     // Purchase an item (deduct gold from main user doc and add to owned items)
    suspend fun purchaseItem(userId: String, itemId: String, price: Int): Result<Unit> {
        return try {
            // Use a transaction to ensure atomic operation
            firestore.runTransaction { transaction ->
                val userRef = userDoc(userId)
                val cosmeticsRef = cosmeticsDoc(userId)

                // Get current gold
                val userSnapshot = transaction.get(userRef)
                val currentGold = userSnapshot.getLong("gold")?.toInt() ?: 0

                // Get current owned items
                val cosmeticsSnapshot = transaction.get(cosmeticsRef)
                val currentOwned = (cosmeticsSnapshot.get("ownedItems") as? List<*>)
                    ?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()

                // Check if user can afford it
                if (currentGold < price) {
                    throw Exception("Insufficient funds")
                }

                // Check if already owned
                if (currentOwned.contains(itemId)) {
                    throw Exception("Already owned")
                }

                // Deduct gold from main user document
                transaction.update(userRef, "gold", currentGold - price)

                // Add item to owned items
                currentOwned.add(itemId)
                transaction.update(cosmeticsRef, "ownedItems", currentOwned)

            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Equip an item
    suspend fun equipItem(userId: String, itemType: String, itemId: String): Result<Unit> {
        return try {
            val fieldName = when (itemType) {
                "horns" -> "equippedHorns"
                "wings" -> "equippedWings"
                "palette" -> "equippedPalette"
                else -> return Result.failure(Exception("Invalid item type"))
            }

            cosmeticsDoc(userId).update(fieldName, itemId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add currency to user's balance (updates main user document)
    suspend fun addCurrency(userId: String, amount: Int): Result<Unit> {
        return try {
            val userRef = userDoc(userId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentGold = snapshot.getLong("gold")?.toInt() ?: 0
                val newGold = (currentGold + amount).coerceAtLeast(0)
                transaction.update(userRef, "gold", newGold)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
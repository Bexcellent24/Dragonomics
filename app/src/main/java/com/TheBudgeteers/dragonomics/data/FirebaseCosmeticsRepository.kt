package com.TheBudgeteers.dragonomics.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase repository for user cosmetics and shop data.
 * Stores purchased items, equipped items, and currency balance.
 *
 * Firestore structure:
 * /users/{userId}/cosmetics/data
 *   - currency: Int
 *   - ownedItems: List<String> (item IDs the user owns)
 *   - equippedHorns: String?
 *   - equippedWings: String?
 *   - equippedPalette: String
 */
class FirebaseCosmeticsRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Data class for cosmetics state
     */
    data class CosmeticsData(
        val currency: Int = 0,
        val ownedItems: List<String> = emptyList(),
        val equippedHorns: String? = "horns_chipped",
        val equippedWings: String? = "wings_ragged",
        val equippedPalette: String = "pal_ember"
    )

    /**
     * Get reference to user's cosmetics document
     */
    private fun cosmeticsDoc(userId: String) =
        firestore.collection("users")
            .document(userId)
            .collection("cosmetics")
            .document("data")

    /**
     * Initialize cosmetics data for a new user with defaults
     */
    suspend fun initializeDefaults(userId: String): Result<Unit> {
        return try {
            android.util.Log.d("CosmeticsRepo", "Creating default cosmetics document...")

            val defaultData = hashMapOf(
                "currency" to 500,
                "ownedItems" to listOf("horns_chipped", "wings_ragged", "pal_ember"),
                "equippedHorns" to "horns_chipped",
                "equippedWings" to "wings_ragged",
                "equippedPalette" to "pal_ember"
            )

            cosmeticsDoc(userId).set(defaultData).await()

            android.util.Log.d("CosmeticsRepo", "Default cosmetics created successfully!")

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CosmeticsRepo", "Failed to create defaults", e)
            Result.failure(e)
        }
    }

    /**
     * Get cosmetics data as a one-time fetch
     */
    suspend fun getCosmeticsData(userId: String): CosmeticsData? {
        return try {
            android.util.Log.d("CosmeticsRepo", "Getting cosmetics for userId: $userId")

            val doc = cosmeticsDoc(userId).get().await()

            android.util.Log.d("CosmeticsRepo", "Document exists: ${doc.exists()}")

            if (!doc.exists()) {
                android.util.Log.d("CosmeticsRepo", "Initializing defaults...")
                initializeDefaults(userId)
                return CosmeticsData()
            }

            val data = CosmeticsData(
                currency = (doc.getLong("currency")?.toInt()) ?: 0,
                ownedItems = (doc.get("ownedItems") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                equippedHorns = doc.getString("equippedHorns"),
                equippedWings = doc.getString("equippedWings"),
                equippedPalette = doc.getString("equippedPalette") ?: "pal_ember"
            )

            android.util.Log.d("CosmeticsRepo", "Loaded data - currency: ${data.currency}, owned: ${data.ownedItems.size}")

            data
        } catch (e: Exception) {
            android.util.Log.e("CosmeticsRepo", "Error getting cosmetics", e)
            null
        }
    }

    /**
     * Get cosmetics data as a reactive Flow
     */
    fun getCosmeticsDataFlow(userId: String): Flow<CosmeticsData?> = callbackFlow {
        android.util.Log.d("CosmeticsRepo", "getCosmeticsDataFlow() called for userId: $userId")

        // Initialize defaults if document doesn't exist (one-time check)
        val initialDoc = cosmeticsDoc(userId).get().await()
        if (!initialDoc.exists()) {
            android.util.Log.d("CosmeticsRepo", "Document doesn't exist, initializing...")
            initializeDefaults(userId)
        }

        val listener = cosmeticsDoc(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CosmeticsRepo", "Listener error: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    android.util.Log.w("CosmeticsRepo", "⚠Snapshot is null")
                    trySend(CosmeticsData())
                    return@addSnapshotListener
                }

                if (!snapshot.exists()) {
                    android.util.Log.w("CosmeticsRepo", "Document doesn't exist, returning defaults")
                    trySend(CosmeticsData())
                    return@addSnapshotListener
                }

                android.util.Log.d("CosmeticsRepo", "Document exists! Reading data...")

                val data = CosmeticsData(
                    currency = (snapshot.getLong("currency")?.toInt()) ?: 500,
                    ownedItems = (snapshot.get("ownedItems") as? List<*>)?.mapNotNull { it as? String }
                        ?: listOf("horns_chipped", "wings_ragged", "pal_ember"),  //Fallback to defaults
                    equippedHorns = snapshot.getString("equippedHorns") ?: "horns_chipped",
                    equippedWings = snapshot.getString("equippedWings") ?: "wings_ragged",
                    equippedPalette = snapshot.getString("equippedPalette") ?: "pal_ember"
                )

                android.util.Log.d("CosmeticsRepo", "Sending data - currency: ${data.currency}")
                trySend(data)
            }

        awaitClose {
            android.util.Log.d("CosmeticsRepo", "Flow closed")
            listener.remove()
        }
    }

    /**
     * Purchase an item (deduct currency and add to owned items)
     */
    suspend fun purchaseItem(userId: String, itemId: String, price: Int): Result<Unit> {
        return try {
            val doc = cosmeticsDoc(userId)
            val snapshot = doc.get().await()

            val currentCurrency = (snapshot.getLong("currency")?.toInt()) ?: 0
            val currentOwned = (snapshot.get("ownedItems") as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()

            if (currentCurrency < price) {
                return Result.failure(Exception("Insufficient funds"))
            }

            if (currentOwned.contains(itemId)) {
                return Result.failure(Exception("Already owned"))
            }

            currentOwned.add(itemId)

            doc.update(
                mapOf(
                    "currency" to (currentCurrency - price),
                    "ownedItems" to currentOwned
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Equip an item
     */
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

    /**
     * Add currency to user's balance
     */
    suspend fun addCurrency(userId: String, amount: Int): Result<Unit> {
        return try {
            val doc = cosmeticsDoc(userId)
            val snapshot = doc.get().await()

            val currentCurrency = (snapshot.getLong("currency")?.toInt()) ?: 0
            val newCurrency = (currentCurrency + amount).coerceAtLeast(0)

            doc.update("currency", newCurrency).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set currency to specific amount (useful for rewards/penalties)
     */
    suspend fun setCurrency(userId: String, amount: Int): Result<Unit> {
        return try {
            cosmeticsDoc(userId).update("currency", amount.coerceAtLeast(0)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
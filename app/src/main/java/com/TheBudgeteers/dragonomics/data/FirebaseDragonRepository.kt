package com.TheBudgeteers.dragonomics.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


// Firebase repository for dragon game state.
// Stores dragon XP, mood, and last login per user.

class FirebaseDragonRepository {

    private val firestore = FirebaseFirestore.getInstance()


     // Save dragon state to Firebase for a specific user.

    suspend fun saveDragonState(
        userId: String,
        totalXp: Int,
        moodScore: Int,
        lastLoginYmd: Int
    ): Result<Unit> {
        return try {
            val dragonData = hashMapOf(
                "totalXp" to totalXp,
                "moodScore" to moodScore,
                "lastLoginYmd" to lastLoginYmd,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(userId)
                .collection("dragonState")
                .document("current")
                .set(dragonData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseDragonRepo", "Failed to save dragon state", e)
            Result.failure(e)
        }
    }


     // Load dragon state from Firebase for a specific user.
     // Returns null if no state exists (new user).
    suspend fun loadDragonState(userId: String): DragonStateData? {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("dragonState")
                .document("current")
                .get()
                .await()

            if (doc.exists()) {
                DragonStateData(
                    totalXp = doc.getLong("totalXp")?.toInt() ?: 0,
                    moodScore = doc.getLong("moodScore")?.toInt() ?: 0,
                    lastLoginYmd = doc.getLong("lastLoginYmd")?.toInt() ?: 0
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseDragonRepo", "Failed to load dragon state", e)
            null
        }
    }


     // Get dragon state as a reactive Flow.
    fun getDragonStateFlow(userId: String): Flow<DragonStateData?> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userId)
            .collection("dragonState")
            .document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val state = snapshot?.let { doc ->
                    if (doc.exists()) {
                        DragonStateData(
                            totalXp = doc.getLong("totalXp")?.toInt() ?: 0,
                            moodScore = doc.getLong("moodScore")?.toInt() ?: 0,
                            lastLoginYmd = doc.getLong("lastLoginYmd")?.toInt() ?: 0
                        )
                    } else null
                }

                trySend(state)
            }

        awaitClose { listener.remove() }
    }
}


// Simple data class for dragon state from Firebase.
data class DragonStateData(
    val totalXp: Int,
    val moodScore: Int,
    val lastLoginYmd: Int
)
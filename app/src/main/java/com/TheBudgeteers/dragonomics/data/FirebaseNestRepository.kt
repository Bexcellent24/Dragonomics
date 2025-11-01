package com.TheBudgeteers.dragonomics.data

import com.TheBudgeteers.dragonomics.models.Nest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Firestore repository for Nest operations.
 * Replaces NestDao with Firestore implementation.
 *
 * Firestore structure:
 * /users/{userId}/nests/{nestId}
 *   - name: String
 *   - budget: Double?
 *   - icon: String
 *   - colour: String
 *   - type: String ("INCOME" or "EXPENSE")
 */
class FirebaseNestRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Get reference to user's nests collection
     */
    private fun nestsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("nests")

    /**
     * Insert a new nest into Firestore.
     * Returns the generated nest ID.
     */
    suspend fun insert(userId: String, nest: Nest): String {
        val nestData = hashMapOf(
            "name" to nest.name,
            "budget" to nest.budget,
            "icon" to nest.icon,
            "colour" to nest.colour,
            "type" to nest.type.name,
            "createdAt" to System.currentTimeMillis()
        )

        val docRef = nestsCollection(userId).add(nestData).await()
        return docRef.id
    }

    /**
     * Get all nests for a user (non-reactive).
     * Returns List<Nest> with Firestore document IDs.
     */
    suspend fun getAll(userId: String): List<Nest> {
        val snapshot = nestsCollection(userId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            documentToNest(userId, doc.id, doc.data)
        }
    }

    /**
     * Get a single nest by ID.
     */
    suspend fun getById(userId: String, nestId: String): Nest? {
        val doc = nestsCollection(userId).document(nestId).get().await()
        return if (doc.exists()) {
            documentToNest(userId, doc.id, doc.data)
        } else null
    }

    /**
     * Get all nests for a user as a reactive Flow.
     * Updates automatically when Firestore data changes.
     */
    fun getAllFlow(userId: String): Flow<List<Nest>> = callbackFlow {
        val listener = nestsCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val nests = snapshot?.documents?.mapNotNull { doc ->
                    documentToNest(userId, doc.id, doc.data)
                } ?: emptyList()

                trySend(nests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get nests filtered by type (INCOME or EXPENSE) as a reactive Flow.
     */
    fun getAllFlowByType(userId: String, type: NestType): Flow<List<Nest>> = callbackFlow {
        val listener = nestsCollection(userId)
            .whereEqualTo("type", type.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val nests = snapshot?.documents?.mapNotNull { doc ->
                    documentToNest(userId, doc.id, doc.data)
                } ?: emptyList()

                trySend(nests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Update a nest's data.
     */
    suspend fun update(userId: String, nestId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            nestsCollection(userId)
                .document(nestId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a nest.
     */
    suspend fun delete(userId: String, nestId: String): Result<Unit> {
        return try {
            nestsCollection(userId)
                .document(nestId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert Firestore document to Nest model.
     * Handles the conversion from Map to typed Nest object.
     */
    private fun documentToNest(userId: String, docId: String, data: Map<String, Any?>?): Nest? {
        if (data == null) return null

        return try {
            Nest(
                id = docId, // Firestore document ID (String)
                userId = userId, // Firebase UID (String)
                name = data["name"] as? String ?: return null,
                budget = data["budget"] as? Double,
                icon = data["icon"] as? String ?: "",
                colour = data["colour"] as? String ?: "#000000",
                type = NestType.valueOf(data["type"] as? String ?: return null)
            )
        } catch (e: Exception) {
            null
        }
    }
}
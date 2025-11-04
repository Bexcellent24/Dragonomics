package com.TheBudgeteers.dragonomics.data

import com.TheBudgeteers.dragonomics.gamify.Achievement
import com.TheBudgeteers.dragonomics.gamify.AchievementType
import com.TheBudgeteers.dragonomics.gamify.UserAchievement
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


// Repository for achievement operations with Firebase Firestore.

class FirebaseAchievementRepository {

    private val firestore = FirebaseFirestore.getInstance()

    // ---------- GLOBAL ACHIEVEMENTS ----------

    // begin code attribution
    // Firestore write operations with coroutines adapted from:
    // “Use Kotlin coroutines with Firebase” — Firebase Documentation

    // Initialize default achievements in Firestore.
    suspend fun initializeDefaultAchievements(): Result<Unit> {
        return try {
            val achievements = getDefaultAchievements()

            achievements.forEach { achievement ->
                firestore.collection("achievements")
                    .document(achievement.id)
                    .set(achievement.toMap())
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // end code attribution (Firebase, 2024)

    // Get all available achievements.
    suspend fun getAllAchievements(): List<Achievement> {
        return try {
            val snapshot = firestore.collection("achievements")
                .orderBy("order")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                mapToAchievement(doc.data)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Get a specific achievement definition.
    suspend fun getAchievement(achievementId: String): Achievement? {
        return try {
            val doc = firestore.collection("achievements")
                .document(achievementId)
                .get()
                .await()

            if (doc.exists()) mapToAchievement(doc.data) else null
        } catch (e: Exception) {
            null
        }
    }

    // ---------- USER ACHIEVEMENTS ----------

    // begin code attribution
    // Snapshot listener to Flow conversion pattern adapted from:
    // “Kotlin Flows on Android” — Android Developers guide

    // Get user's progress on all achievements as a Flow.
    fun getUserAchievementsFlow(userId: String): Flow<List<UserAchievement>> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userId)
            .collection("userAchievements")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val achievements = snapshot?.documents?.mapNotNull { doc ->
                    mapToUserAchievement(doc.data)
                } ?: emptyList()

                trySend(achievements)
            }

        awaitClose { listener.remove() }
    }

    // end code attribution (Android Developers, 2024)


    // Get user's progress on a specific achievement.
    suspend fun getUserAchievement(userId: String, achievementId: String): UserAchievement? {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("userAchievements")
                .document(achievementId)
                .get()
                .await()

            if (doc.exists()) {
                mapToUserAchievement(doc.data)
            } else {
                // Create initial entry if doesn't exist
                UserAchievement(
                    achievementId = achievementId,
                    userId = userId,
                    achieved = false,
                    progress = 0
                )
            }
        } catch (e: Exception) {
            null
        }
    }


    //  Update user's progress on an achievement.
    suspend fun updateUserAchievement(userId: String, achievementId: String, progress: Int, achieved: Boolean): Result<Unit> {
        return try {
            val data = hashMapOf(
                "achievementId" to achievementId,
                "userId" to userId,
                "progress" to progress,
                "achieved" to achieved,
                "unlockedAt" to if (achieved) System.currentTimeMillis() else null
            )

            firestore.collection("users")
                .document(userId)
                .collection("userAchievements")
                .document(achievementId)
                .set(data)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Unlock an achievement for a user.
    suspend fun unlockAchievement(userId: String, achievementId: String): Result<Int> {
        return try {
            // Get the achievement to find gold reward
            val achievement = getAchievement(achievementId)
            val goldReward = achievement?.goldReward ?: 0

            // Mark as unlocked
            val data = hashMapOf(
                "achievementId" to achievementId,
                "userId" to userId,
                "achieved" to true,
                "unlockedAt" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(userId)
                .collection("userAchievements")
                .document(achievementId)
                .set(data)
                .await()

            // Award gold to user
            if (goldReward > 0) {
                awardGold(userId, goldReward).getOrThrow()
            }

            Result.success(goldReward)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- USER STATS ----------

    // Update user statistics for tracking achievements.
    suspend fun updateUserStats(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("stats")
                .document("tracking")
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Get user statistics.
    suspend fun getUserStats(userId: String): Map<String, Any> {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("stats")
                .document("tracking")
                .get()
                .await()

            doc.data ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // begin code attribution
    // Firestore transaction pattern adapted from:
    // “Perform simple and complex transactions” — Firebase Documentation

    // Award gold to user.
    private suspend fun awardGold(userId: String, amount: Int): Result<Unit> {
        return try {
            val userDoc = firestore.collection("users").document(userId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userDoc)
                val currentGold = snapshot.getLong("gold") ?: 0L
                transaction.update(userDoc, "gold", currentGold + amount)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // end code attribution (Firebase, 2024)



    // ---------- HELPER METHODS ----------

    private fun mapToAchievement(data: Map<String, Any?>?): Achievement? {
        if (data == null) return null

        return try {
            Achievement(
                id = data["id"] as? String ?: "",
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                medalRes = (data["medalRes"] as? Long)?.toInt() ?: 0,
                goldReward = (data["goldReward"] as? Long)?.toInt() ?: 0,
                type = AchievementType.valueOf(data["type"] as? String ?: "ONE_TIME"),
                targetValue = (data["targetValue"] as? Long)?.toInt() ?: 1,
                order = (data["order"] as? Long)?.toInt() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun mapToUserAchievement(data: Map<String, Any?>?): UserAchievement? {
        if (data == null) return null

        return try {
            UserAchievement(
                achievementId = data["achievementId"] as? String ?: "",
                userId = data["userId"] as? String ?: "",
                achieved = data["achieved"] as? Boolean ?: false,
                progress = (data["progress"] as? Long)?.toInt() ?: 0,
                unlockedAt = data["unlockedAt"] as? Long
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun Achievement.toMap() = hashMapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "medalRes" to medalRes,
        "goldReward" to goldReward,
        "type" to type.name,
        "targetValue" to targetValue,
        "order" to order
    )


    // Default achievements for the prototype.
    // In production, these would be managed via admin panel.
    private fun getDefaultAchievements() = listOf(
        Achievement(
            id = "first_nest",
            title = "Nest Builder",
            description = "Create your first nest",
            medalRes = com.TheBudgeteers.dragonomics.R.drawable.bronze_badge,
            goldReward = 10,
            type = AchievementType.ONE_TIME,
            order = 1
        ),
        Achievement(
            id = "first_expense",
            title = "First Steps",
            description = "Log your first expense",
            medalRes = com.TheBudgeteers.dragonomics.R.drawable.bronze_badge,
            goldReward = 10,
            type = AchievementType.ONE_TIME,
            order = 2
        ),
        Achievement(
            id = "five_expenses",
            title = "Getting Started",
            description = "Log 5 expenses",
            medalRes = com.TheBudgeteers.dragonomics.R.drawable.bronze_badge,
            goldReward = 25,
            type = AchievementType.CUMULATIVE,
            targetValue = 5,
            order = 3
        ),
        Achievement(
            id = "day_one",
            title = "Welcome!",
            description = "Login on day one",
            medalRes = com.TheBudgeteers.dragonomics.R.drawable.bronze_badge,
            goldReward = 5,
            type = AchievementType.ONE_TIME,
            order = 4
        ),
        Achievement(
            id = "day_two_streak",
            title = "Coming Back",
            description = "Login 2 days in a row",
            medalRes = com.TheBudgeteers.dragonomics.R.drawable.bronze_badge,
            goldReward = 15,
            type = AchievementType.STREAK,
            targetValue = 2,
            order = 5
        ),
        Achievement(
            id = "week_streak",
            title = "Consistent",
            description = "Login 7 days in a row",
            medalRes = com.TheBudgeteers.dragonomics.R.drawable.silver_badge,
            goldReward = 50,
            type = AchievementType.STREAK,
            targetValue = 7,
            order = 6
        ),
        Achievement(
            id = "month_streak",
            title = "Flames of Authority",
            description = "Login 30 days in a row",
            medalRes = com.TheBudgeteers.dragonomics.R.drawable.gold_badge,
            goldReward = 200,
            type = AchievementType.STREAK,
            targetValue = 30,
            order = 7
        ),
        Achievement(
            id = "ten_expenses",
            title = "Expense Tracker",
            description = "Log 10 expenses",
            medalRes = com.TheBudgeteers.dragonomics.R.drawable.silver_badge,
            goldReward = 50,
            type = AchievementType.CUMULATIVE,
            targetValue = 10,
            order = 8
        ),
        Achievement(
            id = "first_income",
            title = "Money Maker",
            description = "Log your first income",
            medalRes = com.TheBudgeteers.dragonomics.R.drawable.bronze_badge,
            goldReward = 10,
            type = AchievementType.ONE_TIME,
            order = 9
        )
    )
}

// Firebase, 2024. Use Kotlin coroutines with Firebase. [online] Available at: <https://firebase.google.com/docs/firestore/extend-with-functions> [Accessed 4 November 2025]
// Android Developers, 2024. Kotlin Flows on Android. [online] Available at: <https://developer.android.com/kotlin/flow> [Accessed 4 November 2025]
// Firebase, 2024. Perform simple and complex transactions. [online] Available at: <https://firebase.google.com/docs/firestore/manage-data/transactions> [Accessed 4 November 2025]
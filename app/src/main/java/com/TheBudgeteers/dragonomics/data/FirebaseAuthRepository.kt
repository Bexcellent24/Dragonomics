package com.TheBudgeteers.dragonomics.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Handles Firebase Authentication and user profile management.
 * Replaces Room-based authentication with Firebase Auth.
 *
 * User profile data (username, goals) is stored in Firestore at:
 * /users/{uid}/profile
 */
class FirebaseAuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Sign up a new user with Firebase Authentication.
     * Also creates a user profile document in Firestore.
     *
     * @return Result containing Firebase UID on success
     */
    suspend fun signUpUser(username: String, email: String, password: String): Result<String> {
        val u = username.trim()
        val e = email.trim()

        if (u.isBlank() || e.isBlank() || password.isEmpty()) {
            return Result.failure(IllegalArgumentException("All fields required"))
        }

        return try {
            // Check if username is already taken in Firestore
            val usernameCheck = firestore.collection("usernames")
                .document(u)
                .get()
                .await()

            if (usernameCheck.exists()) {
                return Result.failure(IllegalStateException("Username taken"))
            }

            // Create Firebase Auth user
            val authResult = auth.createUserWithEmailAndPassword(e, password).await()
            val uid = authResult.user?.uid
                ?: return Result.failure(Exception("Failed to create user"))

            // Create user profile in Firestore
            val userProfile = hashMapOf(
                "username" to u,
                "email" to e,
                "minGoal" to null,
                "maxGoal" to null,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(uid)
                .set(userProfile)
                .await()

            // Reserve username in separate collection for uniqueness check
            firestore.collection("usernames")
                .document(u)
                .set(hashMapOf("uid" to uid))
                .await()

            Result.success(uid)

        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(IllegalStateException("Email already in use"))
        } catch (e: Exception) {
            Result.failure(Exception("Sign up failed: ${e.message}"))
        }
    }

    /**
     * Log in an existing user with Firebase Authentication.
     * Note: Firebase Auth uses email for login, but we accept username
     * and look up the associated email from Firestore.
     *
     * @return Result containing Firebase UID on success
     */
    suspend fun loginUser(username: String, password: String): Result<String> {
        val u = username.trim()

        if (u.isBlank() || password.isEmpty()) {
            return Result.failure(Exception("Invalid credentials"))
        }

        return try {
            // Look up email from username
            val usernameDoc = firestore.collection("usernames")
                .document(u)
                .get()
                .await()

            if (!usernameDoc.exists()) {
                return Result.failure(Exception("Invalid credentials"))
            }

            val uid = usernameDoc.getString("uid")
                ?: return Result.failure(Exception("Invalid credentials"))

            // Get email from user profile
            val userProfile = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            val email = userProfile.getString("email")
                ?: return Result.failure(Exception("Invalid credentials"))

            // Sign in with Firebase Auth
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val loggedInUid = authResult.user?.uid
                ?: return Result.failure(Exception("Login failed"))

            Result.success(loggedInUid)

        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Invalid credentials"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Invalid credentials"))
        } catch (e: Exception) {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    /**
     * Update user's financial goals in Firestore
     */
    suspend fun updateUserGoals(userId: String, minGoal: Double?, maxGoal: Double?): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "minGoal" to minGoal,
                        "maxGoal" to maxGoal
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current Firebase user UID, or null if not logged in
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Sign out the current user
     */
    fun signOut() {
        auth.signOut()
    }
}
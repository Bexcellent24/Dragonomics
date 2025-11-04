package com.TheBudgeteers.dragonomics.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


 // Handles Firebase Authentication and user profile management.
 // Replaces Room-based authentication with Firebase Auth.

class FirebaseAuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()


    // begin code attribution
    // Usage of createUserWithEmailAndPassword then await() in Kotlin coroutines adapted from:
    // “Using Firebase on Android with Kotlin Coroutines” — Joe Birch (2020)

    // Sign up a new user with Firebase Authentication.
    // Also creates a user profile document in Firestore.
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
                "gold" to 500,
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

            // Initialize default cosmetics for new user
            initializeDefaultCosmetics(uid)

            Result.success(uid)

        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(IllegalStateException("Email already in use"))
        } catch (e: Exception) {
            Result.failure(Exception("Sign up failed: ${e.message}"))
        }
    }

    // end code attribution (Birch, 2020)

    // begin code attribution
    // Pattern of signInWithEmailAndPassword with Kotlin coroutine await() adapted from:
    // StackOverflow answer “How to use Coroutines while using signInWithEmailAndPassword in Firebase Auth”
    // Log in an existing user with Firebase Authentication.
    // Note: Firebase Auth uses email for login, but we accept username and look up the associated email from Firestore.
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
    // end code attribution (StackOverflow, 2023)


    // Update user's financial goals in Firestore
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


    // Initialize default cosmetics for new users.
    // Gives them the starter items (chipped horns, ragged wings, ember palette).
    private suspend fun initializeDefaultCosmetics(userId: String) {
        try {
            val defaultCosmetics = hashMapOf(
                "ownedItems" to listOf("horns_chipped", "wings_ragged", "pal_ember"),
                "equippedHorns" to "horns_chipped",
                "equippedWings" to "wings_ragged",
                "equippedPalette" to "pal_ember"
            )

            firestore.collection("users")
                .document(userId)
                .collection("cosmetics")
                .document("data")
                .set(defaultCosmetics)
                .await()

            android.util.Log.d("FirebaseAuthRepo", "Initialized default cosmetics for user $userId")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseAuthRepo", "Failed to initialize cosmetics for user $userId", e)
            // Don't fail the signup if cosmetics initialization fails
        }
    }


    // Get current Firebase user UID, or null if not logged in
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Sign out the current user
    fun signOut() {
        auth.signOut()
    }
}

// Birch, J. 2020. Using Firebase on Android with Kotlin Coroutines. [online] Available at: https://joebirch.co/android/using-firebase-on-android-with-kotlin-coroutines/ [Accessed 4 November 2025]
// Puffelen, F., 2023. How to use Coroutines while using signInWithEmailAndPassword in Firebase Auth. [online] Available at: https://stackoverflow.com/questions/74271981/how-to-use-coroutines-while-using-signinwithemailandpassword-in-firebase-authent [Accessed 4 November 2025]
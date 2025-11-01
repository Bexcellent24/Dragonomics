package com.TheBudgeteers.dragonomics.testing

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TestFirebaseSetup {
    private val TAG = "FirebaseTest"

    fun testConnection() {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        Log.d(TAG, "🔥 Testing Firebase Authentication...")
        Log.d(TAG, "Auth instance: ${auth != null}")
        Log.d(TAG, "Current user: ${auth.currentUser?.email ?: "Not logged in"}")

        Log.d(TAG, "🔥 Testing Firestore...")
        val testData = hashMapOf(
            "message" to "Hello from Dragonomics!",
            "timestamp" to System.currentTimeMillis(),
            "test" to true
        )

        db.collection("test")
            .add(testData)
            .addOnSuccessListener { doc ->
                Log.d(TAG, "✅ Firestore WRITE successful! Doc ID: ${doc.id}")

                // Test reading it back
                db.collection("test").document(doc.id)
                    .get()
                    .addOnSuccessListener { document ->
                        Log.d(TAG, "✅ Firestore READ successful! Data: ${document.data}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Firestore READ failed", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Firestore WRITE failed", e)
            }
    }
}
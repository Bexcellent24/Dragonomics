package com.TheBudgeteers.dragonomics

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream


 // Helper class for managing user avatars.
 // Handles copying images from content URIs to app-specific storage.

object AvatarManager {

     // Copies a photo from a content URI (like photo picker) to app-specific storage.
     // Each user gets their own avatar directory.

     // context Android context
     // sourceUri URI from photo picker or camera
     // userId Current user's ID
     // Uri pointing to the copied file, or null if copy failed


    // begin code attribution
    // File copying using contentResolver and app-specific storage adapted from:
    // Android Developers guide to Scoped Storage and ContentResolver

    fun copyToAppStorage(context: Context, sourceUri: Uri, userId: Long): Uri? {
        return try {
            if (userId <= 0) return null

            // Create user-specific directory
            val dir = File(context.filesDir, "users/u_$userId/avatars").apply {
                if (!exists()) mkdirs()
            }

            // Get filename or generate one
            val name = queryDisplayName(context, sourceUri).takeIf { !it.isNullOrBlank() }
                ?: "avatar_${System.currentTimeMillis()}.jpg"

            val destFile = File(dir, name)

            // Copy the file
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // end code attribution (Android Developers, 2021)

    // begin code attribution
    // Querying display name from a content URI adapted from:
    // Android Developers guide to Querying Content Providers

     // Gets the display name of a file from its content URI
    private fun queryDisplayName(context: Context, uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }
    // end code attribution (Android Developers, 2021)

     // Deletes old avatar file when user changes their avatar
    fun deleteAvatar(avatarUri: Uri?): Boolean {
        if (avatarUri == null) return false
        return try {
            val file = File(avatarUri.path ?: return false)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

// reference list
// Android Developers, 2021. Scoped Storage Overview. [online] Available at: <https://developer.android.com/training/data-storage/shared> [Accessed 5 October 2025].
// Android Developers, 2021. Querying Content Providers. [online] Available at: <https://developer.android.com/guide/topics/providers/content-provider-basics> [Accessed 5 October 2025].
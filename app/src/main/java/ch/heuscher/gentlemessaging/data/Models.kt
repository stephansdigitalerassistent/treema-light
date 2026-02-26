package ch.heuscher.gentlemessaging.data

import android.graphics.Bitmap

/**
 * Simplified Contact model for gentle messaging UI.
 * Maps from Threema's ContactModel.
 */
data class Contact(
    val id: String,
    val name: String,
    val phoneNumber: String = "",
    val isFavorite: Boolean = true,
    val avatarColor: Long = 0xFF4CAF50
)

/**
 * Simplified Message model for gentle messaging UI.
 * Maps from Threema's MessageModel.
 *
 * thumbnailBitmap: inline image preview for IMAGE / FILE (image) messages.
 * caption: optional text caption (e.g. from file attachments with captions).
 */
data class Message(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isOutgoing: Boolean = false,
    val thumbnailBitmap: Bitmap? = null,
    val caption: String? = null
)

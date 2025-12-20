package ch.threema.treemalight.data

/**
 * Simplified Contact model for Treema Light UI.
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
 * Simplified Message model for Treema Light UI.
 * Maps from Threema's MessageModel.
 */
data class Message(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isOutgoing: Boolean = false
)

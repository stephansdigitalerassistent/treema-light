package ch.threema.treemalight.data

import android.content.Context
import ch.threema.app.ThreemaApplication
import ch.threema.app.managers.ServiceManager
import ch.threema.app.services.ContactService
import ch.threema.app.services.MessageService
import ch.threema.storage.models.ContactModel
import ch.threema.storage.models.MessageModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bridge between Treema Light's simple UI and the full Threema services.
 * Provides a simplified facade for contacts and messaging.
 */
class ThreemaBridge(private val context: Context) {

    private val serviceManager: ServiceManager?
        get() = ThreemaApplication.getServiceManager()

    private val contactService: ContactService?
        get() = try {
            serviceManager?.contactService
        } catch (e: Exception) {
            null
        }

    private val messageService: MessageService?
        get() = try {
            serviceManager?.messageService
        } catch (e: Exception) {
            null
        }

    // =========================================================================
    // CONTACTS
    // =========================================================================

    /**
     * Get all contacts as a Flow for reactive updates.
     * Converts Threema's ContactModel to our simple Contact data class.
     */
    fun getContacts(): Flow<List<Contact>> = flow {
        val contacts = withContext(Dispatchers.IO) {
            contactService?.all?.map { it.toSimpleContact() } ?: emptyList()
        }
        emit(contacts)
    }

    /**
     * Get a single contact by Threema ID.
     */
    suspend fun getContact(threemaId: String): Contact? = withContext(Dispatchers.IO) {
        contactService?.getByIdentity(threemaId)?.toSimpleContact()
    }

    /**
     * Add a new contact by Threema ID.
     * Returns the created Contact or null if failed.
     */
    suspend fun addContact(threemaId: String): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            val contact = contactService?.createContactByIdentity(threemaId, true)
            if (contact != null) {
                Result.success(contact.toSimpleContact())
            } else {
                Result.failure(Exception("Could not create contact"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // MESSAGES
    // =========================================================================

    /**
     * Get all messages (simplified - inbox only).
     * In real implementation, this would filter by conversation.
     */
    fun getMessages(): Flow<List<Message>> = flow {
        val messages = withContext(Dispatchers.IO) {
            // Get messages from all 1:1 conversations
            val allMessages = mutableListOf<Message>()
            
            contactService?.all?.forEach { contact ->
                try {
                    // Get the message receiver for this contact
                    val receiver = contactService?.createReceiver(contact)
                    if (receiver != null) {
                        val recentMessages = messageService?.getMessagesForReceiver(receiver, null)
                        recentMessages?.mapNotNull { it.toSimpleMessage(contact) }?.let {
                            allMessages.addAll(it)
                        }
                    }
                } catch (e: Exception) {
                    // Skip this contact if we can't get messages
                }
            }
            
            // Sort by timestamp, newest first
            allMessages.sortedByDescending { it.timestamp }
        }
        emit(messages)
    }

    /**
     * Send a text message to a contact.
     */
    suspend fun sendMessage(contactId: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val contact = contactService?.getByIdentity(contactId)
            if (contact == null) {
                return@withContext Result.failure(Exception("Contact not found: $contactId"))
            }
            
            val receiver = contactService?.createReceiver(contact)
            if (receiver == null) {
                return@withContext Result.failure(Exception("Could not create message receiver"))
            }
            
            messageService?.sendText(text, receiver)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark a message as read.
     */
    suspend fun markAsRead(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Implementation would find and mark the message as read
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // CONNECTION
    // =========================================================================

    /**
     * Check if we're connected to Threema servers.
     */
    fun isConnected(): Boolean {
        return try {
            serviceManager?.connection?.isConnected ?: false
        } catch (e: Exception) {
            false
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private fun ContactModel.toSimpleContact(): Contact {
        return Contact(
            id = identity ?: "",
            name = getDisplayName() ?: identity ?: "Unknown",
            phoneNumber = "", // Could be fetched from linked contact
            isFavorite = true, // All contacts shown in simplified UI
            avatarColor = colorIndex?.toLong() ?: 0xFF4CAF50
        )
    }

    private fun MessageModel.toSimpleMessage(contact: ContactModel): Message? {
        val body = this.body ?: return null
        return Message(
            id = this.uid ?: "",
            senderId = if (this.isOutbox) "self" else contact.identity ?: "",
            senderName = if (this.isOutbox) "Ich" else contact.getDisplayName() ?: "Unknown",
            content = body,
            timestamp = this.createdAt?.time ?: System.currentTimeMillis(),
            isRead = this.isRead,
            isOutgoing = this.isOutbox
        )
    }

    private fun ContactModel.getDisplayName(): String? {
        return when {
            !firstName.isNullOrBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName"
            !firstName.isNullOrBlank() -> firstName
            !lastName.isNullOrBlank() -> lastName
            else -> identity
        }
    }
}

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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ch.threema.app.services.UserService
import ch.threema.domain.protocol.api.APIConnector
import ch.threema.data.repositories.ContactModelRepository
import ch.threema.app.asynctasks.BasicAddOrUpdateContactBackgroundTask
import ch.threema.app.asynctasks.AddContactRestrictionPolicy
import ch.threema.app.asynctasks.ContactCreated
import ch.threema.app.asynctasks.ContactAvailable
import ch.threema.domain.protocol.connection.ConnectionState
import ch.threema.storage.models.AbstractMessageModel
import ch.threema.storage.models.MessageModel

/**
 * Bridge between Treema Light's simple UI and the full Threema services.
 * Provides a simplified facade for contacts and messaging.
 */
class ThreemaBridge(private val context: Context) : KoinComponent {

    private val contactService: ContactService by inject()
    private val messageService: MessageService by inject()
    private val serviceManager: ServiceManager by inject()
    private val userService: UserService by inject()
    private val apiConnector: APIConnector by inject()
    private val contactModelRepository: ContactModelRepository by inject()

    // =========================================================================
    // CONTACTS
    // =========================================================================

    /**
     * Get all contacts as a Flow for reactive updates.
     * Converts Threema's ContactModel to our simple Contact data class.
     */
    fun getContacts(): Flow<List<Contact>> = flow {
        val contacts = withContext(Dispatchers.IO) {
            contactService.all.map { it.toSimpleContact() }
        }
        emit(contacts)
    }

    /**
     * Get a single contact by Threema ID.
     */
    suspend fun getContact(threemaId: String): Contact? = withContext(Dispatchers.IO) {
        contactService.getByIdentity(threemaId)?.toSimpleContact()
    }

    /**
     * Add a new contact by Threema ID.
     * Returns the created Contact or null if failed.
     */
    suspend fun addContact(threemaId: String): Result<Contact> = withContext(Dispatchers.IO) {
        try {
            // Check if exists first
            val existing = contactService.getByIdentity(threemaId)
            if (existing != null) {
                return@withContext Result.success(existing.toSimpleContact())
            }

            // Use Threema's background task to create contact
            val task = BasicAddOrUpdateContactBackgroundTask(
                threemaId,
                ContactModel.AcquaintanceLevel.DIRECT,
                userService.identity, // Assuming property access for getIdentity()
                apiConnector,
                contactModelRepository,
                AddContactRestrictionPolicy.CHECK,
                context, // Using context passed to constructor
                null
            )
            
            val result = task.runSynchronously()
            
            if (result is ContactCreated || result is ContactAvailable) {
                val newContact = contactService.getByIdentity(threemaId)
                if (newContact != null) {
                    Result.success(newContact.toSimpleContact())
                } else {
                    Result.failure(Exception("Contact created but not found"))
                }
            } else {
                Result.failure(Exception("Failed to create contact: $result"))
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
            
            contactService.all.forEach { contact ->
                try {
                    // Get the message receiver for this contact
                    val receiver = contactService.createReceiver(contact)
                    if (receiver != null) {
                        val recentMessages = messageService.getMessagesForReceiver(receiver, null)
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
            val contact = contactService.getByIdentity(contactId)
            if (contact == null) {
                return@withContext Result.failure(Exception("Contact not found: $contactId"))
            }
            
            val receiver = contactService.createReceiver(contact)
            if (receiver == null) {
                return@withContext Result.failure(Exception("Could not create message receiver"))
            }
            
            messageService.sendText(text, receiver)
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
             serviceManager.connection.connectionState == ConnectionState.LOGGEDIN
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
            avatarColor = this.getIdColor().colorIndex.toLong()
        )
    }

    private fun AbstractMessageModel.toSimpleMessage(contact: ContactModel): Message? {
        if (this !is MessageModel) return null // Only handle text messages for now
        
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
            !firstName.isNullOrBlank() -> "$firstName $lastName" // Simplification
            !firstName.isNullOrBlank() -> firstName
            !lastName.isNullOrBlank() -> lastName
            else -> identity
        }
    }
}

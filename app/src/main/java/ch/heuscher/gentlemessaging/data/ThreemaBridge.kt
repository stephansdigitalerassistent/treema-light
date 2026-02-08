package ch.heuscher.gentlemessaging.data

import android.content.Context
import ch.threema.app.ThreemaApplication
import ch.threema.app.managers.ServiceManager
import ch.threema.app.services.ContactService
import ch.threema.app.services.GroupService
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
import ch.threema.storage.models.GroupModel
import ch.threema.domain.models.IdentityState

/**
 * Bridge between Treema Light's simple UI and the full Threema services.
 * Now supports unified Chat Entries (Contacts & Groups).
 */
class ThreemaBridge(private val context: Context) : KoinComponent {

    private val contactService: ContactService by inject()
    private val groupService: GroupService by inject()
    private val messageService: MessageService by inject()
    private val serviceManager: ServiceManager by inject()
    private val userService: UserService by inject()
    private val apiConnector: APIConnector by inject()
    private val contactModelRepository: ContactModelRepository by inject()

    // =========================================================================
    // UNIFIED CHAT ENTRIES
    // =========================================================================

    /**
     * Represents a chat target (either a 1:1 Contact or a Group).
     */
    sealed class ChatEntry {
        abstract val id: String
        abstract val name: String
        abstract val avatarColor: Long
        
        data class ContactEntry(
            override val id: String,
            override val name: String,
            override val avatarColor: Long,
            val isFavorite: Boolean
        ) : ChatEntry()
        
        data class GroupEntry(
            override val id: String, // Group ID (api id or local id string)
            override val name: String,
            override val avatarColor: Long,
            val memberCount: Int
        ) : ChatEntry()
    }

    /**
     * Get all chat entries (Contacts + Groups) as a Flow.
     * Sorted alphabetically by name.
     */
    fun getChatEntries(): Flow<List<ChatEntry>> = flow {
        val entries = withContext(Dispatchers.IO) {
            val allEntries = mutableListOf<ChatEntry>()
            
            // 1. Fetch Contacts
            // Filter: Show only Active contacts that are NOT hidden (e.g. not just group members)
            // Note: identity is already a Threema ID here.
            val contacts = contactService.getAll()
                .filter { it.state == IdentityState.ACTIVE && !it.isHidden() }
                .map { it.toContactEntry() }
            allEntries.addAll(contacts)
            
            // 2. Fetch Groups
            val groups = groupService.getAll().map { it.toGroupEntry() }
            allEntries.addAll(groups)
            
            // 3. Sort by Name and return
            allEntries.sortedBy { it.name.lowercase() }
        }
        emit(entries)
    }

    // =========================================================================
    // CONTACTS (Legacy / Specific)
    // =========================================================================

    /**
     * Get a single contact by Threema ID.
     */
    suspend fun getContact(threemaId: String): ChatEntry.ContactEntry? = withContext(Dispatchers.IO) {
        contactService.getByIdentity(threemaId)?.toContactEntry()
    }

    /**
     * Add a new contact by Threema ID.
     */
    suspend fun addContact(threemaId: String): Result<ChatEntry.ContactEntry> = withContext(Dispatchers.IO) {
        try {
            val existing = contactService.getByIdentity(threemaId)
            if (existing != null) {
                return@withContext Result.success(existing.toContactEntry())
            }

            val myIdentity = userService.getIdentity()
                ?: return@withContext Result.failure(Exception("Local identity not available"))

            val task = BasicAddOrUpdateContactBackgroundTask(
                threemaId,
                ContactModel.AcquaintanceLevel.DIRECT,
                myIdentity,
                apiConnector,
                contactModelRepository,
                AddContactRestrictionPolicy.CHECK,
                context,
                null
            )
            
            val result = task.runSynchronously()
            
            if (result is ContactCreated || result is ContactAvailable) {
                val newContact = contactService.getByIdentity(threemaId)
                if (newContact != null) {
                    Result.success(newContact.toContactEntry())
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
     * Get messages for a specific chat (Contact or Group).
     */
    fun getMessages(chatId: String, isGroup: Boolean): Flow<List<Message>> = flow {
        val messages = withContext(Dispatchers.IO) {
            val simpleMessages = mutableListOf<Message>()
            
            if (isGroup) {
                // Determine if chatId is integer group ID or API ID? 
                // GroupService usually uses integer internal ID for getting model
                // We'll try to find the group first
                val group = try {
                    // Try parsing as int (local ID) first, if fails assume it's API ID (rare for groups in this context)
                    groupService.getById(chatId.toInt()) 
                } catch (e: NumberFormatException) {
                    null 
                }
                
                if (group != null) {
                    val receiver = groupService.createReceiver(group)
                     messageService.getMessagesForReceiver(receiver)?.mapNotNull { 
                        // For groups, we need sender info which might be complex to resolve fully here
                        // For now simplified
                        it.toSimpleMessage() 
                    }?.let { simpleMessages.addAll(it) }
                }
            } else {
                // 1:1 Contact
                val contact = contactService.getByIdentity(chatId)
                if (contact != null) {
                    val receiver = contactService.createReceiver(contact)
                    if (receiver != null) {
                        messageService.getMessagesForReceiver(receiver)?.mapNotNull { it.toSimpleMessage() }
                            ?.let { simpleMessages.addAll(it) }
                    }
                }
            }
            
            // Sort by timestamp, newest first
            simpleMessages.sortedByDescending { it.timestamp }
        }
        emit(messages)
    }

    /**
     * Send a text message to a contact or group.
     */
    suspend fun sendMessage(chatId: String, text: String, isGroup: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (userService.getIdentity() == null) {
             return@withContext Result.failure(Exception("Local identity not available"))
        }

        try {
            if (isGroup) {
                val group = try { groupService.getById(chatId.toInt()) } catch (e: NumberFormatException) { null }
                    ?: return@withContext Result.failure(Exception("Group not found: $chatId"))
                
                val receiver = groupService.createReceiver(group)
                messageService.sendText(text, receiver)
                Result.success(Unit)
            } else {
                val contact = contactService.getByIdentity(chatId)
                    ?: return@withContext Result.failure(Exception("Contact not found: $chatId"))
                
                val receiver = contactService.createReceiver(contact)
                    ?: return@withContext Result.failure(Exception("Could not create message receiver"))
                
                messageService.sendText(text, receiver)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private fun ContactModel.toContactEntry(): ChatEntry.ContactEntry {
        return ChatEntry.ContactEntry(
            id = identity ?: "",
            name = getDisplayName() ?: identity ?: "Unknown",
            avatarColor = this.idColor.colorIndex.toLong(),
            isFavorite = true // Simplified
        )
    }
    
    private fun GroupModel.toGroupEntry(): ChatEntry.GroupEntry {
        return ChatEntry.GroupEntry(
            id = this.id.toString(), // Use local ID for easier retrieval
            name = this.name ?: "Unbenannte Gruppe",
            avatarColor = 0xFF6200EE, // Use a distinct color for groups if no image
            memberCount = groupService.countMembers(this)
        )
    }

    private fun AbstractMessageModel.toSimpleMessage(): Message? {
        if (this !is MessageModel) return null
        val body = this.body ?: return null
        
        // Simplified sender resolution
        val senderId = if (this.isOutbox) "self" else this.identity?.toString() ?: "unknown"
        val senderName = if (this.isOutbox) "Ich" else {
            this.identity?.let { id ->
                contactService.getByIdentity(id.toString())?.firstName ?: "Jemand"
            } ?: "Jemand"
        }
        
        return Message(
            id = this.uid ?: "",
            senderId = senderId,
            senderName = senderName,
            content = body,
            timestamp = this.createdAt?.time ?: System.currentTimeMillis(),
            isRead = this.isRead,
            isOutgoing = this.isOutbox
        )
    }

    private fun ContactModel.getDisplayName(): String? {
        return when {
            !firstName.isNullOrBlank() -> "$firstName $lastName"
            !firstName.isNullOrBlank() -> firstName
            !lastName.isNullOrBlank() -> lastName
            else -> identity
        }
    }
}

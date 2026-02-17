package ch.heuscher.gentlemessaging.data

import android.content.Context
import ch.threema.app.ThreemaApplication
import ch.threema.app.managers.ListenerManager
import ch.threema.app.managers.ServiceManager
import ch.threema.app.listeners.ContactListener
import ch.threema.app.listeners.MessageListener
import ch.threema.app.services.ContactService
import ch.threema.app.services.GroupService
import ch.threema.app.services.MessageService
import ch.threema.storage.models.ContactModel
import ch.threema.storage.models.MessageModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
import ch.threema.storage.models.ReceiverModel
import ch.threema.domain.models.IdentityState

/**
 * Bridge between gentle messaging's simple UI and the full Threema services.
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
    private val gentlePrefs = GentlePreferences.getInstance(context)

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
        abstract val receiverModel: ReceiverModel
        
        data class ContactEntry(
            override val id: String,
            override val name: String,
            override val avatarColor: Long,
            override val receiverModel: ReceiverModel,
            val isFavorite: Boolean
        ) : ChatEntry()
        
        data class GroupEntry(
            override val id: String, // Group ID (api id or local id string)
            override val name: String,
            override val avatarColor: Long,
            override val receiverModel: ReceiverModel,
            val memberCount: Int,
            val members: List<ContactModel> = emptyList(),
            val isFavorite: Boolean = false
        ) : ChatEntry()
    }

    /**
     * Get all chat entries (Contacts + Groups) as a Flow.
     * Sorted alphabetically by name.
     */
    private suspend fun fetchChatEntries(): List<ChatEntry> = withContext(Dispatchers.IO) {
        val allEntries = mutableListOf<ChatEntry>()
        
        // 1. Fetch Contacts
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

    fun getChatEntries(): Flow<List<ChatEntry>> = callbackFlow {
        // Emit initial list
        trySend(fetchChatEntries())
        
        // Re-emit on new/modified/removed messages (updates last message etc.)
        val messageListener = object : MessageListener {
            override fun onNew(newMessage: AbstractMessageModel) {
                trySend(emptyList()) // trigger recomposition; actual data follows
                kotlinx.coroutines.runBlocking { trySend(fetchChatEntries()) }
            }
            override fun onModified(modifiedMessageModel: MutableList<AbstractMessageModel>) {
                kotlinx.coroutines.runBlocking { trySend(fetchChatEntries()) }
            }
            override fun onRemoved(removedMessageModel: AbstractMessageModel) {
                kotlinx.coroutines.runBlocking { trySend(fetchChatEntries()) }
            }
        }
        
        // Re-emit on contact changes
        val contactListener = object : ContactListener {
            override fun onNew(identity: String) {
                kotlinx.coroutines.runBlocking { trySend(fetchChatEntries()) }
            }
            override fun onModified(identity: String) {
                kotlinx.coroutines.runBlocking { trySend(fetchChatEntries()) }
            }
            override fun onRemoved(identity: String) {
                kotlinx.coroutines.runBlocking { trySend(fetchChatEntries()) }
            }
        }
        
        ListenerManager.messageListeners.add(messageListener)
        ListenerManager.contactListeners.add(contactListener)
        
        awaitClose {
            ListenerManager.messageListeners.remove(messageListener)
            ListenerManager.contactListeners.remove(contactListener)
        }
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
    private suspend fun fetchMessages(chatId: String, isGroup: Boolean): List<Message> = withContext(Dispatchers.IO) {
        val simpleMessages = mutableListOf<Message>()
        
        if (isGroup) {
            val group = try {
                groupService.getById(chatId.toInt()) 
            } catch (e: NumberFormatException) {
                null 
            }
            
            if (group != null) {
                val receiver = groupService.createReceiver(group)
                messageService.getMessagesForReceiver(receiver)?.mapNotNull { 
                    it.toSimpleMessage() 
                }?.let { simpleMessages.addAll(it) }
            }
        } else {
            val contact = contactService.getByIdentity(chatId)
            if (contact != null) {
                val receiver = contactService.createReceiver(contact)
                if (receiver != null) {
                    messageService.getMessagesForReceiver(receiver)?.mapNotNull { it.toSimpleMessage() }
                        ?.let { simpleMessages.addAll(it) }
                }
            }
        }
        
        simpleMessages.sortedByDescending { it.timestamp }
    }

    fun getMessages(chatId: String, isGroup: Boolean): Flow<List<Message>> = callbackFlow {
        // Emit initial messages
        trySend(fetchMessages(chatId, isGroup))
        
        // Listen for real-time message updates
        val listener = object : MessageListener {
            override fun onNew(newMessage: AbstractMessageModel) {
                kotlinx.coroutines.runBlocking { trySend(fetchMessages(chatId, isGroup)) }
            }
            override fun onModified(modifiedMessageModel: MutableList<AbstractMessageModel>) {
                kotlinx.coroutines.runBlocking { trySend(fetchMessages(chatId, isGroup)) }
            }
            override fun onRemoved(removedMessageModel: AbstractMessageModel) {
                kotlinx.coroutines.runBlocking { trySend(fetchMessages(chatId, isGroup)) }
            }
        }
        
        ListenerManager.messageListeners.add(listener)
        
        awaitClose {
            ListenerManager.messageListeners.remove(listener)
        }
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

    /** Toggle favorite status for a contact or group */
    fun toggleFavorite(id: String): Boolean {
        return gentlePrefs.toggleFavorite(id)
    }

    private fun ContactModel.toContactEntry(): ChatEntry.ContactEntry {
        val contactId = identity ?: ""
        return ChatEntry.ContactEntry(
            id = contactId,
            name = getDisplayName() ?: identity ?: "Unknown",
            avatarColor = this.idColor.colorIndex.toLong(),
            receiverModel = this,
            isFavorite = gentlePrefs.isFavorite(contactId)
        )
    }
    
    private fun GroupModel.toGroupEntry(): ChatEntry.GroupEntry {
        val groupId = this.id.toString()
        val memberModels = groupService.getMembers(this).take(3) // First 3 members for avatars
        return ChatEntry.GroupEntry(
            id = groupId, // Use local ID for easier retrieval
            name = this.name ?: "Unbenannte Gruppe",
            avatarColor = 0xFF6200EE, // Use a distinct color for groups if no image
            receiverModel = this,
            memberCount = groupService.countMembers(this),
            members = memberModels,
            isFavorite = gentlePrefs.isFavorite(groupId)
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
            !firstName.isNullOrBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName"
            !firstName.isNullOrBlank() -> firstName
            !lastName.isNullOrBlank() -> lastName
            else -> identity
        }
    }
}

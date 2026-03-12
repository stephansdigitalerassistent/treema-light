package ch.threema.cli.stores

import ch.threema.domain.models.BasicContact
import ch.threema.domain.models.Contact
import ch.threema.domain.stores.ContactStore

class CliContactStore : ContactStore {
    private val contacts = mutableMapOf<String, Contact>()
    private val contactsCache = mutableMapOf<String, BasicContact>()

    override fun getContactForIdentity(identity: String): Contact? {
        return contacts[identity]
    }

    override fun addContact(contact: Contact) {
        contacts[contact.identity] = contact
    }

    override fun addCachedContact(contact: BasicContact) {
        contactsCache[contact.identity] = contact
    }

    override fun getCachedContact(identity: String): BasicContact? {
        return contactsCache[identity]
    }

    override fun getContactForIdentityIncludingCache(identity: String): Contact? {
        val cached = contactsCache[identity]
        if (cached is Contact) {
            return cached
        }
        return getContactForIdentity(identity)
    }

    override fun isSpecialContact(identity: String): Boolean {
        return false
    }
}

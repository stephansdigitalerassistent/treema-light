package ch.threema.cli.stores

import ch.threema.base.crypto.NaCl
import ch.threema.domain.stores.IdentityStore
import ch.threema.domain.types.Identity

class CliIdentityStore(
    private var identity: Identity? = null,
    private var serverGroup: String? = null,
    private var privateKey: ByteArray? = null,
    private var publicNickname: String = ""
) : IdentityStore {

    private var publicKey: ByteArray? = null

    init {
        privateKey?.let {
            publicKey = NaCl.derivePublicKey(it)
        }
    }

    override fun encryptData(plaintext: ByteArray, nonce: ByteArray, receiverPublicKey: ByteArray): ByteArray? {
        val key = privateKey ?: return null
        val nacl = NaCl(key, receiverPublicKey)
        return try {
            nacl.encrypt(plaintext, nonce)
        } catch (e: Exception) {
            null
        }
    }

    override fun decryptData(ciphertext: ByteArray, nonce: ByteArray, senderPublicKey: ByteArray): ByteArray? {
        val key = privateKey ?: return null
        val nacl = NaCl(key, senderPublicKey)
        return try {
            nacl.decrypt(ciphertext, nonce)
        } catch (e: Exception) {
            null
        }
    }

    override fun calcSharedSecret(publicKey: ByteArray): ByteArray {
        val key = privateKey ?: throw IllegalStateException("Private key not set")
        val nacl = NaCl(key, publicKey)
        return nacl.sharedSecret
    }

    override fun getIdentity(): Identity? = identity

    override fun getServerGroup(): String? = null

    override fun getPublicKey(): ByteArray? = publicKey

    override fun getPrivateKey(): ByteArray? = privateKey

    override fun getPublicNickname(): String = publicNickname

    override fun setPublicNickname(publicNickname: String) {
        this.publicNickname = publicNickname
    }

    override fun storeIdentity(identity: String, serverGroup: String, privateKey: ByteArray) {
        this.identity = identity
        this.serverGroup = serverGroup
        this.privateKey = privateKey
        this.publicKey = NaCl.derivePublicKey(privateKey)
    }

    override fun clear() {
        identity = null
        serverGroup = null
        privateKey = null
        publicKey = null
        publicNickname = ""
    }
}

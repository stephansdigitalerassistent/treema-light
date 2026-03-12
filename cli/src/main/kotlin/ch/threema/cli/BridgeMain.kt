package ch.threema.cli

import ch.threema.cli.stores.CliContactStore
import ch.threema.cli.stores.CliDHSessionStore
import ch.threema.cli.stores.CliIdentityStore
import ch.threema.common.DispatcherProvider
import ch.threema.domain.protocol.ServerAddressProvider
import ch.threema.domain.protocol.Version
import ch.threema.domain.protocol.connection.ConnectionLock
import ch.threema.domain.protocol.connection.ConnectionLockProvider
import ch.threema.domain.protocol.connection.BaseServerConnectionProvider
import ch.threema.domain.protocol.connection.csp.CspConnectionConfiguration
import ch.threema.domain.protocol.connection.csp.DeviceCookieManager
import ch.threema.domain.protocol.connection.csp.socket.SocketFactory
import ch.threema.domain.protocol.connection.data.CspMessage
import ch.threema.domain.protocol.connection.data.InboundD2mMessage
import ch.threema.domain.protocol.connection.data.InboundMessage
import ch.threema.domain.protocol.connection.socket.ServerSocketCloseReason
import ch.threema.domain.protocol.csp.coders.MessageBox
import ch.threema.domain.taskmanager.ActiveTaskCodec
import ch.threema.domain.taskmanager.IncomingMessageProcessor
import ch.threema.domain.taskmanager.QueueSendCompleteListener
import ch.threema.domain.taskmanager.Task
import ch.threema.domain.taskmanager.TaskArchiver
import ch.threema.domain.taskmanager.TaskCodec
import ch.threema.domain.taskmanager.TaskManager
import ch.threema.domain.taskmanager.TaskManagerConfiguration
import ch.threema.domain.taskmanager.TaskManagerProvider
import ch.threema.domain.protocol.urls.BlobUrl
import ch.threema.domain.protocol.urls.DeviceGroupUrl
import ch.threema.domain.protocol.urls.MapPoiAroundUrl
import ch.threema.domain.protocol.urls.MapPoiNamesUrl
import ch.threema.domain.protocol.urls.AppRatingUrl
import ch.threema.domain.protocol.connection.d2m.MultiDevicePropertyProvider
import ch.threema.domain.protocol.connection.csp.socket.HostResolver
import ch.threema.domain.stores.ContactStore
import ch.threema.domain.stores.DHSessionStoreInterface
import ch.threema.domain.stores.IdentityStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.net.InetAddress
import java.net.Socket

class ProductionServerAddressProvider : ServerAddressProvider {
    private fun bytes(vararg byte: Int): ByteArray = byte.map { it.toByte() }.toByteArray()

    private val prodServer = bytes(0x45, 0x0b, 0x97, 0x57, 0x35, 0x27, 0x9f, 0xde, 0xcb, 0x33, 0x13, 0x64, 0x8f, 0x5f, 0xc6, 0xee, 0x9f, 0xf4, 0x36, 0x0e, 0xa9, 0x2a, 0x8c, 0x17, 0x51, 0xc6, 0x61, 0xe4, 0xc0, 0xd8, 0xc9, 0x09)
    private val prodServerAlt = bytes(0xda, 0x7c, 0x73, 0x79, 0x8f, 0x97, 0xd5, 0x87, 0xc3, 0xa2, 0x5e, 0xbe, 0x0a, 0x91, 0x41, 0x7f, 0x76, 0xdb, 0xcc, 0xcd, 0xda, 0x29, 0x30, 0xe6, 0xa9, 0x09, 0x0a, 0xf6, 0x2e, 0xba, 0x6f, 0x15)

    override fun getChatServerNamePrefix(ipv6: Boolean): String = "ds.g-16.0"
    override fun getChatServerNameSuffix(ipv6: Boolean): String = "threema.ch"
    override fun getChatServerPorts(): IntArray = intArrayOf(5222, 443)
    override fun getChatServerUseServerGroups(): Boolean = false
    override fun getChatServerPublicKey(): ByteArray = prodServer
    override fun getChatServerPublicKeyAlt(): ByteArray = prodServerAlt
    
    override fun getDirectoryServerUrl(ipv6: Boolean): String = "https://ds-apip.threema.ch/"
    override fun getWorkServerUrl(ipv6: Boolean): String? = null
    override fun getBlobServerDownloadUrl(useIpV6: Boolean): BlobUrl = BlobUrl("https://blob.threema.ch/{blobId}")
    override fun getBlobServerUploadUrl(useIpV6: Boolean): String = "https://blob.threema.ch/upload"
    override fun getBlobServerDoneUrl(useIpV6: Boolean): BlobUrl = BlobUrl("https://blob.threema.ch/{blobId}/done")
    override fun getBlobMirrorServerDownloadUrl(multiDevicePropertyProvider: MultiDevicePropertyProvider): BlobUrl = BlobUrl("")
    override fun getBlobMirrorServerUploadUrl(multiDevicePropertyProvider: MultiDevicePropertyProvider): String = ""
    override fun getBlobMirrorServerDoneUrl(multiDevicePropertyProvider: MultiDevicePropertyProvider): BlobUrl = BlobUrl("")
    override fun getAvatarServerUrl(ipv6: Boolean): String = "https://avatar.threema.ch/"
    override fun getSafeServerUrl(ipv6: Boolean): String = "https://safe.threema.ch/"
    override fun getWebServerUrl(): String? = null
    override fun getWebOverrideSaltyRtcHost(): String? = null
    override fun getWebOverrideSaltyRtcPort(): Int = 0
    override fun getThreemaPushPublicKey(): ByteArray? = null
    override fun getMediatorUrl(): DeviceGroupUrl = DeviceGroupUrl("https://md.threema.ch/")
    override fun getAppRatingUrl(): AppRatingUrl = AppRatingUrl("")
    override fun getMapStyleUrl(): String? = null
    override fun getMapPoiNamesUrl(): MapPoiNamesUrl? = null
    override fun getMapPoiAroundUrl(): MapPoiAroundUrl? = null
}

class JvmHostResolver : HostResolver {
    override fun getAllByName(name: String): Array<InetAddress> = InetAddress.getAllByName(name)
}

class DummyDeviceCookieManager : DeviceCookieManager {
    override fun obtainDeviceCookie(): ByteArray = ByteArray(16)
    override fun changeIndicationReceived() {}
    override fun deleteDeviceCookie() {}
}

fun main(args: Array<String>) {
    println("Starting Threema Kotlin Bridge...")
    
    val identityId = "A62B5XJ3"
    val clientKeyHex = "026bae3512223a4f60a63e0d19b0fb7d36307b6b23271e1889fb7ab43082b84f"
    val privateKey = clientKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    val identityStore = CliIdentityStore()
    identityStore.storeIdentity(identityId, "16", privateKey)
    
    val contactStore = CliContactStore()
    val dhSessionStore = CliDHSessionStore()

    // Initialize Koin
    startKoin {
        modules(module {
            single<IdentityStore> { identityStore }
            single<ContactStore> { contactStore }
            single<DHSessionStoreInterface> { dhSessionStore }
            single { DispatcherProvider.default }
        })
    }

    val incomingMessageProcessor = object : IncomingMessageProcessor {
        override suspend fun processIncomingCspMessage(messageBox: MessageBox, handle: ActiveTaskCodec) {
            println("Received message box: $messageBox")
        }
        override suspend fun processIncomingD2mMessage(message: InboundD2mMessage.Reflected, handle: ActiveTaskCodec) {}
        override fun processIncomingServerAlert(alertData: CspMessage.ServerAlertData) {}
        override fun processIncomingServerError(errorData: CspMessage.ServerErrorData) {}
    }
    
    val taskManager = TaskManagerProvider.getTaskManager(
        TaskManagerConfiguration(
            {
                object : TaskArchiver {
                    override fun addTask(task: Task<*, TaskCodec>) {}
                    override fun removeTask(task: Task<*, TaskCodec>) {}
                    override fun loadAllTasks(): List<Task<*, TaskCodec>> = emptyList()
                }
            },
            DummyDeviceCookieManager(),
            true
        )
    )

    val connectionLockProvider = object : ConnectionLockProvider {
        override fun acquire(timeoutMillis: Long, tag: ConnectionLockProvider.ConnectionLogTag): ConnectionLock {
            return object : ConnectionLock {
                override fun release() {}
                override fun isHeld() = false
            }
        }
    }

    val configuration = CspConnectionConfiguration(
        identityStore = identityStore,
        serverAddressProvider = ProductionServerAddressProvider(),
        version = Version(),
        assertDispatcherContext = false,
        deviceCookieManager = DummyDeviceCookieManager(),
        incomingMessageProcessor = incomingMessageProcessor,
        taskManager = taskManager,
        hostResolver = JvmHostResolver(),
        ipv6 = false,
        socketFactory = SocketFactory { Socket() }
    )

    val connection = BaseServerConnectionProvider.createConnection(configuration, connectionLockProvider)

    runBlocking {
        println("Connecting to Threema server...")
        try {
            connection.start()
            println("Connected! Waiting for messages...")
            // Keep running
            while(true) {
                kotlinx.coroutines.delay(1000)
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}

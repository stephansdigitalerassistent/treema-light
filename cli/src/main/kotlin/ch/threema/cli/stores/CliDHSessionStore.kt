package ch.threema.cli.stores

import ch.threema.domain.fs.DHSession
import ch.threema.domain.fs.DHSessionId
import ch.threema.domain.stores.DHSessionStoreInterface
import ch.threema.domain.taskmanager.ActiveTaskCodec

class CliDHSessionStore : DHSessionStoreInterface {
    private val dhSessionList = mutableListOf<DHSession>()

    override fun getDHSession(
        myIdentity: String,
        peerIdentity: String,
        sessionId: DHSessionId,
        handle: ActiveTaskCodec
    ): DHSession? {
        return dhSessionList.find {
            it.myIdentity == myIdentity &&
            it.peerIdentity == peerIdentity &&
            it.id == sessionId
        }
    }

    override fun getBestDHSession(
        myIdentity: String,
        peerIdentity: String,
        handle: ActiveTaskCodec
    ): DHSession? {
        var currentBestSession: DHSession? = null

        for (session in dhSessionList) {
            if (session.myIdentity != myIdentity || session.peerIdentity != peerIdentity) {
                continue
            }

            if (currentBestSession == null ||
                (currentBestSession.myRatchet4DH == null && session.myRatchet4DH != null) ||
                (session.myRatchet4DH != null && currentBestSession.id > session.id)
            ) {
                currentBestSession = session
            }
        }

        return currentBestSession
    }

    override fun getAllDHSessions(
        myIdentity: String,
        peerIdentity: String,
        handle: ActiveTaskCodec
    ): List<DHSession> {
        return dhSessionList.filter {
            it.myIdentity == myIdentity && it.peerIdentity == peerIdentity
        }
    }

    override fun storeDHSession(session: DHSession) {
        deleteDHSession(session.myIdentity, session.peerIdentity, session.id)
        dhSessionList.add(session)
    }

    override fun deleteDHSession(
        myIdentity: String,
        peerIdentity: String,
        sessionId: DHSessionId
    ): Boolean {
        val iterator = dhSessionList.iterator()
        while (iterator.hasNext()) {
            val curSession = iterator.next()
            if (curSession.myIdentity == myIdentity &&
                curSession.peerIdentity == peerIdentity &&
                curSession.id == sessionId
            ) {
                iterator.remove()
                return true
            }
        }
        return false
    }

    override fun deleteAllDHSessions(myIdentity: String, peerIdentity: String): Int {
        var numDeleted = 0
        val iterator = dhSessionList.iterator()
        while (iterator.hasNext()) {
            val session = iterator.next()
            if (session.myIdentity == myIdentity && session.peerIdentity == peerIdentity) {
                iterator.remove()
                numDeleted++
            }
        }
        return numDeleted
    }

    override fun deleteAllSessionsExcept(
        myIdentity: String,
        peerIdentity: String,
        exceptSessionId: DHSessionId,
        fourDhOnly: Boolean
    ): Int {
        var numDeleted = 0
        val iterator = dhSessionList.iterator()
        while (iterator.hasNext()) {
            val session = iterator.next()
            if (session.myIdentity == myIdentity &&
                session.peerIdentity == peerIdentity &&
                (!fourDhOnly || session.myRatchet4DH != null) &&
                session.id != exceptSessionId
            ) {
                iterator.remove()
                numDeleted++
            }
        }
        return numDeleted
    }

    override fun setDHSessionStoreErrorHandler(errorHandler: DHSessionStoreInterface.DHSessionStoreErrorHandler) {
        // Nothing to do here
    }

    override fun executeNull() {
        // Nothing to do
    }

    override fun close() {
        // Nothing to do
    }
}

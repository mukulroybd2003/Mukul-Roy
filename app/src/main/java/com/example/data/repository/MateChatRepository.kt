package com.example.data.repository

import android.content.Context
import com.example.crypto.CryptoEngine
import com.example.data.local.AddaRoomEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CallHistoryEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MateChatRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val callHistoryDao = db.callHistoryDao()
    private val addaRoomDao = db.addaRoomDao()

    val conversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    val callHistory: Flow<List<CallHistoryEntity>> = callHistoryDao.getAllCalls()
    val liveAddaRooms: Flow<List<AddaRoomEntity>> = addaRoomDao.getLiveRooms()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
        }
    }

    fun getMessagesForConversation(convId: Long): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForConversation(convId)
    }

    suspend fun sendMessage(
        conversationId: Long,
        senderId: Long,
        senderName: String,
        plainText: String,
        peerPublicKey: String?,
        messageType: String = "text",
        voiceDuration: Int? = null
    ): MessageEntity {
        val targetPub = peerPublicKey ?: CryptoEngine.getLocalPublicKeyBase64()
        val encrypted = CryptoEngine.encryptText(plainText, targetPub)
        val timeNow = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        val msg = MessageEntity(
            id = System.currentTimeMillis(),
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            messageType = messageType,
            ciphertextBase64 = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            authTagBase64 = encrypted.authTagBase64,
            decryptedText = plainText,
            attachmentUrl = null,
            voiceDurationSeconds = voiceDuration,
            isOutgoing = true,
            status = "delivered",
            reactionEmoji = null,
            timestamp = timeNow
        )

        messageDao.insertMessage(msg)

        // Update conversation last message
        val conv = conversationDao.getConversationById(conversationId)
        if (conv != null) {
            val preview = when (messageType) {
                "voice_note" -> "🎙️ Voice message (${voiceDuration ?: 5}s)"
                "image" -> "📷 Photo"
                else -> plainText
            }
            conversationDao.insertOrUpdate(
                conv.copy(
                    lastMessageText = preview,
                    lastMessageTimestamp = timeNow
                )
            )
        }

        return msg
    }

    suspend fun logCall(
        peerName: String,
        callType: String,
        isIncoming: Boolean,
        status: String,
        durationSeconds: Int
    ) {
        val timeNow = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())
        callHistoryDao.insertCall(
            CallHistoryEntity(
                channelName = "call_" + System.currentTimeMillis(),
                peerName = peerName,
                peerAvatar = null,
                callType = callType,
                isIncoming = isIncoming,
                status = status,
                durationSeconds = durationSeconds,
                timestamp = timeNow
            )
        )
    }

    suspend fun reactToMessage(messageId: Long, emoji: String?) {
        messageDao.updateReaction(messageId, emoji)
    }

    suspend fun markConversationRead(conversationId: Long) {
        conversationDao.markAsRead(conversationId)
    }

    private suspend fun seedInitialDataIfNeeded() {
        val dummyPub = CryptoEngine.getLocalPublicKeyBase64()

        // Seed conversations if empty
        val seedConvs = listOf(
            ConversationEntity(
                id = 1L,
                type = "direct",
                title = null,
                avatarUrl = null,
                lastMessageText = "The double ratchet is working seamlessly across both Web and Android.",
                lastMessageTimestamp = "10:44 AM",
                unreadCount = 0,
                isPinned = true,
                isMuted = false,
                peerUserId = 2L,
                peerFullName = "Sophia Zhang",
                peerUsername = "sophia_code",
                peerOnline = true,
                peerPublicKey = dummyPub
            ),
            ConversationEntity(
                id = 2L,
                type = "direct",
                title = null,
                avatarUrl = null,
                lastMessageText = "Check out the new M3 UI token designs ✨",
                lastMessageTimestamp = "Yesterday",
                unreadCount = 2,
                isPinned = false,
                isMuted = false,
                peerUserId = 3L,
                peerFullName = "Marcus Vance",
                peerUsername = "marcus_design",
                peerOnline = false,
                peerPublicKey = dummyPub
            ),
            ConversationEntity(
                id = 3L,
                type = "group",
                title = "MATE Core Engineering",
                avatarUrl = null,
                lastMessageText = "Alex: Security audit passed with zero vulnerabilities! 🛡️",
                lastMessageTimestamp = "Nov 6",
                unreadCount = 0,
                isPinned = false,
                isMuted = false,
                peerUserId = null,
                peerFullName = null,
                peerUsername = null,
                peerOnline = false,
                peerPublicKey = dummyPub
            )
        )
        conversationDao.insertAll(seedConvs)

        // Seed initial encrypted messages for conv 1
        val enc1 = CryptoEngine.encryptText("Hey Alex! Did you review the Curve25519 ratchet rotation?", dummyPub)
        val enc2 = CryptoEngine.encryptText("Yes! The double ratchet is working seamlessly across both Web and Android.", dummyPub)

        val seedMsgs = listOf(
            MessageEntity(
                id = 101L,
                conversationId = 1L,
                senderId = 2L,
                senderName = "Sophia Zhang",
                messageType = "text",
                ciphertextBase64 = enc1.ciphertextBase64,
                ivBase64 = enc1.ivBase64,
                authTagBase64 = enc1.authTagBase64,
                decryptedText = "Hey Alex! Did you review the Curve25519 ratchet rotation?",
                attachmentUrl = null,
                voiceDurationSeconds = null,
                isOutgoing = false,
                status = "read",
                reactionEmoji = "🔥",
                timestamp = "10:42 AM"
            ),
            MessageEntity(
                id = 102L,
                conversationId = 1L,
                senderId = 1L,
                senderName = "Alex Rivers",
                messageType = "text",
                ciphertextBase64 = enc2.ciphertextBase64,
                ivBase64 = enc2.ivBase64,
                authTagBase64 = enc2.authTagBase64,
                decryptedText = "Yes! The double ratchet is working seamlessly across both Web and Android.",
                attachmentUrl = null,
                voiceDurationSeconds = null,
                isOutgoing = true,
                status = "read",
                reactionEmoji = "🚀",
                timestamp = "10:44 AM"
            )
        )
        messageDao.insertMessages(seedMsgs)

        // Seed Call History
        callHistoryDao.insertCall(
            CallHistoryEntity(
                id = 1L,
                channelName = "webrtc_call_001",
                peerName = "Sophia Zhang",
                peerAvatar = null,
                callType = "video",
                isIncoming = true,
                status = "completed",
                durationSeconds = 342,
                timestamp = "Today, 9:15 AM"
            )
        )
        callHistoryDao.insertCall(
            CallHistoryEntity(
                id = 2L,
                channelName = "webrtc_call_002",
                peerName = "Marcus Vance",
                peerAvatar = null,
                callType = "audio",
                isIncoming = false,
                status = "missed",
                durationSeconds = 0,
                timestamp = "Yesterday, 4:20 PM"
            )
        )

        // Seed Adda Live Rooms
        addaRoomDao.insertRooms(
            listOf(
                AddaRoomEntity(
                    id = 1L,
                    title = "Cybersecurity & Cryptography Cafe 🔐",
                    topic = "Signal Protocol, X3DH, and MLS group ratchets discussion",
                    hostName = "Alex Rivers",
                    speakerCount = 4,
                    listenerCount = 42,
                    isLive = true
                ),
                AddaRoomEntity(
                    id = 2L,
                    title = "Kotlin & Jetpack Compose Stage 🚀",
                    topic = "M3 design system, state hoisting, and smooth animations",
                    hostName = "Sophia Zhang",
                    speakerCount = 2,
                    listenerCount = 18,
                    isLive = true
                ),
                AddaRoomEntity(
                    id = 3L,
                    title = "Open Mic Chill Adda ☕",
                    topic = "Weekend stories, favorite music, and general chat",
                    hostName = "Marcus Vance",
                    speakerCount = 3,
                    listenerCount = 65,
                    isLive = true
                )
            )
        )
    }
}

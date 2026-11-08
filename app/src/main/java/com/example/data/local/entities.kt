package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val username: String,
    val email: String,
    val fullName: String,
    val avatarUrl: String?,
    val bio: String?,
    val isOnline: Boolean,
    val lastSeen: String?,
    val publicIdentityKey: String?
)

@Entity(tableName = "cached_conversations")
data class ConversationEntity(
    @PrimaryKey val id: Long,
    val type: String, // "direct" or "group"
    val title: String?,
    val avatarUrl: String?,
    val lastMessageText: String?,
    val lastMessageTimestamp: String,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val peerUserId: Long?,
    val peerFullName: String?,
    val peerUsername: String?,
    val peerOnline: Boolean,
    val peerPublicKey: String?
)

@Entity(tableName = "cached_messages")
data class MessageEntity(
    @PrimaryKey val id: Long,
    val conversationId: Long,
    val senderId: Long,
    val senderName: String,
    val messageType: String, // "text", "image", "voice_note", "document", "system"
    val ciphertextBase64: String,
    val ivBase64: String,
    val authTagBase64: String,
    val decryptedText: String?,
    val attachmentUrl: String?,
    val voiceDurationSeconds: Int?,
    val isOutgoing: Boolean,
    val status: String, // "sending", "sent", "delivered", "read", "failed"
    val reactionEmoji: String?,
    val timestamp: String
)

@Entity(tableName = "call_history")
data class CallHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelName: String,
    val peerName: String,
    val peerAvatar: String?,
    val callType: String, // "audio" or "video"
    val isIncoming: Boolean,
    val status: String, // "completed", "missed", "rejected"
    val durationSeconds: Int,
    val timestamp: String
)

@Entity(tableName = "adda_rooms")
data class AddaRoomEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val topic: String,
    val hostName: String,
    val speakerCount: Int,
    val listenerCount: Int,
    val isLive: Boolean
)

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM cached_conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM cached_conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversations: List<ConversationEntity>)

    @Query("UPDATE cached_conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun markAsRead(id: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM cached_messages WHERE conversationId = :convId ORDER BY id ASC")
    fun getMessagesForConversation(convId: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE cached_messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE cached_messages SET reactionEmoji = :emoji WHERE id = :id")
    suspend fun updateReaction(id: Long, emoji: String?)
}

@Dao
interface CallHistoryDao {
    @Query("SELECT * FROM call_history ORDER BY id DESC")
    fun getAllCalls(): Flow<List<CallHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallHistoryEntity)
}

@Dao
interface AddaRoomDao {
    @Query("SELECT * FROM adda_rooms WHERE isLive = 1 ORDER BY listenerCount DESC")
    fun getLiveRooms(): Flow<List<AddaRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<AddaRoomEntity>)
}

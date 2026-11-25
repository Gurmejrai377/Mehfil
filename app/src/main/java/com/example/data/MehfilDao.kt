package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MehfilDao {
    @Query("SELECT * FROM user_accounts WHERE isActiveSession = 1 LIMIT 1")
    fun observeActiveUser(): Flow<UserAccount?>

    @Query("SELECT * FROM user_accounts WHERE userId = :userId LIMIT 1")
    fun observeUserById(userId: String): Flow<UserAccount?>

    @Query("SELECT * FROM user_accounts WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserAccount?

    @Query("UPDATE user_accounts SET isActiveSession = 0")
    suspend fun clearActiveSessions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserAccount)

    @Query("SELECT * FROM contacts WHERE userId = :userId ORDER BY favorite DESC, displayName ASC")
    fun observeContacts(userId: String): Flow<List<Contact>>

    @Query("SELECT COUNT(*) FROM contacts WHERE userId = :userId")
    suspend fun getContactCount(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(contact: Contact)

    @Query("UPDATE contacts SET favorite = :favorite, updatedAt = :updatedAt WHERE id = :contactId")
    suspend fun updateContactFavorite(contactId: String, favorite: Boolean, updatedAt: Long)

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteContact(contactId: String)

    @Query("SELECT * FROM chats WHERE ownerUserId = :userId ORDER BY pinned DESC, updatedAt DESC")
    fun observeChats(userId: String): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): Chat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChat(chat: Chat)

    @Query("UPDATE chats SET lastMessageText = :lastText, lastMessageType = :lastType, lastMessageSenderId = :senderId, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun updateChatPreview(
        chatId: String,
        lastText: String,
        lastType: String,
        senderId: String,
        updatedAt: Long
    )

    @Query("UPDATE chats SET pinned = :pinned, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun updateChatPin(chatId: String, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE chats SET muted = :muted, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun updateChatMute(chatId: String, muted: Boolean, updatedAt: Long)

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    fun observeMessages(chatId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("UPDATE chat_messages SET reaction = :reaction, updatedAt = :updatedAt WHERE id = :messageId")
    suspend fun updateMessageReaction(messageId: String, reaction: String, updatedAt: Long)

    @Query("SELECT * FROM call_records WHERE ownerUserId = :userId ORDER BY createdAt DESC")
    fun observeCalls(userId: String): Flow<List<CallRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCall(call: CallRecord)

    @Query("UPDATE call_records SET status = :status, durationSeconds = :durationSeconds, qualityMode = :qualityMode, updatedAt = :updatedAt WHERE id = :callId")
    suspend fun updateCall(
        callId: String,
        status: String,
        durationSeconds: Long,
        qualityMode: String,
        updatedAt: Long
    )

    @Query("SELECT * FROM stories ORDER BY createdAt DESC")
    fun observeStories(): Flow<List<Story>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStory(story: Story)

    @Query("UPDATE stories SET viewCount = viewCount + 1, updatedAt = :updatedAt WHERE id = :storyId")
    suspend fun incrementStoryView(storyId: String, updatedAt: Long)

    // Party Rooms
    @Query("SELECT * FROM party_rooms ORDER BY updatedAt DESC")
    fun observePartyRooms(): Flow<List<PartyRoom>>

    @Query("SELECT * FROM party_rooms WHERE id = :roomId LIMIT 1")
    fun observePartyRoom(roomId: String): Flow<PartyRoom?>

    @Query("SELECT * FROM party_rooms WHERE id = :roomId LIMIT 1")
    suspend fun getPartyRoom(roomId: String): PartyRoom?

    @Query("SELECT COUNT(*) FROM party_rooms")
    suspend fun getPartyRoomCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPartyRoom(room: PartyRoom)

    @Query("UPDATE party_rooms SET isMusicPlaying = :playing, currentSongTitle = :songTitle, updatedAt = :updatedAt WHERE id = :roomId")
    suspend fun updateRoomMusic(roomId: String, playing: Boolean, songTitle: String, updatedAt: Long)

    @Query("UPDATE party_rooms SET hostMicOn = :micOn, updatedAt = :updatedAt WHERE id = :roomId")
    suspend fun updateRoomHostMic(roomId: String, micOn: Boolean, updatedAt: Long)

    @Query("UPDATE party_rooms SET totalGiftCoins = totalGiftCoins + :coins, updatedAt = :updatedAt WHERE id = :roomId")
    suspend fun addRoomGiftCoins(roomId: String, coins: Long, updatedAt: Long)

    @Query("UPDATE party_rooms SET viewerCount = viewerCount + :delta WHERE id = :roomId")
    suspend fun updateRoomViewers(roomId: String, delta: Long)

    // Room Mic Seats (8 seats in circle)
    @Query("SELECT * FROM room_seats WHERE roomId = :roomId ORDER BY seatIndex ASC")
    fun observeRoomSeats(roomId: String): Flow<List<RoomSeat>>

    @Query("SELECT * FROM room_seats WHERE roomId = :roomId ORDER BY seatIndex ASC")
    suspend fun getRoomSeats(roomId: String): List<RoomSeat>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoomSeats(seats: List<RoomSeat>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoomSeat(seat: RoomSeat)

    @Query("UPDATE room_seats SET occupantId = NULL, occupantName = NULL, isMuted = 0, lastReaction = NULL, updatedAt = :updatedAt WHERE roomId = :roomId AND occupantId = :userId")
    suspend fun vacateUserSeats(roomId: String, userId: String, updatedAt: Long)

    @Query("UPDATE room_seats SET lastReaction = :reaction, updatedAt = :updatedAt WHERE roomId = :roomId AND occupantId = :userId")
    suspend fun updateUserSeatReaction(roomId: String, userId: String, reaction: String, updatedAt: Long)

    @Query("UPDATE room_seats SET giftCoins = giftCoins + :coins, updatedAt = :updatedAt WHERE id = :seatId")
    suspend fun addSeatGiftCoins(seatId: String, coins: Long, updatedAt: Long)

    // Room Live Chat
    @Query("SELECT * FROM room_messages WHERE roomId = :roomId ORDER BY createdAt ASC")
    fun observeRoomMessages(roomId: String): Flow<List<RoomMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomMessage(message: RoomMessage)

    // Music Room Song Queue
    @Query("SELECT * FROM room_songs WHERE roomId = :roomId ORDER BY isPlaying DESC, createdAt ASC")
    fun observeRoomSongs(roomId: String): Flow<List<RoomSongRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoomSong(song: RoomSongRequest)

    @Query("UPDATE room_songs SET isPlaying = CASE WHEN id = :songId THEN 1 ELSE 0 END WHERE roomId = :roomId")
    suspend fun setRoomPlayingSong(roomId: String, songId: String)
}

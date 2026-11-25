package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserAccount::class,
        Contact::class,
        Chat::class,
        ChatMessage::class,
        CallRecord::class,
        Story::class,
        PartyRoom::class,
        RoomSeat::class,
        RoomMessage::class,
        RoomSongRequest::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MehfilDatabase : RoomDatabase() {
    abstract fun mehfilDao(): MehfilDao

    companion object {
        @Volatile
        private var INSTANCE: MehfilDatabase? = null

        fun getInstance(context: Context): MehfilDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MehfilDatabase::class.java,
                    "mehfil_app_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

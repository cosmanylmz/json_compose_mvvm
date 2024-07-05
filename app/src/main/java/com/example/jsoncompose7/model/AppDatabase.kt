package com.example.jsoncompose7.model

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CommentEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commentDao(): CommentDao
}

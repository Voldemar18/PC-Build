package com.example.kt_fife.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PcBuildEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PcBuildDatabase : RoomDatabase() {
    abstract fun pcBuildDao(): PcBuildDao
}
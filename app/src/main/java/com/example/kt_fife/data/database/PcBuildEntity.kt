package com.example.kt_fife.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pc_builds")
data class PcBuildEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val isPublic: Boolean,
    val viewsCount: Int,
    val createdAt: String,
    val totalPrice: Double?,
    val userId: Long,
    val userName: String,
    val componentsJson: String = "[]"
)
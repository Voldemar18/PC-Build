package com.example.kt_fife.data.network
import com.example.kt_fife.domain.models.PcBuildComponent

data class PcBuildResponseItem(
    val id: Long,
    val name: String,
    val isPublic: Boolean,
    val viewsCount: Int,
    val createdAt: String,
    val totalPrice: Double?,
    val userId: Long,
    val userName: String,
    val components: List<PcBuildComponent>
)
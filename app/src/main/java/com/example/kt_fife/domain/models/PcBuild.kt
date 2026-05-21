package com.example.kt_fife.domain.models

data class PcBuild(
    val id: Long,
    val name: String,
    val isPublic: Boolean,
    val viewsCount: Int,
    val createdAt: String,
    val totalPrice: Double?,
    val userId: Long,
    val userName: String,
    val components: List<PcBuildComponent> = emptyList()
)

package com.example.kt_fife.domain.models
data class PcBuildComponent(
    val componentType: String,
    val productId: Long,
    val productName: String,
    val price: Double?,
    val quantity: Int
)
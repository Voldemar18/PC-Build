package com.example.kt_fife.data.network

data class CreatePcBuildRequest(
    val name: String,
    val isPublic: Boolean = true,
    val components: List<Long> = emptyList()
)

data class PcBuildsResponse(
    val content: List<PcBuildResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)

data class PcBuildResponse(
    val id: Long,
    val name: String,
    val isPublic: Boolean,
    val viewsCount: Int,
    val createdAt: String,
    val totalPrice: Double?,
    val userId: Long,
    val userName: String,
    val components: List<PcBuildComponentResponse>
)

data class PcBuildComponentResponse(
    val componentType: String,
    val productId: Long,
    val productName: String,
    val price: Double?,
    val quantity: Int
)

data class TotalPriceResponse(
    val totalPrice: Double
)

data class PageResponseProductResponse(
    val content: List<ProductResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)
data class ProductResponse(
    val id: Long,
    val name: String,
    val slug: String,
    val description: String?,
    val price: Double,
    val oldPrice: Double?,
    val inStock: Boolean,
    val sku: String,
    val categoryId: Long,
    val categoryName: String,
    val categorySlug: String,
    val componentTypeId: Long,
    val componentTypeName: String,
    val specifications: Map<String, String>?,
    val images: List<String>?,
    val createdAt: String,
    val updatedAt: String?
)

data class ComponentTypeResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val step: Int,
    val isRequired: Boolean,
    val multipleAllowed: Boolean,
    val isStorage: Boolean,
    val isProcessor: Boolean,
    val isMemory: Boolean,
    val slug: String = name.lowercase()
)

data class CategoryResponse(
    val id: Long,
    val name: String,
    val slug: String,
    val order: Int,
    val parentId: Long? = null,
    val parentName: String? = null,
    val image: String? = null
)
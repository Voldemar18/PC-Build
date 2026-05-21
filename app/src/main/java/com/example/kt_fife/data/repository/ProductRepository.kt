package com.example.kt_fife.data.repository

import android.util.Log
import com.example.kt_fife.data.network.ApiService
import com.example.kt_fife.data.network.CategoryResponse
import com.example.kt_fife.data.network.ComponentTypeResponse
import com.example.kt_fife.data.network.ProductResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val apiService: ApiService
) {

    private val productCache = mutableMapOf<String, List<ProductResponse>>()
    private var componentTypesCache: List<ComponentTypeResponse>? = null
    private var categoriesCache: List<CategoryResponse>? = null

    suspend fun getCategories(forceRefresh: Boolean = false): Result<List<CategoryResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!forceRefresh && categoriesCache != null) {
                    return@withContext Result.success(categoriesCache!!)
                }

                val response = apiService.getCategories()
                if (response.isSuccessful && response.body() != null) {
                    val pageResponse = response.body()!!
                    val categories = pageResponse.content
                    categoriesCache = categories
                    Log.d("ProductRepository", "Loaded ${categories.size} categories")
                    Result.success(categories)
                } else {
                    Log.e("ProductRepository", "Failed to load categories: ${response.code()}")
                    Result.failure(Exception("Failed to load categories: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error loading categories", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getCategoryId(componentName: String): Long? {
        val categories = getCategories().getOrNull() ?: return null

        val categoryNameMap = mapOf(
            "Процессор" to "Процессоры",
            "Материнская плата" to "Материнские платы",
            "Видеокарта" to "Видеокарты",
            "Оперативная память" to "Оперативная память",
            "Накопитель" to "Накопители",
            "Блок питания" to "Блоки питания",
            "Корпус" to "Корпуса",
            "Охлаждение" to "Охлаждение"
        )

        val categoryName = categoryNameMap[componentName] ?: componentName

        val foundCategory = categories.find {
            it.name.equals(categoryName, ignoreCase = true)
        }

        Log.d("ProductRepository", "Category lookup for '$componentName' -> '${foundCategory?.name}' with id: ${foundCategory?.id}")
        return foundCategory?.id
    }

    suspend fun getComponentTypesOrdered(): Result<List<ComponentTypeResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                if (componentTypesCache != null) {
                    return@withContext Result.success(componentTypesCache!!)
                }

                val response = apiService.getComponentTypesOrdered()
                if (response.isSuccessful && response.body() != null) {
                    val types = response.body()!!
                    componentTypesCache = types
                    Log.d("ProductRepository", "Loaded ${types.size} component types")
                    Result.success(types)
                } else {
                    Log.e("ProductRepository", "Failed to load component types: ${response.code()}")
                    Result.failure(Exception("Failed to load component types: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error loading component types", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getProductsByCategoryId(
        categoryId: Long,
        page: Int = 0,
        size: Int = 50,
        forceRefresh: Boolean = false
    ): Result<List<ProductResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = "category:$categoryId:$page:$size"

                if (!forceRefresh && productCache.containsKey(cacheKey)) {
                    return@withContext Result.success(productCache[cacheKey]!!)
                }

                val response = apiService.getProductsByCategoryId(categoryId, page, size)

                if (response.isSuccessful && response.body() != null) {
                    val products = response.body()!!.content
                    productCache[cacheKey] = products
                    Log.d("ProductRepository", "Loaded ${products.size} products for category ID: $categoryId")
                    Result.success(products)
                } else {
                    Log.e("ProductRepository", "Failed to load products: ${response.code()}")
                    Result.failure(Exception("Failed to load products: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error loading products", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getProductsByComponentType(
        componentType: String,
        page: Int = 0,
        size: Int = 50
    ): Result<List<ProductResponse>> {
        val categoryId = getCategoryId(componentType)
        return if (categoryId != null) {
            getProductsByCategoryId(categoryId, page, size)
        } else {
            Result.failure(Exception("Unknown component type: $componentType"))
        }
    }

    suspend fun getProductById(productId: Long): Result<ProductResponse> {
        return withContext(Dispatchers.IO) {
            try {
                for (cached in productCache.values) {
                    cached.find { it.id == productId }?.let {
                        return@withContext Result.success(it)
                    }
                }

                val response = apiService.getProducts(page = 0, size = 100)
                if (response.isSuccessful && response.body() != null) {
                    val product = response.body()!!.content.find { it.id == productId }
                    if (product != null) {
                        Result.success(product)
                    } else {
                        Result.failure(Exception("Product not found"))
                    }
                } else {
                    Result.failure(Exception("Failed to load product: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error loading product", e)
                Result.failure(e)
            }
        }
    }

    fun clearCache() {
        productCache.clear()
        componentTypesCache = null
        categoriesCache = null
    }
}
package com.example.kt_fife.data.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/register")
    suspend fun register(@Body request: UserRegistrationRequest): Response<AuthResponse>

    @POST("api/login")
    suspend fun login(@Body request: UserLoginRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/pc-builds/public")
    suspend fun getPublicPcBuilds(): Response<PcBuildsResponse>

    @GET("api/pc-builds/public/{id}")
    suspend fun getPcBuildById(@Path("id") id: Long): Response<PcBuildResponse>

    @GET("api/pc-builds/me")
    suspend fun getMyPcBuilds(): Response<PcBuildsResponse>

    @GET("api/pc-builds/me/{id}")
    suspend fun getMyPcBuildById(@Path("id") id: Long): Response<PcBuildResponse>

    @POST("api/pc-builds/me")
    suspend fun createPcBuild(@Body request: CreatePcBuildRequest): Response<PcBuildResponse>

    @PUT("api/pc-builds/me/{id}")
    suspend fun updatePcBuild(@Path("id") id: Long, @Body request: CreatePcBuildRequest): Response<PcBuildResponse>

    @DELETE("api/pc-builds/me/{id}")
    suspend fun deletePcBuild(@Path("id") id: Long): Response<Unit>

    @POST("api/pc-builds/me/{id}/components/{productId}")
    suspend fun addComponentToBuild(
        @Path("id") buildId: Long,
        @Path("productId") productId: Long,
        @Query("quantity") quantity: Int = 1
    ): Response<Unit>

    @DELETE("api/pc-builds/me/{id}/components/{productId}")
    suspend fun removeComponentFromBuild(
        @Path("id") buildId: Long,
        @Path("productId") productId: Long
    ): Response<Unit>

    @GET("api/pc-builds/me/{id}/total")
    suspend fun getBuildTotalPrice(@Path("id") id: Long): Response<TotalPriceResponse>

    @GET("api/pc-builds/me/{id}/components")
    suspend fun getBuildComponents(@Path("id") id: Long): Response<List<PcBuildComponentResponse>>

    @GET("api/products")
    suspend fun getProducts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("category") category: String? = null,
        @Query("componentType") componentType: String? = null,
        @Query("categoryId") categoryId: Long? = null
    ): Response<PageResponse<ProductResponse>>

    @GET("api/products/search")
    suspend fun searchProducts(
        @Query("query") query: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<ProductResponse>>

    @GET("api/products/category/{categoryId}")
    suspend fun getProductsByCategoryId(
        @Path("categoryId") categoryId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<ProductResponse>>

    @GET("api/component-types")
    suspend fun getComponentTypes(): Response<List<ComponentTypeResponse>>

    @GET("api/component-types/ordered")
    suspend fun getComponentTypesOrdered(): Response<List<ComponentTypeResponse>>

    @GET("api/categories")
    suspend fun getCategories(): Response<PageResponse<CategoryResponse>>

    @GET("api/categories/tree")
    suspend fun getCategoriesTree(): Response<List<CategoryResponse>>
}
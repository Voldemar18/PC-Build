package com.example.kt_fife.data.repository

import com.example.kt_fife.data.database.PcBuildDao
import com.example.kt_fife.data.database.PcBuildEntity
import com.example.kt_fife.data.network.ApiService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PcBuildRepository @Inject constructor(
    private val pcBuildDao: PcBuildDao,
    private val apiService: ApiService
) {

    fun getAllPcBuilds(): Flow<List<PcBuildEntity>> {
        return pcBuildDao.getAllPcBuilds()
    }

    suspend fun getPcBuildById(id: Long): PcBuildEntity? {
        return withContext(Dispatchers.IO) {
            pcBuildDao.getPcBuildById(id)
        }
    }
    suspend fun refreshCache(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPublicPcBuilds()

                if (response.isSuccessful && response.body() != null) {
                    val buildsResponse = response.body()!!
                    val builds = buildsResponse.content

                    val entities = builds.map { build ->
                        PcBuildEntity(
                            id = build.id,
                            name = build.name,
                            isPublic = build.isPublic,
                            viewsCount = build.viewsCount,
                            createdAt = build.createdAt,
                            totalPrice = build.totalPrice,
                            userId = build.userId,
                            userName = build.userName,
                            componentsJson = Gson().toJson(build.components)
                        )
                    }

                    pcBuildDao.insertAll(entities)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Server error: ${response.code()}"))
                }
            } catch (e: IOException) {
                Result.failure(Exception("Network error: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(Exception("Unknown error: ${e.message}"))
            }
        }
    }

    suspend fun getCacheSize(): Int {
        return withContext(Dispatchers.IO) {
            pcBuildDao.getCount()
        }
    }

    suspend fun insertBuild(build: PcBuildEntity) {
        return withContext(Dispatchers.IO) {
            pcBuildDao.insert(build)
        }
    }
}
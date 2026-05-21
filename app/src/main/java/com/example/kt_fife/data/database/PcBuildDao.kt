package com.example.kt_fife.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PcBuildDao {

    @Query("SELECT * FROM pc_builds ORDER BY viewsCount DESC")
    fun getAllPcBuilds(): Flow<List<PcBuildEntity>>

    @Query("SELECT * FROM pc_builds WHERE id = :id")
    suspend fun getPcBuildById(id: Long): PcBuildEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(builds: List<PcBuildEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(build: PcBuildEntity)

    @Query("DELETE FROM pc_builds")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM pc_builds")
    suspend fun getCount(): Int
}
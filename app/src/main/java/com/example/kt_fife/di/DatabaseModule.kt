package com.example.kt_fife.di

import android.content.Context
import androidx.room.Room
import com.example.kt_fife.data.database.PcBuildDao
import com.example.kt_fife.data.database.PcBuildDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "pc_build_database"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PcBuildDatabase {
        return Room.databaseBuilder(
            context,
            PcBuildDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePcBuildDao(database: PcBuildDatabase): PcBuildDao {
        return database.pcBuildDao()
    }
}
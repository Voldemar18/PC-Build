package com.example.kt_fife.data.database

import androidx.room.TypeConverter
import com.example.kt_fife.domain.models.PcBuildComponent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    @TypeConverter
    fun fromComponentsList(components: List<PcBuildComponent>): String {
        return Gson().toJson(components)
    }

    @TypeConverter
    fun toComponentsList(componentsJson: String): List<PcBuildComponent> {
        val type = object : TypeToken<List<PcBuildComponent>>() {}.type
        return Gson().fromJson(componentsJson, type) ?: emptyList()
    }
}
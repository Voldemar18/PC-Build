package com.example.kt_fife.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kt_fife.data.local.ThemeManager
import com.example.kt_fife.data.repository.PcBuildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager,
    private val repository: PcBuildRepository
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = themeManager.isDarkTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val buildsCount: StateFlow<Int> = repository.getAllPcBuilds()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            themeManager.setDarkTheme(isDark)
        }
    }
}
package com.example.kt_fife.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kt_fife.data.local.ThemeManager
import com.example.kt_fife.data.local.TokenManager
import com.example.kt_fife.ui.auth.LoginScreen
import com.example.kt_fife.ui.auth.RegisterScreen
import com.example.kt_fife.ui.detail.PcBuildDetailScreen
import com.example.kt_fife.ui.list.PcBuildListScreen
import com.example.kt_fife.ui.create.CreatePcBuildScreen
import com.example.kt_fife.ui.edit.EditPcBuildScreen
import com.example.kt_fife.ui.home.HomeScreen
import com.example.kt_fife.ui.settings.SettingsScreen
import com.example.kt_fife.ui.theme.KT_fifeTheme

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Login : Screen("login")
    object Register : Screen("register")
    object BuildList : Screen("build_list")
    object BuildDetail : Screen("build_detail/{buildId}") {
        fun passId(id: Long): String = "build_detail/$id"
    }
    object CreateBuild : Screen("create_build")
    object EditBuild : Screen("edit_build/{buildId}") {
        fun passId(id: Long): String = "edit_build/$id"
    }
    object Settings : Screen("settings")
}

@Composable
fun NavGraph(tokenManager: TokenManager, themeManager: ThemeManager) {
    val navController = rememberNavController()
    val isLoggedIn by tokenManager.isLoggedIn.collectAsState()
    val isDarkTheme by themeManager.isDarkTheme.collectAsState(initial = false)

    KT_fifeTheme(darkTheme = isDarkTheme) {
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route

        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToCreateBuild = {
                        navController.navigate(Screen.CreateBuild.route)
                    },
                    onNavigateToMyBuilds = {
                        navController.navigate(Screen.BuildList.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        tokenManager.clearTokens()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            // Остальные экраны без изменений...
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.BuildList.route) {
                PcBuildListScreen(
                    onPcBuildClick = { buildId ->
                        navController.navigate(Screen.BuildDetail.passId(buildId))
                    },
                    onCreateBuildClick = {
                        navController.navigate(Screen.CreateBuild.route)
                    }
                )
            }

            composable(
                route = Screen.BuildDetail.route,
                arguments = listOf(navArgument("buildId") { type = NavType.LongType })
            ) { backStackEntry ->
                val buildId = backStackEntry.arguments?.getLong("buildId") ?: -1L
                PcBuildDetailScreen(
                    buildId = buildId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Screen.EditBuild.passId(buildId)) }
                )
            }

            composable(Screen.CreateBuild.route) {
                CreatePcBuildScreen(
                    onSave = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditBuild.route,
                arguments = listOf(navArgument("buildId") { type = NavType.LongType })
            ) { backStackEntry ->
                val buildId = backStackEntry.arguments?.getLong("buildId") ?: -1L
                EditPcBuildScreen(
                    buildId = buildId,
                    onSave = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}
package com.example.kt_fife.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kt_fife.data.local.TokenManager
import com.example.kt_fife.ui.auth.LoginScreen
import com.example.kt_fife.ui.auth.RegisterScreen
import com.example.kt_fife.ui.detail.PcBuildDetailScreen
import com.example.kt_fife.ui.list.PcBuildListScreen
import com.example.kt_fife.ui.create.CreatePcBuildScreen
import com.example.kt_fife.ui.edit.EditPcBuildScreen

sealed class Screen(val route: String) {
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
}

@Composable
fun NavGraph(tokenManager: TokenManager) {
    val navController = rememberNavController()
    val isLoggedIn by tokenManager.isLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.BuildList.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.BuildList.route) {
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
                    navController.navigate(Screen.BuildList.route) {
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
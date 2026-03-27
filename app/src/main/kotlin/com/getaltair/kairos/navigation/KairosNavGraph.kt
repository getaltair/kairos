package com.getaltair.kairos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.getaltair.kairos.feature.auth.ForgotPasswordScreen
import com.getaltair.kairos.feature.auth.LoginScreen
import com.getaltair.kairos.feature.auth.SignUpScreen
import com.getaltair.kairos.feature.habit.CreateHabitScreen
import com.getaltair.kairos.feature.habit.EditHabitScreen
import com.getaltair.kairos.feature.habit.HabitDetailScreen
import com.getaltair.kairos.feature.settings.NotificationSettingsScreen
import com.getaltair.kairos.feature.settings.SettingsScreen
import com.getaltair.kairos.feature.today.TodayScreen
import java.util.UUID

@Composable
fun KairosNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "today"
    ) {
        composable("today") {
            TodayScreen(
                onAddHabit = { navController.navigate("createHabit") },
                onHabitClick = { habitId -> navController.navigate("habitDetail/$habitId") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("createHabit") {
            CreateHabitScreen(
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() }
            )
        }
        composable("habitDetail/{habitId}") { backStackEntry ->
            val habitId = runCatching {
                UUID.fromString(backStackEntry.arguments?.getString("habitId"))
            }.getOrElse {
                navController.popBackStack()
                return@composable
            }
            HabitDetailScreen(
                habitId = habitId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("editHabit/$id") },
                onDeleted = { navController.popBackStack() }
            )
        }
        composable("editHabit/{habitId}") { backStackEntry ->
            val habitId = runCatching {
                UUID.fromString(backStackEntry.arguments?.getString("habitId"))
            }.getOrElse {
                navController.popBackStack()
                return@composable
            }
            EditHabitScreen(
                habitId = habitId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToNotificationSettings = {
                    navController.navigate("settings/notifications")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings/notifications") {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.popBackStack() },
                onNavigateToSignUp = { navController.navigate("signup") },
                onNavigateToForgotPassword = { navController.navigate("forgotPassword") }
            )
        }
        composable("signup") {
            SignUpScreen(
                onSignUpSuccess = { navController.popBackStack("login", inclusive = true) },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable("forgotPassword") {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

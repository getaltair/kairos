package com.getaltair.kairos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.getaltair.kairos.feature.auth.ForgotPasswordScreen
import com.getaltair.kairos.feature.auth.LoginScreen
import com.getaltair.kairos.feature.auth.SignUpScreen
import com.getaltair.kairos.feature.habit.CreateHabitScreen
import com.getaltair.kairos.feature.habit.EditHabitScreen
import com.getaltair.kairos.feature.habit.HabitDetailScreen
import com.getaltair.kairos.feature.recovery.RecoverySessionScreen
import com.getaltair.kairos.feature.routine.RoutineBuilderScreen
import com.getaltair.kairos.feature.routine.RoutineListScreen
import com.getaltair.kairos.feature.routine.RoutineRunnerScreen
import com.getaltair.kairos.feature.routine.RoutineSummaryScreen
import com.getaltair.kairos.feature.settings.NotificationSettingsScreen
import com.getaltair.kairos.feature.settings.SettingsScreen
import com.getaltair.kairos.feature.today.TodayScreen
import java.util.UUID
import timber.log.Timber

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
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToRoutines = { navController.navigate("routines") },
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
        composable(
            route = "recovery/{habitId}",
            deepLinks = listOf(navDeepLink { uriPattern = "kairos://recovery/{habitId}" })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getString("habitId")
            if (habitId == null) {
                Timber.w("Recovery route: habitId argument is null")
                navController.popBackStack()
                return@composable
            }
            // Validate UUID format
            runCatching { UUID.fromString(habitId) }.onFailure {
                Timber.w("Recovery route: invalid UUID format for habitId=%s", habitId)
                navController.popBackStack()
                return@composable
            }
            RecoverySessionScreen(
                habitId = habitId,
                onComplete = { navController.popBackStack() }
            )
        }
        composable("routines") {
            RoutineListScreen(
                onCreateRoutine = { navController.navigate("createRoutine") },
                onRoutineClick = { routineId ->
                    navController.navigate("routineRunner/$routineId")
                },
            )
        }
        composable("createRoutine") {
            RoutineBuilderScreen(
                routineId = null,
                onNavigateBack = { navController.popBackStack() },
                onRoutineSaved = { routineId ->
                    navController.navigate("routineRunner/$routineId") {
                        popUpTo("createRoutine") { inclusive = true }
                    }
                },
            )
        }
        composable("editRoutine/{routineId}") { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId")
            if (routineId == null) {
                navController.popBackStack()
                return@composable
            }
            RoutineBuilderScreen(
                routineId = routineId,
                onNavigateBack = { navController.popBackStack() },
                onRoutineSaved = { navController.popBackStack() },
            )
        }
        composable("routineRunner/{routineId}") { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId")
            if (routineId == null) {
                navController.popBackStack()
                return@composable
            }
            RoutineRunnerScreen(
                routineId = routineId,
                onComplete = { executionId ->
                    navController.navigate("routineSummary/$executionId") {
                        popUpTo("routineRunner/{routineId}") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("routineSummary/{executionId}") { backStackEntry ->
            val executionId = backStackEntry.arguments?.getString("executionId")
            if (executionId == null) {
                navController.popBackStack()
                return@composable
            }
            RoutineSummaryScreen(
                executionId = executionId,
                onDone = {
                    navController.navigate("routines") {
                        popUpTo("routines") { inclusive = true }
                    }
                },
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

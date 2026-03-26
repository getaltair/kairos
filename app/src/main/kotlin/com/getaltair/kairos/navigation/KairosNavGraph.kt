package com.getaltair.kairos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.getaltair.kairos.feature.habit.CreateHabitScreen
import com.getaltair.kairos.feature.habit.EditHabitScreen
import com.getaltair.kairos.feature.habit.HabitDetailScreen
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
                onHabitClick = { habitId -> navController.navigate("habitDetail/$habitId") }
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
            // TODO: Settings screen
        }
    }
}

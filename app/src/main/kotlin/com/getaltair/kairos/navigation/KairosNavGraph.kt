package com.getaltair.kairos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.getaltair.kairos.feature.habit.CreateHabitScreen
import com.getaltair.kairos.feature.today.TodayScreen

@Composable
fun KairosNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "today"
    ) {
        composable("today") {
            TodayScreen(onAddHabit = { navController.navigate("createHabit") })
        }
        composable("createHabit") {
            CreateHabitScreen(
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() }
            )
        }
        composable("settings") {
            // TODO: Settings screen
        }
    }
}

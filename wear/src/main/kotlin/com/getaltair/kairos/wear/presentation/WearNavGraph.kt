package com.getaltair.kairos.wear.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable

const val ROUTE_HABIT_LIST = "habit_list"
const val ROUTE_HABIT_DETAIL = "habit_detail/{habitId}"
const val ROUTE_ROUTINE_LIST = "routine_list"
const val ROUTE_ROUTINE_RUNNER = "routine_runner/{routineId}"

/**
 * Wear navigation graph for the Kairos watch app.
 * Uses SwipeDismissableNavHost for back-swipe navigation.
 *
 * Routes:
 * - habit_list: Main screen showing today's habits grouped by category
 * - habit_detail/{habitId}: Detail screen with complete/partial/skip actions
 * - routine_list: Shows active routine (if any)
 * - routine_runner/{routineId}: Step-by-step routine execution with timer
 */
@Composable
fun WearNavGraph(navController: NavHostController) {
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = ROUTE_HABIT_LIST,
    ) {
        composable(ROUTE_HABIT_LIST) {
            HabitListScreen(
                onHabitClick = { habitId ->
                    navController.navigate("habit_detail/$habitId")
                },
                onRoutinesClick = {
                    navController.navigate(ROUTE_ROUTINE_LIST)
                },
            )
        }
        composable(ROUTE_HABIT_DETAIL) { backStackEntry ->
            val habitId = backStackEntry.arguments?.getString("habitId") ?: return@composable
            HabitDetailScreen(
                habitId = habitId,
                onActionComplete = { navController.popBackStack() },
            )
        }
        composable(ROUTE_ROUTINE_LIST) {
            RoutineListScreen(
                onRoutineClick = { routineId ->
                    navController.navigate("routine_runner/$routineId")
                },
            )
        }
        composable(ROUTE_ROUTINE_RUNNER) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId") ?: return@composable
            RoutineRunnerScreen(
                routineId = routineId,
                onFinished = { navController.popBackStack() },
            )
        }
    }
}

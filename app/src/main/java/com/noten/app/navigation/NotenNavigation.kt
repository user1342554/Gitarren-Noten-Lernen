package com.noten.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.noten.app.model.TunerUiState
import com.noten.app.ui.HomeScreen
import com.noten.app.ui.QuizScreen
import com.noten.app.ui.ResultScreen
import com.noten.app.ui.TunerScreen

object Routes {
    const val HOME = "home"
    const val TUNER = "tuner"
    const val QUIZ = "quiz"
    const val RESULT = "result/{score}/{total}"

    fun result(score: Int, total: Int) = "result/$score/$total"
}

@Composable
fun NotenNavigation(
    tunerUiState: TunerUiState,
    onToggleListening: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartQuiz = { navController.navigate(Routes.QUIZ) },
                onOpenTuner = { navController.navigate(Routes.TUNER) }
            )
        }

        composable(Routes.TUNER) {
            TunerScreen(
                uiState = tunerUiState,
                onToggleListening = onToggleListening,
                onRequestPermission = onRequestPermission,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.QUIZ) {
            QuizScreen(
                onFinished = { score, total ->
                    navController.navigate(Routes.result(score, total)) {
                        popUpTo(Routes.QUIZ) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument("score") { type = NavType.IntType },
                navArgument("total") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val total = backStackEntry.arguments?.getInt("total") ?: 10
            ResultScreen(
                score = score,
                total = total,
                onPlayAgain = {
                    navController.navigate(Routes.QUIZ) {
                        popUpTo(Routes.HOME)
                    }
                },
                onGoHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}

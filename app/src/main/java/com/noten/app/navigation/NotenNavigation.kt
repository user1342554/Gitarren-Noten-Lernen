package com.noten.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.noten.app.model.GuitarTuning
import com.noten.app.model.TunerUiState
import com.noten.app.quiz.Difficulty
import com.noten.app.ui.HomeScreen
import com.noten.app.ui.QuizScreen
import com.noten.app.ui.ResultScreen
import com.noten.app.ui.TunerScreen

object Routes {
    const val HOME = "home"
    const val TUNER = "tuner"
    const val QUIZ = "quiz/{difficulty}"
    const val RESULT = "result/{score}/{total}"

    fun quiz(difficulty: Difficulty) = "quiz/${difficulty.name}"
    fun result(score: Int, total: Int) = "result/$score/$total"
}

@Composable
fun NotenNavigation(
    tunerUiState: TunerUiState,
    onToggleListening: () -> Unit,
    onTuningChanged: (GuitarTuning) -> Unit,
    onRequestPermission: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartQuiz = { difficulty ->
                    navController.navigate(Routes.quiz(difficulty))
                },
                onOpenTuner = { navController.navigate(Routes.TUNER) }
            )
        }

        composable(Routes.TUNER) {
            TunerScreen(
                uiState = tunerUiState,
                onToggleListening = onToggleListening,
                onRequestPermission = onRequestPermission,
                onTuningChanged = onTuningChanged,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.QUIZ,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val difficultyName = backStackEntry.arguments?.getString("difficulty") ?: Difficulty.OPEN_STRINGS.name
            val difficulty = Difficulty.valueOf(difficultyName)
            QuizScreen(
                difficulty = difficulty,
                onStop = { score, total ->
                    navController.navigate(Routes.result(score, total)) {
                        popUpTo(Routes.HOME)
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
            val total = backStackEntry.arguments?.getInt("total") ?: 0
            ResultScreen(
                score = score,
                total = total,
                onPlayAgain = {
                    navController.popBackStack(Routes.HOME, false)
                },
                onGoHome = {
                    navController.popBackStack(Routes.HOME, false)
                }
            )
        }
    }
}

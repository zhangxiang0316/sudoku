package com.family.sudoku.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.family.sudoku.game.AppScreen
import com.family.sudoku.ui.game.ExitConfirmDialog
import com.family.sudoku.ui.game.GameScreen
import com.family.sudoku.ui.game.PauseDialog
import com.family.sudoku.ui.game.RestartConfirmDialog
import com.family.sudoku.ui.game.WinDialog
import com.family.sudoku.ui.home.HomeScreen
import com.family.sudoku.ui.leaderboard.LeaderboardScreen

@Composable
fun SudokuApp(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirm by remember { mutableStateOf(false) }
    var showRestartConfirm by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Crossfade(
            targetState = state.screen,
            animationSpec = tween(durationMillis = 260),
            label = "screen",
        ) { screen ->
            when (screen) {
                AppScreen.HOME -> HomeScreen(
                    selectedSize = state.size,
                    selectedDifficulty = state.difficulty,
                    stats = state.stats,
                    savedPreview = state.savedPreview.takeIf { state.hasSavedGame },
                    onSelectSize = viewModel::selectSize,
                    onSelectDifficulty = viewModel::selectDifficulty,
                    onStart = { viewModel.startNewGame(state.size, state.difficulty) },
                    onContinue = viewModel::continueSavedGame,
                    onOpenLeaderboard = viewModel::openLeaderboard,
                    modifier = Modifier.safeDrawingPadding(),
                )

                AppScreen.LEADERBOARD -> LeaderboardScreen(
                    selectedSize = state.leaderboardSize,
                    selectedDifficulty = state.leaderboardDifficulty,
                    entries = state.leaderboard,
                    onBack = viewModel::closeLeaderboard,
                    onSelectSize = viewModel::selectLeaderboardSize,
                    onSelectDifficulty = viewModel::selectLeaderboardDifficulty,
                    modifier = Modifier.safeDrawingPadding(),
                )

                AppScreen.GAME -> GameScreen(
                    state = state,
                    onBack = { showExitConfirm = true },
                    onPause = viewModel::pauseGame,
                    onCellClick = viewModel::selectCell,
                    onDigit = viewModel::inputDigit,
                    onErase = viewModel::eraseSelected,
                    onUndo = viewModel::undo,
                    onToggleNotes = viewModel::toggleNoteMode,
                    onHint = viewModel::useHint,
                    onRetry = viewModel::retryGeneration,
                    onToggleQuiet = viewModel::toggleQuietMode,
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
        }
    }

    val result = state.result
    // The win dialog belongs to the game screen only; if the player navigates away it must
    // not linger on top of another screen.
    if (state.screen == AppScreen.GAME && state.isComplete && result != null) {
        WinDialog(
            result = result,
            size = state.size,
            difficulty = state.difficulty,
            onPlayAgain = viewModel::playAgain,
            onHome = viewModel::goHomeAfterCompletion,
            onViewLeaderboard = viewModel::openLeaderboard,
        )
    }

    if (state.isPaused && state.screen == AppScreen.GAME) {
        PauseDialog(
            onResume = viewModel::resumeFromPause,
            onRestart = {
                viewModel.resumeFromPause()
                showRestartConfirm = true
            },
            onExit = {
                viewModel.resumeFromPause()
                viewModel.exitToHome()
            },
        )
    }

    if (showExitConfirm) {
        ExitConfirmDialog(
            onStay = { showExitConfirm = false },
            onLeave = {
                showExitConfirm = false
                viewModel.exitToHome()
            },
        )
    }

    if (showRestartConfirm) {
        RestartConfirmDialog(
            onCancel = { showRestartConfirm = false },
            onConfirm = {
                showRestartConfirm = false
                viewModel.restartPuzzle()
            },
        )
    }

    BackHandler(enabled = state.screen == AppScreen.LEADERBOARD) {
        viewModel.closeLeaderboard()
    }

    BackHandler(enabled = state.screen == AppScreen.GAME) {
        when {
            state.isComplete -> viewModel.goHomeAfterCompletion()
            state.isPaused -> viewModel.resumeFromPause()
            showRestartConfirm -> showRestartConfirm = false
            showExitConfirm -> showExitConfirm = false
            else -> showExitConfirm = true
        }
    }
}

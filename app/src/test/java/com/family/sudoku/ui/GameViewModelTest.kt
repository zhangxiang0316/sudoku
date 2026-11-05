package com.family.sudoku.ui

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.family.sudoku.feedback.NoFeedback
import com.family.sudoku.data.BoardSize
import com.family.sudoku.data.Difficulty
import com.family.sudoku.data.GameRepository
import com.family.sudoku.game.AppScreen
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameViewModelTest {

    // Generation runs on Unconfined so that startNewGame() finishes synchronously and the
    // tests do not need to drive a virtual clock.
    private val computeDispatcher = Dispatchers.Unconfined
    private lateinit var application: Application
    private lateinit var repository: GameRepository
    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        repository = GameRepository(application)
        application.getSharedPreferences("sudoku_kids_prefs", 0).edit().clear().commit()
        viewModel = GameViewModel(application, computeDispatcher, NoFeedback)
    }

    @After
    fun tearDown() {
        viewModel.stopTimers()
    }

    /** The injected Unconfined dispatcher makes generation complete inside this call. */
    private fun startAndSettle(
        size: BoardSize = BoardSize.FOUR,
        difficulty: Difficulty = Difficulty.EASY,
    ) {
        viewModel.startNewGame(size, difficulty)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun newGameProducesPlayableBoard() {
        startAndSettle()

        val state = viewModel.uiState.value
        assertEquals(AppScreen.GAME, state.screen)
        assertFalse(state.isGenerating)
        assertEquals(16, state.board.size)
        assertTrue("puzzle must contain clues", state.givenCount in 5..12)
        assertEquals("board must start identical to the puzzle", state.puzzle, state.board)
        assertNotNull("a cell should be pre-selected", state.selectedCell)
    }

    @Test
    fun everyBoardSizeStartsAGameOfTheRightShape() {
        for (size in BoardSize.entries) {
            val vm = GameViewModel(application, computeDispatcher, NoFeedback)
            try {
                vm.startNewGame(size, Difficulty.EASY)
                shadowOf(Looper.getMainLooper()).idle()
                val state = vm.uiState.value
                assertEquals(size, state.size)
                assertEquals(size.cellCount, state.board.size)
                assertEquals(size.cellCount, state.notes.size)
                assertEquals(size.cellCount, state.solution.size)
                assertFalse("${size.title} should not be generating", state.isGenerating)
                assertTrue("${size.title} solution must be complete", state.solution.none { it == 0 })
                assertTrue(
                    "${size.title} digits must fit the board",
                    state.solution.all { it in 1..size.size },
                )
                assertTrue("${size.title} must have blanks", state.puzzle.contains(0))
            } finally {
                vm.stopTimers()
            }
        }
    }

    @Test
    fun correctEntryFillsCellAndWrongEntryCountsAMistake() {
        startAndSettle()
        val state = viewModel.uiState.value
        val empty = state.board.indexOfFirst { it == 0 }
        val correct = state.solution[empty]
        val wrong = (1..4).first { it != correct }

        viewModel.selectCell(empty)
        viewModel.inputDigit(wrong)
        assertEquals(1, viewModel.uiState.value.mistakes)
        assertEquals(wrong, viewModel.uiState.value.board[empty])

        viewModel.undo()
        assertEquals(0, viewModel.uiState.value.board[empty])

        viewModel.inputDigit(correct)
        assertEquals(correct, viewModel.uiState.value.board[empty])
        assertEquals("a correct entry must not add a mistake", 1, viewModel.uiState.value.mistakes)
    }

    @Test
    fun digitsLargerThanTheBoardAreIgnored() {
        startAndSettle(BoardSize.FOUR, Difficulty.EASY)
        val empty = viewModel.uiState.value.board.indexOfFirst { it == 0 }
        viewModel.selectCell(empty)
        viewModel.inputDigit(7)

        assertEquals(0, viewModel.uiState.value.board[empty])
        assertEquals(0, viewModel.uiState.value.mistakes)
    }

    @Test
    fun eraseAndUndoRestoreTheBoard() {
        startAndSettle()
        val state = viewModel.uiState.value
        val empty = state.board.indexOfFirst { it == 0 }
        viewModel.selectCell(empty)
        viewModel.inputDigit(state.solution[empty])
        assertEquals(state.solution[empty], viewModel.uiState.value.board[empty])

        viewModel.eraseSelected()
        assertEquals(0, viewModel.uiState.value.board[empty])

        viewModel.undo()
        assertEquals(state.solution[empty], viewModel.uiState.value.board[empty])
    }

    @Test
    fun notesToggleWithoutChangingTheBoard() {
        startAndSettle()
        val empty = viewModel.uiState.value.board.indexOfFirst { it == 0 }
        viewModel.selectCell(empty)
        viewModel.toggleNoteMode()
        viewModel.inputDigit(3)
        viewModel.inputDigit(3)

        val state = viewModel.uiState.value
        assertEquals("notes must never overwrite the board", 0, state.board[empty])
        assertEquals("toggling the same digit twice clears the note", 0, state.notes[empty])

        viewModel.inputDigit(3)
        assertTrue(viewModel.uiState.value.notes[empty] and (1 shl 2) != 0)
    }

    @Test
    fun hintFillsACorrectCellAndIsLimited() {
        startAndSettle()
        repeat(5) {
            viewModel.useHint()
        }
        val state = viewModel.uiState.value
        assertEquals(
            "4x4 boards allow ${state.hintAllowance} hints",
            state.hintAllowance,
            state.hints,
        )
        assertEquals(
            "every hinted cell must equal the solution",
            state.board,
            state.board.mapIndexed { index, value ->
                when {
                    state.puzzle[index] != 0 -> state.puzzle[index]
                    value != 0 -> state.solution[index]
                    else -> 0
                }
            },
        )
    }

    @Test
    fun completingTheBoardShowsAResultAndClearsTheSave() {
        startAndSettle()

        var state = viewModel.uiState.value
        for (index in state.board.indices) {
            if (state.puzzle[index] == 0) {
                viewModel.selectCell(index)
                viewModel.inputDigit(state.solution[index])
            }
        }

        state = viewModel.uiState.value
        assertTrue("board should be complete", state.isComplete)
        assertNotNull(state.result)
        assertEquals(3, state.result!!.stars)
        assertEquals(1, state.stats.totalWins)
        assertEquals(null, repository.loadGame())
    }

    @Test
    fun progressIsSavedAndCanBeResumed() {
        startAndSettle()
        val empty = viewModel.uiState.value.board.indexOfFirst { it == 0 }
        viewModel.selectCell(empty)
        viewModel.inputDigit(viewModel.uiState.value.solution[empty])

        viewModel.exitToHome()
        val home = viewModel.uiState.value
        assertEquals(AppScreen.HOME, home.screen)
        assertTrue(home.hasSavedGame)
        assertNotNull(home.savedPreview)
        assertEquals(BoardSize.FOUR, home.savedPreview!!.size)

        val saved = repository.loadGame()
        assertNotNull(saved)
        assertEquals(Difficulty.EASY, saved!!.difficulty)
        assertEquals(BoardSize.FOUR, saved.size)

        val reopened = GameViewModel(application, computeDispatcher, NoFeedback)
        try {
            reopened.continueSavedGame()
            assertEquals(AppScreen.GAME, reopened.uiState.value.screen)
            assertEquals(saved.board, reopened.uiState.value.board)
            assertEquals(saved.seconds, reopened.uiState.value.seconds)
            assertEquals(BoardSize.FOUR, reopened.uiState.value.size)
        } finally {
            reopened.stopTimers()
        }
    }

    @Test
    fun sixBySixSaveKeepsItsSizeAfterRestart() {
        startAndSettle(BoardSize.SIX, Difficulty.MEDIUM)
        val empty = viewModel.uiState.value.board.indexOfFirst { it == 0 }
        viewModel.selectCell(empty)
        viewModel.inputDigit(viewModel.uiState.value.solution[empty])
        viewModel.exitToHome()

        val saved = repository.loadGame()
        assertNotNull(saved)
        assertEquals(BoardSize.SIX, saved!!.size)
        assertEquals(36, saved.board.size)

        val reopened = GameViewModel(application, computeDispatcher, NoFeedback)
        try {
            reopened.continueSavedGame()
            val state = reopened.uiState.value
            assertEquals(BoardSize.SIX, state.size)
            assertEquals(Difficulty.MEDIUM, state.difficulty)
            assertEquals(36, state.board.size)
        } finally {
            reopened.stopTimers()
        }
    }

    @Test
    fun pauseStopsTheClockAndKeepsTheProgress() {
        startAndSettle()
        val empty = viewModel.uiState.value.board.indexOfFirst { it == 0 }
        viewModel.selectCell(empty)
        viewModel.inputDigit(viewModel.uiState.value.solution[empty])

        viewModel.pauseGame()
        val paused = viewModel.uiState.value
        assertTrue(paused.isPaused)
        assertEquals(empty, paused.selectedCell)

        // Paused screens must not accept input.
        viewModel.inputDigit(1)
        assertEquals(paused.board, viewModel.uiState.value.board)

        viewModel.resumeFromPause()
        assertFalse(viewModel.uiState.value.isPaused)
    }

    @Test
    fun restartResetsProgressButKeepsTheSamePuzzle() {
        startAndSettle()
        val before = viewModel.uiState.value
        val empty = before.board.indexOfFirst { it == 0 }
        viewModel.selectCell(empty)
        viewModel.inputDigit(before.solution[empty])

        viewModel.restartPuzzle()
        val after = viewModel.uiState.value
        assertEquals(before.puzzle, after.puzzle)
        assertEquals(before.puzzle, after.board)
        assertEquals(0, after.seconds)
        assertEquals(0, after.mistakes)
        assertEquals(0, after.hints)
    }

    @Test
    fun selectionAndSizeAreRemembered() {
        viewModel.selectSize(BoardSize.NINE)
        viewModel.selectDifficulty(Difficulty.EXPERT)
        assertEquals(BoardSize.NINE, viewModel.uiState.value.size)
        assertEquals(Difficulty.EXPERT, viewModel.uiState.value.difficulty)

        val reopened = GameViewModel(application, computeDispatcher, NoFeedback)
        try {
            assertEquals(BoardSize.NINE, reopened.uiState.value.size)
            assertEquals(Difficulty.EXPERT, reopened.uiState.value.difficulty)
        } finally {
            reopened.stopTimers()
        }
    }

    @Test
    fun statsAreTrackedPerBoardSizeAndDifficulty() {
        startAndSettle(BoardSize.FOUR, Difficulty.EASY)
        solveCurrentBoard()
        startAndSettle(BoardSize.SIX, Difficulty.EASY)
        solveCurrentBoard()

        val stats = viewModel.uiState.value.stats
        assertEquals(2, stats.totalWins)
        assertNotNull(stats.bestTimeFor(BoardSize.FOUR, Difficulty.EASY))
        assertNotNull(stats.bestTimeFor(BoardSize.SIX, Difficulty.EASY))
        assertEquals(
            "a board size that was never won must have no record",
            null,
            stats.bestTimeFor(BoardSize.NINE, Difficulty.EASY),
        )
    }

    @Test
    fun legacyNineByNineSaveWithoutSizeFieldStillLoads() {
        // Older app versions only had 9x9, so a save without a "size" field must still work.
        startAndSettle(BoardSize.NINE, Difficulty.EASY)
        val state = viewModel.uiState.value
        val empty = state.board.indexOfFirst { it == 0 }
        viewModel.selectCell(empty)
        viewModel.inputDigit(state.solution[empty])
        viewModel.exitToHome()

        val prefs = application.getSharedPreferences("sudoku_kids_prefs", 0)
        val raw = prefs.getString("active_game", null)
        assertNotNull(raw)
        prefs.edit().putString("active_game", raw!!.replace("\"size\":\"9\",", "")).commit()

        val reopened = GameViewModel(application, computeDispatcher, NoFeedback)
        try {
            reopened.continueSavedGame()
            assertEquals(BoardSize.NINE, reopened.uiState.value.size)
            assertEquals(AppScreen.GAME, reopened.uiState.value.screen)
        } finally {
            reopened.stopTimers()
        }
    }

    @Test
    fun leaderboardOpensOnTheBoardThatWasJustPlayed() {
        startAndSettle(BoardSize.SIX, Difficulty.HARD)
        solveCurrentBoard()

        val result = viewModel.uiState.value.result
        assertNotNull("the win must be ranked", result?.rank)
        assertEquals(1, result!!.rank)

        viewModel.openLeaderboard()
        val state = viewModel.uiState.value
        assertEquals(AppScreen.LEADERBOARD, state.screen)
        assertEquals(BoardSize.SIX, state.leaderboardSize)
        assertEquals(Difficulty.HARD, state.leaderboardDifficulty)
        assertEquals(1, state.leaderboard.size)
        assertEquals(
            "the leaderboard must show the game that was just finished",
            result.seconds,
            state.leaderboard.first().seconds,
        )

        viewModel.closeLeaderboard()
        assertEquals(AppScreen.HOME, viewModel.uiState.value.screen)
    }

    @Test
    fun openingTheLeaderboardDismissesTheWinDialog() {
        startAndSettle(BoardSize.FOUR, Difficulty.EASY)
        solveCurrentBoard()
        assertTrue("the win dialog must be up first", viewModel.uiState.value.isComplete)
        assertNotNull(viewModel.uiState.value.result)

        viewModel.openLeaderboard()
        val state = viewModel.uiState.value
        assertEquals(AppScreen.LEADERBOARD, state.screen)
        assertFalse(
            "closing the game screen must also close the win dialog",
            state.isComplete,
        )
        assertEquals(null, state.result)
        assertEquals("a finished game has nothing left to undo", false, state.canUndo)
        assertEquals(null, state.selectedCell)
    }

    @Test
    fun leaderboardTabsSwitchWithoutChangingTheBoardSelection() {
        startAndSettle(BoardSize.FOUR, Difficulty.EASY)
        solveCurrentBoard()
        viewModel.openLeaderboard()

        viewModel.selectLeaderboardSize(BoardSize.NINE)
        assertEquals(BoardSize.NINE, viewModel.uiState.value.leaderboardSize)
        assertEquals(
            "switching leaderboard tabs must not change the next game",
            BoardSize.FOUR,
            viewModel.uiState.value.size,
        )
        assertEquals(emptyList<Any>(), viewModel.uiState.value.leaderboard)

        viewModel.selectLeaderboardDifficulty(Difficulty.EXPERT)
        assertEquals(Difficulty.EXPERT, viewModel.uiState.value.leaderboardDifficulty)

        viewModel.selectLeaderboardSize(BoardSize.FOUR)
        viewModel.selectLeaderboardDifficulty(Difficulty.EASY)
        assertEquals(1, viewModel.uiState.value.leaderboard.size)
    }

    @Test
    fun winningTheSameLevelTwiceKeepsBothResultsOnTheLeaderboard() {
        startAndSettle(BoardSize.FOUR, Difficulty.EASY)
        solveCurrentBoard()
        startAndSettle(BoardSize.FOUR, Difficulty.EASY)
        solveCurrentBoard()

        assertEquals(2, viewModel.uiState.value.stats.totalWins)

        viewModel.openLeaderboard()
        assertEquals(2, viewModel.uiState.value.leaderboard.size)
    }

    // --- 错题提示 -------------------------------------------------------------

    /** Finds an empty cell and a digit that is guaranteed to be wrong there. */
    private fun findWrongMove(): Triple<Int, Int, Int>? {
        val state = viewModel.uiState.value
        val index = state.puzzle.indexOfFirst { it == 0 }
        if (index < 0) return null
        val correct = state.solution[index]
        val wrong = (1..state.size.size).firstOrNull { it != correct } ?: return null
        return Triple(index, wrong, correct)
    }

    @Test
    fun aWrongDigitIsExplainedToThePlayer() {
        startAndSettle(BoardSize.FOUR, Difficulty.EASY)
        val (index, wrong, _) = findWrongMove()!!
        viewModel.selectCell(index)
        viewModel.inputDigit(wrong)

        val conflict = viewModel.uiState.value.conflict
        assertNotNull("a wrong digit must produce an explanation", conflict)
        assertTrue(
            "the sentence should tell the child what happened, got: ${conflict!!.text}",
            conflict.text.contains("已经") || conflict.text.contains("再看看"),
        )
    }

    @Test
    fun theExplanationPointsAtTheClashingCells() {
        startAndSettle(BoardSize.FOUR, Difficulty.EASY)
        val (index, wrong, _) = findWrongMove()!!
        viewModel.selectCell(index)
        viewModel.inputDigit(wrong)

        val conflict = viewModel.uiState.value.conflict!!
        // Any cells listed must really hold the digit that was just rejected.
        conflict.cells.forEach { cell ->
            assertEquals(wrong, viewModel.uiState.value.board[cell])
        }
    }

    @Test
    fun aCorrectDigitClearsTheExplanation() {
        startAndSettle(BoardSize.FOUR, Difficulty.EASY)
        val (index, wrong, correct) = findWrongMove()!!

        viewModel.selectCell(index)
        viewModel.inputDigit(wrong)
        assertNotNull(viewModel.uiState.value.conflict)

        viewModel.inputDigit(correct)
        assertEquals(
            "placing the right digit must clear the hint",
            null,
            viewModel.uiState.value.conflict,
        )
    }

    @Test
    fun selectingAnotherCellClearsTheExplanation() {
        startAndSettle(BoardSize.FOUR, Difficulty.EASY)
        val (index, wrong, _) = findWrongMove()!!
        viewModel.selectCell(index)
        viewModel.inputDigit(wrong)
        assertNotNull(viewModel.uiState.value.conflict)

        val otherIndex = viewModel.uiState.value.puzzle
            .indices
            .first { it != index && viewModel.uiState.value.puzzle[it] == 0 }
        viewModel.selectCell(otherIndex)

        assertEquals(null, viewModel.uiState.value.conflict)
    }

    @Test
    fun theExplanationClearsItselfAfterAWhile() {
        startAndSettle(BoardSize.FOUR, Difficulty.EASY)
        val (index, wrong, _) = findWrongMove()!!
        viewModel.selectCell(index)
        viewModel.inputDigit(wrong)
        assertNotNull(viewModel.uiState.value.conflict)

        // Let the auto-dismiss delay elapse.
        shadowOf(Looper.getMainLooper()).idleFor(
            java.time.Duration.ofMillis(GameViewModel.CONFLICT_VISIBLE_MS + 500),
        )

        assertEquals(null, viewModel.uiState.value.conflict)
    }

    // --- 安静模式 -------------------------------------------------------------

    @Test
    fun quietModeStartsOffAndCanBeToggled() {
        assertFalse(viewModel.uiState.value.quietMode)

        viewModel.toggleQuietMode()
        assertTrue(viewModel.uiState.value.quietMode)

        viewModel.toggleQuietMode()
        assertFalse(viewModel.uiState.value.quietMode)
    }

    @Test
    fun quietModeIsRememberedAcrossRestarts() {
        viewModel.toggleQuietMode()
        viewModel.stopTimers()

        val reopened = GameViewModel(application, computeDispatcher, NoFeedback)
        try {
            assertTrue("the setting must survive a restart", reopened.uiState.value.quietMode)
        } finally {
            reopened.stopTimers()
        }
    }

    private fun solveCurrentBoard() {
        var state = viewModel.uiState.value
        for (index in state.board.indices) {
            if (state.puzzle[index] == 0) {
                viewModel.selectCell(index)
                viewModel.inputDigit(state.solution[index])
            }
            state = viewModel.uiState.value
        }
        assertTrue("board should be complete", viewModel.uiState.value.isComplete)
    }
}

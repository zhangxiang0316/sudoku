package com.family.sudoku.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.family.sudoku.data.BoardSize
import com.family.sudoku.data.Difficulty
import com.family.sudoku.data.GameRepository
import com.family.sudoku.data.toPreview
import com.family.sudoku.feedback.GameFeedback
import com.family.sudoku.feedback.SoundHapticFeedback
import com.family.sudoku.game.ConflictExplainer
import com.family.sudoku.game.HintPlanner
import com.family.sudoku.game.HintStage
import com.family.sudoku.game.AppScreen
import com.family.sudoku.game.BoardSpec
import com.family.sudoku.game.GameResult
import com.family.sudoku.game.PlayerStats
import com.family.sudoku.game.SavedGame
import com.family.sudoku.game.SavedGamePreview
import com.family.sudoku.game.ScoreEntry
import com.family.sudoku.game.SudokuGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

data class GameUiState(
    val screen: AppScreen = AppScreen.HOME,
    val size: BoardSize = BoardSize.FOUR,
    val difficulty: Difficulty = Difficulty.EASY,
    val board: List<Int> = emptyBoard(BoardSize.FOUR),
    val puzzle: List<Int> = emptyBoard(BoardSize.FOUR),
    val solution: List<Int> = emptyBoard(BoardSize.FOUR),
    val notes: List<Int> = emptyBoard(BoardSize.FOUR),
    val selectedCell: Int? = null,
    val noteMode: Boolean = false,
    val mistakes: Int = 0,
    val hints: Int = 0,
    val seconds: Int = 0,
    val isGenerating: Boolean = false,
    val generationFailed: Boolean = false,
    val isPaused: Boolean = false,
    val isComplete: Boolean = false,
    val result: GameResult? = null,
    val canUndo: Boolean = false,
    val hasSavedGame: Boolean = false,
    val savedPreview: SavedGamePreview? = null,
    val stats: PlayerStats = PlayerStats(),
    val leaderboardSize: BoardSize = BoardSize.FOUR,
    val leaderboardDifficulty: Difficulty = Difficulty.EASY,
    val leaderboard: List<ScoreEntry> = emptyList(),
    val quietMode: Boolean = false,
    val conflict: ConflictMessage? = null,
    /** Which step the next hint will show: guide, focus, then reveal. */
    val hintStage: HintStage = HintStage.GUIDE,
    /** Cell the current hint is pointing at, highlighted on the board. */
    val hintFocus: Int? = null,
    /** Sentence shown for the current teaching hint. */
    val hintText: String? = null,
) {
    val spec: BoardSpec get() = size.spec
    val cellCount: Int get() = size.cellCount
    val filledCount: Int get() = board.count { it != 0 }
    val givenCount: Int get() = puzzle.count { it != 0 }
    val hintAllowance: Int get() = size.hintAllowance
    val isEditableSelection: Boolean
        get() = selectedCell?.let { puzzle.getOrNull(it) == 0 } == true
}

/**
 * The "why was that wrong" hint shown under the board. [cells] points at the existing digits
 * that caused the clash so the board can highlight them, and [id] lets the UI re-run its
 * animation when the same sentence is triggered twice in a row.
 */
data class ConflictMessage(
    val id: Long,
    val text: String,
    val cells: List<Int> = emptyList(),
)

class GameViewModel @JvmOverloads constructor(
    application: Application,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    feedback: GameFeedback? = null,
) : AndroidViewModel(application) {
    private val repository = GameRepository(application)

    /** Sound and vibration. Swapped for a no-op recorder in tests. */
    private val feedback: GameFeedback = feedback ?: SoundHapticFeedback(application)
    private val initialSavedGame = repository.loadGame()

    private val _uiState = MutableStateFlow(
        GameUiState(
            size = repository.lastSize() ?: BoardSize.FOUR,
            difficulty = repository.lastDifficulty(),
            stats = repository.loadStats(),
            hasSavedGame = initialSavedGame != null,
            savedPreview = initialSavedGame?.toPreview(),
            quietMode = repository.isQuietMode(),
        ),
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null
    private var timerJob: Job? = null
    private var conflictJob: Job? = null
    private var conflictSeq = 0L
    private val undoStack = ArrayDeque<UndoSnapshot>()

    init {
        applyQuietMode(_uiState.value.quietMode)
    }

    private fun applyQuietMode(quiet: Boolean) {
        (feedback as? SoundHapticFeedback)?.quiet = quiet
    }

    /**
     * One switch for both sound and vibration. The real use case is "the little one is
     * playing while the baby sleeps", so both should stop together.
     */
    fun toggleQuietMode() {
        val next = !_uiState.value.quietMode
        repository.saveQuietMode(next)
        applyQuietMode(next)
        _uiState.update { it.copy(quietMode = next) }
        if (!next) feedback.tap()
    }

    fun selectSize(size: BoardSize) {
        if (_uiState.value.screen != AppScreen.HOME) return
        repository.saveLastSelection(size, _uiState.value.difficulty)
        _uiState.update { it.copy(size = size) }
    }

    fun selectDifficulty(difficulty: Difficulty) {
        if (_uiState.value.screen != AppScreen.HOME) return
        repository.saveLastSelection(_uiState.value.size, difficulty)
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun startNewGame(
        size: BoardSize = _uiState.value.size,
        difficulty: Difficulty = _uiState.value.difficulty,
    ) {
        generationJob?.cancel()
        timerJob?.cancel()
        timerJob = null
        undoStack.clear()
        clearConflict()
        repository.clearGame()
        repository.saveLastSelection(size, difficulty)

        val blank = emptyBoard(size)
        _uiState.update {
            it.copy(
                screen = AppScreen.GAME,
                size = size,
                difficulty = difficulty,
                board = blank,
                puzzle = blank,
                solution = blank,
                notes = blank,
                selectedCell = null,
                noteMode = false,
                mistakes = 0,
                hints = 0,
                seconds = 0,
                isGenerating = true,
                generationFailed = false,
                isPaused = false,
                isComplete = false,
                result = null,
                canUndo = false,
                hasSavedGame = false,
                savedPreview = null,
                hintStage = HintStage.GUIDE,
                hintFocus = null,
                hintText = null,
            )
        }

        val clueTarget = difficulty.clueTargetFor(size)
        generationJob = viewModelScope.launch {
            try {
                val generated = withContext(computeDispatcher) {
                    SudokuGenerator.generate(size.spec, clueTarget)
                }
                ensureActive()

                val puzzle = generated.puzzle.toList()
                val solution = generated.solution.toList()
                val firstEmpty = puzzle.indexOfFirst { it == 0 }

                _uiState.update { state ->
                    if (state.screen != AppScreen.GAME || !state.isGenerating || state.size != size) {
                        state
                    } else {
                        state.copy(
                            board = puzzle,
                            puzzle = puzzle,
                            solution = solution,
                            notes = emptyBoard(size),
                            selectedCell = firstEmpty.takeIf { it >= 0 },
                            isGenerating = false,
                            generationFailed = false,
                            canUndo = false,
                        )
                    }
                }

                if (_uiState.value.screen == AppScreen.GAME && !_uiState.value.isGenerating) {
                    saveActiveGame()
                    startTimer()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.update { state ->
                    if (state.screen == AppScreen.GAME) {
                        state.copy(isGenerating = false, generationFailed = true)
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun retryGeneration() {
        startNewGame(_uiState.value.size, _uiState.value.difficulty)
    }

    fun continueSavedGame() {
        val saved = repository.loadGame()
        if (saved == null) {
            _uiState.update {
                it.copy(
                    hasSavedGame = false,
                    savedPreview = null,
                )
            }
            return
        }
        resume(saved)
    }

    private fun resume(saved: SavedGame) {
        generationJob?.cancel()
        generationJob = null
        timerJob?.cancel()
        undoStack.clear()

        val firstEmpty = saved.board.indexOfFirst { it == 0 }
        _uiState.update {
            it.copy(
                screen = AppScreen.GAME,
                size = saved.size,
                difficulty = saved.difficulty,
                board = saved.board,
                puzzle = saved.puzzle,
                solution = saved.solution,
                notes = saved.notes,
                selectedCell = firstEmpty.takeIf { index -> index >= 0 },
                noteMode = false,
                mistakes = saved.mistakes,
                hints = saved.hints,
                seconds = saved.seconds,
                isGenerating = false,
                generationFailed = false,
                isPaused = false,
                isComplete = false,
                result = null,
                canUndo = false,
                hasSavedGame = false,
                savedPreview = null,
            )
        }
        startTimer()
    }

    fun exitToHome() {
        generationJob?.cancel()
        generationJob = null
        timerJob?.cancel()
        timerJob = null

        val current = _uiState.value
        if (current.screen != AppScreen.GAME) return

        if (!current.isComplete &&
            !current.isGenerating &&
            current.puzzle.any { it != 0 }
        ) {
            saveActiveGame(current)
            val preview = SavedGamePreview(
                size = current.size,
                difficulty = current.difficulty,
                filledCount = current.board.count { it != 0 },
                totalCount = current.cellCount,
                seconds = current.seconds,
            )
            _uiState.value = current.copy(
                screen = AppScreen.HOME,
                selectedCell = null,
                isPaused = false,
                result = null,
                canUndo = false,
                hasSavedGame = true,
                savedPreview = preview,
            )
        } else {
            _uiState.value = current.copy(
                screen = AppScreen.HOME,
                selectedCell = null,
                isPaused = false,
                result = null,
                canUndo = false,
                hasSavedGame = false,
                savedPreview = null,
            )
        }
    }

    /** Opens the leaderboard, starting on whichever board the player last finished. */
    fun openLeaderboard() {
        val current = _uiState.value
        val size = current.leaderboardSize.takeIf { current.screen == AppScreen.LEADERBOARD }
            ?: current.size
        val difficulty = current.difficulty
        // Leaving the game for the leaderboard also dismisses the win dialog; otherwise the
        // dialog would stay on top of the new screen because it is driven by isComplete.
        _uiState.update {
            it.copy(
                screen = AppScreen.LEADERBOARD,
                leaderboardSize = size,
                leaderboardDifficulty = difficulty,
                leaderboard = repository.loadLeaderboard(size, difficulty),
                isComplete = false,
                result = null,
                selectedCell = null,
                isPaused = false,
                canUndo = false,
            )
        }
    }

    fun closeLeaderboard() {
        if (_uiState.value.screen != AppScreen.LEADERBOARD) return
        _uiState.update { it.copy(screen = AppScreen.HOME) }
    }

    fun selectLeaderboardSize(size: BoardSize) {
        if (_uiState.value.screen != AppScreen.LEADERBOARD) return
        _uiState.update {
            it.copy(
                leaderboardSize = size,
                leaderboard = repository.loadLeaderboard(size, it.leaderboardDifficulty),
            )
        }
    }

    fun selectLeaderboardDifficulty(difficulty: Difficulty) {
        if (_uiState.value.screen != AppScreen.LEADERBOARD) return
        _uiState.update {
            it.copy(
                leaderboardDifficulty = difficulty,
                leaderboard = repository.loadLeaderboard(it.leaderboardSize, difficulty),
            )
        }
    }

    fun selectCell(index: Int) {
        val current = _uiState.value
        if (index !in 0 until current.cellCount) return
        if (current.conflict != null) clearConflict()
        if (current.screen != AppScreen.GAME ||
            current.isGenerating ||
            current.isPaused ||
            current.isComplete
        ) {
            return
        }
        _uiState.update { it.copy(selectedCell = index) }
    }

    /** Shows the explanation under the board and clears it again after a few seconds. */
    private fun showConflict(text: String, cells: List<Int>) {
        conflictSeq += 1
        val message = ConflictMessage(id = conflictSeq, text = text, cells = cells)
        _uiState.update { it.copy(conflict = message) }
        conflictJob?.cancel()
        conflictJob = viewModelScope.launch {
            delay(CONFLICT_VISIBLE_MS)
            _uiState.update { state ->
                if (state.conflict?.id == message.id) state.copy(conflict = null) else state
            }
        }
    }

    private fun clearConflict() {
        conflictJob?.cancel()
        conflictJob = null
        if (_uiState.value.conflict != null) {
            _uiState.update { it.copy(conflict = null) }
        }
    }

    fun inputDigit(digit: Int) {
        val current = _uiState.value
        if (digit !in 1..current.size.size) return
        if (!canEdit(current)) return
        val index = current.selectedCell ?: return
        if (current.puzzle[index] != 0) return

        if (current.noteMode) {
            if (current.board[index] != 0) return
            pushUndo(current)
            val notes = current.notes.toMutableList()
            notes[index] = notes[index] xor (1 shl (digit - 1))
            _uiState.update { it.copy(notes = notes, canUndo = true) }
            saveActiveGame()
            return
        }

        if (current.board[index] == digit) {
            pushUndo(current)
            val board = current.board.toMutableList().also { it[index] = 0 }
            _uiState.update { it.copy(board = board, canUndo = true) }
            saveActiveGame()
            return
        }

        pushUndo(current)
        val board = current.board.toMutableList().also { it[index] = digit }
        val notes = current.notes.toMutableList().also { it[index] = 0 }
        val isWrong = digit != current.solution[index]

        if (!isWrong) {
            val bit = 1 shl (digit - 1)
            for (peer in current.spec.peersOf(index)) {
                if (board[peer] == 0) {
                    notes[peer] = notes[peer] and bit.inv()
                }
            }
        }

        _uiState.update {
            it.copy(
                board = board,
                notes = notes,
                mistakes = it.mistakes + if (isWrong) 1 else 0,
                canUndo = true,
            )
        }
        saveActiveGame()

        if (isWrong) {
            feedback.wrong()
            val conflict = ConflictExplainer.explain(board, current.spec, index, digit)
            showConflict(ConflictExplainer.messageFor(conflict, digit), conflict.cells)
        } else {
            feedback.correct()
            clearConflict()
        }

        if (!isWrong && board == current.solution) {
            finishGame()
        }
    }

    fun eraseSelected() {
        val current = _uiState.value
        if (!canEdit(current)) return
        val index = current.selectedCell ?: return
        if (current.puzzle[index] != 0) return
        if (current.board[index] == 0 && current.notes[index] == 0) return

        pushUndo(current)
        val board = current.board.toMutableList().also { it[index] = 0 }
        val notes = current.notes.toMutableList().also { it[index] = 0 }
        _uiState.update {
            it.copy(board = board, notes = notes, canUndo = true)
        }
        saveActiveGame()
        clearConflict()
        feedback.tap()
    }

    fun undo() {
        val current = _uiState.value
        if (current.screen != AppScreen.GAME ||
            current.isGenerating ||
            current.isPaused ||
            current.isComplete
        ) {
            return
        }
        val snapshot = undoStack.removeLastOrNull() ?: return
        clearConflict()
        _uiState.update {
            it.copy(
                board = snapshot.board,
                notes = snapshot.notes,
                selectedCell = snapshot.selectedCell,
                canUndo = undoStack.isNotEmpty(),
            )
        }
        saveActiveGame()
    }

    fun toggleNoteMode() {
        val current = _uiState.value
        if (current.screen != AppScreen.GAME ||
            current.isGenerating ||
            current.isPaused ||
            current.isComplete
        ) {
            return
        }
        _uiState.update { it.copy(noteMode = !it.noteMode) }
    }

    fun useHint() {
        val current = _uiState.value
        if (current.screen != AppScreen.GAME || current.isGenerating ||
            current.isPaused || current.isComplete
        ) {
            return
        }
        if (current.hints >= current.hintAllowance) return
        clearConflict()

        val step = HintPlanner.next(
            board = current.board,
            solution = current.solution,
            spec = current.spec,
            preferredCell = current.selectedCell,
            stage = current.hintStage,
        ) ?: return

        when (step.stage) {
            HintStage.GUIDE, HintStage.FOCUS -> {
                // The first two steps only point at a technique or a cell. They do not fill
                // anything in, so they cost no hint allowance; otherwise a child would be
                // punished for asking to be taught.
                _uiState.update {
                    it.copy(
                        hintStage = if (step.stage == HintStage.GUIDE) {
                            HintStage.FOCUS
                        } else {
                            HintStage.REVEAL
                        },
                        hintFocus = step.cell,
                        hintText = step.message,
                    )
                }
                conflictJob?.cancel()
                conflictJob = viewModelScope.launch {
                    delay(HINT_VISIBLE_MS)
                    _uiState.update { it.copy(hintText = null, hintFocus = null) }
                }
            }

            HintStage.REVEAL -> {
                val digit = step.digit ?: return
                val target = step.cell ?: return
                val board = current.board.toMutableList().also { it[target] = digit }
                val notes = current.notes.toMutableList().also { it[target] = 0 }
                val bit = 1 shl (digit - 1)
                for (peer in current.spec.peersOf(target)) {
                    if (board[peer] == 0) {
                        notes[peer] = notes[peer] and bit.inv()
                    }
                }

                _uiState.update {
                    it.copy(
                        board = board,
                        notes = notes,
                        hints = it.hints + 1,
                        selectedCell = target,
                        noteMode = false,
                        hintStage = HintStage.GUIDE,
                        hintFocus = null,
                        hintText = null,
                    )
                }
                saveActiveGame()
                if (board == current.solution) finishGame()
            }
        }
    }

    fun restartPuzzle() {
        val current = _uiState.value
        if (current.screen != AppScreen.GAME || current.isGenerating || current.puzzle.none { it != 0 }) {
            return
        }
        timerJob?.cancel()
        undoStack.clear()
        clearConflict()
        val firstEmpty = current.puzzle.indexOfFirst { it == 0 }

        _uiState.value = current.copy(
            board = current.puzzle,
            notes = emptyBoard(current.size),
            selectedCell = firstEmpty.takeIf { it >= 0 },
            noteMode = false,
            mistakes = 0,
            hints = 0,
            seconds = 0,
            isPaused = false,
            isComplete = false,
            result = null,
            canUndo = false,
            hasSavedGame = false,
            savedPreview = null,
        )
        saveActiveGame()
        startTimer()
    }

    fun playAgain() {
        startNewGame(_uiState.value.size, _uiState.value.difficulty)
    }

    fun goHomeAfterCompletion() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update {
            it.copy(
                screen = AppScreen.HOME,
                selectedCell = null,
                isPaused = false,
                result = null,
                canUndo = false,
                hasSavedGame = false,
                savedPreview = null,
            )
        }
    }

    fun pauseGame() {
        val current = _uiState.value
        if (current.screen != AppScreen.GAME ||
            current.isGenerating ||
            current.isPaused ||
            current.isComplete
        ) {
            return
        }
        clearConflict()
        _uiState.update { it.copy(isPaused = true) }
        saveActiveGame()
    }

    fun resumeFromPause() {
        val current = _uiState.value
        if (!current.isPaused) return
        _uiState.update { it.copy(isPaused = false) }
    }

    fun onAppBackgrounded() {
        val current = _uiState.value
        if (current.screen != AppScreen.GAME ||
            current.isGenerating ||
            current.isComplete ||
            current.puzzle.none { it != 0 }
        ) {
            return
        }
        if (!current.isPaused) {
            _uiState.update { it.copy(isPaused = true) }
        }
        saveActiveGame()
    }

    /**
     * Stops the repeating timer. Called when the screen is disposed and by tests so that
     * no pending work is left on the dispatcher.
     */
    fun stopTimers() {
        generationJob?.cancel()
        generationJob = null
        timerJob?.cancel()
        timerJob = null
        conflictJob?.cancel()
        conflictJob = null
    }

    override fun onCleared() {
        saveActiveGame()
        stopTimers()
        super.onCleared()
    }

    private fun canEdit(state: GameUiState): Boolean =
        state.screen == AppScreen.GAME &&
            !state.isGenerating &&
            !state.isPaused &&
            !state.isComplete &&
            state.selectedCell != null

    private fun pushUndo(state: GameUiState) {
        if (undoStack.size >= MAX_UNDO) {
            undoStack.removeFirst()
        }
        undoStack.addLast(
            UndoSnapshot(
                board = state.board,
                notes = state.notes,
                selectedCell = state.selectedCell,
            ),
        )
    }

    private fun finishGame() {
        val current = _uiState.value
        if (current.isComplete || current.board != current.solution) return

        val penalty = current.mistakes + current.hints
        val stars = when {
            penalty == 0 -> 3
            penalty <= 3 -> 2
            else -> 1
        }
        val record = repository.recordWin(
            size = current.size,
            difficulty = current.difficulty,
            seconds = current.seconds,
            stars = stars,
            mistakes = current.mistakes,
            hints = current.hints,
        )
        repository.clearGame()
        timerJob?.cancel()
        timerJob = null
        undoStack.clear()
        clearConflict()

        _uiState.update {
            it.copy(
                isComplete = true,
                isPaused = false,
                canUndo = false,
                result = GameResult(
                    seconds = current.seconds,
                    mistakes = current.mistakes,
                    hints = current.hints,
                    stars = stars,
                    bestSeconds = record.bestSeconds,
                    isNewBest = record.isNewBest,
                    rank = record.rank,
                ),
                stats = repository.loadStats(),
                leaderboardSize = current.size,
                leaderboardDifficulty = current.difficulty,
                leaderboard = repository.loadLeaderboard(current.size, current.difficulty),
                hasSavedGame = false,
                savedPreview = null,
            )
        }
        feedback.win()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                val current = _uiState.value
                if (current.screen == AppScreen.GAME &&
                    !current.isGenerating &&
                    !current.isPaused &&
                    !current.isComplete
                ) {
                    val nextSeconds = min(current.seconds + 1, MAX_GAME_SECONDS)
                    _uiState.update { it.copy(seconds = nextSeconds) }
                    if (nextSeconds % 5 == 0) {
                        saveActiveGame()
                    }
                }
            }
        }
    }

    private fun saveActiveGame(state: GameUiState = _uiState.value) {
        if (state.screen != AppScreen.GAME ||
            state.isGenerating ||
            state.isComplete ||
            state.puzzle.size != state.cellCount ||
            state.puzzle.none { it != 0 }
        ) {
            return
        }
        repository.saveGame(
            SavedGame(
                size = state.size,
                difficulty = state.difficulty,
                puzzle = state.puzzle,
                solution = state.solution,
                board = state.board,
                notes = state.notes,
                seconds = state.seconds,
                mistakes = state.mistakes,
                hints = state.hints,
            ),
        )
    }

    private data class UndoSnapshot(
        val board: List<Int>,
        val notes: List<Int>,
        val selectedCell: Int?,
    )

    companion object {
        /** Long enough for a child to read the sentence, short enough not to linger. */
        const val CONFLICT_VISIBLE_MS = 3_200L

        /** A teaching hint is a little longer than a mistake note, so it can be read. */
        const val HINT_VISIBLE_MS = 4_800L

        private const val MAX_UNDO = 100
        private const val MAX_GAME_SECONDS = 24 * 60 * 60
    }
}

private fun emptyBoard(size: BoardSize): List<Int> = List(size.cellCount) { 0 }

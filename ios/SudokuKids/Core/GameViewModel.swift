import Combine
import Foundation

@MainActor
final class GameViewModel: ObservableObject {
    enum Screen { case home, game, leaderboard }
    enum Dialog: Identifiable {
        case pause, exit, restart, win
        var id: String { String(describing: self) }
    }

    @Published var screen: Screen = .home
    @Published var selectedSize: BoardSize
    @Published var selectedDifficulty: Difficulty
    @Published var leaderboardSize: BoardSize = .four
    @Published var leaderboardDifficulty: Difficulty = .easy
    @Published var board: [Int] = []
    @Published var puzzle: [Int] = []
    @Published var solution: [Int] = []
    @Published var notes: [Int] = []
    @Published var selectedCell: Int?
    @Published var noteMode = false
    @Published var mistakes = 0
    @Published var hints = 0
    @Published var seconds = 0
    @Published var isPaused = false
    @Published var isGenerating = false
    @Published var isComplete = false
    @Published var canUndo = false
    @Published var conflict: ConflictInfo?
    @Published var hintStage: HintStage = .guide
    @Published var hintFocus: Int?
    @Published var hintText: String?
    @Published var dialog: Dialog?
    @Published var quietMode: Bool
    @Published var lastResult: GameResult?

    private let store: GameStore
    private var timer: Timer?
    private var generationTask: Task<Void, Never>?
    private var conflictWork: DispatchWorkItem?
    private var undoStack: [UndoState] = []
    private var gameKey: GameKey?

    struct GameResult {
        var seconds: Int
        var mistakes: Int
        var hints: Int
        var stars: Int
        var best: Int
        var isNewBest: Bool
        var rank: Int?
    }

    init(store: GameStore? = nil) {
        let resolvedStore = store ?? GameServices.shared.store
        self.store = resolvedStore
        selectedSize = resolvedStore.lastSize ?? .four
        selectedDifficulty = resolvedStore.lastDifficulty
        quietMode = resolvedStore.quietMode
    }

    var cellCount: Int { board.count }
    var filledCount: Int { board.filter { $0 != 0 }.count }
    var hintAllowance: Int { selectedSize.hintAllowance }
    var currentLeaderboard: [ScoreEntry] {
        store.leaderboard(size: leaderboardSize, difficulty: leaderboardDifficulty)
    }
    var savedGame: SavedGame? { store.loadGame() }
    var totalWins: Int { store.totalWins }
    var totalStars: Int { store.totalStars }
    func bestTime(size: BoardSize, difficulty: Difficulty) -> Int? {
        store.bestTime(size: size, difficulty: difficulty)
    }

    func selectSize(_ size: BoardSize) {
        selectedSize = size
        store.lastSize = size
    }
    func selectDifficulty(_ difficulty: Difficulty) {
        selectedDifficulty = difficulty
        store.lastDifficulty = difficulty
    }

    func startNewGame(size: BoardSize? = nil, difficulty: Difficulty? = nil) {
        let size = size ?? selectedSize
        let difficulty = difficulty ?? selectedDifficulty
        selectedSize = size
        selectedDifficulty = difficulty
        store.lastSize = size
        store.lastDifficulty = difficulty
        store.clearGame()
        stopTimer()
        generationTask?.cancel()
        conflictWork?.cancel()
        undoStack.removeAll()
        isGenerating = true
        screen = .game
        board = []
        puzzle = []
        solution = []
        notes = []
        selectedCell = nil
        noteMode = false
        mistakes = 0
        hints = 0
        seconds = 0
        isPaused = false
        isComplete = false
        canUndo = false
        conflict = nil
        hintStage = .guide
        hintFocus = nil
        hintText = nil
        dialog = nil
        gameKey = GameKey(size: size, difficulty: difficulty)

        // Generating a 9x9 board measured up to 64ms on the main thread, which is long
        // enough to be felt as a stutter when starting a game. It now runs detached and the
        // screen shows a loading overlay until the puzzle arrives.
        generationTask?.cancel()
        generationTask = Task { [weak self] in
            let generated = await Task.detached(priority: .userInitiated) {
                SudokuEngine.generate(size: size, difficulty: difficulty)
            }.value

            guard !Task.isCancelled, let self else { return }
            // The player may have gone home or picked another board while this was running.
            guard self.isGenerating, self.gameKey == GameKey(size: size, difficulty: difficulty) else {
                return
            }
            self.puzzle = generated.puzzle
            self.solution = generated.solution
            self.board = generated.puzzle
            self.notes = Array(repeating: 0, count: size.count)
            self.selectedCell = generated.puzzle.firstIndex(of: 0)
            self.isGenerating = false
            self.startTimer()
            self.saveActiveGame()
        }
    }

    func continueGame() {
        guard let saved = store.loadGame() else { return }
        selectedSize = saved.size
        selectedDifficulty = saved.difficulty
        gameKey = GameKey(size: saved.size, difficulty: saved.difficulty)
        puzzle = saved.puzzle
        solution = saved.solution
        board = saved.board
        notes = saved.notes
        seconds = saved.seconds
        mistakes = saved.mistakes
        hints = saved.hints
        selectedCell = saved.board.firstIndex(of: 0)
        screen = .game
        isPaused = false
        isComplete = false
        noteMode = false
        undoStack.removeAll()
        startTimer()
    }

    func selectCell(_ index: Int) {
        guard screen == .game, !isPaused, !isComplete, board.indices.contains(index) else { return }
        selectedCell = index
        clearConflict()
    }

    func inputDigit(_ digit: Int) {
        guard screen == .game, !isPaused, !isComplete,
              (1...selectedSize.rawValue).contains(digit),
              let index = selectedCell, puzzle.indices.contains(index), puzzle[index] == 0 else { return }

        if noteMode {
            guard board[index] == 0 else { return }
            pushUndo()
            notes[index] ^= (1 << (digit - 1))
            canUndo = true
            saveActiveGame()
            return
        }
        if board[index] == digit {
            pushUndo()
            board[index] = 0
            canUndo = true
            saveActiveGame()
            clearConflict()
            return
        }
        pushUndo()
        board[index] = digit
        notes[index] = 0
        canUndo = true
        if digit == solution[index] {
            let bit = 1 << (digit - 1)
            for peer in selectedSize.peers(of: index) where board[peer] == 0 {
                notes[peer] &= ~bit
            }
            GameFeedback.correct()
            clearConflict()
        } else {
            mistakes += 1
            GameFeedback.wrong()
            showConflict(ConflictExplainer.explain(board: board, size: selectedSize, index: index, digit: digit))
        }
        saveActiveGame()
        if board == solution { finishGame() }
    }

    func eraseSelected() {
        guard screen == .game, !isPaused, !isComplete,
              let index = selectedCell, puzzle[index] == 0,
              board[index] != 0 || notes[index] != 0 else { return }
        pushUndo()
        board[index] = 0
        notes[index] = 0
        canUndo = true
        saveActiveGame()
        clearConflict()
        GameFeedback.tap()
    }

    func toggleNotes() {
        guard screen == .game, !isPaused, !isComplete else { return }
        noteMode.toggle()
    }

    func undo() {
        guard screen == .game, !isPaused, !isComplete, let previous = undoStack.popLast() else { return }
        board = previous.board
        notes = previous.notes
        selectedCell = previous.selected
        canUndo = !undoStack.isEmpty
        clearConflict()
        saveActiveGame()
    }

    func useHint() {
        guard screen == .game, !isPaused, !isComplete, hints < hintAllowance else { return }
        clearConflict()

        guard let step = HintPlanner.next(
            board: board, solution: solution, size: selectedSize,
            preferredCell: selectedCell, stage: hintStage
        ) else { return }

        switch step.stage {
        case .guide, .focus:
            // The first two steps only point at a technique or a cell; nothing is filled in,
            // so they cost no allowance. Being taught should not be penalised.
            hintStage = step.stage == .guide ? .focus : .reveal
            hintFocus = step.cell
            hintText = step.message
            scheduleHintDismissal()
        case .reveal:
            guard let target = step.cell, let digit = step.digit else { return }
            pushUndo()
            board[target] = digit
            notes[target] = 0
            selectedCell = target
            noteMode = false
            hints += 1
            hintStage = .guide
            hintFocus = nil
            hintText = nil
            let bit = 1 << (digit - 1)
            for peer in selectedSize.peers(of: target) where board[peer] == 0 {
                notes[peer] &= ~bit
            }
            canUndo = true
            saveActiveGame()
            if board == solution { finishGame() }
        }
    }

    private func scheduleHintDismissal() {
        conflictWork?.cancel()
        let work = DispatchWorkItem { [weak self] in
            self?.hintText = nil
            self?.hintFocus = nil
        }
        conflictWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + 4.8, execute: work)
    }

    func restartPuzzle() {
        guard screen == .game, !puzzle.isEmpty else { return }
        stopTimer()
        undoStack.removeAll()
        board = puzzle
        notes = Array(repeating: 0, count: selectedSize.count)
        selectedCell = puzzle.firstIndex(of: 0)
        noteMode = false
        mistakes = 0
        hints = 0
        seconds = 0
        isPaused = false
        isComplete = false
        canUndo = false
        dialog = nil
        clearConflict()
        saveActiveGame()
        startTimer()
    }

    func showLeaderboard(size: BoardSize? = nil, difficulty: Difficulty? = nil) {
        leaderboardSize = size ?? selectedSize
        leaderboardDifficulty = difficulty ?? selectedDifficulty
        dialog = nil
        screen = .leaderboard
    }
    func closeLeaderboard() { screen = .home }
    func selectLeaderboardSize(_ size: BoardSize) { leaderboardSize = size }
    func selectLeaderboardDifficulty(_ difficulty: Difficulty) { leaderboardDifficulty = difficulty }

    func toggleQuietMode() {
        quietMode.toggle()
        store.quietMode = quietMode
        if !quietMode { GameFeedback.tap() }
    }

    func onBackgrounded() {
        guard screen == .game, !puzzle.isEmpty, !isComplete else { return }
        isPaused = true
        dialog = .pause
        saveActiveGame()
        stopTimer()
        clearConflict()
    }
    func resume() {
        isPaused = false
        dialog = nil
        startTimer()
    }
    func goHome() {
        if !isComplete { saveActiveGame() }
        stopTimer()
        dialog = nil
        screen = .home
    }
    func playAgain() { startNewGame() }

    private func pushUndo() {
        if undoStack.count >= 100 { undoStack.removeFirst() }
        undoStack.append(UndoState(board: board, notes: notes, selected: selectedCell))
    }
    private func saveActiveGame() {
        guard !puzzle.isEmpty, !isComplete else { return }
        store.saveGame(SavedGame(size: selectedSize, difficulty: selectedDifficulty, puzzle: puzzle,
                                 solution: solution, board: board, notes: notes,
                                 seconds: seconds, mistakes: mistakes, hints: hints))
    }
    private func startTimer() {
        stopTimer()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor [weak self] in
                guard let self, !self.isPaused, !self.isComplete else { return }
                self.seconds += 1
                if self.seconds % 5 == 0 { self.saveActiveGame() }
            }
        }
    }
    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
    private func showConflict(_ info: ConflictInfo) {
        conflictWork?.cancel()
        conflict = info
        let work = DispatchWorkItem { [weak self] in self?.conflict = nil }
        conflictWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.2, execute: work)
    }
    private func clearConflict() {
        conflictWork?.cancel()
        conflictWork = nil
        conflict = nil
    }
    private func finishGame() {
        guard !isComplete, board == solution,
              let key = gameKey else { return }
        stopTimer()
        isComplete = true
        isPaused = false
        let penalty = mistakes + hints
        let stars = penalty == 0 ? 3 : (penalty <= 3 ? 2 : 1)
        let record = store.recordWin(size: key.size, difficulty: key.difficulty, seconds: seconds,
                                     stars: stars, mistakes: mistakes, hints: hints)
        store.clearGame()
        undoStack.removeAll()
        canUndo = false
        lastResult = GameResult(seconds: seconds, mistakes: mistakes, hints: hints, stars: stars,
                                best: record.best, isNewBest: record.newBest, rank: record.rank)
        dialog = .win
        GameFeedback.win()
    }
}

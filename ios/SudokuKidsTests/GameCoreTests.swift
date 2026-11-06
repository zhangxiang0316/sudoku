import XCTest
@testable import SudokuKids

final class GameCoreTests: XCTestCase {
    func testBoardGeometryAndPeers() {
        XCTAssertEqual(BoardSize.four.peers(of: 0).count, 7)
        XCTAssertEqual(BoardSize.six.box(of: 0), BoardSize.six.box(of: 2))
        XCTAssertNotEqual(BoardSize.six.box(of: 0), BoardSize.six.box(of: 3))
        XCTAssertEqual(BoardSize.nine.peers(of: 0).count, 20)
    }

    func testAllTwelveLevelsGenerateUniqueSolvableBoards() {
        for size in BoardSize.allCases {
            for difficulty in Difficulty.allCases {
                let generated = SudokuEngine.generate(size: size, difficulty: difficulty)
                XCTAssertEqual(generated.puzzle.count, size.count)
                XCTAssertEqual(generated.solution.count, size.count)
                XCTAssertTrue(SudokuEngine.isSolved(generated.solution, size: size), "\(size) \(difficulty)")
                XCTAssertEqual(SudokuEngine.countSolutions(generated.puzzle, size: size, limit: 2), 1,
                               "\(size) \(difficulty) must have one solution")
                XCTAssertEqual(generated.puzzle.filter { $0 != 0 }.count, difficulty.clueTarget(for: size),
                               "\(size) \(difficulty) clue count")
            }
        }
    }

    func testExplainsRowConflict() {
        var board = Array(repeating: 0, count: BoardSize.four.count)
        board[1] = 2
        let result = ConflictExplainer.explain(board: board, size: .four, index: 3, digit: 2)
        XCTAssertEqual(result.reason, .row)
        XCTAssertEqual(result.cells, [1])
        XCTAssertEqual(result.message, "这一行已经有 2 了哦")
    }

    func testExplainsSixBySixBoxConflict() {
        var board = Array(repeating: 0, count: BoardSize.six.count)
        board[BoardSize.six.rawValue] = 5 // row 1, col 0
        let target = 1 // row 0, col 1
        let result = ConflictExplainer.explain(board: board, size: .six, index: target, digit: 5)
        XCTAssertEqual(result.reason, .box)
    }

    @MainActor
    func testStoreKeepsTenScoresPerSizeAndDifficulty() {
        let suite = "sudoku-ios-test-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let store = GameStore(defaults: defaults)
        for seconds in stride(from: 20, through: 0, by: -1) {
            _ = store.recordWin(size: .four, difficulty: .easy, seconds: seconds, stars: 3, mistakes: 0, hints: 0)
        }
        XCTAssertEqual(store.leaderboard(size: .four, difficulty: .easy).count, 10)
        XCTAssertEqual(store.leaderboard(size: .four, difficulty: .easy).first?.seconds, 0)
        XCTAssertTrue(store.leaderboard(size: .six, difficulty: .easy).isEmpty)
    }
}

import Foundation

// MARK: - Game configuration

enum BoardSize: Int, CaseIterable, Codable, Identifiable {
    case four = 4
    case six = 6
    case nine = 9

    var id: Int { rawValue }
    var title: String { "\(rawValue)×\(rawValue)" }
    var subtitle: String {
        switch self {
        case .four: "2×2 宫 · 入门"
        case .six: "2×3 宫 · 进阶"
        case .nine: "3×3 宫 · 标准"
        }
    }
    var emoji: String {
        switch self {
        case .four: "🐣"
        case .six: "🐥"
        case .nine: "🦉"
        }
    }
    var boxRows: Int {
        switch self {
        case .four, .six: 2
        case .nine: 3
        }
    }
    var boxColumns: Int {
        switch self {
        case .four: 2
        case .six: 3
        case .nine: 3
        }
    }
    var count: Int { rawValue * rawValue }
    var hintAllowance: Int { self == .four ? 2 : 3 }
    func box(of index: Int) -> Int {
        let row = index / rawValue
        let column = index % rawValue
        return (row / boxRows) * (rawValue / boxColumns) + column / boxColumns
    }
    func peers(of index: Int) -> [Int] {
        let row = index / rawValue
        let column = index % rawValue
        return (0..<count).filter { other in
            guard other != index else { return false }
            let otherRow = other / rawValue
            let otherColumn = other % rawValue
            return otherRow == row || otherColumn == column || box(of: other) == box(of: index)
        }
    }
}

enum Difficulty: String, CaseIterable, Codable, Identifiable {
    case easy, medium, hard, expert
    var id: String { rawValue }
    var title: String {
        switch self {
        case .easy: "轻松"
        case .medium: "进阶"
        case .hard: "挑战"
        case .expert: "大师"
        }
    }
    var subtitle: String {
        switch self {
        case .easy: "慢慢来，先热身"
        case .medium: "动动小脑筋"
        case .hard: "需要多想一步"
        case .expert: "留给最勇敢的你"
        }
    }
    var emoji: String {
        switch self {
        case .easy: "🌱"
        case .medium: "🌿"
        case .hard: "🔥"
        case .expert: "🏆"
        }
    }
    func clueTarget(for size: BoardSize) -> Int {
        switch (size, self) {
        case (.four, .easy): 9
        case (.four, .medium): 8
        case (.four, .hard): 7
        case (.four, .expert): 6
        case (.six, .easy): 20
        case (.six, .medium): 17
        case (.six, .hard): 14
        case (.six, .expert): 12
        case (.nine, .easy): 46
        case (.nine, .medium): 38
        case (.nine, .hard): 31
        case (.nine, .expert): 26
        }
    }
}

struct GameKey: Hashable, Codable, Identifiable {
    var size: BoardSize
    var difficulty: Difficulty
    var id: String { "\(size.rawValue)_\(difficulty.rawValue)" }
}

struct GeneratedPuzzle {
    var puzzle: [Int]
    var solution: [Int]
    var clueCount: Int { puzzle.filter { $0 != 0 }.count }
}

// MARK: - Solver and generator

enum SudokuEngine {
    static func isSolved(_ board: [Int], size: BoardSize) -> Bool {
        countSolutions(board, size: size, limit: 1) == 1 && !board.contains(0)
    }

    static func countSolutions(_ board: [Int], size: BoardSize, limit: Int = 2) -> Int {
        guard board.count == size.count, limit > 0 else { return 0 }
        var work = board
        var rows = Array(repeating: 0, count: size.rawValue)
        var columns = Array(repeating: 0, count: size.rawValue)
        var boxes = Array(repeating: 0, count: size.rawValue)

        for index in work.indices {
            let value = work[index]
            guard (0...size.rawValue).contains(value) else { return 0 }
            guard value != 0 else { continue }
            let row = index / size.rawValue
            let column = index % size.rawValue
            let box = size.box(of: index)
            let bit = 1 << (value - 1)
            guard rows[row] & bit == 0, columns[column] & bit == 0, boxes[box] & bit == 0 else {
                return 0
            }
            rows[row] |= bit
            columns[column] |= bit
            boxes[box] |= bit
        }
        return search(&work, size: size, rows: &rows, columns: &columns, boxes: &boxes, limit: limit)
    }

    private static func search(
        _ board: inout [Int], size: BoardSize,
        rows: inout [Int], columns: inout [Int], boxes: inout [Int], limit: Int
    ) -> Int {
        var bestIndex = -1
        var bestMask = 0
        var bestCount = size.rawValue + 1
        for index in board.indices where board[index] == 0 {
            let row = index / size.rawValue
            let column = index % size.rawValue
            let mask = ((1 << size.rawValue) - 1) & ~(rows[row] | columns[column] | boxes[size.box(of: index)])
            let count = mask.nonzeroBitCount
            if count == 0 { return 0 }
            if count < bestCount {
                bestIndex = index
                bestMask = mask
                bestCount = count
                if count == 1 { break }
            }
        }
        if bestIndex == -1 { return 1 }

        let row = bestIndex / size.rawValue
        let column = bestIndex % size.rawValue
        let box = size.box(of: bestIndex)
        var found = 0
        var mask = bestMask
        while mask != 0 {
            let bit = mask & -mask
            let value = bit.trailingZeroBitCount + 1
            board[bestIndex] = value
            rows[row] |= bit
            columns[column] |= bit
            boxes[box] |= bit
            found += search(&board, size: size, rows: &rows, columns: &columns, boxes: &boxes, limit: limit - found)
            if found >= limit { return found }
            rows[row] ^= bit
            columns[column] ^= bit
            boxes[box] ^= bit
            board[bestIndex] = 0
            mask ^= bit
        }
        return found
    }

    static func generate(size: BoardSize, difficulty: Difficulty) -> GeneratedPuzzle {
        let target = difficulty.clueTarget(for: size)
        var best: GeneratedPuzzle?
        var bestDistance = Int.max
        for _ in 0..<8 {
            var solution = Array(repeating: 0, count: size.count)
            _ = fill(&solution, size: size)
            var puzzle = solution
            var clueCount = size.count
            let starts = Array(0...(size.count / 2)).shuffled()
            for start in starts {
                let mirror = size.count - 1 - start
                if start == mirror {
                    if clueCount - 1 >= target {
                        clueCount = removeIfUnique(&puzzle, size: size, indices: [start], clues: clueCount)
                    }
                } else if clueCount - 2 >= target {
                    clueCount = removeIfUnique(&puzzle, size: size, indices: [start, mirror], clues: clueCount)
                }
            }
            if clueCount > target {
                for index in puzzle.indices.filter({ puzzle[$0] != 0 }).shuffled() {
                    guard clueCount > target else { break }
                    clueCount = removeIfUnique(&puzzle, size: size, indices: [index], clues: clueCount)
                }
            }
            let result = GeneratedPuzzle(puzzle: puzzle, solution: solution)
            let distance = abs(result.clueCount - target)
            if distance < bestDistance {
                best = result
                bestDistance = distance
            }
            if distance == 0 { return result }
        }
        return best ?? GeneratedPuzzle(puzzle: [], solution: [])
    }

    private static func fill(_ board: inout [Int], size: BoardSize) -> Bool {
        var best = -1
        var candidates: [Int] = []
        for index in board.indices where board[index] == 0 {
            let row = index / size.rawValue
            let column = index % size.rawValue
            let used = rowMask(board, row: row, size: size)
                | columnMask(board, column: column, size: size)
                | boxMask(board, box: size.box(of: index), size: size)
            let choices = (1...size.rawValue).filter { used & (1 << ($0 - 1)) == 0 }
            if choices.isEmpty { return false }
            if best == -1 || choices.count < candidates.count {
                best = index
                candidates = choices
                if choices.count == 1 { break }
            }
        }
        if best == -1 { return true }
        for value in candidates.shuffled() {
            board[best] = value
            if fill(&board, size: size) { return true }
        }
        board[best] = 0
        return false
    }

    private static func rowMask(_ board: [Int], row: Int, size: BoardSize) -> Int {
        (0..<size.rawValue).reduce(0) { mask, column in
            let value = board[row * size.rawValue + column]
            return mask | (value == 0 ? 0 : 1 << (value - 1))
        }
    }
    private static func columnMask(_ board: [Int], column: Int, size: BoardSize) -> Int {
        (0..<size.rawValue).reduce(0) { mask, row in
            let value = board[row * size.rawValue + column]
            return mask | (value == 0 ? 0 : 1 << (value - 1))
        }
    }
    private static func boxMask(_ board: [Int], box: Int, size: BoardSize) -> Int {
        board.indices.reduce(0) { mask, index in
            guard size.box(of: index) == box else { return mask }
            let value = board[index]
            return mask | (value == 0 ? 0 : 1 << (value - 1))
        }
    }
    private static func removeIfUnique(_ puzzle: inout [Int], size: BoardSize, indices: [Int], clues: Int) -> Int {
        let old = indices.map { puzzle[$0] }
        indices.forEach { puzzle[$0] = 0 }
        guard countSolutions(puzzle, size: size, limit: 2) == 1 else {
            for (index, value) in zip(indices, old) { puzzle[index] = value }
            return clues
        }
        return clues - indices.count
    }
}

// MARK: - Explanations and persistence models

enum ConflictReason { case row, column, box, none }
struct ConflictInfo {
    var reason: ConflictReason
    var cells: [Int]
    var message: String
}

enum ConflictExplainer {
    static func explain(board: [Int], size: BoardSize, index: Int, digit: Int) -> ConflictInfo {
        let row = index / size.rawValue
        let column = index % size.rawValue
        let rowCells = (0..<size.rawValue).map { row * size.rawValue + $0 }
            .filter { $0 != index && board[$0] == digit }
        if !rowCells.isEmpty { return ConflictInfo(reason: .row, cells: rowCells, message: "这一行已经有 \(digit) 了哦") }
        let columnCells = (0..<size.rawValue).map { $0 * size.rawValue + column }
            .filter { $0 != index && board[$0] == digit }
        if !columnCells.isEmpty { return ConflictInfo(reason: .column, cells: columnCells, message: "这一列已经有 \(digit) 了哦") }
        let boxCells = size.peers(of: index).filter { size.box(of: $0) == size.box(of: index) && board[$0] == digit }
        if !boxCells.isEmpty { return ConflictInfo(reason: .box, cells: boxCells, message: "这一宫已经有 \(digit) 了哦") }
        return ConflictInfo(reason: .none, cells: [], message: "这一格还不能填 \(digit)，再看看别的线索～")
    }
}

struct ScoreEntry: Codable, Identifiable {
    var seconds: Int
    var stars: Int
    var mistakes: Int
    var hints: Int
    var playedAt: Date
    var id: UUID = UUID()
}

struct SavedGame: Codable {
    var size: BoardSize
    var difficulty: Difficulty
    var puzzle: [Int]
    var solution: [Int]
    var board: [Int]
    var notes: [Int]
    var seconds: Int
    var mistakes: Int
    var hints: Int
}

struct UndoState {
    var board: [Int]
    var notes: [Int]
    var selected: Int?
}

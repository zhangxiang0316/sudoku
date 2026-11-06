import Foundation

@MainActor
final class GameStore {
    private let defaults: UserDefaults
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    private func key(_ prefix: String, size: BoardSize, difficulty: Difficulty) -> String {
        "\(prefix)_\(size.rawValue)_\(difficulty.rawValue)"
    }

    var lastSize: BoardSize? {
        get {
            guard let raw = defaults.object(forKey: "last_size") as? Int else { return nil }
            return BoardSize(rawValue: raw)
        }
        set { defaults.set(newValue?.rawValue, forKey: "last_size") }
    }
    var lastDifficulty: Difficulty {
        get { Difficulty(rawValue: defaults.string(forKey: "last_difficulty") ?? "easy") ?? .easy }
        set { defaults.set(newValue.rawValue, forKey: "last_difficulty") }
    }
    var quietMode: Bool {
        get { defaults.bool(forKey: "quiet_mode") }
        set { defaults.set(newValue, forKey: "quiet_mode") }
    }
    var totalWins: Int { defaults.integer(forKey: "total_wins") }
    var totalStars: Int { defaults.integer(forKey: "total_stars") }

    func bestTime(size: BoardSize, difficulty: Difficulty) -> Int? {
        let key = key("best", size: size, difficulty: difficulty)
        return defaults.object(forKey: key) == nil ? nil : defaults.integer(forKey: key)
    }

    func leaderboard(size: BoardSize, difficulty: Difficulty) -> [ScoreEntry] {
        guard let data = defaults.data(forKey: key("leaderboard", size: size, difficulty: difficulty)),
              let entries = try? decoder.decode([ScoreEntry].self, from: data) else { return [] }
        return entries.sorted { lhs, rhs in
            lhs.seconds == rhs.seconds ? lhs.playedAt < rhs.playedAt : lhs.seconds < rhs.seconds
        }.prefix(10).map { $0 }
    }

    func recordWin(size: BoardSize, difficulty: Difficulty, seconds: Int, stars: Int, mistakes: Int, hints: Int) -> (best: Int, newBest: Bool, rank: Int?) {
        let previous = bestTime(size: size, difficulty: difficulty)
        let isNewBest = previous == nil || seconds < previous!
        let best = isNewBest ? seconds : previous!
        defaults.set(best, forKey: key("best", size: size, difficulty: difficulty))
        defaults.set(totalWins + 1, forKey: "total_wins")
        defaults.set(totalStars + stars, forKey: "total_stars")

        let entry = ScoreEntry(seconds: seconds, stars: stars, mistakes: mistakes, hints: hints, playedAt: Date())
        let ranked = (leaderboard(size: size, difficulty: difficulty) + [entry]).sorted { lhs, rhs in
            lhs.seconds == rhs.seconds ? lhs.playedAt < rhs.playedAt : lhs.seconds < rhs.seconds
        }.prefix(10).map { $0 }
        if let data = try? encoder.encode(ranked) {
            defaults.set(data, forKey: key("leaderboard", size: size, difficulty: difficulty))
        }
        let rank = ranked.firstIndex(where: { $0.id == entry.id }).map { $0 + 1 }
        return (best, isNewBest, rank)
    }

    func loadGame() -> SavedGame? {
        guard let data = defaults.data(forKey: "active_game"),
              let game = try? decoder.decode(SavedGame.self, from: data),
              game.puzzle.count == game.size.count,
              game.solution.count == game.size.count,
              game.board.count == game.size.count,
              game.notes.count == game.size.count,
              game.solution.allSatisfy({ (1...game.size.rawValue).contains($0) }),
              SudokuEngine.isSolved(game.solution, size: game.size),
              !game.board.elementsEqual(game.solution) else { return nil }
        return game
    }

    func saveGame(_ game: SavedGame) {
        guard let data = try? encoder.encode(game) else { return }
        defaults.set(data, forKey: "active_game")
        lastSize = game.size
        lastDifficulty = game.difficulty
    }

    func clearGame() { defaults.removeObject(forKey: "active_game") }
}

import AudioToolbox
import UIKit

@MainActor
enum GameFeedback {
    static func correct() {
        guard !GameServices.shared.quietMode else { return }
        AudioServicesPlaySystemSound(1104)
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }
    static func wrong() {
        guard !GameServices.shared.quietMode else { return }
        AudioServicesPlaySystemSound(1053)
        UINotificationFeedbackGenerator().notificationOccurred(.error)
    }
    static func win() {
        guard !GameServices.shared.quietMode else { return }
        AudioServicesPlaySystemSound(1025)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
    }
    static func tap() {
        guard !GameServices.shared.quietMode else { return }
        AudioServicesPlaySystemSound(1104)
        UIImpactFeedbackGenerator(style: .soft).impactOccurred()
    }
}

@MainActor
final class GameServices: ObservableObject {
    static let shared = GameServices()
    let store = GameStore()
    var quietMode: Bool {
        get { store.quietMode }
        set { store.quietMode = newValue }
    }
    private init() {}
}

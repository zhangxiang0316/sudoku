import SwiftUI

struct LeaderboardView: View {
    @ObservedObject var model: GameViewModel
    @Environment(\.theme) private var theme

    var body: some View {
        VStack(spacing: 0) {
            header
            tabs
            Spacer().frame(height: 12)
            if model.currentLeaderboard.isEmpty {
                emptyState
            } else {
                scoreList
            }
        }
        .background(theme.background.ignoresSafeArea())
        .accessibilityIdentifier("leaderboard_screen")
    }

    private var header: some View {
        HStack(spacing: 0) {
            Button { model.closeLeaderboard() } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(theme.onBackground)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("返回")

            VStack(alignment: .leading, spacing: 1) {
                Text("🏆 排行榜")
                    .font(.system(size: 19, weight: .bold, design: .rounded))
                    .foregroundStyle(theme.onBackground)
                Text("各难度前十名，用时越短越靠前")
                    .font(.system(size: 11))
                    .foregroundStyle(theme.onSurfaceVariant)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 8)
        .padding(.top, 4)
    }

    private var tabs: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                ForEach(BoardSize.allCases) { size in
                    tab(size.title, selected: model.leaderboardSize == size) {
                        model.selectLeaderboardSize(size)
                    }
                    .accessibilityIdentifier("leaderboard_size_\(size.rawValue)")
                }
            }
            HStack(spacing: 8) {
                ForEach(Difficulty.allCases) { difficulty in
                    tab("\(difficulty.emoji) \(difficulty.title)",
                        selected: model.leaderboardDifficulty == difficulty) {
                        model.selectLeaderboardDifficulty(difficulty)
                    }
                    .accessibilityIdentifier("leaderboard_difficulty_\(difficulty.rawValue)")
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 6)
    }

    private func tab(_ label: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 13, weight: selected ? .bold : .regular, design: .rounded))
                .foregroundStyle(selected ? theme.surface : theme.onSurfaceVariant)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 9)
                .background(selected ? theme.primary : theme.surface,
                            in: RoundedRectangle(cornerRadius: 12))
                .overlay {
                    if !selected {
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(theme.outlineVariant.opacity(0.8), lineWidth: 1)
                    }
                }
        }
        .buttonStyle(.plain)
    }

    private var scoreList: some View {
        ScrollView {
            LazyVStack(spacing: 8) {
                ForEach(Array(model.currentLeaderboard.enumerated()), id: \.element.id) { index, entry in
                    scoreRow(rank: index + 1, entry: entry)
                }
                Spacer().frame(height: 16)
            }
            .padding(.horizontal, 16)
        }
    }

    private func scoreRow(rank: Int, entry: ScoreEntry) -> some View {
        let medal: String? = switch rank {
        case 1: "🥇"
        case 2: "🥈"
        case 3: "🥉"
        default: nil
        }
        let topThree = medal != nil
        return HStack(spacing: 6) {
            ZStack {
                if let medal {
                    Text(medal).font(.system(size: 22))
                } else {
                    Text("\(rank)")
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                        .foregroundStyle(theme.onSurfaceVariant)
                }
            }
            .frame(width: 38)

            VStack(alignment: .leading, spacing: 3) {
                Text(formatTime(entry.seconds))
                    .font(.system(size: 19, weight: .bold, design: .rounded).monospacedDigit())
                    .foregroundStyle(theme.onBackground)
                Text("失误 \(entry.mistakes) · 提示 \(entry.hints) · \(dateText(entry.playedAt))")
                    .font(.system(size: 11))
                    .foregroundStyle(theme.onSurfaceVariant)
                    .lineLimit(1)
                    .minimumScaleFactor(0.85)
            }

            Spacer(minLength: 4)

            HStack(spacing: 3) {
                ForEach(0..<3, id: \.self) { index in
                    Text("★")
                        .font(.system(size: 13))
                        .foregroundStyle(index < entry.stars ? theme.tertiary : theme.outlineVariant)
                }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
        .background(topThree ? theme.secondaryContainer.opacity(0.45) : theme.surface,
                    in: RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16)
            .stroke(topThree ? theme.secondary.opacity(0.45) : theme.outlineVariant.opacity(0.7),
                    lineWidth: 1))
        .accessibilityIdentifier("score_row_\(rank)")
    }

    private var emptyState: some View {
        VStack(spacing: 0) {
            Spacer()
            Text("🌱").font(.system(size: 44))
            Text("\(model.leaderboardSize.title) \(model.leaderboardDifficulty.title) 还没有记录")
                .font(.system(size: 14, weight: .bold, design: .rounded))
                .foregroundStyle(theme.onBackground)
                .multilineTextAlignment(.center)
                .padding(.top, 12)
            Text("通关一局就会出现在这里")
                .font(.system(size: 12))
                .foregroundStyle(theme.onSurfaceVariant)
                .padding(.top, 6)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityIdentifier("leaderboard_empty")
    }

    private func dateText(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = Calendar.current.isDate(date, equalTo: Date(), toGranularity: .year)
            ? "M月d日"
            : "yyyy年M月d日"
        return formatter.string(from: date)
    }
}

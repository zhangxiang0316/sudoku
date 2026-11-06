import SwiftUI

struct LeaderboardView: View {
    @ObservedObject var model: GameViewModel

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                Button { model.closeLeaderboard() } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Palette.ink)
                        .frame(width: 40, height: 40)
                        .background(Palette.white, in: Circle())
                }
                VStack(alignment: .leading, spacing: 1) {
                    Text("排行榜").font(.system(size: 23, weight: .heavy, design: .rounded)).foregroundStyle(Palette.ink)
                    Text("每种盘面和难度分别记录前十名")
                        .font(.system(size: 11, weight: .medium)).foregroundStyle(Palette.muted)
                }
                Spacer()
            }
            .padding(.horizontal, 18)
            .padding(.top, 8)
            .padding(.bottom, 14)

            sizePicker
            difficultyPicker
                .padding(.top, 8)

            if model.currentLeaderboard.isEmpty {
                emptyState
            } else {
                scoreList
            }
        }
        .background(Palette.paper)
        .accessibilityIdentifier("leaderboard_screen")
    }

    private var sizePicker: some View {
        HStack(spacing: 6) {
            ForEach(BoardSize.allCases) { size in
                let selected = model.leaderboardSize == size
                Button { model.selectLeaderboardSize(size) } label: {
                    Text("\(size.emoji) \(size.title)")
                        .font(.system(size: 13, weight: .bold, design: .rounded))
                        .foregroundStyle(selected ? .white : Palette.ink)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 9)
                        .background(selected ? Palette.teal : Palette.white, in: RoundedRectangle(cornerRadius: 9))
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("leaderboard_size_\(size.rawValue)")
            }
        }
        .padding(.horizontal, 18)
    }

    private var difficultyPicker: some View {
        HStack(spacing: 6) {
            ForEach(Difficulty.allCases) { difficulty in
                let selected = model.leaderboardDifficulty == difficulty
                Button { model.selectLeaderboardDifficulty(difficulty) } label: {
                    Text("\(difficulty.emoji) \(difficulty.title)")
                        .font(.system(size: 12, weight: .bold, design: .rounded))
                        .foregroundStyle(selected ? Palette.ink : Palette.muted)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(selected ? Palette.hint : Palette.white, in: RoundedRectangle(cornerRadius: 9))
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("leaderboard_difficulty_\(difficulty.rawValue)")
            }
        }
        .padding(.horizontal, 18)
    }

    private var scoreList: some View {
        ScrollView {
            LazyVStack(spacing: 8) {
                ForEach(Array(model.currentLeaderboard.enumerated()), id: \.element.id) { index, score in
                    HStack(spacing: 10) {
                        Text(medal(index))
                            .font(.system(size: index < 3 ? 22 : 15, weight: .heavy, design: .rounded))
                            .frame(width: 34)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(formatTime(score.seconds))
                                .font(.system(size: 18, weight: .heavy, design: .rounded).monospacedDigit())
                                .foregroundStyle(Palette.ink)
                            Text("\(score.mistakes) 次失误 · \(score.hints) 次提示 · \(dateText(score.playedAt))")
                                .font(.system(size: 10, weight: .medium)).foregroundStyle(Palette.muted)
                        }
                        Spacer(minLength: 4)
                        HStack(spacing: 1) {
                            ForEach(0..<score.stars, id: \.self) { _ in
                                Image(systemName: "star.fill").foregroundStyle(Palette.coral)
                            }
                        }
                        .font(.system(size: 12))
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(Palette.white, in: RoundedRectangle(cornerRadius: 12))
                    .accessibilityIdentifier("score_row_\(index + 1)")
                }
            }
            .padding(.horizontal, 18)
            .padding(.top, 14)
            .padding(.bottom, 20)
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Spacer()
            Text("🏆").font(.system(size: 44))
            Text("还没有记录").font(.system(size: 19, weight: .heavy, design: .rounded)).foregroundStyle(Palette.ink)
            Text("通关一局，就会出现在这里")
                .font(.system(size: 14, weight: .medium)).foregroundStyle(Palette.muted)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityIdentifier("leaderboard_empty")
    }

    private func medal(_ index: Int) -> String {
        switch index {
        case 0: "🥇"
        case 1: "🥈"
        case 2: "🥉"
        default: "#\(index + 1)"
        }
    }

    private func dateText(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = Calendar.current.isDate(date, equalTo: Date(), toGranularity: .year) ? "M月d日" : "yyyy年M月d日"
        return formatter.string(from: date)
    }
}

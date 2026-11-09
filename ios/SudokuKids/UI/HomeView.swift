import SwiftUI

struct HomeView: View {
    @ObservedObject var model: GameViewModel
    @Environment(\.theme) private var theme

    var body: some View {
        VStack(spacing: 0) {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    header
                        .padding(.top, 12)
                    statsCard
                        .padding(.top, 16)
                    sectionHeading("① 选择盘面", note: "第一次玩建议从 4×4 开始，熟练了再换大的")
                        .padding(.top, 20)
                    sizeRow
                        .padding(.top, 12)
                    sectionHeading("② 选择难度", note: "同一盘面下，留的提示越少越难")
                        .padding(.top, 20)
                    difficultyGrid
                        .padding(.top, 12)
                    if model.savedGame != nil {
                        continueCard
                            .padding(.top, 18)
                    }
                    Spacer(minLength: 16)
                }
                .frame(maxWidth: 660)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 20)
            }
            bottomBar
                .frame(maxWidth: 660)
                .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(theme.background.ignoresSafeArea())
    }

    // MARK: Header

    private var header: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text("数独乐园")
                    .font(.system(size: 28, weight: .bold, design: .rounded))
                    .foregroundStyle(theme.onBackground)
                Text("每天动动脑，越玩越聪明")
                    .font(.system(size: 14))
                    .foregroundStyle(theme.onSurfaceVariant)
            }
            Spacer(minLength: 4)
            Button { model.showLeaderboard() } label: {
                HStack(spacing: 5) {
                    Text("🏆").font(.system(size: 16))
                    Text("排行榜")
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundStyle(theme.onSecondaryContainer)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 9)
                .background(theme.secondaryContainer.opacity(0.6), in: RoundedRectangle(cornerRadius: 18))
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("leaderboard_entry")
        }
    }

    // MARK: Progress

    private var statsCard: some View {
        HStack(alignment: .center, spacing: 0) {
            VStack(alignment: .leading, spacing: 6) {
                Text("通关 \(model.totalWins) 局")
                    .font(.system(size: 16, weight: .bold, design: .rounded))
                    .foregroundStyle(theme.onSecondaryContainer)
                HStack(spacing: 6) {
                    stars(filled: min(model.totalStars % 4, 3), size: 15)
                    Text("共收集 \(model.totalStars) 颗星")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(theme.onSecondaryContainer.opacity(0.75))
                }
            }
            Spacer(minLength: 0)
            Text(progressEmoji)
                .font(.system(size: 34))
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity)
        .background(theme.secondaryContainer.opacity(0.5), in: RoundedRectangle(cornerRadius: 22))
    }

    /// Mirrors the Android ladder, which is driven by completed games rather than stars.
    private var progressEmoji: String {
        switch model.totalWins {
        case 0: "🌱"
        case 1..<5: "🌿"
        case 5..<15: "🌳"
        default: "🏆"
        }
    }

    private func stars(filled: Int, size: CGFloat) -> some View {
        HStack(spacing: 3) {
            ForEach(0..<3, id: \.self) { index in
                Text("★")
                    .font(.system(size: size))
                    .foregroundStyle(index < filled ? theme.tertiary : theme.outlineVariant)
            }
        }
    }

    // MARK: Sections

    private func sectionHeading(_ title: String, note: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.system(size: 19, weight: .bold, design: .rounded))
                .foregroundStyle(theme.onBackground)
            Text(note)
                .font(.system(size: 12))
                .foregroundStyle(theme.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var sizeRow: some View {
        HStack(spacing: 10) {
            ForEach(BoardSize.allCases) { size in
                sizeCard(size)
            }
        }
    }

    private func sizeCard(_ size: BoardSize) -> some View {
        let selected = model.selectedSize == size
        let accent = accentFor(size)
        let best = model.bestTime(size: size, difficulty: model.selectedDifficulty)
        return Button { model.selectSize(size) } label: {
            VStack(spacing: 0) {
                Text(size.emoji).font(.system(size: 24))
                Text(size.title)
                    .font(.system(size: 19, weight: .bold, design: .rounded))
                    .foregroundStyle(selected ? accent : theme.onBackground)
                    .padding(.top, 6)
                Text(size.subtitle)
                    .font(.system(size: 11))
                    .foregroundStyle(theme.onSurfaceVariant)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .padding(.top, 3)
                Text(best.map { "最佳 \(formatTime($0))" } ?? "未通关")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(best != nil ? accent.opacity(0.9) : theme.onSurfaceVariant.opacity(0.55))
                    .lineLimit(1)
                    .padding(.top, 6)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .padding(.horizontal, 8)
            .background(selected ? accent.opacity(0.14) : theme.surface,
                        in: RoundedRectangle(cornerRadius: 18))
            .overlay(RoundedRectangle(cornerRadius: 18)
                .stroke(selected ? accent : theme.outlineVariant.opacity(0.8),
                        lineWidth: selected ? 2 : 1))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("size_\(size.rawValue)")
    }

    private var difficultyGrid: some View {
        VStack(spacing: 10) {
            let pairs = stride(from: 0, to: Difficulty.allCases.count, by: 2).map {
                Array(Difficulty.allCases[$0..<min($0 + 2, Difficulty.allCases.count)])
            }
            ForEach(Array(pairs.enumerated()), id: \.offset) { _, pair in
                HStack(spacing: 10) {
                    ForEach(pair) { difficulty in
                        difficultyCard(difficulty)
                    }
                    if pair.count == 1 { Spacer() }
                }
            }
        }
    }

    private func difficultyCard(_ difficulty: Difficulty) -> some View {
        let selected = model.selectedDifficulty == difficulty
        let accent = accentFor(difficulty)
        let best = model.bestTime(size: model.selectedSize, difficulty: difficulty)
        let clues = difficulty.clueTarget(for: model.selectedSize)
        return Button { model.selectDifficulty(difficulty) } label: {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 8) {
                    Text(difficulty.emoji).font(.system(size: 20))
                    Text(difficulty.title)
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                        .foregroundStyle(selected ? accent : theme.onBackground)
                    Spacer(minLength: 4)
                    if selected {
                        Text("已选")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(accent, in: Capsule())
                    }
                }
                Text("留存 \(clues) 个数字")
                    .font(.system(size: 12))
                    .foregroundStyle(theme.onSurfaceVariant)
                    .lineLimit(1)
                    .padding(.top, 6)
                Text(best.map { "最佳 \(formatTime($0))" } ?? "还没通关过")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(best != nil ? accent.opacity(0.9) : theme.onSurfaceVariant.opacity(0.6))
                    .padding(.top, 8)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 14)
            .padding(.vertical, 13)
            .background(selected ? accent.opacity(0.14) : theme.surface,
                        in: RoundedRectangle(cornerRadius: 18))
            .overlay(RoundedRectangle(cornerRadius: 18)
                .stroke(selected ? accent : theme.outlineVariant.opacity(0.8),
                        lineWidth: selected ? 2 : 1))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("difficulty_\(difficulty.rawValue)")
    }

    private func accentFor(_ size: BoardSize) -> Color {
        switch size {
        case .four: theme.secondary
        case .six: theme.sizeAccentSix
        case .nine: theme.sizeAccentNine
        }
    }

    private func accentFor(_ difficulty: Difficulty) -> Color {
        switch difficulty {
        case .easy: theme.secondary
        case .medium: theme.sizeAccentSix
        case .hard: theme.primary
        case .expert: theme.sizeAccentNine
        }
    }

    // MARK: Continue

    private var continueCard: some View {
        Button { model.continueGame() } label: {
            let saved = model.savedGame!
            HStack(spacing: 12) {
                Text("⏳").font(.system(size: 24))
                VStack(alignment: .leading, spacing: 2) {
                    Text("继续上一局 · \(saved.size.title) \(saved.difficulty.title)")
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundStyle(theme.onTertiaryContainer)
                    Text("已完成 \(saved.board.filter { $0 != 0 }.count)/\(saved.size.count) · 用时 \(formatTime(saved.seconds))")
                        .font(.system(size: 12))
                        .foregroundStyle(theme.onTertiaryContainer.opacity(0.8))
                }
                Spacer(minLength: 4)
                Text("继续")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundStyle(theme.onTertiaryContainer)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity)
            .background(theme.tertiaryContainer.opacity(0.7), in: RoundedRectangle(cornerRadius: 18))
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(theme.tertiary.opacity(0.5), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("continue_button")
    }

    // MARK: Bottom action

    private var bottomBar: some View {
        VStack(spacing: 0) {
            Button { model.startNewGame() } label: {
                HStack(spacing: 8) {
                    Image(systemName: "play.fill").font(.system(size: 17, weight: .bold))
                    Text("开始 · \(model.selectedSize.title) \(model.selectedDifficulty.title)")
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                }
                .foregroundStyle(theme.surface)
                .frame(maxWidth: .infinity)
                .frame(height: 58)
                .background(theme.primary, in: RoundedRectangle(cornerRadius: 18))
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("start_button")

            Text("小提示：点格子，再点数字就能填啦")
                .font(.system(size: 12))
                .foregroundStyle(theme.onSurfaceVariant.opacity(0.8))
                .padding(.top, 8)

            Spacer().frame(height: 12)
        }
        .padding(.horizontal, 20)
    }
}

func formatTime(_ seconds: Int) -> String {
    let safe = max(0, seconds)
    let hours = safe / 3600
    let minutes = (safe % 3600) / 60
    let secs = safe % 60
    return hours > 0
        ? String(format: "%d:%02d:%02d", hours, minutes, secs)
        : String(format: "%02d:%02d", minutes, secs)
}

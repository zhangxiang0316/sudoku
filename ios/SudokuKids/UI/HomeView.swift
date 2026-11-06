import SwiftUI

struct HomeView: View {
    @ObservedObject var model: GameViewModel

    var body: some View {
        VStack(spacing: 0) {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 22) {
                    header
                    progressStrip
                    sizeSection
                    difficultySection
                    if model.savedGame != nil { continueButton }
                }
                .frame(maxWidth: 660)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .padding(.bottom, 18)
            }
            startButton
                .frame(maxWidth: 660)
                .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.paper.ignoresSafeArea())
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text("数独乐园")
                    .font(.system(size: 28, weight: .heavy, design: .rounded))
                    .foregroundStyle(Palette.ink)
                Text("今天也来动动小脑筋")
                    .font(.subheadline)
                    .foregroundStyle(Palette.muted)
            }
            Spacer(minLength: 4)
            Button { model.showLeaderboard() } label: {
                Label("排行榜", systemImage: "trophy.fill")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundStyle(Palette.ink)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(Palette.hint, in: Capsule())
            }
            .accessibilityIdentifier("leaderboard_entry")
        }
    }

    private var progressStrip: some View {
        HStack(spacing: 0) {
            stat(value: "\(model.totalWins)", label: "通关")
            Rectangle().fill(Palette.line.opacity(0.7)).frame(width: 1, height: 34)
            stat(value: "\(model.totalStars)", label: "星星", symbol: "star.fill")
            Spacer(minLength: 0)
            Text(model.totalWins == 0 ? "🌱 从第一颗星开始" : progressMessage)
                .font(.system(size: 13, weight: .semibold, design: .rounded))
                .foregroundStyle(Palette.muted)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .padding(.horizontal, 15)
        .padding(.vertical, 12)
        .background(Palette.white, in: RoundedRectangle(cornerRadius: 14))
    }

    private var progressMessage: String {
        switch model.totalStars {
        case 0..<5: "🌱 正在发芽"
        case 5..<15: "🌿 慢慢长大"
        case 15..<30: "🌳 越来越棒"
        default: "🏆 数独小高手"
        }
    }

    private func stat(value: String, label: String, symbol: String? = nil) -> some View {
        HStack(spacing: 5) {
            if let symbol { Image(systemName: symbol).foregroundStyle(Palette.coral) }
            VStack(alignment: .leading, spacing: 0) {
                Text(value).font(.system(size: 16, weight: .heavy, design: .rounded)).foregroundStyle(Palette.ink)
                Text(label).font(.system(size: 10, weight: .medium)).foregroundStyle(Palette.muted)
            }
        }
        .frame(minWidth: 56, alignment: .leading)
    }

    private var sizeSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("① 选盘面", trailing: model.selectedSize.title)
            HStack(spacing: 9) {
                ForEach(BoardSize.allCases) { size in
                    let selected = model.selectedSize == size
                    Button { model.selectSize(size) } label: {
                        VStack(spacing: 5) {
                            Text(size.emoji).font(.system(size: 27))
                            Text(size.title).font(.system(size: 16, weight: .heavy, design: .rounded))
                                .foregroundStyle(Palette.ink)
                            Text(size.subtitle).font(.system(size: 10, weight: .medium))
                                .foregroundStyle(Palette.muted)
                                .lineLimit(1)
                                .minimumScaleFactor(0.75)
                            if let best = model.bestTime(size: size, difficulty: model.selectedDifficulty) {
                                Text("最佳 \(formatTime(best))")
                                    .font(.system(size: 10, weight: .bold, design: .rounded))
                                    .foregroundStyle(Palette.teal)
                            } else {
                                Text("从这里开始").font(.system(size: 10, weight: .medium)).foregroundStyle(Palette.muted.opacity(0.8))
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                        .background(selected ? Palette.selected : Palette.white,
                                    in: RoundedRectangle(cornerRadius: 13))
                        .overlay(RoundedRectangle(cornerRadius: 13)
                            .stroke(selected ? Palette.coral : Palette.line.opacity(0.7), lineWidth: selected ? 2 : 1))
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("size_\(size.rawValue)")
                }
            }
        }
    }

    private var difficultySection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionTitle("② 选难度", trailing: "留存数字越少越难")
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 9) {
                ForEach(Difficulty.allCases) { difficulty in
                    let selected = model.selectedDifficulty == difficulty
                    Button { model.selectDifficulty(difficulty) } label: {
                        HStack(spacing: 9) {
                            Text(difficulty.emoji).font(.system(size: 23))
                            VStack(alignment: .leading, spacing: 2) {
                                Text(difficulty.title).font(.system(size: 15, weight: .bold, design: .rounded)).foregroundStyle(Palette.ink)
                                Text("留 \(difficulty.clueTarget(for: model.selectedSize)) 个数字")
                                    .font(.system(size: 11, weight: .medium)).foregroundStyle(Palette.muted)
                            }
                            Spacer(minLength: 0)
                            if selected { Image(systemName: "checkmark.circle.fill").foregroundStyle(Palette.teal) }
                        }
                        .padding(.horizontal, 11)
                        .padding(.vertical, 11)
                        .background(selected ? Palette.hint.opacity(0.8) : Palette.white,
                                    in: RoundedRectangle(cornerRadius: 12))
                        .overlay(RoundedRectangle(cornerRadius: 12)
                            .stroke(selected ? Palette.coral.opacity(0.75) : Palette.line.opacity(0.6), lineWidth: selected ? 1.5 : 1))
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("difficulty_\(difficulty.rawValue)")
                }
            }
        }
    }

    private func sectionTitle(_ title: String, trailing: String) -> some View {
        HStack(alignment: .firstTextBaseline) {
            Text(title).font(.system(size: 18, weight: .heavy, design: .rounded)).foregroundStyle(Palette.ink)
            Spacer()
            Text(trailing).font(.system(size: 11, weight: .medium)).foregroundStyle(Palette.muted)
        }
    }

    private var continueButton: some View {
        Button { model.continueGame() } label: {
            let saved = model.savedGame!
            HStack(spacing: 12) {
                Image(systemName: "play.circle.fill").font(.system(size: 28)).foregroundStyle(Palette.teal)
                VStack(alignment: .leading, spacing: 2) {
                    Text("继续上一局").font(.system(size: 15, weight: .bold, design: .rounded)).foregroundStyle(Palette.ink)
                    Text("\(saved.size.title) · \(saved.difficulty.title) · 已完成 \(saved.board.filter { $0 != 0 }.count)/\(saved.size.count)")
                        .font(.system(size: 12)).foregroundStyle(Palette.muted)
                }
                Spacer()
                Image(systemName: "chevron.right").font(.system(size: 13, weight: .bold)).foregroundStyle(Palette.muted)
            }
            .padding(13)
            .background(Palette.white, in: RoundedRectangle(cornerRadius: 13))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("continue_button")
    }

    private var startButton: some View {
        Button { model.startNewGame() } label: {
            HStack(spacing: 9) {
                Image(systemName: "play.fill")
                Text("开始 · \(model.selectedSize.title) \(model.selectedDifficulty.title)")
                    .font(.system(size: 17, weight: .heavy, design: .rounded))
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .background(Palette.coral, in: RoundedRectangle(cornerRadius: 15))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("start_button")
        .padding(.horizontal, 20)
        .padding(.top, 10)
        .padding(.bottom, 8)
        .background(Palette.paper)
    }
}

func formatTime(_ seconds: Int) -> String {
    String(format: "%d:%02d", seconds / 60, seconds % 60)
}

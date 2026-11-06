import SwiftUI

struct GameDialogView: View {
    @ObservedObject var model: GameViewModel
    let dialog: GameViewModel.Dialog

    var body: some View {
        VStack(spacing: 16) {
            switch dialog {
            case .win:
                winContent
            case .pause:
                pauseContent
            case .exit:
                exitContent
            case .restart:
                restartContent
            }
        }
        .padding(.horizontal, 22)
        .padding(.top, 24)
        .padding(.bottom, 18)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Palette.paper.ignoresSafeArea())
    }

    private var winContent: some View {
        Group {
            let result = model.lastResult
            Text(result?.isNewBest == true ? "🏆" : "🎉").font(.system(size: 45))
            Text("太棒了，完成啦！")
                .font(.system(size: 23, weight: .heavy, design: .rounded)).foregroundStyle(Palette.ink)
            Text("\(model.selectedSize.title) · \(model.selectedDifficulty.title) · \(formatTime(result?.seconds ?? 0))")
                .font(.system(size: 14, weight: .semibold, design: .rounded)).foregroundStyle(Palette.muted)
            HStack(spacing: 5) {
                ForEach(0..<(result?.stars ?? 1), id: \.self) { _ in
                    Image(systemName: "star.fill").foregroundStyle(Palette.coral)
                }
            }
            .font(.system(size: 23))
            if let result {
                if result.isNewBest {
                    Text("🏅 新纪录！登上了第 \(result.rank ?? 1) 名")
                        .font(.system(size: 14, weight: .bold, design: .rounded)).foregroundStyle(Palette.teal)
                } else if let rank = result.rank {
                    Text("🎖️ 进入排行榜第 \(rank) 名")
                        .font(.system(size: 14, weight: .bold, design: .rounded)).foregroundStyle(Palette.teal)
                } else {
                    Text("失误 \(result.mistakes) 次 · 提示 \(result.hints) 次")
                        .font(.system(size: 13, weight: .medium)).foregroundStyle(Palette.muted)
                }
            }
            Spacer(minLength: 0)
            primaryButton("再来一局", symbol: "arrow.clockwise") { model.playAgain() }
            secondaryButton("看看排行榜", symbol: "trophy.fill") {
                model.showLeaderboard(size: model.selectedSize, difficulty: model.selectedDifficulty)
            }
            Button("回到首页") { model.goHome() }
                .font(.system(size: 14, weight: .semibold, design: .rounded))
                .foregroundStyle(Palette.muted)
        }
    }

    private var pauseContent: some View {
        Group {
            Text("⏸️").font(.system(size: 42))
            Text("休息一下")
                .font(.system(size: 23, weight: .heavy, design: .rounded)).foregroundStyle(Palette.ink)
            Text("时间已经暂停，进度会自动保存")
                .font(.system(size: 13, weight: .medium)).foregroundStyle(Palette.muted)
            Spacer(minLength: 0)
            primaryButton("继续游戏", symbol: "play.fill") { model.resume() }
            secondaryButton("重新开始本局", symbol: "arrow.counterclockwise") { model.dialog = .restart }
            Button("保存并回到首页") { model.goHome() }
                .font(.system(size: 14, weight: .semibold, design: .rounded))
                .foregroundStyle(Palette.muted)
        }
    }

    private var exitContent: some View {
        Group {
            Text("💾").font(.system(size: 42))
            Text("先回首页吗？")
                .font(.system(size: 23, weight: .heavy, design: .rounded)).foregroundStyle(Palette.ink)
            Text("这一局会自动保存，下次可以接着玩")
                .font(.system(size: 13, weight: .medium)).foregroundStyle(Palette.muted)
            Spacer(minLength: 0)
            primaryButton("保存并回到首页", symbol: "house.fill") { model.goHome() }
            Button("继续游戏") { model.dialog = nil }
                .font(.system(size: 14, weight: .semibold, design: .rounded))
                .foregroundStyle(Palette.teal)
        }
    }

    private var restartContent: some View {
        Group {
            Text("🔄").font(.system(size: 42))
            Text("重新开始这一局？")
                .font(.system(size: 22, weight: .heavy, design: .rounded)).foregroundStyle(Palette.ink)
            Text("题目不变，用时、失误和提示会重新计算")
                .font(.system(size: 13, weight: .medium)).foregroundStyle(Palette.muted)
            Spacer(minLength: 0)
            primaryButton("重新开始", symbol: "arrow.counterclockwise") { model.restartPuzzle() }
            Button("再想一想") { model.dialog = .pause }
                .font(.system(size: 14, weight: .semibold, design: .rounded))
                .foregroundStyle(Palette.teal)
        }
    }

    private func primaryButton(_ title: String, symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Label(title, systemImage: symbol)
                .font(.system(size: 16, weight: .heavy, design: .rounded))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 51)
                .background(Palette.coral, in: RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }

    private func secondaryButton(_ title: String, symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Label(title, systemImage: symbol)
                .font(.system(size: 15, weight: .bold, design: .rounded))
                .foregroundStyle(Palette.teal)
                .frame(maxWidth: .infinity)
                .frame(height: 47)
                .background(Palette.white, in: RoundedRectangle(cornerRadius: 13))
        }
        .buttonStyle(.plain)
    }
}

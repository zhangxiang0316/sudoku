import SwiftUI

/// Centred card, mirroring the Android dialogs rather than using a bottom sheet.
struct GameDialogView: View {
    @ObservedObject var model: GameViewModel
    let dialog: GameViewModel.Dialog
    @Environment(\.theme) private var theme

    var body: some View {
        VStack(spacing: 0) {
            switch dialog {
            case .win:
                winContent
            case .pause:
                pauseContent
            case .exit:
                confirmContent(
                    icon: "💾",
                    title: "要离开这一局吗？",
                    message: "离开后进度会保存，下次可以接着玩。",
                    confirmTitle: "离开",
                    cancelTitle: "继续玩",
                    onConfirm: { model.goHome() },
                    onCancel: { model.dialog = nil }
                )
            case .restart:
                confirmContent(
                    icon: "🔄",
                    title: "重新开始本局？",
                    message: "已经填的数字和用时都会清空。",
                    confirmTitle: "重新开始",
                    cancelTitle: "取消",
                    onConfirm: { model.restartPuzzle() },
                    onCancel: { model.dialog = .pause }
                )
            }
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 24)
        .background(theme.surface, in: RoundedRectangle(cornerRadius: 28))
        .shadow(color: .black.opacity(0.18), radius: 24, y: 8)
    }

    // MARK: Win

    private var winContent: some View {
        let result = model.lastResult
        return VStack(spacing: 0) {
            Text("🎉")
                .font(.system(size: 32))
                .frame(width: 64, height: 64)
                .background(theme.tertiaryContainer, in: Circle())

            Text("\(model.selectedSize.title) · \(model.selectedDifficulty.emoji) \(model.selectedDifficulty.title)")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(theme.onSurfaceVariant)
                .padding(.top, 14)

            Text((result?.stars ?? 0) == 3 ? "太棒了，全对通关！" : "恭喜通关！")
                .font(.system(size: 21, weight: .bold, design: .rounded))
                .foregroundStyle(theme.onBackground)
                .multilineTextAlignment(.center)
                .padding(.top, 6)

            Text(praise(result?.stars ?? 1))
                .font(.system(size: 12))
                .foregroundStyle(theme.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.top, 6)

            if let rank = result?.rank {
                Text(rank == 1 ? "🏆 新纪录！登上了第 1 名" : "🎖️ 进入排行榜第 \(rank) 名")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundStyle(theme.onSecondaryContainer)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 7)
                    .background(theme.secondaryContainer.opacity(0.7), in: RoundedRectangle(cornerRadius: 12))
                    .padding(.top, 10)
            }

            HStack(spacing: 6) {
                ForEach(0..<3, id: \.self) { index in
                    Text("★")
                        .font(.system(size: 40))
                        .foregroundStyle(index < (result?.stars ?? 0) ? theme.tertiary : theme.outlineVariant)
                }
            }
            .padding(.top, 16)

            HStack(spacing: 10) {
                resultTile("用时", formatTime(result?.seconds ?? 0))
                resultTile("失误", "\(result?.mistakes ?? 0)")
                resultTile("提示", "\(result?.hints ?? 0)")
            }
            .padding(.top, 18)

            if let result {
                Text(result.isNewBest
                     ? "🏅 新纪录！最佳成绩 \(formatTime(result.best))"
                     : "最佳成绩 \(formatTime(result.best))")
                    .font(.system(size: 14, weight: result.isNewBest ? .bold : .medium, design: .rounded))
                    .foregroundStyle(result.isNewBest ? theme.tertiary : theme.onSurfaceVariant)
                    .multilineTextAlignment(.center)
                    .padding(.top, 12)
            }

            primaryButton("再来一局") { model.playAgain() }
                .padding(.top, 22)

            Button { model.showLeaderboard(size: model.selectedSize, difficulty: model.selectedDifficulty) } label: {
                Text("🏆 看看排行榜")
                    .font(.system(size: 14, weight: .medium, design: .rounded))
                    .foregroundStyle(theme.onBackground)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.outline, lineWidth: 1))
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("win_leaderboard")
            .padding(.top, 8)

            Button("回到首页") { model.goHome() }
                .font(.system(size: 14, weight: .medium, design: .rounded))
                .foregroundStyle(theme.onSurfaceVariant)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .padding(.top, 8)
        }
    }

    private func praise(_ stars: Int) -> String {
        switch stars {
        case 3: "没有失误也没有用提示，真厉害！"
        case 2: "表现不错，再细心一点就满星啦"
        default: "完成了就是胜利，下一局会更顺"
        }
    }

    private func resultTile(_ label: String, _ value: String) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.system(size: 16, weight: .bold, design: .rounded))
                .foregroundStyle(theme.onBackground)
            Text(label)
                .font(.system(size: 11))
                .foregroundStyle(theme.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(theme.surfaceVariant.opacity(0.6), in: RoundedRectangle(cornerRadius: 14))
    }

    // MARK: Pause

    private var pauseContent: some View {
        VStack(spacing: 0) {
            Text("⏸️").font(.system(size: 34))
            Text("休息一下")
                .font(.system(size: 21, weight: .bold, design: .rounded))
                .foregroundStyle(theme.onBackground)
                .padding(.top, 8)
            Text("进度已经帮你保存好了")
                .font(.system(size: 12))
                .foregroundStyle(theme.onSurfaceVariant)
                .padding(.top, 6)

            primaryButton("继续游戏") { model.resume() }
                .padding(.top, 20)
            secondaryButton("重玩本局") { model.dialog = .restart }
            secondaryButton("回到首页") { model.goHome() }
        }
    }

    // MARK: Confirm

    private func confirmContent(
        icon: String,
        title: String,
        message: String,
        confirmTitle: String,
        cancelTitle: String,
        onConfirm: @escaping () -> Void,
        onCancel: @escaping () -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(.system(size: 19, weight: .bold, design: .rounded))
                .foregroundStyle(theme.onBackground)
                .padding(.top, 4)
            Text(message)
                .font(.system(size: 14))
                .foregroundStyle(theme.onSurfaceVariant)
                .padding(.top, 6)

            HStack(spacing: 10) {
                Button(action: onCancel) {
                    Text(cancelTitle)
                        .font(.system(size: 14, weight: .medium, design: .rounded))
                        .foregroundStyle(theme.onBackground)
                        .frame(maxWidth: .infinity)
                        .frame(height: 46)
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(theme.outline, lineWidth: 1))
                }
                .buttonStyle(.plain)

                Button(action: onConfirm) {
                    Text(confirmTitle)
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundStyle(theme.surface)
                        .frame(maxWidth: .infinity)
                        .frame(height: 46)
                        .background(theme.primary, in: RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
            }
            .padding(.top, 18)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: Buttons

    private func primaryButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 15, weight: .bold, design: .rounded))
                .foregroundStyle(theme.surface)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(theme.primary, in: RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(.plain)
    }

    private func secondaryButton(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14, weight: .medium, design: .rounded))
                .foregroundStyle(theme.onBackground)
                .frame(maxWidth: .infinity)
                .frame(height: 46)
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(theme.outline, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .padding(.top, 8)
    }
}

import SwiftUI

struct GameView: View {
    @ObservedObject var model: GameViewModel
    @Environment(\.theme) private var theme

    var body: some View {
        GeometryReader { geometry in
            let reserved: CGFloat = model.selectedSize == .nine ? 356 : 322
            let boardSide = min(geometry.size.width - 32, geometry.size.height - reserved, 560)
            VStack(spacing: 0) {
                header
                    .padding(.horizontal, 16)
                    .padding(.top, 4)

                Spacer(minLength: 10)

                ZStack {
                    SudokuBoardView(model: model)
                        .frame(width: max(220, boardSide), height: max(220, boardSide))
                        .opacity(model.isGenerating ? 0.35 : 1)
                        .accessibilityIdentifier("board")

                    if model.isGenerating {
                        VStack(spacing: 14) {
                            ProgressView()
                                .controlSize(.large)
                                .tint(theme.primary)
                            Text("正在出题…")
                                .font(.system(size: 14, weight: .medium, design: .rounded))
                                .foregroundStyle(theme.onSurfaceVariant)
                            Text("每一关都保证只有一个答案哦")
                                .font(.system(size: 12))
                                .foregroundStyle(theme.onSurfaceVariant.opacity(0.7))
                        }
                        .accessibilityIdentifier("generating_overlay")
                    }
                }

                Spacer(minLength: 6)

                ConflictBanner(message: model.conflict, hintText: model.hintText)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                    .frame(height: 40)
                    .padding(.horizontal, 16)

                Spacer(minLength: 6)

                statusRow
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 16)

                Spacer(minLength: 10)

                toolRow
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 16)

                Spacer(minLength: 10)

                numberPad
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 16)

                Spacer(minLength: 14)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(theme.background.ignoresSafeArea())
    }

    // MARK: Header

    private var header: some View {
        HStack(spacing: 6) {
            Button { model.dialog = .exit } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(theme.onBackground)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("返回")

            VStack(alignment: .leading, spacing: 1) {
                Text("\(model.selectedSize.title) · \(model.selectedDifficulty.emoji) \(model.selectedDifficulty.title)")
                    .font(.system(size: 16, weight: .bold, design: .rounded))
                    .foregroundStyle(theme.onBackground)
                Text("已完成 \(model.filledCount)/\(model.selectedSize.count)")
                    .font(.system(size: 11))
                    .foregroundStyle(theme.onSurfaceVariant)
            }
            Spacer(minLength: 4)

            Button { model.toggleQuietMode() } label: {
                Text(model.quietMode ? "🔇" : "🔊")
                    .font(.system(size: 15))
                    .padding(.horizontal, 9)
                    .padding(.vertical, 7)
                    .background(theme.surfaceVariant.opacity(model.quietMode ? 1 : 0.5),
                                in: RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("quiet_toggle")

            Button { model.dialog = .pause } label: {
                HStack(spacing: 6) {
                    Image(systemName: "pause.fill").font(.system(size: 13))
                        .foregroundStyle(theme.onSurfaceVariant)
                    Text(formatTime(model.seconds))
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundStyle(theme.onBackground)
                        .monospacedDigit()
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(theme.surfaceVariant.opacity(0.7), in: RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .disabled(model.isPaused || model.isComplete)
        }
    }

    // MARK: Status

    private var statusRow: some View {
        HStack(spacing: 8) {
            statusPill("失误", "\(model.mistakes)",
                       accent: model.mistakes > 0 ? theme.error : theme.onSurfaceVariant)
            statusPill("提示", "\(model.hints)/\(model.hintAllowance)", accent: theme.onSurfaceVariant)
            Spacer(minLength: 4)
            Text(model.noteMode ? "笔记模式" : "填数模式")
                .font(.system(size: 12, weight: model.noteMode ? .bold : .medium))
                .foregroundStyle(model.noteMode ? theme.secondary : theme.onSurfaceVariant)
        }
    }

    private func statusPill(_ label: String, _ value: String, accent: Color) -> some View {
        HStack(spacing: 5) {
            Text(label).font(.system(size: 11)).foregroundStyle(theme.onSurfaceVariant)
            Text(value).font(.system(size: 14, weight: .bold)).foregroundStyle(accent)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(theme.surfaceVariant.opacity(0.5), in: RoundedRectangle(cornerRadius: 10))
    }

    // MARK: Tools

    private var toolRow: some View {
        HStack(spacing: 10) {
            toolButton("撤销", symbol: "arrow.uturn.backward", active: false,
                       enabled: model.canUndo) { model.undo() }
            toolButton("笔记", symbol: "pencil.line", active: model.noteMode,
                       enabled: true) { model.toggleNotes() }
            toolButton("提示 \(model.hintAllowance - model.hints)", symbol: "lightbulb", active: false,
                       enabled: model.hints < model.hintAllowance) { model.useHint() }
        }
    }

    private func toolButton(
        _ title: String,
        symbol: String,
        active: Bool,
        enabled: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: symbol).font(.system(size: 15))
                Text(title).font(.system(size: 13, weight: .medium, design: .rounded))
            }
            .foregroundStyle(active ? theme.surface : theme.onBackground)
            .frame(maxWidth: .infinity)
            .frame(height: 46)
            .background(active ? theme.secondary : theme.surface, in: RoundedRectangle(cornerRadius: 14))
            .overlay {
                if !active {
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(theme.outlineVariant.opacity(0.8), lineWidth: 1)
                }
            }
            .opacity(enabled ? 1 : 0.4)
        }
        .buttonStyle(.plain)
        .disabled(!enabled || model.isPaused || model.isComplete)
    }

    // MARK: Number pad

    /// Same packing rule as Android: digits plus the erase key in one grid, so 4x4 is a single
    /// row of five, 6x6 a single row of seven, and 9x9 two rows of five.
    private var numberPad: some View {
        let size = model.selectedSize.rawValue
        let columns = size <= 6 ? size + 1 : 5
        let rows = (size + 1 + columns - 1) / columns
        let keyHeight: CGFloat = size <= 6 ? 64 : 58

        return VStack(spacing: 8) {
            ForEach(0..<rows, id: \.self) { row in
                HStack(spacing: 8) {
                    ForEach(0..<columns, id: \.self) { column in
                        let position = row * columns + column
                        if position < size {
                            digitKey(position + 1, height: keyHeight)
                        } else if position == size {
                            eraseKey(height: keyHeight)
                        } else {
                            Color.clear.frame(maxWidth: .infinity)
                        }
                    }
                }
            }
        }
    }

    private func digitKey(_ digit: Int, height: CGFloat) -> some View {
        let used = model.board.filter { $0 == digit }.count
        let remaining = max(0, model.selectedSize.rawValue - used)
        let enabled = !model.isPaused && !model.isComplete
            && model.selectedCell != nil && remaining > 0
        let content = model.noteMode ? theme.onSecondaryContainer : theme.onBackground
        return Button { model.inputDigit(digit) } label: {
            ZStack {
                RoundedRectangle(cornerRadius: 18)
                    .fill(model.noteMode ? theme.secondaryContainer : theme.surface.opacity(0.96))
                RoundedRectangle(cornerRadius: 18)
                    .stroke(theme.outlineVariant.opacity(0.7), lineWidth: 1)
                Text("\(digit)")
                    .font(.system(size: model.selectedSize == .nine ? 28 : 32, weight: .bold, design: .rounded))
                    .foregroundStyle(content.opacity(enabled ? 1 : 0.32))
                if remaining > 0 && remaining < model.selectedSize.rawValue {
                    VStack {
                        Spacer()
                        HStack {
                            Spacer()
                            Text("\(remaining)")
                                .font(.system(size: 10, weight: .medium))
                                .foregroundStyle(content.opacity(0.5))
                                .padding(.trailing, 6)
                                .padding(.bottom, 5)
                        }
                    }
                }
            }
            .frame(height: height)
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityIdentifier("digit_\(digit)")
    }

    private func eraseKey(height: CGFloat) -> some View {
        Button { model.eraseSelected() } label: {
            ZStack {
                RoundedRectangle(cornerRadius: 18).fill(theme.surfaceVariant)
                Image(systemName: "delete.left")
                    .font(.system(size: 21))
                    .foregroundStyle(theme.onSurfaceVariant)
            }
            .frame(height: height)
        }
        .buttonStyle(.plain)
        .disabled(model.isPaused || model.isComplete || model.selectedCell == nil)
        .accessibilityLabel("擦除")
    }
}

private struct ConflictBanner: View {
    @Environment(\.theme) private var theme
    let message: ConflictInfo?
    var hintText: String?

    var body: some View {
        ZStack {
            if let shown = hintText ?? message?.message {
                HStack(spacing: 6) {
                    Text("💡").font(.system(size: 14))
                    Text(shown)
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundStyle(theme.hintInk)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                        .accessibilityIdentifier("conflict_text")
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(theme.hintSurface, in: RoundedRectangle(cornerRadius: 12))
                .accessibilityIdentifier("conflict_banner")
            }
        }
        .transition(.opacity)
        .animation(.easeInOut(duration: 0.18), value: hintText ?? message?.message)
    }
}

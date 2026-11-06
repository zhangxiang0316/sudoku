import SwiftUI

struct GameView: View {
    @ObservedObject var model: GameViewModel

    var body: some View {
        GeometryReader { geometry in
            let boardSide = min(geometry.size.width - 32, geometry.size.height - (model.selectedSize == .nine ? 340 : 300), 560)
            VStack(spacing: 9) {
                header
                SudokuBoardView(model: model)
                    .frame(width: max(220, boardSide), height: max(220, boardSide))
                    .accessibilityIdentifier("board")
                ConflictBanner(message: model.conflict)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                    .frame(height: 38)
                    .padding(.horizontal, 16)
                statusRow
                    .padding(.horizontal, 18)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                toolRow
                    .padding(.horizontal, 16)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                numberPad
                    .padding(.horizontal, 16)
                    .frame(maxWidth: 592)
                    .frame(maxWidth: .infinity)
                Spacer(minLength: 2)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Palette.paper)
            .padding(.top, 5)
        }
        .background(Palette.paper.ignoresSafeArea())
    }

    private var header: some View {
        HStack(spacing: 10) {
            Button { model.dialog = .exit } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(Palette.ink)
                    .frame(width: 40, height: 40)
                    .background(Palette.white, in: Circle())
            }
            .accessibilityLabel("回到首页")
            VStack(alignment: .leading, spacing: 2) {
                Text("\(model.selectedSize.title) · \(model.selectedDifficulty.emoji) \(model.selectedDifficulty.title)")
                    .font(.system(size: 15, weight: .heavy, design: .rounded))
                    .foregroundStyle(Palette.ink)
                Text("已完成 \(model.filledCount)/\(model.selectedSize.count)")
                    .font(.system(size: 11, weight: .medium)).foregroundStyle(Palette.muted)
            }
            Spacer(minLength: 4)
            Button { model.toggleQuietMode() } label: {
                Image(systemName: model.quietMode ? "speaker.slash.fill" : "speaker.wave.2.fill")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Palette.ink)
                    .frame(width: 38, height: 38)
                    .background(Palette.white, in: Circle())
            }
            .accessibilityLabel(model.quietMode ? "开启声音与触感" : "静音")
            .accessibilityIdentifier("quiet_toggle")
            Button { model.dialog = .pause } label: {
                HStack(spacing: 5) {
                    Image(systemName: "clock")
                    Text(formatTime(model.seconds)).monospacedDigit()
                }
                .font(.system(size: 14, weight: .bold, design: .rounded))
                .foregroundStyle(Palette.ink)
                .padding(.horizontal, 11)
                .frame(height: 38)
                .background(Palette.hint, in: Capsule())
            }
            .disabled(model.isPaused || model.isGenerating || model.isComplete)
        }
        .padding(.horizontal, 16)
    }

    private var statusRow: some View {
        HStack(spacing: 8) {
            status(label: "失误", value: "\(model.mistakes)", tint: model.mistakes > 0 ? Palette.error : Palette.muted)
            status(label: "提示", value: "\(model.hints)/\(model.hintAllowance)", tint: Palette.muted)
            Spacer()
            Text(model.noteMode ? "✍️ 笔记模式" : "填数模式")
                .font(.system(size: 12, weight: .bold, design: .rounded))
                .foregroundStyle(model.noteMode ? Palette.teal : Palette.muted)
        }
    }

    private func status(label: String, value: String, tint: Color) -> some View {
        HStack(spacing: 4) {
            Text(label).foregroundStyle(Palette.muted)
            Text(value).foregroundStyle(tint).fontWeight(.heavy)
        }
        .font(.system(size: 12, weight: .medium, design: .rounded))
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(Palette.white, in: Capsule())
    }

    private var toolRow: some View {
        HStack(spacing: 8) {
            tool(title: "撤销", symbol: "arrow.uturn.backward", enabled: model.canUndo) { model.undo() }
            tool(title: model.noteMode ? "笔记中" : "笔记", symbol: "pencil.line", enabled: true, active: model.noteMode) { model.toggleNotes() }
            tool(title: "擦除", symbol: "delete.left", enabled: model.selectedCell != nil) { model.eraseSelected() }
            tool(title: "提示", symbol: "lightbulb", enabled: model.hints < model.hintAllowance) { model.useHint() }
        }
        .frame(height: 43)
    }

    private func tool(title: String, symbol: String, enabled: Bool, active: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Label(title, systemImage: symbol)
                .font(.system(size: 12, weight: .bold, design: .rounded))
                .foregroundStyle(active ? Palette.teal : Palette.ink)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(active ? Palette.selected : Palette.white, in: RoundedRectangle(cornerRadius: 11))
                .overlay(RoundedRectangle(cornerRadius: 11).stroke(active ? Palette.teal.opacity(0.4) : Palette.line.opacity(0.6), lineWidth: 1))
        }
        .buttonStyle(.plain)
        .disabled(!enabled || model.isPaused || model.isComplete)
        .opacity(enabled ? 1 : 0.45)
    }

    private var numberPad: some View {
        let digits = Array(1...model.selectedSize.rawValue)
        return VStack(spacing: 6) {
            if model.selectedSize == .nine {
                HStack(spacing: 6) { ForEach(Array(digits.prefix(5)), id: \.self) { digitButton($0) } }
                HStack(spacing: 6) { ForEach(Array(digits.suffix(4)), id: \.self) { digitButton($0) } }
            } else {
                HStack(spacing: 6) { ForEach(digits, id: \.self) { digitButton($0) } }
            }
        }
        .frame(maxWidth: 560)
        .frame(maxWidth: .infinity)
    }

    private func digitButton(_ digit: Int) -> some View {
        let remaining = model.board.filter { $0 == digit }.count
        let enabled = !model.isPaused && !model.isComplete && model.selectedCell != nil && remaining < model.selectedSize.rawValue
        return Button { model.inputDigit(digit) } label: {
            VStack(spacing: 1) {
                Text("\(digit)")
                    .font(.system(size: model.selectedSize == .nine ? 20 : 23, weight: .heavy, design: .rounded))
                    .foregroundStyle(Palette.teal)
                if model.selectedSize == .nine {
                    Text("\(model.selectedSize.rawValue - remaining)")
                        .font(.system(size: 8, weight: .medium, design: .rounded))
                        .foregroundStyle(Palette.muted.opacity(0.75))
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: model.selectedSize == .nine ? 43 : 53)
            .background(Palette.white, in: RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.4)
        .accessibilityIdentifier("digit_\(digit)")
    }
}

private struct ConflictBanner: View {
    let message: ConflictInfo?
    var body: some View {
        Group {
            if let message {
                HStack(spacing: 7) {
                    Text("💡")
                    Text(message.message)
                        .font(.system(size: 13, weight: .bold, design: .rounded))
                        .lineLimit(1)
                        .minimumScaleFactor(0.72)
                        .accessibilityIdentifier("conflict_text")
                    Spacer(minLength: 0)
                }
                .foregroundStyle(Color(red: 0.42, green: 0.29, blue: 0.07))
                .padding(.horizontal, 12)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Palette.hint, in: RoundedRectangle(cornerRadius: 11))
                .transition(.opacity)
                .accessibilityIdentifier("conflict_banner")
            }
        }
        .animation(.easeInOut(duration: 0.18), value: message?.message)
    }
}

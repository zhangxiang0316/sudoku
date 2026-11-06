import SwiftUI

struct SudokuBoardView: View {
    @ObservedObject var model: GameViewModel

    var body: some View {
        let size = model.selectedSize
        VStack(spacing: 0) {
            ForEach(0..<size.rawValue, id: \.self) { row in
                HStack(spacing: 0) {
                    ForEach(0..<size.rawValue, id: \.self) { column in
                        let index = row * size.rawValue + column
                        cell(index, row: row, column: column)
                    }
                }
                .frame(maxHeight: .infinity)
            }
        }
        .overlay {
            GridLines(size: size)
                .allowsHitTesting(false)
        }
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Palette.ink, lineWidth: 2))
        .shadow(color: Palette.ink.opacity(0.08), radius: 5, y: 2)
    }

    private func cell(_ index: Int, row: Int, column: Int) -> some View {
        let value = model.board.indices.contains(index) ? model.board[index] : 0
        let given = model.puzzle.indices.contains(index) && model.puzzle[index] != 0
        let isError = value != 0 && !given && model.solution.indices.contains(index) && value != model.solution[index]
        let selected = model.selectedCell == index
        let isConflict = model.conflict?.cells.contains(index) == true
        let selectedValue = model.selectedCell.flatMap { model.board.indices.contains($0) ? model.board[$0] : nil } ?? 0
        let same = value != 0 && value == selectedValue && !selected
        let peer = model.selectedCell.map { $0 != index && model.selectedSize.peers(of: $0).contains(index) } ?? false
        let background: Color = {
            if selected { return Palette.selected }
            if isError { return Palette.errorFill }
            if isConflict { return Palette.hint }
            if same { return Palette.same }
            if peer { return Palette.peer }
            return Palette.white
        }()

        return Button { model.selectCell(index) } label: {
            ZStack {
                background
                if isConflict {
                    Rectangle().stroke(Color(red: 0.85, green: 0.58, blue: 0.12), lineWidth: 1.5)
                }
                if value != 0 {
                    Text("\(value)")
                        .font(.system(size: cellFontSize, weight: given ? .heavy : .semibold, design: .rounded))
                        .foregroundStyle(isError ? Palette.error : (given ? Palette.ink : Palette.teal))
                        .minimumScaleFactor(0.6)
                } else if model.notes.indices.contains(index), model.notes[index] != 0 {
                    NotesView(mask: model.notes[index], size: model.selectedSize)
                        .padding(2)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(model.isPaused || model.isComplete || model.isGenerating)
        .accessibilityLabel("第\(row + 1)行第\(column + 1)列，\(value == 0 ? "空格" : "数字\(value)")")
        .accessibilityIdentifier("cell_\(index)")
    }

    private var cellFontSize: CGFloat {
        switch model.selectedSize {
        case .four: 31
        case .six: 25
        case .nine: 21
        }
    }
}

private struct NotesView: View {
    let mask: Int
    let size: BoardSize

    var body: some View {
        VStack(spacing: 0) {
            ForEach(0..<size.boxRows, id: \.self) { noteRow in
                HStack(spacing: 0) {
                    ForEach(0..<size.boxColumns, id: \.self) { noteColumn in
                        let digit = noteRow * size.boxColumns + noteColumn + 1
                        Text(mask & (1 << (digit - 1)) != 0 ? "\(digit)" : "")
                            .font(.system(size: size == .nine ? 7 : 9, weight: .medium, design: .rounded))
                            .foregroundStyle(Palette.muted)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                }
            }
        }
    }
}

private struct GridLines: View {
    let size: BoardSize

    var body: some View {
        Canvas { context, canvasSize in
            let side = min(canvasSize.width, canvasSize.height)
            for boundary in 0...size.rawValue {
                let position = side * CGFloat(boundary) / CGFloat(size.rawValue)
                let verticalThick = boundary % size.boxColumns == 0
                var vertical = Path()
                vertical.move(to: CGPoint(x: position, y: 0))
                vertical.addLine(to: CGPoint(x: position, y: side))
                context.stroke(vertical, with: .color(verticalThick ? Palette.ink : Palette.line), lineWidth: verticalThick ? 2 : 0.7)

                let horizontalThick = boundary % size.boxRows == 0
                var horizontal = Path()
                horizontal.move(to: CGPoint(x: 0, y: position))
                horizontal.addLine(to: CGPoint(x: side, y: position))
                context.stroke(horizontal, with: .color(horizontalThick ? Palette.ink : Palette.line), lineWidth: horizontalThick ? 2 : 0.7)
            }
        }
    }
}

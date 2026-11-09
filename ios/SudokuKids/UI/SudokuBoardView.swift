import SwiftUI

struct SudokuBoardView: View {
    @ObservedObject var model: GameViewModel
    @Environment(\.theme) private var theme

    var body: some View {
        let size = model.selectedSize
        VStack(spacing: 0) {
            ForEach(0..<size.rawValue, id: \.self) { row in
                HStack(spacing: 0) {
                    ForEach(0..<size.rawValue, id: \.self) { column in
                        cell(row * size.rawValue + column, row: row, column: column)
                    }
                }
                .frame(maxHeight: .infinity)
            }
        }
        .background(theme.surface)
        .overlay { GridLines(size: size).allowsHitTesting(false) }
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(theme.boardThickLine, lineWidth: 2))
    }

    private func cell(_ index: Int, row: Int, column: Int) -> some View {
        let value = model.board.indices.contains(index) ? model.board[index] : 0
        let given = model.puzzle.indices.contains(index) && model.puzzle[index] != 0
        let isError = value != 0 && !given
            && model.solution.indices.contains(index) && value != model.solution[index]
        let selected = model.selectedCell == index
        let isConflict = model.conflict?.cells.contains(index) == true
            || model.hintFocus == index
        let selectedValue = model.selectedCell
            .flatMap { model.board.indices.contains($0) ? model.board[$0] : nil } ?? 0
        let same = value != 0 && value == selectedValue && !selected
        let peer = model.selectedCell.map {
            $0 != index && model.selectedSize.peers(of: $0).contains(index)
        } ?? false

        // Android draws error first, then conflict, then same-number, then peer.
        let background: Color = {
            if selected { return theme.cellSelected }
            if isError { return theme.cellErrorFill }
            if isConflict { return theme.hintSurface }
            if same { return theme.cellSame }
            if peer { return theme.cellPeer }
            return Color.clear
        }()

        return Button { model.selectCell(index) } label: {
            ZStack {
                background
                if isConflict {
                    Rectangle().stroke(theme.conflictBorder, lineWidth: 1.5)
                }
                if value != 0 {
                    Text("\(value)")
                        .font(.system(size: fontSize, weight: given ? .bold : .medium, design: .rounded))
                        .foregroundStyle(isError ? theme.cellError
                                         : (given ? theme.onBackground : theme.userDigit))
                        .minimumScaleFactor(0.6)
                } else if model.notes.indices.contains(index), model.notes[index] != 0 {
                    NotesView(mask: model.notes[index], size: model.selectedSize)
                        .padding(3)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(model.isPaused || model.isComplete)
        .accessibilityLabel("第\(row + 1)行第\(column + 1)列，\(value == 0 ? "空格" : "数字\(value)")")
        .accessibilityIdentifier("cell_\(index)")
    }

    private var fontSize: CGFloat {
        switch model.selectedSize {
        case .four: 34
        case .six: 27
        case .nine: 22
        }
    }
}

/// Candidate digits laid out inside one cell, following the box shape like Android does.
private struct NotesView: View {
    @Environment(\.theme) private var theme
    let mask: Int
    let size: BoardSize

    var body: some View {
        VStack(spacing: 0) {
            ForEach(0..<size.boxRows, id: \.self) { row in
                HStack(spacing: 0) {
                    ForEach(0..<size.boxColumns, id: \.self) { column in
                        let digit = row * size.boxColumns + column + 1
                        Text(digit <= size.rawValue && mask & (1 << (digit - 1)) != 0 ? "\(digit)" : "")
                            .font(.system(size: size == .nine ? 8 : 10, weight: .medium))
                            .foregroundStyle(theme.noteText)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                }
            }
        }
    }
}

private struct GridLines: View {
    @Environment(\.theme) private var theme
    let size: BoardSize

    var body: some View {
        Canvas { context, canvasSize in
            let side = min(canvasSize.width, canvasSize.height)
            for boundary in 0...size.rawValue {
                let position = side * CGFloat(boundary) / CGFloat(size.rawValue)

                // Vertical lines separate boxes every `boxColumns` columns, horizontal ones
                // every `boxRows` rows. 6x6 needs both: 3 columns but only 2 rows.
                let verticalThick = boundary % size.boxColumns == 0
                var vertical = Path()
                vertical.move(to: CGPoint(x: position, y: 0))
                vertical.addLine(to: CGPoint(x: position, y: side))
                context.stroke(vertical,
                               with: .color(verticalThick ? theme.boardThickLine : theme.boardLine),
                               lineWidth: verticalThick ? 2 : 0.7)

                let horizontalThick = boundary % size.boxRows == 0
                var horizontal = Path()
                horizontal.move(to: CGPoint(x: 0, y: position))
                horizontal.addLine(to: CGPoint(x: side, y: position))
                context.stroke(horizontal,
                               with: .color(horizontalThick ? theme.boardThickLine : theme.boardLine),
                               lineWidth: horizontalThick ? 2 : 0.7)
            }
        }
    }
}

package com.family.sudoku.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.sudoku.game.BoardSpec
import com.family.sudoku.ui.theme.BoardColors
import com.family.sudoku.ui.theme.isDarkTheme
import com.family.sudoku.ui.theme.rememberBoardColors

/** Corner radius shared by the board clip and its border so the two always line up. */
val BoardCornerRadius = 20.dp

/**
 * Draws a Sudoku board for any [BoardSpec]: 4x4, 6x6 or 9x9. Both the thick "box" lines and
 * the candidate note layout are derived from the spec, so nothing here is hard coded to
 * nine columns.
 */
@Composable
fun SudokuBoard(
    spec: BoardSpec,
    board: List<Int>,
    puzzle: List<Int>,
    solution: List<Int>,
    notes: List<Int>,
    selectedCell: Int?,
    enabled: Boolean,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    conflictCells: List<Int> = emptyList(),
) {
    val dark = isDarkTheme
    val colors = rememberBoardColors(dark)
    val selectedValue = selectedCell?.let { board.getOrNull(it) } ?: 0

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(BoardCornerRadius))
            .background(colors.boardBackground)
            .testTag("board"),
    ) {
        val cellSize = maxWidth / spec.size
        val digitFontSize = (cellSize.value * 0.54f).coerceIn(15f, 54f).sp

        Column(Modifier.fillMaxSize()) {
            for (row in 0 until spec.size) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until spec.size) {
                        val index = spec.indexOf(row, col)
                        val value = board.getOrElse(index) { 0 }
                        val isGiven = puzzle.getOrElse(index) { 0 } != 0
                        val isError = value != 0 &&
                            !isGiven &&
                            value != solution.getOrElse(index) { 0 }
                        val isSelected = selectedCell == index
                        val isPeer = selectedCell != null &&
                            selectedCell != index &&
                            spec.arePeers(selectedCell, index)
                        val isSameNumber = selectedValue != 0 &&
                            value == selectedValue &&
                            !isSelected
                        val isConflict = conflictCells.contains(index)

                        val background = when {
                            isSelected -> colors.selectedBackground
                            isError -> colors.errorBackground
                            isConflict -> colors.conflictBackground
                            isSameNumber -> colors.sameNumberBackground
                            isPeer -> colors.peerBackground
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(background)
                                .then(
                                    if (isConflict) {
                                        Modifier.border(2.dp, colors.conflictBorder)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable(enabled = enabled) { onCellClick(index) }
                                .semantics {
                                    contentDescription = "第${row + 1}行第${col + 1}列" +
                                        if (value == 0) "空格" else "数字$value"
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (value != 0) {
                                Text(
                                    text = value.toString(),
                                    color = when {
                                        isError -> colors.errorText
                                        isGiven -> colors.givenText
                                        else -> colors.userText
                                    },
                                    fontSize = digitFontSize,
                                    fontWeight = if (isGiven) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    },
                                    textAlign = TextAlign.Center,
                                )
                            } else if (notes.getOrElse(index) { 0 } != 0) {
                                NoteGrid(
                                    spec = spec,
                                    mask = notes[index],
                                    color = colors.noteText,
                                    cellSizeDp = cellSize.value,
                                )
                            }
                        }
                    }
                }
            }
        }

        SudokuGridLines(
            spec = spec,
            modifier = Modifier.fillMaxSize(),
            colors = colors,
            dark = dark,
        )
    }
}

@Composable
private fun SudokuGridLines(
    spec: BoardSpec,
    modifier: Modifier,
    colors: BoardColors,
    dark: Boolean,
) {
    val thin = if (dark) 1f else 1.2f
    val thick = if (dark) 2.6f else 2.8f
    val cornerRadiusPx = with(LocalDensity.current) { BoardCornerRadius.toPx() }
    Canvas(modifier) {
        val cell = size.width / spec.size

        for (i in 1 until spec.size) {
            // A line is thick when it separates two boxes.
            val verticalThick = i % spec.boxCols == 0
            val horizontalThick = i % spec.boxRows == 0
            val position = cell * i

            drawLine(
                color = if (verticalThick) colors.thickLine else colors.line,
                start = Offset(position, 0f),
                end = Offset(position, size.height),
                strokeWidth = if (verticalThick) thick else thin,
                cap = StrokeCap.Square,
            )
            drawLine(
                color = if (horizontalThick) colors.thickLine else colors.line,
                start = Offset(0f, position),
                end = Offset(size.width, position),
                strokeWidth = if (horizontalThick) thick else thin,
                cap = StrokeCap.Square,
            )
        }

        // The frame must follow the rounded clip. Drawing it as a plain rectangle left the
        // four corners without any border, because the square corners fell outside the
        // rounded clip and were cut away.
        val border = thick * 1.4f
        val radius = cornerRadiusPx.coerceAtLeast(border / 2f)
        drawRoundRect(
            color = colors.thickLine,
            topLeft = Offset(border / 2f, border / 2f),
            size = Size(size.width - border, size.height - border),
            cornerRadius = CornerRadius(radius - border / 2f),
            style = Stroke(width = border),
        )
    }
}

/**
 * Notes are rendered as a small candidate grid inside one cell, the way a paper Sudoku
 * looks. The grid shape follows the board size: 2x2 for 4x4, 2x3 for 6x6, 3x3 for 9x9.
 */
@Composable
private fun NoteGrid(
    spec: BoardSpec,
    mask: Int,
    color: Color,
    cellSizeDp: Float,
) {
    val columns = spec.noteColumns
    val rows = spec.noteRows
    val fontSize = (cellSizeDp * 0.22f).coerceIn(7f, 11f).sp

    Column(
        modifier = Modifier.fillMaxSize().padding(2.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                for (col in 0 until columns) {
                    val digit = row * columns + col + 1
                    val visible = digit <= spec.size && mask and (1 shl (digit - 1)) != 0
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (visible) digit.toString() else "",
                            color = color,
                            fontSize = fontSize,
                            lineHeight = fontSize,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

package com.family.sudoku.data

import com.family.sudoku.game.BoardSpec

/**
 * The three board sizes offered to kids, ordered from the easiest entry point to the
 * classic 9x9 grid. The home screen presents them in this order.
 */
enum class BoardSize(
    val key: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val spec: BoardSpec,
    val hintAllowance: Int,
) {
    FOUR(
        key = "4",
        title = "4×4",
        subtitle = "2×2 宫 · 入门",
        emoji = "🐣",
        spec = BoardSpec.FOUR,
        hintAllowance = 2,
    ),
    SIX(
        key = "6",
        title = "6×6",
        subtitle = "2×3 宫 · 进阶",
        emoji = "🐥",
        spec = BoardSpec.SIX,
        hintAllowance = 3,
    ),
    NINE(
        key = "9",
        title = "9×9",
        subtitle = "3×3 宫 · 标准",
        emoji = "🦉",
        spec = BoardSpec.NINE,
        hintAllowance = 3,
    ),
    ;

    val size: Int get() = spec.size
    val cellCount: Int get() = spec.cellCount

    companion object {
        fun fromKey(key: String?): BoardSize? = entries.firstOrNull { it.key == key }

        fun fromSize(size: Int): BoardSize? = entries.firstOrNull { it.size == size }
    }
}

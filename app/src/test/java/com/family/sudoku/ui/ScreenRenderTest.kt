package com.family.sudoku.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.family.sudoku.data.BoardSize
import com.family.sudoku.data.Difficulty
import com.family.sudoku.game.AppScreen
import com.family.sudoku.game.GameKey
import com.family.sudoku.game.PlayerStats
import com.family.sudoku.game.SavedGamePreview
import com.family.sudoku.ui.game.GameScreen
import com.family.sudoku.game.ScoreEntry
import com.family.sudoku.ui.home.HomeScreen
import com.family.sudoku.game.GameResult
import com.family.sudoku.ui.game.WinDialog
import com.family.sudoku.ui.leaderboard.LeaderboardScreen
import com.family.sudoku.ui.theme.SudokuTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenRenderTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun homeScreenRenders() {
        composeRule.setContent {
            SudokuTheme(darkTheme = false) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    HomeScreen(
                        selectedSize = BoardSize.FOUR,
                        selectedDifficulty = Difficulty.MEDIUM,
                        stats = PlayerStats(
                            totalWins = 7,
                            totalStars = 15,
                            bestTimes = mapOf(
                                GameKey(BoardSize.FOUR, Difficulty.EASY) to 42,
                                GameKey(BoardSize.FOUR, Difficulty.MEDIUM) to 96,
                                GameKey(BoardSize.SIX, Difficulty.EASY) to 154,
                                GameKey(BoardSize.NINE, Difficulty.MEDIUM) to 402,
                            ),
                        ),
                        savedPreview = SavedGamePreview(
                            size = BoardSize.SIX,
                            difficulty = Difficulty.HARD,
                            filledCount = 28,
                            totalCount = 36,
                            seconds = 623,
                        ),
                        onSelectSize = {},
                        onSelectDifficulty = {},
                        onStart = {},
                        onContinue = {},
                        onOpenLeaderboard = {},
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        saveScreenshot("home.png")

        // The CTA must stay on screen and near the bottom, which is what the pinned layout
        // is for. This catches the layout regression found during development.
        composeRule.onNodeWithTag("leaderboard_entry").fetchSemanticsNode()
            .let { println("  leaderboard entry bounds=${it.boundsInRoot}") }

        val button = composeRule.onNodeWithTag("start_button").fetchSemanticsNode()
        val root = composeRule.onRoot().fetchSemanticsNode()
        println("  start button bounds=${button.boundsInRoot} root=${root.size}")
        assertTrue(
            "the start button must be inside the screen",
            button.boundsInRoot.bottom <= root.size.height,
        )
        assertTrue(
            "the start button should sit in the lower half of the screen",
            button.boundsInRoot.top > root.size.height / 2,
        )
    }

    @Test
    fun fourByFourGameRenders() {
        renderGameScreen(BoardSize.FOUR, "game_4x4.png")
    }

    @Test
    fun sixBySixGameRenders() {
        renderGameScreen(BoardSize.SIX, "game_6x6.png")
    }

    @Test
    fun nineByNineGameRenders() {
        renderGameScreen(BoardSize.NINE, "game_9x9.png")
    }

    // --- 错题提示 -------------------------------------------------------------

    /** Builds a mid-game state on the 4x4 board, optionally with a conflict explanation. */
    private fun conflictState(
        conflict: ConflictMessage?,
        quietMode: Boolean = false,
    ): GameUiState {
        val size = BoardSize.FOUR
        val solution = solvedGrid(size).toList()
        val puzzle = solution.toMutableList()
        listOf(2, 5, 8, 13).forEach { puzzle[it] = 0 }
        val board = puzzle.toMutableList()
        board[5] = solution[5]
        return GameUiState(
            screen = AppScreen.GAME,
            size = size,
            difficulty = Difficulty.EASY,
            board = board,
            puzzle = puzzle,
            solution = solution,
            notes = MutableList(size.spec.cellCount) { 0 },
            selectedCell = 8,
            mistakes = 1,
            seconds = 96,
            canUndo = true,
            quietMode = quietMode,
            conflict = conflict,
        )
    }

    @Test
    fun gameScreenExplainsAConflictWithoutMovingTheBoard() {
        val state = mutableStateOf(conflictState(null))
        composeRule.setContent {
            SudokuTheme(darkTheme = false) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    GameScreen(
                        state = state.value,
                        onBack = {},
                        onPause = {},
                        onCellClick = {},
                        onDigit = {},
                        onErase = {},
                        onUndo = {},
                        onToggleNotes = {},
                        onHint = {},
                        onRetry = {},
                        onToggleQuiet = {},
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val boardBefore = composeRule.onNodeWithTag("board").getUnclippedBoundsInRoot()
        saveScreenshot("game_no_conflict.png")

        // The sound switch must be on screen and must show the audible state.
        val speaker = composeRule.onNodeWithTag("quiet_toggle").getUnclippedBoundsInRoot()
        val screenWidth = composeRule.onRoot().getUnclippedBoundsInRoot().right
        println("  quiet toggle=$speaker screenWidth=$screenWidth")
        assertTrue(
            "the sound switch must stay on screen: $speaker",
            speaker.left >= 0.dp && speaker.right <= screenWidth,
        )
        composeRule.onNodeWithText("🔊").assertIsDisplayed()

        // Now show the explanation. The board must not move or shrink: a child aiming at a
        // cell would otherwise hit the wrong one the moment a hint appears.
        // Use the longest sentence the app can produce, so the test would catch the text
        // being clipped or pushed onto a second line.
        state.value = conflictState(
            ConflictMessage(
                id = 1,
                text = "这一格还不能填 8，再看看别的线索～",
                cells = listOf(1, 5),
            ),
        )
        composeRule.waitForIdle()

        val boardAfter = composeRule.onNodeWithTag("board").getUnclippedBoundsInRoot()
        println("  board before=$boardBefore after=$boardAfter")
        assertTrue(
            "the board must not move when a hint appears: $boardBefore vs $boardAfter",
            boardBefore == boardAfter,
        )

        composeRule.onNodeWithTag("conflict_text", useUnmergedTree = true)
            .assertIsDisplayed()

        // Muted state flips the switch glyph.
        state.value = conflictState(
            ConflictMessage(id = 2, text = "这一行已经有 3 了哦", cells = listOf(1, 5)),
            quietMode = true,
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithText("🔇").assertIsDisplayed()

        state.value = conflictState(
            ConflictMessage(
                id = 3,
                text = "这一格还不能填 8，再看看别的线索～",
                cells = listOf(1, 5),
            ),
        )
        composeRule.waitForIdle()

        val banner = composeRule.onNodeWithTag("conflict_banner").getUnclippedBoundsInRoot()
        val text = composeRule.onNodeWithTag("conflict_text", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        println("  banner=$banner text=$text")
        assertTrue(
            "the hint sentence must fit on one line inside the banner: text=$text banner=$banner",
            text.right <= banner.right && text.left >= banner.left,
        )
        saveScreenshot("game_conflict.png")
    }

    private fun renderGameScreen(size: BoardSize, screenshot: String) {
        val spec = size.spec
        val solution = solvedGrid(size).toList()
        val puzzle = solution.toMutableList()
        // Punch a deterministic set of holes so the screenshot shows a realistic mid-game.
        val holes = when (size) {
            BoardSize.FOUR -> listOf(2, 5, 8, 13)
            BoardSize.SIX -> listOf(1, 4, 9, 14, 20, 25, 31)
            BoardSize.NINE -> listOf(2, 3, 10, 11, 20, 40, 58, 66, 73, 78)
        }
        holes.forEach { puzzle[it] = 0 }
        val board = puzzle.toMutableList()
        // Fill the first hole correctly and note the rest, so highlights and notes render.
        val firstEmpty = holes.first()
        board[firstEmpty] = solution[firstEmpty]
        val notes = MutableList(spec.cellCount) { 0 }
        if (holes.size > 1) {
            notes[holes[1]] = 1 or 2 or (1 shl 4)
        }
        if (holes.size > 2) {
            notes[holes[2]] = 4 or 8
        }
        val selected = holes.getOrElse(3) { firstEmpty }

        val state = GameUiState(
            screen = AppScreen.GAME,
            size = size,
            difficulty = Difficulty.MEDIUM,
            board = board,
            puzzle = puzzle,
            solution = solution,
            notes = notes,
            selectedCell = selected,
            mistakes = 1,
            hints = if (size == BoardSize.FOUR) 1 else 2,
            seconds = 372,
            canUndo = true,
        )

        composeRule.setContent {
            SudokuTheme(darkTheme = false) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    GameScreen(
                        state = state,
                        onBack = {},
                        onPause = {},
                        onCellClick = {},
                        onDigit = {},
                        onErase = {},
                        onUndo = {},
                        onToggleNotes = {},
                        onHint = {},
                        onRetry = {},
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val bitmap = saveScreenshot(screenshot)

        // Every cell of the board must be reachable, and the digit keys for this size must
        // exist while keys above the board size must not.
        val boardRect = composeRule.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot
        val boardW = boardRect.right - boardRect.left
        val boardH = boardRect.bottom - boardRect.top
        println("  $screenshot board bounds=$boardRect  w=$boardW h=$boardH")
        assertTrue(
            "$screenshot board must stay square: w=$boardW h=$boardH",
            kotlin.math.abs(boardW - boardH) < 2f,
        )

        val corner = composeRule.onNodeWithContentDescription(
            "第${spec.size}行第${spec.size}列",
            substring = true,
            useUnmergedTree = true,
        ).fetchSemanticsNode()
        println("  $screenshot last cell bounds=${corner.boundsInRoot}")
        assertTrue(
            "$screenshot last cell must sit inside the board",
            corner.boundsInRoot.right <= boardRect.right + 1f &&
                corner.boundsInRoot.bottom <= boardRect.bottom + 1f,
        )
        // The numeric keypad must offer exactly the digits this board uses.
        composeRule.onNodeWithTag("digit_${size.size}").fetchSemanticsNode()
            .let { println("  $screenshot digit ${size.size} bounds=${it.boundsInRoot}") }
        if (size != BoardSize.NINE) {
            composeRule.onAllNodesWithTag("digit_${size.size + 1}").assertCountEquals(0)
        }

        assertBoardCornersAreRoundedAndBordered(bitmap, boardRect, screenshot)

        val rootHeight = composeRule.onRoot().fetchSemanticsNode().size.height
        val erase = composeRule.onNodeWithContentDescription("擦除", useUnmergedTree = true)
            .fetchSemanticsNode()
        println("  $screenshot erase bounds=${erase.boundsInRoot}")
        assertTrue(
            "$screenshot controls must fit above the bottom edge",
            erase.boundsInRoot.bottom <= rootHeight,
        )
    }

    @Test
    fun leaderboardRendersTopTen() {
        val entries = listOf(
            ScoreEntry(45, 3, 0, 0, 1_760_000_000_000),
            ScoreEntry(61, 3, 0, 0, 1_760_000_100_000),
            ScoreEntry(78, 2, 1, 1, 1_760_000_200_000),
            ScoreEntry(95, 2, 2, 0, 1_760_000_300_000),
            ScoreEntry(120, 1, 4, 2, 1_760_000_400_000),
            ScoreEntry(143, 3, 0, 0, 1_760_000_500_000),
            ScoreEntry(167, 2, 1, 0, 1_760_000_600_000),
            ScoreEntry(190, 1, 3, 1, 1_760_000_700_000),
            ScoreEntry(215, 2, 0, 2, 1_760_000_800_000),
            ScoreEntry(240, 1, 5, 3, 1_760_000_900_000),
        )

        composeRule.setContent {
            SudokuTheme(darkTheme = false) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    LeaderboardScreen(
                        selectedSize = BoardSize.FOUR,
                        selectedDifficulty = Difficulty.EASY,
                        entries = entries,
                        onBack = {},
                        onSelectSize = {},
                        onSelectDifficulty = {},
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        saveScreenshot("leaderboard.png")

        // All ten rows must exist and the fastest must be on top.
        (1..10).forEach { rank ->
            composeRule.onNodeWithTag("score_row_$rank").fetchSemanticsNode()
                .let { node -> println("  rank $rank bounds=${node.boundsInRoot}") }
        }
        val first = composeRule.onNodeWithTag("score_row_1").fetchSemanticsNode()
        val tenth = composeRule.onNodeWithTag("score_row_10").fetchSemanticsNode()
        println("  first=${first.boundsInRoot} tenth=${tenth.boundsInRoot}")
        assertTrue("rank 1 must sit above rank 10", first.boundsInRoot.top < tenth.boundsInRoot.top)
    }

    @Test
    fun winDialogShowsTheLeaderboardRank() {
        composeRule.setContent {
            SudokuTheme(darkTheme = false) {
                WinDialog(
                    result = GameResult(
                        seconds = 96,
                        mistakes = 0,
                        hints = 0,
                        stars = 3,
                        bestSeconds = 84,
                        isNewBest = false,
                        rank = 2,
                    ),
                    size = BoardSize.FOUR,
                    difficulty = Difficulty.MEDIUM,
                    onPlayAgain = {},
                    onHome = {},
                    onViewLeaderboard = {},
                )
            }
        }
        composeRule.waitForIdle()
        saveDialogScreenshot("win_dialog.png")
        composeRule.onNodeWithText("🎖️ 进入排行榜第 2 名").fetchSemanticsNode()
        composeRule.onNodeWithTag("win_leaderboard").fetchSemanticsNode()
    }

    @Test
    fun emptyLeaderboardShowsAHint() {
        composeRule.setContent {
            SudokuTheme(darkTheme = false) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    LeaderboardScreen(
                        selectedSize = BoardSize.NINE,
                        selectedDifficulty = Difficulty.EXPERT,
                        entries = emptyList(),
                        onBack = {},
                        onSelectSize = {},
                        onSelectDifficulty = {},
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        saveScreenshot("leaderboard_empty.png")
        composeRule.onNodeWithText("9×9 大师 还没有记录").fetchSemanticsNode()
    }

    @Test
    fun homeScreenKeepsEverythingReachable() {
        composeRule.setContent {
            SudokuTheme(darkTheme = false) {
                HomeScreen(
                    selectedSize = BoardSize.NINE,
                    selectedDifficulty = Difficulty.EXPERT,
                    stats = PlayerStats(totalWins = 2, totalStars = 6),
                    savedPreview = null,
                    onSelectSize = {},
                    onSelectDifficulty = {},
                    onStart = {},
                    onContinue = {},
                    onOpenLeaderboard = {},
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
        }
        composeRule.waitForIdle()
        BoardSize.entries.forEach { size ->
            composeRule.onNodeWithTag("size_${size.key}").fetchSemanticsNode()
                .let { println("  size ${size.title} card bounds=${it.boundsInRoot}") }
        }
        Difficulty.entries.forEach { difficulty ->
            composeRule.onNodeWithTag("difficulty_${difficulty.key}").fetchSemanticsNode()
                .let { println("  difficulty ${difficulty.title} card bounds=${it.boundsInRoot}") }
        }
        saveScreenshot("home_9x9_expert.png")
    }

    /**
     * A Compose dialog lives in its own window, so it has to be drawn separately from the
     * activity content; [onRoot] would otherwise fail with two roots.
     */
    private fun saveDialogScreenshot(name: String) {
        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        checkNotNull(dialog) { "expected a dialog to be shown" }
        // The dialog window is translucent, so draw the activity first and composite the
        // dialog on top; otherwise the screenshot is mostly transparent.
        val background = drawWindowToBitmap(composeRule.activity.window)
        val bitmap = background.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val dialogView = dialog.window!!.decorView
        canvas.save()
        canvas.translate(
            (bitmap.width - dialogView.width) / 2f,
            (bitmap.height - dialogView.height) / 2f,
        )
        dialogView.draw(canvas)
        canvas.restore()
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, name).outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        println("saved dialog screenshot: ${File(dir, name).absolutePath} (${bitmap.width}x${bitmap.height})")
    }

    private fun drawWindowToBitmap(window: android.view.Window): Bitmap {
        val view = window.decorView
        val width = view.width.takeIf { it > 0 } ?: 1080
        val height = view.height.takeIf { it > 0 } ?: 2340
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    private fun saveScreenshot(name: String): Bitmap {
        composeRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        val view = composeRule.activity.window.decorView
        val width = view.width.takeIf { it > 0 } ?: 1080
        val height = view.height.takeIf { it > 0 } ?: 2340
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, name).outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        println("saved screenshot: ${File(dir, name).absolutePath} (${width}x${height})")
        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        println("  robolectric root bounds=$rootBounds")
        return bitmap
    }

    /**
     * The board uses a rounded clip, so its frame must be drawn as a rounded rectangle too.
     * A square frame gets clipped away at the corners, leaving them blank - a regression that
     * was visible in the screenshots and is checked here on real pixels.
     */
    private fun assertBoardCornersAreRoundedAndBordered(
        bitmap: Bitmap,
        boardRect: androidx.compose.ui.geometry.Rect,
        label: String,
    ) {
        val left = boardRect.left.toInt()
        val top = boardRect.top.toInt()
        val right = boardRect.right.toInt()
        val bottom = boardRect.bottom.toInt()
        val density = composeRule.activity.resources.displayMetrics.density
        val probe = (28 * density).toInt()

        data class Corner(val name: String, val x: Int, val y: Int, val dx: Int, val dy: Int)
        val corners = listOf(
            Corner("左上", left, top, 1, 1),
            Corner("右上", right, top, -1, 1),
            Corner("左下", left, bottom, 1, -1),
            Corner("右下", right, bottom, -1, -1),
        )

        fun isBorder(x: Int, y: Int): Boolean {
            if (x !in 0 until bitmap.width || y !in 0 until bitmap.height) return false
            val pixel = bitmap.getPixel(x, y)
            val gray = ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3
            return gray < 110
        }

        for (corner in corners) {
            var borderPixels = 0
            for (dy in 0 until probe) {
                for (dx in 0 until probe) {
                    val x = corner.x + dx * corner.dx
                    val y = corner.y + dy * corner.dy
                    if (isBorder(x, y)) borderPixels++
                }
            }
            println("  $label ${corner.name} corner border pixels=$borderPixels")
            assertTrue(
                "$label ${corner.name} corner has no border; the frame must follow the rounded clip",
                borderPixels > 20,
            )
            // The rounded corner means the very tip is cut away: it must not be a hard
            // square corner painted with border colour.
            assertFalse(
                "$label ${corner.name} corner is not rounded",
                isBorder(corner.x, corner.y) && isBorder(corner.x + corner.dx, corner.y + corner.dy),
            )
        }
    }

    /** A deterministic completed grid of the requested size, used only for rendering. */
    private fun solvedGrid(size: BoardSize): IntArray {
        val spec = size.spec
        val grid = IntArray(spec.cellCount)
        fun canPlace(index: Int, value: Int): Boolean {
            val row = spec.rowOf(index)
            val col = spec.colOf(index)
            for (offset in 0 until spec.size) {
                if (grid[spec.indexOf(row, offset)] == value) return false
                if (grid[spec.indexOf(offset, col)] == value) return false
            }
            val boxRowStart = (row / spec.boxRows) * spec.boxRows
            val boxColStart = (col / spec.boxCols) * spec.boxCols
            for (r in boxRowStart until boxRowStart + spec.boxRows) {
                for (c in boxColStart until boxColStart + spec.boxCols) {
                    if (grid[spec.indexOf(r, c)] == value) return false
                }
            }
            return true
        }

        fun fill(index: Int): Boolean {
            if (index == grid.size) return true
            for (value in 1..spec.size) {
                if (canPlace(index, value)) {
                    grid[index] = value
                    if (fill(index + 1)) return true
                    grid[index] = 0
                }
            }
            return false
        }

        check(fill(0)) { "could not build a ${size.title} grid" }
        return grid
    }
}

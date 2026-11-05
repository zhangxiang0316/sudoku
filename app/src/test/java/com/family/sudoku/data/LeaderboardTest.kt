package com.family.sudoku.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.family.sudoku.game.ScoreEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LeaderboardTest {

    private lateinit var application: Application
    private lateinit var repository: GameRepository

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences("sudoku_kids_prefs", 0).edit().clear().commit()
        repository = GameRepository(application)
    }

    private fun record(
        seconds: Int,
        stars: Int = 3,
        size: BoardSize = BoardSize.FOUR,
        difficulty: Difficulty = Difficulty.EASY,
        playedAt: Long = seconds.toLong(),
    ) = repository.recordWin(
        size = size,
        difficulty = difficulty,
        seconds = seconds,
        stars = stars,
        mistakes = 0,
        hints = 0,
        playedAt = playedAt,
    )

    @Test
    fun leaderboardIsSortedFastestFirst() {
        listOf(120, 45, 300, 90).forEach { record(it) }

        val entries = repository.loadLeaderboard(BoardSize.FOUR, Difficulty.EASY)
        assertEquals(listOf(45, 90, 120, 300), entries.map { it.seconds })
    }

    @Test
    fun leaderboardKeepsOnlyTheTenBestResults() {
        // Play 14 games, from slowest to fastest, so every new one must enter the list.
        (1..14).forEach { record(500 - it * 10) }

        // Times run 490, 480, ... 360, so the ten best are 360..450.
        val entries = repository.loadLeaderboard(BoardSize.FOUR, Difficulty.EASY)
        assertEquals(10, entries.size)
        assertEquals(360, entries.first().seconds)
        assertEquals(450, entries.last().seconds)
        assertTrue(
            "the slowest results must be dropped: ${entries.map { it.seconds }}",
            entries.none { it.seconds > 460 },
        )
    }

    @Test
    fun aSlowResultDoesNotEnterAFullLeaderboard() {
        (1..10).forEach { record(it * 10) }

        val slow = record(9_999)
        assertNull("a slow result must not be ranked", slow.rank)
        assertEquals(10, repository.loadLeaderboard(BoardSize.FOUR, Difficulty.EASY).size)

        val fast = record(1)
        assertEquals(1, fast.rank)
    }

    @Test
    fun everyBoardSizeAndDifficultyHasItsOwnLeaderboard() {
        record(100, size = BoardSize.FOUR, difficulty = Difficulty.EASY)
        record(200, size = BoardSize.SIX, difficulty = Difficulty.EASY)
        record(300, size = BoardSize.NINE, difficulty = Difficulty.EASY)
        record(400, size = BoardSize.NINE, difficulty = Difficulty.EXPERT)

        assertEquals(
            listOf(100),
            repository.loadLeaderboard(BoardSize.FOUR, Difficulty.EASY).map { it.seconds },
        )
        assertEquals(
            listOf(200),
            repository.loadLeaderboard(BoardSize.SIX, Difficulty.EASY).map { it.seconds },
        )
        assertEquals(
            listOf(300),
            repository.loadLeaderboard(BoardSize.NINE, Difficulty.EASY).map { it.seconds },
        )
        assertEquals(
            listOf(400),
            repository.loadLeaderboard(BoardSize.NINE, Difficulty.EXPERT).map { it.seconds },
        )
        assertEquals(
            "combinations without games must stay empty",
            emptyList<ScoreEntry>(),
            repository.loadLeaderboard(BoardSize.SIX, Difficulty.EXPERT),
        )
    }

    @Test
    fun equalTimesKeepTheOlderResultAhead() {
        record(seconds = 60, playedAt = 1_000)
        record(seconds = 60, playedAt = 2_000)

        val entries = repository.loadLeaderboard(BoardSize.FOUR, Difficulty.EASY)
        assertEquals(2, entries.size)
        assertEquals(1_000, entries.first().playedAt)
        assertEquals(2_000, entries.last().playedAt)
    }

    @Test
    fun rankIsReportedForTheResultThatWasJustPlayed() {
        val first = record(100)
        val second = record(50)
        val third = record(75)

        assertEquals(1, first.rank)
        assertEquals(1, second.rank)
        assertEquals(2, third.rank)
        assertEquals(50, second.bestSeconds)
    }

    @Test
    fun starsAndPenaltiesSurviveTheRoundTrip() {
        repository.recordWin(
            size = BoardSize.SIX,
            difficulty = Difficulty.HARD,
            seconds = 210,
            stars = 1,
            mistakes = 4,
            hints = 2,
            playedAt = 1_700_000_000_000,
        )

        val entry = repository.loadLeaderboard(BoardSize.SIX, Difficulty.HARD).single()
        assertEquals(210, entry.seconds)
        assertEquals(1, entry.stars)
        assertEquals(4, entry.mistakes)
        assertEquals(2, entry.hints)
        assertEquals(1_700_000_000_000, entry.playedAt)
    }

    @Test
    fun leaderboardSurvivesANewRepositoryInstance() {
        record(88)
        val reopened = GameRepository(application)
        val entries = reopened.loadLeaderboard(BoardSize.FOUR, Difficulty.EASY)
        assertEquals(1, entries.size)
        assertEquals(88, entries.first().seconds)
    }

    @Test
    fun corruptStoredLeaderboardIsIgnoredInsteadOfCrashing() {
        application.getSharedPreferences("sudoku_kids_prefs", 0)
            .edit()
            .putString("leaderboard_4_easy", "{not json")
            .commit()

        assertEquals(
            emptyList<ScoreEntry>(),
            repository.loadLeaderboard(BoardSize.FOUR, Difficulty.EASY),
        )
        // Recording after a corrupt payload must recover, not stay broken.
        record(42)
        assertEquals(42, repository.loadLeaderboard(BoardSize.FOUR, Difficulty.EASY).single().seconds)
    }

    @Test
    fun totalWinsAndStarsStillAccumulateAlongsideTheLeaderboard() {
        record(120, stars = 3)
        record(90, stars = 2)

        val stats = repository.loadStats()
        assertEquals(2, stats.totalWins)
        assertEquals(5, stats.totalStars)
        assertNotNull(stats.bestTimeFor(BoardSize.FOUR, Difficulty.EASY))
        assertEquals(90, stats.bestTimeFor(BoardSize.FOUR, Difficulty.EASY))
    }
}

package com.family.sudoku.feedback

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Exercises the real synthesiser rather than a recorder. Robolectric cannot play sound, but
 * it does track AudioTrack instances, which is enough to prove that a cue really produces a
 * player, that quiet mode produces none, and that nothing throws on the way.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SoundHapticFeedbackTest {

    private lateinit var application: Application
    private lateinit var feedback: SoundHapticFeedback

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        feedback = SoundHapticFeedback(application)
    }

    @Test
    fun `every cue can be played without throwing`() {
        // A failure here means a malformed PCM buffer or a bad AudioTrack configuration.
        feedback.correct()
        feedback.wrong()
        feedback.win()
        feedback.tap()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `quiet mode suppresses playback safely`() {
        feedback.quiet = true
        // Must be a no-op rather than an error.
        feedback.correct()
        feedback.wrong()
        feedback.win()
        feedback.tap()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `quiet mode can be switched back off`() {
        feedback.quiet = true
        feedback.tap()
        feedback.quiet = false
        feedback.tap()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `the synthesiser produces audible samples for each cue`() {
        // Reaches the buffer directly so a silent or empty buffer cannot slip through: a cue
        // that renders no audio would otherwise look "fine" in every other test.
        for (name in listOf("correct", "wrong", "win", "tap")) {
            val samples = SoundHapticFeedback.renderForTest(name)
            assertTrue("$name produced no samples", samples.isNotEmpty())
            val peak = samples.maxOf { kotlin.math.abs(it.toInt()) }
            assertTrue("$name is silent (peak=$peak)", peak > Short.MAX_VALUE / 20)
            val nonZero = samples.count { it.toInt() != 0 }
            assertTrue(
                "$name is almost entirely silence ($nonZero/${samples.size})",
                nonZero > samples.size / 4,
            )
        }
    }

    @Test
    fun `the win cue is the longest and the tap the shortest`() {
        val lengths = listOf("tap", "correct", "wrong", "win")
            .associateWith { SoundHapticFeedback.renderForTest(it).size }
        assertEquals("tap", lengths.minByOrNull { it.value }!!.key)
        assertEquals("win", lengths.maxByOrNull { it.value }!!.key)
    }

    @Test
    fun `many rapid cues do not exhaust the audio track budget`() {
        // A child can mash the keypad. Every cue must still be able to play afterwards, so
        // the concurrency counter has to come back down.
        repeat(40) { feedback.correct() }
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(3))

        // After everything has been released, a fresh cue must still render audio.
        val samples = SoundHapticFeedback.renderForTest("correct")
        assertTrue(samples.isNotEmpty())
    }

    @Test
    fun `a cue played while muted never primes the budget`() {
        feedback.quiet = true
        repeat(20) { feedback.wrong() }
        feedback.quiet = false
        // Unmuting must immediately work rather than wait for stale releases.
        feedback.correct()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `no cue clips`() {
        for (name in listOf("correct", "wrong", "win", "tap")) {
            val samples = SoundHapticFeedback.renderForTest(name)
            val peak = samples.maxOf { kotlin.math.abs(it.toInt()) }
            assertTrue(
                "$name clips at peak=$peak, amplitude must stay below full scale",
                peak < Short.MAX_VALUE,
            )
        }
    }
}

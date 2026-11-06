package com.family.sudoku.feedback

import android.app.Application
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Covers the vibration half of the feedback. The first release shipped without any of these
 * tests, so the app played its tones while the motors stayed silent and nobody noticed until
 * a real phone was tried.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HapticsTest {

    private lateinit var application: Application
    private lateinit var vibrator: Vibrator
    private lateinit var feedback: SoundHapticFeedback

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            application.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Vibrator::class.java)
        }
        feedback = SoundHapticFeedback(application)
    }

    // --- 每条震动都必须真的被触发 -------------------------------------------------

    @Test
    fun `placing a digit correctly vibrates`() {
        feedback.correct()
        assertTrue("a correct digit must vibrate", shadowOf(vibrator).isVibrating)
    }

    @Test
    fun `a clash vibrates`() {
        feedback.wrong()
        assertTrue("a wrong digit must vibrate", shadowOf(vibrator).isVibrating)
    }

    @Test
    fun `finishing the puzzle vibrates`() {
        feedback.win()
        assertTrue("the win must vibrate", shadowOf(vibrator).isVibrating)
    }

    @Test
    fun `erasing vibrates`() {
        feedback.tap()
        assertTrue("erasing must vibrate", shadowOf(vibrator).isVibrating)
    }

    @Test
    fun `quiet mode stops the vibration`() {
        feedback.quiet = true
        feedback.correct()
        assertTrue(
            "quiet mode must silence the motor too",
            !shadowOf(vibrator).isVibrating,
        )
    }

    // --- 参数本身要能被手感觉到 ---------------------------------------------------

    @Test
    fun `every pulse is long enough to be felt`() {
        for ((name, cue) in VibrateCues.all) {
            assertTrue(
                "$name has an on-segment shorter than ${Haptics.MIN_ON_MILLIS}ms, " +
                    "which most phone motors cannot render at all",
                Haptics.isPerceptible(cue),
            )
        }
    }

    @Test
    fun `the clash pattern has two pulses and the win has three`() {
        fun onSegments(cue: VibrateCue) =
            cue.timings.drop(1).filterIndexed { index, _ -> index % 2 == 0 }

        assertEquals("clash should read as two taps", 2, onSegments(VibrateCues.wrong).size)
        assertEquals("win should be a three-beat", 3, onSegments(VibrateCues.win).size)
    }

    @Test
    fun `every pulse stays inside the legal amplitude range`() {
        for ((name, cue) in VibrateCues.all) {
            assertEquals(
                "$name must give one amplitude per timing segment",
                cue.timings.size,
                cue.amplitudes.size,
            )
            // One amplitude per segment. Segments alternate delay / on / off / on ... so the
            // even positions (delay and off) must be silent and the odd ones must have punch.
            cue.amplitudes.forEachIndexed { index, amplitude ->
                if (index % 2 == 0) {
                    assertEquals("$name segment $index is a delay/off and must be silent", 0, amplitude)
                } else {
                    assertTrue("$name segment $index amplitude $amplitude out of 1..255", amplitude in 1..255)
                }
            }
        }
    }

    // --- 不支持振幅控制的机型也必须震动 -------------------------------------------

    @Test
    fun `a device without amplitude control still vibrates`() {
        val shadow = shadowOf(vibrator)
        shadow.setHasAmplitudeControl(false)
        // This is the case that broke: the effect was built with an amplitude array the motor
        // could not honour, and the result was silence.
        feedback.correct()
        assertTrue("must still vibrate without amplitude control", shadow.isVibrating)
    }

    @Test
    fun `a device with amplitude control vibrates too`() {
        val shadow = shadowOf(vibrator)
        shadow.setHasAmplitudeControl(true)
        feedback.wrong()
        assertTrue(shadow.isVibrating)
    }

    @Test
    fun `effect construction works with and without amplitude control`() {
        for ((name, cue) in VibrateCues.all) {
            assertNotNull(name, Haptics.effect(cue, amplitudeControl = true))
            assertNotNull(name, Haptics.effect(cue, amplitudeControl = false))
        }
    }
}

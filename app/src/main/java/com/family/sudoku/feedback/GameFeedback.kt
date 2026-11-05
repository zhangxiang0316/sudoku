package com.family.sudoku.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.sin

/**
 * All player feedback that is not visual. Kept behind an interface so the ViewModel stays
 * unit testable: tests pass a recorder and assert on the cue that was requested.
 */
interface GameFeedback {
    /** A digit was placed correctly. */
    fun correct()

    /** A digit clashes with something already on the board. */
    fun wrong()

    /** The puzzle was completed. */
    fun win()

    /** Neutral confirmation, such as erasing. */
    fun tap()
}

/** Silent implementation used by tests that do not care about feedback. */
object NoFeedback : GameFeedback {
    override fun correct() = Unit
    override fun wrong() = Unit
    override fun win() = Unit
    override fun tap() = Unit
}

/** A single tone in a cue. */
internal data class Note(val frequency: Double, val millis: Int)

/**
 * The cues, defined once so both playback and tests read the same source of truth.
 *
 * The sounds are deliberately simple: a rising pair for a correct digit, a low falling pair
 * for a mistake (never harsh, because a child hears it often), a major arpeggio for a win,
 * and a very short blip for a neutral action.
 */
/** A full cue: the tones to play plus how loud this particular cue should be. */
internal data class Cue(val notes: Array<Note>, val gain: Double)

internal object Cues {
    private const val DO = 523.25
    private const val MI = 659.25
    private const val SO = 783.99
    private const val HIGH_DO = 1046.50

    private const val RISING_1 = 880.0
    private const val RISING_2 = 1174.66

    private const val FALL_1 = 311.13
    private const val FALL_2 = 233.08

    private const val SOFT = 659.25

    // Loudness is chosen per cue. A mistake is heard far more often than a win, so it stays
    // softer; the win is allowed to be the loudest thing in the app. None of them approach
    // full scale, which keeps the result pleasant on a phone speaker instead of harsh.
    val correct = Cue(arrayOf(Note(RISING_1, 70), Note(RISING_2, 95)), gain = 0.40)
    val wrong = Cue(arrayOf(Note(FALL_1, 130), Note(FALL_2, 200)), gain = 0.30)
    val win = Cue(
        arrayOf(Note(DO, 110), Note(MI, 110), Note(SO, 110), Note(HIGH_DO, 260)),
        gain = 0.46,
    )
    val tap = Cue(arrayOf(Note(SOFT, 45)), gain = 0.28)

    val all: Map<String, Cue> = mapOf(
        "correct" to correct,
        "wrong" to wrong,
        "win" to win,
        "tap" to tap,
    )
}

/**
 * Turns notes into 16-bit mono PCM. Separated from playback so the waveform can be inspected
 * without a device: a cue that silently rendered silence would otherwise look fine.
 */
internal object Synthesis {
    const val SAMPLE_RATE = 44_100
    const val AMPLITUDE = 0.22

    /** Safety ceiling; individual cues stay well below it. */
    private const val MAX_AMPLITUDE = 0.6

    fun render(
        notes: Array<Note>,
        sampleRate: Int = SAMPLE_RATE,
        amplitude: Double = AMPLITUDE,
    ): ShortArray {
        val amplitude = amplitude.coerceIn(0.0, MAX_AMPLITUDE)
        val total = notes.sumOf { it.millis } * sampleRate / 1000
        if (total <= 0) return ShortArray(0)
        val out = ShortArray(total)
        var cursor = 0
        for (note in notes) {
            val length = note.millis * sampleRate / 1000
            if (length <= 0) continue
            val attack = (sampleRate * 0.006f).toInt().coerceAtLeast(1)
            val release = (length * 0.55f).toInt().coerceAtLeast(1)
            for (i in 0 until length) {
                val phase = 2.0 * PI * note.frequency * i / sampleRate
                val envelope = when {
                    i < attack -> i.toDouble() / attack
                    i > length - release -> (length - i).toDouble() / release
                    else -> 1.0
                }
                val at = cursor + i
                if (at < out.size) {
                    out[at] = (sin(phase) * envelope * amplitude * Short.MAX_VALUE)
                        .toInt()
                        .toShort()
                }
            }
            cursor += length
        }
        return out
    }
}

/**
 * Plays short synthesised tones and vibrates, using only platform APIs so the app ships no
 * audio files.
 *
 * A single [quiet] switch disables both the sound and the vibration. That is deliberate: the
 * real world use case is "the little one is playing while the baby sleeps", where both should
 * stop at once.
 */
class SoundHapticFeedback(context: Context) : GameFeedback {

    /** When true nothing is played and nothing vibrates. */
    @Volatile
    var quiet: Boolean = false

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    /**
     * A child can mash the keypad faster than the cues finish. Android limits how many audio
     * tracks can exist at once, so sound is dropped rather than allowed to pile up.
     */
    private val activeTracks = AtomicInteger(0)

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = appContext.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Vibrator::class.java)
            }
        } catch (t: Throwable) {
            // A missing vibrator must never take the game down.
            null
        }
    }

    override fun correct() {
        play(Cues.correct)
        buzz(longArrayOf(0, 18), intArrayOf(0, 70))
    }

    override fun wrong() {
        play(Cues.wrong)
        buzz(longArrayOf(0, 55, 70, 55), intArrayOf(0, 150, 0, 150))
    }

    override fun win() {
        play(Cues.win)
        buzz(longArrayOf(0, 40, 55, 40, 55, 130), intArrayOf(0, 160, 0, 160, 0, 220))
    }

    override fun tap() {
        play(Cues.tap)
        buzz(longArrayOf(0, 12), intArrayOf(0, 45))
    }

    private fun buzz(timings: LongArray, amplitudes: IntArray) {
        if (quiet) return
        val target = vibrator ?: return
        if (!target.hasVibrator()) return
        try {
            target.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } catch (t: Throwable) {
            // Vibration is a nicety on top of the game.
        }
    }

    private fun play(cue: Cue) {
        if (quiet) return
        if (activeTracks.get() >= MAX_CONCURRENT_TRACKS) return
        val samples = Synthesis.render(cue.notes, amplitude = cue.gain)
        if (samples.isEmpty()) return
        val durationMs = cue.notes.sumOf { it.millis }

        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(Synthesis.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            activeTracks.incrementAndGet()
            track.write(samples, 0, samples.size)
            track.play()
        } catch (t: Throwable) {
            // Audio can fail on unusual devices; the game must keep working silently. The
            // counter is released here too, otherwise a few failures would stop sound for
            // the rest of the session.
            releaseTrack(track)
            return
        }

        val playing = track
        // Short sounds are not worth tracking individually; releasing on a delay keeps this
        // simple and avoids cutting the tail off.
        handler.postDelayed({ releaseTrack(playing) }, durationMs + 200L)
    }

    /** Releases a track exactly once and gives its slot back to the concurrency budget. */
    private fun releaseTrack(track: AudioTrack?) {
        if (track == null) return
        try {
            track.stop()
        } catch (t: Throwable) {
            // Already stopped.
        }
        try {
            track.release()
        } catch (t: Throwable) {
            // Already released.
        }
        activeTracks.decrementAndGet()
    }

    companion object {
        /** Enough overlap for a quick correction, well below the system's track limit. */
        private const val MAX_CONCURRENT_TRACKS = 4

        /** Renders a named cue to PCM without touching any device API. */
        internal fun renderForTest(cue: String): ShortArray {
            val found = Cues.all[cue] ?: error("unknown cue: $cue")
            return Synthesis.render(found.notes, amplitude = found.gain)
        }
    }
}

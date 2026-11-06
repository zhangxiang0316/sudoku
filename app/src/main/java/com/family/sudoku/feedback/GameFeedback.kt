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

/** One vibration pattern: alternating on/off durations plus the strength of each "on" segment. */
internal data class VibrateCue(val timings: LongArray, val amplitudes: IntArray)

/**
 * Builds vibration effects in a way every device accepts.
 *
 * The amplitudes overload only works where the motor can actually scale strength. On phones
 * that cannot, some ROMs ignore the request or refuse it outright, which shows up as "sound
 * plays but nothing vibrates". Falling back to the plain timings overload keeps those phones
 * buzzing at the motor's own strength instead of going silent.
 */
internal object Haptics {

    /** Phone motors need roughly 20ms or more before a pulse is felt at all. */
    const val MIN_ON_MILLIS = 20L

    fun effect(cue: VibrateCue, amplitudeControl: Boolean): VibrationEffect =
        if (amplitudeControl) {
            VibrationEffect.createWaveform(cue.timings, cue.amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(cue.timings, -1)
        }

    /** True when every "on" segment is long enough to be felt. */
    fun isPerceptible(cue: VibrateCue): Boolean {
        val onSegments = cue.timings.drop(1).filterIndexed { index, _ -> index % 2 == 0 }
        return onSegments.isNotEmpty() && onSegments.all { it >= MIN_ON_MILLIS }
    }
}

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
/**
 * The vibration patterns, kept next to [Cues] so the two can be read side by side.
 *
 * Every "on" segment is at least [Haptics.MIN_ON_MILLIS] long. The first version used 12ms
 * and 18ms pulses for the two most frequent actions, which most phone motors cannot render at
 * all, so a correct digit and an erase felt like nothing happened.
 */
internal object VibrateCues {
    /** Placing a digit correctly: one short acknowledgement. */
    val correct = VibrateCue(
        timings = longArrayOf(0, 30),
        amplitudes = intArrayOf(0, 130),
    )

    /** A clash: two pulses, so it reads as "no" rather than "yes". */
    val wrong = VibrateCue(
        timings = longArrayOf(0, 55, 60, 55),
        amplitudes = intArrayOf(0, 200, 0, 200),
    )

    /** Finishing the puzzle: a longer rising three-beat. */
    val win = VibrateCue(
        timings = longArrayOf(0, 45, 45, 45, 45, 150),
        amplitudes = intArrayOf(0, 170, 0, 170, 0, 255),
    )

    /** Erasing: the lightest of the four. */
    val tap = VibrateCue(
        timings = longArrayOf(0, 22),
        amplitudes = intArrayOf(0, 90),
    )

    val all: Map<String, VibrateCue> = mapOf(
        "correct" to correct,
        "wrong" to wrong,
        "win" to win,
        "tap" to tap,
    )
}

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

    private val vibrator: Vibrator? by lazy { resolveVibrator(appContext) }

    private fun resolveVibrator(context: Context): Vibrator? {
        // Prefer the modern manager, then fall back: some devices answer one path but not the
        // other, and a null here would silently disable every vibration.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { context.getSystemService(VibratorManager::class.java)?.defaultVibrator }
                .getOrNull()
                ?.let { return it }
        }
        return runCatching {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }.getOrNull()
    }

    override fun correct() {
        play(Cues.correct)
        buzz(VibrateCues.correct)
    }

    override fun wrong() {
        play(Cues.wrong)
        buzz(VibrateCues.wrong)
    }

    override fun win() {
        play(Cues.win)
        buzz(VibrateCues.win)
    }

    override fun tap() {
        play(Cues.tap)
        buzz(VibrateCues.tap)
    }

    private fun buzz(cue: VibrateCue) {
        if (quiet) return
        val target = vibrator ?: return
        // hasVibrator() is not consulted on purpose: it reports false on some devices that do
        // have a motor when queried through VibratorManager. Trying is harmless anyway.
        try {
            target.vibrate(Haptics.effect(cue, target.hasAmplitudeControl()))
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

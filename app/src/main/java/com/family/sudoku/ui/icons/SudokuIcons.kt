package com.family.sudoku.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The handful of icons this app actually draws, defined locally instead of depending on
 * `material-icons-extended`.
 *
 * That library ships every Material icon (about eleven thousand classes). Linking it pulled
 * the whole catalogue into the APK and made the debug build roughly 17 MB, where the app's
 * own code plus Compose is only a few megabytes. The path data below is copied verbatim from
 * the rounded Material icon set, so the icons look identical to before.
 */
object SudokuIcons {

    private fun build(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes(pathData),
                fill = SolidColor(Color.Black),
            )
        }.build()

    /** Undo arrow, mirrored so it points the natural way in a left-to-right layout. */
    val Undo: ImageVector by lazy {
        build(
            name = "SudokuIcon.Undo",
            pathData = "M12.5,8 c-2.65,0 -5.05,0.99 -6.9,2.6 L3.71,8.71 C3.08,8.08 2,8.52 " +
                "2,9.41 V15 c0,0.55 0.45,1 1,1 h5.59 c0.89,0 1.34,-1.08 0.71,-1.71 " +
                "l-1.91,-1.91 c1.39,-1.16 3.16,-1.88 5.12,-1.88 c3.16,0 5.89,1.84 7.19,4.5 " +
                "c0.27,0.56 0.91,0.84 1.5,0.64 c0.71,-0.23 1.07,-1.04 0.75,-1.72 " +
                "C20.23,10.42 16.65,8 12.5,8 Z",
        )
    }

    /** Outlined lightbulb used for the hint action. */
    val Lightbulb: ImageVector by lazy {
        build(
            name = "SudokuIcon.Lightbulb",
            pathData = "M9,21 c0,0.55 0.45,1 1,1 h4 c0.55,0 1,-0.45 1,-1 v-1 L9,20 v1 Z " +
                "M12,2 C8.14,2 5,5.14 5,9 c0,2.38 1.19,4.47 3,5.74 L8,17 " +
                "c0,0.55 0.45,1 1,1 h6 c0.55,0 1,-0.45 1,-1 v-2.26 " +
                "c1.81,-1.27 3,-3.36 3,-5.74 c0,-3.86 -3.14,-7 -7,-7 Z " +
                "M14.85,13.1 l-0.85,0.6 L14,16 h-4 v-2.3 l-0.85,-0.6 " +
                "C7.8,12.16 7,10.63 7,9 c0,-2.76 2.24,-5 5,-5 s5,2.24 5,5 " +
                "c0,1.63 -0.8,3.16 -2.15,4.1 Z",
        )
    }

    /** Backspace key used by the erase button. */
    val Backspace: ImageVector by lazy {
        build(
            name = "SudokuIcon.Backspace",
            pathData = "M22,3 L7,3 c-0.69,0 -1.23,0.35 -1.59,0.88 L0.37,11.45 " +
                "c-0.22,0.34 -0.22,0.77 0,1.11 l5.04,7.56 c0.36,0.52 0.9,0.88 1.59,0.88 " +
                "h15 c1.1,0 2,-0.9 2,-2 L24,5 c0,-1.1 -0.9,-2 -2,-2 Z " +
                "M18.3,16.3 c-0.39,0.39 -1.02,0.39 -1.41,0 L14,13.41 l-2.89,2.89 " +
                "c-0.39,0.39 -1.02,0.39 -1.41,0 c-0.39,-0.39 -0.39,-1.02 0,-1.41 " +
                "L12.59,12 L9.7,9.11 c-0.39,-0.39 -0.39,-1.02 0,-1.41 " +
                "c0.39,-0.39 1.02,-0.39 1.41,0 L14,10.59 l2.89,-2.89 " +
                "c0.39,-0.39 1.02,-0.39 1.41,0 c0.39,0.39 0.39,1.02 0,1.41 " +
                "L15.41,12 l2.89,2.89 c0.38,0.38 0.38,1.02 0,1.41 Z",
        )
    }

    /** Note taking icon: lines plus a pencil. */
    val EditNote: ImageVector by lazy {
        build(
            name = "SudokuIcon.EditNote",
            pathData = "M14,11 c0,0.55 -0.45,1 -1,1 H4 c-0.55,0 -1,-0.45 -1,-1 " +
                "s0.45,-1 1,-1 h9 C13.55,10 14,10.45 14,11 Z " +
                "M3,7 c0,0.55 0.45,1 1,1 h9 c0.55,0 1,-0.45 1,-1 s-0.45,-1 -1,-1 H4 " +
                "C3.45,6 3,6.45 3,7 Z " +
                "M10,15 c0,-0.55 -0.45,-1 -1,-1 H4 c-0.55,0 -1,0.45 -1,1 " +
                "s0.45,1 1,1 h5 C9.55,16 10,15.55 10,15 Z " +
                "M18.01,12.87 l0.71,-0.71 c0.39,-0.39 1.02,-0.39 1.41,0 l0.71,0.71 " +
                "c0.39,0.39 0.39,1.02 0,1.41 l-0.71,0.71 L18.01,12.87 Z " +
                "M17.3,13.58 l-5.16,5.16 C12.05,18.83 12,18.95 12,19.09 v1.41 " +
                "c0,0.28 0.22,0.5 0.5,0.5 h1.41 c0.13,0 0.26,-0.05 0.35,-0.15 " +
                "l5.16,-5.16 L17.3,13.58 Z",
        )
    }

    /** Pause bars for the timer chip. */
    val Pause: ImageVector by lazy {
        build(
            name = "SudokuIcon.Pause",
            pathData = "M8,19 c1.1,0 2,-0.9 2,-2 L10,7 c0,-1.1 -0.9,-2 -2,-2 " +
                "s-2,0.9 -2,2 v10 c0,1.1 0.9,2 2,2 Z " +
                "M14,7 v10 c0,1.1 0.9,2 2,2 s2,-0.9 2,-2 L18,7 c0,-1.1 -0.9,-2 -2,-2 " +
                "s-2,0.9 -2,2 Z",
        )
    }
}

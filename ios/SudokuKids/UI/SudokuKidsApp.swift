import SwiftUI

@main
struct SudokuKidsApp: App {
    @StateObject private var model = GameViewModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView(model: model)
                // Follows the system setting, exactly like Android's isSystemInDarkTheme().
                .onChange(of: scenePhase) { phase in
                    if phase != .active { model.onBackgrounded() }
                }
        }
    }
}

/// Semantic colours for one appearance. Every screen reads from here instead of hard coding
/// values, which is what makes dark mode a single-file change.
struct Theme {
    // Surfaces
    let background: Color
    let surface: Color
    let surfaceVariant: Color

    // Text
    let onBackground: Color
    let onSurface: Color
    let onSurfaceVariant: Color

    // Accents
    let primary: Color
    let onPrimary: Color
    let primaryContainer: Color
    let onPrimaryContainer: Color
    let secondary: Color
    let onSecondary: Color
    let secondaryContainer: Color
    let onSecondaryContainer: Color
    let tertiary: Color
    let onTertiary: Color
    let tertiaryContainer: Color
    let onTertiaryContainer: Color

    // Lines and errors
    let outline: Color
    let outlineVariant: Color
    let error: Color
    let errorContainer: Color
    let onErrorContainer: Color

    // Board
    let boardLine: Color
    let boardThickLine: Color
    let givenDigit: Color
    let userDigit: Color
    let cellError: Color
    let cellErrorFill: Color
    let cellSelected: Color
    let cellPeer: Color
    let cellSame: Color
    let noteText: Color
    let hintSurface: Color
    let hintInk: Color
    let conflictBorder: Color

    /// Card accents for the three board sizes, matching Android's teal / blue / purple.
    let sizeAccentFour: Color
    let sizeAccentSix: Color
    let sizeAccentNine: Color
}

extension Theme {
    /// Values copied from the Android light scheme.
    static let light = Theme(
        background: Color(hex: 0xFFF8EE),
        surface: Color(hex: 0xFFFCF8),
        surfaceVariant: Color(hex: 0xF1E7D9),
        onBackground: Color(hex: 0x332A22),
        onSurface: Color(hex: 0x332A22),
        onSurfaceVariant: Color(hex: 0x6D5D4E),
        primary: Color(hex: 0xF26B4F),
        onPrimary: Color(hex: 0xFFFCF8),
        primaryContainer: Color(hex: 0xFFE3DB),
        onPrimaryContainer: Color(hex: 0x4A1E14),
        secondary: Color(hex: 0x2A9D8F),
        onSecondary: Color(hex: 0xFFFCF8),
        secondaryContainer: Color(hex: 0xDDF4EF),
        onSecondaryContainer: Color(hex: 0x0A3B35),
        tertiary: Color(hex: 0xF4B942),
        onTertiary: Color(hex: 0x4A3300),
        tertiaryContainer: Color(hex: 0xFFF0C9),
        onTertiaryContainer: Color(hex: 0x3D2A00),
        outline: Color(hex: 0xC9B7A3),
        outlineVariant: Color(hex: 0xE8DCCB),
        error: Color(hex: 0xC84B45),
        errorContainer: Color(hex: 0xFFE1DE),
        onErrorContainer: Color(hex: 0x5F1410),
        boardLine: Color(hex: 0xE0D2C0),
        boardThickLine: Color(hex: 0x4A3B2E),
        givenDigit: Color(hex: 0x332A22),
        userDigit: Color(hex: 0x1F7A6E),
        cellError: Color(hex: 0xC0392B),
        cellErrorFill: Color(hex: 0xFFE0DC),
        cellSelected: Color(hex: 0xFFD9C9),
        cellPeer: Color(hex: 0xF7EFE2),
        cellSame: Color(hex: 0xFFEBC2),
        noteText: Color(hex: 0x8A7766),
        hintSurface: Color(hex: 0xFFF0C9),
        hintInk: Color(hex: 0x6B4A12),
        conflictBorder: Color(hex: 0xE39A1F),
        sizeAccentFour: Color(hex: 0x2A9D8F),
        sizeAccentSix: Color(hex: 0x4C8DD9),
        sizeAccentNine: Color(hex: 0x8E6BC8)
    )

    /// Values copied from the Android dark scheme (Night* colours).
    static let dark = Theme(
        background: Color(hex: 0x171310),
        surface: Color(hex: 0x211B16),
        surfaceVariant: Color(hex: 0x2B241E),
        onBackground: Color(hex: 0xF4EADF),
        onSurface: Color(hex: 0xF4EADF),
        onSurfaceVariant: Color(hex: 0xD6C4B4),
        primary: Color(hex: 0xFF9B84),
        onPrimary: Color(hex: 0x4A160A),
        primaryContainer: Color(hex: 0x6A2A1B),
        onPrimaryContainer: Color(hex: 0xFFDAD1),
        secondary: Color(hex: 0x63D4C3),
        onSecondary: Color(hex: 0x003730),
        secondaryContainer: Color(hex: 0x0C4A42),
        onSecondaryContainer: Color(hex: 0xB5F1E8),
        tertiary: Color(hex: 0xF6C762),
        onTertiary: Color(hex: 0x402D00),
        tertiaryContainer: Color(hex: 0x5C4400),
        onTertiaryContainer: Color(hex: 0xFFE08A),
        outline: Color(hex: 0x4D4036),
        outlineVariant: Color(hex: 0x3A3028),
        error: Color(hex: 0xFFB4AB),
        errorContainer: Color(hex: 0x93000A),
        onErrorContainer: Color(hex: 0xFFDAD6),
        boardLine: Color(hex: 0x494038),
        boardThickLine: Color(hex: 0xE6D5C3),
        givenDigit: Color(hex: 0xF4EADF),
        userDigit: Color(hex: 0x63D4C3),
        cellError: Color(hex: 0xFF9A93),
        cellErrorFill: Color(hex: 0x5A2320),
        cellSelected: Color(hex: 0x5C3A2A),
        cellPeer: Color(hex: 0x2C2A25),
        cellSame: Color(hex: 0x3E3830),
        noteText: Color(hex: 0xD6C4B4),
        hintSurface: Color(hex: 0x4A3A18),
        hintInk: Color(hex: 0xF2D89A),
        conflictBorder: Color(hex: 0xE0B04A),
        sizeAccentFour: Color(hex: 0x63D4C3),
        sizeAccentSix: Color(hex: 0x7FB2E8),
        sizeAccentNine: Color(hex: 0xB79BE0)
    )
}

private struct ThemeKey: EnvironmentKey {
    static let defaultValue = Theme.light
}

extension EnvironmentValues {
    var theme: Theme {
        get { self[ThemeKey.self] }
        set { self[ThemeKey.self] = newValue }
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

struct RootView: View {
    @ObservedObject var model: GameViewModel
    @Environment(\.colorScheme) private var colorScheme

    private var theme: Theme { colorScheme == .dark ? .dark : .light }

    var body: some View {
        ZStack {
            theme.background.ignoresSafeArea()
            Group {
                switch model.screen {
                case .home:
                    HomeView(model: model)
                case .game:
                    GameView(model: model)
                case .leaderboard:
                    LeaderboardView(model: model)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            // Android uses a centred dialog over a scrim; a system sheet would look like a
            // completely different product, so the same card is drawn here.
            if let dialog = model.dialog {
                Color.black.opacity(0.42)
                    .ignoresSafeArea()
                    .transition(.opacity)
                GameDialogView(model: model, dialog: dialog)
                    .frame(maxWidth: 380)
                    .padding(.horizontal, 28)
                    .transition(.opacity.combined(with: .scale(scale: 0.94)))
            }
        }
        .environment(\.theme, theme)
        .animation(.easeInOut(duration: 0.16), value: model.dialog)
    }
}

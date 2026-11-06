import SwiftUI

@main
struct SudokuKidsApp: App {
    @StateObject private var model = GameViewModel()
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            RootView(model: model)
                .preferredColorScheme(.light)
                .onChange(of: scenePhase) { phase in
                    if phase != .active { model.onBackgrounded() }
                }
        }
    }
}

enum Palette {
    static let paper = Color(red: 1.0, green: 0.988, blue: 0.973)
    static let ink = Color(red: 0.20, green: 0.16, blue: 0.13)
    static let muted = Color(red: 0.53, green: 0.45, blue: 0.39)
    static let coral = Color(red: 0.88, green: 0.36, blue: 0.27)
    static let teal = Color(red: 0.12, green: 0.48, blue: 0.43)
    static let line = Color(red: 0.84, green: 0.77, blue: 0.68)
    static let peer = Color(red: 0.97, green: 0.94, blue: 0.89)
    static let selected = Color(red: 1.0, green: 0.85, blue: 0.78)
    static let same = Color(red: 1.0, green: 0.92, blue: 0.76)
    static let error = Color(red: 0.77, green: 0.21, blue: 0.16)
    static let errorFill = Color(red: 1.0, green: 0.88, blue: 0.86)
    static let hint = Color(red: 1.0, green: 0.94, blue: 0.79)
    static let white = Color(red: 1.0, green: 0.996, blue: 0.98)
}

struct RootView: View {
    @ObservedObject var model: GameViewModel

    var body: some View {
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
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(Palette.paper.ignoresSafeArea())
        .animation(.easeInOut(duration: 0.18), value: model.screen)
        .sheet(item: $model.dialog) { dialog in
            GameDialogView(model: model, dialog: dialog)
                .presentationDetents(dialog == .win ? [.medium, .large] : [.medium])
                .presentationDragIndicator(.visible)
                .interactiveDismissDisabled(true)
        }
    }
}

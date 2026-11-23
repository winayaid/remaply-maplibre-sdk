import Shared
import SwiftUI

@main
struct IOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
        }
    }
}

/// Hosts the shared Compose UI and hands it the Swift map factory.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(mapFactory: RemaplyNativeMapFactory())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

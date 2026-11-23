import SwiftUI

/// Shared look for the sample screens.
enum SampleColors {
    static let accent = Color(red: 0, green: 0x79 / 255.0, blue: 0x6B / 255.0)
    static let panel = Color.white.opacity(0.92)
}

/// A full-screen map with an overlay panel at the top. The panel leaves room on the right for the
/// map's compass (top-right) and stays inside the safe area.
struct MapScreen<Map: View, Panel: View>: View {
    @ViewBuilder let map: () -> Map
    @ViewBuilder let panel: () -> Panel

    var body: some View {
        ZStack(alignment: .top) {
            map().ignoresSafeArea()
            VStack(alignment: .leading, spacing: 4, content: panel)
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(SampleColors.panel)
                .cornerRadius(8)
                .padding(.leading, 12)
                .padding(.trailing, 64) // compass clearance
                .padding(.top, 12)
        }
    }
}

struct StatusText: View {
    let text: String
    var secondary = false

    var body: some View {
        Text(text)
            .font(.system(size: secondary ? 13 : 15))
            .foregroundColor(secondary ? .secondary : .primary)
            .accessibilityIdentifier(secondary ? "camera" : "status")
    }
}

struct SampleButton: View {
    private let title: String
    private let action: () -> Void

    init(_ title: String, action: @escaping () -> Void) {
        self.title = title
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 14))
                .foregroundColor(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(SampleColors.accent)
                .cornerRadius(6)
        }
    }
}

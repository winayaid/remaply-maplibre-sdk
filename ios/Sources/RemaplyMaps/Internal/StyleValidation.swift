import Foundation

/// Validation rules for ``MapStyle`` from API_SPEC.md §5.4. Port of the Android version.
enum StyleValidation {
    static func problem(_ style: MapStyle) -> (MapErrorCode, String)? {
        switch style {
        case .url(let url):
            return urlProblem(url).map { (.invalidArgument, $0) }
        case .bundled(let path):
            return bundledProblem(path).map { (.invalidArgument, $0) }
        }
    }

    private static func urlProblem(_ url: String) -> String? {
        let trimmed = url.trimmingCharacters(in: .whitespaces)
        // URLComponents rejects strings with spaces and other invalid characters, like java.net.URI.
        guard let components = URLComponents(string: trimmed), !trimmed.isEmpty else {
            return "Style URL is not a valid URL: '\(url)'"
        }
        guard let scheme = components.scheme?.lowercased() else {
            return "Style URL must be absolute: '\(url)'"
        }
        guard scheme == "http" || scheme == "https" else {
            return "Style URL must use http or https: '\(url)'"
        }
        guard let host = components.host, !host.isEmpty else {
            return "Style URL has no host: '\(url)'"
        }
        return nil
    }

    private static func bundledProblem(_ path: String) -> String? {
        if path.trimmingCharacters(in: .whitespaces).isEmpty {
            return "Bundled style path is empty"
        }
        if path.split(whereSeparator: { $0 == "/" || $0 == "\\" }).contains("..") {
            return "Bundled style path must not contain '..': '\(path)'"
        }
        return nil
    }
}

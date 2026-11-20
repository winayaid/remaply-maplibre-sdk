import MapLibre
import UIKit

/// Native camera callbacks, animation frames, and taps for ``MapSession``.
extension MapSession {
    nonisolated func mapView(_ mapView: MLNMapView, regionIsChangingWith reason: MLNCameraChangeReason) {
        MainActor.assumeIsolated { onNativeCamera(idle: false) }
    }

    nonisolated func mapView(_ mapView: MLNMapView, regionDidChangeWith reason: MLNCameraChangeReason, animated: Bool) {
        MainActor.assumeIsolated { onNativeCamera(idle: true) }
    }

    func onNativeCamera(idle: Bool) {
        // Only user gestures are reported from native callbacks; see the class comment.
        guard isReady, programmaticDepth == 0, running == nil else { return }
        if idle {
            gate.idle(currentCamera(), reason: .userGesture)
        } else {
            gate.change(currentCamera(), reason: .userGesture)
        }
    }

    func onAnimationFrame(_ timestamp: CFTimeInterval) {
        guard var run = running, isReady else { return }
        if run.startTime == nil {
            run.startTime = timestamp
            running = run
        }
        let elapsedMs = Int(((timestamp - (run.startTime ?? timestamp)) * 1000).rounded())
        moveNative(run.animation.value(atElapsedMs: elapsedMs))
        if run.animation.isFinished(elapsedMs: elapsedMs) {
            running = nil
            frameDriver?.stop()
            frameDriver = nil
            gate.idle(currentCamera(), reason: .programmatic)
            if let completion = run.completion { controller.events.post { completion(true) } }
        } else {
            gate.change(currentCamera(), reason: .programmatic)
        }
    }

    // MARK: - Taps (API_SPEC.md §8.2)

    func installTapRecognizer(on view: MLNMapView) {
        let tap = UITapGestureRecognizer(target: self, action: #selector(handleTap(_:)))
        tap.delegate = self
        // A confirmed single tap: wait until the map's own double-tap recognizer fails.
        for case let other as UITapGestureRecognizer in view.gestureRecognizers ?? []
            where other.numberOfTapsRequired == 2 && other.numberOfTouchesRequired == 1 {
            tap.require(toFail: other)
        }
        view.addGestureRecognizer(tap)
        tapRecognizer = tap
    }

    nonisolated func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        true
    }

    @objc private func handleTap(_ recognizer: UITapGestureRecognizer) {
        guard recognizer.state == .ended, let mapView, isReady else { return }
        let point = recognizer.location(in: mapView)
        let radius = Self.tapRadius
        let rect = CGRect(x: point.x - radius, y: point.y - radius, width: radius * 2, height: radius * 2)
        let hits = mapView.visibleFeatures(in: rect, styleLayerIdentifiers: [MarkerLayer.layerID])
        let ids = hits.compactMap { $0.attribute(forKey: MarkerLayer.propID) as? String }
        SdkLog.debug("Tap at \(point) hits=\(ids)")
        if let marker = controller.topmostMarker(ids) {
            postEvent { $0.sessionMarkerTap(marker) }
        } else {
            let tapped = mapView.convert(point, toCoordinateFrom: mapView)
            let coordinate = GeoCoordinate(latitude: tapped.latitude,
                                           longitude: CameraMath.wrapLongitude(tapped.longitude))
            postEvent { $0.sessionMapTap(coordinate) }
        }
    }
}

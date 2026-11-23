import UIKit

/// The SDK's default marker pin, 28 × 40 pt, anchored at the bottom tip.
/// Draws the same path data as Android's `remaply_default_pin.xml` (keep both in sync).
enum DefaultPin {
    static let size = CGSize(width: 28, height: 40)

    static func makeImage() -> UIImage {
        UIGraphicsImageRenderer(size: size).image { _ in
            let pin = UIBezierPath()
            pin.move(to: CGPoint(x: 14, y: 1))
            pin.addCurve(to: CGPoint(x: 1, y: 14), controlPoint1: CGPoint(x: 6.8, y: 1), controlPoint2: CGPoint(x: 1, y: 6.8))
            pin.addCurve(to: CGPoint(x: 14, y: 39), controlPoint1: CGPoint(x: 1, y: 23.5), controlPoint2: CGPoint(x: 14, y: 39))
            pin.addCurve(to: CGPoint(x: 27, y: 14), controlPoint1: CGPoint(x: 14, y: 39), controlPoint2: CGPoint(x: 27, y: 23.5))
            pin.addCurve(to: CGPoint(x: 14, y: 1), controlPoint1: CGPoint(x: 27, y: 6.8), controlPoint2: CGPoint(x: 21.2, y: 1))
            pin.close()
            UIColor(red: 0xE5 / 255.0, green: 0x39 / 255.0, blue: 0x35 / 255.0, alpha: 1).setFill()
            pin.fill()
            UIColor.white.setStroke()
            pin.lineWidth = 2
            pin.stroke()

            UIColor.white.setFill()
            UIBezierPath(ovalIn: CGRect(x: 9, y: 9, width: 10, height: 10)).fill()
        }
    }
}

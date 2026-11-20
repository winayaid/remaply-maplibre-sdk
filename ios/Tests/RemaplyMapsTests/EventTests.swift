@testable import RemaplyMaps
import XCTest

@MainActor
final class CameraEventGateTests: XCTestCase {
    private var events: [String] = []
    private lazy var gate = CameraEventGate(
        emitChange: { [weak self] camera, reason in self?.events.append("change(\(camera.zoom),\(reason))") },
        emitIdle: { [weak self] camera in self?.events.append("idle(\(camera.zoom))") }
    )

    private func cam(_ zoom: Double) -> CameraPosition { CameraPosition(center: coord(0, 0), zoom: zoom) }

    func testNothingIsEmittedBeforeEnable() {
        gate.change(cam(2), reason: .userGesture)
        gate.idle(cam(2), reason: .userGesture)
        XCTAssertEqual(events, [])
    }

    func testMovementIsChangesThenOneIdle() {
        gate.enable(current: cam(1))
        gate.change(cam(2), reason: .userGesture)
        gate.change(cam(3), reason: .userGesture)
        gate.idle(cam(3), reason: .userGesture)
        XCTAssertEqual(events, ["change(2.0,userGesture)", "change(3.0,userGesture)", "idle(3.0)"])
    }

    func testDuplicatePositionsAreDropped() {
        gate.enable(current: cam(1))
        gate.change(cam(1), reason: .userGesture)
        gate.change(cam(2), reason: .userGesture)
        gate.change(cam(2), reason: .userGesture)
        gate.idle(cam(2), reason: .userGesture)
        gate.idle(cam(2), reason: .userGesture)
        XCTAssertEqual(events, ["change(2.0,userGesture)", "idle(2.0)"])
    }

    func testJumpEmitsExactlyOneChangeAndIdle() {
        gate.enable(current: cam(1))
        gate.idle(cam(5), reason: .programmatic)
        XCTAssertEqual(events, ["change(5.0,programmatic)", "idle(5.0)"])
    }

    func testJumpToSamePositionEmitsNothing() {
        gate.enable(current: cam(1))
        gate.idle(cam(1), reason: .programmatic)
        XCTAssertEqual(events, [])
    }

    func testDisableStopsEvents() {
        gate.enable(current: cam(1))
        gate.disable()
        gate.idle(cam(4), reason: .programmatic)
        XCTAssertEqual(events, [])
    }
}

@MainActor
final class EventQueueTests: XCTestCase {
    func testEventsRunImmediatelyWhenIdle() {
        let queue = EventQueue()
        var log: [String] = []
        queue.post { log.append("a") }
        XCTAssertEqual(log, ["a"])
    }

    func testEventsPostedInsideACallbackRunAfterItReturns() {
        let queue = EventQueue()
        var log: [String] = []
        queue.post {
            log.append("outer-start")
            queue.post { log.append("inner") }
            log.append("outer-end")
        }
        XCTAssertEqual(log, ["outer-start", "outer-end", "inner"])
    }
}

@MainActor
final class LifecycleStateMachineTests: XCTestCase, LifecycleTarget {
    private var calls: [String] = []
    private lazy var machine = LifecycleStateMachine(target: self)

    func performCreate() { calls.append("create") }
    func performStart() { calls.append("start") }
    func performResume() { calls.append("resume") }
    func performPause() { calls.append("pause") }
    func performStop() { calls.append("stop") }
    func performDestroy() { calls.append("destroy") }

    func testSkippedStepsArePerformedInOrder() {
        machine.moveUp(to: .resumed)
        XCTAssertEqual(calls, ["create", "start", "resume"])
        XCTAssertEqual(machine.state, .resumed)
    }

    func testRepeatedAndLowerUpCallsAreIgnored() {
        machine.moveUp(to: .resumed)
        calls.removeAll()
        machine.moveUp(to: .resumed)
        machine.moveUp(to: .started)
        machine.moveUp(to: .created)
        XCTAssertEqual(calls, [])
    }

    func testDownCallsStepDownAndIgnoreLowerStates() {
        machine.moveUp(to: .resumed)
        calls.removeAll()
        machine.moveDown(to: .created)
        XCTAssertEqual(calls, ["pause", "stop"])
        machine.moveDown(to: .started)
        XCTAssertEqual(machine.state, .created)
        XCTAssertEqual(calls, ["pause", "stop"])
    }

    func testDownCallsBeforeCreateDoNothing() {
        machine.moveDown(to: .created)
        XCTAssertEqual(machine.state, .initialized)
        XCTAssertEqual(calls, [])
    }

    func testDestroyFromResumedStepsDownThenDestroys() {
        machine.moveUp(to: .resumed)
        calls.removeAll()
        machine.destroy()
        XCTAssertEqual(calls, ["pause", "stop", "destroy"])
        XCTAssertEqual(machine.state, .destroyed)
    }

    func testDestroyBeforeCreateReleasesNothing() {
        machine.destroy()
        XCTAssertEqual(calls, [])
        XCTAssertEqual(machine.state, .destroyed)
    }

    func testEveryCallAfterDestroyIsIgnored() {
        machine.moveUp(to: .started)
        machine.destroy()
        calls.removeAll()
        machine.moveUp(to: .resumed)
        machine.moveDown(to: .created)
        machine.destroy()
        XCTAssertEqual(calls, [])
        XCTAssertEqual(machine.state, .destroyed)
    }
}

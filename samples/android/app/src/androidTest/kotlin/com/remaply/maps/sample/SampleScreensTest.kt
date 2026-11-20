package com.remaply.maps.sample

import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opens every sample from the launcher and exercises it (Phase 6: "every sample runs").
 * iOS runs the same checks in `SampleScreensUITests`.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SampleScreensTest {
    private val launchIntent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            // Bundled style: the style JSON is local, so the screens do not depend on the network to load.
            .putExtra("sampleStyleAsset", "styles/demo-bundled.json")

    private val scenarioRule = ActivityScenarioRule<MainActivity>(launchIntent)

    @get:Rule
    val compose =
        AndroidComposeTestRule(scenarioRule) { rule ->
            var activity: MainActivity? = null
            rule.scenario.onActivity { activity = it }
            checkNotNull(activity)
        }

    @Test
    fun basicMap() {
        open("Basic map")
        waitForText("Map ready.")
    }

    @Test
    fun markersAndTaps() {
        open("Markers and taps")
        waitForText("Tap the map to add a marker.")
        tapMap(0.3f, 0.7f)
        waitForText("Added m1 ")
        tapMap(0.7f, 0.75f)
        waitForText("Added m2 ")
        waitForText("2 markers")
        compose.onNode(hasText("Clear")).performClick()
        waitForText("All markers removed.")
    }

    @Test
    fun cameraAnimation() {
        open("Camera animation")
        waitForText("Pick a destination.")
        compose.onNode(hasText("Tokyo")).performClick()
        waitForText("Arrived at Tokyo")
        compose.onNode(hasText("Rotate 90°")).performClick()
        waitForText("Rotated to 90°")
        compose.onNode(hasText("Jump to world")).performClick()
        waitForText("Jumped")
    }

    @Test
    fun composeExample() {
        open("Compose example")
        waitForText("Map ready · 3 markers")
        compose.onNode(hasText("Animate")).performClick()
        waitForText("Animation finished: true")
    }

    @Test
    fun classicViewExample() {
        open("Classic View example")
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val pkg = "com.remaply.maps.sample"
        assertTrue(
            "View sample did not become ready",
            device.wait(Until.hasObject(By.res(pkg, "status").textStartsWith("Map ready")), TIMEOUT_MS),
        )
        val animate = device.findObject(By.res(pkg, "animate"))
        assertNotNull(animate)
        animate.click()
        assertTrue(
            "View sample animation did not finish",
            device.wait(Until.hasObject(By.res(pkg, "status").textStartsWith("Animation finished: true")), TIMEOUT_MS),
        )
    }

    private fun open(sample: String) {
        compose.onNode(hasText(sample)).performClick()
    }

    private fun tapMap(
        fx: Float,
        fy: Float,
    ) {
        compose.onRoot().performTouchInput { click(Offset(width * fx, height * fy)) }
    }

    private fun waitForText(fragment: String) {
        compose.waitUntilAtLeastOneExists(hasText(fragment, substring = true), TIMEOUT_MS)
    }

    private companion object {
        const val TIMEOUT_MS = 20_000L
    }
}

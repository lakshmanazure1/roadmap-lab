package com.lakshman.roadmaplab.screens

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lakshman.roadmaplab.MainActivity
import com.lakshman.roadmaplab.RoadmapLabApp
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Pictures of the app as it is installed today (v1.1.1), for the audit. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h851dp-xxhdpi")
class CurrentAppScreensTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun captureCurrentScreens() {
        val app = ApplicationProvider.getApplicationContext<RoadmapLabApp>()
        runBlocking {
            app.repository.importBundledIfEmpty()
            app.repository.completeOnboarding(emptySet()) { it }
        }
        ActivityScenario.launch(MainActivity::class.java).use { sc ->
            runCatching {
                compose.waitUntil(20_000) { compose.onAllNodesWithText("Loading the plan…").fetchSemanticsNodes().isEmpty() }
            }
            compose.waitForIdle()
            Thread.sleep(500)
            compose.waitForIdle()
            sc.onActivity { Shots.save(it, "v1_1_path") }
            for ((tab, name) in listOf("Progress" to "v1_2_progress", "Journal" to "v1_3_journal")) {
                runCatching {
                    compose.onAllNodesWithText(tab).onLast().performClick()
                    compose.waitForIdle()
                }
                sc.onActivity { Shots.save(it, name) }
            }
        }
    }
}

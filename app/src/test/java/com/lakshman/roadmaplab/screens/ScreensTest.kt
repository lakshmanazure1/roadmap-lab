package com.lakshman.roadmaplab.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lakshman.roadmaplab.TestData
import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.data.LabState
import com.lakshman.roadmaplab.ui.AppRoot
import com.lakshman.roadmaplab.ui.AreaScreen
import com.lakshman.roadmaplab.ui.BackupHooks
import com.lakshman.roadmaplab.ui.CheckScreen
import com.lakshman.roadmaplab.ui.EvidenceScreen
import com.lakshman.roadmaplab.ui.HowItWorksScreen
import com.lakshman.roadmaplab.ui.IntroScreen
import com.lakshman.roadmaplab.ui.LogScreen
import com.lakshman.roadmaplab.ui.Nav
import com.lakshman.roadmaplab.ui.PlanScreen
import com.lakshman.roadmaplab.ui.PracticeScreen
import com.lakshman.roadmaplab.ui.ProgressScreen
import com.lakshman.roadmaplab.ui.Screen
import com.lakshman.roadmaplab.ui.SettingsScreen
import com.lakshman.roadmaplab.ui.TaskScreen
import com.lakshman.roadmaplab.ui.TodayScreen
import com.lakshman.roadmaplab.ui.UiState
import com.lakshman.roadmaplab.ui.WeeklyReviewScreen
import com.lakshman.roadmaplab.ui.derive
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.RoadmapLabTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Draws every screen with the real code and saves pictures (app/build/screens) so the design can be checked. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h851dp-xxhdpi")
class ScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val nav = Nav({}, {}, {})
    private val mid = d("2026-12-10") // Thursday of week 4: user training #1 day

    private fun shot(name: String, s: LabState, today: Long, content: @Composable (UiState) -> Unit) {
        val ui = derive(s, today)!!
        compose.setContent {
            RoadmapLabTheme(s.settings.theme) {
                Box(Modifier.fillMaxSize().background(LocalPalette.current.bg)) { content(ui) }
            }
        }
        compose.waitForIdle()
        Shots.save(compose.activity, name)
    }

    @Test fun a01_todayDay1() = shot("01_today_day1", DemoData.empty(), d("2026-11-16")) { TodayScreen(it, NoActions, nav) }
    @Test fun a02_todayMidPlan() = shot("02_today_mid", DemoData.midPlan(mid), mid) { TodayScreen(it, NoActions, nav) }
    @Test fun a03_todayDark() = shot("03_today_dark", DemoData.midPlan(mid, "dark"), mid) { TodayScreen(it, NoActions, nav) }
    @Test fun a04_appWithNav() = shot("04_app_nav", DemoData.midPlan(mid), mid) { AppRoot(it, NoActions, null, BackupHooks()) }
    @Test fun a05_todayBeforeStart() = shot("05_today_before_start", DemoData.empty(), d("2026-11-14")) { TodayScreen(it, NoActions, nav) }
    @Test fun a06_todayReduced() = shot("06_today_reduced", DemoData.midPlan(d("2026-12-09"), reducedToday = true), d("2026-12-09")) { TodayScreen(it, NoActions, nav) }
    @Test fun a07_todaySaturday() = shot("07_today_saturday", DemoData.midPlan(d("2026-12-12")), d("2026-12-12")) { TodayScreen(it, NoActions, nav) }
    /** After the update: old records exist and the one-time "Start fresh from today?" card shows (Day 1). */
    @Test fun a09_freshStartOffer() = shot("09_today_fresh_start_offer",
        DemoData.empty().let { it.copy(legacyTaskLogs = 12, settings = it.settings.copy(freshStartAsked = false)) }, d("2026-11-16")) { TodayScreen(it, NoActions, nav) }
    /** Day 1 after starting fresh: accounting and the Python recap side by side. */
    @Test fun a10_freshDay1() = shot("10_today_fresh_day1",
        DemoData.empty().let { it.copy(settings = it.settings.copy(freshStartDay = d("2026-11-16"), freshStartAsked = true)) }, d("2026-11-16")) { TodayScreen(it, NoActions, nav) }
    @Test fun a11_settingsFresh() = shot("11_settings_fresh",
        DemoData.empty().let { it.copy(settings = it.settings.copy(freshStartDay = d("2026-11-16"), freshStartAsked = true)) }, d("2026-11-16")) {
            SettingsScreen(it, NoActions, nav, BackupHooks(latestCopy = "Mon 16 Nov, before your fresh start", safetyCopies = 1)) }
    @Test fun a08_restDay() = shot("08_today_rest", DemoData.midPlan(d("2026-12-25")), d("2026-12-25")) { TodayScreen(it, NoActions, nav) }

    @Test fun b01_progress() = shot("11_progress", DemoData.midPlan(mid), mid) { ProgressScreen(it, nav) }
    @Test fun b02_progressDark() = shot("12_progress_dark", DemoData.midPlan(mid, "dark"), mid) { ProgressScreen(it, nav) }
    @Test fun b03_area() = shot("13_area_accounting", DemoData.midPlan(mid), mid) { AreaScreen(it, nav, "accounting") }
    @Test fun b04_plan() = shot("14_plan_week4", DemoData.midPlan(mid), mid) { PlanScreen(it, NoActions, nav) }
    @Test fun b05_log() = shot("15_log", DemoData.midPlan(mid), mid) { LogScreen(it, NoActions, nav, BackupHooks()) }

    @Test fun c01_task() = shot("21_task_T126", DemoData.midPlan(mid), mid) { TaskScreen(it, NoActions, nav, "T126") }
    @Test fun c02_taskAccounting() = shot("22_task_T109", DemoData.midPlan(mid), mid) { TaskScreen(it, NoActions, nav, "T109") }
    @Test fun c03_quiz() = shot("23_check_quiz_T109", DemoData.midPlan(mid), mid) { CheckScreen(it, NoActions, nav, "T109", 0) }
    @Test fun c04_journal() = shot("24_check_journal_T164", DemoData.midPlan(mid), mid) { CheckScreen(it, NoActions, nav, "T164", 0) }
    @Test fun c05_french() = shot("25_check_french_T130", DemoData.midPlan(mid), mid) { CheckScreen(it, NoActions, nav, "T130", 0) }
    @Test fun c06_rubric() = shot("26_check_rubric_T139", DemoData.midPlan(mid), mid) { CheckScreen(it, NoActions, nav, "T139", 0) }
    @Test fun c07_translate() = shot("27_check_translate_T120", DemoData.midPlan(mid), mid) { CheckScreen(it, NoActions, nav, "T120", 0) }

    @Test fun d01_weekly() = shot("31_weekly_week3", DemoData.midPlan(mid), mid) { WeeklyReviewScreen(it, NoActions, nav, 3) }
    @Test fun d02_evidence() = shot("32_evidence_new", DemoData.midPlan(mid), mid) { EvidenceScreen(it, NoActions, nav, Screen.EvidenceEdit(null, mid, null, false)) }
    @Test fun d03_star() = shot("33_star_story", DemoData.midPlan(mid), mid) { s ->
        val story = s.s.evidence.first { it.isStory }
        EvidenceScreen(s, NoActions, nav, Screen.EvidenceEdit(story.id, story.day, "T077", true))
    }
    @Test fun d04_practice() = shot("34_practice", DemoData.midPlan(mid), mid) { PracticeScreen(it, NoActions, nav) }
    @Test fun d05_settings() = shot("35_settings", DemoData.midPlan(mid), mid) { SettingsScreen(it, NoActions, nav, BackupHooks()) }
    @Test fun d06_how() = shot("36_how_it_works", DemoData.midPlan(mid), mid) { HowItWorksScreen(it, nav) }
    @Test fun d07_intro() = shot("37_intro", DemoData.empty().let { it.copy(settings = it.settings.copy(v2Intro = false)) }, d("2026-11-14")) { IntroScreen(it, NoActions) }

    @Test
    fun planHasEveryCsvRow() {
        assertTrue(TestData.plan.csvTasks.size == 380 && TestData.plan.tasks.size == 387)
    }
}

/** The same screens on a narrow phone and as tall pages (to check everything below the fold). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h1800dp-xhdpi")
class NarrowScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val nav = Nav({}, {}, {})
    private val mid = d("2026-12-10")

    private fun shot(name: String, s: LabState, today: Long, content: @Composable (UiState) -> Unit) {
        val ui = derive(s, today)!!
        compose.setContent { RoadmapLabTheme(s.settings.theme) { Box(Modifier.fillMaxSize().background(LocalPalette.current.bg)) { content(ui) } } }
        compose.waitForIdle()
        Shots.save(compose.activity, name)
    }

    @Test fun n1_today() = shot("41_narrow_today", DemoData.midPlan(mid), mid) { TodayScreen(it, NoActions, nav) }
    @Test fun n2_progress() = shot("42_narrow_progress", DemoData.midPlan(mid), mid) { ProgressScreen(it, nav) }
    @Test fun n3_weekly() = shot("43_narrow_weekly", DemoData.midPlan(mid), mid) { WeeklyReviewScreen(it, NoActions, nav, 3) }
    @Test fun n4_task() = shot("44_narrow_task", DemoData.midPlan(mid), mid) { TaskScreen(it, NoActions, nav, "T164") }
    @Test fun n5_settings() = shot("45_narrow_settings", DemoData.midPlan(mid), mid) { SettingsScreen(it, NoActions, nav, BackupHooks()) }
    @Test fun n6_freshDay1() = shot("46_narrow_fresh_day1",
        DemoData.empty().let { it.copy(settings = it.settings.copy(freshStartDay = d("2026-11-16"), freshStartAsked = true)) }, d("2026-11-16")) {
            TodayScreen(it, NoActions, nav) }
    @Test fun n7_freshOffer() = shot("47_narrow_fresh_offer",
        DemoData.empty().let { it.copy(legacyTaskLogs = 12, settings = it.settings.copy(freshStartAsked = false)) }, d("2026-11-16")) {
            TodayScreen(it, NoActions, nav) }
    @Test fun n8_settingsFresh() = shot("48_narrow_settings_fresh",
        DemoData.empty().let { it.copy(settings = it.settings.copy(freshStartDay = d("2026-11-16"), freshStartAsked = true)) }, d("2026-11-16")) {
            SettingsScreen(it, NoActions, nav, BackupHooks(latestCopy = "Mon 16 Nov, before your fresh start", safetyCopies = 1)) }
}

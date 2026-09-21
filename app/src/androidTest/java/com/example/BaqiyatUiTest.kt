package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaqiyatUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun googleSignInButton_isVisibleAndEnabled() {
        finishOnboardingIfNeeded()
        composeRule.onNodeWithText("الإعدادات").performClick()
        composeRule.onNodeWithTag("google-sign-in-button")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun morningDhikrCard_isVisibleAndRespondsToTap() {
        finishOnboardingIfNeeded()
        composeRule.onNodeWithText("الصباح").performClick()
        composeRule.onNodeWithTag("dhikr-card").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("dhikr-counter").assertIsDisplayed()
    }

    private fun finishOnboardingIfNeeded() {
        try {
            composeRule.onNodeWithText("التالي").performClick()
            composeRule.onNodeWithText("التالي").performClick()
            composeRule.onNodeWithText("ابدأ الآن").performClick()
        } catch (_: AssertionError) {
            // The app may already have completed onboarding in a reused test installation.
        }
        composeRule.onNodeWithText("وردك اليومي").assertIsDisplayed()
    }
}

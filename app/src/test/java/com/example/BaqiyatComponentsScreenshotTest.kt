package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.components.BaqiyatBackground
import com.example.ui.components.BaqiyatButton
import com.example.ui.components.BaqiyatButtonStyle
import com.example.ui.components.BaqiyatChip
import com.example.ui.components.BaqiyatNavItem
import com.example.ui.components.FloatingNavBar
import com.example.ui.components.GlassCard
import com.example.ui.components.MihrabCard
import com.example.ui.components.ProgressRing
import com.example.ui.theme.BaqiyatTheme
import com.example.ui.theme.TimeMode
import com.example.ui.theme.ZekrTextStyle
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** لقطات مرجعية للمكوّنات في الأوضاع الأربعة (تشغيل: ./gradlew recordRoborazziDebug). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class BaqiyatComponentsScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun shoot(mode: TimeMode) {
        composeTestRule.setContent {
            BaqiyatTheme(mode) {
                BaqiyatBackground {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        MihrabCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(start = 26.dp, end = 26.dp, top = 92.dp, bottom = 24.dp)) {
                                Text("رَضِيتُ بِاللَّهِ رَبًّا", style = ZekrTextStyle)
                                ProgressRing(progress = 0.66f) { Text("1") }
                            }
                        }
                        GlassCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            Text("بطاقة زجاجية")
                            BaqiyatChip("شكر", selected = true)
                        }
                        BaqiyatButton("ابدأ الورد", onClick = {})
                        BaqiyatButton("مفضلة", onClick = {}, style = BaqiyatButtonStyle.Glass)
                        FloatingNavBar(
                            items = listOf(BaqiyatNavItem("الرئيسية", Icons.Default.Home), BaqiyatNavItem("الإعدادات", Icons.Default.Settings)),
                            selectedIndex = 0,
                            onSelect = {}
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/components_${mode.name.lowercase()}.png")
    }

    @Test fun day() = shoot(TimeMode.DAY)
    @Test fun fajr() = shoot(TimeMode.FAJR)
    @Test fun maghrib() = shoot(TimeMode.MAGHRIB)
    @Test fun night() = shoot(TimeMode.NIGHT)
}

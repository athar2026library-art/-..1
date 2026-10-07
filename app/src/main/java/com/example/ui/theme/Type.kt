package com.example.ui.theme

import androidx.compose.material3.Typography as M3Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/** خط الذكر: Amiri (مضمَّن في التطبيق ليعمل دون اتصال). */
val AmiriFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold)
)

/** خط الواجهة: IBM Plex Sans Arabic. */
val PlexArabicFamily = FontFamily(
    Font(R.font.plex_arabic_regular, FontWeight.Normal),
    Font(R.font.plex_arabic_medium, FontWeight.Medium),
    Font(R.font.plex_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.plex_arabic_semibold, FontWeight.Bold)
)

/** نمط نص الذكر: تباعد أسطر 1.95 من الحجم كي لا تتزاحم حركات التشكيل. */
val ZekrTextStyle = TextStyle(
    fontFamily = AmiriFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 28.sp,
    lineHeight = 54.sp
)

private val Base = M3Typography()

val Typography = Base.copy(
    displayLarge = Base.displayLarge.copy(fontFamily = PlexArabicFamily),
    displayMedium = Base.displayMedium.copy(fontFamily = PlexArabicFamily),
    displaySmall = TextStyle(fontFamily = PlexArabicFamily, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 42.sp),
    headlineLarge = Base.headlineLarge.copy(fontFamily = PlexArabicFamily),
    headlineMedium = Base.headlineMedium.copy(fontFamily = PlexArabicFamily),
    headlineSmall = TextStyle(fontFamily = PlexArabicFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = PlexArabicFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 32.sp),
    titleMedium = TextStyle(fontFamily = PlexArabicFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 28.sp),
    titleSmall = Base.titleSmall.copy(fontFamily = PlexArabicFamily),
    bodyLarge = TextStyle(fontFamily = PlexArabicFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 29.sp),
    bodyMedium = TextStyle(fontFamily = PlexArabicFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 25.sp),
    bodySmall = TextStyle(fontFamily = PlexArabicFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = PlexArabicFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = PlexArabicFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = PlexArabicFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)

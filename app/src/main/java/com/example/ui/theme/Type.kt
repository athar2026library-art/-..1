package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val cairoFamily = FontFamily(
    Font(googleFont = GoogleFont("Cairo"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Cairo"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Cairo"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Cairo"), fontProvider = provider, weight = FontWeight.Bold)
)

val Typography = Typography(
    displaySmall = TextStyle(fontFamily = cairoFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 40.sp),
    headlineSmall = TextStyle(fontFamily = cairoFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = cairoFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = cairoFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 25.sp),
    bodyLarge = TextStyle(fontFamily = cairoFamily, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 31.sp),
    bodyMedium = TextStyle(fontFamily = cairoFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontFamily = cairoFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
)

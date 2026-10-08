package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AirlineSeatFlat
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

/** مفاتيح الأيقونات المسموحة في قواعد Firestore ولوحة الإدارة. المجهول يرجع نجمة. */
fun categoryIcon(key: String): ImageVector = when (key) {
    "sun" -> Icons.Default.WbSunny
    "moon" -> Icons.Default.NightsStay
    "bed" -> Icons.Default.AirlineSeatFlat
    "car" -> Icons.Default.DirectionsCar
    "heart" -> Icons.Default.Favorite
    "book" -> Icons.Default.Book
    "home" -> Icons.Default.Home
    "food" -> Icons.Default.Restaurant
    "rain" -> Icons.Default.Umbrella
    "shield" -> Icons.Default.Shield
    "mosque" -> Icons.Default.AccountBalance
    else -> Icons.Default.Star
}

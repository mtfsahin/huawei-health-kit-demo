package com.example.healthkitdemo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

enum class Metric(
    private val light: Color,
    private val lightContainer: Color,
    private val dark: Color,
    private val darkContainer: Color,
) {
    Steps(Color(0xFF006B5F), Color(0xFF8DF5E4), Color(0xFF70D8C8), Color(0xFF005048)),
    Calories(Color(0xFF964900), Color(0xFFFFDCC6), Color(0xFFFFB786), Color(0xFF723600)),
    HeartRate(Color(0xFFBB171C), Color(0xFFFFDAD6), Color(0xFFFFB4AC), Color(0xFF93000D)),
    Sleep(Color(0xFF4858AB), Color(0xFFDEE0FF), Color(0xFFBAC3FF), Color(0xFF2F3F92)),
    Stress(Color(0xFF835400), Color(0xFFFFDDB5), Color(0xFFFFB957), Color(0xFF643F00)),
    Distance(Color(0xFF0060A8), Color(0xFFD3E4FF), Color(0xFFA2C9FF), Color(0xFF004881)),
    Workout(Color(0xFF942CB0), Color(0xFFFDD6FF), Color(0xFFF3AEFF), Color(0xFF790096));

    val color: Color
        @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) dark else light

    val container: Color
        @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) darkContainer else lightContainer
}

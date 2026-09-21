package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = CalcOrangePrimary,
  onPrimary = CalcOrangeOnPrimary,
  primaryContainer = CalcOrangeContainer,
  onPrimaryContainer = CalcOrangeOnContainer,
  secondary = FunctionKeyOnDark,
  onSecondary = Color(0xFF082F49),
  secondaryContainer = FunctionKeyDark,
  onSecondaryContainer = Color(0xFFE2E8F0),
  background = DarkBackground,
  onBackground = DarkOnSurface,
  surface = DarkBackground,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  surfaceContainer = DarkSurfaceContainer,
  surfaceContainerHigh = DarkSurfaceContainerHigh
)

private val LightColorScheme = lightColorScheme(
  primary = LightOrangePrimary,
  onPrimary = Color.White,
  primaryContainer = LightOrangeContainer,
  onPrimaryContainer = LightOrangeOnContainer,
  secondary = FunctionKeyOnLight,
  onSecondary = Color.White,
  secondaryContainer = FunctionKeyLight,
  onSecondaryContainer = Color(0xFF0F172A),
  background = LightBackground,
  onBackground = LightOnSurface,
  surface = LightBackground,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceVariant,
  surfaceContainer = LightSurfaceContainer,
  surfaceContainerHigh = LightSurfaceContainerHigh
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our handcrafted calculator palette by default for distinctive look
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

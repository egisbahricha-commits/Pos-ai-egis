package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

val LocalIsDarkTheme = staticCompositionLocalOf { false }

// Fixed Light & Dark Token Helpers
internal val LightBgCanvas = Color(0xFFF3F5F8)
internal val DarkBgCanvas = Color(0xFF0B1120)

internal val LightSurfaceWhite = Color(0xFFFFFFFF)
internal val DarkSurfaceWhite = Color(0xFF151F32)

internal val LightSurfaceStrong = Color(0xFFF1F5F9)
internal val DarkSurfaceStrong = Color(0xFF1E293B)

internal val LightBorderDefault = Color(0xFFE2E8F0)
internal val DarkBorderDefault = Color(0xFF283548)

internal val LightBorderInput = Color(0xFFCBD5E1)
internal val DarkBorderInput = Color(0xFF334155)

// Theme-Adaptive Surface & Neutral Tokens
val BgCanvas: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkBgCanvas else LightBgCanvas

val SurfaceWhite: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkSurfaceWhite else LightSurfaceWhite

val SurfaceStrong: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkSurfaceStrong else LightSurfaceStrong

val BorderDefault: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkBorderDefault else LightBorderDefault

val BorderInput: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkBorderInput else LightBorderInput

val DisabledBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E293B) else Color(0xFFE2E8F0)

val DisabledText: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF64748B) else Color(0xFF94A3B8)

val ChipActive: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2563EB) else Color(0xFF1E293B)

// Primary Royal Blue Accent
val AccentBlue: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF60A5FA) else Color(0xFF1D4ED8)

val AccentBlueSoft: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E3A5F) else Color(0xFFDBEAFE)

// Sapphire Blue Palette
val Sapphire900 = Color(0xFF0F172A)
val Sapphire800: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
val Sapphire700 = Color(0xFF1E40AF)
val Sapphire600 = Color(0xFF2563EB)
val Sapphire500 = Color(0xFF3B82F6)
val Sapphire200: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E3A8A) else Color(0xFFBFDBFE)
val Sapphire100 = Color(0xFFDBEAFE)
val Sapphire50: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF172554) else Color(0xFFEFF6FF)

// Emerald Green Palette (Success / Laba / Kasir)
val Emerald900 = Color(0xFF064E3B)
val Emerald800 = Color(0xFF065F46)
val Emerald700: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF34D399) else Color(0xFF15803D)
val Emerald600 = Color(0xFF16A34A)
val Emerald500 = Color(0xFF10B981)
val Emerald200: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF065F46) else Color(0xFFA7F3D0)
val Emerald100: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF064E3B) else Color(0xFFD1FAE5)
val Emerald50: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF062C22) else Color(0xFFECFDF5)

// Semantic Accents
val Amber500 = Color(0xFFD97706)
val Amber400 = Color(0xFFF59E0B)
val AmberWarningBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF451A03) else Color(0xFFFEF3C7)
val AmberWarningBorder = Color(0xFFF59E0B)
val Rose500: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF87171) else Color(0xFFDC2626)
val Rose100: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF450A0A) else Color(0xFFFEE2E2)

// Theme-Adaptive Slate Neutral Scale (flips cleanly in Dark Mode)
val Slate950 = Color(0xFF020617)
val Slate900: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFF8FAFC) else Color(0xFF0F172A)
val Slate800: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFE2E8F0) else Color(0xFF1E293B)
val Slate700: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFCBD5E1) else Color(0xFF334155)
val Slate600: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF94A3B8) else Color(0xFF475569)
val Slate500: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF94A3B8) else Color(0xFF64748B)
val Slate400: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF64748B) else Color(0xFF94A3B8)
val Slate300: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF475569) else Color(0xFFCBD5E1)
val Slate200: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF334155) else Color(0xFFE2E8F0)
val Slate100: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E293B) else Color(0xFFF1F5F9)
val Slate50: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E293B) else Color(0xFFF8FAFC)

// Module Colors
val ModuleBeranda = Color(0xFF334155)
val ModuleKasir = Color(0xFF0F766E)
val ModuleProduk = Color(0xFFB45309)
val ModuleLaporan = Color(0xFF6D28D9)
val ModuleKas = Color(0xFF0E7490)
val ModulePengaturan = Color(0xFF1E293B)

// Legacy Module Header Aliases
val HeaderRed = ModuleBeranda
val HeaderGreen = ModuleKasir
val HeaderOrange = ModuleProduk
val HeaderPurple = ModuleLaporan
val HeaderBlue = Color(0xFF1D4ED8)
val HeaderTeal = ModuleKas

// Tabular numerals helper for currency & numbers
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

package com.duck.twominute.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

data class IdentityGlyph(val key: String, val vector: ImageVector)

/** The icon library offered in the identity editor. */
val identityGlyphs: List<IdentityGlyph> = listOf(
    IdentityGlyph("spark", Icons.Rounded.AutoAwesome),
    IdentityGlyph("book", Icons.Rounded.MenuBook),
    IdentityGlyph("run", Icons.Rounded.DirectionsRun),
    IdentityGlyph("gym", Icons.Rounded.FitnessCenter),
    IdentityGlyph("water", Icons.Rounded.WaterDrop),
    IdentityGlyph("drink", Icons.Rounded.LocalDrink),
    IdentityGlyph("calm", Icons.Rounded.SelfImprovement),
    IdentityGlyph("mind", Icons.Rounded.Psychology),
    IdentityGlyph("code", Icons.Rounded.Code),
    IdentityGlyph("write", Icons.Rounded.Edit),
    IdentityGlyph("music", Icons.Rounded.MusicNote),
    IdentityGlyph("language", Icons.Rounded.Translate),
    IdentityGlyph("study", Icons.Rounded.School),
    IdentityGlyph("morning", Icons.Rounded.WbSunny),
    IdentityGlyph("night", Icons.Rounded.NightsStay),
    IdentityGlyph("heart", Icons.Rounded.FavoriteBorder),
    IdentityGlyph("clean", Icons.Rounded.CleaningServices),
    IdentityGlyph("food", Icons.Rounded.Restaurant),
    IdentityGlyph("plant", Icons.Rounded.Spa),
    IdentityGlyph("focus", Icons.Rounded.Bolt)
)

fun glyphFor(key: String): ImageVector =
    identityGlyphs.firstOrNull { it.key == key }?.vector ?: Icons.Rounded.AutoAwesome

/** Ready made starters so a first identity takes seconds, not thought. */
data class IdentityTemplate(
    val titleFa: String,
    val titleEn: String,
    val ritualFa: String,
    val ritualEn: String,
    val iconKey: String,
    val colorHex: String,
    val seconds: Int
)

val identityTemplates: List<IdentityTemplate> = listOf(
    IdentityTemplate("من یک کتاب‌خوان هستم", "I am a reader", "خواندن یک صفحه", "Read one page", "book", "#39D6C3", 120),
    IdentityTemplate("من ورزشکارم", "I am an athlete", "دو دقیقه حرکت کششی", "Two minutes of stretching", "gym", "#F5B85C", 120),
    IdentityTemplate("من آرام هستم", "I am calm", "یک دقیقه تنفس آگاهانه", "One minute of breathing", "calm", "#8B7BF7", 60),
    IdentityTemplate("من نویسنده‌ام", "I am a writer", "نوشتن سه خط", "Write three lines", "write", "#5AC8FA", 300),
    IdentityTemplate("من زبان یاد می‌گیرم", "I am a language learner", "پنج کلمه تازه", "Five new words", "language", "#7ED957", 300),
    IdentityTemplate("من برنامه‌نویسم", "I am a developer", "ده دقیقه کد تمرینی", "Ten minutes of practice code", "code", "#FF7AB8", 600),
    IdentityTemplate("من مرتب هستم", "I am tidy", "دو دقیقه مرتب کردن میز", "Two minutes tidying the desk", "clean", "#C3CBD6", 120),
    IdentityTemplate("من به بدنم می‌رسم", "I hydrate", "یک لیوان آب", "One glass of water", "water", "#39D6C3", 30)
)

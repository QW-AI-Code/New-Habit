package com.duck.twominute.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duck.twominute.ai.network.FreeModelCatalog
import com.duck.twominute.ai.network.GeminiClient
import com.duck.twominute.ai.planner.PlannerViewModel
import com.duck.twominute.ai.usage.QuotaClock
import com.duck.twominute.toPersianDigits

/**
 * Gemini API key, free model picker and today's exact token usage.
 *
 * The model picker and the usage block appear only once the saved key has
 * passed a connection test; before that the dialog explains what to do next.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSettingsDialog(fa: Boolean, planner: PlannerViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings by planner.settings.collectAsStateWithLifecycle()
    val models by planner.models.collectAsStateWithLifecycle()
    val keyCheck by planner.keyCheck.collectAsStateWithLifecycle()
    val usage by planner.usage.collectAsStateWithLifecycle()
    var keyText by remember { mutableStateOf(settings.apiKey) }
    var keyVisible by remember { mutableStateOf(false) }
    LaunchedEffect(settings.apiKey) { if (keyText.isBlank()) keyText = settings.apiKey }

    fun num(value: Long): String = if (fa) value.toString().toPersianDigits() else value.toString()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(SurfaceNavy, RoundedCornerShape(28.dp))
                .border(1.dp, Hairline, RoundedCornerShape(28.dp))
                .padding(22.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(tr(fa, "تنظیمات هوش مصنوعی", "AI settings"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                tr(fa, "برنامه‌ریز عادت با کلید رایگان Gemini کار می‌کند. کلید فقط روی همین گوشی ذخیره می‌شود.", "The habit planner runs on a free Gemini key. The key is stored on this phone only."),
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            Spacer(Modifier.height(4.dp))

            Text(tr(fa, "کلید API", "API key"), style = MaterialTheme.typography.labelLarge, color = Muted)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("AIza…", color = Muted) },
                    leadingIcon = { Icon(Icons.Rounded.Key, null, tint = Turquoise) },
                    trailingIcon = {
                        IconButton({ keyVisible = !keyVisible }) {
                            Icon(if (keyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = Muted)
                        }
                    },
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Turquoise, unfocusedBorderColor = Hairline, cursorColor = Turquoise)
                )
            }
            val checking = keyCheck is PlannerViewModel.KeyCheck.Checking
            val removing = keyText.isBlank() && settings.apiKey.isNotBlank()
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { planner.saveApiKey(keyText) },
                    enabled = !checking && (keyText.isNotBlank() || removing),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(
                        if (removing) tr(fa, "حذف کلید", "Remove key") else tr(fa, "ذخیره و بررسی اتصال", "Save & test connection"),
                        maxLines = 1
                    )
                }
                TextButton(
                    onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GeminiClient.API_KEY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Rounded.OpenInNew, null, tint = Turquoise, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tr(fa, "گرفتن کلید رایگان", "Get a free key"), color = Turquoise, maxLines = 1)
                }
            }
            when (val check = keyCheck) {
                PlannerViewModel.KeyCheck.Idle -> Unit
                PlannerViewModel.KeyCheck.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Turquoise, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(tr(fa, "در حال اتصال و بررسی کلید…", "Connecting and checking the key…"), style = MaterialTheme.typography.labelMedium, color = Muted)
                }
                is PlannerViewModel.KeyCheck.Ok -> Text(
                    tr(fa, "اتصال برقرار شد · ${num(check.models.toLong())} مدل رایگان در دسترس", "Connected · ${check.models} free models available"),
                    style = MaterialTheme.typography.labelMedium,
                    color = Turquoise
                )
                is PlannerViewModel.KeyCheck.Failed -> Text(errorText(fa, check.kind), style = MaterialTheme.typography.labelMedium, color = Coral)
            }

            HorizontalDivider(color = Hairline, modifier = Modifier.padding(vertical = 6.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr(fa, "مدل", "Model"), style = MaterialTheme.typography.labelLarge, color = Muted, modifier = Modifier.weight(1f))
                if (models.isNotEmpty()) Pill(tr(fa, "به‌روزرسانی فهرست", "Refresh list"), icon = Icons.Rounded.Refresh) { planner.refreshModels() }
            }
            if (models.isEmpty()) {
                ModelsLocked(fa, hasKey = settings.apiKey.isNotBlank(), check = keyCheck) { planner.refreshModels() }
            }
            models.forEach { entry ->
                val chosen = entry.id == settings.model
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (chosen) Turquoise.copy(alpha = 0.10f) else RaisedNavy.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .border(1.dp, if (chosen) Turquoise else Hairline, RoundedCornerShape(16.dp))
                        .clickable { planner.setModel(entry.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (chosen) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (chosen) Turquoise else Muted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(entry.label(fa), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                            if (FreeModelCatalog.isRecommended(entry.id)) {
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Rounded.Star, null, tint = Amber, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(entry.note(fa), style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }


            // Usage belongs to a chosen model, so it is shown only once models exist.
            if (models.isNotEmpty()) {
                HorizontalDivider(color = Hairline, modifier = Modifier.padding(vertical = 6.dp))

                Text(tr(fa, "مصرف امروز", "Today's usage"), style = MaterialTheme.typography.labelLarge, color = Muted)
                val modelUsage = usage?.usage?.get(settings.model)
                val limits = usage?.limits?.get(settings.model)
                val perDay = limits?.effective(com.duck.twominute.ai.network.LimitKind.REQUESTS_PER_DAY)
                val requests = modelUsage?.requests ?: 0L
                val minute = planner.lastMinute(settings.model)
                Text(
                    tr(
                        fa,
                        "درخواست‌ها: ${num(requests)}" + (if (perDay != null) " از ${num(perDay)}" else "") + " · توکن‌ها: ${num(modelUsage?.totalTokens ?: 0L)}",
                        "Requests: $requests" + (if (perDay != null) " of $perDay" else "") + " · Tokens: ${modelUsage?.totalTokens ?: 0L}"
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    tr(fa, "دقیقه اخیر: ${num(minute.first.toLong())} درخواست · ${num(minute.second)} توکن ورودی", "Last minute: ${minute.first} requests · ${minute.second} input tokens"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                val hours = QuotaClock.millisUntilReset() / 3_600_000L
                val minutes = (QuotaClock.millisUntilReset() / 60_000L) % 60
                Text(
                    tr(fa, "سهمیه رایگان تا ${num(hours)} ساعت و ${num(minutes)} دقیقه دیگر صفر می‌شود (نیمه‌شب به وقت اقیانوس آرام).", "Free quota resets in ${hours}h ${minutes}m (midnight Pacific time)."),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Pill(tr(fa, "صفر کردن شمارنده‌ها", "Reset counters"), icon = Icons.Rounded.Refresh, accent = Coral) { planner.resetUsage() }
            }

            HorizontalDivider(color = Hairline, modifier = Modifier.padding(vertical = 6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Shield, null, tint = Turquoise, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    tr(fa, "کلید در فایل جداگانه‌ای نگه داشته می‌شود؛ نه در خروجی پشتیبان JSON می‌آید و نه در پشتیبان ابری اندروید.", "The key lives in a separate file: it is never part of the JSON backup or Android cloud backup."),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(tr(fa, "بستن", "Close"), color = Muted) }
        }
    }
}

/** What the model area shows while no model is available for the current key. */
@Composable
private fun ModelsLocked(fa: Boolean, hasKey: Boolean, check: PlannerViewModel.KeyCheck, onRetry: () -> Unit) {
    val failed = check as? PlannerViewModel.KeyCheck.Failed
    val checking = check is PlannerViewModel.KeyCheck.Checking
    Column(
        Modifier
            .fillMaxWidth()
            .background(RaisedNavy.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .border(1.dp, if (failed != null) Coral.copy(alpha = 0.5f) else Hairline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            when {
                checking -> CircularProgressIndicator(Modifier.size(18.dp), color = Turquoise, strokeWidth = 2.dp)
                failed != null -> Icon(Icons.Rounded.CloudOff, null, tint = Coral, modifier = Modifier.size(18.dp))
                else -> Icon(Icons.Rounded.Info, null, tint = Turquoise, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = when {
                    checking -> tr(fa, "در حال دریافت فهرست مدل‌های رایگان این کلید…", "Loading the free models for this key…")
                    !hasKey -> tr(fa, "هنوز کلیدی وارد نشده. کلید API را وارد کن و «ذخیره و بررسی اتصال» را بزن؛ پس از اتصال موفق، مدل‌های در دسترس همین‌جا نمایش داده می‌شوند.", "No key yet. Enter your API key and tap “Save & test connection”; once it connects, the available models appear here.")
                    failed != null -> tr(fa, "اتصال برقرار نشد، پس مدلی نمایش داده نمی‌شود. مشکل را برطرف کن و دوباره بررسی کن.", "The connection failed, so no models are shown. Fix the problem and test again.")
                    else -> tr(fa, "کلید ذخیره شده ولی هنوز اتصالش تأیید نشده. برای دیدن مدل‌ها اتصال را بررسی کن.", "The key is saved but its connection is not confirmed yet. Test it to see the models.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.weight(1f)
            )
        }
        if (hasKey && !checking) {
            Pill(tr(fa, "بررسی دوباره اتصال", "Test connection again"), icon = Icons.Rounded.Refresh) { onRetry() }
        }
    }
}

/** One sentence per error kind, in the UI language. */
fun errorText(fa: Boolean, kind: PlannerViewModel.ErrorKind): String = when (kind) {
    PlannerViewModel.ErrorKind.NO_KEY -> tr(fa, "اول کلید API رایگان Gemini را در تنظیمات هوش مصنوعی وارد کن.", "Add your free Gemini API key in AI settings first.")
    PlannerViewModel.ErrorKind.NOT_CONNECTED -> tr(fa, "اتصال کلید هنوز تأیید نشده. در تنظیمات هوش مصنوعی «ذخیره و بررسی اتصال» را بزن تا مدل‌ها بارگذاری شوند، بعد دوباره امتحان کن.", "Your key's connection is not confirmed yet. Tap “Save & test connection” in AI settings to load the models, then try again.")
    PlannerViewModel.ErrorKind.AUTH -> tr(fa, "کلید API معتبر نیست یا دسترسی ندارد. کلید را بررسی کن.", "The API key is invalid or has no access. Check the key.")
    PlannerViewModel.ErrorKind.QUOTA_MINUTE -> tr(fa, "به سقف درخواست در دقیقه رسیدی. یک دقیقه صبر کن و دوباره امتحان کن.", "Per-minute limit reached. Wait a minute and try again.")
    PlannerViewModel.ErrorKind.QUOTA_DAILY -> tr(fa, "سهمیه رایگان امروز این مدل تمام شده. مدل دیگری انتخاب کن؛ سهمیه هر مدل جداست.", "Today's free quota of this model is used up. Pick another model: each model has its own quota.")
    PlannerViewModel.ErrorKind.NETWORK -> tr(fa, "اتصال اینترنت برقرار نشد. اتصال را بررسی کن.", "Could not connect. Check your internet connection.")
    PlannerViewModel.ErrorKind.SERVER -> tr(fa, "سرور Gemini موقتاً پاسخ نداد. کمی بعد دوباره امتحان کن.", "Gemini's server did not answer. Try again in a moment.")
    PlannerViewModel.ErrorKind.SAFETY -> tr(fa, "Gemini به این درخواست پاسخ نداد. هدف را طور دیگری بنویس.", "Gemini declined this request. Try wording the goal differently.")
    PlannerViewModel.ErrorKind.PARSE -> tr(fa, "پاسخ مدل کامل یا خوانا نبود. دوباره امتحان کن.", "The model's answer was incomplete. Please try again.")
    PlannerViewModel.ErrorKind.UNKNOWN -> tr(fa, "خطای ناشناخته‌ای رخ داد. دوباره امتحان کن.", "Something went wrong. Please try again.")
}

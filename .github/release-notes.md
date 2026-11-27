# New Habit v1.1.0

## New: date-and-time reminders

- **Reminders tab:** A new tab for standalone reminders. Give one a title and an optional note, pick a day on the Jalali (Shamsi) calendar and an exact time, and the notification arrives at that moment.
- **Repeat during the day:** Pick a day and ring, for example, every hour from 09:00 to 21:00. Intervals of 15 min, 30 min, 1 h, 2 h, 3 h, 4 h or any custom value in 5-minute steps, with a start and an end time. The editor lists every ring of the day, such as "13 notifications a day: 09:00, 10:00, …".
- **Repeat across days:** Only that day, every day, chosen weekdays (Saturday first), every month or every year on the Jalali calendar, with an optional end date. A reminder on the 31st rings on the last day of 30-day months, and 30 Esfand rings on 29 Esfand in common years.
- **Jalali date picker:** A Saturday-first month grid with Persian digits and today highlighted. The Gregorian view is one tap away, quick picks cover today, tomorrow, in 2 days and next week, and past days cannot be picked.
- **Live preview:** Before saving, the editor shows the next 5 notifications with their Jalali date, time and "in 2 h 15 min". A one-time reminder whose time has passed cannot be saved.
- **Notification actions:** "Done" (or "Done for today", which skips the rest of today's rings) and "Snooze 10 min". Tapping the notification opens the Reminders tab.
- **Calendar and Today:** Days with a reminder get an amber dot in the calendar. The selected day lists its reminders and has an "Add a reminder on this day" button. The Today screen shows the next reminder of the coming 24 hours.
- **Reminder health check:** The Reminders tab shows in plain words what could stop a reminder on this phone (notifications off, exact alarms not allowed, battery optimization) and opens the right settings page. A "Test" button sends a real notification through the alarm system within 5 seconds.
- **Reliable after a reboot:** Reminders are re-armed after a reboot, an app update, a clock or time-zone change and an exact-alarm permission change. A ring that fell while the phone was off is shown once, marked as missed. Each ring is handled exactly once, even if Android delivers it twice.

## Fixes and improvements

- **Reminders never appeared on Android 8 to 12 (root cause fixed):** The app checked the Android 13 notification permission with `checkSelfPermission(POST_NOTIFICATIONS)`. On older Android versions that permission does not exist, so the answer was always "denied" and no reminder notification was ever posted. The app now uses `areNotificationsEnabled()`, which is correct on every version. This applies to habit reminders too.
- **On-time reminders on Android 14+:** Exact alarms are off by default on Android 14+, so reminders arrived late. The app now declares `USE_EXACT_ALARM` (Android 13+) and `SCHEDULE_EXACT_ALARM` (Android 12 only), and recovers if exact-alarm access is revoked.
- **Snooze fixes:** A pending snooze of a deleted or archived identity no longer rings, and "Done" on a habit notification cancels its pending snooze.
- **Minute-exact time picker:** Minutes now step by 1 (09:07 is possible), with quick chips for :00, :10, :15, :20, :30, :40, :45 and :50. The picker shows Persian digits in Persian.
- **Natural Persian times and dates:** Times read «صبح، ظهر، بعدازظهر، عصر، شب» (12:30 is no longer «عصر»). The Today header shows the weekday and Persian digits («جمعه ۶ آذر ۱۴۰۵» instead of «6 آذر 1405»), and counters, streaks, goals and repetitions use Persian digits everywhere.
- **Calendar arrows in RTL:** The previous and next month arrows now point the right way in Persian.
- **Data safety:** If the stored data can ever not be read, a copy is kept before anything is written, instead of being silently replaced by an empty state. Unknown values from a newer backup fall back to defaults instead of breaking the whole file.
- **Backup and restore:** Reminders are included in the JSON backup. Restoring cancels the alarms of reminders that are not in the backup and does not replay old rings.
- **Tested before release:** Unit tests for the reminder schedule and the Jalali calendar run in the GitHub workflow before any APK is built. A failing test stops the release.

## Security audit summary

| Area | Result |
| --- | --- |
| Repository secrets | No secrets are stored in source. Release signing uses GitHub Actions secrets and the same signing key as before, so v1.1.0 installs over earlier versions. |
| User data | Progress and reminders stay in local DataStore unless the user exports them. Unreadable data is copied before any write. |
| Gemini API key | Stored only on the device in a separate DataStore, excluded from the JSON export, Android cloud backup and device transfer. Sent only to Google's Gemini API over HTTPS. |
| Reminder intents | All reminder receivers are not exported. Every alarm and notification action uses an immutable PendingIntent with its own data URI, so reminders cannot collide or be triggered by other apps. |
| Permissions | Notifications, vibration, exact alarms (`USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` up to Android 12), the battery-optimization prompt (only when the user taps it), boot restore and internet (AI planner only) are used only for their stated features. |

<div dir="rtl" align="right">

# New Habit نسخه ۱.۱.۰

## تازه: یادآور با تاریخ و ساعت

- **زبانه یادآورها:** یک زبانه تازه برای یادآورهای مستقل. یک عنوان و در صورت نیاز توضیح بنویس، روز را از تقویم شمسی و ساعت دقیق را انتخاب کن تا همان لحظه اعلان بیاید.
- **تکرار در طول روز:** یک روز را انتخاب کن و بگو مثلاً از ساعت ۹ صبح تا ۹ شب هر ۱ ساعت یادآوری کند. فاصله‌ها: ۱۵ دقیقه، ۳۰ دقیقه، ۱، ۲، ۳ یا ۴ ساعت یا هر مقدار دلخواه با گام ۵ دقیقه، همراه با ساعت شروع و پایان. ویرایشگر همه زمان‌های اعلان آن روز را نشان می‌دهد، مثل «۱۳ اعلان در هر روز: ۰۹:۰۰، ۱۰:۰۰، …».
- **تکرار روزها:** فقط همان روز، هر روز، روزهای دلخواه هفته (از شنبه)، هر ماه یا هر سال بر پایه تقویم شمسی، با تاریخ پایان اختیاری. یادآورِ روز ۳۱ در ماه‌های ۳۰ روزه روز آخر ماه می‌آید و یادآورِ ۳۰ اسفند در سال‌های غیرکبیسه ۲۹ اسفند می‌آید.
- **انتخاب تاریخ شمسی:** جدول ماه از شنبه با رقم‌های فارسی و امروزِ مشخص. نمای میلادی هم با یک لمس در دسترس است، گزینه‌های سریع امروز، فردا، پس‌فردا و هفته بعد را داری و روزهای گذشته انتخاب نمی‌شوند.
- **پیش‌نمایش زنده:** پیش از ذخیره، ۵ اعلان بعدی با تاریخ شمسی، ساعت و زمان باقی‌مانده (مثل «۲ ساعت و ۱۵ دقیقه دیگر») نمایش داده می‌شود. یادآور یک‌باره‌ای که زمانش گذشته ذخیره نمی‌شود.
- **دکمه‌های اعلان:** «انجام شد» (یا «تمام برای امروز» که بقیه اعلان‌های امروز را رد می‌کند) و «۱۰ دقیقه بعد». لمس اعلان زبانه یادآورها را باز می‌کند.
- **تقویم و صفحه امروز:** روزهایی که یادآور دارند در تقویم یک نقطه کهربایی می‌گیرند. روز انتخاب‌شده یادآورهای خودش و دکمه «افزودن یادآور برای این روز» را نشان می‌دهد. صفحه امروز هم یادآور بعدیِ ۲۴ ساعت آینده را نشان می‌دهد.
- **بررسی سلامت یادآورها:** زبانه یادآورها به زبان ساده می‌گوید چه چیزی روی این گوشی جلوی یادآور را می‌گیرد (اعلان خاموش، اجازه نداشتن زنگ دقیق، بهینه‌سازی باتری) و صفحه درست تنظیمات را باز می‌کند. دکمه «اعلان آزمایشی» یک اعلان واقعی را از مسیر زنگ سیستم تا ۵ ثانیه بعد می‌فرستد.
- **پایدار پس از خاموش و روشن شدن:** یادآورها پس از راه‌اندازی دوباره، به‌روزرسانی برنامه، تغییر ساعت یا منطقه زمانی و تغییر اجازه زنگ دقیق دوباره تنظیم می‌شوند. اگر زمان یادآوری وقتی گوشی خاموش بود برسد، یک‌بار با برچسب «رد شده» نمایش داده می‌شود. هر اعلان دقیقاً یک‌بار ثبت می‌شود، حتی اگر اندروید آن را دو بار بفرستد.

## رفع اشکال‌ها و بهبودها

- **نیامدن یادآورها در اندروید ۸ تا ۱۲ (علت اصلی برطرف شد):** برنامه اجازه اعلانِ اندروید ۱۳ را با <span dir="ltr">`checkSelfPermission(POST_NOTIFICATIONS)`</span> بررسی می‌کرد. این اجازه در نسخه‌های قدیمی‌تر اندروید وجود ندارد، پس جواب همیشه «رد شده» بود و هیچ اعلان یادآوری نمایش داده نمی‌شد. حالا برنامه از <span dir="ltr">`areNotificationsEnabled()`</span> استفاده می‌کند که در همه نسخه‌ها درست است. این اصلاح یادآورهای عادت‌ها را هم در بر می‌گیرد.
- **یادآور سر وقت در اندروید ۱۴ به بالا:** در این نسخه‌ها زنگ دقیق به‌طور پیش‌فرض خاموش است و یادآورها با تأخیر می‌رسیدند. حالا برنامه <span dir="ltr">`USE_EXACT_ALARM`</span> (اندروید ۱۳ به بالا) و <span dir="ltr">`SCHEDULE_EXACT_ALARM`</span> (فقط اندروید ۱۲) را اعلام می‌کند و اگر اجازه زنگ دقیق گرفته شود، کار را ادامه می‌دهد.
- **اصلاح تعویق یادآور:** تعویقِ در انتظارِ هویتی که حذف یا آرشیو شده دیگر زنگ نمی‌زند و «انجام شد» در اعلان عادت، تعویقِ در انتظارش را لغو می‌کند.
- **انتخاب ساعت تا دقیقه:** دقیقه‌ها حالا یکی‌یکی جلو می‌روند (۰۹:۰۷ هم ممکن است) و دکمه‌های سریع :۰۰، :۱۰، :۱۵، :۲۰، :۳۰، :۴۰، :۴۵ و :۵۰ اضافه شد. در حالت فارسی رقم‌ها فارسی‌اند.
- **ساعت و تاریخ طبیعی فارسی:** ساعت‌ها با «صبح، ظهر، بعدازظهر، عصر، شب» نوشته می‌شوند (ساعت ۱۲:۳۰ دیگر «عصر» نیست). سربرگ صفحه امروز روز هفته و رقم فارسی را نشان می‌دهد («جمعه ۶ آذر ۱۴۰۵» به‌جای «6 آذر 1405») و شمارنده‌ها، زنجیره‌ها، هدف‌ها و تکرارها همه‌جا با رقم فارسی نمایش داده می‌شوند.
- **جهت فلش‌های تقویم:** فلش‌های ماه قبل و بعد در حالت فارسی درست نشان داده می‌شوند.
- **امنیت داده‌ها:** اگر روزی اطلاعات ذخیره‌شده خوانا نباشد، پیش از هر نوشتنی یک نسخه از آن نگه داشته می‌شود و دیگر بی‌صدا با وضعیت خالی جایگزین نمی‌شود. مقدارهای ناشناخته از پشتیبانِ نسخه جدیدتر هم به مقدار پیش‌فرض برمی‌گردند و کل فایل را خراب نمی‌کنند.
- **پشتیبان و بازیابی:** یادآورها در پشتیبان <span dir="ltr">JSON</span> هستند. بازیابی، زنگ یادآورهایی را که در پشتیبان نیستند لغو می‌کند و اعلان‌های قدیمی را دوباره پخش نمی‌کند.
- **آزمون پیش از انتشار:** آزمون‌های واحد زمان‌بندی یادآور و تقویم شمسی پیش از ساخت هر <span dir="ltr">APK</span> در روند <span dir="ltr">GitHub</span> اجرا می‌شوند. اگر آزمونی شکست بخورد، انتشار متوقف می‌شود.

## خلاصه ممیزی امنیتی

| بخش | نتیجه |
| --- | --- |
| اسرار مخزن | هیچ کلیدی در کد ذخیره نشده است. امضای نسخه با اسرار <span dir="ltr">GitHub Actions</span> و همان کلید امضای قبلی انجام می‌شود، پس نسخه ۱.۱.۰ روی نسخه‌های قبلی نصب می‌شود. |
| اطلاعات کاربر | پیشرفت‌ها و یادآورها در <span dir="ltr">DataStore</span> محلی می‌مانند مگر اینکه کاربر خروجی بگیرد. پیش از هر نوشتن از داده ناخوانا یک نسخه نگه داشته می‌شود. |
| کلید <span dir="ltr">Gemini API</span> | فقط روی دستگاه و در <span dir="ltr">DataStore</span> جداگانه ذخیره می‌شود، از خروجی <span dir="ltr">JSON</span>، پشتیبان ابری و انتقال دستگاه حذف شده و فقط از راه <span dir="ltr">HTTPS</span> به <span dir="ltr">Gemini API</span> گوگل فرستاده می‌شود. |
| اینتنت‌های یادآور | همه گیرنده‌های یادآور غیرقابل‌دسترس برای برنامه‌های دیگرند. هر زنگ و دکمه اعلان یک <span dir="ltr">PendingIntent</span> تغییرناپذیر با آدرس داده مخصوص به خود دارد، پس یادآورها با هم تداخل ندارند و برنامه دیگری نمی‌تواند آن‌ها را اجرا کند. |
| دسترسی‌ها | اعلان، لرزش، زنگ دقیق (<span dir="ltr">USE_EXACT_ALARM</span> و <span dir="ltr">SCHEDULE_EXACT_ALARM</span> تا اندروید ۱۲)، درخواست بهینه‌سازی باتری (فقط با لمس کاربر)، بازیابی پس از راه‌اندازی و اینترنت (فقط مربی هوشمند) فقط برای قابلیت‌های اعلام‌شده استفاده می‌شوند. |

</div>

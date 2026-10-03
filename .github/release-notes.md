# New Habit v1.0.1

- **AI habit coach (Gemini):** Write any goal, such as learning English or building your ideal body, and get a complete identity-based plan built on proven behavior science: identity statement, scientific methods, phases on a week timeline, habits with cue, reason and level-up rule, reminder times, weekdays, milestones, if-then obstacle plans, tips and a safety note for health goals.
- **Plan preview and import:** Review the whole plan before anything changes, add one habit or all of them as identities with their reminders already set, revise the plan with your own feedback, or discard it. The last plan is kept after leaving the screen.
- **AI settings:** Enter a free Gemini API key, test it, pick from a strict list of free models (Gemini 3.1 Flash-Lite recommended), and see today's exact requests and tokens, the last-minute usage and the time until the free quota resets.
- **Multiple reminders per day:** "Times per day = 3" now creates 3 independent reminders. Each one has its own time, a "Spread evenly" action places them across the day, duplicate times are blocked, and Cancel in the time picker really cancels.
- **Smarter notifications:** A new "Done" action logs the repetition straight from the notification, the text shows "Reminder 2 of 3 · today 1/3", reminders are skipped once the day is complete, and a changed custom sound is applied right away.
- **Accurate streaks:** A day counts only when every repetition is done, and days an identity does not run on no longer break the best streak.
- **Reliability fixes:** Every change is saved in one atomic step so quick taps and notification actions can no longer overwrite each other, restoring a backup cancels the alarms of removed identities, and v1.0.0 reminders are migrated automatically.
- **English "Why it matters" field** in the identity editor.
- **Privacy:** The API key is stored in a separate local file that is never included in the JSON backup, Android cloud backup or device transfer. Internet access is used only by the AI planner.
- **Models only after a real connection:** The model picker stays empty until an API key is saved and its connection test succeeds. Changing or removing the key clears the old list, a rejected key hides the models again, and an untested key is re-checked automatically.
- **No more squeezed buttons:** Buttons keep their label on one line, and screen headers move their actions to a new line when space runs out, so labels like "Add" can no longer break into a vertical column of letters (Persian and English).
- **No more clipped text:** Line heights now scale with each text size and are never trimmed, and checklist cards grow with their content instead of cutting the second line in half.
- **Vazirmatn font:** The app now uses the Vazir (Vazirmatn) font in five weights, which fixes letters drawn wrongly by the old font, such as the upside-down dots of «پ».

## Security audit summary

| Area | Result |
| --- | --- |
| Repository secrets | No secrets are stored in source; release signing uses GitHub Actions secrets. |
| User data | Progress remains in local DataStore unless the user exports it. |
| Gemini API key | Stored only on the device in a separate DataStore, excluded from the JSON export, Android cloud backup and device transfer. Sent only to Google's Gemini API over HTTPS. |
| Permissions | Notifications, vibration, exact alarms, audio, boot restore and internet (AI planner only) are used only for their stated features. |

<div dir="rtl">

# New Habit نسخه ۱.۰.۱

- **مربی هوشمند عادت (Gemini):** هر هدفی را بنویس، مثل یادگیری زبان انگلیسی یا رسیدن به اندام ایده‌آل، و یک برنامه کامل هویت‌محور بر پایه علم تغییر رفتار بگیر: جمله هویت، روش‌های علمی، مراحل روی خط زمانی هفته‌ای، عادت‌ها با نشانه، دلیل و قانون پیشرفت، ساعت یادآورها، روزهای هفته، نقاط عطف، برنامه‌های «اگر… آنگاه…» برای موانع، نکته‌ها و هشدار ایمنی برای هدف‌های سلامتی.
- **پیش‌نمایش و افزودن برنامه:** پیش از هر تغییری کل برنامه را ببین، یک عادت یا همه را با یادآورهای تنظیم‌شده به هویت‌هایت اضافه کن، برنامه را با نظر خودت بازنویسی کن یا حذفش کن. آخرین برنامه پس از خروج از صفحه هم باقی می‌ماند.
- **تنظیمات هوش مصنوعی:** کلید رایگان <span dir="ltr">Gemini API</span> را وارد و بررسی کن، از فهرست دقیق مدل‌های رایگان انتخاب کن (پیشنهاد: <span dir="ltr">Gemini 3.1 Flash-Lite</span>) و تعداد دقیق درخواست‌ها و توکن‌های امروز، مصرف دقیقه اخیر و زمان صفر شدن سهمیه رایگان را ببین.
- **چند یادآور در روز:** «چند بار در روز = ۳» حالا ۳ یادآور مستقل می‌سازد. هر یادآور ساعت خودش را دارد، گزینه «پخش یکنواخت در روز» آن‌ها را در طول روز می‌چیند، ساعت تکراری پذیرفته نمی‌شود و «انصراف» در انتخاب ساعت واقعاً لغو می‌کند.
- **اعلان‌های هوشمندتر:** دکمه تازه «انجام شد» تکرار را مستقیم از اعلان ثبت می‌کند، متن اعلان «یادآور ۲ از ۳ · امروز ۱/۳» را نشان می‌دهد، پس از کامل شدن روز یادآوری ارسال نمی‌شود و صدای دلخواهِ تغییرکرده بلافاصله اعمال می‌شود.
- **زنجیره دقیق:** روزی کامل حساب می‌شود که همه تکرارهایش انجام شده باشد و روزهایی که هویت در آن‌ها برنامه ندارد، دیگر بهترین زنجیره را قطع نمی‌کنند.
- **رفع مشکلات پایداری:** هر تغییر در یک مرحله اتمیک ذخیره می‌شود تا لمس‌های سریع و دکمه‌های اعلان همدیگر را بازنویسی نکنند، بازیابی پشتیبان زنگ‌های هویت‌های حذف‌شده را لغو می‌کند و یادآورهای نسخه ۱.۰.۰ خودکار منتقل می‌شوند.
- **فیلد انگلیسی «چرا مهم است»** در ویرایشگر هویت.
- **حریم خصوصی:** کلید API در فایل محلی جداگانه‌ای ذخیره می‌شود و هرگز در پشتیبان <span dir="ltr">JSON</span>، پشتیبان ابری اندروید یا انتقال دستگاه قرار نمی‌گیرد. دسترسی اینترنت فقط برای مربی هوشمند استفاده می‌شود.
- **نمایش مدل‌ها فقط پس از اتصال واقعی:** تا کلید API ذخیره نشده و آزمون اتصالش موفق نشده، هیچ مدلی نمایش داده نمی‌شود. با تغییر یا حذف کلید فهرست قبلی پاک می‌شود، کلید نامعتبر مدل‌ها را دوباره پنهان می‌کند و کلید بررسی‌نشده خودکار دوباره بررسی می‌شود.
- **پایان دکمه‌های فشرده:** متن دکمه‌ها همیشه در یک خط می‌ماند و اگر جا کم باشد دکمه‌های سربرگ به خط بعد می‌روند؛ دیگر «افزودن» به‌صورت عمودی و حرف‌به‌حرف شکسته نمی‌شود (فارسی و انگلیسی).
- **پایان متن‌های بریده:** ارتفاع خط متناسب با اندازه هر متن است و هیچ‌وقت بریده نمی‌شود و کارت‌های چک‌لیست با محتوایشان بزرگ می‌شوند، نه اینکه خط دوم نصفه زیر لبه کارت برود.
- **فونت وزیر:** فونت برنامه به وزیر (Vazirmatn) در پنج وزن تغییر کرد؛ ایرادهای فونت قبلی مثل برعکس بودن نقطه‌های «پ» برطرف شد.

## خلاصه ممیزی امنیتی

| بخش | نتیجه |
| --- | --- |
| اسرار مخزن | هیچ کلیدی در کد ذخیره نشده و امضای نسخه با اسرار <span dir="ltr">GitHub Actions</span> انجام می‌شود. |
| اطلاعات کاربر | پیشرفت‌ها در <span dir="ltr">DataStore</span> محلی می‌مانند مگر اینکه کاربر خروجی بگیرد. |
| کلید <span dir="ltr">Gemini API</span> | فقط روی دستگاه و در <span dir="ltr">DataStore</span> جداگانه ذخیره می‌شود، از خروجی <span dir="ltr">JSON</span>، پشتیبان ابری و انتقال دستگاه حذف شده و فقط از راه <span dir="ltr">HTTPS</span> به <span dir="ltr">Gemini API</span> گوگل فرستاده می‌شود. |
| دسترسی‌ها | اعلان، لرزش، زنگ دقیق، صدا، بازیابی پس از راه‌اندازی و اینترنت (فقط مربی هوشمند) فقط برای قابلیت‌های اعلام‌شده استفاده می‌شوند. |

</div>

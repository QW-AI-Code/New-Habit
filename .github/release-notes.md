# New Habit v1.0.0

- **Flexible habit duration:** Choose any duration from 10 seconds to 2 hours.
- **Habit tracking:** Set goals, repeat habits, review streaks, browse calendar history, and view 12 weeks of activity.
- **Professional interface:** Use the New Habit brain mark, high-contrast surfaces, animated feedback, identity colors, and a dedicated About page.
- **Persian localization:** Use RTL layout, IranSans typography, and Persian numerals in the Jalali calendar.
- **Backup and restore:** Export and import progress as JSON.

## Security audit summary

| Area | Result |
| --- | --- |
| Repository secrets | No secrets are stored in source; release signing uses GitHub Actions secrets. |
| User data | Progress remains in local DataStore unless the user exports it. |
| Permissions | Notifications, vibration, exact alarms, audio, and boot restore are used only for their stated features. |

<div dir="rtl">

# New Habit نسخه ۱.۰.۰

- **مدت دلخواه عادت:** هر مدتی از ۱۰ ثانیه تا ۲ ساعت انتخاب کن.
- **پیگیری عادت:** هدف تعیین کن، عادت‌ها را تکرار و زنجیره‌ها را بررسی کن، سابقه تقویمی و فعالیت ۱۲ هفته‌ای را ببین.
- **رابط حرفه‌ای:** نشان مغز New Habit، سطوح با کنتراست بالا، بازخوردهای متحرک، رنگ‌های هویت و صفحه درباره را استفاده کن.
- **بومی‌سازی فارسی:** چیدمان راست‌به‌چپ، تایپوگرافی IranSans و اعداد فارسی در تقویم شمسی.
- **پشتیبان‌گیری و بازیابی:** پیشرفت را با فرمت <span dir="ltr">JSON</span> خروجی بگیر و بازیابی کن.

## خلاصه ممیزی امنیتی

| بخش | نتیجه |
| --- | --- |
| اسرار مخزن | هیچ کلیدی در کد ذخیره نشده و امضای نسخه با اسرار <span dir="ltr">GitHub Actions</span> انجام می‌شود. |
| اطلاعات کاربر | پیشرفت‌ها در <span dir="ltr">DataStore</span> محلی می‌مانند مگر اینکه کاربر خروجی بگیرد. |
| دسترسی‌ها | اعلان، لرزش، زنگ دقیق، صدا و بازیابی پس از راه‌اندازی فقط برای قابلیت‌های اعلام‌شده استفاده می‌شوند. |

</div>

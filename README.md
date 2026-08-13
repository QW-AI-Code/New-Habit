# New Habit

Kotlin + Jetpack Compose Android habit-building app, Persian first and English second.

**Languages:** [English](README.md) · [فارسی](README.fa.md)

## What's new in v1.0.0

- **Flexible habit duration:** Choose any duration from 10 seconds to 2 hours.
- **Habit tracking:** Set goals, repeat habits, review streaks, browse calendar history, and view 12 weeks of activity.
- **Professional interface:** Use the New Habit brain mark, high-contrast surfaces, animated feedback, identity colors, and a dedicated About page.
- **Persian localization:** Use RTL layout, IranSans typography, and Persian numerals in the Jalali calendar.
- **Backup and restore:** Export and import progress as JSON.

## Release

- **Version:** 1.0.0
- **Version code:** 1
- **APK names:** `new-habit-v1.0.0-<abi>.apk`
- **ABIs:** armeabi-v7a, arm64-v8a, x86, x86_64
- **Workflow:** Run `New Habit Android Build` manually with tag `v1.0.0`.

## Security audit summary

| Area | Result |
| --- | --- |
| Repository secrets | No secrets are stored in source; release signing uses GitHub Actions secrets. |
| User data | Progress remains in local DataStore unless the user exports it. |
| Permissions | Notifications, vibration, exact alarms, audio, and boot restore are used only for their stated features. |

## Typography

Persian UI uses the bundled IranSans regular and bold font files through the app-wide Compose typography and RTL layout direction. Jalali calendar numerals use Persian digits.

## Source

- Author: https://github.com/QW-AI-Code

# New Habit

Kotlin + Jetpack Compose Android habit-building app, Persian first and English second.

**Languages:** [English](README.md) · [فارسی](README.fa.md)

## What's new in v1.0.1

- **AI habit coach (Gemini):** Write any goal, such as learning English or building your ideal body, and get a complete identity-based plan built on proven behavior science: identity statement, scientific methods, phases on a week timeline, habits with cue, reason and level-up rule, reminder times, weekdays, milestones, if-then obstacle plans, tips and a safety note for health goals.
- **Plan preview and import:** Review the whole plan before anything changes, add one habit or all of them as identities with their reminders already set, revise the plan with your own feedback, or discard it. The last plan is kept after leaving the screen.
- **AI settings:** Enter a free Gemini API key, test it, pick from a strict list of free models (Gemini 3.1 Flash-Lite recommended), and see today's exact requests and tokens, the last-minute usage and the time until the free quota resets.
- **Multiple reminders per day:** "Times per day = 3" now creates 3 independent reminders. Each one has its own time, a "Spread evenly" action places them across the day, duplicate times are blocked, and Cancel in the time picker really cancels.
- **Smarter notifications:** A new "Done" action logs the repetition straight from the notification, the text shows "Reminder 2 of 3 · today 1/3", reminders are skipped once the day is complete, and a changed custom sound is applied right away.
- **Accurate streaks:** A day counts only when every repetition is done, and days an identity does not run on no longer break the best streak.
- **Reliability fixes:** Every change is saved in one atomic step so quick taps and notification actions can no longer overwrite each other, restoring a backup cancels the alarms of removed identities, and v1.0.0 reminders are migrated automatically.
- **English "Why it matters" field** in the identity editor.
- **Privacy:** The API key is stored in a separate local file that is never included in the JSON backup, Android cloud backup or device transfer. Internet access is used only by the AI planner.

## What's new in v1.0.0

- **Flexible habit duration:** Choose any duration from 10 seconds to 2 hours.
- **Habit tracking:** Set goals, repeat habits, review streaks, browse calendar history, and view 12 weeks of activity.
- **Professional interface:** Use the New Habit brain mark, high-contrast surfaces, animated feedback, identity colors, and a dedicated About page.
- **Persian localization:** Use RTL layout, Vazirmatn (Vazir) typography, and Persian numerals in the Jalali calendar.
- **Backup and restore:** Export and import progress as JSON.

## Release

- **Version:** 1.0.1
- **Version code:** 2
- **APK names:** `new-habit-v1.0.1-<abi>.apk`
- **ABIs:** armeabi-v7a, arm64-v8a, x86, x86_64
- **Workflow:** Run `New Habit Android Build` manually with tag `v1.0.1`.

## Security audit summary

| Area | Result |
| --- | --- |
| Repository secrets | No secrets are stored in source; release signing uses GitHub Actions secrets. |
| User data | Progress remains in local DataStore unless the user exports it. |
| Gemini API key | Stored only on the device in a separate DataStore, excluded from the JSON export, Android cloud backup and device transfer. Sent only to Google's Gemini API over HTTPS. |
| Permissions | Notifications, vibration, exact alarms, audio, boot restore and internet (AI planner only) are used only for their stated features. |

## Typography

The whole UI (Persian and English) uses the bundled Vazirmatn font (Regular, Medium, SemiBold, Bold, ExtraBold; SIL Open Font License 1.1, © The Vazirmatn Project Authors) through the app-wide Compose typography, with line heights sized for Persian glyphs, and RTL layout direction. Jalali calendar numerals use Persian digits.

## Source

- Author: https://github.com/QW-AI-Code

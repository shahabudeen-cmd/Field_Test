# CCTV Commissioning – Android app

Checklist app for commissioning engineers, built from "CCTV - PATCHING SCHEDULE - OVERALL 19.09.xlsx" (sheet OVERALL, 747 items, 23 racks).

For every item the engineer answers **Yes / No** for:
1. Field Tag   (compare with Camera Tag)
2. Cable Tag   (compare with Patch Panel Tag)
3. Cable Location (compare with Location → Rack / Patch Panel / Port)
and types **Comments / observations**.

Features: search, rack filter, status filter (All / To do / Verified / Issues), Previous/Next stepping,
auto-save on device (works offline), progress summary, CSV export (opens in Excel) with the schedule columns + results.

## Build
1. Open this folder in Android Studio (Ladybug or newer) and let Gradle sync.
2. Run on a phone, or Build > Build APK(s) to get app/build/outputs/apk/debug/app-debug.apk.
   (No Gradle wrapper is included; Android Studio creates it on first sync.)

## Updating the data
Schedule data lives in app/src/main/assets/patching_schedule.json (generated from the OVERALL sheet).
Replace it with a regenerated file to load a new revision. Keys match the sheet columns.

## Photos
Each item can have photos (Take photo / Choose photos). They are shrunk to 1600 px, saved on the phone and
listed in the CSV report (columns "Photos" and "Photo Files"). Menu > "Export photos (ZIP)" saves all photos
named SL<no>_<n>.jpg, matching the "Photo Files" column.

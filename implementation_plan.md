# FuelBuddy SL - Premium Fuel Management App

FuelBuddy is a high-end Android application designed for the Sri Lankan fuel market, featuring cutting-edge Glassmorphism UI, offline-first logic, and intelligent vehicle plate tracking.

## 🎨 Visual Identity & UI/UX
- **Design Language**: Glassmorphism with 24dp+ rounded corners, subtle white outlines, and linear gradients.
- **Color Palette**: Dark Mode default with #00E5FF (Electric Blue) for Petrol and #FFC107 (Amber) for Diesel.
- **Animations**: Integrated Lottie for success states and smooth transitions.
- **Dashboard**: Card-based interface showing live prices, plate-based schedules, and a scrolling news feed.

## ⚙️ Core Logic
- **Offline Engine**: Uses **Room Database** to cache the latest news and prices. 100% functional without internet.
- **News Aggregator**: **WorkManager** background task fetches RSS from Ada Derana and NewsFirst every hour.
- **Keyword Filtering**: Logic filters for "Fuel", "Petrol", "Diesel", "CPC", and "LIOC".

## 🚗 Vehicle Intelligence (Odd/Even)
- **Automatic Detection**: Extracting the last digit from the plate number.
- **Scheduling**:
  - **Odd (1,3,5,7,9)**: Mon, Wed, Fri alerts.
  - **Even (0,2,4,6,8)**: Tue, Thu, Sat alerts.
- **Reliability**: Uses **AlarmManager (Exact Alarms)** to trigger notifications at **6:00 AM sharp**, even if the app is closed.

## 🔔 Notifications
- **Style**: `BigTextStyle` for full news headlines.
- **Content**: Mixed Sinhala/English ("Ada Oyage Fuel Dawasa!") for a local premium feel.

## 🚀 DevOps & Deployment
- **GitHub Actions**: Automated pipeline in [.github/workflows/android.yml](file:///e:/mywork/Android/first/.github/workflows/android.yml).
- **JDK**: 17.
- **Output**: Generates a downloadable APK on every push to `main`.

---
Developer: Antigravity Assistant
Date: March 2026
Location: Sri Lanka

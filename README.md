# TVMime 📺

TVMime is an ultra-fast, high-performance IPTV player built for **Android TV** and **Android Mobile**. Built with a modern **Kotlin Multiplatform (KMP)** core and pure **Jetpack Compose** UI, TVMime delivers a buttery-smooth 60fps experience inspired by TiviMate and modern streaming services.

> **Production Web Admin**: [https://tvmime.vercel.app](https://tvmime.vercel.app)  
> **Direct TV APK**: [https://tvmime.vercel.app/tv.apk](https://tvmime.vercel.app/tv.apk)  
> **Direct Mobile APK**: [https://tvmime.vercel.app/mobile.apk](https://tvmime.vercel.app/mobile.apk)  
> **GitHub Releases**: [https://github.com/Fragger7/tvmime/releases](https://github.com/Fragger7/tvmime/releases)

---

## 🏗️ Architecture

```
                    ┌─────────────────────────┐
                    │       Web Admin         │
                    │   React 19 + Tailwind   │
                    │ (Firebase Auth/Firestore│
                    └────────────┬────────────┘
                                 │ Cloud Sync
                                 ▼
┌───────────────────────────────────────────────────────────┐
│                    Shared Core (KMP)                      │
│  • Ktor HTTP Client (Spoofed UA: IPTVSmartersPro/1.1.1)   │
│  • Zero-OOM Streaming Catalog Parser (50MB+ JSON safe)    │
│  • KMP Room Database (SQLite caching for 100k+ channels)  │
│  • Centralized Design Tokens (Deep Black & Crimson Red)   │
└────────────────────────────┬──────────────────────────────┘
                             │
              ┌──────────────┴──────────────┐
              ▼                             ▼
   ┌────────────────────┐        ┌────────────────────┐
   │    Android TV      │        │   Android Mobile   │
   │   (tvApp Module)   │        │ (androidApp Module)│
   │  Compose for TV    │        │  Jetpack Compose   │
   │  ExoPlayer Media3  │        │  Chromecast Cast   │
   └────────────────────┘        └────────────────────┘
```

- **Shared Core (`shared/`)**: Platform-agnostic Kotlin engine for Xtream Codes authentication, streaming catalog ingestion, and Room SQLite caching. Zero Android-specific SDK calls.
- **Android TV (`tvApp/`)**: 100% Jetpack Compose for TV (Material 3). Full D-Pad navigation, collapsible left navigation drawer, EPG timeline grid, hardware-accelerated Media3 player, and telemetry HUD.
- **Android Mobile (`androidApp/`)**: Jetpack Compose mobile client with cloud portal synchronization and Chromecast receiver integration.
- **Web Admin Portal (`adminWeb/`)**: Serverless proxy (`/api/test-portal`) for zero-CORS IPTV testing, portal credential management, and direct download links.

---

## ✨ Key Features (v1.1.0 / Build 23)

### 📺 TV Playback & In-Stream OSD
- **Hardware-Accelerated ExoPlayer**: Dynamic buffer tuning (Fast Zap profile vs. Balanced 4K profile) based on hardware capability detection.
- **Top Channel Bar**: Live channel number, channel name, resolution badge (`1080p`, `4K`), active playlist indicator, and active-to-max connection tally (`0/1`, `1/1`, `2/1 Cons`) with live health indicator dot.
- **Dedicated Clock Overlay**: Independent real-time clock pill anchored to the upper-right corner during fullscreen playback.
- **In-Stream Track Selector**: Modal sheets for switching audio tracks (`C.TRACK_TYPE_AUDIO`) and subtitle tracks (`C.TRACK_TYPE_TEXT`).
- **Aspect Ratio Control**: Instant cycling between `FIT`, `FILL`, and `ZOOM`.
- **D-Pad Right Last Channel Zap**: Pressing `KEYCODE_DPAD_RIGHT` immediately flips back and forth between previous and current channels with an animated floating toast badge.
- **Stats for Nerds (Telemetry HUD)**: Live data transfer bitrate (Mbps/kbps), stream host/IP and egress port, buffer depth seconds with cached percentage bar, hardware video decoder, and audio DSP codec.
- **1-Click Issue Reporting**: Immediately logs dead streams and egress blocks (HTTP 456/884) to Firestore for triage.

### 📅 TV Guide (EPG)
- **TiviMate-Style EPG Grid Guide**: Interactive 30-minute time intervals, vertical channel rows, program synopsis, and a persistent PIP mini-preview player in the top-right corner.

### ⚙️ Settings & System Preferences
- **Interactive Preferences**: D-Pad navigable toggles for Clock Overlay, OSD Auto-Hide duration (3s, 5s, 10s, Always On), and Last Channel Zap.
- **Hardware Intelligence**: Displays device model, system RAM, VPU capability, and auto-tuning profile.
- **In-Place OTA Updater**: Checks GitHub Releases and downloads signed APK updates without losing local database caches.

---

## 🎨 Design System
The canonical design aesthetic is **Deep Black (`#0A0A0E`) with Crimson Red (`#E50914`) accents**, flat subtle translucency, and crisp typography.  
Reference mockup: `mockups/tv_livetv_red_black.jpg`.

---

## 🚀 Automated Versioning & CI/CD Pipeline
- **Continuous Delivery**: Handled via GitHub Actions (`.github/workflows/build.yml`).
- **Trigger**: Run manually via `workflow_dispatch` or by pushing a release tag (`v*`) to conserve GitHub Actions runner quota.
- **Monotonic Build Code**: Automatically derived from `git rev-list --count HEAD`.
- **Semantic Versioning**: Automatically parsed from Conventional Commit history (`feat:` $\rightarrow$ Minor bump, `fix:`/`perf:` $\rightarrow$ Patch bump).
- **Artifacts Published**: Rolling release tagged `latest` automatically attaches:
  - `tv.apk` (and `tvmime_tv_vX.X.X.apk`)
  - `mobile.apk` (and `tvmime_mobile_vX.X.X.apk`)
  - Permanent direct download link via Vercel: `https://tvmime.vercel.app/tv.apk`


# AI Agent Context & Phoenix Protocol 🤖

> **Repository**: `https://github.com/Fragger7/tvmime.git`  
> **Canonical Root**: `/Users/admin/Development/tvmime`  
> **Production Web (OTA)**: `https://tvmime.vercel.app`  
> **Direct TV APK (OTA)**: `https://tvmime.vercel.app/tv.apk`  
> **Releases Page**: `https://github.com/Fragger7/tvmime/releases`  
> **Database Structure**: KMP SQLite (Room)
> **Current Lifecycle**: `v3.0.0-beta12`

**ATTENTION ALL AI AGENTS:** You are operating under the **PHOENIX PROTOCOL**. Previous agents attempted a monolithic "organ transplant" of the StreamVault UI, dragging in Dagger Hilt and bloated Domain Repositories. This caused cascading compile failures and violated our Kotlin Multiplatform (KMP) vision. 

## 1. THE PHOENIX PROTOCOL (CRITICAL RULES OF ENGAGEMENT)
1. **NO DAGGER HILT:** You are STRICTLY FORBIDDEN from using, importing, or referencing Dagger Hilt (`@HiltAndroidApp`, `@Inject`, etc.). TVMime uses manual DI or Koin for our lightweight KMP architecture.
2. **STATELESS VISUAL CLONING ONLY:** When building a UI component (Live TV Grid, EPG, Player HUD), you will use StreamVault’s code PURELY as a *visual mockup*. Extract the colors, padding, focus scaling, and typography. Rewrite the UI as a **100% Stateless `@Composable` function**.
3. **NO UI BACKEND WIRING:** A UI component (`LiveTvGrid`, `VodCard`) must NEVER query a database, initiate a network call, or hold a ViewModel. It must only accept raw data classes (e.g., `channels: List<ChannelEntity>`) and lambda callbacks (e.g., `onZap: (ChannelEntity) -> Unit`).
4. **THE ORCHESTRATOR PATTERN:** All data fetching from the V3 backend (SQLite/Ktor) happens in a top-level route (e.g., `LiveTvRoute.kt`), which collects standard Android ViewModels (no Hilt) and passes raw state down to the stateless Compose shells.

## 2. BACKEND & DOMAIN CONTEXT (THE V3 ENGINE)
- **Network Evasion (IPTVMine-Pro inspired):** We use Ktor with strict spoofed User-Agents (`IPTVSmartersPro/1.1.1`) to bypass provider blocks.
- **Mass Ingestion (StreamVault inspired):** We parse 50MB+ JSON/M3U payloads using zero-OOM token-by-token streaming (`StreamingCatalogParser.kt`), injecting directly into Room DB (`AppDatabase`).
- **Dual-Engine Playback (OwnTV inspired):** We use Media3 (ExoPlayer) wrapped in our `LivePreviewEngine` for UI, handing off to `MainPlayer` for full screen.
- **Firebase Constraint:** Firebase Auth & Firestore ONLY sync lightweight portal credentials (URL, username, pass). The heavy channels/EPG stay in local SQLite to stay within free-tier limits.

## 3. DESIGN SYSTEM (THE PAINT)
- Canonical Theme: **Deep Black & Crimson Red**
- Reference: `tvmime/shared/src/commonMain/kotlin/com/tvmime/theme/DesignSystemTokens.kt`
- NEVER use generic Material colors. Always map to `DesignSystemTokens`.

## 4. PIPELINE & OTA SYSTEM
- **GitHub Actions:** Automatically builds Android TV & Mobile APKs on tag pushes.
- **Versioning:** Automated via Conventional Commits (`feat:`, `fix:`). Build version codes are strictly `git rev-list --count HEAD`.
- **Vercel OTA:** Vercel hosts the React admin panel and redirects `/tv.apk` to the latest GitHub release asset. Do NOT break the APK output paths.

## 5. YOUR IMMEDIATE DIRECTIVE
Read `TODO.md` and `IMPLEMENTATION_PLAN.md`. You are to execute "Phase 0: The Purge" to eradicate Dagger Hilt and proxy stubs, followed by "Phase 1: Stateless Live TV Shell". Stop over-engineering. Build stateless UI and wire it cleanly.

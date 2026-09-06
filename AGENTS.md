# 🚨 AI AGENT BOOT SEQUENCE & PHOENIX PROTOCOL 🚨

**IF YOU ARE AN AI AGENT (AGY, CLAUDE, GEMINI, CURSOR) READING THIS, STOP WHAT YOU ARE DOING AND READ THIS ENTIRE FILE LINE-BY-LINE. YOU ARE A LITERAL EXECUTION ENGINE. FOLLOW THESE INSTRUCTIONS EXACTLY.**

## 1. YOUR BOOT SEQUENCE (THE SYMPHONY)
You must execute your context gathering in this EXACT order:
1. **Read this file (`AGENTS.md`)** to understand the unbreakable rules, your memory, and current state.
2. **Read `IMPLEMENTATION_PLAN.md`** to understand the "Phoenix Protocol" (our pivot from a failed monolithic transplant to a Stateless UI).
3. **Read `TODO.md`** to see the EXACT step-by-step file modifications you need to make right now for Sprint 20.
4. **DO NOT** make assumptions. **DO NOT** rewrite the backend. **DO NOT** invent features. Follow `TODO.md` literally.

## 2. CONTEXT & MEMORY (WHERE WE ARE EXACTLY)
*   **Git State:** We are on branch `main`. The next tag to push when a build is ready is `v3.0.0-beta13`.
*   **Code State:** The backend (V3) is perfect (Room DB, zero-OOM parsing, Ktor network evasion). However, the frontend (`tvApp` module) is broken. It is infected with Dagger Hilt annotations, 19 proxy stubs (`MockDomainModule.kt`), and monolithic ViewModels from a failed "StreamVault" transplant.
*   **Learnings & Concerns (Why we are here):** Previous agents suffered "prompt fatigue". They were asked to "build an IPTV app" and hallucinated massive architectures, resulting in a Frankenstein app that won't compile. We learned we CANNOT ask an AI to build UI and Backend logic at the same time.
*   **The Goal:** We want the visual aesthetics of StreamVault (deep black, crimson red, glassmorphism, D-Pad focus scaling) but we want it built as **100% Stateless "Dumb" Jetpack Compose functions** powered by our existing, lightweight V3 backend.

## 3. UNBREAKABLE RULES OF ENGAGEMENT
*   **RULE 1: NO DAGGER HILT.** You will delete anything related to Hilt. We are using standard Android ViewModels and manual DI for our Kotlin Multiplatform (KMP) future.
*   **RULE 2: STATELESS VISUAL CLONING.** When building a UI component (e.g., `LiveTvGrid`), it must NOT fetch data, query databases, or hold ViewModels. It MUST look like this: `@Composable fun LiveTvGrid(channels: List<ChannelEntity>, onZap: (ChannelEntity) -> Unit)`.
*   **RULE 3: THE ORCHESTRATOR.** A single Route file (e.g., `LiveTvRoute.kt`) will fetch the data from the ViewModel and pass it down to the stateless UI. 
*   **RULE 4: DESIGN SYSTEM ONLY.** You will use `DesignSystemTokens.kt` for all colors. Deep Black (`Background = 0xFF070709`) and Crimson Red (`Crimson = 0xFFE50914`).

## 4. DEPLOYMENT STRATEGY
*   **Build:** We use GitHub Actions (`.github/workflows/build.yml`). It compiles Mobile and TV APKs.
*   **OTA (Over The Air):** Vercel hosts `https://tvmime.vercel.app`. It automatically points `/tv.apk` to the latest GitHub Release asset. 
*   **Versioning:** Your commits must be Conventional (`feat:`, `fix:`). The Gradle script dynamically uses `git rev-list --count HEAD` for the Android Version Code.

**END OF BOOT SEQUENCE. PROCEED TO `IMPLEMENTATION_PLAN.md`.**

# AI Agent Context (TVMime) 🤖

> **Repository**: `https://github.com/Fragger7/tvmime.git`  
> **Canonical Root**: `/Users/admin/Development/tvmime`  
> **Production Web**: `https://tvmime.vercel.app`  

**ATTENTION ALL AI AGENTS:** Read this document before modifying the TVMime codebase.

## 1. Project Paradigm
- **Goal:** Build the fastest, most beautiful IPTV Player for Android TV and Android Mobile.
- **Architecture:** Kotlin Multiplatform (KMP). Do NOT write Android-specific dependencies in the `shared/` module. Use `Ktor` and Napier for cross-platform support.
- **UI Framework:** Jetpack Compose (Material 3) and `androidx.tv`. Do NOT use legacy XML fragments.

## 2. Design System Constraints
- **Visuals:** The app uses a strict "Deep Black & Crimson Red" color palette with flat translucency. 

## 3. The History & The Pivot (V2 -> V3)
- **The V2 Failure:** We attempted to transplant the `Sohva-TV` UI entirely over the TVMime backend. It resulted in a brittle "Frankenstein" architecture full of dependency conflicts (Coil2 vs Coil3, missing generic resources) that could not compile cleanly through the CI/CD pipeline.
- **The V3 Pivot:** We have aborted V2. We are now executing the **V3 Master Surgery**. We will measure a billion times and cut once. We will build a highly scalable, Dagger Hilt injected, dual-engine (ExoPlayer + libmpv) architecture with a local proxy for network evasion, based on the best concepts from `StreamVault`, `OwnTV`, and `IPTVMine-Pro`.

## 4. Next Agent Hand-Off: The Research Fleet
The immediate next step for any agent picking up this project is to execute the deep research mandate. You must use autonomous fanning to read the actual code execution paths of the open-source giants cloned in the `scratch/` directory.

### Exact Prompts for the User / Agent:
To restart this work, the User should run the `/teamwork-preview` or `/goal` command with the following explicit prompt:

```text
/goal (or /teamwork-preview)
I need to spin up a fleet of research agents to execute a deep, file-by-file static analysis of the three IPTV repositories cloned in /Users/admin/.gemini/antigravity-cli/brain/f0f5fec6-8870-43fc-930a-0ee6c7ee712b/scratch/repos/. 

Agent 1 (OwnTV): Read OwnTV's PlayerModule.kt and libmpv/ExoPlayer wrappers. Explain exactly how they hand off streams between the dual engines.
Agent 2 (StreamVault): Read StreamVault's database layer. Explain exactly how they use Kotlin Coroutines and Room to ingest 50MB M3U playlists without triggering Out-Of-Memory errors.
Agent 3 (IPTVMine-Pro): Read IPTVMine-Pro's OkHttp layer. Explain exactly how they spoof User-Agents and bypass HTTP redirects to evade provider blocks.

Consolidate these findings into a master document named TVMIME_V3_ARCHITECTURE_BLUEPRINT.md that proves technically how these three systems will be merged into TVMime using Dagger Hilt.
```

## 5. CI/CD Build Rules
- All compilation happens remotely via `.github/workflows/build.yml`.
- You MUST use Conventional Commits (`feat:`, `fix:`) so the pipeline bumps the version correctly.

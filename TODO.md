# TVMime Master Task List 📝

> **Status:** The V2 (Sohva-TV Transplant) has been formally aborted due to deep architectural incompatibility, brittle compilation states, and conflicting frameworks (Coil3 vs Coil2, Experimental Compose APIs, missing resources). We have pivoted to the **V3 Master Surgery**.

## Phase 6: V3 Master Surgery & Deep Architecture Research [ACTIVE SPRINT]
We are adopting a strict "Measure a Billion Times, Cut Once" philosophy.

- [ ] **Step 1: Spawning the Autonomous Research Fleet**
  - Run the exact `/teamwork-preview` or `/goal` command documented in `AGENTS.md` to trigger a massive, overnight deep-read of `OwnTV`, `IPTVMine-Pro`, and `StreamVault`.
  - The fleet must literally trace the execution path of their database ingestion (Room DB chunking), ExoPlayer buffering (`LoadControl`), and dual-engine integration (`libmpv`).
- [ ] **Step 2: Produce `TVMIME_V3_ARCHITECTURE_BLUEPRINT.md`**
  - Based on the team's findings, generate an uncompromising technical blueprint merging StreamVault's DB layer, OwnTV's dual-engine, and IPTVMine-Pro's lightweight network evasion.
- [ ] **Step 3: The Core Teardown & Rebuild**
  - Delete all `v2.0` UI code. Inject Dagger Hilt for modular DI. Build the Network Evasion Proxy and the Room Database ingestion flow.
- [ ] **Step 4: The Jetpack Compose UI Rebuild**
  - Write custom, lightweight Compose vectors tailored specifically for the TVMime "Deep Black & Crimson Red" brand. No more hijacked `R.drawable` resources.

## Phase 7: Rich VOD Integration (Movies & Series) [UPCOMING]
- [ ] Integrate `libmpv` natively to handle heavy `.mkv` files and obscure codecs.
- [ ] Implement TMDB API integration for posters and backdrops.
- [ ] Wire Trakt.tv API for cross-platform watch syncing.

## Phase 8: The Sports Hub [UPCOMING]
- [ ] Aggregate a 3rd-party sports API (e.g., TheSportsDB) against the local Room DB to build a dedicated, graphical "Live Now" sports overlay.

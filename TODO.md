# TVMime Master Surgery - Next Steps (StreamVault UI Transplant)

## The Strategic Pivot
After deploying a fleet of research agents to analyze OwnTV, IPTVMine-Pro, and StreamVault, we have decided to adopt the **StreamVault UI/UX** for TVMime V3.
- **Why:** The side-panel overlay design is incredibly modern and fits our vision best.
- **The Challenge:** StreamVault's UI is heavily coupled to its own domain models.
- **The Solution:** We will use the "Adapter/Strangler Fig" pattern. We will copy the UI, stub the domain models to get it compiling, and slowly wire it to our V3 Backend over multiple sprints.
- **The Prime Objective:** We are stripping away the 1-day constraint. Our immediate, singular focus is to get the **Live TV Ecosystem** (Cloud Sync Settings, 3-Tier Grid, EPG, and Player Controls) 100% functional before touching VOD or Catch-up.

## Upcoming Sprints
- [ ] **Sprint 16: The Great Extraction**
  - Copy StreamVault's `app/src/main/java/.../ui/` (specifically `theme`, `design`, `home`, `player`, `epg`).
  - Create `com.tvmime.tv.mocks` to stub the expected domain interfaces and ViewModels to resolve all compiler errors.
- [ ] **Sprint 17: UI Data Wiring (The Grid)**
  - Replace the mock Live TV ViewModels with our V3 `TvMainViewModel`.
  - Build an Adapter layer mapping our SQLite Room entities (`ChannelEntity`, `CategoryEntity`) into the data classes the StreamVault UI expects.
- [ ] **Sprint 18: Player & EPG Integration**
  - Hook the StreamVault Player Overlays into our `TvMimeVideoEngine` (ExoPlayer + libmpv dual-engine).
  - Wire the EPG grid UI to our Room EPG DAOs.

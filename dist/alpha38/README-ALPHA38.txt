Chainacobblemon 0.4.0-alpha.38 - Regional Layout Watchdog fix + manual placement bridge

Base: release/0.4.0-alpha.36 dist source (NOT alpha.37 - that branch is on a stale/incompatible
tree, see the note at the top of ci.sh).

Fix 1 - the alpha.36 crash (Watchdog / server hang):
- Root cause confirmed from the crash-report: RegionalLayoutService.tickPlan -> emergencyCandidate
  -> sampleTerrain -> World.getBlockState -> ServerChunkManager.getChunkBlocking, all on the main
  thread. The emergency fallback ran up to 24 attempts in a single tick, each of which could force
  a brand-new chunk to generate synchronously (candidates land in a mostly-ungenerated ring far
  from spawn). Enough of those in one tick blocked the main thread long enough to trip the Watchdog.
- Fix: the emergency fallback is now throttled to exactly one attempt per tick (evaluateEmergencyCandidate),
  the same cadence already used safely by the normal search phase (evaluateCandidate). Same search
  positions, same limits, just spread out instead of run in a single burst.

Fix 2 - build the structure yourself, skip the automatic search entirely:
- ImportantLocationService already supported "manuallyMarked" locations (via the existing
  /chainacobblemon admin structures mapmark <region> <number> command - stand at your own
  hand-built structure and mark it) but RegionalLayoutService ignored that flag and would still try
  to search/relocate it.
- Fix: RegionalLayoutService.buildItems() (and the matching overworld count) now skip any location
  that is manuallyMarked. Build a structure wherever you like within the target radius, mark it
  with mapmark, and Regional Layout leaves it alone - no search, no forced chunk generation, no risk
  of it being moved out from under you.

Everything else (target radius 9000, hard cap 10000, min separation 512, tick-throttled normal
search, chunk preload before physical placement, /place structure + StructureTemplateManager use,
alpha35/34/31 safeguards, stream bonus config) is unchanged from alpha.36.

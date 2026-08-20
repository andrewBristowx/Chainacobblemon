Chainacobblemon 0.4.0-alpha.31 - Watchdog-safe Chunky campaign pregeneration

Fix for crash-2026-08-20_04.34.26-server.txt:
- The watchdog showed the main server thread inside RegionalStructureAuditService.normalize()/String.replaceAll while SelectivePregenService rebuilt the 69-location plan from its END_SERVER_TICK loop.
- The Chunky worker itself was alive; the repeated Chaina audit was the primary watchdog stall.

Changes:
- The 69/69 pregeneration plan is now built once before starting/resuming the queue, never rebuilt every server tick.
- Persisted queues can lazily reconstruct the immutable plan once if needed after startup.
- RegionalStructureAuditService caches its audit result for the current server/resource-manager/structure-registry view.
- Structure candidates are normalized/indexed once per audit instead of once per candidate x expected-location comparison.
- Regex replaceAll whitespace normalization was removed and replaced with a small linear character normalizer.
- Chunky remains the generation engine: 256x256-block tiles, one Chunky task at a time, MSPT pause/resume, and 30 GiB free-space hard guard.
- Existing alpha30 progress file (campaign-chunky-pregen-v2-<seed>.json) is intentionally retained, so completed tiles are not lost.
- The server never auto-resumes generation after restart; use /chainacobblemon admin structures generate resume.

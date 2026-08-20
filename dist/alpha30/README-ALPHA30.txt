Chainacobblemon 0.4.0-alpha.30 - Chunky safe selective campaign pregeneration

- Replaces alpha.28 direct ServerWorld#getChunk pregeneration with a soft Chunky API integration.
- Requires Chunky Fabric on the server to run campaign pregeneration; Chainacobblemon still loads safely without it.
- Keeps the already validated 69/69 location map. Only registered/static locations are queued; the two LumyMon instance/template destinations remain dynamic.
- Splits each 2048x2048 location area (1024 block radius) into 256x256 block Chunky tiles (~16x16 chunks each).
- Only one Chunky task is started at a time across the whole Chaina queue.
- Uses Chunky concentric generation and lets Chunky skip already-generated chunks.
- Chaina measures server tick work and automatically pauses the current Chunky task around 45 MSPT; it resumes after sustained recovery below about 32 MSPT.
- Keeps async disk monitoring: warning at 45 GiB free, longer cooldown at 35 GiB, hard pause at 30 GiB free.
- Progress is persisted in a new v2 state file so the crashed alpha.28 direct-pregen cursor is never reused.
- Canonical commands remain /chainacobblemon admin structures generate <start|status|pause|resume|diskcap|reset>.
- A compatibility alias /chainacobblemon admin generate <start|status|pause|resume> is also registered.

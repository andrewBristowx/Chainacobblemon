Chainacobblemon 0.4.0-alpha.28 - Selective campaign pregeneration

- Keeps the validated alpha.25 trainer placement and alpha.27 69/69 multidimension planner.
- Adds selective pregeneration for the 67 registered/static campaign locations.
- Generates a 1024-block radius around each location instead of filling the enormous dimension bounding rectangles.
- The 2 template/dynamic locations remain verified by the 69/69 plan and are not mass-pregenerated.
- Progress is persisted per world seed and can be paused/resumed after restarts.
- Generation is center-out and intentionally throttled to one chunk request at a time.
- Disk allocation defaults to 146 GiB for the current HolyHosting allocation and is admin-configurable with `structures generate diskcap <GiB>`.
- Warns below 45 GiB free, throttles more below 35 GiB, and pauses automatically at 30 GiB free.
- Disk usage is measured asynchronously from the server game directory and combined conservatively with physical free space.
- No chunks/files are ever deleted automatically.

Commands:
/chainacobblemon admin structures generate
/chainacobblemon admin structures generate status
/chainacobblemon admin structures generate pause
/chainacobblemon admin structures generate resume
/chainacobblemon admin structures generate diskcap <GiB>
/chainacobblemon admin structures generate reset confirm

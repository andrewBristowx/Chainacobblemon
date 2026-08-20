Chainacobblemon 0.4.0-alpha.27 - Multidimension 69-location planner

- Keeps alpha.25 stable/separated generic dungeon trainer placement unchanged.
- Keeps alpha.26 multi-candidate and progressive-radius planner retries.
- Important-location searches now iterate every ServerWorld currently loaded by the server, including vanilla and custom Cobbleverse dimensions.
- Dimensions are discovered dynamically; no custom dimension ID is hardcoded.
- Search attempts are spread across ticks in dimension -> candidate ID -> radius order.
- Every located/manual location stores dimension ID plus X/Y/Z.
- The Chaina journal shows the saved dimension beside each location.
- mapstatus reports independent pregeneration rectangles per dimension instead of combining coordinates from unrelated worlds.
- Old alpha.23-alpha.26 saved locations without a dimension migrate as Overworld entries.

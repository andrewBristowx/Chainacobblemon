Chainacobblemon 0.4.0-alpha.26 - Complete 69-location planner retries

- Keeps alpha.25 stable/separated generic dungeon trainer placement unchanged.
- The 69-location planner now keeps every matching STRUCTURE registry candidate for each regional location.
- Missing registered locations are retried progressively at 4096, 6144, 8192 and 12288 chunks (65,536 to 196,608 blocks) from world spawn.
- Existing located coordinates are preserved; normal mapplan only retries unresolved registered locations.
- New command: /chainacobblemon admin structures mapmissing
  It prints every unresolved location, its regional order, candidate registry IDs, and which entries are template/assets requiring manual mapmark.
- mapstatus labels bounds as provisional while fewer than 69 locations have coordinates.
- The Chaina journal only exposes the final pregeneration rectangle after 69/69.
- mapplan no longer says the map is complete when a search round ends below 69/69.

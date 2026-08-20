Chainacobblemon 0.4.0-alpha.34 - Genuine RCT doubles for Lulita + Duber

Fixes alpha.33 showing "combate doble" but opening a singles battle.

Root cause:
- Alpha.33 supplied GEN_9_DOUBLES to a reflective launcher that passed Trainer objects.
- RCT's format-aware startBattle overload expects List<Trainer> for each side.
- That overload was skipped, then Chainacobblemon fell back to startSingle().

Fix:
- Lulita/Duber now use a dedicated doubles-only launch path.
- Preferred path calls RCT startBattle(List<Trainer>, List<Trainer>, GEN_9_DOUBLES, BattleRules).
- Compatibility fallback calls RCT startDouble(Trainer, Trainer, BattleRules), which also hard-codes GEN_9_DOUBLES.
- This path NEVER falls back to startSingle.
- The two visual NPCs, independent skins/dialogues, shared six-Pokemon roster and adaptive difficult AI are retained.

Expected in-game result:
- two active Pokemon per side;
- two player action selections per turn;
- targeting appropriate to a real doubles battle.

from pathlib import Path
import sys

root = Path(sys.argv[1])

# Version
p = root / "gradle.properties"
s = p.read_text()
old = "mod_version=0.3.0-alpha.19+1.21.1"
assert old in s, "alpha.19 version marker missing"
p.write_text(s.replace(old, "mod_version=0.3.0-alpha.20+1.21.1", 1))

# Delay Treasure cleanup/return until the result screen/roulette has finished.
p = root / "src/main/java/com/andrewbristowx/chainacobblemon/events/ChainaEventManager.java"
s = p.read_text()
old = '''    public static void stop(MinecraftServer server, String reason) {
        if (active == null) return;
        TreasureHuntService.cleanup(server, false);
'''
new = '''    public static void stop(MinecraftServer server, String reason) {
        if (active == null) return;
        // Treasure players must only be returned once the results/reward presentation has finished.
        // Returning them while the roulette screen is opening can leave the client visually stranded
        // in the temporary dimension with no chunks rendered.
        TreasureHuntService.cleanup(server, active.type == ChainaEventType.TREASURE);
'''
assert old in s, "stop cleanup marker missing"
s = s.replace(old, new, 1)
old = '''        TreasureHuntService.cleanup(server, true);
        refreshHud(server);
    }

    private static void updateEventStats'''
new = '''        // Do not teleport/clear Treasure here. The winner roulette is opened above and needs a stable
        // world for its full animation. stop() runs after the results phase and performs the safe return.
        refreshHud(server);
    }

    private static void updateEventStats'''
assert old in s, "finish cleanup marker missing"
s = s.replace(old, new, 1)
p.write_text(s)

# Preload the destination chunk before cross-dimension return.
p = root / "src/main/java/com/andrewbristowx/chainacobblemon/events/treasure/TreasureHuntService.java"
s = p.read_text()
old = '''    private static void returnPlayer(ServerPlayerEntity player, PlayerRun run) {
        Identifier id = run.returnWorld;
        ServerWorld target = player.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
        if (target == null) target = player.getServer().getOverworld();
        player.teleport(target, run.returnX, run.returnY, run.returnZ, run.returnYaw, run.returnPitch);
        player.changeGameMode(run.returnCreative ? GameMode.CREATIVE : GameMode.SURVIVAL);
    }
'''
new = '''    private static void returnPlayer(ServerPlayerEntity player, PlayerRun run) {
        Identifier id = run.returnWorld;
        ServerWorld target = player.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
        if (target == null) {
            Chainacobblemon.LOGGER.warn("Treasure return world {} was unavailable for {}; using overworld fallback", id, run.playerName);
            target = player.getServer().getOverworld();
        }
        // Force the destination chunk to be present before the cross-dimension teleport packet is sent.
        // This avoids the client spending the post-reward transition looking at the Treasure dimension fog/void.
        int chunkX = ((int)Math.floor(run.returnX)) >> 4;
        int chunkZ = ((int)Math.floor(run.returnZ)) >> 4;
        target.getChunk(chunkX, chunkZ);
        player.teleport(target, run.returnX, run.returnY, run.returnZ, run.returnYaw, run.returnPitch);
        player.changeGameMode(run.returnCreative ? GameMode.CREATIVE : GameMode.SURVIVAL);
        Chainacobblemon.LOGGER.info("Treasure return: {} -> {} @ {} {} {}",
                run.playerName, target.getRegistryKey().getValue(), run.returnX, run.returnY, run.returnZ);
    }
'''
assert old in s, "returnPlayer marker missing"
p.write_text(s.replace(old, new, 1))

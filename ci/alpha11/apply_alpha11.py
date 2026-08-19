from pathlib import Path
import json

root = Path("/tmp/chainacobblemon")
tower = root / "src/main/java/com/andrewbristowx/chainacobblemon/tower/ChallengeTowerService.java"
npc = root / "src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java"
gradle = root / "gradle.properties"
sounds = root / "src/main/resources/assets/chainacobblemon/sounds.json"

s = tower.read_text(encoding="utf-8")
replacements = [
(
"""        RctCobbleverseBridge.LevelCapStatus cap = RctCobbleverseBridge.levelCap(player);
        if (cap.available() && cap.cap() > 0) target = Math.min(target, cap.cap());
        return target;
""",
"""        // Tower opponents mirror the player's real party level, independent of Cobbleverse's
        // regional RCT cap. The Tower only unlocks after the regional campaign; admin test mode
        // must also be able to validate scaling with any party.
        return target;
"""
),
(
"""            if (session.bossMusic && serverTicks >= session.nextMusicTick) {
                playBossMusic(player);
                session.nextMusicTick = serverTicks + 1_160L;
            }
""",
"""            if (session.towerMusic && session.trainer != null) {
                if (serverTicks >= session.nextSuppressMusicTick) {
                    suppressCompetingBattleMusic(player);
                    session.nextSuppressMusicTick = serverTicks + 10L;
                }
                if (serverTicks >= session.nextMusicTick) {
                    playTowerMusic(player);
                    session.nextMusicTick = serverTicks + 1_160L;
                }
            }
"""
),
(
"""        if (floor == 10) {
            showTitle(player, "MAESTRO DE LA TORRE", displayName, 5, 45, 10);
            session.bossMusic = true;
            session.nextMusicTick = serverTicks + 1_160L;
            playBossMusic(player);
        }
        player.sendMessage(Text.literal("§d⚔ " + displayName + " §7· Piso " + floor + "/10 · equipo adaptado ~Nv." + targetLevel(player, floor)), false);
        if (!NpcBattleService.start(player, npc)) {
            removeTrainer(player.getUuid());
            player.sendMessage(Text.literal("§eNo se pudo iniciar el combate. Puedes volver a intentarlo desde la Profesora Chaina."), false);
            returnPlayer(player, false);
        }
""",
"""        if (floor == 10) {
            showTitle(player, "MAESTRO DE LA TORRE", displayName, 5, 45, 10);
        }
        player.sendMessage(Text.literal("§d⚔ " + displayName + " §7· Piso " + floor + "/10 · rival adaptado ~Nv." + targetLevel(player, floor)), false);
        if (!NpcBattleService.start(player, npc)) {
            removeTrainer(player.getUuid());
            player.sendMessage(Text.literal("§eNo se pudo iniciar el combate. Puedes volver a intentarlo desde la Profesora Chaina."), false);
            returnPlayer(player, false);
        }
        // RCT/Cobbleverse starts its own trainer music client-side as the battle opens.
        // Keep Badge Rush in MASTER while repeatedly silencing the common MUSIC/RECORDS
        // categories used by battle soundtrack packs.
        session.towerMusic = true;
        session.nextMusicTick = serverTicks + 1_160L;
        session.nextSuppressMusicTick = serverTicks;
        suppressCompetingBattleMusic(player);
        playTowerMusic(player);
"""
),
(
"""    private static void playBossMusic(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        world.playSound(null, player.getBlockPos(), ModRegistries.BADGE_RUSH, SoundCategory.MUSIC, 1.0F, 1.0F);
    }

    private static void stopBossMusic(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new StopSoundS2CPacket(Identifier.of(Chainacobblemon.MOD_ID, "badge_rush"), SoundCategory.MUSIC));
        RuntimeSession session = SESSIONS.get(player.getUuid());
        if (session != null) session.bossMusic = false;
    }
""",
"""    private static void suppressCompetingBattleMusic(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new StopSoundS2CPacket(null, SoundCategory.MUSIC));
        player.networkHandler.sendPacket(new StopSoundS2CPacket(null, SoundCategory.RECORDS));
    }

    private static void playTowerMusic(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        world.playSound(null, player.getBlockPos(), ModRegistries.BADGE_RUSH, SoundCategory.MASTER, 4.0F, 1.0F);
    }

    private static void stopTowerMusic(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new StopSoundS2CPacket(Identifier.of(Chainacobblemon.MOD_ID, "badge_rush"), SoundCategory.MASTER));
        RuntimeSession session = SESSIONS.get(player.getUuid());
        if (session != null) {
            session.towerMusic = false;
            session.nextMusicTick = 0L;
            session.nextSuppressMusicTick = 0L;
        }
    }
"""
),
(
"""        boolean bossMusic;
        long nextMusicTick;
""",
"""        boolean towerMusic;
        long nextMusicTick;
        long nextSuppressMusicTick;
"""
)
]
for old,new in replacements:
    if old not in s:
        raise SystemExit("alpha11 tower replacement not found:\n" + old[:180])
    s=s.replace(old,new)
s=s.replace("stopBossMusic(player);","stopTowerMusic(player);")
tower.write_text(s,encoding="utf-8")

n=npc.read_text(encoding="utf-8")
old="""            BattleLaunchConfig launchConfig;
            if (DungeonTrainerService.isDungeonTrainer(npc) || ChallengeTowerService.isTowerTrainer(npc)) {
                try {
                    int target = ChallengeTowerService.isTowerTrainer(npc)
                            ? ChallengeTowerService.targetLevel(player)
                            : DungeonTrainerService.targetLevel(player, npc);
                    launchConfig = dungeonLevelSyncConfig(npcTrainer, target);
                } catch (ReflectiveOperationException | RuntimeException syncFailure) {
                    Chainacobblemon.LOGGER.error("Adaptive level synchronization could not be prepared; battle blocked for safety", syncFailure);
                    player.sendMessage(net.minecraft.text.Text.literal("§cSincronización de nivel no está disponible con esta combinación de RCT/Cobblemon. "
                            + "§7El combate se bloqueó para no modificar tus Pokémon reales."), false);
                    return false;
                }
            } else {
                launchConfig = BattleLaunchConfig.fromTrainer(npcTrainer);
            }
"""
new="""            BattleLaunchConfig launchConfig;
            if (DungeonTrainerService.isDungeonTrainer(npc)) {
                try {
                    int target = DungeonTrainerService.targetLevel(player, npc);
                    launchConfig = dungeonLevelSyncConfig(npcTrainer, target);
                } catch (ReflectiveOperationException | RuntimeException syncFailure) {
                    Chainacobblemon.LOGGER.error("Adaptive level synchronization could not be prepared; battle blocked for safety", syncFailure);
                    player.sendMessage(net.minecraft.text.Text.literal("§cSincronización de nivel no está disponible con esta combinación de RCT/Cobblemon. "
                            + "§7El combate se bloqueó para no modificar tus Pokémon reales."), false);
                    return false;
                }
            } else {
                // The Challenge Tower scales the NPC team to the player's real party.
                // Do not normalize the player's battle clones downward: the visible enemy
                // levels should be the adaptation, exactly as the Tower design intends.
                launchConfig = BattleLaunchConfig.fromTrainer(npcTrainer);
            }
"""
if old not in n:
    raise SystemExit("alpha11 NpcBattleService launch replacement not found")
n=n.replace(old,new)

old="""                } else if (ChallengeTowerService.isTowerTrainer(npc)) {
                    player.sendMessage(net.minecraft.text.Text.literal("§d🏰 Torre adaptativa · §fSincronización temporal ~Nv. "
                            + ChallengeTowerService.targetLevel(player) + "§7."), false);
"""
new="""                } else if (ChallengeTowerService.isTowerTrainer(npc)) {
                    player.sendMessage(net.minecraft.text.Text.literal("§d🏰 Torre adaptativa · §fRival generado alrededor de Nv. "
                            + ChallengeTowerService.targetLevel(player) + "§7 · tu equipo conserva sus niveles reales."), false);
"""
if old not in n:
    raise SystemExit("alpha11 NpcBattleService message replacement not found")
n=n.replace(old,new)
npc.write_text(n,encoding="utf-8")

g=gradle.read_text(encoding="utf-8")
if "mod_version=0.3.0-alpha.10+1.21.1" not in g:
    raise SystemExit("alpha10 version missing")
gradle.write_text(g.replace("mod_version=0.3.0-alpha.10+1.21.1","mod_version=0.3.0-alpha.11+1.21.1"),encoding="utf-8")

j=json.loads(sounds.read_text(encoding="utf-8"))
j["badge_rush"]["subtitle"]="Torre Desafío: combate"
sounds.write_text(json.dumps(j,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

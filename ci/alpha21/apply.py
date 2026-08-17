from pathlib import Path

root = Path('/tmp/chainacobblemon')
props = root / 'gradle.properties'
text = props.read_text(encoding='utf-8')
text = text.replace('mod_version=0.3.0-alpha.20+1.21.1', 'mod_version=0.3.0-alpha.21+1.21.1')
if 'mod_version=0.3.0-alpha.21+1.21.1' not in text:
    raise SystemExit('failed to update version')
props.write_text(text, encoding='utf-8')

path = root / 'src/main/java/com/andrewbristowx/chainacobblemon/events/treasure/TreasureHuntService.java'
s = path.read_text(encoding='utf-8')

old = '''        run.progress = Math.max(run.progress, index);\n        openGate(player.getServerWorld(), run, index);\n        ChainaEventManager.onTreasureProgress(player, run.progress, eventRun.plan.trainerCells.size());\n'''
new = '''        run.progress = Math.max(run.progress, index);\n        openGate(player.getServerWorld(), run, index);\n        // A defeated corridor trainer must disappear immediately. Keeping old NPCs in the maze\n        // both clutters the route and can reveal/checkpoint the solved path. Clean its temporary\n        // battle Pokemon first so no drops/XP can leak, then discard only this trainer.\n        cleanupTrainerPokemon(player, npc);\n        UUID defeatedNpcId = npc.getUuid();\n        npc.discard();\n        run.trainerIndexByNpc.remove(defeatedNpcId);\n        ChainaEventManager.onTreasureProgress(player, run.progress, eventRun.plan.trainerCells.size());\n'''
if old not in s:
    raise SystemExit('victory anchor not found')
s = s.replace(old, new, 1)

old = '''            npc.setCustomName(Text.literal((i == total ? "Guardián del Tesoro" : "Entrenador del Pasillo") + " · " + i + "/" + total));\n            npc.setCustomNameVisible(false);\n'''
new = '''            // Treasure is a maze race: any overhead label is a wallhack. Keep the entity custom name\n            // empty so vanilla/client renderers have nothing to draw through walls; NpcBattleService\n            // supplies the proper battle-facing trainer name from the Treasure role instead.\n            npc.setCustomName(Text.empty());\n            npc.setCustomNameVisible(false);\n'''
if old not in s:
    raise SystemExit('trainer name anchor not found')
s = s.replace(old, new, 1)

old = '''        String display = "summon minecraft:item_display " + (p.getX()+.5) + " " + (p.getY()+1.35) + " " + (p.getZ()+.5)\n                + " {Tags:[\\\"" + DISPLAY_TAG + "\\\",\\\"" + ownerTag + "\\\"],item:{id:\\\"" + itemId + "\\\",count:1},billboard:\\\"fixed\\\",transformation:{scale:[3.4f,3.4f,3.4f]},interpolation_duration:8}";\n        String interaction = "summon minecraft:interaction " + (p.getX()+.5) + " " + p.getY() + " " + (p.getZ()+.5)\n                + " {Tags:[\\\"" + INTERACTION_TAG + "\\\",\\\"" + ownerTag + "\\\"],width:3.2f,height:3.2f,response:1b}";\n        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), display);\n        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), interaction);\n        Box find = new Box(p).expand(3.0D);\n        for (Entity entity : world.getOtherEntities(null, find, e -> e.getCommandTags().contains(INTERACTION_TAG) && e.getCommandTags().contains(ownerTag))) {\n            run.interactionId = entity.getUuid();\n            break;\n        }\n        world.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, p.getX()+.5, p.getY()+1.2, p.getZ()+.5, 20, .8, 1, .8, .03);\n'''
new = '''        // Server command sources default to the overworld. In alpha.20 the summon commands therefore\n        // created the display/interaction at the same coordinates in the wrong dimension. Explicitly\n        // execute inside treasure_maze and verify both entities exist before the player can reach the end.\n        world.getChunk(p.getX() >> 4, p.getZ() >> 4);\n        String prefix = "execute in " + WORLD_ID + " run ";\n        String display = prefix + "summon minecraft:item_display " + (p.getX()+.5) + " " + (p.getY()+1.35) + " " + (p.getZ()+.5)\n                + " {Tags:[\\\"" + DISPLAY_TAG + "\\\",\\\"" + ownerTag + "\\\"],item:{id:\\\"" + itemId + "\\\",count:1},billboard:\\\"fixed\\\",transformation:{scale:[3.4f,3.4f,3.4f]},interpolation_duration:8}";\n        String interaction = prefix + "summon minecraft:interaction " + (p.getX()+.5) + " " + p.getY() + " " + (p.getZ()+.5)\n                + " {Tags:[\\\"" + INTERACTION_TAG + "\\\",\\\"" + ownerTag + "\\\"],width:3.2f,height:3.2f,response:1b}";\n        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), display);\n        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), interaction);\n        Box find = new Box(p).expand(3.0D);\n        boolean foundDisplay = false;\n        boolean foundInteraction = false;\n        for (Entity entity : world.getOtherEntities(null, find, e -> e.getCommandTags().contains(ownerTag))) {\n            if (entity.getCommandTags().contains(DISPLAY_TAG)) foundDisplay = true;\n            if (entity.getCommandTags().contains(INTERACTION_TAG)) {\n                run.interactionId = entity.getUuid();\n                foundInteraction = true;\n            }\n        }\n        if (!foundDisplay || !foundInteraction) {\n            Chainacobblemon.LOGGER.error("Treasure ball spawn verification failed for {} in {} at {}: display={} interaction={}",\n                    run.playerName, WORLD_ID, p, foundDisplay, foundInteraction);\n        } else {\n            Chainacobblemon.LOGGER.info("Treasure ball ready for {}: {} at {} in {}", run.playerName, itemId, p, WORLD_ID);\n        }\n        world.spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, p.getX()+.5, p.getY()+1.2, p.getZ()+.5, 20, .8, 1, .8, .03);\n'''
if old not in s:
    raise SystemExit('ball spawn anchor not found')
s = s.replace(old, new, 1)

path.write_text(s, encoding='utf-8')

battle = root / 'src/main/java/com/andrewbristowx/chainacobblemon/npc/NpcBattleService.java'
b = battle.read_text(encoding='utf-8')
old = '''                String trainerName = npc.getCustomName() == null ? npc.npcId() : npc.getCustomName().getString();\n                npcTrainer = trainerNpcClass.getConstructor(String.class, pokemonArrayClass, trainerBagClass,\n'''
new = '''                boolean treasureBattleName = npc.dungeonStructureKey() != null && npc.dungeonStructureKey().startsWith("treasure_event:");\n                String trainerName = treasureBattleName\n                        ? ("guardian".equalsIgnoreCase(npc.dungeonRole()) ? "Guardián del Tesoro" : "Entrenador del Pasillo")\n                        : npc.getCustomName() == null ? npc.npcId() : npc.getCustomName().getString();\n                npcTrainer = trainerNpcClass.getConstructor(String.class, pokemonArrayClass, trainerBagClass,\n'''
if old not in b:
    raise SystemExit('battle name anchor not found')
b = b.replace(old, new, 1)
battle.write_text(b, encoding='utf-8')

print('alpha21 patch applied')

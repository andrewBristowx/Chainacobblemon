from pathlib import Path

root = Path('/tmp/chainacobblemon')

p = root / 'gradle.properties'
s = p.read_text()
assert 'mod_version=0.3.0-alpha.15+1.21.1' in s
p.write_text(s.replace('mod_version=0.3.0-alpha.15+1.21.1', 'mod_version=0.3.0-alpha.16+1.21.1'))

p = root / 'src/main/java/com/andrewbristowx/chainacobblemon/events/ChainaEventManager.java'
s = p.read_text()
old = '''        if (active != null) {\n            if (admin != null) admin.sendMessage(Text.literal("§cYa hay un Evento Chaina activo: " + active.type.displayName), false);\n            return false;\n        }\n        long now = System.currentTimeMillis();'''
new = '''        if (active != null) {\n            if (admin != null) admin.sendMessage(Text.literal("§cYa hay un Evento Chaina activo: " + active.type.displayName), false);\n            return false;\n        }\n        // A manual Treasure Hunt test can leave a temporary session/protection behind.\n        // Never let that leak into Fishing/Safari/Mining/Beauty. The placed structure itself is kept.\n        if (type != ChainaEventType.TREASURE) TreasureHuntService.cleanup(server, true);\n        long now = System.currentTimeMillis();'''
assert old in s
s = s.replace(old, new)
old = '''        if (active.type == ChainaEventType.TREASURE) {\n            if (!TreasureHuntService.start(server, active.test)) {\n                finish(server, "No se pudo preparar una estructura compatible");\n                return;\n            }\n        }'''
new = '''        if (active.type == ChainaEventType.TREASURE) {\n            boolean prepared;\n            try {\n                prepared = TreasureHuntService.start(server, active.test);\n            } catch (Exception e) {\n                Chainacobblemon.LOGGER.error("Treasure Hunt failed to prepare safely; cancelling event instead of crashing the server", e);\n                TreasureHuntService.cleanup(server, true);\n                prepared = false;\n            }\n            if (!prepared) {\n                finish(server, "No se pudo preparar una estructura compatible");\n                return;\n            }\n        }'''
assert old in s
p.write_text(s.replace(old, new))

p = root / 'src/main/java/com/andrewbristowx/chainacobblemon/events/treasure/TreasureHuntService.java'
s = p.read_text()
old = '''        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->\n                !insideActive(world, pos) || ADMIN_BUILD.contains(player.getUuid()));\n        UseBlockCallback.EVENT.register((player, world, hand, hit) ->\n                insideActive(world, hit.getBlockPos()) && !ADMIN_BUILD.contains(player.getUuid()) ? ActionResult.FAIL : ActionResult.PASS);'''
new = '''        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->\n                !shouldProtect(world, pos) || ADMIN_BUILD.contains(player.getUuid()));\n        UseBlockCallback.EVENT.register((player, world, hand, hit) ->\n                shouldProtect(world, hit.getBlockPos()) && !ADMIN_BUILD.contains(player.getUuid()) ? ActionResult.FAIL : ActionResult.PASS);'''
assert old in s
s = s.replace(old, new)
s = s.replace('            BlockPos p = trainerPos(i);', '            BlockPos p = trainerPos(world, i);')
s = s.replace('        else if ("room".equalsIgnoreCase(target)) p = trainerPos(Math.max(1, Math.min(session.trainerCount, room)));', '        else if ("room".equalsIgnoreCase(target)) p = trainerPos(admin.getServer().getOverworld(), Math.max(1, Math.min(session.trainerCount, room)));')
old = '''    private static BlockPos trainerPos(int index) {\n        if (session != null && session.trainerPositions.containsKey(index)) return session.trainerPositions.get(index);\n        int[][] off = {{-22,0},{-12,10},{-4,-12},{8,10},{18,-6},{0,18},{22,12}};\n        int[] o = off[(Math.max(1,index)-1) % off.length];\n        return findSafe(activeWorld(), session.anchor.add(o[0], 0, o[1]));\n    }\n\n    private static ServerWorld activeWorld() {\n        if (session == null || Chainacobblemon.playerDataManager() == null) return null;\n        // Callers needing this method are always on a live server thread; trainer positions are normally cached.\n        throw new IllegalStateException("Uncached trainer position outside live-world context");\n    }'''
new = '''    private static BlockPos trainerPos(ServerWorld world, int index) {\n        if (session != null && session.trainerPositions.containsKey(index)) return session.trainerPositions.get(index);\n        if (session == null) return world == null ? BlockPos.ORIGIN : world.getSpawnPos();\n        int[][] off = {{-22,0},{-12,10},{-4,-12},{8,10},{18,-6},{0,18},{22,12}};\n        int[] o = off[(Math.max(1,index)-1) % off.length];\n        return findSafe(world, session.anchor.add(o[0], 0, o[1]));\n    }'''
assert old in s
s = s.replace(old, new)
old = '''    private static boolean insideActive(net.minecraft.world.World world, BlockPos p) {\n        if (session == null || world == null || p == null) return false;\n        if (!world.getRegistryKey().getValue().toString().equals(session.worldId)) return false;\n        return Math.abs(p.getX()-session.anchor.getX()) <= PROTECTION_RADIUS && Math.abs(p.getZ()-session.anchor.getZ()) <= PROTECTION_RADIUS\n                && Math.abs(p.getY()-session.anchor.getY()) <= 72;\n    }'''
new = '''    private static boolean shouldProtect(net.minecraft.world.World world, BlockPos p) {\n        // Protection belongs only to the live Treasure Hunt. A stale/manual Treasure session must never\n        // block normal gameplay such as Fishing, Safari, Mining or Beauty events.\n        return ChainaEventManager.isActive(com.andrewbristowx.chainacobblemon.events.ChainaEventType.TREASURE) && insideActive(world, p);\n    }\n\n    private static boolean insideActive(net.minecraft.world.World world, BlockPos p) {\n        if (session == null || world == null || p == null) return false;\n        if (!world.getRegistryKey().getValue().toString().equals(session.worldId)) return false;\n        return Math.abs(p.getX()-session.anchor.getX()) <= PROTECTION_RADIUS && Math.abs(p.getZ()-session.anchor.getZ()) <= PROTECTION_RADIUS\n                && Math.abs(p.getY()-session.anchor.getY()) <= 72;\n    }'''
assert old in s
p.write_text(s.replace(old, new))

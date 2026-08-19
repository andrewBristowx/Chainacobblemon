from pathlib import Path

src = Path('/tmp/chainacobblemon/src/main/java/com/andrewbristowx/chainacobblemon/events/FishingMinigameService.java')
text = src.read_text(encoding='utf-8')

text = text.replace('import com.cobblemon.mod.common.api.events.battles.BattleStartedPreEvent;\n', '', 1)

old_sub = '        CobblemonEvents.BATTLE_STARTED_PRE.subscribe((Consumer<BattleStartedPreEvent>) FishingMinigameService::onBattleStarted);\n'
new_sub = '        subscribeBattleSuppression();\n'
if old_sub not in text:
    raise SystemExit('typed BATTLE_STARTED_PRE subscription not found')
text = text.replace(old_sub, new_sub, 1)

old_method = '''    private static void onBattleStarted(BattleStartedPreEvent event) {
        try {
            Object battle = reflected(event, "getBattle");
            Object playerIdsValue = reflected(battle, "getPlayerUUIDs");
            if (!(playerIdsValue instanceof Iterable<?> playerIds)) return;
            long now = System.currentTimeMillis();
            for (Object value : playerIds) {
                if (!(value instanceof UUID playerId) || !shouldSuppressBattle(playerId, now)) continue;
                event.cancel();
                Session session = SESSIONS.get(playerId);
                Chainacobblemon.LOGGER.info("Canceled Cobblemon auto-battle owned by Chaina fishing: player={} session={} pokemon={}",
                        playerId, session == null ? "grace" : session.id, session == null ? "unknown" : session.speciesId);
                return;
            }
        } catch (RuntimeException exception) {
            Chainacobblemon.LOGGER.warn("Could not inspect Cobblemon battle start for fishing suppression: {}", exception.toString());
        }
    }

'''
new_method = '''    private static void subscribeBattleSuppression() {
        try {
            Object observable = CobblemonEvents.class.getField("BATTLE_STARTED_PRE").get(null);
            Consumer<Object> handler = FishingMinigameService::onBattleStarted;
            for (Method method : observable.getClass().getMethods()) {
                if (!method.getName().equals("subscribe") || method.getParameterCount() != 1) continue;
                if (!method.getParameterTypes()[0].isAssignableFrom(handler.getClass())) continue;
                method.invoke(observable, handler);
                return;
            }
            throw new NoSuchMethodException("Compatible BATTLE_STARTED_PRE#subscribe(Consumer) method not found");
        } catch (ReflectiveOperationException exception) {
            Chainacobblemon.LOGGER.error("Could not subscribe Chaina fishing battle suppression", exception);
        }
    }

    private static void onBattleStarted(Object event) {
        try {
            Object battle = reflected(event, "getBattle");
            Object playerIdsValue = reflected(battle, "getPlayerUUIDs");
            if (!(playerIdsValue instanceof Iterable<?> playerIds)) return;
            long now = System.currentTimeMillis();
            for (Object value : playerIds) {
                if (!(value instanceof UUID playerId) || !shouldSuppressBattle(playerId, now)) continue;
                if (!cancelEvent(event)) return;
                Session session = SESSIONS.get(playerId);
                Chainacobblemon.LOGGER.info("Canceled Cobblemon auto-battle owned by Chaina fishing: player={} session={} pokemon={}",
                        playerId, session == null ? "grace" : session.id, session == null ? "unknown" : session.speciesId);
                return;
            }
        } catch (RuntimeException exception) {
            Chainacobblemon.LOGGER.warn("Could not inspect Cobblemon battle start for fishing suppression: {}", exception.toString());
        }
    }

    private static boolean cancelEvent(Object event) {
        if (event == null) return false;
        try {
            Method cancel = event.getClass().getMethod("cancel");
            cancel.invoke(event);
            return true;
        } catch (ReflectiveOperationException exception) {
            Chainacobblemon.LOGGER.warn("Could not cancel Cobblemon fishing auto-battle: {}", exception.toString());
            return false;
        }
    }

'''
if old_method not in text:
    raise SystemExit('typed onBattleStarted method not found')
text = text.replace(old_method, new_method, 1)

src.write_text(text, encoding='utf-8')
print('Applied alpha.27 Fabric reflection compatibility hook')

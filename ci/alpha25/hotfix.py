from pathlib import Path

root = Path('/tmp/chainacobblemon')
src = root / 'src/main/java/com/andrewbristowx/chainacobblemon/events/FishingMinigameService.java'
props = root / 'gradle.properties'

text = src.read_text(encoding='utf-8')
old = '''    private static Object resolveBall(Identifier ballId) throws ReflectiveOperationException {
        String path = ballId == null ? "poke_ball" : ballId.getPath();
        String fieldName = path.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        Class<?> pokeBallsClass = Class.forName("com.cobblemon.mod.common.api.pokeball.PokeBalls");
        Object instance = null;
        try { instance = pokeBallsClass.getField("INSTANCE").get(null); } catch (ReflectiveOperationException ignored) { }
        try {
            java.lang.reflect.Field field = pokeBallsClass.getField(fieldName);
            return java.lang.reflect.Modifier.isStatic(field.getModifiers()) ? field.get(null) : field.get(instance);
        } catch (NoSuchFieldException missing) {
            java.lang.reflect.Field fallback = pokeBallsClass.getField("POKE_BALL");
            return java.lang.reflect.Modifier.isStatic(fallback.getModifiers()) ? fallback.get(null) : fallback.get(instance);
        }
    }
'''
new = '''    private static Object resolveBall(Identifier ballId) throws ReflectiveOperationException {
        Class<?> pokeBallsClass = Class.forName("com.cobblemon.mod.common.api.pokeball.PokeBalls");

        // Cobblemon 1.7.3 exposes its data registry through the JVM-static
        // getPokeBall(ResourceLocation) method. The Kotlin POKE_BALL property itself is
        // not a public Java field, so reflective field lookup cannot resolve it.
        if (ballId != null) {
            for (Method method : pokeBallsClass.getMethods()) {
                if (!method.getName().equals("getPokeBall") || method.getParameterCount() != 1) continue;
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
                if (!method.getParameterTypes()[0].isInstance(ballId)) continue;
                Object value = method.invoke(null, ballId);
                if (value != null) return value;
            }
        }

        // Safe vanilla fallback. In 1.7.3 @get:JvmName("getPokeBall") exposes the
        // standard Poke Ball as a zero-argument static getter.
        for (Method method : pokeBallsClass.getMethods()) {
            if (!method.getName().equals("getPokeBall") || method.getParameterCount() != 0) continue;
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
            Object value = method.invoke(null);
            if (value != null) {
                if (ballId != null && !"poke_ball".equals(ballId.getPath())) {
                    Chainacobblemon.LOGGER.warn("Fishing ball {} was not found in Cobblemon registry; using cobblemon:poke_ball fallback", ballId);
                }
                return value;
            }
        }

        // Backwards-compatibility fallback exposed by Cobblemon until 1.8.
        Object instance = null;
        try { instance = pokeBallsClass.getField("INSTANCE").get(null); } catch (ReflectiveOperationException ignored) { }
        for (Method method : pokeBallsClass.getMethods()) {
            if (!method.getName().equals("getPOKE_BALL") || method.getParameterCount() != 0) continue;
            Object target = java.lang.reflect.Modifier.isStatic(method.getModifiers()) ? null : instance;
            if (target == null && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
            Object value = method.invoke(target);
            if (value != null) return value;
        }

        throw new NoSuchMethodException("Cobblemon 1.7.3 PokeBalls#getPokeBall API was not found");
    }
'''

if old not in text:
    raise SystemExit('alpha.24 resolveBall implementation was not found; refusing unsafe patch')
text = text.replace(old, new, 1)
src.write_text(text, encoding='utf-8')

properties = props.read_text(encoding='utf-8')
old_version = 'mod_version=0.3.0-alpha.24+1.21.1'
new_version = 'mod_version=0.3.0-alpha.25+1.21.1'
if old_version not in properties:
    raise SystemExit('alpha.24 mod_version was not found')
props.write_text(properties.replace(old_version, new_version, 1), encoding='utf-8')

print('Applied alpha.25 fishing capture resolver hotfix')

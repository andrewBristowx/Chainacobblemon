from pathlib import Path

root = Path('/tmp/chainacobblemon')
props = root / 'gradle.properties'
main = root / 'src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java'
screen = root / 'src/client/java/com/andrewbristowx/chainacobblemon/client/twitch/TwitchScreen.java'

if not props.exists() or 'mod_version=0.4.0-alpha.2+1.21.1' not in props.read_text(encoding='utf-8'):
    raise SystemExit('0.4.0-alpha.2 baseline not found; refusing unsafe alpha.3 patch')

screen_text = screen.read_text(encoding='utf-8')
required = [
    'public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta)',
    'public void renderInGameBackground(DrawContext context)',
    'protected void applyBlur(float delta)',
    'context.fill(0, 0, width, height, 0x22000000)',
    'ConfirmLinkScreen.open',
    'Copiar link',
    'Ver stream',
]
for marker in required:
    if marker not in screen_text:
        raise SystemExit(f'missing alpha.3 TwitchScreen marker: {marker}')

# These overrides must remain empty/no-op except for comments. The exact bodies are
# checked so a future template update cannot silently re-enable vanilla blur.
for body in [
    '''public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {\n        // Intentionally empty. render() draws our non-blurred dim layer instead.\n    }''',
    '''public void renderInGameBackground(DrawContext context) {\n        // Intentionally empty. Avoid a second vanilla in-world background pass.\n    }''',
    '''protected void applyBlur(float delta) {\n        // Intentionally empty. This screen must never activate the vanilla blur shader.\n    }''',
]:
    if body not in screen_text:
        raise SystemExit('alpha.3 no-blur override body changed unexpectedly')

props.write_text(props.read_text(encoding='utf-8').replace(
    'mod_version=0.4.0-alpha.2+1.21.1',
    'mod_version=0.4.0-alpha.3+1.21.1', 1), encoding='utf-8')

main_text = main.read_text(encoding='utf-8')
old = 'public static final String VERSION = "0.4.0-alpha.2+1.21.1";'
new = 'public static final String VERSION = "0.4.0-alpha.3+1.21.1";'
if old not in main_text:
    raise SystemExit('alpha.2 VERSION marker not found')
main.write_text(main_text.replace(old, new, 1), encoding='utf-8')

print('Applied Chainacobblemon 0.4.0-alpha.3 hard no-blur Twitch screen fix')

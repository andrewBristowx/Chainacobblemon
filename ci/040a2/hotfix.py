from pathlib import Path

root = Path('/tmp/chainacobblemon')
props = root / 'gradle.properties'
main = root / 'src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java'
screen = root / 'src/client/java/com/andrewbristowx/chainacobblemon/client/twitch/TwitchScreen.java'
service = root / 'src/main/java/com/andrewbristowx/chainacobblemon/twitch/TwitchService.java'

if not props.exists() or 'mod_version=0.4.0-alpha.1+1.21.1' not in props.read_text(encoding='utf-8'):
    raise SystemExit('0.4.0-alpha.1 baseline not found; refusing unsafe alpha.2 patch')

# The alpha.1 hotfix copies the Twitch templates from this release branch, so verify
# that the intended alpha.2 UI and clickable announcement changes are already present.
screen_text = screen.read_text(encoding='utf-8')
for marker in ['ConfirmLinkScreen.open', 'Copiar link', 'context.fill(0, 0, width, height, 0x22000000)', 'Ver stream']:
    if marker not in screen_text:
        raise SystemExit(f'missing alpha.2 TwitchScreen marker: {marker}')
if 'renderBackground(context, mouseX, mouseY, delta);' in screen_text:
    raise SystemExit('blur-producing renderBackground call is still present')

service_text = service.read_text(encoding='utf-8')
for marker in ['ClickEvent.Action.OPEN_URL', 'HoverEvent.Action.SHOW_TEXT', 'Clic para abrir el stream de Chaina']:
    if marker not in service_text:
        raise SystemExit(f'missing alpha.2 TwitchService marker: {marker}')

props.write_text(props.read_text(encoding='utf-8').replace(
    'mod_version=0.4.0-alpha.1+1.21.1',
    'mod_version=0.4.0-alpha.2+1.21.1', 1), encoding='utf-8')

main_text = main.read_text(encoding='utf-8')
old = 'public static final String VERSION = "0.4.0-alpha.1+1.21.1";'
new = 'public static final String VERSION = "0.4.0-alpha.2+1.21.1";'
if old not in main_text:
    raise SystemExit('alpha.1 VERSION marker not found')
main.write_text(main_text.replace(old, new, 1), encoding='utf-8')

print('Applied Chainacobblemon 0.4.0-alpha.2 Twitch UI/link improvements')

from pathlib import Path

root = Path('/tmp/chainacobblemon')
props = root / 'gradle.properties'
main = root / 'src/main/java/com/andrewbristowx/chainacobblemon/Chainacobblemon.java'

if not props.exists() or 'mod_version=0.4.0-alpha.4+1.21.1' not in props.read_text(encoding='utf-8'):
    raise SystemExit('0.4.0-alpha.4 mod baseline not found')

props.write_text(props.read_text(encoding='utf-8').replace(
    'mod_version=0.4.0-alpha.4+1.21.1',
    'mod_version=0.4.0-alpha.5+1.21.1', 1), encoding='utf-8')

text = main.read_text(encoding='utf-8')
old = 'public static final String VERSION = "0.4.0-alpha.4+1.21.1";'
new = 'public static final String VERSION = "0.4.0-alpha.5+1.21.1";'
if old not in text: raise SystemExit('alpha.4 main VERSION not found')
main.write_text(text.replace(old, new, 1), encoding='utf-8')

print('Applied Chainacobblemon 0.4.0-alpha.5 version bump for persistent ChainaBridge sessions')

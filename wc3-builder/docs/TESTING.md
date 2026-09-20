# Testing

The automated test runner uses synthetic WC3 data and does not bundle user saves.

Run:

```cmd
javac -encoding UTF-8 -d out src\*.java tests\TestRunner.java
java -cp out TestRunner
```

Covered cases:

- `create` produces exactly `0x58C` bytes;
- newly-created card CRC is valid;
- newly-created placeholder RamScript checksum is valid;
- default icon and metadata icon stay synchronized;
- edit changes card fields and CRC;
- edit does not alter any RamScript byte;
- icon edit updates both card and metadata icon;
- text encoder rejects unsupported characters and overlong 40-byte card fields.

Recommended manual integration test:

1. `create custom.wc3`
2. inspect it;
3. inject it with `wc3-injector`;
4. boot FireRed/LeafGreen;
5. talk to the deliveryman;
6. verify the informational placeholder message;
7. replace its RamScript with a real event using the separate RamScript toolchain.


API v1 regression coverage additionally verifies:

- the 20 receive-slot mappings (`1000..1019` -> `0x2A7..0x2BA`);
- JSON protocol/version contract;
- catalog contents and semantics;
- API create/edit/inspect round-trip;
- RamScript byte preservation through API edit;
- rejection of out-of-range Wonder Card receive IDs;
- structured unknown-option errors.


Current API/domain suite baseline: `PASS 56`. This includes semantic species catalog coverage, Hoenn internal-ID/National-Dex separation, default-card catalog exposure, and existing create/inspect/edit/CRC/RamScript-preservation tests.

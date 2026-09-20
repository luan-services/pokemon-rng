# Machine API v1

## Purpose

The `api` command is the stable integration boundary for GUI/automation clients.

Human CLI output is explicitly **not** an API. Clients must not scrape lines printed by `inject`, `extract`, `inspect-save`, or `verify-wc3`.

## Protocol

Every API invocation writes exactly one JSON object to stdout.

Common envelope:

```json
{
  "protocol": "wc3-injector-json",
  "apiVersion": 1,
  "ok": true,
  "command": "version",
  "result": {}
}
```

On failure:

```json
{
  "protocol": "wc3-injector-json",
  "apiVersion": 1,
  "ok": false,
  "command": "verify-wc3",
  "error": {
    "code": "INVALID_WC3",
    "message": "..."
  }
}
```

Successful calls exit with code `0`. Structured API failures exit with code `2`.

## Version handshake

```powershell
java -jar wc3-injector-api-v1.jar api version
```

The client should require:

```text
protocol   = wc3-injector-json
apiVersion = 1
```

Current capabilities:

```text
inject-save
extract-wc3
inspect-save
verify-wc3
build-distribution-rom
```

Capabilities are descriptive. API v1 exposes FR/LG distribution-ROM generation through `api build-distribution`; Celio-GB is not exposed and remains a separate future transport.

## Inspect save

```powershell
java -jar wc3-injector-api-v1.jar api inspect-save --input game.sav
```

Representative result:

```json
{
  "input": "C:\\...\\game.sav",
  "slots": [
    {"slotIndex":1,"valid":true,"counter":41,"status":"ok"},
    {"slotIndex":2,"valid":true,"counter":42,"status":"ok"}
  ],
  "activeSlot": {"slotIndex":2,"valid":true,"counter":42,"status":"ok"}
}
```

`slotIndex` is one-based in the API because it is user-facing.

A structurally invalid save file returns a structured error or invalid slot data; no mutation occurs.

## Verify WC3

```powershell
java -jar wc3-injector-api-v1.jar api verify-wc3 --input event.wc3
```

The result contains:

- flag ID;
- icon species;
- stored and calculated card CRC, both numeric and hex text;
- stored and calculated RamScript checksum, both numeric and hex text;
- validity booleans;
- structured warnings.

Example warning:

```json
{
  "severity": "WARNING",
  "code": "CARD_CRC_MISMATCH",
  "message": "Wonder Card CRC mismatch (...)"
}
```

Warnings are informational and do not automatically make custom/research cards unusable.

Warning codes currently include:

```text
ZERO_FLAG_ID
CARD_CRC_MISMATCH
RAMSCRIPT_CHECKSUM_MISMATCH
WC3_WARNING
```

## Inject into save

```powershell
java -jar wc3-injector-api-v1.jar api inject --input-save game.sav --wc3 event.wc3 --output injected.sav
```

The operation returns:

- absolute input paths;
- output artifact path and byte size;
- selected one-based slot index;
- unsigned save counter as a JSON number;
- physical sector index;
- new sector checksum in numeric and `0xXXXX` forms;
- Wonder Card flag ID;
- WC3 warnings.

The tool writes a new output file. The GUI should never silently overwrite the user's only save unless the user explicitly selected that same path and confirmed that behavior at the UI level.

## Extract WC3

```powershell
java -jar wc3-injector-api-v1.jar api extract --input-save game.sav --output extracted.wc3
```

The result returns:

- input save;
- output artifact path and size (`1420` bytes for a standard WC3);
- selected slot/counter/physical sector;
- full WC3 verification metadata;
- warnings.

API v1 does **not** have a separate RamScript `.bin` extraction command.

## Error codes

Current structured codes:

```text
INVALID_REQUEST
UNKNOWN_API_COMMAND
INPUT_FILE_NOT_FOUND
INVALID_SAVE
INVALID_WC3
IO_ERROR
INTERNAL_ERROR
```

Clients should branch on `error.code`, not on English message text.

## Compatibility rule

API v1 fields may gain additional non-breaking data. Clients should ignore unknown fields.

Existing field meaning, protocol string, numeric API version, commands, and structured error codes should not be silently repurposed.

## `build-distribution`

Builds an FR/LG Wireless Adapter distribution ROM from an untouched user-supplied Aurora Ticket (USA) distribution ROM and a WC3.

```cmd
java -jar wc3-injector-api-v1.jar api build-distribution --base-rom "JPAJ - Aurora Ticket (USA).gba" --wc3 event.wc3 --output output.gba
```

The base ROM must have SHA-1 `94a21d133e3f1b1a2129f450a9bc753d12c13b09`. A different ROM is rejected with `INVALID_BASE_ROM` rather than patched speculatively. The result includes `target`, base/output SHA-1, script ROM offset, script send size, output artifact metadata, and WC3 warnings.

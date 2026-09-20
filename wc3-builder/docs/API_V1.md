# wc3-builder JSON API v1

The desktop UI must use the machine-facing JSON API rather than parsing the human CLI.

```text
protocol: wc3-builder-json
apiVersion: 1
```

The API is intentionally file-oriented. `wc3-builder` owns Wonder Card design/metadata; it does not compile arbitrary event payloads.

## Commands

```powershell
java -jar lib/wc3-builder-api-v1.jar api version
java -jar lib/wc3-builder-api-v1.jar api catalog
java -jar lib/wc3-builder-api-v1.jar api inspect --input input.wc3
java -jar lib/wc3-builder-api-v1.jar api create --output output.wc3 [card options]
java -jar lib/wc3-builder-api-v1.jar api edit --input input.wc3 --output output.wc3 [card options]
```

Card options:

```text
--title --subtitle
--body1 --body2 --body3 --body4
--footer1 --footer2
--flag --icon --id --type --bg --send --stamps
```

Numbers accept decimal or `0x` hexadecimal syntax.

## Response envelope

Success:

```json
{"protocol":"wc3-builder-json","apiVersion":1,"ok":true}
```

Failure:

```json
{
  "protocol":"wc3-builder-json",
  "apiVersion":1,
  "ok":false,
  "error":{"code":"INVALID_REQUEST","message":"..."}
}
```

Stable error codes currently used:

```text
INVALID_REQUEST
UNKNOWN_API_COMMAND
UNKNOWN_OPTION
INPUT_FILE_NOT_FOUND
INTERNAL_ERROR
```

## `api catalog`

The catalog is the UI-facing source of truth for:

- card types;
- send types;
- the 8 Wonder Card backgrounds;
- the 20 FR/LG Wonder Card receive slots;
- the selectable Gen I-III Pokémon icon species using FR/LG internal species IDs, with human labels and National Dex metadata;
- a backend-owned `defaultCard` for new-editor initialization;
- numeric/text limits;
- receive-slot semantics relevant to future wireless transports.

The UI should not hardcode the receive-slot table, species table, or new-card defaults.

`catalog.iconSpecies` contains the 386 real Gen I-III species plus the special `NONE` value. Internal `OLD_UNOWN_*` placeholder species IDs are deliberately omitted. Hoenn entries preserve FR/LG internal species IDs, which diverge from National Dex numbering after Celebi; `nationalDex` is metadata only.

`catalog.defaultCard` is serialized from `Wc3Factory.createBase()` and is the canonical initial form state for a new card. `catalog.defaultRamScriptBehavior` currently reports `DEFAULT_INFORMATIONAL_PLACEHOLDER`: `api create` produces a complete WC3 containing the existing benign deliveryman RamScript when no custom event behavior is supplied by another toolchain.

## `flagId` / receive slot

For API create/edit, `--flag` is intentionally restricted to `1000..1019`.

These are not arbitrary event flags. FR/LG treats `WonderCard.flagId` as a receive ID that maps through its 20-entry `sReceivedGiftFlags` table. General unused event flags used by a custom RamScript are a separate concern and belong to the event toolchain, not to this field.

## Edit preservation guarantee

`api edit` changes Wonder Card fields and recalculates the card CRC. Bytes in the RamScript region are preserved byte-for-byte. The response contains `ramScriptPreserved` so the UI can surface an integration error if that invariant ever changes.

## Mystery Gift default icon

`iconSpecies = 0xFFFF` is the stock/default Mystery Gift question-mark icon used by preserved FR/LG ticket Wonder Cards. In `api catalog` this is exposed semantically as `MYSTERY_GIFT_DEFAULT` / `Mystery Gift (?)` with `kind = SPECIAL`. It is not `NONE` and it is not Unown ?. `Wc3File.setIconSpecies(...)` mirrors the selected value into both `WonderCard.iconSpecies` and `WonderCardMetadata.iconSpecies`. The JavaFX UI must present the semantic catalog item and send its raw `value` back to the backend.

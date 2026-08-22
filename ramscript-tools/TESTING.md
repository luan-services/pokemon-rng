# Testing layout

The project accumulated several one-off persistence probes while the storage model was being discovered. They are intentionally kept because they are useful diagnostics, but new tests should no longer be added to the monolithic `TestRunner` body.

## Test layers

- `tests/TestRunner.java` — legacy regression suite and entry point.
- `tests/PersistentToolkitStorageV2Test.java` — focused tests for the current persistent-module format.
- `PersistenceProbe*` — historical 8-byte SaveBlock1 diagnostic.
- `PersistenceFullRegionProbe*` — historical 400-byte SaveBlock1 validation.
- `PersistenceSaveBlock2Probe*` — historical 1024-byte SaveBlock2 validation.
- `PersistentToolkitStorageV1*` — historical single-payload execution proof.
- `PersistentToolkitStorageV2*` — current table-based module experiment.

Do not delete the historical probes yet. They are small and give us known-good diagnostics if a later storage change regresses. Once V2 is validated across the target ROM profiles, they can move under a dedicated `diagnostics` namespace/package in a larger cleanup.

## Runtime validation status

- SaveBlock1 + 0x348C, 400/400 bytes: validated on FireRed EN 1.0.
- SaveBlock2 + 0x0B20, 1024/1024 bytes: validated on FireRed EN 1.0.
- V1 persistent Thumb execution from SaveBlock2: validated on FireRed EN 1.0.
- V2 module table + lookup by module ID: pending emulator validation (Build 6).

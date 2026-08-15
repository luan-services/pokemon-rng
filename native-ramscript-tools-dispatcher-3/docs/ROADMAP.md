# Research roadmap

## Phase 1 - complete

Known-good small fixed-RAM hotkey effect:

```text
R + SELECT -> reset RNG sequence
```

## Phase 2 - native framework cleanup

Current repository state:

```text
effect catalog
ROM profiles
runtime layout metadata
golden payload regression test
frozen seed implementation
```

## Phase 3 - hotkey dispatcher research

Goal:

```text
VBlank hook
    |
detect button combination
    |
set pending event/action
    |
return to original VBlank
```

Then, outside the interrupt:

```text
main loop / task / safe callback
    |
observe pending event
    |
run larger behavior
```

Questions to research before implementation:

1. Which normal task/callback path is safe and persistent enough to host a
   dispatcher without reproducing the SaveBlock relocation crashes?
2. Can a tiny fixed hook schedule an existing engine task rather than execute
   large code in IRQ context?
3. Can a Field Script be started safely from that deferred context?
4. Where can larger code/data live, and what is its lifetime?
5. How should multiple hotkeys/effects share one dispatcher?
6. What ROM-specific symbols differ across FR10/FR11/LG10/LG11?

## Phase 4 - bridge back to ramscript-tools

If Phase 3 succeeds, the normal event composer can gain a trigger layer:

```text
Trigger:
    Deliveryman
    Hotkey

Hotkey:
    held button
    newly pressed button

Actions:
    ordinary Field Script blocks
```

The native layer should ideally remain only the trigger/dispatcher. Event logic
should remain in `ramscript-tools`.

# Dispatcher Candidate 5a2

Candidate 5a2 is a diagnostic isolation test, not a new runtime design.

## Question

Candidate 4 was stable.

Candidate 5 introduced a reproducible regression involving changing Button Mode
and then installing the runtime before a map transition.

Candidate 5 changed two important things at once:

1. it introduced a real call/return trampoline using `push {lr}`, a fabricated
   local `lr`, and `pop {pc}`;
2. it placed that trampoline at `03003F80..03003F93`, overlapping live link
   globals including `gLinkCallback`.

Candidate 5a2 keeps BOTH of those diagnostic properties but removes
`GetSavedRamScriptIfValid()` from the equation.

## Action

```text
R + SELECT
→ push LR
→ PlaySE(SE_SELECT)
→ return to local continuation
→ pop PC
```

This differs from Candidate 4, which tail-called `PlaySE` directly.

## Interpretation

Run the exact Button Mode sequence that reproduces C5:

```text
change Button Mode in deliveryman map
→ return directly
→ talk to deliveryman
→ press R+SELECT
```

If Candidate 5a2 also crashes:

```text
strong evidence:
C5 call/return trampoline and/or 03003F80 layout is the regression source
```

If Candidate 5a2 does NOT crash and consistently plays SE_SELECT:

```text
stronger suspicion:
GetSavedRamScriptIfValid() or its context interaction contributes to the C5 bug
```

Then also run the clean control:

```text
change Button Mode
→ change map
→ return
→ talk to deliveryman
→ press R+SELECT
```

## Important

Candidate 5a2 deliberately still overwrites `gLinkCallback` and neighboring link
globals. This is intentional for isolation and remains unsafe as a final design.

Do not test cable, wireless, Union Room, or link battle.


## Correction from rejected 5a

The first 5a diagnostic was invalid. After inserting `movs r0,#5`, the local
continuation moved to `03003F90`, but the ADR remained `01 A2`.

At instruction address `03003F86`:

```text
PC base = (03003F86 + 4) & ~3 = 03003F88
01 A2   = base + 4 = 03003F8C   WRONG
02 A2   = base + 8 = 03003F90   CORRECT
```

The bad version returned from PlaySE to the `bx r3` instruction itself,
re-entering PlaySE. Its freeze/crash results are discarded.

Candidate 5a2 uses `02 A2`.

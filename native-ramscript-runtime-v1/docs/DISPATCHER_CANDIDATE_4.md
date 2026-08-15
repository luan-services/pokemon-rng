# Dispatcher Candidate 4 - first external ROM function

Candidate 3 validated persistent normal-context hotkey execution. Candidate 4
changes only the trigger action.

## Action

```text
R + SELECT
    |
    v
PlaySE(SE_SELECT)
```

FireRed 1.0:

```text
PlaySE     080722CC
Thumb ptr  080722CD
SE_SELECT  0x0005
```

The trigger path is intentionally a **tail-call**. The dispatcher keeps the LR
it received from the main loop and executes `bx PlaySE`. When `PlaySE` returns,
it returns directly to the original callback caller.

Therefore, on the trigger frame only:

```text
callback1 wrapper
 -> PlaySE
 -> main loop
```

instead of:

```text
callback1 wrapper
 -> CB1_Overworld
 -> main loop
```

On the next frame the normal callback path resumes. This avoids adding a larger
call/return trampoline before larger safe payload storage has been researched.

## Runtime storage

The Candidate-3 supervisor/detector/installer bytes are preserved exactly.

New data:

```text
03003F98..03003F9F  trigger tail-call
03003FA4..03003FA7  PlaySE|1 literal
```

`03003FA4` lies in the alignment gap after `gLastSendQueueCount` and before
`gLink`; this remains experimental/offline-only storage.

## Expected test

After installation, verify callback1 re-arms as in Candidate 3. In the overworld,
hold R and newly press SELECT. The normal menu select sound should play once.
Repeat after a map transition and after returning from battle.

Do not test cable/wireless/link modes.

# Dispatcher Candidate 6

Candidate 6 is the first end-to-end Field Script bridge experiment.

## Chain

```text
R + SELECT
→ persistent normal-context dispatcher
→ GetSavedRamScriptIfValid()
→ current relocated RamScript pointer
→ +0x0A
→ ScriptContext_SetupScript(pointer)
→ next overworld script-engine tick
→ Field Script payload executes
```

`ScriptContext_SetupScript(const u8 *ptr)` initializes the global script
context, sets its bytecode pointer, locks field controls, and marks the context
running.

## Why `+0x0A`

The RamScript begins with:

```text
+00  setvaddress 0x08010000    (5 bytes)
+05  vgoto installer           (5 bytes)
+0A  HOTKEY PAYLOAD
```

When the deliveryman invokes the card, the first two commands jump over the
hotkey payload and execute the native installer.

When R+SELECT is pressed, `GetSavedRamScriptIfValid()` returns the beginning of
the same relocatable script. The native dispatcher adds 10 and gives that
payload pointer to `ScriptContext_SetupScript()`.

## Payload

For this first proof, the payload is deliberately tiny:

```text
setptr 0x66, 0x03003FA1
end
```

So `03003FA1` changing to `66` proves the write happened from Field Script
bytecode scheduled by the normal script engine.

## Native function addresses (FR10)

```text
GetSavedRamScriptIfValid  08069E48  / Thumb 08069E49
ScriptContext_SetupScript 08069AE4  / Thumb 08069AE5
delta                     00000364
```

The thunk loads the first function into `r4`. Because `r4` is callee-saved,
it survives the first call. Stage 2 subtracts `0x364` to obtain the second
Thumb function pointer without needing another literal.

## Clean layout

```text
03003F42..4F  stage2, linker padding
03003F50      gSendCmd begins -- untouched

03003F70..7F  detector
03003F80..93  untouched

03003F94..97  heldKeys pointer
03003F98..9B  thunk
03003F9C..9F  GetSaved pointer

03003FA0      gLastSendQueueCount untouched
03003FA1      Field Script marker
03003FA2..AF  stage1
03003FB0      gLink begins untouched
```

## Test

After installation, `03003FA1` should be `00`.

Press R+SELECT in the overworld.

The native calls only schedule the script. The marker is expected to become:

```text
03003FA1 = 66
```

when the normal field-script engine runs it.

Repeat after map transitions and battles.

Also repeat the previous first-Options regression path. Candidate 6 must not
reintroduce that freeze.

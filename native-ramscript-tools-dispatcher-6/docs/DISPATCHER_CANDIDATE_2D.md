# Dispatcher Candidate 2d - static-audited test candidate

Candidates 2, 2b, and 2c are rejected.

## Critical 2c problem

Candidate 2c stored:

```text
03003EC0 = CB1_Overworld|1
```

because the callback wrapper used the fixed tail stub.

But the VBlank supervisor used that SAME tail stub at `03003EB4`.

Therefore the VBlank path could tail-chain to `CB1_Overworld` instead of the
original VBlank handler. That is invalid: normal overworld logic must not be
used as the IRQ return path.

## 2d separation

Candidate 2d has two independent return paths.

### VBlank

```text
03005310 supervisor
    |
    `-> 03003EB4 VBlank tail
            |
            `-> literal 03003EC0 = 08000725
                                      original VBlank
```

### callback1

```text
03003F94 callback wrapper
    |
    `-> its own literal at 03003F9C = 08056535
                                      CB1_Overworld
```

The two contexts no longer share a function-pointer literal.

## Supervisor rule

Only:

```text
callback1 == 08056535
```

causes:

```text
callback1 = 03003F95
```

NULL, battle callbacks, transition callbacks, menus, or any other callback are
left untouched.

## No marker write

Candidate 2d removes the `0201C100` debug write. That address is known to be
temporary game memory and is unnecessary for this test.

Observe `030030F0` itself:

```text
08056535 = game's normal CB1
03003F95 = wrapper armed
```

## Static checks performed before release

Automated tests decode and verify:

- supervisor ADR target;
- supervisor conditional branch target;
- supervisor literal values;
- wrapper literal-load target;
- wrapper CB1 pointer;
- VBlank tail literal-load target;
- installer literal-load targets;
- VBlank slot and supervisor pointer;
- exact supervisor opcode bytes;
- golden seed-modifier payload remains unchanged.

This does not prove runtime stability. It only removes the specific static
machine-code mistakes found in candidates 2/2b/2c.

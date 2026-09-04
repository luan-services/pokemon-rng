import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/*
   Build 19 in-game smoke test package.

   Installs three persistent Field Script bodies and three 10-byte SB1 gateways:
     R+SELECT -> Seed Modifier
     R+B      -> Repel
     R+A      -> Shared runtime probe message

   The runtime WC then installs SharedHotkeyRuntime, which dispatches
   those bindings through one shared deferred runtime.
*/
final class SharedHotkeySmokeTestPreset {
    static final int DEFAULT_SEED = 0x1234;

    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;

    private SharedHotkeySmokeTestPreset() {}

    static PlacementPlan placement(RomProfile rom, int seed) {
        return PayloadPlacementPlanner.plan(List.of(
                PresetRequest.fieldScript("seed", SeedModifierPreset.buildPayload(rom, seed), PresetPlacementPreference.PERSISTENT, true),
                PresetRequest.fieldScript("repel", RepelHotkeyPreset.buildPayload(), PresetPlacementPreference.PERSISTENT, true),
                PresetRequest.fieldScript("probe-a", buildProbePayload(), PresetPlacementPreference.PERSISTENT, true)
        ), RamScript.SCRIPT_SIZE);
    }

    static RamScript buildInstaller(RomProfile rom, int seed) {
        byte[] seedBody = SeedModifierPreset.buildPayload(rom, seed);
        byte[] repelBody = RepelHotkeyPreset.buildPayload();
        byte[] sidBody = buildProbePayload();

        PlacementPlan plan = placement(rom, seed);
        List<PersistentFieldScriptAllocation> a = plan.persistentAllocations();
        if (a.size() != 3) throw new IllegalStateException("smoke test expected three persistent allocations");

        List<CopySpec> copies = new ArrayList<>();
        copies.add(new CopySpec(true, a.get(0).sb1GatewayOffset(), PayloadPlacementPlanner.buildGateway(a.get(0))));
        copies.add(new CopySpec(true, a.get(1).sb1GatewayOffset(), PayloadPlacementPlanner.buildGateway(a.get(1))));
        copies.add(new CopySpec(true, a.get(2).sb1GatewayOffset(), PayloadPlacementPlanner.buildGateway(a.get(2))));
        copies.add(new CopySpec(false, a.get(0).sb2PayloadOffset(), seedBody));
        copies.add(new CopySpec(false, a.get(1).sb2PayloadOffset(), repelBody));
        copies.add(new CopySpec(false, a.get(2).sb2PayloadOffset(), sidBody));

        long copier = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper helper = buildBatchCopyHelper(rom, helperAddress, copies);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copier, "shared_hotkey_smoke_install", NativeHelperInstaller.Mode.AUTO
        );
        b.lockAll();
        install.installAndCall(b);
        b.vMessage("ok").waitMessage().waitButtonPress().releaseAll().end()
                .text("ok", "Shared hotkey test modules installed.\\nSave, then install runtime.");
        return RamScript.createWonderCard(b.buildScript());
    }

    static TriggerBuildResult buildRuntime(RomProfile rom, int seed) {
        PlacementPlan plan = placement(rom, seed);
        List<PersistentFieldScriptAllocation> a = plan.persistentAllocations();
        List<SharedHotkeyDispatcher.Entry> entries = List.of(
                new SharedHotkeyDispatcher.Entry(HotkeyButton.SELECT, -a.get(0).gatewayDistanceFromRamScript()),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.B, -a.get(1).gatewayDistanceFromRamScript()),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.A, -a.get(2).gatewayDistanceFromRamScript())
        );
        return SharedHotkeyRuntime.compose(rom, HotkeyButton.R, entries);
    }


    private static byte[] buildProbePayload() {
        return new RamScriptBuilder(VIRTUAL_BASE)
                .setVAddress()
                .lockAll()
                .vMessage("message")
                .waitMessage()
                .waitButtonPress()
                .releaseAll()
                .end()
                .text("message", "Shared R+A binding works.")
                .buildScript();
    }

    static String placementReport(RomProfile rom, int seed) {
        PlacementPlan plan = placement(rom, seed);
        StringBuilder s = new StringBuilder();
        for (PersistentFieldScriptAllocation a : plan.persistentAllocations()) {
            s.append(String.format(
                    "  %-5s gateway SB1+0x%04X (distance -0x%02X) -> SB2+0x%04X, %d B%n",
                    a.presetId(), a.sb1GatewayOffset(), a.gatewayDistanceFromRamScript(), a.sb2PayloadOffset(), a.payloadSize()
            ));
        }
        PlacementDiagnostics d = plan.diagnostics();
        s.append(String.format("  SB1 gateway used/free: %d / %d B%n", d.sb1GatewayBytesUsed(), d.sb1GatewayBytesFree()));
        s.append(String.format("  SB2 used/free:         %d / %d B%n", d.sb2PayloadBytesUsed(), d.sb2PayloadBytesFree()));
        return s.toString();
    }

    private record CopySpec(boolean saveBlock1, int offset, byte[] data) {
        CopySpec {
            if (data == null || data.length == 0 || data.length > 0xFF) {
                throw new IllegalArgumentException("smoke installer copy size must be 1..255 bytes");
            }
            data = data.clone();
        }
        @Override public byte[] data() { return data.clone(); }
    }

    private static NativeHelper buildBatchCopyHelper(RomProfile rom, long address, List<CopySpec> copies) {
        final int loopSize = 24;
        final int codeEnd = copies.size() * loopSize;
        final int bxOffset = codeEnd;
        final int literalOffset = align4(bxOffset + 2);
        final int literalsSize = copies.size() * 12;
        int dataOffset = align4(literalOffset + literalsSize);

        int[] srcOffsets = new int[copies.size()];
        for (int i = 0; i < copies.size(); i++) {
            srcOffsets[i] = dataOffset;
            dataOffset = align4(dataOffset + copies.get(i).data().length);
        }

        byte[] code = new byte[dataOffset];
        for (int i = 0; i < copies.size(); i++) {
            int base = i * loopSize;
            int lit = literalOffset + i * 12;
            int loop = base + 0x0C;
            emitCopy(code, base, lit, lit + 4, lit + 8, copies.get(i).data().length, loop);
            putU32(code, lit, copies.get(i).saveBlock1() ? rom.saveBlock1Ptr : rom.saveBlock2Ptr);
            putU32(code, lit + 4, copies.get(i).offset());
            putU32(code, lit + 8, address + srcOffsets[i]);
            System.arraycopy(copies.get(i).data(), 0, code, srcOffsets[i], copies.get(i).data().length);
        }
        putU16(code, bxOffset, 0x4770); // bx lr
        for (int p = bxOffset + 2; p < literalOffset; p += 2) putU16(code, p, 0x46C0);
        return new NativeHelper(address, code);
    }

    private static void emitCopy(byte[] c, int base, int ptrLit, int offLit, int srcLit, int len, int loop) {
        putU16(c, base + 0x00, ldrLiteral(0, base + 0x00, ptrLit));
        putU16(c, base + 0x02, 0x6800); // ldr r0,[r0]
        putU16(c, base + 0x04, ldrLiteral(1, base + 0x04, offLit));
        putU16(c, base + 0x06, 0x1840); // add r0,r0,r1
        putU16(c, base + 0x08, ldrLiteral(1, base + 0x08, srcLit));
        putU16(c, base + 0x0A, 0x2200 | len); // movs r2,#len
        putU16(c, base + 0x0C, 0x780B); // ldrb r3,[r1]
        putU16(c, base + 0x0E, 0x7003); // strb r3,[r0]
        putU16(c, base + 0x10, 0x3101);
        putU16(c, base + 0x12, 0x3001);
        putU16(c, base + 0x14, 0x3A01);
        putU16(c, base + 0x16, branchCond(1, base + 0x16, loop)); // bne loop
    }

    private static int align4(int n) { return (n + 3) & ~3; }
    private static int ldrLiteral(int rt, int insn, int literal) {
        int base = (insn + 4) & ~3;
        int d = literal - base;
        if (d < 0 || (d & 3) != 0 || d / 4 > 255) throw new IllegalArgumentException("literal range");
        return 0x4800 | (rt << 8) | (d / 4);
    }
    private static int branchCond(int cond, int insn, int target) {
        int d = target - (insn + 4);
        if ((d & 1) != 0 || d / 2 < -128 || d / 2 > 127) throw new IllegalArgumentException("branch range");
        return 0xD000 | (cond << 8) | ((d / 2) & 0xFF);
    }
    private static void putU16(byte[] b, int o, int v) { b[o] = (byte)v; b[o + 1] = (byte)(v >>> 8); }
    private static void putU32(byte[] b, int o, long v) {
        b[o] = (byte)v; b[o + 1] = (byte)(v >>> 8); b[o + 2] = (byte)(v >>> 16); b[o + 3] = (byte)(v >>> 24);
    }
}

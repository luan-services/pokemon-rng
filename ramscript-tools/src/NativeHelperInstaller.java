import java.util.Arrays;

/*
   Chooses how a native Thumb helper is staged from a relocatable RamScript
   payload into writable RAM.

   DIRECT_SET_PTR
     Expands every helper byte to one Field Script setptr command.
     Cost: 6 bytes of RamScript per helper byte.
     Best for small helpers and requires no secondary copier.

   CPU_SET_BLOCK
     Embeds the helper as raw bytes in the RamScript, skips over them with
     vgoto, installs a tiny Thumb copier with setptr, then copies the whole
     helper with the GBA BIOS CpuSet service.
     Best once the helper is large enough to amortize the copier bootstrap.

   AUTO compares the exact encoding cost at the current builder position and
   selects the smaller valid representation. This class only owns helper
   staging; it does not own triggers, hotkeys, or helper behavior.
*/
final class NativeHelperInstaller {
    enum Mode {
        AUTO,
        DIRECT_SET_PTR,
        CPU_SET_BLOCK
    }

    static final class Plan {
        private final Mode mode;
        private final NativeHelper helper;
        private final long copierAddress;
        private final byte[] copier;
        private final int encodedInstallBytes;

        private Plan(Mode mode, NativeHelper helper, long copierAddress, byte[] copier, int encodedInstallBytes) {
            this.mode = mode;
            this.helper = helper;
            this.copierAddress = copierAddress;
            this.copier = copier;
            this.encodedInstallBytes = encodedInstallBytes;
        }

        Mode mode() {
            return mode;
        }

        int encodedInstallBytes() {
            return encodedInstallBytes;
        }

        /* CPU_SET_BLOCK resolves an embedded raw source relative to the script
           base and copies it as 32-bit words. Therefore the containing Field
           Script itself must start word-aligned. DIRECT_SET_PTR has no such
           requirement. */
        int requiredScriptBaseAlignment() {
            return mode == Mode.CPU_SET_BLOCK ? 4 : 1;
        }

        RamScriptBuilder install(RamScriptBuilder builder) {
            if (mode == Mode.DIRECT_SET_PTR) {
                return builder.writeBytes(helper.stagingAddress(), helper.codeCopy());
            }

            return builder
                    .writeBytes(copierAddress, copier)
                    .callNative(copierAddress | 1L);
        }

        RamScriptBuilder installAndCall(RamScriptBuilder builder) {
            return install(builder).callNative(helper.thumbEntryAddress());
        }
    }

    private NativeHelperInstaller() {}

    static Plan prepare(
            RamScriptBuilder builder,
            long virtualBase,
            NativeHelper helper,
            long copierAddress,
            String labelPrefix,
            Mode requestedMode
    ) {
        if (builder == null || helper == null) {
            throw new IllegalArgumentException("builder/helper must not be null");
        }
        if (labelPrefix == null || labelPrefix.isBlank()) {
            throw new IllegalArgumentException("labelPrefix must not be blank");
        }

        int directCost = directEncodingCost(helper.size());
        CpuCandidate cpu = cpuCandidate(builder.position(), helper, copierAddress);

        Mode selected = requestedMode;
        if (requestedMode == Mode.AUTO) {
            selected = cpu.valid && cpu.cost < directCost
                    ? Mode.CPU_SET_BLOCK
                    : Mode.DIRECT_SET_PTR;
        }

        if (selected == Mode.DIRECT_SET_PTR) {
            return new Plan(selected, helper, 0, null, directCost);
        }

        if (!cpu.valid) {
            throw new IllegalArgumentException("CPU_SET_BLOCK is not valid for this helper/layout");
        }

        String afterRaw = "__" + labelPrefix + "_after_native_raw";
        builder.vGoto(afterRaw).padding(cpu.padding).raw(cpu.rawBytes).label(afterRaw);

        long virtualSource = virtualBase + Integer.toUnsignedLong(cpu.rawOffset);
        byte[] copier = CpuSetNativeHelperInstaller.copierBytes(
                copierAddress,
                virtualSource,
                cpu.rawBytes.length
        );

        return new Plan(selected, helper, copierAddress, copier, cpu.cost);
    }

    static Mode chooseMode(int builderPosition, NativeHelper helper, long copierAddress) {
        int directCost = directEncodingCost(helper.size());
        CpuCandidate cpu = cpuCandidate(builderPosition, helper, copierAddress);
        return cpu.valid && cpu.cost < directCost
                ? Mode.CPU_SET_BLOCK
                : Mode.DIRECT_SET_PTR;
    }

    static int directEncodingCost(int helperSize) {
        if (helperSize <= 0) {
            throw new IllegalArgumentException("helperSize must be positive");
        }
        return helperSize * 6;
    }

    private static CpuCandidate cpuCandidate(int builderPosition, NativeHelper helper, long copierAddress) {
        if ((copierAddress & 3L) != 0) {
            return CpuCandidate.invalid();
        }
        if (helper.stagingAddress() != CpuSetNativeHelperInstaller.helperDestination(copierAddress)) {
            return CpuCandidate.invalid();
        }

        byte[] raw = padToWord(helper.codeCopy());

        // vgoto itself is 5 bytes. The raw block must start word-aligned for
        // CpuSet 32-bit mode, so account for the exact padding at this point.
        int afterVGoto = builderPosition + 5;
        int padding = (4 - (afterVGoto & 3)) & 3;
        int rawOffset = afterVGoto + padding;

        int cost = 5                         // vgoto over inert raw helper data
                + padding
                + raw.length
                + CpuSetNativeHelperInstaller.COPIER_SIZE * 6 // setptr copier install
                + 5;                         // callnative copier

        return new CpuCandidate(true, cost, padding, rawOffset, raw);
    }

    private static byte[] padToWord(byte[] code) {
        int paddedLength = (code.length + 3) & ~3;
        return Arrays.copyOf(code, paddedLength);
    }

    private record CpuCandidate(boolean valid, int cost, int padding, int rawOffset, byte[] rawBytes) {
        static CpuCandidate invalid() {
            return new CpuCandidate(false, Integer.MAX_VALUE, 0, 0, null);
        }
    }
}

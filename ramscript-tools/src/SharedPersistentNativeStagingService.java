/*
   Shared Field Script service for complex persistent native modules.

   The service lives once inside the shared RamScript runtime. Persistent SB2
   bridges vcall it after setting VAR_8005 to the desired module id. It stages
   the generic 176-byte loader into EWRAM scratch, calls that loader, and
   returns to the caller. The loader itself resolves/validates the selected SB2
   module and copies its Thumb body to moduleStagingAddress.

   The service never enters the preset module. The caller retains the validated
   contract:
       shared service -> return -> stock callnative(staged module)
*/
final class SharedPersistentNativeStagingService {
    private static final long RAMSCRIPT_VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;

    private SharedPersistentNativeStagingService() {}

    record Build(byte[] fieldScript, int loaderBytes, int requiredBaseAlignment) {
        Build { fieldScript = fieldScript.clone(); }
        @Override public byte[] fieldScript() { return fieldScript.clone(); }
    }

    static Build build(RomProfile rom, int ramScriptOffset, long moduleStagingAddress, int moduleStagingCapacity) {
        if (rom == null) throw new IllegalArgumentException("rom required");
        if (ramScriptOffset < 0 || ramScriptOffset >= RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException("invalid RamScript service offset");
        }

        long serviceVirtualBase = RAMSCRIPT_VIRTUAL_BASE + Integer.toUnsignedLong(ramScriptOffset);
        long copier = rom.stringVar4;
        long loaderAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper loader = PersistentNativeExecutionHelper.buildStagingLoaderAt(
                rom, loaderAddress, moduleStagingAddress, moduleStagingCapacity);

        RamScriptBuilder b = new RamScriptBuilder(serviceVirtualBase);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, serviceVirtualBase, loader, copier,
                "shared_persistent_native_stage_loader", NativeHelperInstaller.Mode.AUTO);
        install.installAndCall(b); // stage selected module; loader returns
        b.returnScript();

        byte[] script = b.buildScript();
        return new Build(script, loader.size(), install.requiredScriptBaseAlignment());
    }

    static int offsetForBindings(int bindingCount, int requiredAlignment) {
        int afterDispatcher = SharedHotkeyRuntime.PAYLOAD_OFFSET
                + SharedHotkeyRuntime.dispatcherSize(bindingCount);
        return align(afterDispatcher, requiredAlignment);
    }

    private static int align(int value, int alignment) {
        if (alignment <= 1) return value;
        if ((alignment & (alignment - 1)) != 0) {
            throw new IllegalArgumentException("alignment must be a power of two");
        }
        return (value + alignment - 1) & ~(alignment - 1);
    }
}

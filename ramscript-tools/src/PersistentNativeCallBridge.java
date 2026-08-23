/*
   Deferred Field Script -> persistent THUMB bridge.

   Small native modules may use the validated direct-dispatch path. Complex
   modules use the production stage-then-callnative path: a temporary loader
   resolves and validates the SB2 module, copies it to stable EWRAM scratch,
   returns to the Field Script, and the stock callnative command enters it.

   No preset-specific code is installed in IWRAM.
*/
final class PersistentNativeCallBridge {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final int VAR_RESULT = 0x800D;
    private static final int VAR_MODULE_ID = 0x8005;

    private PersistentNativeCallBridge() {}

    record Build(byte[] fieldScript, int nativeHelperBytes, int fieldScriptBytes, int requiredBaseAlignment) {
        Build { fieldScript = fieldScript.clone(); }
        @Override public byte[] fieldScript() { return fieldScript.clone(); }

        /* Historical accessor retained for source compatibility with Build-21 tests/tools. */
        @Deprecated int dispatcherBytes() { return nativeHelperBytes; }
    }

    static Build build(RomProfile rom, int moduleId, int successValue,
                       java.util.function.Consumer<RamScriptBuilder> beforeCall,
                       java.util.function.Consumer<RamScriptBuilder> onSuccess,
                       java.util.function.Consumer<RamScriptBuilder> onFailure) {
        if (rom == null) throw new IllegalArgumentException("rom required");
        if (moduleId < 0 || moduleId > 0xFFFF) throw new IllegalArgumentException("module id must be u16");

        long copier = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper dispatcher = PersistentNativeExecutionHelper.buildDirectDispatcherAt(rom, helperAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, dispatcher, copier, "persistent_native_dispatch", NativeHelperInstaller.Mode.AUTO
        );

        b.lockAll().setVar(VAR_RESULT, 0).setVar(VAR_MODULE_ID, moduleId);
        beforeCall.accept(b);
        install.installAndCall(b);
        b.compareVarToValue(VAR_RESULT, successValue)
                .vGotoIfNotEqual("native_fail");
        onSuccess.accept(b);
        b.label("native_fail");
        onFailure.accept(b);
        byte[] script = b.buildScript();
        return new Build(script, dispatcher.size(), script.length, install.requiredScriptBaseAlignment());
    }
    /* Production path for complex persistent native modules (validated in Build 24).
       The loader only resolves/validates/copies the module and RETURNS to the
       Field Script. The stock script engine then enters the staged helper via
       callnative, matching the validated standalone Party IV calling path. */
    static Build buildStagedThenCallNative(
            RomProfile rom, int moduleId, long moduleStagingAddress, int moduleStagingCapacity,
            java.util.function.Consumer<RamScriptBuilder> beforeCall,
            java.util.function.Consumer<RamScriptBuilder> onSuccess,
            java.util.function.Consumer<RamScriptBuilder> onFailure) {
        if (rom == null) throw new IllegalArgumentException("rom required");
        if (moduleId < 0 || moduleId > 0xFFFF) throw new IllegalArgumentException("module id must be u16");

        long copier = rom.stringVar4;
        long loaderAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper loader = PersistentNativeExecutionHelper.buildStagingLoaderAt(
                rom, loaderAddress, moduleStagingAddress, moduleStagingCapacity);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, loader, copier,
                "persistent_native_stage_loader", NativeHelperInstaller.Mode.AUTO);

        b.lockAll().setVar(VAR_RESULT, 0).setVar(VAR_MODULE_ID, moduleId);
        beforeCall.accept(b);
        install.installAndCall(b); // loader returns; it never jumps into module
        b.compareVarToValue(VAR_RESULT, 1).vGotoIfNotEqual("native_fail");
        b.callNative(moduleStagingAddress | 1L); // stock ScriptContext callnative contract
        onSuccess.accept(b);
        b.label("native_fail");
        onFailure.accept(b);
        byte[] script = b.buildScript();
        return new Build(script, loader.size(), script.length, install.requiredScriptBaseAlignment());
    }

    /* Shared-service path: the generic staging loader lives once in the
       shared RamScript runtime. This bridge only selects a module, vcalls the
       service, restores its own relocation state, and then uses stock
       callnative on the staged module. */
    static Build buildViaSharedStagingService(
            RomProfile rom, int moduleId, long sharedServiceVirtualTarget, long moduleStagingAddress,
            java.util.function.Consumer<RamScriptBuilder> beforeCall,
            java.util.function.Consumer<RamScriptBuilder> onSuccess,
            java.util.function.Consumer<RamScriptBuilder> onFailure) {
        if (rom == null) throw new IllegalArgumentException("rom required");
        if (moduleId < 0 || moduleId > 0xFFFF) throw new IllegalArgumentException("module id must be u16");
        if (sharedServiceVirtualTarget < 0 || sharedServiceVirtualTarget > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("shared service target must fit in u32");
        }

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        b.lockAll().setVar(VAR_RESULT, 0).setVar(VAR_MODULE_ID, moduleId);
        beforeCall.accept(b);
        b.vCallAddress(sharedServiceVirtualTarget);
        // The shared service executes setvaddress for its own RamScript base.
        // Restore this bridge's original relocation offset from the CURRENT
        // opcode position, not from script offset 0.
        b.setVAddressHere();
        b.compareVarToValue(VAR_RESULT, 1).vGotoIfNotEqual("native_fail");
        b.callNative(moduleStagingAddress | 1L);
        onSuccess.accept(b);
        b.label("native_fail");
        onFailure.accept(b);
        byte[] script = b.buildScript();
        return new Build(script, 0, script.length, 1);
    }

}

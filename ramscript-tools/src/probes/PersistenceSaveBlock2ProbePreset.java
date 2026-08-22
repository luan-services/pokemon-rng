final class PersistenceSaveBlock2ProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_RESULT = 0x800D;
    private PersistenceSaveBlock2ProbePreset() {}
    private static long copierAddress(RomProfile rom) { return rom.stringVar4 + 0x100L; }
    private static long helperAddress(RomProfile rom) { return CpuSetNativeHelperInstaller.helperDestination(copierAddress(rom)); }

    static RamScript buildInstaller(RomProfile rom) {
        NativeHelper helper = PersistenceSaveBlock2ProbeNativeHelper.buildWriterAt(rom, helperAddress(rom));
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE); b.setVAddress();
        NativeHelperInstaller.Plan p = NativeHelperInstaller.prepare(b, VIRTUAL_BASE, helper, copierAddress(rom), "persist1024_write", NativeHelperInstaller.Mode.AUTO);
        b.lockAll(); p.installAndCall(b);
        return RamScript.createWonderCard(b.vMessage("message").waitMessage().waitButtonPress().releaseAll().end()
                .text("message", "1024-byte persistence pattern installed.\\nSave normally, then reset.").buildScript());
    }

    static RamScript buildChecker(RomProfile rom) {
        NativeHelper helper = PersistenceSaveBlock2ProbeNativeHelper.buildCheckerAt(rom, helperAddress(rom));
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE); b.setVAddress();
        NativeHelperInstaller.Plan p = NativeHelperInstaller.prepare(b, VIRTUAL_BASE, helper, copierAddress(rom), "persist1024_check", NativeHelperInstaller.Mode.AUTO);
        b.lockAll(); p.installAndCall(b);
        return RamScript.createWonderCard(b.compareVarToValue(VAR_RESULT, 1).vGotoIfEqual("found")
                .vMessage("missing").waitMessage().waitButtonPress().releaseAll().end().label("found")
                .vMessage("foundMsg").waitMessage().waitButtonPress().releaseAll().end()
                .text("missing", "1024-byte persistence check FAILED.")
                .text("foundMsg", "Persistent storage OK!\\n1024/1024 bytes survived.").buildScript());
    }
}

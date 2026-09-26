final class MewCoreInstallerPreset {
    private static final long VIRTUAL_BASE=0x08010000L;
    private static final int VAR_RESULT=0x800D;
    private MewCoreInstallerPreset(){}

    static RamScript build(RomProfile rom){
        byte[] image=MewCoreV1.build(rom).bytes();
        RamScriptBuilder b=new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();

        // Keep the large image inert in the RamScript. The helper below copies
        // directly from this relocated source to live SB2; it is never staged
        // in gStringVar4 (the V1 regression).
        String after="__mew_core_after_raw";
        b.vGoto(after);
        int pad=(4-(b.position()&3))&3;
        b.padding(pad);
        int rawOffset=b.position();
        b.raw(image).label(after);

        long copier=rom.stringVar4+0x100L;
        long helperAddr=CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper h=MewCoreInstallerNative.buildAt(rom,helperAddr,VIRTUAL_BASE+Integer.toUnsignedLong(rawOffset),image.length);
        NativeHelperInstaller.Plan p=NativeHelperInstaller.prepare(b,VIRTUAL_BASE,h,copier,"mew_core_v1_transport",NativeHelperInstaller.Mode.AUTO);
        b.lockAll().setVar(VAR_RESULT,0);
        p.installAndCall(b);
        return RamScript.createWonderCard(b.compareVarToValue(VAR_RESULT,1).vGotoIfEqual("ok")
            .vMessage("bad").waitMessage().waitButtonPress().releaseAll().end()
            .label("ok").vMessage("okmsg").waitMessage().waitButtonPress().releaseAll().end()
            .text("bad","Mew Core installation failed.")
            .text("okmsg","Mew Core installed.\\nSave the game before the event card.")
            .buildScript());
    }
}

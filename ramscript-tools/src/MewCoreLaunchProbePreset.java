final class MewCoreLaunchProbePreset {
    private static final long VIRTUAL_BASE=0x08010000L;
    private static final int VAR_RESULT=0x800D;
    private static final int VAR_SELECTOR=0x8005;
    private static final int SPECIES_MEW=151;
    private MewCoreLaunchProbePreset(){}

    static RamScript build(RomProfile rom){
        if(rom!=RomProfile.FIRE_RED_EN_10) throw new IllegalArgumentException("Mew Core launch probe is FR1.0-only");
        RamScriptBuilder b=new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("start");
        int afterBattle=b.position();
        b.label("after_battle").setVAddressHere().vGoto("after_body");

        long copier=rom.stringVar4+0x100L;
        long helperAddr=CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper dispatcher=MewCoreLauncherNative.buildDispatcherAt(rom,helperAddr);
        NativeHelperInstaller.Plan dp=NativeHelperInstaller.prepare(b,VIRTUAL_BASE,dispatcher,copier,"mew_core_dispatch",NativeHelperInstaller.Mode.AUTO);

        b.label("after_body")
         .special(180)
         .vMessage("returned").waitMessage().waitButtonPress().releaseAll().end();

        byte[] callback=TradeEvolutionContinuationRuntime.callback(rom,afterBattle);
        byte[] literals=TradeEvolutionContinuationRuntime.callbackLiterals(rom);

        b.label("start").lockAll()
         .setWildBattle(SPECIES_MEW,100,0)
         .setVar(VAR_RESULT,1)
         .setVar(VAR_SELECTOR,0);
        dp.installAndCall(b); // persistent prepare: backup + clear Ball pocket
        b.compareVarToValue(VAR_RESULT,0).vGotoIfEqual("bad")
         .writeBytes(TradeEvolutionContinuationRuntime.CALLBACK,callback)
         .writeBytes(TradeEvolutionContinuationRuntime.LITERAL_GET_RAM_SCRIPT,literals)
         .writeBytes(rom.fieldCallback2,le32(TradeEvolutionContinuationRuntime.CALLBACK|1L))
         .setVar(VAR_SELECTOR,1);
        dp.installAndCall(b); // persistent launch entry
        b.waitState().end()
         .label("bad").vMessage("badmsg").waitMessage().waitButtonPress().releaseAll().end()
         .text("badmsg","Mew Core missing or invalid.")
         .text("returned","Mew Core battle returned successfully.");
        return RamScript.createWonderCard(b.buildScript());
    }
    private static byte[] le32(long v){return new byte[]{(byte)v,(byte)(v>>>8),(byte)(v>>>16),(byte)(v>>>24)};}
}

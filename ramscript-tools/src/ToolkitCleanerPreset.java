import java.io.ByteArrayOutputStream;
import java.util.Map;

/* Safe cleaner for planner-generated persistent installations.
   It refuses to touch save storage unless the Build-34+ manifest is present,
   and refuses to clean while the resident hotkey wrapper is active. */
final class ToolkitCleanerPreset {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final int VAR_RESULT = 0x800D;
    private static final int STATUS_NOT_FOUND = 0;
    private static final int STATUS_CLEANED = 1;
    private static final int STATUS_RUNTIME_ACTIVE = 2;
    private static final long GMAIN_CALLBACK1 = 0x030030F0L;

    private ToolkitCleanerPreset() {}

    static RamScript build(RomProfile rom) {
        return build(rom, ToolkitCleanerPlan.resolve(ToolkitCleanerOptions.infrastructureOnly()));
    }

    static RamScript build(RomProfile rom, ToolkitCleanerPlan plan) {
        long copier = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper helper = new NativeHelper(helperAddress, helperBytes(rom, helperAddress, plan));

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copier, "toolkit_cleaner", NativeHelperInstaller.Mode.AUTO);
        b.lockAll();
        install.installAndCall(b);
        b.compareVarToValue(VAR_RESULT, STATUS_CLEANED).vGotoIfEqual("cleaned");
        b.compareVarToValue(VAR_RESULT, STATUS_RUNTIME_ACTIVE).vGotoIfEqual("runtime_active");
        b.vMessage("not_found").waitMessage().waitButtonPress().nop().releaseAll().end();
        b.label("runtime_active")
                .vMessage("runtime_msg").waitMessage().waitButtonPress().nop().releaseAll().end();
        b.label("cleaned")
                .vMessage("cleaned_msg").waitMessage().waitButtonPress().nop().releaseAll().end();
        b.text("not_found", "No compatible toolkit installation was found.\\nNothing was changed.");
        b.text("runtime_msg", "Toolkit runtime is still active.\\nRestart the game, then run Cleaner again.");
        String cleanedMessage = plan.mode() == ToolkitCleanerMode.INFRASTRUCTURE_ONLY
                ? "Toolkit data cleared.\\nEvent progress was preserved."
                : (plan.flagsToPreserve().isEmpty()
                    ? "Toolkit data and progress cleared.\\nSave, then restart before reinstalling."
                    : "Toolkit data cleared.\\nSelected event progress was preserved.");
        b.text("cleaned_msg", cleanedMessage);
        return RamScript.createWonderCard(b.buildScript());
    }

    static byte[] helperBytesForTest(RomProfile rom) {
        return helperBytesForTest(rom, ToolkitCleanerPlan.resolve(ToolkitCleanerOptions.infrastructureOnly()));
    }

    static byte[] helperBytesForTest(RomProfile rom, ToolkitCleanerPlan plan) {
        long address = CpuSetNativeHelperInstaller.helperDestination(rom.stringVar4 + 0x100L);
        return helperBytes(rom, address, plan);
    }

    private static byte[] helperBytes(RomProfile rom, long address, ToolkitCleanerPlan plan) {
        Thumb out = new Thumb();
        out.u16(0xB5F0); // push {r4-r7,lr}

        // Refuse to clear persistent gateways while our resident wrapper owns callback1.
        out.ldrLiteral(4, "cb1_ptr");
        out.u16(0x6824); // ldr r4,[r4]
        out.ldrLiteral(5, "wrapper");
        out.u16(0x42AC); // cmp r4,r5
        out.bcond(0x0, "runtime_active"); // beq

        // Validate manifest magic/version/size/secondary marker.
        out.ldrLiteral(4, "sb2_ptr");
        out.u16(0x6824); // ldr r4,[r4]
        out.ldrLiteral(5, "manifest_off");
        out.u16(addReg(5,4,5));
        out.ldrLiteral(6, "magic");
        out.u16(ldrWordImm(7,5,0));
        out.u16(0x42B7); // cmp r7,r6
        out.bcond(0x1, "not_found");
        out.u16(ldrhImm(7,5,4));
        out.u16(0x2F01); // cmp r7,#1
        out.bcond(0x1, "not_found");
        out.u16(ldrhImm(7,5,6));
        out.u16(0x2F20); // cmp r7,#32
        out.bcond(0x1, "not_found");
        out.ldrLiteral(6, "marker2");
        out.u16(ldrWordImm(7,5,8));
        out.u16(0x42B7);
        out.bcond(0x1, "not_found");

        // Clear the complete toolkit-reserved SB1 area: 400 B = 100 words.
        out.ldrLiteral(4, "sb1_ptr");
        out.u16(0x6824);
        out.ldrLiteral(5, "sb1_off");
        out.u16(addReg(4,4,5));
        out.u16(0x2500); // movs r5,#0
        out.u16(0x2664); // movs r6,#100
        out.label("sb1_loop");
        out.u16(strWordImm(5,4,0));
        out.u16(0x3404); // adds r4,#4
        out.u16(0x3E01); // subs r6,#1
        out.bcond(0x1, "sb1_loop");

        // Optional authored-progress wipe. Policy comes from ToolkitCleanerPlan;
        // the native Cleaner only receives byte masks and never owns feature IDs.
        int flagMaskIndex = 0;
        for (Map.Entry<Integer, Integer> entry : plan.flagAndMasks().entrySet()) {
            String offsetLiteral = "progress_flag_off_" + flagMaskIndex++;
            out.ldrLiteral(4, "sb1_ptr");
            out.u16(0x6824);
            out.ldrLiteral(6, offsetLiteral);
            out.u16(addReg(4,4,6));
            out.u16(ldrbImm(0,4,0));
            out.u16(0x2100 | (entry.getValue() & 0xFF)); // movs r1,#andMask
            out.u16(0x4008); // ands r0,r1
            out.u16(strbImm(0,4,0));
            out.literal(offsetLiteral, entry.getKey());
        }

        // Clear complete toolkit-reserved SB2 area: 1024 B = 128 * 8 B.
        out.ldrLiteral(4, "sb2_ptr");
        out.u16(0x6824);
        out.ldrLiteral(6, "sb2_off");
        out.u16(addReg(4,4,6));
        out.u16(0x2680); // movs r6,#128
        out.label("sb2_loop");
        out.u16(strWordImm(5,4,0));
        out.u16(strWordImm(5,4,4));
        out.u16(0x3408); // adds r4,#8
        out.u16(0x3E01);
        out.bcond(0x1, "sb2_loop");
        out.movStatusAndReturn(STATUS_CLEANED, rom.specialVarResult);

        out.label("runtime_active");
        out.movStatusAndReturn(STATUS_RUNTIME_ACTIVE, rom.specialVarResult);
        out.label("not_found");
        out.movStatusAndReturn(STATUS_NOT_FOUND, rom.specialVarResult);

        out.align4();
        out.literal("cb1_ptr", GMAIN_CALLBACK1);
        out.literal("wrapper", RuntimeV1ResidentBlocks.WRAPPER | 1L);
        out.literal("sb2_ptr", rom.saveBlock2Ptr);
        out.literal("manifest_off", InstallationManifest.OFFSET);
        out.literal("magic", 0x4B505352L); // "RSPK" little-endian
        out.literal("marker2", Integer.toUnsignedLong(InstallationManifest.SECONDARY_MARKER));
        out.literal("sb1_ptr", rom.saveBlock1Ptr);
        out.literal("sb1_off", PayloadStorageArea.SAVE_BLOCK1.offset());
        out.literal("sb2_off", PayloadStorageArea.SAVE_BLOCK2.offset());
        out.literal("result", rom.specialVarResult);
        return out.build(address);
    }

    private static int addReg(int rd,int rn,int rm){ return 0x1800 | (rm<<6) | (rn<<3) | rd; }
    private static int ldrWordImm(int rd,int rn,int off){ return 0x6800 | ((off/4)<<6) | (rn<<3) | rd; }
    private static int strWordImm(int rd,int rn,int off){ return 0x6000 | ((off/4)<<6) | (rn<<3) | rd; }
    private static int ldrhImm(int rd,int rn,int off){ return 0x8800 | ((off/2)<<6) | (rn<<3) | rd; }
    private static int ldrbImm(int rd,int rn,int off){ return 0x7800 | (off<<6) | (rn<<3) | rd; }
    private static int strbImm(int rd,int rn,int off){ return 0x7000 | (off<<6) | (rn<<3) | rd; }

    private static final class Thumb {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final java.util.Map<String,Integer> labels = new java.util.HashMap<>();
        private final java.util.List<BranchFix> branches = new java.util.ArrayList<>();
        private final java.util.List<LiteralFix> literalLoads = new java.util.ArrayList<>();
        private final java.util.Map<String,Long> literalValues = new java.util.LinkedHashMap<>();

        int pos(){ return out.size(); }
        void u16(int v){ out.write(v&0xFF); out.write((v>>>8)&0xFF); }
        void label(String s){ labels.put(s,pos()); }
        void bcond(int cond,String label){ int p=pos();u16(0xD000|(cond<<8));branches.add(new BranchFix(p,label)); }
        void ldrLiteral(int rt,String name){ int p=pos();u16(0x4800|(rt<<8));literalLoads.add(new LiteralFix(p,rt,name)); }
        void literal(String name,long value){ literalValues.put(name,value); }
        void align4(){ while((pos()&3)!=0)u16(0x46C0); }
        void movStatusAndReturn(int status,long resultAddr){
            ldrLiteral(4,"result"); u16(0x2500|status); u16(0x8025); u16(0xBDF0);
        }
        byte[] build(long address){
            int literalStart=pos();
            java.util.Map<String,Integer> literalPos=new java.util.HashMap<>();
            for(var e:literalValues.entrySet()){ literalPos.put(e.getKey(),pos()); long v=e.getValue(); out.write((int)v&0xFF);out.write((int)(v>>>8)&0xFF);out.write((int)(v>>>16)&0xFF);out.write((int)(v>>>24)&0xFF); }
            byte[] b=out.toByteArray();
            for(BranchFix f:branches){Integer target=labels.get(f.label);if(target==null)throw new IllegalStateException("missing label "+f.label);int insn=(b[f.pos]&0xFF)|((b[f.pos+1]&0xFF)<<8);int delta=target-(f.pos+4);int h=delta/2;if((delta&1)!=0||h< -128||h>127)throw new IllegalStateException("branch range");insn=(insn&0xFF00)|(h&0xFF);put16(b,f.pos,insn);}
            for(LiteralFix f:literalLoads){Integer lp=literalPos.get(f.name);if(lp==null)throw new IllegalStateException("missing literal "+f.name);int base=(f.pos+4)&~3;int d=lp-base;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("literal range");put16(b,f.pos,0x4800|(f.rt<<8)|(d/4));}
            return b;
        }
        private static void put16(byte[]b,int p,int v){b[p]=(byte)v;b[p+1]=(byte)(v>>>8);}
        record BranchFix(int pos,String label){} record LiteralFix(int pos,int rt,String name){}
    }
}

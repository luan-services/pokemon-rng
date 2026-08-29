import java.io.ByteArrayOutputStream;
import java.util.*;

/* Small transport helpers. These use the already validated NativeHelperInstaller
   only to stage the transport itself; they never place trainer-specific code in IWRAM. */
final class CustomTrainerRuntimeTransportNative {
    private CustomTrainerRuntimeTransportNative() {}

    static long loaderStagingAddress(RomProfile rom) { return rom.stringVar4 + 0x40L; }
    // The runtime installer uses the same +0x100 copier / +0x140 helper staging
    // pair already exercised by the persistent SB2 probes. Descriptor loaders
    // keep their separate +0x40 staging because they later copy the resident
    // runtime into +0x140.
    static long runtimeInstallerStagingAddress(RomProfile rom) {
        return CpuSetNativeHelperInstaller.helperDestination(rom.stringVar4 + 0x100L);
    }

    static NativeHelper runtimeInstaller(RomProfile rom, byte[] runtimeImage) {
        long address = runtimeInstallerStagingAddress(rom);
        Thumb t = new Thumb(address);
        t.label("entry");
        t.u16(0xB5F0);
        t.ldrLit(4,"sb2ptr"); t.u16(0x6824); // ldr r4,[r4]
        t.ldrLit(1,"runtimeHeaderOffset"); t.u16(addReg(4,4,1));
        t.adr(5,"runtime_image");
        t.ldrLit(6,"runtimeImageSize");
        t.label("copy_loop");
        t.u16(ldrByte(0,5,0)); t.u16(strByte(0,4,0));
        t.u16(0x3501); t.u16(0x3401); t.u16(0x3E01); t.bcond(1,"copy_loop");

        // Do not claim success just because the helper returned. Re-resolve the
        // live SaveBlock2 and validate the header we actually wrote.
        t.ldrLit(4,"sb2ptr"); t.u16(0x6824);
        t.ldrLit(1,"runtimeHeaderOffset"); t.u16(addReg(4,4,1));
        t.u16(ldrWord(0,4,0)); t.ldrLit(1,"runtimeMagic"); t.u16(cmpReg(0,1)); t.bcond(1,"install_fail");
        t.u16(ldrHalf(0,4,4)); t.u16(0x2800|CustomTrainerRuntimeStorage.VERSION); t.bcond(1,"install_fail");
        t.u16(ldrHalf(0,4,8)); t.ldrLit(1,"expectedRuntimeSize"); t.u16(cmpReg(0,1)); t.bcond(1,"install_fail");
        t.ldrLit(0,"result"); t.u16(0x2101); t.u16(0x8001); t.u16(0xBDF0);
        t.label("install_fail");
        t.ldrLit(0,"result"); t.u16(0x2100); t.u16(0x8001); t.u16(0xBDF0);
        t.align4();
        t.literal("sb2ptr",rom.saveBlock2Ptr);
        t.literal("runtimeHeaderOffset",CustomTrainerRuntimeStorage.HEADER_OFFSET);
        t.literal("runtimeImageSize",runtimeImage.length);
        t.literal("runtimeMagic",Integer.toUnsignedLong(CustomTrainerRuntimeStorage.MAGIC));
        t.literal("expectedRuntimeSize",runtimeImage.length-CustomTrainerRuntimeStorage.HEADER_SIZE);
        t.literal("result",rom.specialVarResult);
        t.label("runtime_image"); t.raw(runtimeImage);
        return new NativeHelper(address,t.finish());
    }

    static NativeHelper descriptorLoader(RomProfile rom, byte[] descriptor, byte[] fieldTextImage) {
        byte[] manifestImage = InstallationManifest.buildCustomTrainer();
        if (descriptor.length > CustomTrainerRuntimeStorage.DESCRIPTOR_CAPACITY)
            throw new IllegalArgumentException("descriptor too large: "+descriptor.length+"/"+CustomTrainerRuntimeStorage.DESCRIPTOR_CAPACITY);
        if (fieldTextImage.length > CustomTrainerFieldTextStorage.CAPACITY)
            throw new IllegalArgumentException("field text image too large: "+fieldTextImage.length+"/"+CustomTrainerFieldTextStorage.CAPACITY);
        long address = loaderStagingAddress(rom);
        Thumb t = new Thumb(address);
        t.label("entry");
        t.u16(0xB5F0);
        t.ldrLit(4,"sb2ptr"); t.u16(0x6824); // live SB2

        // Validate shared runtime header.
        t.u16(movReg(5,4)); t.ldrLit(1,"headerOffset"); t.u16(addReg(5,5,1));
        t.u16(ldrWord(0,5,0)); t.ldrLit(1,"runtimeMagic"); t.u16(cmpReg(0,1)); t.bcond(1,"fail");
        t.u16(ldrHalf(0,5,4)); t.u16(0x2800|CustomTrainerRuntimeStorage.VERSION); t.bcond(1,"fail");
        t.u16(ldrHalf(7,5,8)); // runtime size
        t.u16(0x2F00); t.bcond(0,"fail");
        t.ldrLit(1,"runtimeCapacity"); t.u16(cmpReg(7,1)); t.bcond(8,"fail");

        // Install this card's descriptor.
        t.u16(movReg(6,4)); t.ldrLit(1,"descriptorOffset"); t.u16(addReg(6,6,1));
        t.adr(5,"descriptor_data");
        t.ldrLit(2,"descriptorSize");
        t.label("copy_descriptor");
        t.u16(ldrByte(0,5,0)); t.u16(strByte(0,6,0));
        t.u16(0x3501); t.u16(0x3601); t.u16(0x3A01); t.bcond(1,"copy_descriptor");

        // Install this card's variable-length overworld dialogue in the
        // validated 400-byte SaveBlock1 auxiliary area.
        t.ldrLit(6,"sb1ptr"); t.u16(0x6836);
        t.ldrLit(1,"fieldTextOffset"); t.u16(addReg(6,6,1));
        t.adr(5,"field_text_data");
        t.ldrLit(2,"fieldTextSize");
        t.label("copy_field_text");
        t.u16(ldrByte(0,5,0)); t.u16(strByte(0,6,0));
        t.u16(0x3501); t.u16(0x3601); t.u16(0x3A01); t.bcond(1,"copy_field_text");

        // Tag the complete Custom Trainer installation for the existing Cleaner.
        t.ldrLit(6,"sb2ptr"); t.u16(0x6836);
        t.ldrLit(1,"manifestOffset"); t.u16(addReg(6,6,1));
        t.adr(5,"manifest_data");
        t.u16(0x2220); // 32 bytes
        t.label("copy_manifest");
        t.u16(ldrByte(0,5,0)); t.u16(strByte(0,6,0));
        t.u16(0x3501); t.u16(0x3601); t.u16(0x3A01); t.bcond(1,"copy_manifest");

        // Copy the persistent shared runtime to stable gStringVar4 scratch.
        // All literal loads needed after this point happen before the loop;
        // the destination may overwrite the helper's embedded descriptor/literal tail.
        t.u16(movReg(5,4)); t.ldrLit(1,"runtimeOffset"); t.u16(addReg(5,5,1));
        t.ldrLit(6,"runtimeStaging");
        // The destination starts only 0x100 bytes after this loader. The runtime
        // copy intentionally overwrites the loader's literal/data tail, so every
        // value needed after the loop must already be held in a register.
        // Probe v1 violated this rule by loading `result` after the copy.
        t.ldrLit(3,"result");
        t.u16(movReg(2,7));
        t.label("copy_runtime");
        t.u16(ldrByte(0,5,0)); t.u16(strByte(0,6,0));
        t.u16(0x3501); t.u16(0x3601); t.u16(0x3A01); t.bcond(1,"copy_runtime");
        t.u16(movReg(0,3)); t.u16(0x2101); t.u16(0x8001); t.u16(0xBDF0);

        t.label("fail");
        t.ldrLit(0,"result"); t.u16(0x2100); t.u16(0x8001); t.u16(0xBDF0);

        t.align4();
        t.literal("sb2ptr",rom.saveBlock2Ptr);
        t.literal("sb1ptr",rom.saveBlock1Ptr);
        t.literal("headerOffset",CustomTrainerRuntimeStorage.HEADER_OFFSET);
        t.literal("runtimeMagic",Integer.toUnsignedLong(CustomTrainerRuntimeStorage.MAGIC));
        t.literal("runtimeCapacity",CustomTrainerRuntimeStorage.RUNTIME_CAPACITY);
        t.literal("descriptorOffset",CustomTrainerRuntimeStorage.DESCRIPTOR_OFFSET);
        t.literal("descriptorSize",descriptor.length);
        t.literal("fieldTextOffset",CustomTrainerFieldTextStorage.OFFSET);
        t.literal("fieldTextSize",fieldTextImage.length);
        t.literal("manifestOffset",InstallationManifest.OFFSET);
        t.literal("runtimeOffset",CustomTrainerRuntimeStorage.RUNTIME_OFFSET);
        t.literal("runtimeStaging",CustomTrainerBattleRuntimeV1.stagingAddress(rom));
        t.literal("result",rom.specialVarResult);
        t.align4(); t.label("descriptor_data"); t.raw(descriptor);
        t.align4(); t.label("field_text_data"); t.raw(fieldTextImage);
        t.align4(); t.label("manifest_data"); t.raw(manifestImage);
        return new NativeHelper(address,t.finish());
    }


    /*
       Production transport: decode the compact Wonder Card stream directly to
       the normal persistent CTD1 descriptor. Runtime V2 never sees this format.
    */
    static NativeHelper compactDescriptorLoader(RomProfile rom, byte[] compactDescriptor, byte[] fieldTextImage) {
        return compactDescriptorLoader(rom, compactDescriptor, fieldTextImage, null);
    }

    /* Giovanni/Five Island specialization. The shared Runtime V2 and CTD1 remain
       unchanged; only this card's transient loader swaps the currently loaded host
       object's graphics before installing the normal descriptor. */
    static NativeHelper compactDescriptorLoaderWithObjectGraphics(
            RomProfile rom, byte[] compactDescriptor, byte[] fieldTextImage,
            ObjectEventTarget target, int graphicsId) {
        return compactDescriptorLoader(rom, compactDescriptor, fieldTextImage,
                new ObjectGraphicsSwap(target.localId(), target.mapNum(), target.mapGroup(), graphicsId,
                        objectEventSetGraphicsIdByLocalIdAndMapThumb(rom)));
    }

    private static NativeHelper compactDescriptorLoader(
            RomProfile rom, byte[] compactDescriptor, byte[] fieldTextImage, ObjectGraphicsSwap graphicsSwap) {
        byte[] manifestImage = InstallationManifest.buildCustomTrainer();
        if (compactDescriptor.length <= 1)
            throw new IllegalArgumentException("compact descriptor required");
        if (fieldTextImage.length > CustomTrainerFieldTextStorage.CAPACITY)
            throw new IllegalArgumentException("field text image too large: "+fieldTextImage.length+"/"+CustomTrainerFieldTextStorage.CAPACITY);
        long address = loaderStagingAddress(rom);
        Thumb t = new Thumb(address);
        t.label("entry");
        t.u16(0xB5F0);
        t.ldrLit(4,"sb2ptr"); t.u16(0x6824);

        // Same validated shared-runtime header gate as the production loader.
        t.u16(movReg(5,4)); t.ldrLit(1,"headerOffset"); t.u16(addReg(5,5,1));
        t.u16(ldrWord(0,5,0)); t.ldrLit(1,"runtimeMagic"); t.u16(cmpReg(0,1)); t.bcondFar(1,"fail");
        t.u16(ldrHalf(0,5,4)); t.u16(0x2800|CustomTrainerRuntimeStorage.VERSION); t.bcondFar(1,"fail");
        t.u16(ldrHalf(7,5,8));
        t.u16(0x2F00); t.bcondFar(0,"fail");
        t.ldrLit(1,"runtimeCapacity"); t.u16(cmpReg(7,1)); t.bcondFar(8,"fail");

        if (graphicsSwap != null) {
            // AAPCS: r0-r3 are arguments; r4-r7 are callee-saved. Keep the
            // validated runtime size in r7 and branch through r6 (ARMv4T-safe).
            t.u16(0x2000 | graphicsSwap.localId());
            t.u16(0x2100 | graphicsSwap.mapNum());
            t.u16(0x2200 | graphicsSwap.mapGroup());
            t.u16(0x2300 | graphicsSwap.graphicsId());
            t.ldrLit(6, "setObjectGraphics");
            t.bl("thunk_r6");
        }

        t.adr(5,"compact_descriptor_data");
        t.u16(movReg(6,4)); t.ldrLit(1,"descriptorOffset"); t.u16(addReg(6,6,1));

        // Tiny sequential decoder. The stream itself is build-generated; the
        // normal CTD1 runtime validation still happens before battle execution.
        t.label("decode_next");
        t.u16(ldrByte(0,5,0)); t.u16(0x3501);
        t.u16(0x28FF); t.bcond(0,"decode_done");
        t.u16(0x28FE); t.bcond(0,"emit_default_ot_iv");
        t.u16(0x2880); t.bcond(3,"literal_run"); // unsigned < 0x80
        t.u16(0x28C0); t.bcond(3,"zero_run");   // unsigned < 0xC0

        // FF run: count = command - 0xBF (C0=>1 ... FD=>62)
        t.u16(0x38BF); t.u16(movReg(2,0)); t.u16(0x21FF);
        t.label("fill_ff");
        t.u16(strByte(1,6,0)); t.u16(0x3601); t.u16(0x3A01); t.bcond(1,"fill_ff");
        t.b("decode_next");

        // Zero run: count = command - 0x7F (80=>1 ... BF=>64)
        t.label("zero_run");
        t.u16(0x387F); t.u16(movReg(2,0)); t.u16(0x2100);
        t.label("fill_zero");
        t.u16(strByte(1,6,0)); t.u16(0x3601); t.u16(0x3A01); t.bcond(1,"fill_zero");
        t.b("decode_next");

        // Literal: count = command + 1.
        t.label("literal_run");
        t.u16(0x3001); t.u16(movReg(2,0));
        t.label("copy_literal");
        t.u16(ldrByte(1,5,0)); t.u16(strByte(1,6,0));
        t.u16(0x3501); t.u16(0x3601); t.u16(0x3A01); t.bcond(1,"copy_literal");
        t.b("decode_next");

        // One-byte dictionary token for the production OT ID + all-31 IV word.
        t.label("emit_default_ot_iv");
        t.ldrLit(1,"defaultOtId"); t.u16(strWord(1,6,0)); t.u16(0x3604);
        t.ldrLit(1,"defaultIvWord"); t.u16(strWord(1,6,0)); t.u16(0x3604);
        t.b("decode_next");

        t.label("decode_done");

        // Existing SB1 dialogue installation.
        t.ldrLit(6,"sb1ptr"); t.u16(0x6836);
        t.ldrLit(1,"fieldTextOffset"); t.u16(addReg(6,6,1));
        t.adr(5,"field_text_data");
        t.ldrLit(2,"fieldTextSize");
        t.label("copy_field_text");
        t.u16(ldrByte(0,5,0)); t.u16(strByte(0,6,0));
        t.u16(0x3501); t.u16(0x3601); t.u16(0x3A01); t.bcond(1,"copy_field_text");

        // Existing manifest / Cleaner contract.
        t.ldrLit(6,"sb2ptr"); t.u16(0x6836);
        t.ldrLit(1,"manifestOffset"); t.u16(addReg(6,6,1));
        t.adr(5,"manifest_data");
        t.u16(0x2220);
        t.label("copy_manifest");
        t.u16(ldrByte(0,5,0)); t.u16(strByte(0,6,0));
        t.u16(0x3501); t.u16(0x3601); t.u16(0x3A01); t.bcond(1,"copy_manifest");

        // Existing persistent-runtime staging, including the preloaded result
        // pointer required by the self-overwrite fix.
        t.ldrLit(4,"sb2ptr"); t.u16(0x6824);
        t.u16(movReg(5,4)); t.ldrLit(1,"runtimeOffset"); t.u16(addReg(5,5,1));
        t.ldrLit(6,"runtimeStaging");
        t.ldrLit(3,"result");
        t.u16(movReg(2,7));
        t.label("copy_runtime");
        t.u16(ldrByte(0,5,0)); t.u16(strByte(0,6,0));
        t.u16(0x3501); t.u16(0x3601); t.u16(0x3A01); t.bcond(1,"copy_runtime");
        t.u16(movReg(0,3)); t.u16(0x2101); t.u16(0x8001); t.u16(0xBDF0);

        t.label("fail");
        t.ldrLit(0,"result"); t.u16(0x2100); t.u16(0x8001); t.u16(0xBDF0);

        if (graphicsSwap != null) {
            t.align4();
            t.label("thunk_r6"); t.u16(0x4730); // bx r6
        }

        t.align4();
        t.literal("sb2ptr",rom.saveBlock2Ptr);
        t.literal("sb1ptr",rom.saveBlock1Ptr);
        t.literal("headerOffset",CustomTrainerRuntimeStorage.HEADER_OFFSET);
        t.literal("runtimeMagic",Integer.toUnsignedLong(CustomTrainerRuntimeStorage.MAGIC));
        t.literal("runtimeCapacity",CustomTrainerRuntimeStorage.RUNTIME_CAPACITY);
        t.literal("descriptorOffset",CustomTrainerRuntimeStorage.DESCRIPTOR_OFFSET);
        t.literal("defaultOtId",CustomTrainerCompactTransport.DEFAULT_OT_ID);
        t.literal("defaultIvWord",CustomTrainerCompactTransport.DEFAULT_IV_WORD);
        t.literal("fieldTextOffset",CustomTrainerFieldTextStorage.OFFSET);
        t.literal("fieldTextSize",fieldTextImage.length);
        t.literal("manifestOffset",InstallationManifest.OFFSET);
        t.literal("runtimeOffset",CustomTrainerRuntimeStorage.RUNTIME_OFFSET);
        t.literal("runtimeStaging",CustomTrainerBattleRuntimeV1.stagingAddress(rom));
        t.literal("result",rom.specialVarResult);
        if (graphicsSwap != null) t.literal("setObjectGraphics", graphicsSwap.functionThumb());
        t.align4(); t.label("compact_descriptor_data"); t.raw(compactDescriptor);
        t.align4(); t.label("field_text_data"); t.raw(fieldTextImage);
        t.align4(); t.label("manifest_data"); t.raw(manifestImage);
        return new NativeHelper(address,t.finish());
    }

    static NativeHelper runtimeHeaderChecker(RomProfile rom) {
        long address = CpuSetNativeHelperInstaller.helperDestination(rom.stringVar4 + 0x100L);
        Thumb t = new Thumb(address);
        t.label("entry");
        t.u16(0xB510); // push {r4,lr}
        t.ldrLit(4,"sb2ptr"); t.u16(0x6824);
        t.ldrLit(1,"headerOffset"); t.u16(addReg(4,4,1));
        t.u16(ldrWord(0,4,0)); t.ldrLit(1,"magic"); t.u16(cmpReg(0,1)); t.bcond(1,"fail");
        t.u16(ldrHalf(0,4,4)); t.u16(0x2800|CustomTrainerRuntimeStorage.VERSION); t.bcond(1,"fail");
        t.u16(ldrHalf(0,4,8)); t.u16(0x2800); t.bcond(0,"fail");
        t.ldrLit(1,"capacity"); t.u16(cmpReg(0,1)); t.bcond(8,"fail");
        t.ldrLit(0,"result"); t.u16(0x2101); t.u16(0x8001); t.u16(0xBD10);
        t.label("fail");
        t.ldrLit(0,"result"); t.u16(0x2100); t.u16(0x8001); t.u16(0xBD10);
        t.align4();
        t.literal("sb2ptr",rom.saveBlock2Ptr);
        t.literal("headerOffset",CustomTrainerRuntimeStorage.HEADER_OFFSET);
        t.literal("magic",Integer.toUnsignedLong(CustomTrainerRuntimeStorage.MAGIC));
        t.literal("capacity",CustomTrainerRuntimeStorage.RUNTIME_CAPACITY);
        t.literal("result",rom.specialVarResult);
        return new NativeHelper(address,t.finish());
    }

    private record ObjectGraphicsSwap(int localId, int mapNum, int mapGroup, int graphicsId, long functionThumb) {
        ObjectGraphicsSwap {
            if ((localId | mapNum | mapGroup | graphicsId) < 0
                    || localId > 0xFF || mapNum > 0xFF || mapGroup > 0xFF || graphicsId > 0xFF)
                throw new IllegalArgumentException("object graphics arguments must fit u8");
        }
    }

    private static long objectEventSetGraphicsIdByLocalIdAndMapThumb(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10, LEAF_GREEN_EN_10 -> 0x0805F1D9L;
            case FIRE_RED_EN_11, LEAF_GREEN_EN_11 -> 0x0805F1EDL;
        };
    }

    private static int movReg(int rd,int rs){return 0x1C00|(rs<<3)|rd;}
    private static int addReg(int rd,int rn,int rm){return 0x1800|(rm<<6)|(rn<<3)|rd;}
    private static int cmpReg(int rn,int rm){return 0x4280|(rm<<3)|rn;}
    private static int subReg(int rd,int rn,int rm){return 0x1A00|(rm<<6)|(rn<<3)|rd;}
    private static int ldrWord(int rt,int rn,int off){return 0x6800|((off/4)<<6)|(rn<<3)|rt;}
    private static int ldrHalf(int rt,int rn,int off){return 0x8800|((off/2)<<6)|(rn<<3)|rt;}
    private static int ldrByte(int rt,int rn,int off){return 0x7800|(off<<6)|(rn<<3)|rt;}
    private static int strByte(int rt,int rn,int off){return 0x7000|(off<<6)|(rn<<3)|rt;}
    private static int strWord(int rt,int rn,int off){return 0x6000|((off/4)<<6)|(rn<<3)|rt;}

    private static final class Thumb {
        final long base; final ByteArrayOutputStream out=new ByteArrayOutputStream(); final Map<String,Integer> labels=new HashMap<>(); final List<Branch> branches=new ArrayList<>(); final List<LongBranch> longBranches=new ArrayList<>(); final List<Bl> bls=new ArrayList<>(); final List<Lit> loads=new ArrayList<>(); final List<Adr> adrs=new ArrayList<>(); final Map<String,Long> literals=new LinkedHashMap<>();
        Thumb(long base){this.base=base;} int pos(){return out.size();} void u16(int v){out.write(v&255);out.write((v>>>8)&255);} void raw(byte[]b){out.writeBytes(b);} void label(String s){if(labels.put(s,pos())!=null)throw new IllegalStateException("dup "+s);} void align4(){if((pos()&1)!=0)out.write(0);while((pos()&3)!=0)u16(0x46C0);} void bcond(int c,String l){int p=pos();u16(0xD000|(c<<8));branches.add(new Branch(p,l));} void b(String l){int p=pos();u16(0xE000);longBranches.add(new LongBranch(p,l));} void bl(String l){int p=pos();u16(0);u16(0);bls.add(new Bl(p,l));} void bcondFar(int c,String l){String skip="__far_skip_"+pos();bcond(c^1,skip);b(l);label(skip);} void ldrLit(int r,String n){int p=pos();u16(0x4800|(r<<8));loads.add(new Lit(p,r,n));} void adr(int r,String l){int p=pos();u16(0xA000|(r<<8));adrs.add(new Adr(p,r,l));} void literal(String n,long v){literals.put(n,v);} byte[] finish(){align4();Map<String,Integer>lp=new HashMap<>();for(var e:literals.entrySet()){lp.put(e.getKey(),pos());long v=e.getValue();out.write((int)v&255);out.write((int)(v>>>8)&255);out.write((int)(v>>>16)&255);out.write((int)(v>>>24)&255);}byte[]b=out.toByteArray();for(var f:branches){int d=req(f.l)-(f.p+4),h=d/2;if((d&1)!=0||h< -128||h>127)throw new IllegalStateException("branch range "+f.l+" p="+f.p+" target="+req(f.l)+" d="+d);int ins=(b[f.p]&255)|((b[f.p+1]&255)<<8);put16(b,f.p,(ins&0xFF00)|(h&255));}for(var f:longBranches){int d=req(f.l)-(f.p+4),h=d/2;if((d&1)!=0||h< -1024||h>1023)throw new IllegalStateException("long branch range "+f.l+" p="+f.p+" target="+req(f.l)+" d="+d);put16(b,f.p,0xE000|(h&0x7FF));}for(var f:bls){int d=req(f.l)-(f.p+4);if((d&1)!=0||d<-(1<<22)||d>=(1<<22))throw new IllegalStateException("BL range "+f.l);put16(b,f.p,0xF000|((d>>12)&0x7FF));put16(b,f.p+2,0xF800|((d>>1)&0x7FF));}for(var f:loads){int q=lp.get(f.n),pc=(f.p+4)&~3,d=q-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("literal range "+f.n+" d="+d);put16(b,f.p,0x4800|(f.r<<8)|(d/4));}for(var f:adrs){int q=req(f.l),pc=(f.p+4)&~3,d=q-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("adr range "+f.l+" d="+d);put16(b,f.p,0xA000|(f.r<<8)|(d/4));}return b;}int req(String n){Integer x=labels.get(n);if(x==null)throw new IllegalStateException("missing "+n);return x;}static void put16(byte[]b,int p,int v){b[p]=(byte)v;b[p+1]=(byte)(v>>>8);} record Branch(int p,String l){} record LongBranch(int p,String l){} record Bl(int p,String l){} record Lit(int p,int r,String n){} record Adr(int p,int r,String l){}
    }
}

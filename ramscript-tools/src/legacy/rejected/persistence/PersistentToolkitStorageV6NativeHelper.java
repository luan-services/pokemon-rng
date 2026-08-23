import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* Native helpers for Build 10.

   The launcher is the first generic persistent dispatcher in the project:
   - desired module id comes from VAR_8005
   - catalog entries are scanned by id
   - entry.location chooses SaveBlock1 or SaveBlock2 at runtime
   - checksum is verified before the THUMB tail-jump
*/
final class PersistentToolkitStorageV6NativeHelper {
    private PersistentToolkitStorageV6NativeHelper() {}

    static NativeHelper buildInstallerAt(RomProfile rom, int desiredSeed, long address) {
        byte[] catalog = PersistentToolkitStorageV6.buildCatalogImage(rom, desiredSeed);
        byte[] sid = PersistentToolkitStorageV6.buildSaveBlock1Payload(rom);
        if (catalog.length > 0xFF || sid.length > 0xFF) {
            throw new IllegalStateException("Build 10 compact installer length overflow");
        }

        final int literalOffset = 0x38;
        final int catalogOffset = 0x50;
        final int sidOffset = align4(catalogOffset + catalog.length);
        byte[] code = new byte[sidOffset + sid.length];

        // Copy catalog -> SaveBlock2 toolkit area.
        put16(code,0x00,ldrLiteral(0,0x00,literalOffset + 0x00));
        put16(code,0x02,0x6800);
        put16(code,0x04,ldrLiteral(1,0x04,literalOffset + 0x04));
        put16(code,0x06,0x1840);
        put16(code,0x08,ldrLiteral(1,0x08,literalOffset + 0x08));
        put16(code,0x0A,0x2200 | catalog.length);
        put16(code,0x0C,0x780B); put16(code,0x0E,0x7003);
        put16(code,0x10,0x3101); put16(code,0x12,0x3001); put16(code,0x14,0x3A01);
        put16(code,0x16,branchCond(0x1,0x16,0x0C)); // bne

        // Copy real SID module -> SaveBlock1 toolkit area + 0x20.
        put16(code,0x18,ldrLiteral(0,0x18,literalOffset + 0x0C));
        put16(code,0x1A,0x6800);
        put16(code,0x1C,ldrLiteral(1,0x1C,literalOffset + 0x10));
        put16(code,0x1E,0x1840);
        put16(code,0x20,ldrLiteral(1,0x20,literalOffset + 0x14));
        put16(code,0x22,0x2220); // 32-byte SID payload
        put16(code,0x24,0x780B); put16(code,0x26,0x7003);
        put16(code,0x28,0x3101); put16(code,0x2A,0x3001); put16(code,0x2C,0x3A01);
        put16(code,0x2E,branchCond(0x1,0x2E,0x24));
        put16(code,0x30,0x4770);
        put16(code,0x32,0x46C0); put16(code,0x34,0x46C0); put16(code,0x36,0x46C0);

        PersistentToolkitStorageV2.putU32(code,literalOffset + 0x00,rom.saveBlock2Ptr);
        PersistentToolkitStorageV2.putU32(code,literalOffset + 0x04,PayloadStorageArea.SAVE_BLOCK2.offset());
        PersistentToolkitStorageV2.putU32(code,literalOffset + 0x08,address + catalogOffset);
        PersistentToolkitStorageV2.putU32(code,literalOffset + 0x0C,rom.saveBlock1Ptr);
        PersistentToolkitStorageV2.putU32(code,literalOffset + 0x10,PayloadStorageArea.SAVE_BLOCK1.offset() + PersistentToolkitStorageV6.SID_SB1_OFFSET);
        PersistentToolkitStorageV2.putU32(code,literalOffset + 0x14,address + sidOffset);
        System.arraycopy(catalog,0,code,catalogOffset,catalog.length);
        System.arraycopy(sid,0,code,sidOffset,sid.length);
        return new NativeHelper(address,code);
    }

    static NativeHelper buildDispatcherAt(RomProfile rom, long address) {
        Thumb b = new Thumb();
        b.emit(0xB430); // push {r4,r5}; preserve LR for tail-jump return
        b.ldrLit(5,"sb2ptr");
        b.emit(0x682D); // ldr r5,[r5]
        b.ldrLit(0,"sb2off");
        b.emit(0x182D); // adds r5,r5,r0 -> catalog base
        b.emit(0x6828); // ldr r0,[r5]
        b.ldrLit(1,"magic");
        b.emit(0x4288); // cmp r0,r1
        b.bCond(1,"fail");
        b.emit(0x7928); // ldrb r0,[r5,#4]
        b.emit(0x2800 | PersistentToolkitStorageV6.VERSION);
        b.bCond(1,"fail");
        b.emit(0x796B); // ldrb r3,[r5,#5] moduleCount
        b.emit(0x2B00); // cmp r3,#0
        b.bCond(0,"fail");
        b.ldrLit(0,"desired");
        b.emit(0x8801); // ldrh r1,[r0]
        b.emit(0x1C2C); // adds r4,r5,#0
        b.emit(0x3410); // adds r4,#0x10
        b.label("scan");
        b.emit(0x8820); // ldrh r0,[r4]
        b.emit(0x4288); // cmp r0,r1
        b.bCond(0,"found");
        b.emit(0x3410);
        b.emit(0x3B01);
        b.bCond(1,"scan");
        b.b("fail");

        b.label("found");
        b.emit(0x78A0); // kind
        b.emit(0x2801);
        b.bCond(1,"fail");
        b.emit(0x78E0); // location
        b.emit(0x2801);
        b.bCond(0,"area1");
        b.emit(0x2802);
        b.bCond(0,"area2");
        b.b("fail");

        b.label("area1");
        b.ldrLit(2,"sb1ptr");
        b.emit(0x6812); // ldr r2,[r2]
        b.ldrLit(0,"sb1off");
        b.emit(0x1812); // adds r2,r2,r0
        b.b("gotbase");
        b.label("area2");
        b.emit(0x1C2A); // adds r2,r5,#0
        b.label("gotbase");
        b.emit(0x88A0); // ldrh r0,[r4,#4] payload offset
        b.emit(0x1812); // adds r2,r2,r0
        b.emit(0x1C15); // adds r5,r2,#0 keep payload start
        b.emit(0x88E1); // ldrh r1,[r4,#6] size
        b.emit(0x8923); // ldrh r3,[r4,#8] checksum
        b.emit(0x2900);
        b.bCond(0,"fail");
        b.emit(0x2000); // sum=0
        b.label("sum");
        b.emit(0x7814); // ldrb r4,[r2]
        b.emit(0x1900); // adds r0,r0,r4
        b.emit(0x3201);
        b.emit(0x3901);
        b.bCond(1,"sum");
        b.emit(0x0400); // lsls r0,#16
        b.emit(0x0C00); // lsrs r0,#16
        b.emit(0x4298); // cmp r0,r3
        b.bCond(1,"fail");
        b.emit(0x1C2B); // adds r3,r5,#0
        b.emit(0xBC30); // pop {r4,r5}
        b.emit(0x3301); // Thumb bit
        b.emit(0x4718); // bx r3

        b.label("fail");
        b.emit(0xBC30);
        b.emit(0x2000);
        b.ldrLit(1,"result");
        b.emit(0x8008); // strh r0,[r1]
        b.emit(0x4770);

        b.literal("sb2ptr",rom.saveBlock2Ptr);
        b.literal("sb2off",PayloadStorageArea.SAVE_BLOCK2.offset());
        b.literal("magic",PersistentToolkitStorageV6.MAGIC);
        b.literal("desired",rom.specialVar8005);
        b.literal("sb1ptr",rom.saveBlock1Ptr);
        b.literal("sb1off",PayloadStorageArea.SAVE_BLOCK1.offset());
        b.literal("result",rom.specialVarResult);
        return new NativeHelper(address,b.finish());
    }


    /* Staged persistent-native dispatcher.

       This keeps the module payload persistent in SB2, but executes it from a
       known EWRAM scratch address. The Party IV viewer was previously validated
       in-game at gStringVar4 + 0x140; staging avoids making direct-SaveBlock
       execution a requirement for larger helpers with PC-relative literal/ADR
       sequences.
    */
    static NativeHelper buildStagingDispatcherAt(RomProfile rom, long address, long stagingAddress, int stagingCapacity) {
        if ((stagingAddress & 3L) != 0) throw new IllegalArgumentException("native staging address must be word-aligned");
        if (stagingCapacity <= 0 || stagingCapacity > 0xFFFF) throw new IllegalArgumentException("invalid native staging capacity");
        Thumb b = new Thumb();
        b.emit(0xB470); // push {r4,r5,r6}; preserve caller LR for module return
        b.ldrLit(5,"sb2ptr");
        b.emit(0x682D); // ldr r5,[r5]
        b.ldrLit(0,"sb2off");
        b.emit(0x182D); // catalog base
        b.emit(0x6828);
        b.ldrLit(1,"magic");
        b.emit(0x4288);
        b.bCond(1,"fail");
        b.emit(0x7928);
        b.emit(0x2800 | PersistentToolkitStorageV6.VERSION);
        b.bCond(1,"fail");
        b.emit(0x796B);
        b.emit(0x2B00);
        b.bCond(0,"fail");
        b.ldrLit(0,"desired");
        b.emit(0x8801);
        b.emit(0x1C2C);
        b.emit(0x3410);
        b.label("scan");
        b.emit(0x8820);
        b.emit(0x4288);
        b.bCond(0,"found");
        b.emit(0x3410);
        b.emit(0x3B01);
        b.bCond(1,"scan");
        b.b("fail");

        b.label("found");
        b.emit(0x78A0);
        b.emit(0x2801);
        b.bCond(1,"fail");
        b.emit(0x78E0);
        b.emit(0x2802); // staged production catalog requires SB2 module
        b.bCond(1,"fail");
        b.emit(0x1C2A); // r2 = catalog base
        b.emit(0x88A0); // payload offset
        b.emit(0x1812); // payload address
        b.emit(0x1C15); // r5 = payload start
        b.emit(0x88E1); // r1 = payload size
        b.emit(0x8923); // r3 = expected checksum while r4 is still the entry pointer
        b.emit(0x2900);
        b.bCond(0,"fail");
        b.ldrLit(0,"capacity");
        b.emit(0x4281); // cmp r1,r0
        b.bCond(8,"fail"); // bhi: payload > staging capacity
        b.emit(0x1C0E); // r6 = payload size
        b.emit(0x2000); // checksum sum
        b.label("sum");
        b.emit(0x7814); // r4 is scratch from here onward
        b.emit(0x1900);
        b.emit(0x3201);
        b.emit(0x3901);
        b.bCond(1,"sum");
        b.emit(0x0400);
        b.emit(0x0C00);
        b.emit(0x4298); // compare computed sum against saved expected checksum in r3
        b.bCond(1,"fail");

        // Copy the validated module bytes into stable EWRAM scratch.
        b.emit(0x1C28); // r0 = r5 source
        b.ldrLit(2,"stage");
        b.emit(0x1C31); // r1 = r6 size
        b.label("copy");
        b.emit(0x7803); // ldrb r3,[r0]
        b.emit(0x7013); // strb r3,[r2]
        b.emit(0x3001);
        b.emit(0x3201);
        b.emit(0x3901);
        b.bCond(1,"copy");
        b.ldrLit(3,"stageThumb");
        b.emit(0xBC70); // restore r4-r6, retain caller LR
        b.emit(0x4718); // module returns directly to ScrCmd_callnative via LR

        b.label("fail");
        b.emit(0xBC70);
        b.emit(0x2000);
        b.ldrLit(1,"result");
        b.emit(0x8008);
        b.emit(0x4770);

        b.literal("sb2ptr",rom.saveBlock2Ptr);
        b.literal("sb2off",PayloadStorageArea.SAVE_BLOCK2.offset());
        b.literal("magic",PersistentToolkitStorageV6.MAGIC);
        b.literal("desired",rom.specialVar8005);
        b.literal("capacity",stagingCapacity);
        b.literal("stage",stagingAddress);
        b.literal("stageThumb",stagingAddress | 1L);
        b.literal("result",rom.specialVarResult);
        return new NativeHelper(address,b.finish());
    }


    /* Production staged loader: validate a persistent SB2 module, copy it to
       stable EWRAM scratch, report success, and RETURN. The Field Script owns
       the subsequent stock callnative into the staged module. */
    static NativeHelper buildStagingLoaderAt(RomProfile rom, long address, long stagingAddress, int stagingCapacity) {
        if ((stagingAddress & 3L) != 0) throw new IllegalArgumentException("native staging address must be word-aligned");
        if (stagingCapacity <= 0 || stagingCapacity > 0xFFFF) throw new IllegalArgumentException("invalid native staging capacity");
        Thumb b = new Thumb();
        b.emit(0xB470); // push {r4,r5,r6}
        b.ldrLit(5,"sb2ptr"); b.emit(0x682D);
        b.ldrLit(0,"sb2off"); b.emit(0x182D);
        b.emit(0x6828); b.ldrLit(1,"magic"); b.emit(0x4288); b.bCond(1,"fail");
        b.emit(0x7928); b.emit(0x2800 | PersistentToolkitStorageV6.VERSION); b.bCond(1,"fail");
        b.emit(0x796B); b.emit(0x2B00); b.bCond(0,"fail");
        b.ldrLit(0,"desired"); b.emit(0x8801);
        b.emit(0x1C2C); b.emit(0x3410);
        b.label("scan");
        b.emit(0x8820); b.emit(0x4288); b.bCond(0,"found");
        b.emit(0x3410); b.emit(0x3B01); b.bCond(1,"scan"); b.b("fail");
        b.label("found");
        b.emit(0x78A0); b.emit(0x2801); b.bCond(1,"fail");
        b.emit(0x78E0); b.emit(0x2802); b.bCond(1,"fail");
        b.emit(0x1C2A); b.emit(0x88A0); b.emit(0x1812); // r2 = payload source
        b.emit(0x1C15); // r5 = source start
        b.emit(0x88E1); // r1 = size
        b.emit(0x8923); // r3 = expected checksum
        b.emit(0x2900); b.bCond(0,"fail");
        b.ldrLit(0,"capacity"); b.emit(0x4281); b.bCond(8,"fail");
        b.emit(0x1C0E); // r6 = size
        b.emit(0x2000);
        b.label("sum");
        b.emit(0x7814); b.emit(0x1900); b.emit(0x3201); b.emit(0x3901); b.bCond(1,"sum");
        b.emit(0x0400); b.emit(0x0C00); b.emit(0x4298); b.bCond(1,"fail");
        b.emit(0x1C28); b.ldrLit(2,"stage"); b.emit(0x1C31);
        b.label("copy");
        b.emit(0x7803); b.emit(0x7013); b.emit(0x3001); b.emit(0x3201); b.emit(0x3901); b.bCond(1,"copy");
        b.emit(0x2001); b.ldrLit(1,"result"); b.emit(0x8008);
        b.emit(0xBC70); b.emit(0x4770);
        b.label("fail");
        b.emit(0x2000); b.ldrLit(1,"result"); b.emit(0x8008);
        b.emit(0xBC70); b.emit(0x4770);
        b.literal("sb2ptr",rom.saveBlock2Ptr);
        b.literal("sb2off",PayloadStorageArea.SAVE_BLOCK2.offset());
        b.literal("magic",PersistentToolkitStorageV6.MAGIC);
        b.literal("desired",rom.specialVar8005);
        b.literal("capacity",stagingCapacity);
        b.literal("stage",stagingAddress);
        b.literal("result",rom.specialVarResult);
        return new NativeHelper(address,b.finish());
    }

    /* Diagnostic staged-copy checker.
       Resolves a persistent SB2 module, verifies the persistent checksum,
       copies it into EWRAM staging, then independently verifies the staged
       bytes. It never executes the module. VAR_RESULT=1 means source and
       staged copies both match the catalog checksum. */
    static NativeHelper buildStagingCopyCheckAt(RomProfile rom, long address, long stagingAddress, int stagingCapacity) {
        if ((stagingAddress & 3L) != 0) throw new IllegalArgumentException("native staging address must be word-aligned");
        if (stagingCapacity <= 0 || stagingCapacity > 0xFFFF) throw new IllegalArgumentException("invalid native staging capacity");
        Thumb b = new Thumb();
        b.emit(0xB470); // push {r4,r5,r6}
        b.ldrLit(5,"sb2ptr"); b.emit(0x682D);
        b.ldrLit(0,"sb2off"); b.emit(0x182D); // r5 = catalog base
        b.emit(0x6828); b.ldrLit(1,"magic"); b.emit(0x4288); b.bCond(1,"fail");
        b.emit(0x7928); b.emit(0x2800 | PersistentToolkitStorageV6.VERSION); b.bCond(1,"fail");
        b.emit(0x796B); b.emit(0x2B00); b.bCond(0,"fail");
        b.ldrLit(0,"desired"); b.emit(0x8801);
        b.emit(0x1C2C); b.emit(0x3410); // r4 = first entry
        b.label("scan");
        b.emit(0x8820); b.emit(0x4288); b.bCond(0,"found");
        b.emit(0x3410); b.emit(0x3B01); b.bCond(1,"scan"); b.b("fail");
        b.label("found");
        b.emit(0x78A0); b.emit(0x2801); b.bCond(1,"fail");
        b.emit(0x78E0); b.emit(0x2802); b.bCond(1,"fail");
        b.emit(0x1C2A);          // r2 = catalog base
        b.emit(0x88A0); b.emit(0x1812); // r2 = payload source
        b.emit(0x1C15);          // r5 = payload source
        b.emit(0x88E1);          // r1 = size
        b.emit(0x8926);          // r6 = expected checksum
        b.emit(0x2900); b.bCond(0,"fail");
        b.ldrLit(0,"capacity"); b.emit(0x4281); b.bCond(8,"fail");
        b.emit(0x1C0C);          // r4 = size (entry pointer no longer needed)

        // Validate persistent source bytes.
        b.emit(0x2000);          // sum = 0
        b.label("sumSrc");
        b.emit(0x7813);          // ldrb r3,[r2]
        b.emit(0x18C0);          // adds r0,r0,r3
        b.emit(0x3201); b.emit(0x3901); b.bCond(1,"sumSrc");
        b.emit(0x0400); b.emit(0x0C00); b.emit(0x42B0); b.bCond(1,"fail"); // cmp r0,r6

        // Copy persistent bytes into the same staging slot used by standalone Party IV.
        b.emit(0x1C28);          // r0 = source
        b.ldrLit(2,"stage");     // r2 = destination
        b.emit(0x1C21);          // r1 = size
        b.label("copy");
        b.emit(0x7803); b.emit(0x7013);
        b.emit(0x3001); b.emit(0x3201); b.emit(0x3901); b.bCond(1,"copy");

        // Independently checksum the staged copy.
        b.ldrLit(2,"stage");
        b.emit(0x1C21);          // r1 = size
        b.emit(0x2000);          // sum = 0
        b.label("sumStage");
        b.emit(0x7813); b.emit(0x18C0);
        b.emit(0x3201); b.emit(0x3901); b.bCond(1,"sumStage");
        b.emit(0x0400); b.emit(0x0C00); b.emit(0x42B0); b.bCond(1,"fail");

        b.emit(0x2001);          // success
        b.ldrLit(1,"result"); b.emit(0x8008);
        b.emit(0xBC70); b.emit(0x4770);
        b.label("fail");
        b.emit(0x2000); b.ldrLit(1,"result"); b.emit(0x8008);
        b.emit(0xBC70); b.emit(0x4770);

        b.literal("sb2ptr",rom.saveBlock2Ptr);
        b.literal("sb2off",PayloadStorageArea.SAVE_BLOCK2.offset());
        b.literal("magic",PersistentToolkitStorageV6.MAGIC);
        b.literal("desired",rom.specialVar8005);
        b.literal("capacity",stagingCapacity);
        b.literal("stage",stagingAddress);
        b.literal("result",rom.specialVarResult);
        return new NativeHelper(address,b.finish());
    }


    /* Build 12 direct-IWRAM dispatcher entry.

       Unlike buildDispatcherAt(), the desired module id arrives in r1 from the
       resident hotkey runtime. This removes the need for a Field Script bridge
       or a temporary executable resolver. The caller uses BL -> bx r4, so LR
       points back to the resident stage2 cleanup.
    */
    static NativeHelper buildDispatcherFromR1At(RomProfile rom, long address) {
        Thumb b = new Thumb();
        b.emit(0xB432); // push {r1,r4,r5}; preserve desired id on stack
        b.ldrLit(5,"sb2ptr");
        b.emit(0x682D); // ldr r5,[r5]
        b.ldrLit(0,"sb2off");
        b.emit(0x182D); // adds r5,r5,r0 -> catalog base
        b.emit(0x6828); // ldr r0,[r5]
        b.ldrLit(1,"magic");
        b.emit(0x4288); // cmp r0,r1
        b.bCond(1,"fail");
        b.emit(0x7928); // ldrb r0,[r5,#4]
        b.emit(0x2800 | PersistentToolkitStorageV6.VERSION);
        b.bCond(1,"fail");
        b.emit(0x796B); // ldrb r3,[r5,#5] moduleCount
        b.emit(0x2B00);
        b.bCond(0,"fail");
        b.emit(0x9900); // ldr r1,[sp,#0] desired module id saved by push
        b.emit(0x1C2C);
        b.emit(0x3410);
        b.label("scan");
        b.emit(0x8820);
        b.emit(0x4288);
        b.bCond(0,"found");
        b.emit(0x3410);
        b.emit(0x3B01);
        b.bCond(1,"scan");
        b.b("fail");

        b.label("found");
        b.emit(0x78A0);
        b.emit(0x2801);
        b.bCond(1,"fail");
        b.emit(0x78E0);
        b.emit(0x2801);
        b.bCond(0,"area1");
        b.emit(0x2802);
        b.bCond(0,"area2");
        b.b("fail");

        b.label("area1");
        b.ldrLit(2,"sb1ptr");
        b.emit(0x6812);
        b.ldrLit(0,"sb1off");
        b.emit(0x1812);
        b.b("gotbase");
        b.label("area2");
        b.emit(0x1C2A);
        b.label("gotbase");
        b.emit(0x88A0);
        b.emit(0x1812);
        b.emit(0x1C15);
        b.emit(0x88E1);
        b.emit(0x8923);
        b.emit(0x2900);
        b.bCond(0,"fail");
        b.emit(0x2000);
        b.label("sum");
        b.emit(0x7814);
        b.emit(0x1900);
        b.emit(0x3201);
        b.emit(0x3901);
        b.bCond(1,"sum");
        b.emit(0x0400);
        b.emit(0x0C00);
        b.emit(0x4298);
        b.bCond(1,"fail");
        b.emit(0x1C2B);
        b.emit(0xBC32); // pop {r1,r4,r5}
        b.emit(0x3301);
        b.emit(0x4718); // tail-jump module; module bx lr returns to resident stage2

        b.label("fail");
        b.emit(0xBC32);
        b.emit(0x2000);
        b.ldrLit(1,"result");
        b.emit(0x8008);
        b.emit(0x4770);

        b.literal("sb2ptr",rom.saveBlock2Ptr);
        b.literal("sb2off",PayloadStorageArea.SAVE_BLOCK2.offset());
        b.literal("magic",PersistentToolkitStorageV6.MAGIC);
        b.literal("sb1ptr",rom.saveBlock1Ptr);
        b.literal("sb1off",PayloadStorageArea.SAVE_BLOCK1.offset());
        b.literal("result",rom.specialVarResult);
        return new NativeHelper(address,b.finish());
    }

    private static int align4(int n){ return (n+3)&~3; }
    private static void put16(byte[] b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);}
    private static int ldrLiteral(int rt,int insn,int literal){int base=(insn+4)&~3;int d=literal-base;if(d<0||(d&3)!=0||d/4>255)throw new IllegalArgumentException("literal out of range");return 0x4800|(rt<<8)|(d/4);}
    private static int branchCond(int cond,int insn,int target){int d=target-(insn+4);if((d&1)!=0||d/2< -128||d/2>127)throw new IllegalArgumentException("branch out of range");return 0xD000|(cond<<8)|((d/2)&0xFF);}

    private static final class Thumb {
        private final List<Integer> words=new ArrayList<>();
        private final Map<String,Integer> labels=new HashMap<>();
        private final List<BranchFixup> branches=new ArrayList<>();
        private final List<LiteralFixup> loads=new ArrayList<>();
        private final List<Literal> literals=new ArrayList<>();
        int pos(){return words.size()*2;}
        void emit(int w){words.add(w&0xFFFF);}
        void label(String n){if(labels.put(n,pos())!=null)throw new IllegalArgumentException("duplicate label "+n);}
        void bCond(int cond,String label){branches.add(new BranchFixup(words.size(),cond,label,true));emit(0);}
        void b(String label){branches.add(new BranchFixup(words.size(),0,label,false));emit(0);}
        void ldrLit(int rt,String name){loads.add(new LiteralFixup(words.size(),rt,name));emit(0);}
        void literal(String name,long value){literals.add(new Literal(name,value));}
        byte[] finish(){
            if((pos()&3)!=0)emit(0x46C0);
            Map<String,Integer> literalOffsets=new HashMap<>();
            int literalStart=pos();
            for(int i=0;i<literals.size();i++)literalOffsets.put(literals.get(i).name,literalStart+i*4);
            for(BranchFixup f:branches){
                Integer target=labels.get(f.label);if(target==null)throw new IllegalStateException("missing label "+f.label);
                int insn=f.wordIndex*2;int delta=target-(insn+4);
                if((delta&1)!=0)throw new IllegalStateException("unaligned branch");
                int hw=delta/2;
                if(f.conditional){if(hw< -128||hw>127)throw new IllegalStateException("conditional branch range");words.set(f.wordIndex,0xD000|(f.cond<<8)|(hw&0xFF));}
                else {if(hw< -1024||hw>1023)throw new IllegalStateException("branch range");words.set(f.wordIndex,0xE000|(hw&0x7FF));}
            }
            for(LiteralFixup f:loads){
                Integer target=literalOffsets.get(f.name);if(target==null)throw new IllegalStateException("missing literal "+f.name);
                int insn=f.wordIndex*2;int base=(insn+4)&~3;int delta=target-base;
                if(delta<0||(delta&3)!=0||delta/4>255)throw new IllegalStateException("literal load range");
                words.set(f.wordIndex,0x4800|(f.rt<<8)|(delta/4));
            }
            byte[] out=new byte[pos()+literals.size()*4];
            for(int i=0;i<words.size();i++)put16(out,i*2,words.get(i));
            int o=pos();for(Literal l:literals){PersistentToolkitStorageV2.putU32(out,o,l.value);o+=4;}
            return out;
        }
        private record BranchFixup(int wordIndex,int cond,String label,boolean conditional){}
        private record LiteralFixup(int wordIndex,int rt,String name){}
        private record Literal(String name,long value){}
    }
}

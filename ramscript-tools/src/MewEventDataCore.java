/* Persistent Module B: final-Mew helpers + exact MYSTRY reward.
   Stored in the validated ~400-byte SB1 tail immediately before RamScript. */
final class MewEventDataCore {
    static final int STORAGE_OFFSET=0x348C;
    static final int CAPACITY=0x190; // ends at 0x361C, the RamScript struct
    static final int HEADER_SIZE=0x20;
    static final long MAGIC=0x4257454DL; // "MEWB"
    static final int VERSION=2;
    record Image(byte[] bytes,int rewardOffset,int ensureMewOffset,int transformRebindOffset,int rewardNativeSize,int ensureSize,int transformSize){}
    private MewEventDataCore(){}
    static Image build(RomProfile rom){
        byte[] reward=MewEventDataRewardNative.buildAt(rom,0).codeCopy();
        byte[] ensure=PersistentEventMewRecoveryProbePreset.buildEnsureMewHelper(rom,0).codeCopy();
        byte[] transform=PersistentEventMewRecoveryProbePreset.buildTransformAndRebindHelper(rom,0).codeCopy();
        int rewardOff=HEADER_SIZE;
        int ensureOff=align4(rewardOff+reward.length);
        int transformOff=align4(ensureOff+ensure.length);
        int tailOff=align4(transformOff+transform.length);
        byte[] tail=buildRewardTail(rom,tailOff,rewardOff);
        int total=tailOff+tail.length;
        if(total>CAPACITY) throw new IllegalStateException("Mew Event/Data Core exceeds SB1 tail: "+total+"/"+CAPACITY);
        byte[] out=new byte[total];
        put32(out,0,MAGIC); out[4]=VERSION; out[5]=1;
        put16(out,8,total); put16(out,0x0A,rewardOff); put16(out,0x0C,ensureOff); put16(out,0x0E,transformOff);
        put16(out,0x10,reward.length); put16(out,0x12,ensure.length); put16(out,0x14,transform.length);
        put16(out,0x16,checksum16(reward)); put16(out,0x18,checksum16(ensure)); put16(out,0x1A,checksum16(transform));
        System.arraycopy(reward,0,out,rewardOff,reward.length); System.arraycopy(ensure,0,out,ensureOff,ensure.length); System.arraycopy(transform,0,out,transformOff,transform.length); System.arraycopy(tail,0,out,tailOff,tail.length);
        // The installed dispatcher still calls the original reward entry (+0x20).
        // Jump to the tail while retaining the PC writer, its MYSTRY data, and all
        // three Core B entry offsets exactly where the installed Core A expects them.
        put16(out,rewardOff,branch(tailOff,rewardOff));
        put16(out,0x16,checksum16(java.util.Arrays.copyOfRange(out,rewardOff,rewardOff+reward.length)));
        return new Image(out,rewardOff,ensureOff,transformOff,reward.length,ensure.length,transform.length);
    }
    private static byte[] buildRewardTail(RomProfile rom,int start,int rewardOff){
        if(rom!=RomProfile.FIRE_RED_EN_10 || start!=332 || rewardOff!=32)
            throw new IllegalStateException("Reward tail needs the verified FR1.0 Core B layout");
        java.io.ByteArrayOutputStream o=new java.io.ByteArrayOutputStream();
        java.util.Map<String,Integer> labels=new java.util.HashMap<>();
        java.util.List<int[]> branches=new java.util.ArrayList<>();
        // push {lr}; result was saved in VAR_8004 before VAR_RESULT became MODE_REWARD.
        emit(o,0xB500); int resultLoad=emit(o,0x4800);emit(o,0x8800);emit(o,0x2800);
        branches.add(new int[]{emit(o,0xD000),0,0}); // beq party
        emit(o,0x2801);branches.add(new int[]{emit(o,0xD100),1,0}); // bne done
        branches.add(new int[]{emit(o,0xE000),2,1}); // b old PC writer (+2)
        labels.put("party",o.size());
        // A successful stock GiveMonToPlayer has already filled one of six slots.
        int countLoad=emit(o,0x4800);emit(o,0x7800);
        emit(o,0x3801);emit(o,0x2164);emit(o,0x4348); // (count-1)*100
        int partyLoad=emit(o,0x4900);emit(o,0x1809);
        int pcRead=emit(o,0x467A);int subHi=emit(o,0x3A00);int subLo=emit(o,0x3A00);
        emit(o,0x0010); // r0 = address of the sole 80-byte MYSTRY blob
        int converterLoad=emit(o,0x4B00);
        int bl=emit(o,0);emit(o,0);
        labels.put("done",o.size());emit(o,0xBD00);
        labels.put("thunk",o.size());emit(o,0x4718);
        while(((start+o.size())&3)!=0)emit(o,0x46C0);
        int literalStart=o.size();
        for(long v:new long[]{rom.specialVar8004,0x02024029L,rom.playerParty,0x0803E775L}){
            for(int i=0;i<4;i++)o.write((int)(v>>>(8*i))&255);
        }
        byte[] a=o.toByteArray();
        for(int[] b:branches){int target=switch(b[1]){case 0->start+labels.get("party");case 1->start+labels.get("done");default->rewardOff+2;};int half=(target-(start+b[0]+4))/2;if(b[2]==0){if(half< -128||half>127)throw new IllegalStateException("conditional reward branch");put16(a,b[0],((a[b[0]+1]&255)<<8)|(half&255));}else put16(a,b[0],branch(target,start+b[0]));}
        int[] loads={resultLoad,countLoad,partyLoad,converterLoad};
        for(int i=0;i<4;i++){int pc=(start+loads[i]+4)&~3;int d=start+literalStart+i*4-pc;if(d<0||d>1020||(d&3)!=0)throw new IllegalStateException("reward literal");put16(a,loads[i],((a[loads[i]+1]&255)<<8)|(d/4));}
        // MOV from PC reads the current Thumb instruction address + 4; unlike
        // LDR (literal), it does not round that address down to a word boundary.
        int pc=start+pcRead+4;int blob=rewardOff+24;int distance=pc-blob;
        if(distance<256||distance>510)throw new IllegalStateException("MYSTRY blob reach");
        put16(a,subHi,0x3AFF);put16(a,subLo,0x3A00|(distance-255));
        int displacement=(start+labels.get("thunk"))-(start+bl+4);
        put16(a,bl,0xF000|((displacement>>12)&0x7FF));put16(a,bl+2,0xF800|((displacement>>1)&0x7FF));
        return a;
    }
    private static int emit(java.io.ByteArrayOutputStream out,int half){int p=out.size();out.write(half&255);out.write((half>>>8)&255);return p;}
    private static int branch(int target,int from){int n=(target-from-4)/2;if(n< -1024||n>1023)throw new IllegalStateException("reward jump");return 0xE000|(n&0x7FF);}
    static int align4(int n){return (n+3)&~3;} static int checksum16(byte[]d){int s=0;for(byte b:d)s=(s+Byte.toUnsignedInt(b))&0xffff;return s;}
    static void put16(byte[]d,int o,int v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);} static void put32(byte[]d,int o,long v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);d[o+2]=(byte)(v>>>16);d[o+3]=(byte)(v>>>24);}
}

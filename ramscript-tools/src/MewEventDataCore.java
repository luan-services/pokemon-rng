/* Persistent Module B: final-Mew helpers + exact MYSTRY reward.
   Stored in the validated ~400-byte SB1 tail immediately before RamScript. */
final class MewEventDataCore {
    static final int STORAGE_OFFSET=0x348C;
    static final int CAPACITY=0x190; // ends at 0x361C, the RamScript struct
    static final int HEADER_SIZE=0x20;
    static final long MAGIC=0x4257454DL; // "MEWB"
    static final int VERSION=1;
    record Image(byte[] bytes,int rewardOffset,int ensureMewOffset,int transformRebindOffset,int rewardNativeSize,int ensureSize,int transformSize){}
    private MewEventDataCore(){}
    static Image build(RomProfile rom){
        byte[] reward=MewEventDataRewardNative.buildAt(rom,0).codeCopy();
        byte[] ensure=PersistentEventMewRecoveryProbePreset.buildEnsureMewHelper(rom,0).codeCopy();
        byte[] transform=PersistentEventMewRecoveryProbePreset.buildTransformAndRebindHelper(rom,0).codeCopy();
        int rewardOff=HEADER_SIZE;
        int ensureOff=align4(rewardOff+reward.length);
        int transformOff=align4(ensureOff+ensure.length);
        int total=align4(transformOff+transform.length);
        if(total>CAPACITY) throw new IllegalStateException("Mew Event/Data Core exceeds SB1 tail: "+total+"/"+CAPACITY);
        byte[] out=new byte[total];
        put32(out,0,MAGIC); out[4]=VERSION; out[5]=1;
        put16(out,8,total); put16(out,0x0A,rewardOff); put16(out,0x0C,ensureOff); put16(out,0x0E,transformOff);
        put16(out,0x10,reward.length); put16(out,0x12,ensure.length); put16(out,0x14,transform.length);
        put16(out,0x16,checksum16(reward)); put16(out,0x18,checksum16(ensure)); put16(out,0x1A,checksum16(transform));
        System.arraycopy(reward,0,out,rewardOff,reward.length); System.arraycopy(ensure,0,out,ensureOff,ensure.length); System.arraycopy(transform,0,out,transformOff,transform.length);
        return new Image(out,rewardOff,ensureOff,transformOff,reward.length,ensure.length,transform.length);
    }
    static int align4(int n){return (n+3)&~3;} static int checksum16(byte[]d){int s=0;for(byte b:d)s=(s+Byte.toUnsignedInt(b))&0xffff;return s;}
    static void put16(byte[]d,int o,int v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);} static void put32(byte[]d,int o,long v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);d[o+2]=(byte)(v>>>16);d[o+3]=(byte)(v>>>24);}
}

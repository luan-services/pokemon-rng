final class PartySelectorHarnessBuild44Test {
    public static void main(String[] args) {
        byte[] p = PartySelectorHarnessPreset.buildPayload(RomProfile.FIRE_RED_EN_10);
        boolean special=false, wait=false, nativeCall=false;
        for (int i=0;i<p.length;i++) {
            int op=p[i]&255;
            if (op==0x23) nativeCall=true;
            if (op==0x27) wait=true;
            if (i+2<p.length && op==0x25 && (p[i+1]&255)==0x29 && (p[i+2]&255)==0)
                special=true;
        }
        if (!special) throw new AssertionError("ChooseHalfPartyForBattle special missing");
        if (!wait) throw new AssertionError("waitstate missing");
        if (nativeCall) throw new AssertionError("harness must contain no callnative");
        System.out.println("payload="+p.length);
        System.out.println("stock special 0x0029 + waitstate: OK");
        System.out.println("no callnative: OK");
    }
}

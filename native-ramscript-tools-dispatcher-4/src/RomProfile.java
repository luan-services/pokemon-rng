enum RomProfile {
    FIRE_RED_EN_10(
            "fr10",
            "FireRed English 1.0",
            0x03003550L,
            0x03003118L,
            0x03005000L,
            0x08000725L,
            0x03005310L,
            0x03003F94L,
            0x03003EB4L,
            0x03003EC0L,
            0x02021D18L
    );

    private final String id;
    private final String displayName;

    final long vblankSlot;
    final long heldKeysRaw;
    final long rngValue;
    final long originalVBlankThumb;

    final long mainHook;
    final long rngExtension;
    final long tailStub;
    final long originalVBlankLiteral;
    final long installerStaging;

    RomProfile(
            String id,
            String displayName,
            long vblankSlot,
            long heldKeysRaw,
            long rngValue,
            long originalVBlankThumb,
            long mainHook,
            long rngExtension,
            long tailStub,
            long originalVBlankLiteral,
            long installerStaging
    ) {
        this.id = id;
        this.displayName = displayName;
        this.vblankSlot = vblankSlot;
        this.heldKeysRaw = heldKeysRaw;
        this.rngValue = rngValue;
        this.originalVBlankThumb = originalVBlankThumb;
        this.mainHook = mainHook;
        this.rngExtension = rngExtension;
        this.tailStub = tailStub;
        this.originalVBlankLiteral = originalVBlankLiteral;
        this.installerStaging = installerStaging;
    }

    String id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    static RomProfile fromId(String id) {
        for (RomProfile profile : values()) {
            if (profile.id.equalsIgnoreCase(id)) {
                return profile;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported ROM profile: " + id + ". Currently supported: fr10"
        );
    }
}

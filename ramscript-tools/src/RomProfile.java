enum RomProfile {
    FIRE_RED_EN_10(
            "fr10",
            "FireRed English 1.0",
            ValidationStatus.RUNTIME_VALIDATED,
            0x03003550L,
            0x03003118L,
            0x03005000L,
            0x08000725L,
            0x03005310L,
            0x03003F94L,
            0x03003EB4L,
            0x03003EC0L,
            0x02021D18L,
            0x08056535L,
            0x08069E49L,
            0x08069AE5L,
            0x03000F9CL,
            0x0300500CL,
            0x020370D0L
    ),

    LEAF_GREEN_EN_10(
            "lg10",
            "LeafGreen English 1.0",
            ValidationStatus.RUNTIME_VALIDATED,
            0x03003550L,
            0x03003118L,
            0x03005000L,
            0x08000725L,
            0x03005310L,
            0x03003F94L,
            0x03003EB4L,
            0x03003EC0L,
            0x02021D18L,
            0x08056535L,
            0x08069E49L,
            0x08069AE5L,
            0x03000F9CL,
            0x0300500CL,
            0x020370D0L
    ),

    FIRE_RED_EN_11(
            "fr11",
            "FireRed English 1.1",
            ValidationStatus.SYMBOL_VERIFIED_UNTESTED,
            0x03003550L,
            0x03003118L,
            0x03005000L,
            0x08000739L,
            0x03005310L,
            0x03003F94L,
            0x03003EB4L,
            0x03003EC0L,
            0x02021D18L,
            0x08056549L,
            0x08069E5DL,
            0x08069AF9L,
            0x03000F9CL,
            0x0300500CL,
            0x020370D0L
    ),

    LEAF_GREEN_EN_11(
            "lg11",
            "LeafGreen English 1.1",
            ValidationStatus.SYMBOL_VERIFIED_UNTESTED,
            0x03003550L,
            0x03003118L,
            0x03005000L,
            0x08000739L,
            0x03005310L,
            0x03003F94L,
            0x03003EB4L,
            0x03003EC0L,
            0x02021D18L,
            0x08056549L,
            0x08069E5DL,
            0x08069AF9L,
            0x03000F9CL,
            0x0300500CL,
            0x020370D0L
    );

    enum ValidationStatus {
        RUNTIME_VALIDATED("runtime-validated"),
        SYMBOL_VERIFIED("symbol-verified; runtime test pending"),
        SYMBOL_VERIFIED_UNTESTED("symbol-verified; ROM runtime untested");

        private final String label;

        ValidationStatus(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    private final String id;
    private final String displayName;
    private final ValidationStatus validationStatus;

    final long vblankSlot;
    final long heldKeysRaw;
    final long rngValue;
    final long originalVBlankThumb;

    // Legacy fields used by historical candidates.
    final long mainHook;
    final long rngExtension;
    final long tailStub;
    final long originalVBlankLiteral;
    final long installerStaging;

    // Runtime-v1 profile fields.
    final long cb1OverworldThumb;
    final long getSavedRamScriptThumb;
    final long scriptContextSetupThumb;
    final long lockFieldControls;

    // Generic native-helper profile fields.
    final long saveBlock2Ptr;
    final long specialVarResult;

    RomProfile(
            String id,
            String displayName,
            ValidationStatus validationStatus,
            long vblankSlot,
            long heldKeysRaw,
            long rngValue,
            long originalVBlankThumb,
            long mainHook,
            long rngExtension,
            long tailStub,
            long originalVBlankLiteral,
            long installerStaging,
            long cb1OverworldThumb,
            long getSavedRamScriptThumb,
            long scriptContextSetupThumb,
            long lockFieldControls,
            long saveBlock2Ptr,
            long specialVarResult
    ) {
        this.id = id;
        this.displayName = displayName;
        this.validationStatus = validationStatus;
        this.vblankSlot = vblankSlot;
        this.heldKeysRaw = heldKeysRaw;
        this.rngValue = rngValue;
        this.originalVBlankThumb = originalVBlankThumb;
        this.mainHook = mainHook;
        this.rngExtension = rngExtension;
        this.tailStub = tailStub;
        this.originalVBlankLiteral = originalVBlankLiteral;
        this.installerStaging = installerStaging;
        this.cb1OverworldThumb = cb1OverworldThumb;
        this.getSavedRamScriptThumb = getSavedRamScriptThumb;
        this.scriptContextSetupThumb = scriptContextSetupThumb;
        this.lockFieldControls = lockFieldControls;
        this.saveBlock2Ptr = saveBlock2Ptr;
        this.specialVarResult = specialVarResult;
    }

    String id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    ValidationStatus validationStatus() {
        return validationStatus;
    }

    static RomProfile fromId(String id) {
        for (RomProfile profile : values()) {
            if (profile.id.equalsIgnoreCase(id)) {
                return profile;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported ROM profile: " + id
                        + ". Supported: fr10, lg10, fr11, lg11"
        );
    }
}

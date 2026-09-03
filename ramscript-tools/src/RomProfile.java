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
            0x03005008L,
            0x020370D0L,
            0x02024284L,
            0x02021D18L,
            0x020370C0L,
            0x020370C2L,
            0x0803FBE9L,
            0x02036DFCL,
            0x081283A9L,
            0x03005024L,
            0x080567DDL,
            0x08042EC5L,
            0x080CDDA9L,
            0x0300537CL,
            0x080568C5L,
            0x08183560L,
            0x081835A0L,
            0x03005080L,
            0x03007300L,
            0x081DE0BCL
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
            0x03005008L,
            0x020370D0L,
            0x02024284L,
            0x02021D18L,
            0x020370C0L,
            0x020370C2L,
            0x0803FBE9L,
            0x02036DFCL,
            0x08128381L,
            0x03005024L,
            0x080567DDL,
            0x08042EC5L,
            0x080CDD7DL,
            0x0300537CL,
            0x080568C5L,
            0x0818353CL,
            0x0818357CL,
            0x03005080L,
            0x03007300L,
            0x081DE0BCL
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
            0x03005008L,
            0x020370D0L,
            0x02024284L,
            0x02021D18L,
            0x020370C0L,
            0x020370C2L,
            0x0803FBFDL,
            0x02036DFCL,
            0x08128421L,
            0x03005024L,
            0x080567F1L,
            0x08042ED9L,
            0x080CDDBDL,
            0x0300537CL,
            0x080568D9L,
            0x081835D8L,
            0x08183618L,
            0x03005080L,
            0x03007300L,
            0x081DE0D0L
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
            0x03005008L,
            0x020370D0L,
            0x02024284L,
            0x02021D18L,
            0x020370C0L,
            0x020370C2L,
            0x0803FBFDL,
            0x02036DFCL,
            0x081283F9L,
            0x03005024L,
            0x080567F1L,
            0x08042ED9L,
            0x080CDD91L,
            0x0300537CL,
            0x080568D9L,
            0x081835B4L,
            0x081835F4L,
            0x03005080L,
            0x03007300L,
            0x081DE0D0L
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
    final long saveBlock1Ptr;
    final long specialVarResult;

    // Party / Pokemon data helpers used by advanced presets.
    final long playerParty;
    final long stringVar4;
    final long specialVar8004;
    final long specialVar8005;
    final long getMonData3Thumb;
    final long mapHeader;

    // Trade-evolution / Party continuation helpers.
    final long choosePartyMonByMenuTypeThumb;
    final long fieldCallback2;
    final long cb2ReturnToFieldThumb;
    final long getEvolutionTargetSpeciesThumb;
    final long beginEvolutionSceneThumb;
    final long cb2AfterEvolution;
    final long cb2ReturnToFieldContinueScriptThumb;

    // Vanilla ROM text pointers used only by the trainer-battle boundary probe.
    final long trainerProbeIntroText;
    final long trainerProbeDefeatText;

    // Mute Music stock BGM control symbols.
    final long gDisableMusic;
    final long gMPlayInfoBgm;
    final long m4aMPlayVolumeControlThumb;

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
            long saveBlock1Ptr,
            long specialVarResult,
            long playerParty,
            long stringVar4,
            long specialVar8004,
            long specialVar8005,
            long getMonData3Thumb,
            long mapHeader,
            long choosePartyMonByMenuTypeThumb,
            long fieldCallback2,
            long cb2ReturnToFieldThumb,
            long getEvolutionTargetSpeciesThumb,
            long beginEvolutionSceneThumb,
            long cb2AfterEvolution,
            long cb2ReturnToFieldContinueScriptThumb,
            long trainerProbeIntroText,
            long trainerProbeDefeatText,
            long gDisableMusic,
            long gMPlayInfoBgm,
            long m4aMPlayVolumeControlThumb
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
        this.saveBlock1Ptr = saveBlock1Ptr;
        this.specialVarResult = specialVarResult;
        this.playerParty = playerParty;
        this.stringVar4 = stringVar4;
        this.specialVar8004 = specialVar8004;
        this.specialVar8005 = specialVar8005;
        this.getMonData3Thumb = getMonData3Thumb;
        this.mapHeader = mapHeader;
        this.choosePartyMonByMenuTypeThumb = choosePartyMonByMenuTypeThumb;
        this.fieldCallback2 = fieldCallback2;
        this.cb2ReturnToFieldThumb = cb2ReturnToFieldThumb;
        this.getEvolutionTargetSpeciesThumb = getEvolutionTargetSpeciesThumb;
        this.beginEvolutionSceneThumb = beginEvolutionSceneThumb;
        this.cb2AfterEvolution = cb2AfterEvolution;
        this.cb2ReturnToFieldContinueScriptThumb = cb2ReturnToFieldContinueScriptThumb;
        this.trainerProbeIntroText = trainerProbeIntroText;
        this.trainerProbeDefeatText = trainerProbeDefeatText;
        this.gDisableMusic = gDisableMusic;
        this.gMPlayInfoBgm = gMPlayInfoBgm;
        this.m4aMPlayVolumeControlThumb = m4aMPlayVolumeControlThumb;
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

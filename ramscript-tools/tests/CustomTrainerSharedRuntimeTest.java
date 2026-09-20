final class CustomTrainerSharedRuntimeTest {
    private CustomTrainerSharedRuntimeTest() {}

    static void run() {
        for (RomProfile rom : RomProfile.values()) {
            NativeHelper runtime = CustomTrainerBattleRuntimeV2.build(rom);
            check(runtime.size() <= CustomTrainerRuntimeStorage.RUNTIME_CAPACITY,
                    rom.id() + ": shared trainer runtime exceeds resident capacity");
            check(runtime.stagingAddress() == rom.stringVar4 + 0x140L,
                    rom.id() + ": shared trainer staging address drifted");

            byte[] image = CustomTrainerRuntimeStorage.runtimeImage(runtime.codeCopy());
            check(Binary.u32(image, 0) == Integer.toUnsignedLong(CustomTrainerRuntimeStorage.MAGIC),
                    rom.id() + ": runtime image magic mismatch");
            check(Binary.u16(image, 8) == runtime.size(),
                    rom.id() + ": runtime image size mismatch");

            TriggerBuildResult installer = CustomTrainerSharedRuntimeInstallerPreset.buildLavenderWorker(rom);
            TriggerBuildResult brock = CustomTrainerBattleSharedRuntimePreset.buildLavenderWorker(rom, CustomTrainerBattleExamples.brock());
            TriggerBuildResult misty = CustomTrainerBattleSharedRuntimePreset.buildLavenderWorker(rom, CustomTrainerBattleExamples.misty());
            check(installer.payloadBytes() <= RamScript.SCRIPT_SIZE, rom.id() + ": runtime installer does not fit RamScript");
            check(brock.payloadBytes() <= RamScript.SCRIPT_SIZE, rom.id() + ": Brock descriptor card does not fit RamScript");
            check(misty.payloadBytes() <= RamScript.SCRIPT_SIZE, rom.id() + ": Misty descriptor card does not fit RamScript");
            TriggerBuildResult prodInstaller = CustomTrainerSharedRuntimeInstallerPreset.buildDeliveryman(rom);
            TriggerBuildResult prodBrock = CustomTrainerBattleSharedRuntimePreset.buildGymLeader(rom, ObjectEventCatalog.PEWTER_GYM_BROCK, CustomTrainerBattleExamples.brock(), CustomTrainerVanillaScripts.brock(rom));
            TriggerBuildResult prodMisty = CustomTrainerBattleSharedRuntimePreset.buildGymLeader(rom, ObjectEventCatalog.CERULEAN_GYM_MISTY, CustomTrainerBattleExamples.misty(), CustomTrainerVanillaScripts.misty(rom));
            check(prodInstaller.ramScript().hasWonderCardHeader(), rom.id() + ": production runtime installer must stay Deliveryman/no-object");
            check(prodInstaller.payloadBytes() <= RamScript.SCRIPT_SIZE, rom.id() + ": production runtime installer does not fit RamScript");
            check(prodBrock.payloadBytes() <= RamScript.SCRIPT_SIZE, rom.id() + ": production Brock card does not fit RamScript");
            check(prodMisty.payloadBytes() <= RamScript.SCRIPT_SIZE, rom.id() + ": production Misty card does not fit RamScript");
            check(prodBrock.ramScript().mapGroup() == ObjectEventCatalog.PEWTER_GYM_BROCK.mapGroup() && prodBrock.ramScript().objectId() == ObjectEventCatalog.PEWTER_GYM_BROCK.localId(), rom.id() + ": Brock binding drifted");
            check(prodMisty.ramScript().mapGroup() == ObjectEventCatalog.CERULEAN_GYM_MISTY.mapGroup() && prodMisty.ramScript().objectId() == ObjectEventCatalog.CERULEAN_GYM_MISTY.localId(), rom.id() + ": Misty binding drifted");

            byte[] b = CustomTrainerBattleDescriptor.encode(CustomTrainerBattleExamples.brock(), 2, 0x1234);
            byte[] m = CustomTrainerBattleDescriptor.encode(CustomTrainerBattleExamples.misty(), 2, 0x1234);
            byte[] bt = CustomTrainerFieldTextStorage.encode(CustomTrainerBattleExamples.brock());
            byte[] mt = CustomTrainerFieldTextStorage.encode(CustomTrainerBattleExamples.misty());
            check(b.length <= CustomTrainerRuntimeStorage.DESCRIPTOR_CAPACITY, "Brock descriptor exceeds capacity");
            check(m.length <= CustomTrainerRuntimeStorage.DESCRIPTOR_CAPACITY, "Misty descriptor exceeds capacity");
            check(bt.length <= CustomTrainerFieldTextStorage.CAPACITY, "Brock text pool exceeds SB1 capacity");
            check(mt.length <= CustomTrainerFieldTextStorage.CAPACITY, "Misty text pool exceeds SB1 capacity");
            check(Binary.u16(b, CustomTrainerBattleDescriptor.OFF_TRAINER_ID) == TrainerIdentityHost.BROCK.trainerId(), "Brock host id mismatch");
            check(Binary.u16(m, CustomTrainerBattleDescriptor.OFF_TRAINER_ID) == TrainerIdentityHost.MISTY.trainerId(), "Misty host id mismatch");
            check(Binary.u16(b, CustomTrainerBattleDescriptor.OFF_COMPLETION_FLAG) == CustomTrainerCompletionFlag.BROCK.eventFlag(), "Brock flag mismatch");
            check(Binary.u16(m, CustomTrainerBattleDescriptor.OFF_COMPLETION_FLAG) == CustomTrainerCompletionFlag.MISTY.eventFlag(), "Misty flag mismatch");
            check(Byte.toUnsignedInt(b[CustomTrainerBattleDescriptor.OFF_PARTY_COUNT]) == 3, "Brock placeholder party should stay three mons");
            check(Byte.toUnsignedInt(m[CustomTrainerBattleDescriptor.OFF_PARTY_COUNT]) == 3, "Misty placeholder party should stay three mons");

            byte[] bc = CustomTrainerCompactTransport.encode(CustomTrainerBattleExamples.brock(), 2, 0x1234);
            byte[] mc = CustomTrainerCompactTransport.encode(CustomTrainerBattleExamples.misty(), 2, 0x1234);
            check(java.util.Arrays.equals(b, CustomTrainerCompactTransport.expandForTest(bc)),
                    rom.id() + ": Brock compact transport must expand byte-for-byte to CTD1");
            check(java.util.Arrays.equals(m, CustomTrainerCompactTransport.expandForTest(mc)),
                    rom.id() + ": Misty compact transport must expand byte-for-byte to CTD1");
            check(bc.length < b.length, rom.id() + ": Brock compact transport must be smaller than raw descriptor");
            check(mc.length < m.length, rom.id() + ": Misty compact transport must be smaller than raw descriptor");

            check(Binary.u32(bt, 0) == Integer.toUnsignedLong(CustomTrainerFieldTextStorage.MAGIC), "Brock text pool magic mismatch");
            check(Binary.u16(mt, 4) == CustomTrainerFieldTextStorage.VERSION, "Misty text pool version mismatch");
        }

        check(InstallationManifest.hasMagic(InstallationManifest.buildCustomTrainer()), "custom trainer manifest must be cleaner-compatible");

        check(CustomTrainerBattleDescriptor.MAX_SIZE <= CustomTrainerRuntimeStorage.DESCRIPTOR_CAPACITY,
                "six-mon descriptor must fit resident descriptor area");
        check(CustomTrainerRuntimeStorage.DESCRIPTOR_OFFSET + CustomTrainerRuntimeStorage.DESCRIPTOR_CAPACITY
                        == InstallationManifest.OFFSET,
                "descriptor area must stop exactly before toolkit manifest");

        CustomTrainerBattleSpec base = CustomTrainerBattleExamples.brock();
        java.util.List<EReaderTrainerData.Mon> sixParty = java.util.Collections.nCopies(6, base.party().get(0));
        CustomTrainerBattleSpec six = new CustomTrainerBattleSpec(
                "six-mon-layout-test", base.identity(), base.battleMusic(), base.completionFlag(),
                base.preBattleText(), base.defeatText(), base.postVictoryText(), base.alreadyCompletedText(), sixParty);
        byte[] sixDescriptor = CustomTrainerBattleDescriptor.encode(six, 1, 0x1234);
        byte[] sixCompact = CustomTrainerCompactTransport.encode(six, 1, 0x1234);
        check(sixDescriptor.length == CustomTrainerBattleDescriptor.MAX_SIZE, "six-mon descriptor size drifted");
        check(sixDescriptor.length <= CustomTrainerRuntimeStorage.DESCRIPTOR_CAPACITY, "six-mon descriptor must fit SB2 descriptor area");
        check(java.util.Arrays.equals(sixDescriptor, CustomTrainerCompactTransport.expandForTest(sixCompact)),
                "six-mon compact transport must expand byte-for-byte to CTD1");
        check(CustomTrainerFieldTextStorage.encode(six).length == CustomTrainerFieldTextStorage.encode(base).length,
                "party size must not consume SB1 dialogue capacity");

        CustomTrainerBattleSpec boundarySix = CustomTrainerBattleExamples.brockSixMonBoundary();
        for (RomProfile rom : RomProfile.values()) {
            TriggerBuildResult sixCard = CustomTrainerBattleSharedRuntimePreset.buildGymLeader(
                    rom, ObjectEventCatalog.PEWTER_GYM_BROCK, boundarySix, CustomTrainerVanillaScripts.brock(rom));
            check(sixCard.payloadBytes() <= RamScript.SCRIPT_SIZE,
                    rom.id() + ": distinct six-mon compact production card does not fit RamScript");
            check(sixCard.payloadBytes() == 978,
                    rom.id() + ": distinct six-mon boundary card size drifted: " + sixCard.payloadBytes());
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

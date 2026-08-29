/* ROM trainer identities reused by custom trainer battles.
   The custom party is external; these ids provide vanilla name/class/pic/AI/money metadata. */
enum TrainerIdentityHost {
    BROCK(414),
    MISTY(415),
    LT_SURGE(416),
    ERIKA(417),
    KOGA(418),
    BLAINE(419),
    SABRINA(420),
    GIOVANNI(348);

    private final int trainerId;
    TrainerIdentityHost(int trainerId) { this.trainerId = trainerId; }
    int trainerId() { return trainerId; }
}

/* ROM trainer identities reused by custom trainer battles.
   The custom party is external; these ids provide vanilla name/class/pic/AI/money metadata. */
enum TrainerIdentityHost {
    BROCK(414),
    MISTY(415);

    private final int trainerId;
    TrainerIdentityHost(int trainerId) { this.trainerId = trainerId; }
    int trainerId() { return trainerId; }
}

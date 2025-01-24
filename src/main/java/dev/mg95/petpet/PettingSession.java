package dev.mg95.petpet;

public class PettingSession {
    public final Long startTime;
    public final Long lastPet;
    public PettingSession(Long startTime, Long lastPet) {
        this.startTime = startTime;
        this.lastPet = lastPet;
    }
}

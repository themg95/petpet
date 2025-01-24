package dev.mg95.petpet;

import java.util.UUID;

public class Key {

    public final UUID petter;
    public final UUID receiver;

    public Key(UUID petter, UUID receiver) {
        this.petter = petter;
        this.receiver = receiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Key)) return false;
        Key key = (Key) o;
        return petter == key.petter && receiver == key.receiver;
    }

    @Override
    public int hashCode() {
        return (petter.toString() + receiver.toString()).hashCode();
    }

}


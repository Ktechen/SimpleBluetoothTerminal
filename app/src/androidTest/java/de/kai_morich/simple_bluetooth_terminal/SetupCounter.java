package de.kai_morich.simple_bluetooth_terminal;

public class SetupCounter {
    public SetupCounter(int millis) {
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + millis;
        while (System.currentTimeMillis() < endTime);
    }

    public SetupCounter() {
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + 5000;
        while (System.currentTimeMillis() < endTime);
    }
}

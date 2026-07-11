package com.tournaments;

/**
 * A single event that happened during a match, used for the minute-by-minute
 * "watch match" commentary feature (Football Manager style).
 */
public class MatchEvent {

    public final int minute;
    public final String type;   // KICKOFF, GOAL, MISS, SAVE, POST, YELLOW, RED, HALFTIME, FULLTIME
    public final String side;   // "Home", "Away" or null for neutral events
    public final String text;   // human-readable commentary line

    public MatchEvent(int minute, String type, String side, String text) {
        this.minute = minute;
        this.type = type;
        this.side = side;
        this.text = text;
    }
}

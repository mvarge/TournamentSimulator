package com.tournaments;

import java.util.ArrayList;
import java.util.List;

public class Round {

    List<Match> matches = new ArrayList<Match>();
    private Integer roundNumber;

    public Round() {
    }

    /** FIX: this was `public void Round(...)` — a method, not a constructor, so roundNumber was never set. */
    public Round(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void addMatch(Match m) {
        this.matches.add(m);
    }
}

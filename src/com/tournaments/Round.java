package com.tournaments;

import java.util.ArrayList;
import java.util.List;

public class Round {

    List<Match> matches = new ArrayList<Match>();
    Integer roundNumber;

    public void Round(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }

    public void addMatch(Match m) {
        this.matches.add(m);
    }
}

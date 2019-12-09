package com.tournaments;

import java.util.ArrayList;
import java.util.List;

public class Turn {

    public List<Round> rounds = new ArrayList<Round>();

    public void addRound(Round r) {
        this.rounds.add(r);
    }

}

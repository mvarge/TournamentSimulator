package com.tournaments;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Match {

    String winner = null;
    HashMap<String, Team> teams = new HashMap<String, Team>();

    List<Integer> numberOfPlays = Arrays.asList(
            0, 0, 0, 0, 0, 9,
            1, 1, 1, 1, 1, 1, 1, 1, 1,
            2, 2, 2, 2, 2, 2, 2, 2, 2,
            3, 3, 3, 3, 3, 3,
            4, 4, 4, 4, 4,
            5, 5, 5,
            6, 6,
            7,
            8,
            9);

    public Match(Team home, Team away) {
        teams.put("Home", home);
        teams.put("Away", away);

    }

    public void play() {
        play(true);
    }

    public void play(boolean homeFactor) {

        Random rand = new Random();
        int plays = numberOfPlays.get(rand.nextInt(numberOfPlays.size()));

        int homeRating = this.teams.get("Home").rating;
        int awayRating = this.teams.get("Away").rating;

        this.teams.get("Home").prepareToPlay(this.teams.get("Away").rating);
        this.teams.get("Away").prepareToPlay(this.teams.get("Home").rating);

        if (homeFactor)
            homeRating += Math.random() * 2;

        if (this.teams.get("Home").morale >= this.teams.get("Away").morale) {
            homeRating += (int) (Math.random() * 2);
        }

        if ((int) (Math.random() * 10) == 1) {
            homeRating = homeRating + awayRating;
            awayRating = homeRating - awayRating;
            homeRating = homeRating - awayRating;
        }

        for (int i = 0; i < plays; i++) {

            int homeDice = (int) (Math.random() * homeRating);
            int awayDice = (int) (Math.random() * awayRating);
            if (homeDice > awayDice) {
                this.teams.get("Home").goalInFavor();
                this.teams.get("Away").goalAgainst();
            }
            else if (awayDice > homeDice) {
                this.teams.get("Away").goalInFavor();
                this.teams.get("Home").goalAgainst();
            }
            else if (homeDice == awayDice) {
            }

        }

        System.out.printf("Final score: %s %d x %d %s\n", this.teams.get("Home").name,
                this.teams.get("Home").goalsMade, this.teams.get("Away").goalsMade, this.teams.get("Away").name);
        if (this.teams.get("Home").goalsMade > this.teams.get("Away").goalsMade) {
            this.teams.get("Home").finishGame("Win");
            this.teams.get("Away").finishGame("Lose");
        } else if (this.teams.get("Away").goalsMade > this.teams.get("Home").goalsMade) {
            this.teams.get("Away").finishGame("Win");
            this.teams.get("Home").finishGame("Lose");
        } else if (this.teams.get("Home").goalsMade == this.teams.get("Away").goalsMade) {
            this.teams.get("Home").finishGame("Draw");
            this.teams.get("Away").finishGame("Draw");
        }
    }
}

package com.tournaments;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Match {
    /**
     * Match class is responsible for leveraging all the match aspects and handling the goals opportunities
     * as well the goals results for each of those.
     */
    String winner = null;  // null means draw
    HashMap<String, Team> teams = new HashMap<String, Team>();

    // Weighted list: heavily skewed to 1-3 chances, rare high-scoring games
    List<Integer> numberOfPlays = Arrays.asList(
            0, 0, 0, 0, 0, 0,
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

    public void playMatch() {
        playMatch(true);
    }

    /**
     * @param homeFactor    Defines if home factors are applied such as giving more advantage to whoever is the home team
     *
     * FIX: winner is now correctly set after the result is known.
     * FIX: rating-swap (upset mechanic) now happens BEFORE prepareToPlay() so opponentRating is stored correctly.
     * FIX: home advantage is now more meaningful (+0 to +3 bonus, reflecting ~60% historical home win rate).
     */
    public void playMatch(boolean homeFactor) {

        Random rand = new Random();
        int plays = numberOfPlays.get(rand.nextInt(numberOfPlays.size()));

        int homeRating = this.teams.get("Home").getRating();
        int awayRating = this.teams.get("Away").getRating();

        // FIX: Upset/surprise mechanic — swap ratings BEFORE prepareToPlay so teams know
        // their real opponent rating when streaks/unexpected-result logic runs.
        boolean swapped = false;
        if ((int) (Math.random() * 10) == 1) {
            int tmp = homeRating;
            homeRating = awayRating;
            awayRating = tmp;
            swapped = true;
        }

        // Now store opponent ratings correctly (after any swap)
        this.teams.get("Home").prepareToPlay(awayRating);
        this.teams.get("Away").prepareToPlay(homeRating);

        // FIX: Home advantage — adds 0–3 points (avg ~1.5) which on a 0–20 scale is ~7.5% boost
        if (homeFactor) {
            homeRating += (int) (Math.random() * 4);  // 0, 1, 2, or 3
        }

        // Morale boost: higher morale side gets a small edge
        if (this.teams.get("Home").getMorale() > this.teams.get("Away").getMorale()) {
            homeRating += (int) (Math.random() * 2);
        } else if (this.teams.get("Away").getMorale() > this.teams.get("Home").getMorale()) {
            awayRating += (int) (Math.random() * 2);
        }

        for (int i = 0; i < plays; i++) {
            int homeDice = (int) (Math.random() * homeRating);
            int awayDice = (int) (Math.random() * awayRating);
            if (homeDice > awayDice) {
                this.teams.get("Home").goalInFavor();
                this.teams.get("Away").goalAgainst();
            } else if (awayDice > homeDice) {
                this.teams.get("Away").goalInFavor();
                this.teams.get("Home").goalAgainst();
            }
            // exact tie on dice = no goal (already handled by the else fallthrough)
        }

        int homeGoals = this.teams.get("Home").getGoalsMade();
        int awayGoals = this.teams.get("Away").getGoalsMade();

        System.err.printf("Final score: %s %d x %d %s%s\n",
                this.teams.get("Home").name, homeGoals,
                awayGoals, this.teams.get("Away").name,
                swapped ? " (upset!)" : "");

        if (homeGoals > awayGoals) {
            this.winner = this.teams.get("Home").name;  // FIX: winner is now set
            this.teams.get("Home").finishGame("Win");
            this.teams.get("Away").finishGame("Lose");
        } else if (awayGoals > homeGoals) {
            this.winner = this.teams.get("Away").name;  // FIX: winner is now set
            this.teams.get("Away").finishGame("Win");
            this.teams.get("Home").finishGame("Lose");
        } else {
            this.winner = null;  // draw
            this.teams.get("Home").finishGame("Draw");
            this.teams.get("Away").finishGame("Draw");
        }
    }
}

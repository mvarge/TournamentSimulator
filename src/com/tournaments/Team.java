package com.tournaments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Probably the most complex Class of the project, it handles all of the team aspect and keeps track of its record
 * throughout a given tournament, since every team instance consumes the same rating aspects and can change
 * accordingly to its performance in different places
 */
public class Team implements Comparable<Team> {

    public String name;
    private int rating;
    private int initialRating;
    private int morale;       // FIX: morale is now a live variable, updated after each result
    private int points;
    private int totalGoalsMade;
    private int totalGoalsTaken;
    private int goalsMade;
    private int goalsTaken;
    private int wins;
    private int draws;
    private int losses;
    private int opponentRating;
    private int winStreak;
    private int lossStreak;
    private int noWinStreak;
    List<Character> form = new ArrayList<Character>();

    public Team(String name, int rating) {
        this.name = name;
        this.rating = rating;
        this.initialRating = rating;
        this.morale = rating;  // starts equal to rating
    }

    /**
     * Just setting initial variables for numbers of goals, this could belong to Match class but I thought it was
     * easier to maintain control around here
     *
     * @param opponentRating    For evaluation of rating difference, which is used for some situations
     */
    public void prepareToPlay(Integer opponentRating) {
        this.goalsMade = 0;
        this.goalsTaken = 0;
        this.opponentRating = opponentRating;
    }

    public Integer getPoints() {
        return this.points;
    }

    public String getName() {
        return name;
    }

    public Integer getWins() { return this.wins; }

    public Integer getDraws() { return this.draws; }

    public Integer getLosses() { return this.losses; }

    public Integer getTotalMatches() {
        return this.wins + this.draws + this.losses;
    }

    public int getMorale() {
        return morale;
    }

    public int getRating() {
        return rating;
    }

    public int getInitialRating() {
        return initialRating;
    }

    public Integer getGoalsDifference() { return this.totalGoalsMade - this.totalGoalsTaken; }

    public void goalInFavor() {
        this.goalsMade += 1;
    }

    public void goalAgainst() {
        this.goalsTaken += 1;
    }

    /**
     * Based on final result, several actions may take place such as rating change, morale change,
     * record store is also maintained here as well win/no-win streak control.
     *
     * FIX: morale is now updated here after every result (was never mutated before).
     *
     * @param result    One of "Win", "Draw" or "Lose"
     */
    public void finishGame(String result) {
        boolean humiliation = false;

        int goalDifference = this.goalsMade - this.goalsTaken;
        if (Math.abs(goalDifference) > 3)
            humiliation = true;

        switch (result) {
            case "Win":
                this.points += 3;
                this.totalGoalsMade += this.goalsMade;
                this.totalGoalsTaken += this.goalsTaken;
                this.wins += 1;
                this.form.add('W');
                this.winStreak += 1;
                this.lossStreak = 0;
                this.noWinStreak = 0;

                // FIX: morale rises on a win
                if (this.morale < 20) this.morale = Math.min(20, this.morale + 1);
                if (humiliation && this.morale < 20) this.morale = Math.min(20, this.morale + 1);

                if (this.rating > 20) {
                    this.rating = 20;
                }
                if (this.rating < 20) {
                    if (winStreak >= 3) {
                        this.rating += (int) (Math.random() * 2);
                        this.winStreak = 0;
                    }
                    if (humiliation)
                        this.rating += (int) (Math.random() * 2);
                    if (this.opponentRating >= (this.rating * 2)) {
                        this.rating += (int) (Math.random() * 2);
                        System.err.println("Unexpected win! " + this.name);
                    }
                    if (this.rating < 4) {
                        this.rating += (int) (Math.random() * 2);
                        if (this.opponentRating >= (this.rating * 2)) {
                            this.rating += (int) (Math.random() * 2);
                        }
                    }
                }
                // FIX: Sayajin now correctly checks against live morale (not static)
                if (this.morale < 10 && this.rating < 10) {
                    if ((int) (Math.random() * 10) == 1) {
                        this.rating += (int) (Math.random() * 5);
                        this.morale += (int) (Math.random() * 3);
                        System.err.println("Sayajin!!! " + this.name);
                    }
                }
                break;

            case "Draw":
                this.points += 1;
                this.totalGoalsMade += this.goalsMade;
                this.totalGoalsTaken += this.goalsTaken;
                this.draws += 1;
                this.form.add('D');
                this.noWinStreak += 1;
                this.winStreak = 0;
                // FIX: morale slightly drops on a no-win streak
                if (this.noWinStreak >= 3 && this.morale > 1) {
                    this.morale = Math.max(1, this.morale - 1);
                }
                break;

            case "Lose":
                this.totalGoalsMade += this.goalsMade;
                this.totalGoalsTaken += this.goalsTaken;
                this.losses += 1;
                this.form.add('L');
                this.lossStreak += 1;
                this.noWinStreak += 1;
                this.winStreak = 0;

                // FIX: morale drops on a loss
                if (this.morale > 1) this.morale = Math.max(1, this.morale - 1);
                if (humiliation && this.morale > 1) this.morale = Math.max(1, this.morale - 1);

                if (this.rating > 2) {
                    if (lossStreak >= 3) {
                        this.rating -= (int) (Math.random() * 2);
                        this.lossStreak = 0;
                    }
                    if (humiliation)
                        this.rating -= (int) (Math.random() * 2);
                    if (this.opponentRating <= (this.rating / 2)) {
                        this.rating -= (int) (Math.random() * 2);
                        System.err.println("Unexpected loss! " + this.name);
                    }
                }
                // FIX: Big Failure now correctly checks live morale
                if (this.morale > 10) {
                    if ((int) (Math.random() * 10) == 10) {
                        this.rating -= (int) (Math.random() * 5);
                        this.morale -= (int) (Math.random() * 3);
                        System.err.println("Big Failure!!! " + this.name);
                    }
                }
                break;

            default:
                break;
        }

        // Floor guard: rating can't collapse too far below initial morale
        if (this.rating <= (this.initialRating / 2)) {
            this.rating += (int) (Math.random() * 2);
        }
        // Clamp rating
        this.rating = Math.max(1, Math.min(20, this.rating));
        this.morale = Math.max(1, Math.min(20, this.morale));
    }

    public String printForm() {
        return String.valueOf(this.form);
    }

    public int getTotalGoalsMade() {
        return totalGoalsMade;
    }

    public int getTotalGoalsTaken() {
        return totalGoalsTaken;
    }

    public int getGoalsMade() {
        return goalsMade;
    }

    public int getGoalsTaken() {
        return goalsTaken;
    }

    /**
     * Override of compareTo so we can sort a list of team based on number of points, followed by win total and finally
     * goals difference.
     *
     * @param t Just a Team object
     * @return  returns call result of .compare
     */
    @Override
    public int compareTo(Team t) {
        return Comparator.comparing(Team::getPoints)
                .thenComparing(Team::getWins)
                .thenComparing(Team::getGoalsDifference)
                .compare(this, t);
    }

}

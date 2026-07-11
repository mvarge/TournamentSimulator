package com.tournaments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Probably the most complex Class of the project, it handles all of the team aspect and keeps track of its record
 * throughout a given tournament, since every team instance consumes the same rating aspects and can change
 * accordingly to its performance in different places
 *
 * Ratings are now split into ATTACK and DEFENCE (0-100 each). The legacy single
 * "rating" is exposed as the average of both, so all previous progression logic
 * (streaks, humiliation, unexpected results, Sayajin/Big Failure) still works —
 * deltas are applied to both halves via changeRating(), plus a few
 * attack/defence-specific tweaks based on goals scored/conceded.
 */
public class Team implements Comparable<Team> {

    public String name;
    private int attack;
    private int defence;
    private int initialAttack;
    private int initialDefence;
    private int initialRating;
    private int morale;       // live variable, updated after each result
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

    /** Legacy constructor: single rating means attack == defence. */
    public Team(String name, int rating) {
        this(name, rating, rating);
    }

    public Team(String name, int attack, int defence) {
        this.name = name;
        this.attack = clamp(attack);
        this.defence = clamp(defence);
        this.initialAttack = this.attack;
        this.initialDefence = this.defence;
        this.initialRating = (this.attack + this.defence) / 2;
        this.morale = this.initialRating;  // starts equal to overall rating
    }

    private static int clamp(int v) {
        return Math.max(5, Math.min(100, v));
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

    /** Overall rating = average of attack and defence (legacy-compatible). */
    public int getRating() {
        return (this.attack + this.defence) / 2;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefence() {
        return defence;
    }

    public int getInitialRating() {
        return initialRating;
    }

    public int getInitialAttack() {
        return initialAttack;
    }

    public int getInitialDefence() {
        return initialDefence;
    }

    /** Applies a rating delta to BOTH attack and defence, clamped to 5-100. */
    private void changeRating(int delta) {
        this.attack = clamp(this.attack + delta);
        this.defence = clamp(this.defence + delta);
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

                // morale rises on a win
                this.morale = Math.min(100, this.morale + 5);
                if (humiliation) this.morale = Math.min(100, this.morale + 5);

                if (this.getRating() < 100) {
                    if (winStreak >= 3) {
                        changeRating((int) (Math.random() * 10));
                        this.winStreak = 0;
                    }
                    if (humiliation)
                        changeRating((int) (Math.random() * 10));
                    if (this.opponentRating >= (this.getRating() * 2)) {
                        changeRating((int) (Math.random() * 10));
                        System.err.println("Unexpected win! " + this.name);
                    }
                    if (this.getRating() < 20) {
                        changeRating((int) (Math.random() * 10));
                        if (this.opponentRating >= (this.getRating() * 2)) {
                            changeRating((int) (Math.random() * 10));
                        }
                    }
                }

                // Attack/Defence-specific tweaks
                if (this.goalsMade >= 4) {
                    this.attack = clamp(this.attack + (int) (Math.random() * 5));  // firing on all cylinders
                }
                if (this.goalsTaken == 0) {
                    this.defence = clamp(this.defence + (int) (Math.random() * 5));  // clean sheet
                }

                // "Sayajin" event: struggling team explodes with power
                if (this.morale < 50 && this.getRating() < 50) {
                    if ((int) (Math.random() * 10) == 1) {
                        changeRating((int) (Math.random() * 25));
                        this.morale += (int) (Math.random() * 15);
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
                // morale slightly drops on a no-win streak
                if (this.noWinStreak >= 3) {
                    this.morale = Math.max(5, this.morale - 5);
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

                // morale drops on a loss
                this.morale = Math.max(5, this.morale - 5);
                if (humiliation) this.morale = Math.max(5, this.morale - 5);

                if (this.getRating() > 10) {
                    if (lossStreak >= 3) {
                        changeRating(-(int) (Math.random() * 10));
                        this.lossStreak = 0;
                    }
                    if (humiliation)
                        changeRating(-(int) (Math.random() * 10));
                    if (this.opponentRating <= (this.getRating() / 2)) {
                        changeRating(-(int) (Math.random() * 10));
                        System.err.println("Unexpected loss! " + this.name);
                    }
                }

                // Attack/Defence-specific tweaks
                if (this.goalsTaken >= 4) {
                    this.defence = clamp(this.defence - (int) (Math.random() * 5));  // leaky defence
                }

                // "Big Failure" event: FIX — the old condition ((int)(Math.random()*10) == 10)
                // could never be true, so this event never fired. Now a 10% chance like Sayajin.
                if (this.morale > 50) {
                    if ((int) (Math.random() * 10) == 1) {
                        changeRating(-(int) (Math.random() * 25));
                        this.morale -= (int) (Math.random() * 15);
                        System.err.println("Big Failure!!! " + this.name);
                    }
                }
                break;

            default:
                break;
        }

        // Floor guard: rating can't collapse too far below initial rating
        if (this.getRating() <= (this.initialRating / 2)) {
            changeRating((int) (Math.random() * 10));
        }
        // Clamp everything
        this.attack = clamp(this.attack);
        this.defence = clamp(this.defence);
        this.morale = Math.max(5, Math.min(100, this.morale));
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
     * Standard football tie-breakers:
     * points -> goal difference -> goals for -> wins.
     *
     * @param t Just a Team object
     * @return  returns call result of .compare
     */
    @Override
    public int compareTo(Team t) {
        return Comparator.comparing(Team::getPoints)
                .thenComparing(Team::getGoalsDifference)
                .thenComparing(Team::getTotalGoalsMade)
                .thenComparing(Team::getWins)
                .compare(this, t);
    }

}

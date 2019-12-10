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
    private int morale;
    List<Character> form = new ArrayList<Character>();

    public Team(String name, int rating) {
        this.name = name;
        this.rating = rating;
        this.morale = rating;
    }

    /**
     * Just setting initial variables for numbers of goals, this could belong to Match class but I though it was
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

    public Integer getGoalsDifference() { return this.totalGoalsMade - this.totalGoalsTaken; }

    public void calcStatistics(Match m) {
        if (m.winner == this.name) {
            this.points += 3;
        } else if (m.winner == null) {
            this.points += 1;
        }
    }

    public void goalInFavor() {
        this.goalsMade += 1;
    }

    public void goalAgainst() {
        this.goalsTaken += 1;
    }

    /**
     * Based on final result, several actions may take place such as rating change, record store is also maintained here
     * as well win/no-win streak control
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
                        System.out.println("Unexpected win!");
                    }
                    if (this.rating < 4) {
                        this.rating += (int) (Math.random() * 2);
                        if (this.opponentRating >= (this.rating * 2)) {
                            this.rating += (int) (Math.random() * 2);
                        }
                    }
                }
                if (this.morale < 10 && this.rating < 10) {
                    if ((int) (Math.random() * 10) == 1) {
                        this.rating += (int) (Math.random() * 5);
                        System.out.println("Sayajin!!!");
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
                break;
            case "Lose":
                this.totalGoalsMade += this.goalsMade;
                this.totalGoalsTaken += this.goalsTaken;
                this.losses += 1;
                this.form.add('L');
                this.lossStreak += 1;
                this.noWinStreak += 1;
                if (this.rating > 2) {
                    if (lossStreak >= 3) {
                        this.rating -= (int) (Math.random() * 2);
                        this.lossStreak = 0;
                    }
                    if (humiliation)
                        this.rating -= (int) (Math.random() * 2);
                    if (this.opponentRating <= (this.rating / 2)) {
                        this.rating -= (int) (Math.random() * 2);
                        System.out.println("Unexpected loss!");
                    }
                }
                if (this.morale > 10) {
                    if ((int) (Math.random() * 10) == 10) {
                        this.rating -= (int) (Math.random() * 5);
                        System.out.println("Big Failure!!!");
                    }
                }
                break;
            default:
                break;
        }
        if (this.rating <= (this.morale / 2)) {
            this.rating += (int) (Math.random() * 2);
        }
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

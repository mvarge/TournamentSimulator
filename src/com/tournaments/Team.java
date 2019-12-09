package com.tournaments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Team implements Comparable<Team> {

    public String name;
    public int rating;
    public int points;
    public int totalGoalsMade;
    public int totalGoalsTaken;
    public int goalsMade;
    public int goalsTaken;
    public int wins;
    public int draws;
    public int losses;
    private int opponentRating;
    private int winStreak;
    private int lossStreak;
    private int noWinStreak;
    public int morale;
    List<Character> form = new ArrayList<Character>();

    public Team(String name, int rating) {
        this.name = name;
        this.rating = rating;
        this.morale = rating;
    }

    public void prepareToPlay(Integer opponentRating) {
        this.goalsMade = 0;
        this.goalsTaken = 0;
        this.opponentRating = opponentRating;
    }

    public Integer getPoints() {
        return this.points;
    }

    public Integer getWins() { return this.wins; }

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

    @Override
    public int compareTo(Team t) {
        return Comparator.comparing(Team::getPoints)
                .thenComparing(Team::getWins)
                .thenComparing(Team::getGoalsDifference)
                .compare(this, t);
    }

}

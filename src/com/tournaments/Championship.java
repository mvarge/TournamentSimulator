package com.tournaments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class Championship {

    private List<Team> teams;
    private Turn[] turns;
    private Integer numberOfTurns;
    private boolean homeFactor;

    /**
     * Championship class will handle the teams structure and the amount of turns to be played. It handles the logic
     * for generating an equal number of matches for each turn and separating the teams on each of the rounds
     * as well
     *
     * @param filename      File name used to open and load teams
     * @param numberOfTurns How many turns are going to be played by this instance
     * @param homeFactor    If home teams will have their variables applied or not
     * @throws Exception
     */
    public Championship (String filename, Integer numberOfTurns, boolean homeFactor) throws Exception {
        this.teams = loadTeams(filename);
        this.turns = new Turn[2];
        this.numberOfTurns = numberOfTurns;
        turns[0] = new Turn();
        turns[1] = new Turn();

        for (Turn t : this.turns) {
            for (int i = 0; i < this.teams.size() - 1; i++) {
                Round r = new Round();
                t.addRound(r);
            }
        }

    }

    /**
     * Method to iterate over a file and populate the team Array
     *
     * @param filename      Filename position
     * @return teams        Returns a List<Team> object
     * @throws Exception
     */
    private static List<Team> loadTeams(String filename) throws Exception {

        List<Team> teams = new ArrayList<Team>();

        File file = new File(filename);

        BufferedReader br = new BufferedReader(new FileReader(file));

        String st;
        while ((st = br.readLine()) != null) {
            String[] values = st.split(",", 2);
            Team team = new Team(values[0], Integer.parseInt(values[1]));
            teams.add(team);
        }

        return teams;
    }

    public void generateMatches() {

        List<Team> regularTeams = new LinkedList<Team>(this.teams);

        int size = regularTeams.size();
        List<Team> first = new LinkedList<Team>(regularTeams.subList(0, (size) / 2));
        List<Team> second = new LinkedList<Team>(regularTeams.subList((size) / 2, size));

        Collections.reverse(second);

        for (int turn = 0; turn < this.numberOfTurns; turn++) {
            for (int x = 0; x < regularTeams.size() - 1; x++) {
                Round r = this.turns[turn].rounds.get(x);
                for (int i = 0; i < first.size(); i++) {
                    if (!first.get(i).getName().equals(second.get(i).getName())) {
                        if (x % 2 == turn) {
                            Match m = new Match(first.get(i), second.get(i));
                            r.addMatch(m);
                        } else {
                            Match m = new Match(second.get(i), first.get(i));
                            r.addMatch(m);
                        }
                    }
                }
                Team swapSecond = second.remove(0);
                Team swapFirst = first.remove(first.size() - 1);
                first.add(1, swapSecond);
                second.add(second.size(), swapFirst);
            }
        }
    }

    /**
     * This method will iterate over the rounds and play every Match within it.
     *
     * @param pauseRounds   Will define if we need to press anything to keep going with rounds or not
     */
    public void playMatches(boolean pauseRounds) {
        /*for (Match m : this.matches) {
            m.play(homeFactor);
        }*/
        Scanner scanner = new Scanner(System.in);
        for (Turn t : turns) {
            for (Round r : t.rounds) {
                for (Match m : r.matches) {
                    m.playMatch(this.homeFactor);
                }
                if (pauseRounds) {
                    this.printResults();
                    System.out.print("Type anything to continue> ");
                    String n = scanner.next();
                }
            }
        }

    }

    /**
     * Just an easy way to pretty-print the current status of the championship.
     *
     * TODO: Evaluate if something can be improved around here specially on the printint form style
     */
    public void printResults() {

        Collections.sort(this.teams, Collections.reverseOrder());

        System.out.printf("Team\tPts\tP\tW\tD\tL\tGF\tGA\tGD\tM\tR\tD\tFORM\n");
        for (Team t : this.teams) {
            System.out.printf("%.7s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%s\n", t.getName(), t.getPoints(),
                    t.getTotalMatches(), t.getWins(), t.getDraws(), t.getLosses(), t.getTotalGoalsMade(),
                    t.getTotalGoalsTaken(), t.getGoalsDifference(), t.getMorale(),
                    t.getRating(), (t.getRating() - t.getMorale()),t.printForm());
        }
    }

    public void playAll() {
        this.generateMatches();
        this.playMatches(true);
        this.printResults();
    }



}

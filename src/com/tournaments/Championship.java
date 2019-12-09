package com.tournaments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class Championship {

    private List<Team> teams;
    private List<Match> matches = new ArrayList<Match>();
    private Turn[] turns;
    private Integer numberOfTurns;
    private boolean homeFactor;

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
                    if (!first.get(i).name.equals(second.get(i).name)) {
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




    public void playMatches(boolean pauseRounds) {
        /*for (Match m : this.matches) {
            m.play(homeFactor);
        }*/
        Scanner scanner = new Scanner(System.in);
        for (Turn t : turns) {
            for (Round r : t.rounds) {
                for (Match m : r.matches) {
                    m.play(this.homeFactor);
                }
                if (pauseRounds) {
                    this.printResults();
                    System.out.print("Type anything to continue> ");
                    String n = scanner.next();
                }
            }
        }

    }



    public void printResults() {

        Collections.sort(this.teams, Collections.reverseOrder());

        System.out.printf("Team\tPts\tP\tW\tD\tL\tGF\tGA\tGD\tM\tR\tD\tFORM\n");
        for (Team t : this.teams) {
            System.out.printf("%.7s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%s\n", t.name, t.points, (t.wins + t.draws + t.losses),
                    t.wins, t.draws, t.losses,
                    t.totalGoalsMade, t.totalGoalsTaken, (t.totalGoalsMade - t.totalGoalsTaken), t.morale,
                    t.rating, (t.rating - t.morale),t.printForm());
        }
    }

    public void playAll() {
        this.generateMatches();
        this.playMatches(true);
        this.printResults();
    }



}

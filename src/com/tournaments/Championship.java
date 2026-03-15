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
    private List<Map<String, Object>> matchLog = new ArrayList<>();

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
    public Championship(String filename, Integer numberOfTurns, boolean homeFactor) throws Exception {
        this.teams = loadTeams(filename);
        this.turns = new Turn[2];
        this.numberOfTurns = numberOfTurns;
        this.homeFactor = homeFactor;
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
     * Play all matches and collect a log of every result for JSON output.
     */
    public void playMatches(boolean pauseRounds) {
        int turnNum = 0;
        int roundNum = 0;
        for (Turn t : turns) {
            turnNum++;
            roundNum = 0;
            for (Round r : t.rounds) {
                roundNum++;
                List<Map<String, Object>> roundResults = new ArrayList<>();
                for (Match m : r.matches) {
                    m.playMatch(this.homeFactor);
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("home", m.teams.get("Home").getName().trim());
                    result.put("away", m.teams.get("Away").getName().trim());
                    result.put("homeGoals", m.teams.get("Home").getGoalsMade());
                    result.put("awayGoals", m.teams.get("Away").getGoalsMade());
                    result.put("winner", m.winner != null ? m.winner.trim() : null);
                    roundResults.add(result);
                }
                Map<String, Object> roundEntry = new LinkedHashMap<>();
                roundEntry.put("turn", turnNum);
                roundEntry.put("round", roundNum);
                roundEntry.put("matches", roundResults);
                matchLog.add(roundEntry);
            }
        }
    }

    /**
     * Print league table to stdout (original behaviour).
     */
    public void printResults() {
        Collections.sort(this.teams, Collections.reverseOrder());
        System.out.printf("Team\tPts\tP\tW\tD\tL\tGF\tGA\tGD\tM\tR\tDf\tFORM\n");
        for (Team t : this.teams) {
            System.out.printf("%.7s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%s\n",
                    t.getName(), t.getPoints(), t.getTotalMatches(),
                    t.getWins(), t.getDraws(), t.getLosses(),
                    t.getTotalGoalsMade(), t.getTotalGoalsTaken(), t.getGoalsDifference(),
                    t.getMorale(), t.getRating(), (t.getRating() - t.getMorale()), t.printForm());
        }
    }

    /**
     * Output the full simulation result as JSON to stdout.
     * Used by the web backend to capture structured data.
     */
    public void printJson() {
        Collections.sort(this.teams, Collections.reverseOrder());

        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // standings
        sb.append("\"standings\":[");
        for (int i = 0; i < teams.size(); i++) {
            Team t = teams.get(i);
            sb.append("{");
            sb.append("\"pos\":").append(i + 1).append(",");
            sb.append("\"name\":\"").append(escape(t.getName().trim())).append("\",");
            sb.append("\"pts\":").append(t.getPoints()).append(",");
            sb.append("\"played\":").append(t.getTotalMatches()).append(",");
            sb.append("\"w\":").append(t.getWins()).append(",");
            sb.append("\"d\":").append(t.getDraws()).append(",");
            sb.append("\"l\":").append(t.getLosses()).append(",");
            sb.append("\"gf\":").append(t.getTotalGoalsMade()).append(",");
            sb.append("\"ga\":").append(t.getTotalGoalsTaken()).append(",");
            sb.append("\"gd\":").append(t.getGoalsDifference()).append(",");
            sb.append("\"morale\":").append(t.getMorale()).append(",");
            sb.append("\"rating\":").append(t.getRating()).append(",");
            sb.append("\"initialRating\":").append(t.getInitialRating()).append(",");
            // form as array of chars
            sb.append("\"form\":[");
            List<Character> form = t.form;
            for (int j = 0; j < form.size(); j++) {
                sb.append("\"").append(form.get(j)).append("\"");
                if (j < form.size() - 1) sb.append(",");
            }
            sb.append("]");
            sb.append("}");
            if (i < teams.size() - 1) sb.append(",");
        }
        sb.append("],");

        // match log
        sb.append("\"rounds\":[");
        for (int r = 0; r < matchLog.size(); r++) {
            Map<String, Object> round = matchLog.get(r);
            sb.append("{");
            sb.append("\"turn\":").append(round.get("turn")).append(",");
            sb.append("\"round\":").append(round.get("round")).append(",");
            sb.append("\"matches\":[");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matches = (List<Map<String, Object>>) round.get("matches");
            for (int m = 0; m < matches.size(); m++) {
                Map<String, Object> match = matches.get(m);
                sb.append("{");
                sb.append("\"home\":\"").append(escape((String) match.get("home"))).append("\",");
                sb.append("\"away\":\"").append(escape((String) match.get("away"))).append("\",");
                sb.append("\"homeGoals\":").append(match.get("homeGoals")).append(",");
                sb.append("\"awayGoals\":").append(match.get("awayGoals")).append(",");
                Object winner = match.get("winner");
                if (winner == null) {
                    sb.append("\"winner\":null");
                } else {
                    sb.append("\"winner\":\"").append(escape((String) winner)).append("\"");
                }
                sb.append("}");
                if (m < matches.size() - 1) sb.append(",");
            }
            sb.append("]}");
            if (r < matchLog.size() - 1) sb.append(",");
        }
        sb.append("]");

        sb.append("}");
        System.out.println(sb.toString());
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public void playAll() {
        this.generateMatches();
        this.playMatches(false);
        this.printResults();
    }
}

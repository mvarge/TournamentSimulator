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
        // FIX: was hardcoded to new Turn[2] — any numberOfTurns > 2 would crash
        this.turns = new Turn[numberOfTurns];
        this.numberOfTurns = numberOfTurns;
        this.homeFactor = homeFactor;

        for (int t = 0; t < numberOfTurns; t++) {
            turns[t] = new Turn();
            for (int i = 0; i < this.teams.size() - 1; i++) {
                turns[t].addRound(new Round(i + 1));
            }
        }
    }

    /**
     * Method to iterate over a file and populate the team Array.
     * Supports two formats:
     *   Name,Rating              (legacy — attack = defence = rating)
     *   Name,Attack,Defence      (new split ratings)
     */
    private static List<Team> loadTeams(String filename) throws Exception {
        List<Team> teams = new ArrayList<Team>();
        File file = new File(filename);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        while ((st = br.readLine()) != null) {
            if (st.trim().isEmpty()) continue;
            String[] values = st.split(",");
            Team team;
            if (values.length >= 3) {
                team = new Team(values[0],
                        Integer.parseInt(values[1].trim()),
                        Integer.parseInt(values[2].trim()));
            } else {
                team = new Team(values[0], Integer.parseInt(values[1].trim()));
            }
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
                        if (x % 2 == turn % 2) {
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
     * Play all matches and collect a log of every result (including the
     * minute-by-minute event feed) for JSON output.
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
                    result.put("events", m.getEvents());
                    result.put("momentum", m.getMomentum());
                    // Team state snapshots AFTER this match — lets the frontend
                    // rebuild standings incrementally as matches are revealed.
                    result.put("homeState", snapshot(m.teams.get("Home")));
                    result.put("awayState", snapshot(m.teams.get("Away")));
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

    /** Small snapshot of a team's live variables right after a match. */
    private static Map<String, Object> snapshot(Team t) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("morale", t.getMorale());
        s.put("attack", t.getAttack());
        s.put("defence", t.getDefence());
        s.put("rating", t.getRating());
        return s;
    }

    /**
     * Print league table to stdout (original behaviour).
     */
    public void printResults() {
        Collections.sort(this.teams, Collections.reverseOrder());
        System.out.printf("Team\tPts\tP\tW\tD\tL\tGF\tGA\tGD\tM\tAtt\tDef\tR\tFORM\n");
        for (Team t : this.teams) {
            System.out.printf("%.7s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%s\n",
                    t.getName(), t.getPoints(), t.getTotalMatches(),
                    t.getWins(), t.getDraws(), t.getLosses(),
                    t.getTotalGoalsMade(), t.getTotalGoalsTaken(), t.getGoalsDifference(),
                    t.getMorale(), t.getAttack(), t.getDefence(), t.getRating(), t.printForm());
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

        // initial team states (before any match) — used by the frontend for step-by-step mode
        sb.append("\"teams\":[");
        for (int i = 0; i < teams.size(); i++) {
            Team t = teams.get(i);
            sb.append("{");
            sb.append("\"name\":\"").append(escape(t.getName().trim())).append("\",");
            sb.append("\"attack\":").append(t.getInitialAttack()).append(",");
            sb.append("\"defence\":").append(t.getInitialDefence()).append(",");
            sb.append("\"rating\":").append(t.getInitialRating()).append(",");
            sb.append("\"morale\":").append(t.getInitialRating());
            sb.append("}");
            if (i < teams.size() - 1) sb.append(",");
        }
        sb.append("],");

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
            sb.append("\"attack\":").append(t.getAttack()).append(",");
            sb.append("\"defence\":").append(t.getDefence()).append(",");
            sb.append("\"initialRating\":").append(t.getInitialRating()).append(",");
            sb.append("\"initialAttack\":").append(t.getInitialAttack()).append(",");
            sb.append("\"initialDefence\":").append(t.getInitialDefence()).append(",");
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
                    sb.append("\"winner\":null,");
                } else {
                    sb.append("\"winner\":\"").append(escape((String) winner)).append("\",");
                }
                // minute-by-minute event feed
                sb.append("\"events\":[");
                @SuppressWarnings("unchecked")
                List<MatchEvent> events = (List<MatchEvent>) match.get("events");
                for (int e = 0; e < events.size(); e++) {
                    MatchEvent ev = events.get(e);
                    sb.append("{");
                    sb.append("\"min\":").append(ev.minute).append(",");
                    sb.append("\"type\":\"").append(ev.type).append("\",");
                    if (ev.side == null) {
                        sb.append("\"side\":null,");
                    } else {
                        sb.append("\"side\":\"").append(ev.side).append("\",");
                    }
                    sb.append("\"text\":\"").append(escape(ev.text)).append("\"");
                    sb.append("}");
                    if (e < events.size() - 1) sb.append(",");
                }
                sb.append("],");
                // per-minute momentum (positive = home pressure)
                sb.append("\"momentum\":[");
                @SuppressWarnings("unchecked")
                List<Integer> moms = (List<Integer>) match.get("momentum");
                for (int q = 0; q < moms.size(); q++) {
                    sb.append(moms.get(q));
                    if (q < moms.size() - 1) sb.append(",");
                }
                sb.append("],");
                // post-match team state snapshots
                appendState(sb, "homeState", match.get("homeState"));
                sb.append(",");
                appendState(sb, "awayState", match.get("awayState"));
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

    @SuppressWarnings("unchecked")
    private static void appendState(StringBuilder sb, String key, Object stateObj) {
        Map<String, Object> state = (Map<String, Object>) stateObj;
        sb.append("\"").append(key).append("\":{");
        sb.append("\"morale\":").append(state.get("morale")).append(",");
        sb.append("\"attack\":").append(state.get("attack")).append(",");
        sb.append("\"defence\":").append(state.get("defence")).append(",");
        sb.append("\"rating\":").append(state.get("rating"));
        sb.append("}");
    }

    public void playAll() {
        this.generateMatches();
        this.playMatches(false);
        this.printResults();
    }
}

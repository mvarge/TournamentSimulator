package com.tournaments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        String basePath = new File("").getAbsolutePath();

        // Default values
        String championshipCountry = "england.txt";
        boolean jsonMode = false;
        boolean watchMode = false;
        List<String> watchTeams = new ArrayList<>();

        // Parse args: java Main [league_file] [--json] [--watch Home Away]
        for (String arg : args) {
            if (arg.equals("--json")) {
                jsonMode = true;
            } else if (arg.equals("--watch")) {
                watchMode = true;
            } else if (arg.endsWith(".txt")) {
                championshipCountry = arg;
            } else if (watchMode) {
                watchTeams.add(arg);
            }
        }

        String leaguePath = basePath + "/databasefiles/" + championshipCountry;

        if (watchMode) {
            watchMatch(leaguePath, watchTeams);
            return;
        }

        Championship c = new Championship(leaguePath, 2, true);
        c.generateMatches();
        c.playMatches(false);

        if (jsonMode) {
            c.printJson();
        } else {
            c.printResults();
        }
    }

    /**
     * FM-style exhibition mode: watch a single match minute by minute.
     * Usage: java com.tournaments.Main [league.txt] --watch [HomeTeam] [AwayTeam]
     * If team names are omitted, two random teams from the league are picked.
     */
    private static void watchMatch(String leaguePath, List<String> names) throws Exception {
        List<Team> teams = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(new File(leaguePath)));
        String st;
        while ((st = br.readLine()) != null) {
            if (st.trim().isEmpty()) continue;
            String[] v = st.split(",");
            teams.add(v.length >= 3
                    ? new Team(v[0].trim(), Integer.parseInt(v[1].trim()), Integer.parseInt(v[2].trim()))
                    : new Team(v[0].trim(), Integer.parseInt(v[1].trim())));
        }

        Team home = null, away = null;
        if (names.size() >= 2) {
            for (Team t : teams) {
                if (t.getName().trim().equalsIgnoreCase(names.get(0))) home = t;
                if (t.getName().trim().equalsIgnoreCase(names.get(1))) away = t;
            }
            if (home == null || away == null) {
                System.err.println("Team not found in league file. Available teams:");
                for (Team t : teams) System.err.println("  " + t.getName().trim());
                return;
            }
        } else {
            int h = (int) (Math.random() * teams.size());
            int a;
            do { a = (int) (Math.random() * teams.size()); } while (a == h);
            home = teams.get(h);
            away = teams.get(a);
        }

        System.out.printf("%n=== %s (Att %d / Def %d) vs %s (Att %d / Def %d) ===%n%n",
                home.getName().trim(), home.getAttack(), home.getDefence(),
                away.getName().trim(), away.getAttack(), away.getDefence());

        Match m = new Match(home, away);
        m.playMatch(true);

        for (MatchEvent ev : m.getEvents()) {
            String tag;
            switch (ev.type) {
                case "GOAL":     tag = "⚽"; break;
                case "SAVE":     tag = "🧤"; break;
                case "POST":     tag = "🥅"; break;
                case "YELLOW":   tag = "🟨"; break;
                case "RED":      tag = "🟥"; break;
                case "HALFTIME":
                case "FULLTIME": tag = "⏱ "; break;
                default:         tag = "  "; break;
            }
            System.out.printf("%3d' %s %s%n", ev.minute, tag, ev.text);
            try { Thread.sleep(ev.type.equals("GOAL") ? 900 : 350); } catch (InterruptedException ignored) {}
        }
    }
}

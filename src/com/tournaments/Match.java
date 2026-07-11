package com.tournaments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Match class simulates a full 90-minute game, Football Manager style.
 *
 * Instead of the old "N goal opportunities decided by dice", the engine now walks
 * minute by minute. Each minute there is a chance of an attacking situation;
 * which side gets it is weighted by ATTACK rating, and whether it becomes a goal
 * depends on the attacker's ATTACK vs the defender's DEFENCE. Everything that
 * happens (goals, saves, misses, shots off the post, cards) is recorded as a
 * MatchEvent with a commentary line, so matches can be replayed/watched.
 *
 * Average goals per match is tuned to ~2.6, matching real football and the old engine.
 */
public class Match {

    String winner = null;  // null means draw
    HashMap<String, Team> teams = new HashMap<String, Team>();
    List<MatchEvent> events = new ArrayList<MatchEvent>();

    private static final String[] GOAL_LINES = {
            "GOOOAL! %s find the back of the net!",
            "GOAL! A clinical finish from %s!",
            "GOAL! %s score with a thunderous strike!",
            "GOAL! A beautiful team move finished off by %s!",
            "GOAL! The keeper had no chance — %s make it count!",
            "GOAL! A header at the far post gives %s the goal!"
    };
    private static final String[] SAVE_LINES = {
            "Great chance for %s but the keeper makes a stunning save!",
            "%s force the goalkeeper into a fingertip save!",
            "The shot from %s is parried away by the keeper!",
            "One-on-one for %s... but the keeper stands tall and wins the duel!"
    };
    private static final String[] MISS_LINES = {
            "%s blaze the shot high over the bar!",
            "A promising attack from %s ends with a shot wide of the post.",
            "%s snatch at the chance and drag it wide!",
            "The cross finds a %s player unmarked... but the header goes over!"
    };
    private static final String[] POST_LINES = {
            "%s rattle the crossbar! So close!",
            "Off the post! %s can't believe it!",
            "%s hit the woodwork — inches away from a goal!"
    };
    private static final String[] YELLOW_LINES = {
            "Cynical foul by %s — the referee shows a yellow card.",
            "A late challenge from %s earns a booking.",
            "%s pick up a yellow card for dissent."
    };
    private static final String[] RED_LINES = {
            "RED CARD! %s are down to ten men after a horrific tackle!",
            "Straight red! A last-man foul and %s must play on a man short!"
    };

    public Match(Team home, Team away) {
        teams.put("Home", home);
        teams.put("Away", away);
    }

    public List<MatchEvent> getEvents() {
        return events;
    }

    public void playMatch() {
        playMatch(true);
    }

    private static String pick(String[] pool) {
        return pool[(int) (Math.random() * pool.length)];
    }

    private void addEvent(int minute, String type, String side, String template, String teamName) {
        events.add(new MatchEvent(minute, type, side, String.format(template, teamName)));
    }

    /**
     * @param homeFactor    Defines if home factors are applied such as giving more advantage to whoever is the home team
     */
    public void playMatch(boolean homeFactor) {

        Team home = this.teams.get("Home");
        Team away = this.teams.get("Away");

        double homeAtt = home.getAttack();
        double homeDef = home.getDefence();
        double awayAtt = away.getAttack();
        double awayDef = away.getDefence();

        // Upset/surprise mechanic — 10% of matches the sides "swap" strengths for the day.
        // Happens BEFORE prepareToPlay so unexpected-result logic sees the effective ratings.
        boolean swapped = false;
        if ((int) (Math.random() * 10) == 1) {
            double t = homeAtt; homeAtt = awayAtt; awayAtt = t;
            t = homeDef; homeDef = awayDef; awayDef = t;
            swapped = true;
        }

        // Store opponent overall ratings (after any swap) for streak/unexpected-result logic
        home.prepareToPlay((int) ((awayAtt + awayDef) / 2));
        away.prepareToPlay((int) ((homeAtt + homeDef) / 2));

        // Home advantage: boost to attack and defence (~7.5% avg, like the old +0-15 rating bump)
        if (homeFactor) {
            homeAtt += (int) (Math.random() * 12);
            homeDef += (int) (Math.random() * 8);
        }

        // Morale boost: higher morale side gets a small edge going forward
        if (home.getMorale() > away.getMorale()) {
            homeAtt += (int) (Math.random() * 10);
        } else if (away.getMorale() > home.getMorale()) {
            awayAtt += (int) (Math.random() * 10);
        }

        int stoppage = 1 + (int) (Math.random() * 4);  // 90+1 .. 90+4
        int totalMinutes = 90 + stoppage;

        addEvent(1, "KICKOFF", null, "The referee blows the whistle — %s!", "we are underway");

        for (int minute = 1; minute <= totalMinutes; minute++) {

            if (minute == 46) {
                addEvent(45, "HALFTIME", null,
                        "Half-time: %s " + home.getGoalsMade() + " x " + away.getGoalsMade() + " " + away.name.trim(),
                        home.name.trim());
            }

            // Chance of an attacking situation this minute; better attacks create more chances.
            double avgAtt = (homeAtt + awayAtt) / 2.0;
            double chanceP = 0.11 * (avgAtt / 75.0);
            if (Math.random() >= chanceP) continue;

            // Who attacks? Weighted by attack strength.
            boolean homeAttacks = Math.random() < (homeAtt / (homeAtt + awayAtt));
            String side = homeAttacks ? "Home" : "Away";
            Team attacker = homeAttacks ? home : away;
            Team defender = homeAttacks ? away : home;
            double att = homeAttacks ? homeAtt : awayAtt;
            double def = homeAttacks ? awayDef : homeDef;

            // Occasionally the "chance" is actually a foul that produces a card
            double flavour = Math.random();
            if (flavour < 0.08) {
                // Card against the DEFENDING side
                String defSide = homeAttacks ? "Away" : "Home";
                if (Math.random() < 0.07) {
                    addEvent(minute, "RED", defSide, pick(RED_LINES), defender.name.trim());
                    // Ten men: defending side loses strength for the rest of the match
                    if (homeAttacks) { awayAtt *= 0.85; awayDef *= 0.85; }
                    else { homeAtt *= 0.85; homeDef *= 0.85; }
                } else {
                    addEvent(minute, "YELLOW", defSide, pick(YELLOW_LINES), defender.name.trim());
                }
                continue;
            }

            // Conversion: attack vs defence duel (tuned for ~2.6 goals/match)
            double goalP = 0.9 * (att / (att + def * 2.2));
            if (Math.random() < goalP) {
                attacker.goalInFavor();
                defender.goalAgainst();
                int displayMin = Math.min(minute, 90);
                addEvent(displayMin, "GOAL", side,
                        pick(GOAL_LINES) + "  (" + home.getGoalsMade() + " x " + away.getGoalsMade() + ")",
                        attacker.name.trim());
            } else {
                double outcome = Math.random();
                if (outcome < 0.50) {
                    addEvent(Math.min(minute, 90), "SAVE", side, pick(SAVE_LINES), attacker.name.trim());
                } else if (outcome < 0.85) {
                    addEvent(Math.min(minute, 90), "MISS", side, pick(MISS_LINES), attacker.name.trim());
                } else {
                    addEvent(Math.min(minute, 90), "POST", side, pick(POST_LINES), attacker.name.trim());
                }
            }
        }

        int homeGoals = home.getGoalsMade();
        int awayGoals = away.getGoalsMade();

        addEvent(90, "FULLTIME", null,
                "Full-time: %s " + homeGoals + " x " + awayGoals + " " + away.name.trim(), home.name.trim());

        System.err.printf("Final score: %s %d x %d %s%s\n",
                home.name, homeGoals, awayGoals, away.name,
                swapped ? " (upset!)" : "");

        if (homeGoals > awayGoals) {
            this.winner = home.name;
            home.finishGame("Win");
            away.finishGame("Lose");
        } else if (awayGoals > homeGoals) {
            this.winner = away.name;
            away.finishGame("Win");
            home.finishGame("Lose");
        } else {
            this.winner = null;  // draw
            home.finishGame("Draw");
            away.finishGame("Draw");
        }
    }
}

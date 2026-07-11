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
    // Per-minute momentum, index = minute-1. Range -100..100, positive = HOME
    // pressure, negative = AWAY pressure. Used for the FM-style pressure bar
    // and the 2D pitch view.
    List<Integer> momentum = new ArrayList<Integer>();

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
    private static final String[] PENALTY_LINES = {
            "PENALTY to %s! The referee points to the spot!",
            "The defender brings him down — it's a penalty for %s!",
            "Handball in the box! %s have a penalty!"
    };
    private static final String[] PEN_GOAL_LINES = {
            "GOAL! %s make no mistake from the spot!",
            "GOAL! Cool as you like, %s convert the penalty!",
            "GOAL! Sent the keeper the wrong way — %s score!"
    };
    private static final String[] PEN_MISS_LINES = {
            "MISSED! %s blaze the penalty over the bar!",
            "SAVED! The keeper guesses right and denies %s from the spot!",
            "Off the post! %s cannot believe they missed the penalty!"
    };
    private static final String[] CORNER_LINES = {
            "Corner for %s — the big players go up.",
            "%s swing in a dangerous corner...",
            "Another corner won by %s as they pile on the pressure."
    };
    private static final String[] FREEKICK_LINES = {
            "Dangerous free-kick in a good area for %s.",
            "%s line up a free-kick on the edge of the box...",
            "Promising set-piece opportunity for %s."
    };
    private static final String[] OFFSIDE_LINES = {
            "The flag is up — %s caught offside.",
            "%s think they've scored but it's ruled out for offside!",
            "A fine offside trap snuffs out the %s attack."
    };
    private static final String[] INJURY_LINES = {
            "A %s player goes down and needs treatment.",
            "Play is stopped as a %s player receives attention.",
            "Injury concern for %s as the physio comes on."
    };
    private static final String[] SUB_LINES = {
            "%s make a change, fresh legs off the bench.",
            "Tactical substitution for %s.",
            "%s turn to their bench to shake things up."
    };
    private static final String[] BUILDUP_LINES = {
            "%s knock it around patiently, probing for an opening.",
            "Good spell of possession for %s.",
            "%s push forward looking for a way through.",
            "The tempo lifts as %s take control of midfield.",
            "%s work it wide but the cross is cleared."
    };
    private static final String[] TACKLE_LINES = {
            "Superb last-ditch tackle to deny %s!",
            "Crunching challenge halts the %s attack.",
            "%s are dispossessed by a well-timed interception."
    };

    public Match(Team home, Team away) {
        teams.put("Home", home);
        teams.put("Away", away);
    }

    public List<MatchEvent> getEvents() {
        return events;
    }

    public List<Integer> getMomentum() {
        return momentum;
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

        // Match tempo: how open/entertaining THIS game is. Drawn per match so some
        // fixtures are end-to-end thrillers and others are cagey, low-event affairs.
        // 0.55 = dour, 1.0 = average, ~1.6 = basketball. Skewed toward the middle.
        double tempo = 0.55 + Math.pow(Math.random(), 1.5) * 1.05;

        addEvent(1, "KICKOFF", null, "The referee blows the whistle — %s!", "we are underway");

        // Momentum: smoothed random walk biased toward the stronger attack,
        // spiked by events (goals, chances, corners...). Positive = home pressure.
        double mom = 0;

        for (int minute = 1; minute <= totalMinutes; minute++) {

            // drift toward the natural balance of strength + some noise
            double bias = 60.0 * (homeAtt - awayAtt) / (homeAtt + awayAtt);
            mom = mom * 0.80 + bias * 0.20 + (Math.random() * 2 - 1) * 26;
            if (mom > 100) mom = 100;
            if (mom < -100) mom = -100;
            momentum.add((int) Math.round(mom));

            if (minute == 46) {
                addEvent(45, "HALFTIME", null,
                        "Half-time: %s " + home.getGoalsMade() + " x " + away.getGoalsMade() + " " + away.name.trim(),
                        home.name.trim());
            }

            // Chance of SOMETHING happening this minute; scaled by attack quality and tempo.
            double avgAtt = (homeAtt + awayAtt) / 2.0;
            double eventP = 0.22 * (avgAtt / 75.0) * tempo;
            if (Math.random() >= eventP) continue;

            // Who has the ball? Weighted by attack strength.
            boolean homeAttacks = Math.random() < (homeAtt / (homeAtt + awayAtt));
            // an attacking moment swings momentum toward that side
            mom += homeAttacks ? 22 : -22;
            if (mom > 100) mom = 100;
            if (mom < -100) mom = -100;
            momentum.set(momentum.size() - 1, (int) Math.round(mom));
            String side = homeAttacks ? "Home" : "Away";
            String defSide = homeAttacks ? "Away" : "Home";
            Team attacker = homeAttacks ? home : away;
            Team defender = homeAttacks ? away : home;
            double att = homeAttacks ? homeAtt : awayAtt;
            double def = homeAttacks ? awayDef : homeDef;
            int displayMin = Math.min(minute, 90);

            // What KIND of moment is this? Not every event is a clear chance —
            // plenty of build-up, set-pieces and stoppages add texture.
            double roll = Math.random();

            // 20%: non-threatening flavour (build-up, tackles, offside, injury, sub)
            if (roll < 0.20) {
                double f = Math.random();
                if (f < 0.40) {
                    addEvent(minute, "INFO", side, pick(BUILDUP_LINES), attacker.name.trim());
                } else if (f < 0.62) {
                    addEvent(minute, "TACKLE", side, pick(TACKLE_LINES), attacker.name.trim());
                } else if (f < 0.80) {
                    addEvent(minute, "OFFSIDE", side, pick(OFFSIDE_LINES), attacker.name.trim());
                } else if (f < 0.92 && minute > 25) {
                    addEvent(minute, "SUB", side, pick(SUB_LINES), attacker.name.trim());
                } else {
                    addEvent(minute, "INJURY", side, pick(INJURY_LINES), attacker.name.trim());
                }
                continue;
            }

            // 12%: a foul — mostly free-kicks, sometimes cards against the defence
            if (roll < 0.32) {
                double f = Math.random();
                if (f < 0.10) {
                    addEvent(minute, "RED", defSide, pick(RED_LINES), defender.name.trim());
                    if (homeAttacks) { awayAtt *= 0.85; awayDef *= 0.85; }
                    else { homeAtt *= 0.85; homeDef *= 0.85; }
                } else if (f < 0.45) {
                    addEvent(minute, "YELLOW", defSide, pick(YELLOW_LINES), defender.name.trim());
                } else {
                    addEvent(minute, "FREEKICK", side, pick(FREEKICK_LINES), attacker.name.trim());
                }
                continue;
            }

            // 8%: a corner (occasionally leads straight to a chance)
            if (roll < 0.40) {
                addEvent(minute, "CORNER", side, pick(CORNER_LINES), attacker.name.trim());
                if (Math.random() > 0.45) continue;  // most corners come to nothing
                // else fall through into the open-play chance below
            } else if (roll < 0.43) {
                // 3%: a penalty
                addEvent(minute, "PENALTY", side, pick(PENALTY_LINES), attacker.name.trim());
                if (Math.random() < 0.76) {
                    attacker.goalInFavor();
                    defender.goalAgainst();
                    addEvent(displayMin, "GOAL", side,
                            pick(PEN_GOAL_LINES) + "  (" + home.getGoalsMade() + " x " + away.getGoalsMade() + ")",
                            attacker.name.trim());
                } else {
                    addEvent(displayMin, "PENMISS", side, pick(PEN_MISS_LINES), attacker.name.trim());
                }
                continue;
            }

            // Otherwise: an open-play chance — attack vs defence duel (tuned ~2.6 goals/match)
            double goalP = 0.62 * (att / (att + def * 2.2));
            if (Math.random() < goalP) {
                attacker.goalInFavor();
                defender.goalAgainst();
                addEvent(displayMin, "GOAL", side,
                        pick(GOAL_LINES) + "  (" + home.getGoalsMade() + " x " + away.getGoalsMade() + ")",
                        attacker.name.trim());
            } else {
                double outcome = Math.random();
                if (outcome < 0.50) {
                    addEvent(displayMin, "SAVE", side, pick(SAVE_LINES), attacker.name.trim());
                } else if (outcome < 0.85) {
                    addEvent(displayMin, "MISS", side, pick(MISS_LINES), attacker.name.trim());
                } else {
                    addEvent(displayMin, "POST", side, pick(POST_LINES), attacker.name.trim());
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

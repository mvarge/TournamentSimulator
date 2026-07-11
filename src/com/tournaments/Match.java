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

    // Ball position track for the 2D pitch view: TICKS_PER_MIN entries per
    // minute, each {x 0..100 (home attacks toward 100), y 0..100, possession
    // 0=home 1=away}. Serialized flat as [x,y,p, x,y,p, ...].
    public static final int TICKS_PER_MIN = 4;
    List<int[]> track = new ArrayList<int[]>();
    private double bx = 0.5, by = 0.5;   // live ball position (0..1)
    private int poss = 0;                // 0 = home, 1 = away

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

    public List<int[]> getTrack() {
        return track;
    }

    // ── Ball track helpers ──────────────────────────────────────────────
    private void trackTick() {
        track.add(new int[] {
                (int) Math.round(bx * 100),
                (int) Math.round(by * 100),
                poss });
    }

    /** Move the ball smoothly toward (tx,ty) over the remaining ticks of this minute. */
    private void ballTravel(double tx, double ty, int ticks) {
        for (int i = 0; i < ticks; i++) {
            double f = (i + 1) / (double) ticks;
            // ease-out travel with a little lateral wobble
            double wob = (Math.random() - 0.5) * 0.05 * (1 - f);
            bx = bx + (tx - bx) * f;
            by = by + (ty - by) * f + wob;
            clampBall();
            trackTick();
        }
    }

    /** Aimless possession football for one minute (TICKS_PER_MIN ticks). */
    private void ballDrift(double mom) {
        // drift target leans toward the goal the possessing side attacks
        double lean = (poss == 0 ? 1 : -1) * (0.10 + Math.random() * 0.12)
                    + (mom / 100.0) * 0.06;
        for (int i = 0; i < TICKS_PER_MIN; i++) {
            // occasional turnover in midfield
            if (Math.random() < 0.16) { poss = 1 - poss; lean = -lean * 0.7; }
            bx += lean * 0.22 + (Math.random() - 0.5) * 0.10;
            by += (Math.random() - 0.5) * 0.16 + (0.5 - by) * 0.06;
            clampBall();
            trackTick();
        }
    }

    private void clampBall() {
        if (bx < 0.02) bx = 0.02;
        if (bx > 0.98) bx = 0.98;
        if (by < 0.04) by = 0.04;
        if (by > 0.96) by = 0.96;
    }

    /** Ball crosses the goal line INTO the net (past the pitch boundary, no clamp). */
    private void ballIntoNet(boolean homeAttacks, double y) {
        bx = homeAttacks ? 1.02 : -0.02;   // beyond x=1 / x=0 → inside the drawn net
        by = y;
        trackTick();
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
            if (Math.random() >= eventP) {
                ballDrift(mom);   // quiet minute: possession football
                continue;
            }

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

            // Pitch geography for this attack (home attacks toward x=1)
            poss = homeAttacks ? 0 : 1;
            double gx  = homeAttacks ? 0.96 : 0.04;   // goal mouth
            double bxT = homeAttacks ? 0.84 : 0.16;   // edge of the box
            double spotX = homeAttacks ? 0.88 : 0.12; // penalty spot
            double fkX = homeAttacks ? 0.72 : 0.28;   // free-kick range
            double midX = homeAttacks ? 0.58 : 0.42;  // final third entry
            double wideY = Math.random() < 0.5 ? 0.12 : 0.88;
            double cy = 0.30 + Math.random() * 0.40;  // central-ish y

            // What KIND of moment is this? Not every event is a clear chance —
            // plenty of build-up, set-pieces and stoppages add texture.
            double roll = Math.random();

            // 20%: non-threatening flavour (build-up, tackles, offside, injury, sub)
            if (roll < 0.20) {
                double f = Math.random();
                if (f < 0.40) {
                    addEvent(minute, "INFO", side, pick(BUILDUP_LINES), attacker.name.trim());
                    ballDrift(mom);
                } else if (f < 0.62) {
                    addEvent(minute, "TACKLE", side, pick(TACKLE_LINES), attacker.name.trim());
                    ballTravel(midX, cy, 2);              // attack builds...
                    poss = 1 - poss;                       // ...and is snuffed out
                    ballTravel(0.5, 0.5, 2);
                } else if (f < 0.80) {
                    addEvent(minute, "OFFSIDE", side, pick(OFFSIDE_LINES), attacker.name.trim());
                    ballTravel(bxT, cy, 3);                // through-ball into the box
                    poss = 1 - poss;                       // flag up, free-kick out
                    ballTravel(midX, 0.5, 1);
                } else if (f < 0.92 && minute > 25) {
                    addEvent(minute, "SUB", side, pick(SUB_LINES), attacker.name.trim());
                    ballDrift(mom);
                } else {
                    addEvent(minute, "INJURY", side, pick(INJURY_LINES), attacker.name.trim());
                    ballDrift(mom);
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
                    ballTravel(midX, cy, 4);               // play stops where the foul was
                } else if (f < 0.45) {
                    addEvent(minute, "YELLOW", defSide, pick(YELLOW_LINES), defender.name.trim());
                    ballTravel(midX, cy, 4);
                } else {
                    addEvent(minute, "FREEKICK", side, pick(FREEKICK_LINES), attacker.name.trim());
                    ballTravel(fkX, cy, 2);                // set it down...
                    ballTravel(bxT, 0.5, 2);               // ...delivery into the box
                }
                continue;
            }

            // 8%: a corner (occasionally leads straight to a chance)
            boolean cornerChance = false;
            if (roll < 0.40) {
                addEvent(minute, "CORNER", side, pick(CORNER_LINES), attacker.name.trim());
                ballTravel(gx, wideY < 0.5 ? 0.05 : 0.95, 2);   // out to the flag
                if (Math.random() > 0.45) {
                    poss = 1 - poss;                       // cleared away
                    ballTravel(midX, cy, 2);
                    continue;
                }
                cornerChance = true;                       // delivery creates a chance
            } else if (roll < 0.43) {
                // 3%: a penalty
                addEvent(minute, "PENALTY", side, pick(PENALTY_LINES), attacker.name.trim());
                ballTravel(spotX, 0.5, 2);                 // placed on the spot
                if (Math.random() < 0.76) {
                    attacker.goalInFavor();
                    defender.goalAgainst();
                    addEvent(displayMin, "GOAL", side,
                            pick(PEN_GOAL_LINES) + "  (" + home.getGoalsMade() + " x " + away.getGoalsMade() + ")",
                            attacker.name.trim());
                    ballIntoNet(homeAttacks, 0.5);         // buried in the net
                    poss = 1 - poss;
                    bx = 0.5; by = 0.5; trackTick();       // back to kickoff
                } else {
                    addEvent(displayMin, "PENMISS", side, pick(PEN_MISS_LINES), attacker.name.trim());
                    ballTravel(gx, Math.random() < 0.5 ? 0.28 : 0.72, 1);  // wide/saved
                    poss = 1 - poss;
                    ballTravel(bxT, 0.5, 1);
                }
                continue;
            }

            // Otherwise: an open-play chance — attack vs defence duel (tuned ~2.6 goals/match)
            // Every branch below emits exactly TICKS_PER_MIN ticks so the
            // frontend can map tick index = (minute-1) * TICKS_PER_MIN.
            int lead = cornerChance ? 0 : 2;               // corner delivery = header from the flag
            ballTravel(bxT, cy, lead);                     // surge into the box
            double goalP = 0.62 * (att / (att + def * 2.2));
            if (Math.random() < goalP) {
                attacker.goalInFavor();
                defender.goalAgainst();
                addEvent(displayMin, "GOAL", side,
                        pick(GOAL_LINES) + "  (" + home.getGoalsMade() + " x " + away.getGoalsMade() + ")",
                        attacker.name.trim());
                ballIntoNet(homeAttacks, 0.42 + Math.random() * 0.16);   // in the net
                poss = 1 - poss;
                bx = 0.5; by = 0.5; trackTick();           // back to kickoff
            } else {
                double outcome = Math.random();
                if (outcome < 0.50) {
                    addEvent(displayMin, "SAVE", side, pick(SAVE_LINES), attacker.name.trim());
                    ballTravel(gx, 0.40 + Math.random() * 0.20, 1);   // shot on target
                    poss = 1 - poss;                       // keeper claims it
                    ballTravel(spotX, 0.5, 1);
                } else if (outcome < 0.85) {
                    addEvent(displayMin, "MISS", side, pick(MISS_LINES), attacker.name.trim());
                    ballTravel(gx, Math.random() < 0.5 ? 0.16 : 0.84, 1);  // dragged wide
                    poss = 1 - poss;
                    ballTravel(bxT, 0.5, 1);               // goal kick out
                } else {
                    addEvent(displayMin, "POST", side, pick(POST_LINES), attacker.name.trim());
                    ballTravel(gx, 0.46 + Math.random() * 0.08, 1);   // clangs the frame
                    poss = 1 - poss;
                    ballTravel(bxT, cy, 1);                // rebounds clear
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

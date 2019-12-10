package com.tournaments;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {

        String basePath = new File("").getAbsolutePath();

        String championshipCountry = "england.txt";

        Championship c = new Championship(basePath + "/databasefiles/" + championshipCountry, 2, true);
        c.generateMatches();
        c.playMatches(true);
        c.printResults();

        //Example for simulating a single match:
        //Team homeTeam = new Team("Team 1 name", 19);
        //Team awayTeam = new Team("Team 2 name", 14);
        //Match m = new Match(homeTeam, awayTeam);
        //m.play(false);

    }
}



package com.tournaments;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {

        String basePath = new File("").getAbsolutePath();

        // Default values
        String championshipCountry = "england.txt";
        boolean jsonMode = false;

        // Parse args: java Main [league_file] [--json]
        for (String arg : args) {
            if (arg.equals("--json")) {
                jsonMode = true;
            } else if (arg.endsWith(".txt")) {
                championshipCountry = arg;
            }
        }

        Championship c = new Championship(basePath + "/databasefiles/" + championshipCountry, 2, true);
        c.generateMatches();
        c.playMatches(false);

        if (jsonMode) {
            c.printJson();
        } else {
            c.printResults();
        }
    }
}

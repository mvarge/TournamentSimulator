# Tournament Simulator

This project is intended to be an experiment for random probabilities calculation between different levels of teams, as 
well as a demonstration over the [Round-Robin tournament algorithm](https://en.wikipedia.org/wiki/Round-robin_tournament), 
used to randomize matches between all teams within a given tournament.

## About

This is a project to simulate football matches and tournaments/championships, based on random probabilities extracted 
from a given team rating, plus a random amount of opportunities of goals within a match.

All of the probabilities are controlled by variables such as team form, results against bigger and/or lower rated teams, 
amount of goals scored or suffered, etc.

## Approach for Matches

The basic principle of a match are:

* The total number of goal opportunities in a match is a random choice between an integer Array
* For each goal opportunity:
  * A random Integer number is choose for each team, within a range of 0 as minimum and the team rating as a maximum
  * Whoever has the biggest number, scores a goal
  
Every team contains a rating and a morale setting, where the morale is variable thorough a single tournament and some 
decisions affect each of its behaviors.

## Round Robin Tournament Algorithm

In order to generate a double turn tournament and simulate correctly the matches objects through one single tournament, 
I used the [Round-Robin tournament algorithm](https://en.wikipedia.org/wiki/Round-robin_tournament) approach available 
on Wikipedia, where you can find a image description:

![Demonstration from Wiki](https://upload.wikimedia.org/wikipedia/commons/b/b7/Round-robin_tournament_10teams_en.png)

## How to use

ATM please refer to _Main class_

## More info

TBD

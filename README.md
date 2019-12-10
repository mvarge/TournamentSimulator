# Tournament Simulator

This project is intended to be an experiment for random probabilities calculation between different levels of teams, as 
well as a demonstration over the [Round-Robin tournament algorithm](https://en.wikipedia.org/wiki/Round-robin_tournament), 
used to randomize matches between all teams within a given tournament.

## What is it?

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

This way every team is paired in the most optimistic way to play one round at home, followed by another round as away, 
and to follow the same pairings on the second turn but in a reserved basis.

## How to use

### Team table

Currently the teams are populated from a `.txt` file inside the `databasefiles` folder. Every line is a team description 
and the format is `Team Name,Rating`, given that `Team Name`should be a `String` and `Rating` an `Integer` between 0 
and 20. A file example as it follows:

```
Valencia,14
Man City,18
Genk,7
APOEL,5
``` 

### Tournament and Matches

The _Main_ class currently holds two of the same examples as described below.

#### Tournament

For creating a whole new Tournament for `england.txt` and playing it:

```$xslt
        String championshipCountry = "england.txt";
        Championship c = new Championship(basePath + "/databasefiles/" + championshipCountry, 2, true);
        c.generateMatches();
        c.playMatches(true);
```

In this case you will simulate round after round and your end result should be something similar as:

```
Team	Pts	P	W	D	L	GF	GA	GD	M	R	Df	FORM
Arsenal	80	38	24	8	6	80	37	43	16	20	4	[W, W, W, L, W, D, D, W, W, D, L, W, W, L, W, W, W, L, D, W, W, W, L, L, W, W, D, W, D, W, W, W, W, D, W, W, D, W]
Liverpo	79	38	25	4	9	77	27	50	19	17	-2	[L, W, W, L, W, W, L, W, W, W, W, W, W, L, W, L, W, W, L, W, D, W, D, D, W, W, W, W, L, L, W, W, W, W, L, W, D, W]
Man Cit	76	38	22	10	6	70	35	35	18	20	2	[W, W, D, W, L, D, W, W, D, W, W, L, W, L, L, W, W, L, W, W, W, W, D, W, D, W, W, W, W, D, D, W, D, L, W, D, D, W]
Bolton	75	38	22	9	7	72	34	38	16	20	4	[L, D, L, W, D, D, W, L, L, W, D, W, W, W, W, D, W, W, W, W, W, W, L, W, W, L, D, L, W, W, D, W, D, W, W, W, D, W]
Chelsea	75	38	21	12	5	80	31	49	18	17	-1	[W, D, W, W, W, D, D, W, W, W, W, L, W, D, D, W, L, W, D, W, L, W, W, W, D, D, W, W, W, D, W, D, W, D, W, L, D, L]
Southam	70	38	21	7	10	61	35	26	13	15	2	[W, W, W, W, W, W, W, D, L, W, W, W, W, L, L, D, L, L, D, W, W, W, W, W, W, W, L, W, D, L, D, D, L, L, W, L, D, W]
Leicest	61	38	17	10	11	58	40	18	12	13	1	[D, W, W, D, L, D, D, L, L, W, D, L, L, W, W, D, W, W, D, W, W, W, L, D, L, D, W, D, W, L, W, L, W, W, L, W, L, W]
Man Utd	57	38	16	9	13	62	57	5	12	13	1	[L, W, L, W, L, D, W, D, W, L, L, W, W, L, W, L, D, W, D, W, W, L, W, L, D, L, W, L, D, D, L, L, D, W, D, W, W, W]
Reading	57	38	15	12	11	52	36	16	7	13	6	[W, D, L, L, W, W, D, W, L, D, L, D, D, L, D, W, L, L, W, L, W, L, W, W, L, W, W, L, D, D, W, D, W, W, D, D, W, D]
Sunderl	54	38	15	9	14	61	48	13	13	15	2	[W, L, L, L, W, W, L, D, L, L, D, D, L, D, D, D, W, L, W, L, D, L, L, D, W, W, W, W, L, W, W, L, W, L, D, W, W, W]
Watford	49	38	13	10	15	48	52	-4	8	9	1	[L, D, D, W, L, W, W, W, L, L, W, W, D, D, D, D, D, W, D, W, L, W, L, L, W, W, L, L, W, L, L, W, L, L, D, L, D, L]
Wolverh	48	38	13	9	16	33	47	-14	11	11	0	[W, D, W, W, D, L, D, L, W, W, W, D, L, W, L, D, L, L, L, L, W, D, D, W, L, L, L, L, W, W, L, W, D, L, L, L, W, D]
Aston V	46	38	12	10	16	47	44	3	10	10	0	[L, L, D, D, D, D, W, L, W, W, D, D, L, W, L, L, W, D, W, W, L, L, W, L, L, L, L, W, W, W, W, D, L, D, L, D, L, L]
Everton	44	38	12	8	18	33	39	-6	7	9	2	[L, D, W, L, L, L, L, L, W, L, D, W, D, D, L, W, D, D, W, L, W, W, L, L, L, W, L, D, L, L, W, D, W, L, W, L, W, L]
QPR   	42	38	11	9	18	34	54	-20	9	7	-2	[W, L, L, L, D, W, D, L, L, L, L, D, L, W, D, W, D, D, L, L, L, L, D, L, W, L, W, D, W, W, L, L, L, D, W, W, W, L]
Burnley	39	38	10	9	19	39	72	-33	8	7	-1	[W, L, L, W, W, L, W, D, D, L, L, L, W, W, D, W, L, D, L, L, L, L, D, W, L, L, L, L, L, L, D, D, L, W, D, D, L, W]
Tottenh	31	38	8	7	23	31	76	-45	11	5	-6	[L, L, L, L, L, L, L, L, W, W, D, L, L, D, D, L, W, W, D, L, L, D, W, W, W, L, D, L, L, L, L, L, L, W, L, D, L, L]
West Br	30	38	7	9	22	24	67	-43	8	5	-3	[D, D, W, D, L, L, L, W, W, L, L, L, L, D, W, L, L, L, L, L, L, L, W, L, L, L, D, D, L, W, L, D, D, W, D, L, L, L]
Sheffie	24	38	4	12	22	32	79	-47	7	4	-3	[L, L, L, L, L, D, L, W, L, L, W, D, D, D, D, L, L, L, D, L, L, L, D, D, D, L, L, W, L, W, L, L, D, L, L, D, D, L]
Notting	14	38	3	5	30	13	97	-84	7	3	-4	[L, D, D, D, W, L, L, L, L, L, L, L, L, D, L, L, L, W, L, L, L, L, L, L, L, W, L, L, L, L, L, D, L, L, L, L, L, L]
```

Where every column description is as it follows:

| Attribute| Description | 
| ---------| ----------- |
| Team | Team name as configured on your `.txt` championship file |
| Pts | Total amount of points |
| W | Total wins |
| D | Total draws |
| L | Total losses|
| GF | Goals in favour |
| GA | Goals taken/against |
| GD | Goal difference (GF - GA) |
| M | Team morale | 
| R | Team rating |
| Df | Difference between initial morale against final rating |
| FORM | Complete record of each team form |

#### Single Match

For playing a single game you can simply do as like:

```
        Team homeTeam = new Team("Team 1 name", 19);
        Team awayTeam = new Team("Team 2 name", 14);
        Match m = new Match(homeTeam, awayTeam);
        m.play(false); //No homeFactor for this match since false as method param
```

## Author

Marcelo Marques S. Varge

_marcelo dot varge at gmail_

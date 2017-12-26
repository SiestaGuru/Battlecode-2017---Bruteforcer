package bruteforcer;

import battlecode.common.*;

/**
 * Created by Hermen on 10/1/2017.
 */
public class Soldier extends RobotBase {


    static MapLocation prevLoc = null;
    static MapLocation prevLoc1 = null;
    static MapLocation prevLoc2 = null;
    static MapLocation prevLoc3 = null;


    static int turnsStuck = 0;
    static int moveRandomlyTurns = 0;

    static float stuckX = -1;
    static float stuckY = -1;

    static int directionDecisionCooldown = 0;

    static boolean rightWardsAroundOwnUnits = false;

    static boolean masscombat = false;

    static int turnsNotStuckToTree = 0;

    static MapLocation cluster1 = null;
    static MapLocation cluster2 = null;
    static MapLocation cluster3 = null;

    static MapLocation guardJob = null;
    static int turnObtainedJob = -1;
    static int treesDetected = 0;

    static RobotInfo duelTarget = null;
    static RobotInfo lastDuelTarget = null;
    static boolean duelTargetStandingStill = false;

    static int lastAlternationShot = 0; //0 = middle, -1 = left, 1= right


    static int aggrMomentum = 0;
    static int defMomentum = 0;

    static int soldirBuddies = 0;


    static int bytesUsed = 0;
    static int zonesFound = 0;

    static TreeInfo nukeThisTree = null;
    static TreeInfo nukeThisTreeLast = null;


    static boolean isAssasin = false;

    static RobotInfo tankLastTurn = null;
    static Direction lastBugDirection = null;
    boolean extremeEconMode = false;

    public void step() throws Exception {
        directionDecisionCooldown--;
        treesDetected = 0;
        soldirBuddies = 0;
        duelTarget = null;
        duelTargetStandingStill = false;
        MapLocation myLocation = RobotBase.myLocation;
        RobotController rc = RobotBase.rc;
        RobotInfo[] robots = rc.senseNearbyRobots();


        turnsNotStuckToTree++;
        masscombat = false;


        int agression = 4;
        int defensive = 2;




        boolean shouldBroadcastThings = false;

        if (turn % 3 == 0  && !isAssasin) {
            shouldBroadcastThings = true;
        }

        if (turnsStuck > 15 && isAssasin) {
            isAssasin = false;
        }

        int DefMainX = rc.readBroadcast(Archon.MAIN_DEFENSIVE_TARGET_X);
        MapLocation defTarget = null;
        boolean closeToDefTarget = false;

        if (DefMainX >= 0) {
            defTarget = new MapLocation(DefMainX, rc.readBroadcast(Archon.MAIN_DEFENSIVE_TARGET_Y));

            closeToDefTarget = myLocation.distanceTo(defTarget) < 15;
        }


        BulletInfo[] bullets = rc.senseNearbyBullets(7.5f);
        dodgeBullets(bullets);

        if (bullets.length * 0.4f + (robots.length * 3) > 24) {
            masscombat = true; //Save all resources we can to properly dodge. Made basically obsolete after change to 15000 bytecodes, but still useful in extreme circumstances
//             rc.setIndicatorDot(myLocation,255,0,0);
        }

        if (turn % 3 == 0) {
            enemyArchonCount = 0;
            if (turn - rc.readBroadcast(THEIR_ARCHON_1_LASTSEEN) < 50) {
                enemyArchonCount++;
            }
            if (turn - rc.readBroadcast(THEIR_ARCHON_2_LASTSEEN) < 50) {
                enemyArchonCount++;
            }
            if (turn - rc.readBroadcast(THEIR_ARCHON_3_LASTSEEN) < 50) {
                enemyArchonCount++;
            }
        }


        cluster1 = null;
        cluster2 = null;
        cluster3 = null;

        if (turn - rc.readBroadcast(CLUSTER1_TURN) < 2) {
            cluster1 = new MapLocation(((float) rc.readBroadcast(CLUSTER1_X)) / 10f, ((float) rc.readBroadcast(CLUSTER1_Y)) / 10f);

            float dist = myLocation.distanceTo(cluster1);
            if (dist < 15) {
                //Chase this group
                addVector(cluster1, 2);
//              rc.setIndicatorLine(myLocation,cluster1,0,255,0);

            }
        }
        if (turn - rc.readBroadcast(CLUSTER2_TURN) < 2) {
            cluster2 = new MapLocation(((float) rc.readBroadcast(CLUSTER2_X)) / 10f, ((float) rc.readBroadcast(CLUSTER2_Y)) / 10f);

            float dist = myLocation.distanceTo(cluster2);
            if (dist < 15) {
                //Chase this group
                addVector(cluster2, 2);
//              rc.setIndicatorLine(myLocation,cluster2,0,0,255);
            }
        }
        if (turn - rc.readBroadcast(CLUSTER3_TURN) < 2) {
            cluster3 = new MapLocation(((float) rc.readBroadcast(CLUSTER3_X)) / 10f, ((float) rc.readBroadcast(CLUSTER3_Y)) / 10f);

            float dist = myLocation.distanceTo(cluster3);
            if (dist < 15) {
                //Chase this group
                addVector(cluster3, 2);
//              rc.setIndicatorLine(myLocation,cluster3,255,0,0);
            }
        }


        boolean enemyGardenerDetected = false;
        RobotInfo firstEnemyGardenerDetected = null;

        int totalEnemies = 0;
        int totalAllies = 0;

        float alliedTeamStrength = rc.getHealth() / 30;
        float enemyTeamStrength = 0;

        boolean fireBeforeMove = false;

        boolean shouldConfirmEnemyLoc = false;
        boolean shouldConfirmEnemyTreeLoc = false;

        MapLocation friendlyarchon1 = null;
        MapLocation friendlyarchon2 = null;
        MapLocation friendlyarchon3 = null;
        int lastAggrUpdate = -1;
        int lastStratUpdate = -1;

        if (!masscombat) {
            becomeCommanderIfNeccessary();

            lastAggrUpdate = turn - rc.readBroadcast(AGRESSIVE_TARGETS_LAST_UPDATE);
            lastStratUpdate = turn - rc.readBroadcast(LAST_UPDATED_GRAND_STRAT);

            if (lastStratUpdate < 10) {
                agression = rc.readBroadcast(AGRESSION_LEVEL) + aggrMomentum;
                defensive = rc.readBroadcast(DEFENSE_LEVEL) + defMomentum;
            }

            if (rc.readBroadcast(CONFIRMED_ENEMY_TURN) < turn) {
                shouldConfirmEnemyLoc = true;
            }
            if (rc.readBroadcast(CONFIRMED_ENEMY_TREE_TURN) < turn) {
                shouldConfirmEnemyTreeLoc = true;
            }

            if (turn - rc.readBroadcast(OUR_ARCHON_1_LASTSEEN) < 5) {
                friendlyarchon1 = new MapLocation(rc.readBroadcast(OUR_ARCHON_1_X), rc.readBroadcast(OUR_ARCHON_1_Y));

                if (myLocation.distanceTo(friendlyarchon1) > 10) {
                    friendlyarchon1 = null;
                }
            }
            if (turn - rc.readBroadcast(OUR_ARCHON_2_LASTSEEN) < 5) {
                friendlyarchon2 = new MapLocation(rc.readBroadcast(OUR_ARCHON_2_X), rc.readBroadcast(OUR_ARCHON_3_Y));

                if (myLocation.distanceTo(friendlyarchon2) > 10) {
                    friendlyarchon2 = null;
                }
            }
            if (turn - rc.readBroadcast(OUR_ARCHON_3_LASTSEEN) < 5) {
                friendlyarchon3 = new MapLocation(rc.readBroadcast(OUR_ARCHON_2_X), rc.readBroadcast(OUR_ARCHON_3_Y));
                if (myLocation.distanceTo(friendlyarchon3) > 10) {
                    friendlyarchon3 = null;
                }
            }

            if (guardJob == null && !isAssasin) {
                if (turn - rc.readBroadcast(GUARD_JOB_TURN) < 10) {
                    MapLocation guardSpot = new MapLocation(rc.readBroadcast(GUARD_JOB_X), rc.readBroadcast(GUARD_JOB_Y));
                    if (myLocation.distanceTo(guardSpot) < 15) {
                        //ok take the job
                        guardJob = guardSpot;
                        turnObtainedJob = turn;
                        rc.broadcast(GUARD_JOB_TURN, -1);
                    }
                }
            }
            if (guardJob != null) {
                if (turn - turnObtainedJob > 30) {
                    guardJob = null;
                } else if (myLocation.distanceTo(guardJob) > 15) {
                    guardJob = null;
                }
            }

            //Harder to dodge around edges
            desireFarEdges(-20, 3);
            desireFarEdges(-10, 6);
        }


        int AggrMainX = rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_X);
        int AggrMainY = -1;

        MapLocation mainAggrGoal = null;

        if (lastAggrUpdate < 10 && AggrMainX > 0) {
            AggrMainY = rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_Y);
            mainAggrGoal = new MapLocation(AggrMainX, AggrMainY);
            //Rotate it randomly by some degrees
            mainAggrGoal = myLocation.add(myLocation.directionTo(mainAggrGoal).rotateLeftDegrees(-15 + (float) Math.random() * 30), myLocation.distanceTo(mainAggrGoal));
        }

        int closeRelevantUnits = 0;
        int scaryUnitCount = 0;

        RobotInfo tankFound = null;

        RobotInfo friendlyGardener = null;
        boolean foundVeryCloseFriendlyGardener = false;
        float totalEnemyX = 0;
        float totalEnemyY = 0;
        if (!masscombat) {


            //Friendly robots
            for (int i = 0; i < robots.length; i++) {
                RobotInfo r = robots[i];
                if (r.team.equals(ally)) {
                    MapLocation loc = r.location;
                    double health = r.health;
                    float distance = myLocation.distanceTo(loc);


                    if (distance < 3.5f + r.getRadius()) {
                        addVectorSqrt(r.location, -0.8f);
                    }

                    switch (r.type) {
                        case SOLDIER:
                            alliedTeamStrength += health / 30;
                            if (distance < 6) {
                                soldirBuddies++;
                                if (distance < 4.5f) {

                                    if(distance < 3.5) {
                                        addDesireZone(loc, 3f, -30);
                                        if (distance < 2.5f) {

                                            //Makes them move around one another a little more smoothly

                                            if (directionLeaning == -1 || directionDecisionCooldown <= 0) {
                                                if (rightWardsAroundOwnUnits) {
                                                    addVector(myLocation.add(myLocation.directionTo(loc).rotateRightDegrees(90), 1), 3f);
                                                } else {
                                                    addVector(myLocation.add(myLocation.directionTo(loc).rotateLeftDegrees(90), 1), 3f);
                                                }
                                            } else {
                                                if (directionLeaning == 0) {
                                                    addVector(myLocation.add(myLocation.directionTo(loc).rotateLeftDegrees(90), 1), 3f);
                                                } else {
                                                    addVector(myLocation.add(myLocation.directionTo(loc).rotateRightDegrees(90), 1), 3f);
                                                }
                                            }

                                        }
                                    }else{
                                        addDesireZone(loc, 3.5f, -20);
                                    }
                                } else {
                                    addDesireZone(loc, 4.5f, -7);
                                }
                            }
                            break;
                        case LUMBERJACK:
                            alliedTeamStrength += health / 50;
                            if (distance < 4.5f) {

                                addVectorSqrt(loc, -0.5f);

                                if (distance < 4.71) {
                                    addDesireZone(loc, 3.76f, -20);



                                    if (distance < 3.96) {
                                        addCircularDesireZone(loc, 3.01f, -60);

                                        if(directionLeaning == -1 || directionDecisionCooldown <= 0){
                                            if (rightWardsAroundOwnUnits) {
                                                addVector(myLocation.add(myLocation.directionTo(loc).rotateRightDegrees(90), 1), 5f);
                                            } else {
                                                addVector(myLocation.add(myLocation.directionTo(loc).rotateLeftDegrees(90), 1), 5f);
                                            }
                                        }else{
                                            if(directionLeaning == 0){
                                                addVector(myLocation.add(myLocation.directionTo(loc).rotateLeftDegrees(90), 1), 5f);
                                            } else {
                                                addVector(myLocation.add(myLocation.directionTo(loc).rotateRightDegrees(90), 1), 5f);
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        case TANK:

                            if (mainAggrGoal != null) {
                                //Try and get out of face of tank so it can fire
                                MapLocation avoid = loc.add(loc.directionTo(mainAggrGoal), 5);
                                addDesireZone(avoid, 5, -15);
                                addVector(avoid, -4);
                            }

                            alliedTeamStrength += health / 20;
                            break;
                        case ARCHON:
                            if (distance < 4.71) {
                                addDesireZone(loc, 3.76f, -10);
                                if (rightWardsAroundOwnUnits) {
                                    addVector(myLocation.add(myLocation.directionTo(loc).rotateRightDegrees(90), 1), 1.5f);
                                } else {
                                    addVector(myLocation.add(myLocation.directionTo(loc).rotateLeftDegrees(90), 1), 1.5f);
                                }
                            }
                            break;
                        case GARDENER:
                            if(friendlyGardener == null){
                                friendlyGardener = r;
                            }
                            if(distance < 2.3f){
                                foundVeryCloseFriendlyGardener = true;
                            }
                            break;
                    }
                }
            }


            //Enemy robots
            for (int i = 0; i < robots.length; i++) {

                RobotInfo r = robots[i];

                if (r.team.equals(enemy)) {
                    MapLocation loc = r.location;
                    double health = r.health;
                    float distance = myLocation.distanceTo(loc);
                    totalEnemies++;
                    totalEnemyX += loc.x;
                    totalEnemyY += loc.y;

                    if (distance < 2.5f + r.getRadius()) {
                        if (!r.type.equals(RobotType.SOLDIER) && !r.type.equals(RobotType.ARCHON)) {
                            closeRelevantUnits++;
                        }
                        if (distance < 1.05f + r.getRadius()) {
                            fireBeforeMove = true;
                        }
                    }


                    switch (r.type) {
                        case SOLDIER:
                            scaryUnitCount++;
                            if (duelTarget == null) {
                                if(!rc.isLocationOccupied(myLocation.add(myLocation.directionTo(loc), distance/2))) {
                                    duelTarget = r;
                                }
                            }
                            if (lastDuelTarget != null) {
                                if (lastDuelTarget.ID == r.ID) {
                                    if (distance < 5) {
                                        if(!rc.isLocationOccupied(myLocation.add(myLocation.directionTo(loc), distance/2))) {
                                            duelTarget = r;//Prefer consistently picking the same duel partner. Don't however if this new guy is too close
                                        }
                                    }
                                }
                                if (lastDuelTarget.ID == r.ID) {
                                    if (lastDuelTarget.health + 0.01f < r.health) {
                                        duelTargetStandingStill = true;
                                        //It's being build! quick run in
                                        addVector(loc, 13);
                                        MapLocation p1 = loc.add(loc.directionTo(myLocation), 2.04f);
                                        if (myLocation.distanceTo(p1) <= maxMove && rc.canMove(p1)) {
                                            addSpecialMapLocation(p1, 20);
                                            fireBeforeMove = false;
                                        }
                                    } else if (lastDuelTarget.location.distanceTo(loc) < 0.05f) {
                                        //Probably healing, or at least standing still
                                        duelTargetStandingStill = true;
                                    }

                                }
                            }
                            enemyTeamStrength += health / 30;
                            if (rc.getHealth() < 26 || r.getHealth() - rc.getHealth() > 20) {
                                addVector(loc, -4); //Let's trust our ability to be useful later over our ability to be useful now
                            }

                            if(friendlyGardener != null && friendlyGardener.location.distanceTo(loc) < 5) {
                                addVector(loc, 20);
                                if (distance < 4.8) {
                                    addCircularDesireZone(loc, 4, 150);
                                }
                            } else if(closeToDefTarget){
                                addVector(loc, 15);
                                if(distance < 4.8) {
                                    addCircularDesireZone(loc, 4, 70);
                                }
                            }




                            if (distance < 7) {
                                addCircularDesireZone(loc, 5.4f, -60);

                                if (distance > 3.05 && distance < 5.06) {
                                    //In this area, a perpendicular movement may be one of the best, as it can allow some bullet dodge

                                    addSpecialMapLocation(myLocation.add(myLocation.directionTo(loc).rotateLeftDegrees(90), maxMove), 10);
                                    addSpecialMapLocation(myLocation.add(myLocation.directionTo(loc).rotateRightDegrees(90), maxMove), 10);
                                    fireBeforeMove = true;

                                }
                            }
                            if (distance >= 5.06) {
                                //These are spots that will bring the player, assuming a near stationary enemy, to neat gaps between triad shots
                                //The real neat spots are at rotations of 10 degrees. But since we have 3 moves before bullets hit us, we'll take a third
                                //The spot appears at spots above ~5.8 distance between us and enemy, with the actual spot at about ~5.84 from enemy, but
                                //I added a bit to be safe, since we have to account for enemy movement
                                float spotDistance = 5.9f;
                                if (distance > spotDistance) spotDistance = distance;
                                MapLocation m1 = loc.add(loc.directionTo(myLocation).rotateLeftDegrees(3.4f), spotDistance);
                                MapLocation m2 = loc.add(loc.directionTo(myLocation).rotateRightDegrees(3.4f), spotDistance);

                                if (myLocation.distanceTo(m1) < maxMove) {
                                    addSpecialMapLocation(m1, 15);
                                    // rc.setIndicatorDot(m1,0,255,0);
                                }
                                if (myLocation.distanceTo(m2) < maxMove) {
                                    addSpecialMapLocation(m2, 15);
                                    //   rc.setIndicatorDot(m2,0,255,0);
                                }

                            }

                            if (rc.getHealth() >= r.health && r.moveCount == 0) {
                                if (distance < 2.99) {


                                    //Do a pulsing type of behavior to confuse some bots. If were in insta range, fire first then moe out.
                                    //If were outside of that range, move in first, then fire.
                                    if (distance < 2.05f) {
                                        MapLocation p1 = myLocation.add(loc.directionTo(myLocation), maxMove);
                                        if (rc.canMove(p1)) {
                                            addSpecialMapLocation(p1, 30);
                                            fireBeforeMove = true;
                                        }
                                    } else {
                                        MapLocation p1 = loc.add(loc.directionTo(myLocation), 2.04f);
                                        //We reeally like chasing these
                                        if (myLocation.distanceTo(p1) <= maxMove && rc.canMove(p1)) {
                                            addSpecialMapLocation(p1, 20);
                                            fireBeforeMove = false;
                                        }
                                    }
                                }
                            } else {
                                if (distance < 3.51) {
                                    addDesireZone(loc, 2.56f, -10);
                                }
                            }


                            if (friendlyarchon1 != null) {
                                if (friendlyarchon1.distanceTo(loc) < 6) {
                                    addVector(loc, 5); //Oh no, they're attacking our archon.. Quick, to the rescue!
                                }
                            }
                            if (friendlyarchon2 != null) {
                                if (friendlyarchon2.distanceTo(loc) < 6) {
                                    addVector(loc, 5); //Oh no, they're attacking our archon.. Quick, to the rescue!
                                }
                            }
                            if (friendlyarchon3 != null) {
                                if (friendlyarchon3.distanceTo(loc) < 6) {
                                    addVector(loc, 5); //Oh no, they're attacking our archon.. Quick, to the rescue!
                                }
                            }

                            break;
                        case LUMBERJACK:
                            scaryUnitCount++;
                            enemyTeamStrength += health / 50;


                            boolean haveToStepIn = false;

                            if (friendlyarchon1 != null) {
                                if (friendlyarchon1.distanceTo(loc) < 6) {
                                    haveToStepIn = true;
                                }
                            }
                            if (friendlyarchon2 != null) {
                                if (friendlyarchon2.distanceTo(loc) < 6) {
                                    haveToStepIn = true;
                                }
                            }
                            if (friendlyarchon3 != null) {
                                if (friendlyarchon3.distanceTo(loc) < 6) {
                                    haveToStepIn = true;
                                }
                            }

                            if ((haveToStepIn && rc.getHealth() > 20) || rc.getHealth() - r.getHealth() > 60) {
                                //It can be very hard to hit lumberjacks, especially if our archon is behind them. So if conditions are favorable, move in and mow them down instead of playing the defensive game
                                addVector(loc, 3);
                                if (distance < 2.99) {
                                    MapLocation p1 = loc.add(loc.directionTo(myLocation), 2.04f);
                                    //Cheap version of assasin spot thing. Could probably do the expensive one here too, but will see if suffices
                                    if (myLocation.distanceTo(p1) <= maxMove && rc.canMove(p1)) {
                                        addSpecialMapLocation(p1, 30);
                                        fireBeforeMove = false;
                                    }
                                }
                            }
                            else if(closeToDefTarget) {
                                addVector(loc, 10);
                                if (distance < 4.71) {
                                    addCircularDesireZone(loc, 3.76f, -150f); //Is this the correct amount? 1 lumberjack radius, 1 strike radius, 0.75 lumber movement, 1 soldier size
                                }
                            }else {
                                if (distance < 4.71) {
                                    fireBeforeMove = true;
                                    addCircularDesireZone(loc, 3.76f, -100f); //Is this the correct amount? 1 lumberjack radius, 1 strike radius, 0.75 lumber movement, 1 soldier size
                                    if (zoneLoc < 10) {
                                        addDesireZone(loc, 2.1f, -10f); //Same without movement so that we at least have some differences between spots
                                    }
                                }
                                addVector(loc, -3);
                            }

                            break;

                        case TANK:
                            scaryUnitCount++;
                            enemyTeamStrength += health / 20;

                            if (rc.getHealth() < 20) {
                                addVector(loc, -4); //Let's trust our ability to be useful later over our ability to be useful now
                            }


                            if (distance < 8.25) {
                                addVector(loc, -25);
                            } else if (distance > 8.5f) {
                                addVector(loc, 10 + agression * 2);
                                fireBeforeMove = false;//dont want to have to dodge our own shots and then not be able to stay in range

                                float move = distance - 8.26f;

                                if (move > maxMove) {
                                    move = maxMove;
                                }
                                MapLocation p1 = myLocation.add(myLocation.directionTo(loc), move);
                                if (myLocation.distanceTo(p1) <= maxMove && rc.canMove(p1)) {
                                    addSpecialMapLocation(p1, 40);
                                }
                            } else {
                                addVector(loc, 7 + agression * 2);

                                float move = distance - 8.26f;

                                if (move > maxMove) {
                                    move = maxMove;
                                }
                                MapLocation p1 = myLocation.add(myLocation.directionTo(loc), move);
                                if (myLocation.distanceTo(p1) <= maxMove && rc.canMove(p1)) {
                                    addSpecialMapLocation(p1, 40);
                                }
                            }
                            addCircularDesireZone(loc, 8.26f, -400); // Stay just out of vision range of this thing. We can see it because it's bigger, it can't see us
                            addCircularDesireZone(loc, 8.5f, 200); //But keep stalking it so that it cant run. Putting this at 8.5 instead of 8.74 because we sometimes have to dodge our own bullets
                            addCircularDesireZone(loc, 9f, -80); //Don't want to run out of range


                            if (rc.readBroadcast(CONFIRMED_ENEMY_TANK_TURN) != turn) {
                                rc.broadcast(CONFIRMED_ENEMY_TANK_X, (int) (robots[i].location.x * 10));
                                rc.broadcast(CONFIRMED_ENEMY_TANK_Y, (int) (robots[i].location.y * 10));
                                rc.broadcast(CONFIRMED_ENEMY_TANK_TURN, turn);
                            }
                            break;

                        case ARCHON:

                            if (r.getHealth() < 120) {
                                addVector(loc, 4); //Try to finish this off
                            } else {

                                if ((turn > 1000 && initialEnemyArchonCount == 3) || (turn > 500 && initialEnemyArchonCount == 2) || (turn > 250 && initialEnemyArchonCount == 1)) {
                                    addVector(loc, 3);
                                    addSidewaysVector(loc, 2);

                                    if (specialMapLocationsCount == 0) {
                                        if (distance < 3.98) {

                                            Direction dir = loc.directionTo(myLocation);
                                            MapLocation p1 = loc.add(dir, 3.04f);
                                            //Cheap version of assasin spot thing. Could probably do the expensive one here too, but will see if suffices
                                            if (myLocation.distanceTo(p1) < maxMove && rc.canMove(p1)) {
                                                addSpecialMapLocation(p1, 4);
                                                fireBeforeMove = false;
                                            }

                                            if (distance < 3.2) {
                                                //Also add rotational points to make it easier to get around these to find gardeners etc
                                                MapLocation p2 = loc.add(dir.rotateLeftDegrees(5), 3.04f);
                                                if (myLocation.distanceTo(p2) < maxMove && rc.canMove(p2)) {
                                                    addSpecialMapLocation(p2, 6);
                                                }

                                                MapLocation p3 = loc.add(dir.rotateRightDegrees(5), 3.04f);
                                                if (myLocation.distanceTo(p3) < maxMove && rc.canMove(p3)) {
                                                    addSpecialMapLocation(p3, 6);
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    addSidewaysVector(loc, 5);
                                }
                            }
                            enemyTeamStrength -= 5;
                                broadcastEnemyArchon(r.ID, (int) loc.x, (int) loc.y);
                                if (lastAggrUpdate >= 10) {
                                    //well, if the archon ain't doing it
                                    rc.broadcast(MAIN_AGRESSIVE_TARGET_X, (int) loc.x);
                                    rc.broadcast(MAIN_AGRESSIVE_TARGET_Y, (int) loc.y);
                                    rc.broadcast(AGRESSIVE_TARGETS_LAST_UPDATE, turn);
//                                    rc.setIndicatorDot(myLocation,100,155,55);
                                }

                            if (distance < 4.46) {
                                addDesireZone(loc, 3.5f, 4);
                            }

                            break;
                        case GARDENER:
                            enemyTeamStrength -= 5;
                            //Override the confirmed enemy if it's a gardener, since that's a more valuable target
                            rc.broadcast(CONFIRMED_ENEMY_X, (int) loc.x);
                            rc.broadcast(CONFIRMED_ENEMY_Y, (int) loc.y);
                            rc.broadcast(CONFIRMED_ENEMY_TURN, turn);
                            shouldConfirmEnemyLoc = false;

                            enemyGardenerDetected = true;

                            if (firstEnemyGardenerDetected == null) {
                                firstEnemyGardenerDetected = r;
                            }

                            if (turnsStuck > 3) {
                                if (turn < 350 || mapSizeType <= MEDIUM) {
                                    addVector(loc, 4);
                                    addSidewaysVector(loc, 4);
                                } else {
                                    addVector(loc, 2);
                                    addSidewaysVector(loc, 4);
                                }
                            } else {
                                if (turn < 350 || mapSizeType <= MEDIUM) {
                                    addVector(loc, 17);
                                } else {
                                    addVector(loc, 11);
                                }
                            }


                            if (distance < 3.95) {
                                addCircularDesireZone(loc, 3f, 40);
                                if (distance < 2.99) {
                                    MapLocation p1 = loc.add(loc.directionTo(myLocation), 2.04f);
                                    //Cheap version of assassin spot thing. Could probably do the expensive one here too, but will see if suffices
                                    if (myLocation.distanceTo(p1) <= maxMove && rc.canMove(p1)) {
                                        addSpecialMapLocation(p1, 20);
                                        fireBeforeMove = false;
                                    }
                                }
                            } else {
                                MapLocation p1 = myLocation.add(myLocation.directionTo(loc), maxMove);
                                //Move in closer
                                if (rc.canMove(p1)) {
                                    addSpecialMapLocation(p1, 20);
                                    fireBeforeMove = false;
                                }
                            }

                            if (distance > 1.4f && distance < 3f) {
                                fireBeforeMove = false; //Give us space to walk in range
                            }

                            if (rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_TURN) != turn) {
                                rc.broadcast(CONFIRMED_ENEMY_GARDENER_X, (int) (robots[i].location.x * 100));
                                rc.broadcast(CONFIRMED_ENEMY_GARDENER_Y, (int) (robots[i].location.y * 100));
                                rc.broadcast(CONFIRMED_ENEMY_GARDENER_TURN, turn);
                            }
                            break;
                        case SCOUT:

                            boolean rushin = distance <= 2.99;

                            if(!rushin && friendlyGardener != null){
                                if(loc.distanceTo(friendlyGardener.location) < 4){
                                    rushin = true;
                                }
                            }

                            if (rushin) {
                                MapLocation p1 = loc.add(loc.directionTo(myLocation), 2.04f);
                                //Cheap version of assasin spot thing. Could probably do the expensive one here too, but will see if suffices
                                if (myLocation.distanceTo(p1) <= maxMove && rc.canMove(p1)) {
                                    addSpecialMapLocation(p1, 50);
                                    fireBeforeMove = false;
                                }
                                addVector(loc, 10);
                            }
                            if (distance > 2.4f && distance < 4f) {
                                fireBeforeMove = false; //Give us space to step in close enough to hit it
                                addVector(loc, 8);
                            } else if (distance < 6) {
                                addVector(loc, 5);
                            }

                            break;
                    }
                    if (shouldConfirmEnemyLoc && !r.type.equals(RobotType.SCOUT)) {
                        rc.broadcast(CONFIRMED_ENEMY_X, (int) loc.x);
                        rc.broadcast(CONFIRMED_ENEMY_Y, (int) loc.y);
                        rc.broadcast(CONFIRMED_ENEMY_TURN, turn);
                    }
                }
            }

            //This bit makes us advance to the enemy or flee if we feel outnumbered
            if (totalEnemies > 0) {
                float repelStr;
                if ((enemyTeamStrength + (defensive / 2)) * 1.1f > (alliedTeamStrength + (agression * 2)) || isAssasin || extremeEconMode) {
                    repelStr = -5;// / totalEnemies; //flee

                } else {
                    repelStr = 5;// / totalEnemies; //advance
                }
                addVector(totalEnemyX / (float) totalEnemies, totalEnemyY / (float) totalEnemies, repelStr); //Aimed at the average position of enemies
            }


        } else {
            for (int i = 0; i < robots.length && i < 8; i++) {
                RobotInfo r = robots[i];
                MapLocation loc = r.location;
                if (r.team.equals(enemy)) {
                    totalEnemies++;
                    totalEnemyX += loc.x;
                    totalEnemyY += loc.y;
                    float dist = loc.distanceTo(myLocation);
                    switch (r.type) {
                        case LUMBERJACK:
                            if (dist < 4.71f) {
                                addCircularDesireZone(loc, 3.76f, -40f);
                                if (zoneLoc < 10) {
                                    addDesireZone(loc, 3.01f, -20f);
                                }
                            }
                            break;
                        case ARCHON:
                            addVector(loc, 1);
                            break;
                        case GARDENER:
                            addVector(loc, 4);
                            break;
                        case TANK:
                            float distance = loc.distanceTo(myLocation);
                            if (distance < 8.25) {
                                addVector(loc, -25);
                            } else if (distance > 8.5f) {
                                addVector(loc, 9);
                            }
                            addCircularDesireZone(loc, 8.26f, -400); // Stay just out of vision range of this thing. We can see it because it's bigger, it can't see us
                            addCircularDesireZone(loc, 8.74f, 60); //But keep stalking it so that it cant run
                            break;
                    }

                    if (dist < 3f) {
                        fireBeforeMove = true;
                    }
                } else {
                    switch (r.type) {
                        case SOLDIER:
                            if (loc.distanceTo(myLocation) < 4.25f) {
                                addDesireZone(loc, 3.25f, -25);
                                addVector(loc, -1.5f);
                            }
                            break;
                        case LUMBERJACK:
                            if (loc.distanceTo(myLocation) < 4.71f) {
                                addDesireZone(loc, 3.01f, -40f); //avoid their strike range so our lumbers can actually hit
                                if (zoneLoc < 10) {
                                    addDesireZone(loc, 3.76f, -20f); //avoid their strike+move range if we have enough power
                                }
                            }
                            break;
                    }
                }
            }


            if (totalEnemies > 0) {
                float repelStr = 0;
                if ((totalEnemies + (defensive)) * 1.1f > (totalAllies + (agression * 2)) || extremeEconMode || isAssasin) {
                    repelStr = -5;// / totalEnemies; //flee
                } else {
                    repelStr = 5;// / totalEnemies; //advance
                }
                addVector(totalEnemyX / (float) totalEnemies, totalEnemyY / (float) totalEnemies, repelStr);

            }
        }


        if (guardJob != null) {
            if (enemyTeamStrength < 0.5) {
                addVector(guardJob, 5);
            }
        }


        boolean stuck = false;

        if (!masscombat) {
            if (prevLoc3 != null) {
                if (myLocation.distanceTo(prevLoc3) < 0.5f) {
                    //We're probably stuck behind something
                    stuck = true;
                    turnsStuck++;
                    directionDecisionCooldown -= 10;
                } else {
                    turnsStuck = 0;
                }
            }

        }

        //Only report in every couple of turns to confuse some players (who might depend too much on broadcasts every turn)
        if (shouldBroadcastThings) {
            if (rc.readBroadcast(SOLDIERS_REPORTING_IN_TURN) != turn) {
                rc.broadcast(SOLDIERS_REPORTING_IN_TURN, turn);
                rc.broadcast(SOLDIERS_REPORTING_IN, 1);
                if (stuck || moveRandomlyTurns > 0) {
                    rc.broadcast(SOLDIERS_REPORTING_IN_STUCK, 1);
                } else {
                    rc.broadcast(SOLDIERS_REPORTING_IN_STUCK, 0);
                }
            } else {
                rc.broadcast(SOLDIERS_REPORTING_IN, rc.readBroadcast(SOLDIERS_REPORTING_IN) + 1);
                if (stuck || moveRandomlyTurns > 0) {
                    rc.broadcast(SOLDIERS_REPORTING_IN_STUCK, rc.readBroadcast(SOLDIERS_REPORTING_IN) + 1);
                }
            }
        }


        if (!masscombat) {
            if (!stuck) {
                moveRandomlyTurns--; //Moverandomly used to make us move randomly, now kind of obsolete
            }
            if (turnsStuck >= 2) {
                moveRandomlyTurns = 10;
            }
        }

        if (guardJob == null) {

            //Makes us stick to our choice of agression/defense a little more, so we don't jump back and forth
            if (agression >= defensive) {
                aggrMomentum = 2;
                defMomentum = 0;
            } else {
                aggrMomentum = 0;
                defMomentum = 1;
            }

            if (mainAggrGoal != null) {
                if (!isAssasin) {
                    if (turn < 200) {
                        addVector(mainAggrGoal, agression + 5);
                    } else {
                        addVector(mainAggrGoal, agression + 3);
                    }
                } else {
                    //Assasins have completely different movement patterns and can so sometimes get to the valuable targets
                    float mainaggrForce = 13;
                    desireTowardsMiddle(-8);
                    desireFarEdges(5, 20);
                    desireFarEdges(-10, 4);

                    if (cluster1 != null) {
                        if (cluster1.distanceTo(myLocation) < 15) {
                            if (cluster1.distanceTo(mainAggrGoal) > 15) {
                                addVector(cluster1, -8);
                            }
                        }
                    }
                    if (cluster2 != null) {
                        if (cluster2.distanceTo(myLocation) < 15) {
                            if (cluster2.distanceTo(mainAggrGoal) > 15) {
                                addVector(cluster2, -8);
                            }
                        }
                    }
                    if (cluster3 != null) {
                        if (cluster3.distanceTo(myLocation) < 15) {
                            if (cluster3.distanceTo(mainAggrGoal) > 15) {
                                addVector(cluster3, -8);
                            }
                        }
                    }

                    if (Clock.getBytecodesLeft() > 6000) {
                        MapLocation[] pings = rc.senseBroadcastingRobotLocations();
                        for (int i = 0; i < pings.length && i < 15; i++) {
                            if (myLocation.distanceTo(pings[i]) < 18) {
                                addVector(pings[i], -1);
                                mainaggrForce += 0.3f;
                                //  rc.setIndicatorLine(myLocation,pings[i], 0,0,0);
                            }
                        }
//                        rc.setIndicatorDot(myLocation.add(Direction.NORTH, 2), 255,255,0);
                    }
                    addVector(mainAggrGoal, mainaggrForce);
                }

//                if(ally.name().equals("A")) {
//                    rc.setIndicatorDot(mainAggrGoal,255,0,0);
//                }
//                else{
//                    rc.setIndicatorDot(mainAggrGoal,200,200,0);
//                }
            } else {
                //Halp, we don't know what do. Spread out?   Basically made obsolete because we almost always have a target now.
                addVector(getRandomGoal(), 7);
                desireTowardsMiddle(-2);

                if (rc.getID() + (turn / 10) % 3 == 0) {
                    addVector(rc.readBroadcast(HOME_X), rc.readBroadcast(OPPOSITE_HOME_Y), 3);
                } else if (rc.getID() + (turn / 10) % 3 == 1) {
                    addVector(rc.readBroadcast(OPPOSITE_HOME_X), rc.readBroadcast(HOME_Y), 3);
                } else {
                    addVector(rc.readBroadcast(OPPOSITE_HOME_X), rc.readBroadcast(OPPOSITE_HOME_Y), 3);
                }
            }


            if (!masscombat && !isAssasin) {
                int AggrSecondaryX = rc.readBroadcast(Archon.SECONDARY_AGRESSIVE_TARGET_X);
                if (AggrSecondaryX > 0) {
                    addVectorSqrt(AggrSecondaryX, rc.readBroadcast(Archon.SECONDARY_AGRESSIVE_TARGET_Y), agression / 1.5f);
                }
            }
        }

        if(defTarget != null){
            if (closeToDefTarget) {
                if(guardJob != null){
                    addVector(defTarget,  5 + defensive * 2f);

                }else {
                    addVector(defTarget, defensive * 2f);
                }
//                rc.setIndicatorLine(myLocation,defTarget,0,0,255);
            }
            else if (mainAggrGoal == null) {
                addVector(defTarget, defensive);
//                rc.setIndicatorLine(myLocation,defTarget,255,0,255);
            }
        }


        nukeThisTree = null;
        int nukeableTestAttempts = 0;

        TreeInfo[] nearbyTrees;
        if (!masscombat) {
            nearbyTrees = rc.senseNearbyTrees(5); //Not sensing all trees, was costing too much bytecodes on wood maps

            boolean doingTreeMovement = false;
            int attempts = 0;
            for (int i = 0; i < nearbyTrees.length && i < 10; i++) {

                TreeInfo tree = nearbyTrees[i];
                MapLocation loc = tree.location;
                float distance = loc.distanceTo(myLocation);



                //This section here does the bugpathing. Basically it checks whether there's a tree closeby that is between us and the target. If so, it's going to try moving along it
                //It resets the direction were pathing around it ocassionaly,
                if (!doingTreeMovement && attempts < 3) {
                    attempts++;
                    if (distance < tree.radius + 1.7f) {
                        //Ok, we may be stuck here. By now we have a good idea what our movement vector will look like. Use that to figure out which side we should be going in, so let's check
                        Direction vectorDir = myLocation.directionTo(new MapLocation(myLocation.x + moveVectorX, myLocation.y + moveVectorY));
                        MapLocation directionDesire = myLocation.add(vectorDir, 0.3f);

                        float treeToApproxDist = tree.location.distanceTo(directionDesire);
                        if (distance > treeToApproxDist) {

                            //Aha, we do want to go in that direction
                            Direction curDir = myLocation.directionTo(loc);

                            //This makes us flip between clockwise/anticlockwise if we accidentally start selecting trees that are basically on the opposite side of the previous
                            if(lastBugDirection != null) {
                                if (curDir.degreesBetween(lastBugDirection) > 130) {

                                    if (directionLeaning == 0) {
                                        directionLeaning = 1;
                                        directionDecisionCooldown = 200;
                                    } else if (directionLeaning == 1) {
                                        directionLeaning = 0;
                                        directionDecisionCooldown = 200;
                                    }
                                }
                            }

                            //This bit will flip between clockwise/anticlockwise if were stuck, and it'll reset our direction picker if we haven't had to bugpath for a bit
                            if (directionDecisionCooldown <= 0 || turnsNotStuckToTree > 3) {
                                if (turnsStuck > 10 && directionLeaning == 0) {
                                    directionLeaning = 1;
                                    directionDecisionCooldown = 200;
                                } else if (turnsStuck > 10 && directionLeaning == 1) {
                                    directionLeaning = 0;
                                    directionDecisionCooldown = 200;
                                }
                                else{
                                    directionLeaning = -1;
                                }
                            }

                            //Now find the first spot that we can actually get to

                            boolean found = false;
                            MapLocation directionPicker;

                            //This is the angle we want to start looking at, we'll test angles at an offst from this
                            //This is cast to 'neat' angles because mapmakers seem to have a preference for these, makes going through straight bits easier
                            int degs = 10*((int)((Direction.NORTH.degreesBetween(curDir)/10)+0.5f));
                            Direction startDir = Direction.NORTH.rotateLeftDegrees(degs);

                            for (int attempt = 1; attempt < 60; attempt++) {
                                Direction newDir;

                                boolean left;
                                float amount;

                                if (directionLeaning == 0) {
                                    //If we're going clockwise around trees, check the angles on the left side first
                                    left = true;
                                    amount = -9 + ( 3f * attempt);
                                } else if(directionLeaning == 1) {
                                    //If we're going anti-clockwise around trees, check the angles on the right side first
                                    left = false;
                                    amount =  -9 + (3f * attempt);
                                } else{
                                    //If we don't know which way we're going yt, alternate left/right till we find something that works.
                                    amount = 2f * attempt;
                                    if(attempt %2==0) {
                                        left = true;
                                    }else{
                                        left = false;
                                    }
                                }

                                if(left){
                                    newDir = startDir.rotateLeftDegrees(amount);
                                }
                                else{
                                    newDir = startDir.rotateRightDegrees(amount);
                                }

                                directionPicker = myLocation.add(newDir, maxMove);

                                if (rc.canMove(directionPicker)) {
                                    //This spot works! Go here
                                    doingTreeMovement = true;
                                    addSpecialMapLocation(directionPicker, 40);
                                    addVector(directionPicker, 8);

                                    if(directionLeaning == -1){
                                        //Ok, let's stick with this direction for now.
                                        if(left){
                                            directionLeaning = 0;
                                        }
                                        else{
                                            directionLeaning = 1;
                                        }
                                        directionDecisionCooldown = 120;
                                    }
                                    found = true;
                                    break;
                                }
                                else{
                                    //Attempt again with smaller stride
                                    directionPicker = myLocation.add(newDir,0.5f);

                                    if (rc.canMove(directionPicker)) {
                                        //This spot works! Go here

                                        doingTreeMovement = true;
                                        addSpecialMapLocation(directionPicker, 40);
                                        addVector(directionPicker, 8);

                                        if(directionLeaning == -1){
                                            //Ok, let's stick with this direction for now.
                                            if(left){
                                                directionLeaning = 0;
                                            }
                                            else{
                                                directionLeaning = 1;
                                            }
                                            directionDecisionCooldown = 120;
                                        }
                                        lastBugDirection = myLocation.directionTo(loc);
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if (!found) {
                                directionDecisionCooldown -= 5;
                            }
                            turnsNotStuckToTree = -1;
                        }
                    }
                }


                if (tree.containedBullets > 0) {
                    if (rc.canShake(tree.ID)) {
                        rc.shake(tree.ID);
                    } else {
                        addVector(tree.getLocation(), 3);
                    }
                } else{

                    if (!tree.team.equals(enemy)) {
                        //If we're in combat, don't stand too close to trees if possible, makes it harder to dodge bullets etc
                        if(duelTarget != null){
                            if (distance < tree.radius + 3) {
                                addVector(loc, -4);

                                if(distance < tree.radius + 2){
                                    addCircularDesireZone(loc, tree.radius + 1.2f, -15);
                                }
                                else{
                                    addDesireZone(loc, tree.radius + 2, -8);
                                }
                            }
                        }else if(enemyTeamStrength > 0){
                            if (distance < tree.radius + 3) {
                                addVector(loc, -2);

                                if(distance < tree.radius + 2){
                                    addCircularDesireZone(loc, tree.radius + 1.2f, -9);
                                }
                                else{
                                    addDesireZone(loc, tree.radius + 2, -4);
                                }
                            }
                        }

                    } else {
                        if (shouldConfirmEnemyTreeLoc) {
                            shouldConfirmEnemyTreeLoc = false;
                            rc.broadcast(CONFIRMED_ENEMY_TREE_X, (int) loc.x);
                            rc.broadcast(CONFIRMED_ENEMY_TREE_Y, (int) loc.y);
                            rc.broadcast(CONFIRMED_ENEMY_TREE_TURN, turn);
                        }


                        if (distance < 5) {

                            //Select an enemy tree to nuke down if there's a gardener near.
                            //Probably should've just tried to snipe through gaps instead, but this works.
                            if (rc.getTreeCount() > 5 || turn > 1000) { //Only do this if we can afford to

                                if (nukeThisTreeLast != null) {
                                    if (nukeThisTreeLast.ID == tree.ID) {

                                        if (firstEnemyGardenerDetected != null) {
                                            if (distance + 0.2f < firstEnemyGardenerDetected.location.distanceTo(myLocation)) {
                                                nukeThisTree = tree;
                                            }
                                        }
                                    }
                                }
                                if ((nukeThisTree == null || tree.health < nukeThisTree.health) && nukeableTestAttempts < 3) {
                                    nukeableTestAttempts++;
                                    if (firstEnemyGardenerDetected != null) {
                                        if (tree.location.distanceTo(firstEnemyGardenerDetected.location) < 2.5f) {
                                            if (distance + 0.2f < firstEnemyGardenerDetected.location.distanceTo(myLocation)) {
                                                TreeInfo senseTree = rc.senseTreeAtLocation(firstEnemyGardenerDetected.location.add(firstEnemyGardenerDetected.location.directionTo(myLocation), 2.2f));
                                                if (senseTree != null && senseTree.ID == tree.ID) {
                                                    nukeThisTree = tree;
                                                    addVector(loc, 7);
                                                    fireBeforeMove = false;
                                                    if (myLocation.distanceTo(loc) < 3.05f) {
                                                        MapLocation p1 = loc.add(loc.directionTo(myLocation), 2.04f);
                                                        if (myLocation.distanceTo(p1) <= maxMove && rc.canMove(p1)) {
                                                            addSpecialMapLocation(p1, 30);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


            }
        } else {
            nearbyTrees = rc.senseNearbyTrees(4); //Not sensing all trees, was costing too much bytecodes on wood maps
            for (int i = nearbyTrees.length - 1; i >= 0; i--) {
                addDesireZone(nearbyTrees[i].location, nearbyTrees[i].radius + 1.1f, -6); // Don't stand too close to trees if possible, makes it harder to dodge bullets etc
            }
        }

        nukeThisTreeLast = nukeThisTree;

        treesDetected += nearbyTrees.length;

        prevLoc3 = prevLoc2;
        prevLoc2 = prevLoc1;
        prevLoc1 = prevLoc;
        prevLoc = myLocation;
        lastDuelTarget = duelTarget;

        boolean shouldDoDuelBehavior = false;

        if (duelTarget != null && scaryUnitCount < 4 && closeRelevantUnits == 0) {
            shouldDoDuelBehavior = true;
        }

        if (fireBeforeMove && !shouldDoDuelBehavior && rc.canFireSingleShot()) {
            //Probably have the best shot now
            doFiringLogic(robots, nearbyTrees, enemyGardenerDetected, agression);
        }
        if ((shouldDoDuelBehavior && duelTarget.location.distanceTo(myLocation) < 5) && rc.canFireSingleShot()) {
            doDuelFire(3250);
        }

        if (rc.canFireSingleShot()) {
            int approxBytsReq = 1100;
            if (!shouldDoDuelBehavior) {
                if (robots.length < 15) {
                    approxBytsReq += robots.length * 120;
                } else {
                    approxBytsReq += 1800;
                }
                if (nearbyTrees.length < 13) {
                    approxBytsReq += nearbyTrees.length * 100;
                } else {
                    approxBytsReq += 1300;
                }
                if (approxBytsReq + 2000 > Clock.getBytecodesLeft()) {
                    //Shit, not enough bytes left to both do movement and firing properly. Prioritize moving here, staying alive is more important here.
                    doMovement(1000);
                    fireCheap();
                } else {
                    doMovement(approxBytsReq);
                    doFiringLogic(rc.senseNearbyRobots(), rc.senseNearbyTrees(5), enemyGardenerDetected, agression);
                }
            } else {
                approxBytsReq = 2000;
                if (approxBytsReq + 2500 > Clock.getBytecodesLeft()) {
                    doDuelFire(3250);
                    doMovement(800);
                } else {
                    doMovement(approxBytsReq);
                    doDuelFire(100);
                }
            }
        } else {
            doMovement(800);
        }
    }

    //This is just for bytecode emergencies, barely used anymore after change to 15000 bytecodes
    public void fireCheap() throws GameActionException {
        if (rc.canFireSingleShot()) {
            RobotInfo[] robots = rc.senseNearbyRobots(3f);
            for (int i = 0; i < robots.length; i++) {
                if (robots[i].getTeam().equals(enemy)) {
                    Direction dir = myLocation.directionTo(robots[0].location);
                    float halfway = 1 + ((myLocation.distanceTo(robots[i].location) - (1 + robots[i].getRadius())) / 2);
                    if (!rc.isLocationOccupiedByTree(myLocation.add(dir, halfway)) && !rc.isLocationOccupiedByRobot(myLocation.add(dir, halfway))) {
                        rc.fireSingleShot(dir);
                        return;
                    }
                }
            }
        }
    }

    //This firemode is exclusively used for soldier on soldier duels.
    public void doDuelFire(float remaining) throws GameActionException {

        MapLocation myLocation = rc.getLocation();
        boolean canPenta = rc.canFirePentadShot();
        boolean canTriad = rc.canFireTriadShot();
        boolean canSingle = rc.canFireSingleShot();

        float distance = myLocation.distanceTo(duelTarget.location);
        Direction line1 = myLocation.directionTo(duelTarget.location);

        if (distance <= 2.05f) {
            if (canPenta) {
                rc.firePentadShot(line1);
                return;
            } else if (canTriad) {
                rc.fireTriadShot(line1);
            } else if (canSingle) {
                rc.fireSingleShot(line1);
            }
        }


        //This bit is the 'alternator' gun, which shoots slightly ahead of the target on half the turns, to have a bigger bullet spread, makes dodging harder
        //Should've used more tweaking probably
        if (distance > 6.05) {
            if (lastDuelTarget != null) {
                if (lastDuelTarget.ID == duelTarget.ID) {

                    Direction lastDir = myLocation.directionTo(lastDuelTarget.location);

                    boolean movingLeft = false;
                    if (line1.degreesBetween(lastDir) < 0) {
                        movingLeft = true;
                    }
                    if (lastAlternationShot == 0) {
                        if (movingLeft) {
                            line1 = line1.rotateLeftDegrees(4.5f);
                            lastAlternationShot = -1;
                        } else {
                            line1 = line1.rotateRightDegrees(4.5f);
                            lastAlternationShot = 1;
                        }
                    } else if (lastAlternationShot == -1) {
                        //Left last time,  right now
                        if (!movingLeft) { //Lead them if they're moving this way, but don't shoot behind them if they're not
                            line1 = line1.rotateRightDegrees(4.5f);
                        }
                        lastAlternationShot = 1;
                    } else {
                        //Right last time, left now
                        if (movingLeft) {//Lead them if they're moving this way, but don't shoot behind them if they're not
                            line1 = line1.rotateLeftDegrees(4.5f);
                        }
                        lastAlternationShot = -1;
                    }
                } else {
                    lastAlternationShot = 0;
                }
            } else {
                lastAlternationShot = 0;
            }
        }



        //In econ mode, we'll shoot slightly less. Could use some more work though..
        boolean econMode = false;
        if ((turn > 80 && rc.getTeamBullets() < 50 && rc.getRobotCount() >= 3 && rc.getTreeCount() < 6) || (turn > 250 && rc.getTeamBullets() < 100 && rc.getTreeCount() < 6)) {
            econMode = true;
        }
        if (rc.getHealth() < 20 || duelTarget.getHealth() < 10) {
            econMode = false; //Let's not econ in life/death situations
        }


        for (float bulletDist = 1.05f; bulletDist < distance && bulletDist < 7; bulletDist += 0.5) {
            MapLocation loc = myLocation.add(line1, bulletDist);
            if (rc.canSenseLocation(loc)) {
                if (rc.isLocationOccupiedByTree(loc)) {
                    canSingle = false;
                    canTriad = false;
                    canPenta = false;
                    break;
                }
            }
        }


        //Check the lines of the penta shot, if they collide with allies or too many trees, select triad/single instead
        if (canPenta) {
            int linesHitTeamMate = 0;
            int colleteral = 0;

            Direction line2 = line1.rotateLeftDegrees(30);
            Direction line3 = line1.rotateLeftDegrees(15);
            Direction line4 = line1.rotateRightDegrees(30);
            Direction line5 = line1.rotateRightDegrees(15);

            boolean line1Going = true;
            boolean line2Going = true;
            boolean line3Going = true;
            boolean line4Going = true;
            boolean line5Going = true;


            for (float bulletDist = 1.05f; bulletDist < distance && bulletDist < 6 && Clock.getBytecodesLeft() > 2000 + remaining; bulletDist += 1) {
                if (line1Going) {
                    MapLocation loc = myLocation.add(line1, bulletDist);
                    if (rc.isLocationOccupiedByRobot(loc)) {
                        if (rc.senseRobotAtLocation(loc).team.equals(enemy)) {
                            colleteral++;
                        } else {
                            linesHitTeamMate++;
                        }
                        line1Going = false;
                    }


                    if (rc.isLocationOccupiedByTree(loc)) {
                        canPenta = false;
                        canTriad = false;
                        canSingle = false;
                        break;
                    }
                }



                if (line2Going) {
                    MapLocation loc = myLocation.add(line2, bulletDist);
                    if (rc.isLocationOccupiedByRobot(loc)) {
                        if (rc.senseRobotAtLocation(loc).team.equals(enemy)) {
                            colleteral++;
                        } else {
                            linesHitTeamMate++;
                        }
                        line2Going = false;
                    }
                    if(rc.isLocationOccupiedByTree(loc)) {
                        line2Going = false;
                    }
                }

                if (line3Going) {
                    MapLocation loc = myLocation.add(line3, bulletDist);
                    if (rc.isLocationOccupiedByRobot(loc)) {
                        if (rc.senseRobotAtLocation(loc).team.equals(enemy)) {
                            colleteral++;
                        } else {
                            linesHitTeamMate++;
                        }
                        line3Going = false;
                    }
                }

                if (line4Going) {
                    MapLocation loc = myLocation.add(line4, bulletDist);
                    if (rc.isLocationOccupiedByRobot(loc)) {
                        if (rc.senseRobotAtLocation(loc).team.equals(enemy)) {
                            colleteral++;
                        } else {
                            linesHitTeamMate++;
                        }
                        line4Going = false;
                    }
                    if(rc.isLocationOccupiedByTree(loc)) {
                        line4Going = false;
                    }
                }

                if (line5Going) {
                    MapLocation loc = myLocation.add(line5, bulletDist);
                    if (rc.isLocationOccupiedByRobot(loc)) {
                        if (rc.senseRobotAtLocation(loc).team.equals(enemy)) {
                            colleteral++;
                        } else {
                            linesHitTeamMate++;
                        }
                        line5Going = false;
                    }
                }
            }

            if(!line2Going && !line4Going){
                canPenta = false;
            }
            else if (linesHitTeamMate > colleteral) {
                //Probably not worth it to fire pentas
                canPenta = false;
            }


        }

        //Check the lines of the triad shot, if they collide with allies or too many trees, select single instead
        if (canTriad) {
            int linesHitTrees = 0;
            int linesHitTeamMate = 0;
            int colleteral = 0;

            Direction line2 = line1.rotateLeftDegrees(20);
            Direction line3 = line1.rotateRightDegrees(20);

            boolean line1Going = true;
            boolean line2Going = true;
            boolean line3Going = true;

            for (float bulletDist = 1.05f; bulletDist < distance && bulletDist < 6 && Clock.getBytecodesLeft() > 1300 + remaining; bulletDist += 0.8) {
                if (line1Going) {
                    MapLocation loc = myLocation.add(line1, bulletDist);
                    if (rc.isLocationOccupiedByRobot(loc)) {
                        if (rc.senseRobotAtLocation(loc).team.equals(enemy)) {
                            colleteral++;
                        } else {
                            linesHitTeamMate++;
                        }
                        line1Going = false;
                    }
                }

                if (line2Going) {
                    MapLocation loc = myLocation.add(line2, bulletDist);
                    if (rc.isLocationOccupiedByRobot(loc)) {
                        if (rc.senseRobotAtLocation(loc).team.equals(enemy)) {
                            colleteral++;
                        } else {
                            linesHitTeamMate++;
                        }
                        line2Going = false;
                    }
                    if (rc.isLocationOccupiedByTree(loc)) {
                        line2Going = false;
                    }
                }

                if (line3Going) {
                    MapLocation loc = myLocation.add(line3, bulletDist);
                    if (rc.isLocationOccupiedByRobot(loc)) {
                        if (rc.senseRobotAtLocation(loc).team.equals(enemy)) {
                            colleteral++;
                        } else {
                            linesHitTeamMate++;
                        }
                        line3Going = false;
                    }
                    if (rc.isLocationOccupiedByTree(loc)) {
                        line3Going = false;
                    }
                }
            }
            if(!line2Going && !line3Going){
                canTriad = false;
            }
            else if (linesHitTeamMate > colleteral) {
                //Probably not worth it to fire triads if we have teammates
                canTriad = false;
            }
        }

        if (canSingle) {
            for (float bulletDist = 1.05f; bulletDist < distance && bulletDist < 7 && Clock.getBytecodesLeft() > 600 + remaining; bulletDist += 0.5) {
                MapLocation loc = myLocation.add(line1, bulletDist);
                if (rc.isLocationOccupiedByRobot(loc)) {
                    if (rc.senseRobotAtLocation(loc).team.equals(enemy)) {
                        break;
                    } else {
                        canSingle = false;
                        break;
                    }
                }
            }
        }


        boolean firePenta = false;
        boolean fireTriad = false;
        boolean fireSingle = false;


        //This bit selects our prefered shot type, probably needs a good amount of tweaking
        if (duelTargetStandingStill) {
            if(distance < 3.05f){
                if (canPenta) {
                    firePenta = true;
                } else if (canTriad) {
                    fireTriad = true;
                } else if (canSingle) {
                    fireSingle = true;
                }
            } else if(distance < 4){
                if (canTriad) {
                    fireTriad = true;
                } else if (canSingle) {
                    fireSingle = true;
                 }
            } else{
                if (canSingle) {
                    fireSingle = true;
                }
            }
        } else {
            if (distance < 3.05) {
                //We can hit a lot of lines here, just go full out
                if (canPenta) {
                    firePenta = true;
                } else if (canTriad) {
                    fireTriad = true;
                } else if (canSingle) {
                    fireSingle = true;
                }
            } else if (distance < 4.3) {
                //Less likely to hit the double pent, so calm down on that. Singles get much worse at hitting
                if (canTriad) {
                    fireTriad = true;
                } else if (canPenta && !econMode) {
                    firePenta = true;
                } else if (canSingle) {
                    fireSingle = true;
                }

            } else if (distance < 5.05) {
                //2 pent line hits is basically out of the question, but singles are too easy to dodge here
                if (canTriad) {
                    fireTriad = true;
                } else if (canPenta) {
                    firePenta = true;
                } else if (canSingle && !econMode) {
                    fireSingle = true;
                }
            } else if (distance < 5.65f) {
                //At this distance, single shot become completely ineffective because it can easily be dodged (they'll have 3 steps)
                if (canTriad) {
                    fireTriad = true;
                } else if (canPenta) {
                    firePenta = true;
                }
            } else if (distance < 6.8f) {
                //From this distance on, triads can be dodged rather easily by stepping in the gaps. Only pentads can really hit reliably
                if (canPenta) {
                    firePenta = true;
                }
                else if (canTriad && !econMode) {
                    fireTriad = true;
                }
            } else {
                //Kind of want to make this even more econ as you can start sitting between pentad shots easily, problem is, we sometimes seem to lose at this distance just because were not firing
                if (canPenta) {
                    firePenta = true;
                } else if (canTriad && !econMode) {
                    fireTriad = true;
                }
            }
        }


        if (rc.hasMoved()) {
            if (firePenta) {
                rc.firePentadShot(line1);
            } else if (fireTriad) {
                rc.fireTriadShot(line1);
            } else if (fireSingle) {
                rc.fireSingleShot(line1);
            }
        } else {
            if (firePenta) {
                rc.firePentadShot(line1);
                addDesireZone(myLocation.add(line1, 1.05f), 1.1f, -70); //Don't shoot urself in the face man
                addDesireZone(myLocation.add(line1.rotateRightDegrees(25), 1.05f), 1.1f, -70); //Don't shoot urself in the face man
                addDesireZone(myLocation.add(line1.rotateLeftDegrees(25), 1.05f), 1.1f, -70); //Don't shoot urself in the face man
            } else if (fireTriad) {
                rc.fireTriadShot(line1);

                addDesireZone(myLocation.add(line1, 1.05f), 1.1f, -70); //Don't shoot urself in the face man
                addDesireZone(myLocation.add(line1.rotateRightDegrees(20), 1.05f), 1.1f, -70); //Don't shoot urself in the face man
                addDesireZone(myLocation.add(line1.rotateLeftDegrees(20), 1.05f), 1.1f, -70); //Don't shoot urself in the face man
            } else if (fireSingle) {
                rc.fireSingleShot(line1);
                addDesireZone(myLocation.add(line1, 2.05f), 1.1f, -70); //Don't shoot urself in the face man
                addDesireZone(myLocation.add(line1, 1.05f), 1.1f, -60);
            }
        }

    }

    public void doFiringLogic(RobotInfo[] robots, TreeInfo[] nearbyTrees, boolean enemyGardenerDetected, int agression) throws GameActionException {
        //We cut the circle around our unit into 18 slices. Then look at how many enemy targets there are here, as well as how many allies and add up a total score.
        //With different values based on distance/health etc. Then, these are blended together a little for colleteral damage issues, and after that, we shoot at whichever scores the best.
        //With firetype mostly dependent on total score. (more units = higher score = go for pentads)

        //This honestly needs a big rework because it's clearly not as good as some other systems. While target prioritization is excellent, selecting the shot type is not.

        float[] targetImportance = new float[18];
        Direction[] representativeForSlot = new Direction[18];



        int markedTargetId = -1;
        if (rc.readBroadcast(MARKED_TARGET_TURN) == turn) {
            markedTargetId = rc.readBroadcast(MARKED_TARGET_ID);
        }

        //We can snipe enemy clusters and tanks outside of vision with soldiers

        int treeCount = rc.getTreeCount();
        if (turn - rc.readBroadcast(CONFIRMED_ENEMY_TANK_TURN) < 3) {
            MapLocation snipeSpot = new MapLocation(((float) rc.readBroadcast(CONFIRMED_ENEMY_TANK_X)) / 10f, ((float) rc.readBroadcast(CONFIRMED_ENEMY_TANK_Y)) / 10f);

            //Let max distance depend on how many trees we sort of expect in the path
            if ((myLocation.distanceTo(snipeSpot) < 10 && (treesDetected < 4 || treeCount > 10)) || (myLocation.distanceTo(snipeSpot) < 12 && (treesDetected < 2 || treeCount > 13)) || (myLocation.distanceTo(snipeSpot) < 14 && (treesDetected == 0 || treeCount > 20))) {
                Direction dir = myLocation.directionTo(snipeSpot);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                representativeForSlot[slot] = dir;
                targetImportance[slot] += 6;
            }
        }
        if (cluster1 != null) {
            //Let's shoot when it's just out of our sensor range (or inside, we don't mind adding a little extra preference here)
            if (myLocation.distanceTo(cluster1) < 12) {
                Direction dir = myLocation.directionTo(cluster1);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                targetImportance[slot] += rc.readBroadcast(CLUSTER1_SIZE);
            }
        }

        if (cluster2 != null) {
            //Let's shoot when it's just out of our sensor range (or inside, we don't mind adding a little here)
            if (myLocation.distanceTo(cluster2) < 12) {
                Direction dir = myLocation.directionTo(cluster2);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                targetImportance[slot] += rc.readBroadcast(CLUSTER2_SIZE);
            }
        }
        if (cluster3 != null) {
            //Let's shoot when it's just out of our sensor range (or inside, we don't mind adding a little here)
            if (myLocation.distanceTo(cluster3) < 12) {
                Direction dir = myLocation.directionTo(cluster3);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                targetImportance[slot] += rc.readBroadcast(CLUSTER3_SIZE);
            }
        }


        boolean foundVeryCloseScout = false;

        boolean forcePentadIfPossible = false;

        for (int i = 0; i < robots.length && i < 13; i++) {

            RobotInfo r = robots[i];
            MapLocation loc = r.location;

            double health = r.health;

            float curScore = 0;
            float distance = loc.distanceTo(myLocation);
            Direction dir = myLocation.directionTo(loc);
            int slot = (int) (dir.radians * 2.8647f);

            if (slot < 0) {
                slot += 18;
            }

            if (r.team.equals(enemy)) {
                curScore = 0;


                //Mark an enemy as a target for all our soldiers. Overlapping fire makes it much harder to dodge
                if (markedTargetId == -1 && !r.type.equals(RobotType.SCOUT) && !r.type.equals(RobotType.ARCHON) && distance < 6f ) {
                    markedTargetId = r.ID;
                    rc.broadcast(MARKED_TARGET_ID, r.ID);
                    rc.broadcast(MARKED_TARGET_TURN, turn);
                    rc.broadcast(MARKED_TARGET_X, (int) (r.location.x * 100));
                    rc.broadcast(MARKED_TARGET_Y, (int) (r.location.y * 100));
                }

                if (r.ID == markedTargetId) { //Should theoretically make it much harder for this marked target to dodge
                    curScore += 4;
                }

                if (representativeForSlot[slot] == null) {
                    representativeForSlot[slot] = dir;
                }
                if (health < 20) {
                    curScore += 2;
                }

                if (distance <= 1.05f + r.getRadius()) {
                    curScore += 20; //Instantly hit, prioritize this highly
                    representativeForSlot[slot] = dir;
                }

                switch (r.type) {
                    case SCOUT:


                        curScore += 1; //Possible collateral hit
                        if (distance < 4.05f) {
                            curScore += 6; //Pretty hard to dodge, especially if we keep firing
                            if (distance <= 2.05f) {
                                foundVeryCloseScout = true;
                            }
                        }

                        break;
                    case SOLDIER:
                        curScore += 8; //Possible colleteral hit
                        if (distance < 5.05f) {
                            curScore += 8; //Won't be dodged
                            representativeForSlot[slot] = dir;
                        }

                        break;
                    case GARDENER:
                        curScore += 5; //Possible colleteral hit
                        if (distance < 7.05f) {
                            curScore += 3; //Pretty hard to dodge, especially if we keep firing
                            if (distance < 5.05f) {
                                curScore += 5; //Won't be dodged, but maybe not penta

                                if (distance < 3) {
                                    curScore += 20;
                                }
                                representativeForSlot[slot] = dir;
                            }
                        }

                        if (!masscombat && ((rc.getTreeCount() > 7 && soldirBuddies > 1) || (rc.getTreeCount() > 1 && soldirBuddies > 2))) {
                            if (rc.isLocationOccupiedByTree(loc.add(dir.opposite(), 2.01f)) || rc.isLocationOccupiedByTree(loc.add(dir.opposite(), 2.5f))) {
                                curScore = -5;
                            }
                        }



                        if(Clock.getBytecodesLeft() > 3000) {
                            boolean canHitMiddle = true;

                            for (float j = 1.05f; j < distance - 2; j += 0.15) {
                                if (rc.isLocationOccupiedByTree(myLocation.add(dir, j))) {
                                    canHitMiddle = false;
                                    break;
                                }
                            }

                            if (canHitMiddle) {
                                representativeForSlot[slot] = dir;
                            } else {
                                boolean canHitLeft = true;

                                MapLocation leftSide = loc.add(dir.rotateLeftDegrees(90), 0.8f);
                                float distToSide = myLocation.distanceTo(leftSide) - 2f;
                                Direction dirToLeft = myLocation.directionTo(leftSide);

                                for (float j = 1.05f; j < distToSide; j += 0.1) {
                                    MapLocation l = myLocation.add(dirToLeft, j);
                                    if (rc.canSenseLocation(l)) {
                                        if (rc.isLocationOccupiedByTree(myLocation.add(dirToLeft, j))) {
                                            canHitLeft = false;
                                            break;
                                        }
                                    }
                                }

                                if (canHitLeft) {
                                    representativeForSlot[slot] = dirToLeft;
//                                    rc.setIndicatorLine(myLocation, leftSide, 0, 0, 0255);

                                } else {
                                    boolean canHitRight = true;

                                    MapLocation rightSide = loc.add(dir.rotateRightDegrees(90), 0.8f);
                                    float distToRight = myLocation.distanceTo(rightSide) - 2f;
                                    Direction dirToRight = myLocation.directionTo(rightSide);

                                    for (float j = 1.05f; j < distToRight; j += 0.1) {
                                        MapLocation l = myLocation.add(dirToRight, j);
                                        if (rc.canSenseLocation(l)) {
                                            if (rc.isLocationOccupiedByTree(myLocation.add(dirToRight, j))) {
                                                canHitRight = false;
                                                break;
                                            }
                                        }
                                    }

                                    if (canHitRight) {
//                                        rc.setIndicatorLine(myLocation, rightSide, 0, 255, 0);
                                        representativeForSlot[slot] = dirToRight;

                                    } else {
                                        curScore = 0;//Were not hitting anything it seems
                                    }

                                }
                            }
                        }


                        break;
                    case ARCHON:

                        if (initialEnemyArchonCount == 1) {
                            curScore = 9;
                            if (turn < 350) {
                                curScore -= 5;
                            } else if (distance < 4 && (rc.getTreeCount() > 3 || rc.getTeamBullets() > 110)) {
                                curScore += 5; //Penta shots possible
                            }
                            if (treeCount > 2) {
                                curScore += 2;
                            }
                        } else if (initialEnemyArchonCount == 2) {
                            curScore = 8;
                            if (turn < 550) {
                                curScore -= 5;
                            } else {
                                if (distance < 4) {
                                    curScore += 5;
                                }
                            }
                            if (treeCount < 3) {
                                curScore -= 3;
                            }
                        } else if (initialEnemyArchonCount == 3) {
                            curScore = 7;
                            if (turn < 750) {
                                curScore -= 4;
                            } else {
                                if (distance < 4) {
                                    curScore += 2;
                                }
                            }
                            if (treeCount < 3) {
                                curScore -= 3;
                            }
                        }

                        if(distance < 4) {
                            if (rc.readBroadcast(OUR_ARCHON_CAPTURED) == 1) {
                                curScore += 12;
//                                rc.setIndicatorLine(myLocation, loc, 255, 0, 0);
                            }
                        }
                        if (rc.getTreeCount() > 10) {
                            curScore += 6;
                        }
                        if (health < 70) {
                            if (rc.getRobotCount() > 8) {
                                curScore += 9; //Just finish it off fast
                            } else {
                                curScore += 1;
                            }
                        }
                        if (turn < 160) {
                            curScore -= 4; //Countering the addition for early game
                        }

                        break;
                    case TANK:

                        int maxTotal = 7;
                        if (distance < 5.05f) {
                            maxTotal += 6; //Triad terrain
                            representativeForSlot[slot] = dir;
                            if (distance <= 4.05f) {
                                maxTotal += 40; //Pentad this down
                            }
                        }

                        //Dont want to stack score with tank reports too much
                        if(targetImportance[slot] > maxTotal){
                            curScore = 2;
                        }
                        else{
                            curScore = 2 + maxTotal - targetImportance[slot];
                        }


                        if(Clock.getBytecodesLeft() > 2000) {
                            boolean canHitMiddle = true;

                            for (float j = 1.05f; j < distance - 2; j += 0.3) {
                                if (rc.isLocationOccupiedByTree(myLocation.add(dir, j))) {
                                    canHitMiddle = false;
                                    break;
                                }
                            }

                            if (canHitMiddle) {
                                representativeForSlot[slot] = dir;
                            } else {
                                boolean canHitLeft = true;

                                MapLocation leftSide = loc.add(dir.rotateLeftDegrees(90), 1.8f);
                                float distToSide = myLocation.distanceTo(leftSide) - 2f;
                                Direction dirToLeft = myLocation.directionTo(leftSide);

                                for (float j = 1.05f; j < distToSide; j += 0.3) {
                                    MapLocation l = myLocation.add(dirToLeft, j);
                                    if (rc.canSenseLocation(l)) {
                                        if (rc.isLocationOccupiedByTree(myLocation.add(dirToLeft, j))) {
                                            canHitLeft = false;
                                            break;
                                        }
                                    }
                                }

                                if (canHitLeft) {
                                    representativeForSlot[slot] = dirToLeft;
//                                    rc.setIndicatorLine(myLocation, leftSide, 0, 0, 0255);

                                } else {
                                    boolean canHitRight = true;

                                    MapLocation rightSide = loc.add(dir.rotateRightDegrees(90), 1.8f);
                                    float distToRight = myLocation.distanceTo(rightSide) - 2f;
                                    Direction dirToRight = myLocation.directionTo(rightSide);

                                    for (float j = 1.05f; j < distToRight; j += 0.3) {
                                        MapLocation l = myLocation.add(dirToRight, j);
                                        if (rc.canSenseLocation(l)) {
                                            if (rc.isLocationOccupiedByTree(myLocation.add(dirToRight, j))) {
                                                canHitRight = false;
                                                break;
                                            }
                                        }
                                    }

                                    if (canHitRight) {
//                                        rc.setIndicatorLine(myLocation, rightSide, 0, 255, 0);
                                        representativeForSlot[slot] = dirToRight;

                                    } else {
                                        curScore = 0;//Were not hitting anything it seems
                                    }

                                }
                            }
                        }




//                            System.out.println("cur: " + curScore +  " max " + maxTotal + "aleady " + targetImportance[slot] + "slot:" + slot );

                            break;
                    case LUMBERJACK:
                        curScore += 3; //Possible colleteral hit
                        if (distance < 5.05f) {
                            curScore += 8; //Pretty hard to dodge, especially if we keep firing
                            if (distance < 3.05f) {
                                curScore += 15; //Won't be dodged
                                representativeForSlot[slot] = dir;
                                if (distance <= 2.05f) {
                                    curScore += 40; //Instantly hit, prioritize this highly
                                }
                            }
                        }
                        break;
                }
                if (health < 20) { //Focus fire
                    curScore += 2;
                }
            } else {

                if (distance < 4 + r.getRadius()) {
                    curScore -= 25;


                    if(r.getRadius() > 1.01) {
                        int slotBelow = slot-1;
                        int slotAbove = slot+1;
                        if (slotBelow < 0) {
                            slotBelow = 17;
                        }
                        if (slotAbove > 17) {
                            slotAbove = 0;
                        }

                        targetImportance[slotBelow] -= 10;
                        targetImportance[slotAbove] -= 10;
                    }
                }

                curScore -= 15;
            }
//            System.out.println("slot: " +slot + "before: " + targetImportance[slot] );

            targetImportance[slot] += curScore;
//            System.out.println("slot: " +slot + "after: " + targetImportance[slot] );

//            rc.setIndicatorLine(myLocation,loc, slot*16, 255 - (slot*16), 0);

        }

        if (Clock.getBytecodesLeft() < 600) {
            return; //Well, too bad..
        }


        boolean shootAtNeutral = false;

        for (int i = 0; i < nearbyTrees.length && i < 10; i++) {

            TreeInfo tree = nearbyTrees[i];
            MapLocation loc = tree.location;

            float curScore = 0;

            float distance = loc.distanceTo(myLocation);
            Direction dir = myLocation.directionTo(loc);
            int slot = (int) (dir.radians * 2.8647f);

            if (slot < 0) {
                slot += 18;
            }
            if (tree.team.equals(enemy)) {
                if (representativeForSlot[slot] == null) {
                    representativeForSlot[slot] = dir;
                }

                if(rc.getTreeCount() > 5) {
                    if (distance < 4.3) {
                        if (!enemyGardenerDetected) {
                            if (masscombat) {
                                //We can afford to hit these in mass combat, because well break through
                                curScore += 6;
                            } else {
                                curScore += 3;
                            }
                        } else {
                            curScore -= 8;
                        }

                        if (nukeThisTree != null && nukeThisTree.ID == tree.ID) {
//                        rc.setIndicatorLine(myLocation,loc,255,0,0);
                            curScore += 5;
                            forcePentadIfPossible = true;
                        }
                    }
                    if (!enemyGardenerDetected) {
                        curScore += 3;
                    }
                    if (distance > 6) {
                        curScore += 1;//slightly easier to hit enemies if theyre in the middle of trees
                    }
                }
            } else if (tree.team.equals(ally)) {
                if (!foundVeryCloseScout) {
                    curScore -= 4; //Don't shoot in one of our trees direction! Unless there's a scout sitting there of course (as that'll be hit before the tree)
                    if (distance < 3 + tree.getRadius() && !foundVeryCloseScout) {
                        curScore -= 8; //Don't really want to shoot at trees if they're the closest
                    }
                }
            } else {
                if (distance < 1.9f + tree.getRadius() && !foundVeryCloseScout) {
                    curScore -= 18; //Don't really want to shoot at trees if they're the closest

                    if(tree.getRadius() > 1.01) {
                        int slotBelow = slot-1;
                        int slotAbove = slot+1;
                        if (slotBelow < 0) {
                            slotBelow = 17;
                        }
                        if (slotAbove > 17) {
                            slotAbove = 0;
                        }
                        targetImportance[slotBelow] -= 10;
                        targetImportance[slotAbove] -= 10;
                    }

                }
                if (distance < 3.9f + tree.getRadius() && !foundVeryCloseScout) {
                    curScore -= 6; //Don't really want to shoot at trees if they're the closest
                }
                if (distance > 6) {
                    curScore += 2;//slightly easier to hit enemies if theyre in the middle of trees
                }
                if(rc.getTreeCount() > 10) {
                    if (!shootAtNeutral && turnsStuck > 10 && distance < 1.4f + tree.getRadius()) {
                        curScore = 12;
                        shootAtNeutral = true;
//                        rc.setIndicatorDot(loc, 255, 0, 0);
                    }
                }
            }
            targetImportance[slot] += curScore;
//            rc.setIndicatorLine(myLocation,loc, slot*16, 255 - (slot*16), 0);
        }


        if (Clock.getBytecodesLeft() < 450) return; //Well, too bad..

        //Struggling with bytecodes for soldiers. Just do it without loop..
        int bestSlot = 0;
        float bestScore = targetImportance[0] + (targetImportance[17] + targetImportance[1]) / 2;
        float score1 = targetImportance[1] + (targetImportance[0] + targetImportance[2]) / 2;
        float score2 = targetImportance[2] + (targetImportance[1] + targetImportance[3]) / 2;
        float score3 = targetImportance[3] + (targetImportance[2] + targetImportance[4]) / 2;
        float score4 = targetImportance[4] + (targetImportance[3] + targetImportance[5]) / 2;
        float score5 = targetImportance[5] + (targetImportance[4] + targetImportance[6]) / 2;
        float score6 = targetImportance[6] + (targetImportance[5] + targetImportance[7]) / 2;
        float score7 = targetImportance[7] + (targetImportance[6] + targetImportance[8]) / 2;
        float score8 = targetImportance[8] + (targetImportance[7] + targetImportance[9]) / 2;
        float score9 = targetImportance[9] + (targetImportance[8] + targetImportance[10]) / 2;
        float score10 = targetImportance[10] + (targetImportance[9] + targetImportance[11]) / 2;
        float score11 = targetImportance[11] + (targetImportance[10] + targetImportance[12]) / 2;
        float score12 = targetImportance[12] + (targetImportance[11] + targetImportance[13]) / 2;
        float score13 = targetImportance[13] + (targetImportance[12] + targetImportance[14]) / 2;
        float score14 = targetImportance[14] + (targetImportance[13] + targetImportance[15]) / 2;
        float score15 = targetImportance[15] + (targetImportance[14] + targetImportance[16]) / 2;
        float score16 = targetImportance[16] + (targetImportance[15] + targetImportance[17]) / 2;
        float score17 = targetImportance[17] + (targetImportance[16] + targetImportance[0]) / 2;
        if (score1 > bestScore) {
            bestScore = score1;
            bestSlot = 1;
        }
        if (score2 > bestScore) {
            bestScore = score2;
            bestSlot = 2;
        }
        if (score3 > bestScore) {
            bestScore = score3;
            bestSlot = 3;
        }
        if (score4 > bestScore) {
            bestScore = score4;
            bestSlot = 4;
        }
        if (score5 > bestScore) {
            bestScore = score5;
            bestSlot = 5;
        }
        if (score6 > bestScore) {
            bestScore = score6;
            bestSlot = 6;
        }
        if (score7 > bestScore) {
            bestScore = score7;
            bestSlot = 7;
        }
        if (score8 > bestScore) {
            bestScore = score8;
            bestSlot = 8;
        }
        if (score9 > bestScore) {
            bestScore = score9;
            bestSlot = 9;
        }
        if (score10 > bestScore) {
            bestScore = score10;
            bestSlot = 10;
        }
        if (score11 > bestScore) {
            bestScore = score11;
            bestSlot = 11;
        }
        if (score12 > bestScore) {
            bestScore = score12;
            bestSlot = 12;
        }
        if (score13 > bestScore) {
            bestScore = score13;
            bestSlot = 13;
        }
        if (score14 > bestScore) {
            bestScore = score14;
            bestSlot = 14;
        }
        if (score15 > bestScore) {
            bestScore = score15;
            bestSlot = 15;
        }
        if (score16 > bestScore) {
            bestScore = score16;
            bestSlot = 16;
        }
        if (score17 > bestScore) {
            bestScore = score17;
            bestSlot = 17;
        }


        float teamBullets = rc.getTeamBullets();
        if (teamBullets < rc.readBroadcast(KEEP_BULLETS_FLOATING) + 50) {
            bestScore -= 1;
        }
        float minBullets = 50;

        if ((turn - rc.readBroadcast(GARDENERS_REPORTING_IN_TURN) > 4) || turn > 250) {
            minBullets = 100;
        }

        if (rc.getTreeCount() < 5  && !shootAtNeutral) {
            bestScore -= 1;
        }

        if (turn < 160) {
            bestScore += 4; //Don't want to risk losing very early engagements
        }
        if (masscombat) {
            bestScore += 4;
        }

        if (rc.getRobotCount() <= 2) { //Well just go full out here, we're likely losing anyway. Just hope the enemy is equally weak
            bestScore += 4;
        } else if (turn > 150 && rc.getTreeCount() == 0) {         //So we may be shooting a little too much...
            bestScore -= 1;
        }

//        System.out.println("best shot:" + bestScore);


        if (bestScore > 4 || (agression > 4 && bestScore > 3)) {
            if (representativeForSlot[bestSlot] != null) {

                if ((bestScore > 11 || (agression > 6 && bestScore > 8) || forcePentadIfPossible) && rc.canFirePentadShot() && (bestScore > 15 || teamBullets > minBullets || forcePentadIfPossible)) {
                    rc.firePentadShot(representativeForSlot[bestSlot]);

                    if (!rc.hasMoved()) {
                        addCircularDesireZone(myLocation.add(representativeForSlot[bestSlot], 1.05f), 1.15f, -200); //Don't shoot urself in the face man
                        addCircularDesireZone(myLocation.add(representativeForSlot[bestSlot].rotateRightDegrees(25), 1.05f), 1.15f, -200); //Don't shoot urself in the face man
                        addCircularDesireZone(myLocation.add(representativeForSlot[bestSlot].rotateLeftDegrees(25), 1.05f), 1.15f, -200); //Don't shoot urself in the face man
                        //Try to do with just 3 zones instead of the 5 req
                    }
                } else if (bestScore > 6 && rc.canFireTriadShot() && (bestScore > 10 || teamBullets > minBullets)) {
                    rc.fireTriadShot(representativeForSlot[bestSlot]);
                    if (!rc.hasMoved()) {
                        addCircularDesireZone(myLocation.add(representativeForSlot[bestSlot], 1.05f), 1.1f, -120); //Don't shoot urself in the face man
                        addCircularDesireZone(myLocation.add(representativeForSlot[bestSlot].rotateRightDegrees(20), 1.05f), 1.1f, -120); //Don't shoot urself in the face man
                        addCircularDesireZone(myLocation.add(representativeForSlot[bestSlot].rotateLeftDegrees(20), 1.05f), 1.1f, -120); //Don't shoot urself in the face man
                    }
                } else {
                    rc.fireSingleShot(representativeForSlot[bestSlot]);
                    if (!rc.hasMoved()) {
                        addCircularDesireZone(myLocation.add(representativeForSlot[bestSlot], 2.05f), 1.1f, -70); //Don't shoot urself in the face man

                        addCircularDesireZone(myLocation.add(representativeForSlot[bestSlot], 1.05f), 1.1f, -60); //Don't shoot urself in the face man
                    }
                }
            }
        }



    }

    public void initial() throws GameActionException {
        maxHp = RobotType.SOLDIER.maxHealth;
        radius =  RobotType.SOLDIER.bodyRadius;
        sightradius = RobotType.SOLDIER.sensorRadius;
        bulletsightradius = RobotType.SOLDIER.bulletSightRadius;
        maxMove = RobotType.SOLDIER.strideRadius;
        bulletSpeed = RobotType.SOLDIER.bulletSpeed;
        attackpower = RobotType.SOLDIER.attackPower;


        initialEnemyArchonCount = 1;
        if (rc.readBroadcast(THEIR_ARCHON_2_ID) > -2) {
            enemyArchonCount++;
        }
        if (rc.readBroadcast(THEIR_ARCHON_3_ID) > -2) {
            enemyArchonCount++;
        }

        updateMapSize();
        if(mapSizeType >= LARGE && turn > 400 && rc.getID() % 7 == 0){
            isAssasin = true;
        }
//        System.out.println("mapsize: " + mapSizeType);
        if (turn % 2 == 0) rightWardsAroundOwnUnits = true;
    }
}
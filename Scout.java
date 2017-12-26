package bruteforcer;

import battlecode.common.*;

/**
 * Created by Hermen on 10/1/2017.
 */
public class Scout extends RobotBase {

    private static int our_scout_nr = -1; 
    
    
    private  static int enemiesDetectedThisTurn = 0;
    private  static int scaryEnemiesDetectedThisTurn = 0;

    private static int softEnemiesClose = 0;
    private static int scaryEnemiesClose = 0;


    private static int wasAtMainTargetTurn = -20;

    private static boolean isExplorer = false;

    static int enemyArchonCount;

    static RobotInfo dogfightTarget = null;
    static int dogFightTurns = 0;

    static boolean defensiveScout = false;


    public void step() throws Exception {

        MapLocation myLocation = RobotBase.myLocation;
        RobotController rc = RobotBase.rc;

        BulletInfo[] bullets = rc.senseNearbyBullets(8);
        RobotInfo[] robots = rc.senseNearbyRobots();
        TreeInfo[] nearbyTrees = rc.senseNearbyTrees();

        boolean saveResources = false;

        dodgeBullets(bullets);


        int trees = nearbyTrees.length;
        if (trees > 10) {
            trees = 10;
        }
        if (bullets.length + robots.length * 3 + trees > 50) {
            saveResources = true;
        }

        enemiesDetectedThisTurn = 0;
        scaryEnemiesDetectedThisTurn = 0;
        scaryEnemiesClose = 0;
        softEnemiesClose = 0;
        enemyScoutsDetectedThisTurn = 0;
        boolean radioSilence = false;

        boolean shouldConfirmEnemyLoc = false;
        boolean shouldConfirmEnemyTreeLoc = false;

        float scoutavoidance = 0;

        boolean hideMode = false;

        if(rc.getRobotCount() <= 1){
            hideMode = true;
            radioSilence = true;
        }

        if (!radioSilence) {
            if (turn % 3 == 0) {
                if (rc.readBroadcast(SCOUTS_REPORTING_IN_TURN) != turn) {
                    rc.broadcast(SCOUTS_REPORTING_IN_TURN, turn);
                    rc.broadcast(SCOUTS_REPORTING_IN_TOTAL, rc.readBroadcast(SCOUTS_REPORTING_IN));
                    rc.broadcast(SCOUTS_REPORTING_IN, 1);
                } else {
                    rc.broadcast(SCOUTS_REPORTING_IN, rc.readBroadcast(SCOUTS_REPORTING_IN) + 1);
                }
            }
        }



        if (!saveResources) {
            becomeCommanderIfNeccessary();


            if (turn % 3 == 0) {
                enemyArchonCount = 0;
                if (turn - rc.readBroadcast(THEIR_ARCHON_1_LASTSEEN) < 30) {
                    enemyArchonCount++;
                }
                if (rc.readBroadcast(THEIR_ARCHON_2_ID) > -1 && turn - rc.readBroadcast(THEIR_ARCHON_2_LASTSEEN) < 30) {
                    enemyArchonCount++;
                }
                if (rc.readBroadcast(THEIR_ARCHON_3_ID) > -1 && turn - rc.readBroadcast(THEIR_ARCHON_3_LASTSEEN) < 30) {
                    enemyArchonCount++;
                }
            }

            if(hideMode){
                desireTowardsMiddle(-6);
                //If we're hiding, avoid far edges, but give a preference to the area just outside
                desireFarEdges(-20,5);
                desireFarEdges(6,9);
            }else {
                desireTowardsMiddle(-2);
                //Avoid edges
                desireFarEdges(-12, 2);
                desireFarEdges(-2, 5);
            }


            MapLocation archonloc1 = null;
            MapLocation archonloc2 = null;
            MapLocation archonloc3 = null;

            float archons = 0;

            if (turn - rc.readBroadcast(Archon.OUR_ARCHON_1_LASTSEEN) < 5) {
                archonloc1 = new MapLocation(rc.readBroadcast(Archon.OUR_ARCHON_1_X), rc.readBroadcast(Archon.OUR_ARCHON_1_Y));
                float distance = archonloc1.distanceTo(myLocation);
                if (distance < 1.1f) {
                    distance = 1.1f;
                }
                archons++;
            }


            if (turn - rc.readBroadcast(Archon.OUR_ARCHON_2_LASTSEEN) < 5) {
                archonloc2 = new MapLocation(rc.readBroadcast(Archon.OUR_ARCHON_2_X), rc.readBroadcast(Archon.OUR_ARCHON_2_Y));
                float distance = archonloc2.distanceTo(myLocation);
                if (distance < 1.1f) {
                    distance = 1.1f;
                }
                archons++;
            }

            if (turn - rc.readBroadcast(Archon.OUR_ARCHON_3_LASTSEEN) < 5) {
                archonloc3 = new MapLocation(rc.readBroadcast(Archon.OUR_ARCHON_3_X), rc.readBroadcast(Archon.OUR_ARCHON_3_Y));
                float distance = archonloc3.distanceTo(myLocation);
                if (distance < 1.1f) {
                    distance = 1.1f;
                }
                archons++;
            }

            if (archons > 0) {
                float archonRepelForce = -3 / archons;

                if (archonloc1 != null) {
                    addVectorSqrt(archonloc1, archonRepelForce);//Get out into the world little birds!
                }
                if (archonloc2 != null) {
                    addVectorSqrt(archonloc2, archonRepelForce);//Get out into the world little birds!
                }
                if (archonloc3 != null) {
                    addVectorSqrt(archonloc3, archonRepelForce);//Get out into the world little birds!
                }
            }

            scoutavoidance = -5 + rc.readBroadcast(SCOUTS_REPORTING_IN);
            if (scoutavoidance > -1.5f) scoutavoidance = -1.5f;
        }

        if (rc.readBroadcast(CONFIRMED_ENEMY_TURN) < turn && !hideMode) {
            shouldConfirmEnemyLoc = true;
        }
        if (rc.readBroadcast(CONFIRMED_ENEMY_TREE_TURN) < turn && !hideMode) {
            shouldConfirmEnemyTreeLoc = true;
        }

        int enemiesPointBlank = 0;


        boolean enemyGardnereNear = false;
        boolean foundSuitableHitSpots = false;
        boolean alreadyBroadcastAGardener = false;

        MapLocation home = new MapLocation(rc.readBroadcast(HOME_X), rc.readBroadcast(HOME_Y));

        MapLocation stalkspot1 = null;
        MapLocation stalkspot2 = null;
        MapLocation stalkspot3 = null;
        MapLocation stalkspot4 = null;
        float stalkscore1 = -1;
        float stalkscore2 = -1;
        float stalkscore3 = -1;
        float stalkscore4 = -1;

        dogfightTarget = null;

        RobotInfo foundFriendlyGardener1 = null;
        RobotInfo foundFriendlyGardener2 = null;
        RobotInfo foundFriendlyGardener3 = null;

        for (int i = 0; i < robots.length && i < 15; i++) {
            if (robots[i].team.equals(ally)) {

                switch (robots[i].type) {
                    case SCOUT:
                        //Dodge our own scouts so we're not all trying to harass same units. but don't overdo it if we have a ton of them
                        if (!saveResources) {
                            addVector(robots[i].location, scoutavoidance);
                        }
                        break;
                    case LUMBERJACK:
                        if (myLocation.distanceTo(robots[i].location) < 3.1f) {
                            addDesireZone(robots[i].location, 2f, -60);
                            addDesireZone(robots[i].location, 2.75f, -30);
                            addVectorSqrt(robots[i].location, -0.5f);
                        }
                        break;
                    case GARDENER:
                        if (foundFriendlyGardener1 == null) {
                            foundFriendlyGardener1 = robots[i];
                        } else if (foundFriendlyGardener2 == null) {
                            foundFriendlyGardener2 = robots[i];
                        } else if (foundFriendlyGardener3 == null) {
                            foundFriendlyGardener3 = robots[i];
                        }
                        if (!saveResources) {
                            if (myLocation.distanceTo(robots[i].location) < 3) {
                                addSidewaysVector(robots[i].location, 2);
                            }
                        }
                        break;
                    case ARCHON:
                        if (!saveResources) {
                            if (myLocation.distanceTo(robots[i].location) < 4) {
                                addSidewaysVector(robots[i].location, 2);
                            }
                        }
                        break;
                }
            }
        }

        RobotInfo zeroingInOn = null;

        robotsloop:
        for (int i = 0; i < robots.length && i < 15; i++) {
            if (robots[i].team.equals(enemy)) {
                enemiesDetectedThisTurn++;
                MapLocation loc = robots[i].location;
                float distance = myLocation.distanceTo(loc);

                if(hideMode){
                    addVector(loc, -10);
                    addCircularDesireZone(loc,8, -200);
                }

                switch (robots[i].type) {
                    case SOLDIER:

                        if (distance < 8.26) {
                            addCircularDesireZone(robots[i].location, 7f, -100); //Should be enough distance to dodge bullets
                            addVector(loc, -8);
                            if (distance < 7.26) {
                                if (distance < 6.81f) {
                                    if (distance <= 2.05) {
                                        enemiesPointBlank++;
                                    }

                                    addSpecialMapLocation(myLocation.add(loc.directionTo(myLocation).rotateLeftDegrees(3), maxMove),2);
                                    addSpecialMapLocation(myLocation.add(loc.directionTo(myLocation).rotateRightDegrees(3), maxMove),2);

                                    addDesireZone(robots[i].location, 5, -80);
                                    addVector(loc, -14);
                                    addSidewaysVector(loc, 2);
                                    scaryEnemiesClose++;
                                } else {
                                    addDesireZone(robots[i].location, 6, -40);
                                    addVector(loc, -10);
                                }
                            }
                        }


                        scaryEnemiesDetectedThisTurn++;

                        break;

                    case ARCHON:
                        //Used to have a sniping logic here like for the gardener, but removed after balance changes made shooting archons with scouts kind of dumb
                        break;
                    case GARDENER:
                        enemyGardnereNear = true;

                        if (distance <= 2.05f) {
                            enemiesPointBlank++;
                        }
                        //Override the confirmed enemy if it's a gardener, since that's a more valuable main target than other robots are
                        if (!alreadyBroadcastAGardener && !hideMode) {
                            rc.broadcast(CONFIRMED_ENEMY_X, (int) loc.x);
                            rc.broadcast(CONFIRMED_ENEMY_Y, (int) loc.y);
                            rc.broadcast(CONFIRMED_ENEMY_TURN, turn);
                            shouldConfirmEnemyLoc = false;
                            alreadyBroadcastAGardener = true;
                        }

                        if (rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_TURN) != turn && !hideMode) {
                            rc.broadcast(CONFIRMED_ENEMY_GARDENER_X, (int) (robots[i].location.x * 100));
                            rc.broadcast(CONFIRMED_ENEMY_GARDENER_Y, (int) (robots[i].location.y * 100));
                            rc.broadcast(CONFIRMED_ENEMY_GARDENER_TURN, turn);
                        }

                        if (zeroingInOn == null || zeroingInOn.type.equals(RobotType.ARCHON) || robots[i].health < zeroingInOn.health) {
                            zeroingInOn = robots[i];

                            if (zeroingInOn != null) {
                                if (zeroingInOn.type.equals(RobotType.GARDENER)) {
                                    //If we're zeroin in on a different gardener, we want at least one slot open for a stalkspot, but let's not erase all other spots
                                    if (stalkspot4 != null) {
                                        stalkspot4 = null;
                                    }
                                } else {
                                    //Just remove all archon/scout stalk spots so we can actually get to the gardener, which is the main priority
                                    stalkspot1 = null;
                                    stalkspot2 = null;
                                    stalkspot3 = null;
                                    stalkspot4 = null;

                                }
                            }
                        }
                        addVector(loc,3);
                        addCircularDesireZone(loc,3.25f, 15);
                        stalkspotsblock:
                        if (distance < 3.26f) {
                            softEnemiesClose++;

                            float points = 80;
                            points += (robots[i].getType().maxHealth - robots[i].health) / 5;
                            float bestStartAngle = loc.directionTo(home).radians;

                            if (!foundSuitableHitSpots) {
                                //Check if there's a spot we can hit this from that's not blocked by trees (gardener surrounded by trees is common)
                                if (Clock.getBytecodesLeft() > 6500) {
                                    for (float dist = 2.04f; dist < 2.9f; dist += 0.3) {
                                        for (float angle = 0; angle < 6.20f; angle += 0.2) {
                                            Direction dir = new Direction(angle + bestStartAngle);
                                            MapLocation possibleSpot = loc.add(dir, dist);
                                            if (myLocation.distanceTo(possibleSpot) <= maxMove && rc.canMove(possibleSpot)) {
                                                Direction opposite = dir.opposite();
                                                if (dist < 2.05f || !rc.isLocationOccupiedByTree(possibleSpot.add(opposite, 1.05f))) {
                                                    if (dist < 2.05f || !rc.isLocationOccupiedByTree(possibleSpot.add(opposite, dist / 2))) {

                                                        if (stalkspot1 == null) {
                                                            stalkspot1 = possibleSpot;
                                                            angle += 0.3;
                                                            stalkscore1 =   ((10 + points) - (angle / 30f)) - (dist * 10f);
                                                        } else {
                                                            if (stalkspot2 == null) {
                                                                stalkspot2 = possibleSpot;
                                                                stalkscore2 = (points - (angle / 30f)) - (dist * 10f);
                                                                angle += 0.3;
                                                            } else {
                                                                if (stalkspot3 == null) {
                                                                    stalkspot3 = possibleSpot;
                                                                    stalkscore3 = (points - (angle / 30f)) - (dist * 10f);
                                                                    break stalkspotsblock;
                                                                } else {
                                                                    if (stalkspot4 == null) {
                                                                        stalkspot4 = possibleSpot;
                                                                        stalkscore4 = (points - (angle / 30f)) - (dist * 10f);
                                                                        foundSuitableHitSpots = true;
                                                                        break stalkspotsblock;
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
//                                    rc.setIndicatorDot(myLocation,0,0,255);

                                    //Cheap version
                                    MapLocation possibleSpot = loc.add(loc.directionTo(myLocation), 2.04f);
                                    if (myLocation.distanceTo(possibleSpot) < maxMove && rc.canMove(possibleSpot)) {
                                        if (stalkspot1 == null) {
                                            stalkspot1 = possibleSpot;
                                            stalkscore3 = 30;
                                        } else {
                                            if (stalkspot2 == null) {
                                                stalkspot2 = possibleSpot;
                                                stalkscore2 = 30;
                                            } else if (stalkspot3 == null) {
                                                stalkspot3 = possibleSpot;
                                                stalkscore3 = 30;
                                                break stalkspotsblock;
                                            }
                                        }
                                    }
                                }
                            }
//                            else{
//                                rc.setIndicatorDot(myLocation,0,255,0);
//                            }
                        }
                        else if(distance < 5){
                            MapLocation p1 = myLocation.add(myLocation.directionTo(loc), maxMove);
                            if (rc.canMove(p1)) {
                                addSpecialMapLocation(p1, 30);
//                                rc.setIndicatorDot(myLocation, 255, 0, 0);
                            }
//                            else{
//                                rc.setIndicatorDot(myLocation,255,255,0);
//                            }
                        }
                        break;

                    case TANK:

                        if (distance < 9.55) {

                            addCircularDesireZone(robots[i].location, 8.3f, -400); //Don't get near these
                            if(distance < 8.5) {
                                addCircularDesireZone(robots[i].location, 7.25f, -400); //DONT get near these

                                if (distance <= 3.05f) {
                                    enemiesPointBlank++;
                                }
                            }

                            addVector(robots[i].location,-20);
                            addSidewaysVector(loc, 2);
                            if (distance < 7) {
                                scaryEnemiesClose++;
                            }
                        }else {
                            addVector(loc, 3); // We want to stalk these, to confirm their locations so we can hit them with our own tanks
                        }
                        scaryEnemiesDetectedThisTurn++;

                        if (rc.readBroadcast(CONFIRMED_ENEMY_TANK_TURN) != turn && !hideMode) {
                            rc.broadcast(CONFIRMED_ENEMY_TANK_X, (int) (robots[i].location.x * 10));
                            rc.broadcast(CONFIRMED_ENEMY_TANK_Y, (int) (robots[i].location.y * 10));
                            rc.broadcast(CONFIRMED_ENEMY_TANK_TURN, turn);
                            //System.out.println("scout found tank");
                        }

                        break;

                    case LUMBERJACK:
                        if (distance <= 2.05f) {
                            enemiesPointBlank++;
                        }

                        //The maximum distance at which one of these can affect us is:    1 (my body) + 1 (lumber body) + 1.5 (lumber move) + 2.5 (scout move) + 1 (strike around lumber)  = 7
                        addSidewaysVector(loc, 2);

                        // if (rc.getHealth() < 6) {
                        //Just go for max safety
                        if (distance < 5.76) {
                            addDesireZone(loc, 4.51f, -8f); //To help select best stalk spots etc

                            if (distance <= 5.01) {
                                addSpecialMapLocation(myLocation.add(myLocation.directionTo(loc).opposite()), 10);
                                addCircularDesireZone(loc, 3.76f, -300f); //Shouldn't ever be hit unless we get cornered and are forced into a position into this area

                                if (distance < 3.76) {
                                    addDesireZone(loc, 2.5f, -120f);
                                }
                            }
                        }
                        addVector(loc, -6);
                        if (distance <= 7) {
                            scaryEnemiesClose++;
                        }
                        scaryEnemiesDetectedThisTurn++;

                        break;

                    case SCOUT:
                        if (distance < 1.01f + robots[i].getRadius()) {
                            enemiesPointBlank++;
                        }
                        enemyScoutsDetectedThisTurn++;
                        if (distance < 3) {
                            dogfightTarget = robots[i];
                        }
                        //Initiate a dogfight either if we think we can win, or if they're close to our gardeners
                        if ((rc.getHealth() > 3 && ((rc.getHealth() - robots[i].health > -2) || rc.getOpponentVictoryPoints() > rc.getTeamVictoryPoints())) || (foundFriendlyGardener1 != null && foundFriendlyGardener1.location.distanceTo(loc) < 5) || (foundFriendlyGardener2 != null && foundFriendlyGardener2.location.distanceTo(loc) < 5) || (foundFriendlyGardener3 != null && foundFriendlyGardener3.location.distanceTo(loc) < 5)) {
                            addVector(robots[i].location, 9);//Try battling?
                            //Engage in aggressive dogfight
                            if (distance <= 3.26f) {
                                finddogfightspots:
                                if (!foundSuitableHitSpots) {
                                    dogfightTarget = robots[i];

                                    if (Clock.getBytecodesLeft() > 6000) {
                                        float points = 60;
                                        float bestStartAngle = home.directionTo(loc).radians;
                                        //Check if there's a spot we can hit this from that's not blocked by trees (gardener surrounded by trees is common)
                                        float dist = 2.049f;

                                        for (float angle = 0; angle < 6.20f; angle += 0.2) {
                                            Direction dir = new Direction(angle + bestStartAngle);
                                            MapLocation possibleSpot = loc.add(dir, dist);
                                            if (myLocation.distanceTo(possibleSpot) <= maxMove && rc.canMove(possibleSpot)) {
                                                //For this spot we don't really have to look at trees, since we'll only be firing point blank
                                                if (stalkspot1 == null) {
                                                    stalkspot1 = possibleSpot;
                                                    angle += 0.6;
                                                    stalkscore3 = (points - (angle / 10));
                                                } else {
                                                    if (stalkspot2 == null) {
                                                        stalkspot2 = possibleSpot;
                                                        stalkscore2 = (points - (angle / 10));
                                                        angle += 0.6;
                                                    } else if (stalkspot3 == null) {
                                                        stalkspot3 = possibleSpot;
                                                        stalkscore3 = (points - (angle / 10));
                                                        break finddogfightspots;
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        //Cheap version
                                        MapLocation possibleSpot = loc.add(loc.directionTo(myLocation), 2.04f);
                                        if (myLocation.distanceTo(possibleSpot) < maxMove && rc.canMove(possibleSpot)) {
                                            if (stalkspot1 == null) {
                                                stalkspot1 = possibleSpot;
                                                stalkscore3 = 60;
                                            } else {
                                                if (stalkspot2 == null) {
                                                    stalkspot2 = possibleSpot;
                                                    stalkscore2 = 60;
                                                } else if (stalkspot3 == null) {
                                                    stalkspot3 = possibleSpot;
                                                    stalkscore3 = 60;
                                                    break finddogfightspots;
                                                }
                                            }
                                        }
                                    }

                                }
                            }


                        } else {
                            if (distance < 6) {
                                //Fleeing!
                                addVector(robots[i].location, -3);
                                addDesireZone(loc, 5f, -30f);
                                addSidewaysVector(loc, 2);
                            }
                        }
                        break;
                }
                if (shouldConfirmEnemyLoc && !robots[i].type.equals(RobotType.SCOUT)) {
                    rc.broadcast(CONFIRMED_ENEMY_X, (int) robots[i].location.x);
                    rc.broadcast(CONFIRMED_ENEMY_Y, (int) robots[i].location.y);
                    rc.broadcast(CONFIRMED_ENEMY_TURN, turn);
                    shouldConfirmEnemyLoc = false;
                }

            } else {

                if (myLocation.distanceTo(robots[i].location) < 1.5 + robots[i].getRadius()) {
                    addSidewaysVector(robots[i].location, 5);
                }

                if (robots[i].type.equals(RobotType.TANK)) {
                    if(!saveResources) {
                        addVector(robots[i].location, 0.5f); //Hover sort of around these to act as spotter
                        addDesireZone(robots[i].location, 8, 2);
                    }
                } else if (robots[i].type.equals(RobotType.LUMBERJACK)) {
                    if(myLocation.distanceTo(robots[i].location) < 4) {
                        addCircularDesireZone(robots[i].location, 2.75f, -30);
                    }

                }else if (robots[i].type.equals(RobotType.SOLDIER)) {
                    if(myLocation.distanceTo(robots[i].location) < 6.25) {
                        addDesireZone(robots[i].location, 5f, -30);
                    }
                    addVector(robots[i].location, -3);
                }
            }
        }


        if (zeroingInOn != null) {
            addVector(zeroingInOn.location, 9);
        }

        boolean foundStalk = false;


        if (stalkspot1 != null) {
            addSpecialMapLocation(stalkspot1, stalkscore1);
            foundStalk = true;
        }

        if (stalkspot2 != null) {
            addSpecialMapLocation(stalkspot2, stalkscore2);
            foundStalk = true;
        }

        if (stalkspot3 != null) {
            addSpecialMapLocation(stalkspot3, stalkscore3);
            foundStalk = true;
        }

        if (stalkspot4 != null) {
            addSpecialMapLocation(stalkspot4, stalkscore4);
            foundStalk = true;
        }

        if (dogfightTarget != null) {
            dogFightTurns++;
        } else {
            dogFightTurns = 0;
        }

        //System.out.println("t4 " + Clock.getBytecodesLeft());




        int maxtrees = 25;

        if (Clock.getBytecodesLeft() < 7000) {
            maxtrees = 10;
        }

        float bestShakeScore = - 999;
        MapLocation bestShakeLoc = null;

        for (int i = 0; i < nearbyTrees.length && i < maxtrees; i++) {
            TreeInfo tree = nearbyTrees[i];
            if (!saveResources) {
                if (scaryEnemiesClose > 0 && tree.radius > 1 && rc.getHealth() < 8) {
                    if (tree.location.distanceTo(myLocation) < 4) {
                        addDesireZone(tree.location, tree.getRadius() - 1, 5);  //Perching on big trees makes them harder to kill
                    }
                }
                if (scaryEnemiesClose == 0 && softEnemiesClose > 0) {
                    if (myLocation.distanceTo(tree.location) < 1.26 + tree.radius)
                        addDesireZone(tree.location, tree.radius, -5);  //If we have the ability to hit things for free, dont fly into the trees yo
                }
            }

            if (shouldConfirmEnemyTreeLoc) {
                if (tree.team.equals(enemy)) {
                    shouldConfirmEnemyTreeLoc = false;
                    rc.broadcast(CONFIRMED_ENEMY_TREE_X, (int) tree.location.x);
                    rc.broadcast(CONFIRMED_ENEMY_TREE_Y, (int) tree.location.y);
                    rc.broadcast(CONFIRMED_ENEMY_TREE_TURN, turn);
                }
            }
            if (!tree.team.equals(enemy)) {
                if (tree.containedBullets > 0) {
                    if (rc.canShake(tree.ID)) {
                        rc.shake(tree.ID);
                    } else {
                        float distScore =  (tree.containedBullets / 5) + 8 - tree.location.distanceTo(myLocation);
                        if (distScore < 6) distScore = 6;

                        if(distScore > bestShakeScore){
                            bestShakeScore = distScore;
                            bestShakeLoc = tree.location;
                        }
                    }
                }

                if (!saveResources) {
                    woodcuttingjobcreation:
                    if (tree.containedRobot != null && !radioSilence && Clock.getBytecodesLeft() > 5000) {
                        int x4 = rc.readBroadcast(WOODCUT_JOB_2_X);
                        int y4 = rc.readBroadcast(WOODCUT_JOB_4_Y);
                        if (tree.location.x < x4 + 0.005f && tree.location.x > x4 - 0.005f) {
                            if (tree.location.y < y4 + 0.005f && tree.location.y > y4 - 0.005f) {
                                //This tree is already in our job list
                                break woodcuttingjobcreation;
                            }
                        }
                        int x3 = rc.readBroadcast(WOODCUT_JOB_2_X);
                        int y3 = rc.readBroadcast(WOODCUT_JOB_3_Y);
                        if (tree.location.x < x3 + 0.005f && tree.location.x > x3 - 0.005f) {
                            if (tree.location.y < y3 + 0.005f && tree.location.y > y3 - 0.005f) {
                                //This tree is already in our job list
                                break woodcuttingjobcreation;
                            }
                        }
                        if (rc.readBroadcast(WOODCUT_JOB_4_PRIORITY) < 13) {
                            woodcutJob4 = tree.location;
                            //rc.setIndicatorLine(myLocation,tree.location, 255,255,0);
                            prioritywoodcutJob4 = 13;
                            updateWoodCuttingTargets();
                            break woodcuttingjobcreation;
                        }

                        if (rc.readBroadcast(WOODCUT_JOB_3_PRIORITY) < 13) {
                            //   rc.setIndicatorLine(myLocation,tree.location, 0,255,255);

                            woodcutJob3 = tree.location;
                            prioritywoodcutJob3 = 13;
                            updateWoodCuttingTargets();
                            break woodcuttingjobcreation;
                        }

                        MapLocation homeloc = new MapLocation(rc.readBroadcast(HOME_X), rc.readBroadcast(HOME_Y));
                        if (new MapLocation(x4, y4).distanceTo(homeloc) > tree.location.distanceTo(homeloc)) {
                            woodcutJob4 = tree.location;
                            //rc.setIndicatorLine(myLocation,tree.location, 255,255,0);
                            prioritywoodcutJob4 = 13;
                            updateWoodCuttingTargets();
                            break woodcuttingjobcreation;
                        }

                        if (new MapLocation(x3, y3).distanceTo(homeloc) > tree.location.distanceTo(homeloc)) {
                            woodcutJob3 = tree.location;
                            // rc.setIndicatorLine(myLocation,tree.location, 255,255,0);
                            prioritywoodcutJob3 = 13;
                            updateWoodCuttingTargets();
                            break woodcuttingjobcreation;
                        }
                    }
                }
            }
        }

        if(bestShakeLoc != null){
            addVector(bestShakeLoc, bestShakeScore);
            MapLocation p1 = myLocation.add(myLocation.directionTo(bestShakeLoc),maxMove);
            if(rc.canMove(p1)){
                addSpecialMapLocation(p1, 10);
            }
        }

        //System.out.println("t5 " + Clock.getBytecodesLeft());

        if (Clock.getBytecodesLeft() > 5000) { //if we're struggling on bytecode, we're likely in combat anyway
            MapLocation randomgoal = getRandomGoal();

            MapLocation maintarget = new MapLocation(rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_X), rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_Y));

            if (maintarget.distanceTo(myLocation) < 4) {
                wasAtMainTargetTurn = turn;
            }


            if (turn - rc.readBroadcast(DETECTED_SCOUT_TURN) < 4) {
                MapLocation enemyScoutLoc = new MapLocation(rc.readBroadcast(DETECTED_SCOUT_X), rc.readBroadcast(DETECTED_SCOUT_Y));

                if ((myLocation.distanceTo(enemyScoutLoc) < 15 || !enemyGardnereNear) && rc.getHealth() > 4 && enemyScoutsDetectedThisTurn < 1) {
                    addVector(enemyScoutLoc, 12); //Defend against attacking enemy scouts. Shouldn't be strng enough to break gardener extrapots etc
                    //   rc.setIndicatorLine(myLocation,enemyScoutLoc,0,255,255);
                }
            }


            if (maintarget.x >= 0 && (wasAtMainTargetTurn - turn > 15 || enemiesDetectedThisTurn > 0)) {
                addVector(maintarget, 4.2f);
                //System.out.println("main target: " + maintarget);

            } else {
                //Go find the enemies
                addVector(randomgoal, 3);
                int homex = rc.readBroadcast(HOME_X);
                int homey = rc.readBroadcast(HOME_Y);
                addVectorSqrt(homex, homey, -3);

                if (!saveResources) {
                    if ((rc.getID() + (int) (turn / 10)) % 3 == 0) {
                        addVectorSqrt(rc.readBroadcast(HOME_X), rc.readBroadcast(OPPOSITE_HOME_Y), 1.5f);
                        //System.out.println("side target1: " + rc.readBroadcast(HOME_X) + "," +rc.readBroadcast(OPPOSITE_HOME_Y));

                    } else if ((rc.getID() + (int) (turn / 10)) % 3 == 1) {
                        addVectorSqrt(rc.readBroadcast(OPPOSITE_HOME_X), rc.readBroadcast(HOME_Y), 1.5f);
                        //System.out.println("side target2: " + rc.readBroadcast(OPPOSITE_HOME_X) + "," +rc.readBroadcast(HOME_Y));

                    } else if ((rc.getID() + (int) (turn / 10)) % 3 == 2) {
                        addVectorSqrt(rc.readBroadcast(OPPOSITE_HOME_X), rc.readBroadcast(OPPOSITE_HOME_Y), 1.5f);
                        // System.out.println("side target3: " + rc.readBroadcast(OPPOSITE_HOME_X) + "," +rc.readBroadcast(OPPOSITE_HOME_Y));
                    }
                }
            }

            if (!saveResources) {
                int AggrSecondaryX = rc.readBroadcast(Archon.SECONDARY_AGRESSIVE_TARGET_X);
                if (AggrSecondaryX >= 0) {
                    addVectorSqrt(AggrSecondaryX, rc.readBroadcast(Archon.SECONDARY_AGRESSIVE_TARGET_Y), 1.5f);
                }
            }

            //Free scouting
            // System.out.println("loc: " + randomgoal.x + "," + randomgoal.y);
            if (isExplorer) {
                addVector(randomgoal, 4);
            } else {
                addVector(randomgoal, 1.5f);

            }
        }


        int approxBytsReq = 2500;

        //System.out.println("t6 " + Clock.getBytecodesLeft());

        if (enemiesPointBlank > 0 && dogFightTurns < 2 && !foundStalk) { //If enemies close, fire before moving. However, if we're in a dogfight with an enemy scout, we're much better off shooting after moving, both in pursuit and while followed
//            System.out.println("firing before move");

            fireIfPossible();
            doMovement(250);
        } else {
            if (approxBytsReq + 2000 > Clock.getBytecodesLeft()) {
                //Shit, not enough bytes left to both do movement and firing properly. In this case, we're likely stuck between many of our own units. So prefer moving over shooting
                //System.out.println("Scout cant shoot the expensive way due to bytecode problems");
                if (Clock.getBytecodesLeft() > 3000) {
//                    System.out.println("moving before fire cheap");

                    doMovement(800);
                    fireCheap();
                } else {
//                    System.out.println("firing before mov cheape");

                    fireCheapAddZone();
                    doMovement(250);
                }
            } else {
//                System.out.println("moving before fire");
                doMovement(approxBytsReq);
                myLocation = rc.getLocation();
                fireIfPossible();
            }
        }

        if (turn % 3 == 0 && !radioSilence) {

            if (Clock.getBytecodesLeft() > 1000 && !hideMode) {
                MapLocation n = myLocation.add(Direction.NORTH, 13.9f);
                MapLocation e = myLocation.add(Direction.EAST, 13.9f);
                MapLocation s = myLocation.add(Direction.SOUTH, 13.9f);
                MapLocation w = myLocation.add(Direction.WEST, 13.9f);

                if(rc.onTheMap(n)){
                    if(n.y > mapHighestY){
                        mapHighestY = n.y;
                        if((int)(mapHighestY * 100) > rc.readBroadcast(MAP_MAX_Y_BROADCAST)){
                            rc.broadcast(MAP_MAX_Y_BROADCAST,(int)(mapHighestY * 100));
                        }
                    }
                }
                if(rc.onTheMap(s)){
                    if(s.y < mapLowestY){
                        mapLowestY = s.y;
                        if((int)(mapLowestY * 100) < rc.readBroadcast(MAP_MIN_Y_BROADCAST)){
                            rc.broadcast(MAP_MIN_Y_BROADCAST,(int)(mapLowestY * 100));
                        }
                    }
                }


                if(rc.onTheMap(e)){
                    if(e.x > mapHighestX){
                        mapHighestX = e.x;
                        if((int)(mapHighestX * 100) > rc.readBroadcast(MAP_MAX_X_BROADCAST)){
                            rc.broadcast(MAP_MAX_X_BROADCAST,(int)(mapHighestX * 100));
                        }
                    }
                }
                if(rc.onTheMap(w)){
                    if(w.x < mapLowestX){
                        mapLowestX = w.x;
                        if((int)(mapLowestX * 100) < rc.readBroadcast(MAP_MIN_X_BROADCAST)){
                            rc.broadcast(MAP_MIN_X_BROADCAST,(int)(mapLowestX * 100));
                        }
                    }
                }
            }
            if (Clock.getBytecodesLeft() > 200) {
                broadcastOwnLocation();
            }

        }
        //System.out.println("tfinal " + Clock.getBytecodesLeft());
    }


    public void fireCheap() throws GameActionException{
        MapLocation myLocation = this.myLocation;
        if(rc.canFireSingleShot()) {
            RobotInfo[] robots = rc.senseNearbyRobots(2.7f);
            for(int i = 0 ; i < robots.length; i++) {
                if(robots[i].getTeam().equals(enemy)) {
                    Direction dir = myLocation.directionTo(robots[0].location);
                    if(!rc.isLocationOccupiedByTree(myLocation.add(dir,1.03f)) ){
                        if(!rc.isLocationOccupiedByTree(myLocation.add(dir,1.2f) )){
                            rc.fireSingleShot(dir);
                            return;
                        }
                        else{
                            if(robots[i].location.distanceTo(myLocation) <= 1.05f + robots[i].getRadius()){
                                rc.fireSingleShot(dir);
                                return;
                            }
                        }
                    }else{
                        if(robots[i].location.distanceTo(myLocation) <= 1.05f + robots[i].getRadius()){
                            rc.fireSingleShot(dir);
                            return;
                        }
                    }
                }
            }
        }
    }

    //Splitting this up to save a bit of bytecode
    public void fireCheapAddZone() throws GameActionException{
        MapLocation myLocation = rc.getLocation();
        if(rc.canFireSingleShot()) {
            RobotInfo[] robots = rc.senseNearbyRobots(2.7f);
            for(int i = 0 ; i < robots.length; i++) {
                if(robots[i].getTeam().equals(enemy)) {
                    Direction dir = myLocation.directionTo(robots[0].location);
                    if(!rc.isLocationOccupiedByTree(myLocation.add(dir,1.03f)) ){
                        if(!rc.isLocationOccupiedByTree(myLocation.add(dir,1.2f) )){
                            rc.fireSingleShot(dir);
                            addDesireZone(myLocation.add(dir,1.05f),1.05f,-200);
                            return;
                        }
                        else{
                            if(robots[i].location.distanceTo(myLocation) <= 1.05f + robots[i].getRadius()){
                                rc.fireSingleShot(dir);
                                addDesireZone(myLocation.add(dir,1.05f),1.05f,-200);
                                return;
                            }
                        }
                    }else{
                        if(robots[i].location.distanceTo(myLocation) <= 1.05f + robots[i].getRadius()){
                            rc.fireSingleShot(dir);
                            addDesireZone(myLocation.add(dir,1.05f),1.05f,-200);
                            return;
                        }
                    }
                }
            }
        }
    }


    public void fireIfPossible( ) throws GameActionException{
        if (rc.canFireSingleShot()) {

            // Update locations since we just moved
            RobotInfo[] robots = rc.senseNearbyRobots(5);

            //We slide the circle around our unit into 18 segments. Then well look how many enemy targets there are here, as well as how many allies and add up a total score.
            //Then, we shoot at an enemy we found inside of this circleslice
            float[] shotScoreSlots = new float[18];
            Direction[] representativeForSlot = new Direction[18];


            boolean canWeShoot = rc.canFireSingleShot();
            boolean enemyGardnereNear = false;

            boolean dogFighting = false;
            MapLocation myLocation = rc.getLocation();

            for (int i = 0; i < robots.length && i < 15; i++) {

                RobotInfo r = robots[i];
                MapLocation loc = r.location;
                float distance = loc.distanceTo(myLocation);

                double health = r.health;
                float curScore = 0;
                Direction dir = myLocation.directionTo(loc);
                int slot = (int) (dir.radians * 2.8647f);

                if (slot < 0) {
                    slot += 18;
                }
                if (r.team.equals(enemy)) {
                    if (representativeForSlot[slot] == null) {
                        representativeForSlot[slot] = dir;
                    }



                    switch(r.type){
                        case SCOUT:

//                            if(distance < 5) {
//                                curScore += 2;
//                            }

                            //We should hit this with very good certainty, so let's just fire at it


                            if(distance <=2.05f  || (dogfightTarget != null && dogfightTarget.ID == r.ID && (dogFightTurns >= 2 && distance < 3))){
                               // if(dogfightTarget.ID == r.ID){
                                    curScore += 300;
                                    representativeForSlot[slot] = dir;
                               // }
                            }
                           break;
                        case GARDENER:
                            enemyGardnereNear = true;
                            curScore += 1;

//                            System.out.println("distance is" + distance);


                            if(distance < 5.05) {
                                //Big chance of hitting
                                curScore += 5;
                                if(distance < 3.55f){
                                    //Unavoidable
                                    curScore += 15;
                                    representativeForSlot[slot] = dir;
                                    if(distance <= 2.05f){
                                        //pointblank, always fire, even if tree
//                                        System.out.println("GARDENERSNIPE");
                                        curScore += 250;
                                        representativeForSlot[slot] = dir;
                                    }
                                }

                            }

                            if(distance > 3.01) {
                                if (rc.isLocationOccupiedByTree(loc.add(dir.opposite(), 2.01f))) {
                                    curScore = 0;
                                }
                            }
                            else if(distance > 2.51){
                                if (rc.isLocationOccupiedByTree(loc.add(dir.opposite(), 1.51f))) {
                                    curScore = 0;
                                }
                            }else if(distance > 2.21){
                                if (rc.isLocationOccupiedByTree(loc.add(dir.opposite(), 1.21f))) {
                                    curScore = 0;
                                }
                            }

                            break;
                        case LUMBERJACK:

                            curScore += 1; //Possible colleteral hit
                            if(distance < 5.05f){
                                curScore += 5; //Pretty hard to dodge, especially if we keep firing

                                if(distance < 3.55f){
                                    curScore += 15; //Won't be dodged
                                    representativeForSlot[slot] = dir;

                                    if(distance <= 2.05f){
                                        curScore += 40; //Instantly hit, prioritize this highly
                                    }
                                }
                            }


                            break;
                        case SOLDIER:
                            curScore += 1; //Possible colleteral hit
                            if(distance < 5.05f){
                                curScore += 5; //Pretty hard to dodge, especially if we keep firing

                                if(distance < 3.55f){
                                    curScore += 15; //Won't be dodged
                                    representativeForSlot[slot] = dir;

                                    if(distance <= 2.05f){
                                        curScore += 40; //Instantly hit, prioritize this highly
                                    }
                                }
                            }


                            break;
                        case ARCHON:
                            curScore += 1;
                            int ownTreeCount = rc.getTreeCount();
                            if(turn > 1500 ||    (turn > 800 && enemyArchonCount == 1 && ownTreeCount > 3)) {
                                if(distance <= 6.55){
                                    //Guaranteed hit now if we fire straight at it (unless trees etc)
                                    curScore += 8;
                                }
                             }
                            break;
                        case TANK:
                            //We should in theory always hit tanks, because they dont have enough vision to see our bullets in time
                            curScore += 13; //Easy target, easy hits
                            representativeForSlot[slot] = dir;

                            if(distance <= 6){
                                //Extra prioritzation
                                curScore += 15;
                            }
                            break;
                    }

                    if (health < 10) {
                        curScore += 2;
                    }
                } else {
                    if(distance < 7) {
//                        System.out.println("teammmate");
                        curScore -= 10;
                        if (distance < 4) {
                            curScore -= 30;
                        }
                    }
                }
                shotScoreSlots[slot] += curScore;
            }

            TreeInfo[] nearbyTrees = rc.senseNearbyTrees(5);

            for (int i = 0 ; i < nearbyTrees.length && i < 10; i++) {

                TreeInfo tree = nearbyTrees[i];
                MapLocation loc = tree.location;
                if (canWeShoot) {
                    float curScore = 1;

                    Direction dir = myLocation.directionTo(loc);
                    int slot = (int) (dir.radians * 2.8647f);

                    if (slot < 0) {
                        slot += 18;
                    }


                    if (tree.team.equals(enemy)) {
                        if (representativeForSlot[slot] == null) {
                            representativeForSlot[slot] = dir;
                        }

                        if(loc.distanceTo(myLocation) < 3){
                            if (!enemyGardnereNear) {
                                curScore += 3;
                            }
                        }
                        else if(loc.distanceTo(myLocation) < 4){
                            if (!enemyGardnereNear) {
                                curScore += 2;
                            }
                        }
                    } else {
                        if (loc.distanceTo(myLocation) < 2 + tree.getRadius()) {
                            curScore -= 10; //Don't want to shoot at trees if they're the closest
                        }
                    }
                    shotScoreSlots[slot] += curScore;
                }
                if (tree.containedBullets > 0 && rc.canShake(tree.ID)) {
                    rc.shake(tree.ID);
                }

            }


            if(Clock.getBytecodesLeft() < 800) {
                if (Clock.getBytecodesLeft() > 500) {
                    fireCheap();
                }
                return; //Well, too bad..
            }


            for(int i = 0 ; i < 18; i++){

                if(representativeForSlot[i] != null) {
                    //Check that we're not sitting on a tree (this would be behind us, thus doesn't show up in the other tree checks)
                    if (rc.isLocationOccupiedByTree(myLocation.add(representativeForSlot[i],1.04f))){
                        shotScoreSlots[i] -= 50;
                    }
                    if (rc.isLocationOccupiedByTree(myLocation.add(representativeForSlot[i], 1.3f))) {
                        shotScoreSlots[i] -= 50;
                    }
                    if (rc.isLocationOccupiedByTree(myLocation.add(representativeForSlot[i], 1.5f))) {
                        shotScoreSlots[i] -= 50;
                    }
                }

            }


            //Struggling with bytecodes
            int bestSlot = 0;
            float bestScore = shotScoreSlots[0] + (shotScoreSlots[17] + shotScoreSlots[1]) / 3;

            float score1 = shotScoreSlots[1] + (shotScoreSlots[0] + shotScoreSlots[2]) / 3;
            float score2 = shotScoreSlots[2] + (shotScoreSlots[1] + shotScoreSlots[3]) / 3;
            float score3 = shotScoreSlots[3] + (shotScoreSlots[2] + shotScoreSlots[4]) / 3;
            float score4 = shotScoreSlots[4] + (shotScoreSlots[3] + shotScoreSlots[5]) / 3;
            float score5 = shotScoreSlots[5] + (shotScoreSlots[4] + shotScoreSlots[6]) / 3;
            float score6 = shotScoreSlots[6] + (shotScoreSlots[5] + shotScoreSlots[7]) / 3;
            float score7 = shotScoreSlots[7] + (shotScoreSlots[6] + shotScoreSlots[8]) / 3;
            float score8 = shotScoreSlots[8] + (shotScoreSlots[7] + shotScoreSlots[9]) / 3;
            float score9 = shotScoreSlots[9] + (shotScoreSlots[8] + shotScoreSlots[10]) / 3;
            float score10 = shotScoreSlots[10] + (shotScoreSlots[9] + shotScoreSlots[11]) / 3;
            float score11 = shotScoreSlots[11] + (shotScoreSlots[10] + shotScoreSlots[12]) / 3;
            float score12 = shotScoreSlots[12] + (shotScoreSlots[11] + shotScoreSlots[13]) / 3;
            float score13 = shotScoreSlots[13] + (shotScoreSlots[12] + shotScoreSlots[14]) / 3;
            float score14 = shotScoreSlots[14] + (shotScoreSlots[13] + shotScoreSlots[15]) / 3;
            float score15 = shotScoreSlots[15] + (shotScoreSlots[14] + shotScoreSlots[16]) / 3;
            float score16 = shotScoreSlots[16] + (shotScoreSlots[15] + shotScoreSlots[17]) / 3;
            float score17 = shotScoreSlots[17] + (shotScoreSlots[16] + shotScoreSlots[0]) / 3;


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
//            System.out.println(" 1:" + score1 + " 2:" + score2+ " 3:" + score3+ " 4:" + score4+ " 5:" + score5+ " 6:" + score6+ " 7:" + score7+ " 8:" + score8+ " 9:" + score9+ " 10:" + score10+ " 11:" + score11+ " 12:" + score12+ " 13:" + score13+ " 14:" + score14+ " 15:" + score15+ " 16:" + score16+ " 17:" + score17 );

//            System.out.println("WAAAT  :" + bestScore +  "slot:" + bestMove);

            int treeCount = rc.getTreeCount();

            if(treeCount > 5 || rc.getRobotCount() <= 2){
                bestScore += 3;
            } else if(treeCount > 2){
                bestScore += 2;
            } else if(treeCount >= 1){
                bestScore++;
            }

            if (bestScore >= 5) {
                if (representativeForSlot[bestSlot] != null) {
                    rc.fireSingleShot(representativeForSlot[bestSlot]);
//                    System.out.println("FIRED!!!");
                    if(!rc.hasMoved()){
                        addCircularDesireZone(myLocation.add(representativeForSlot[bestSlot],1.05f),1.02f,-200);
                        addDesireZone(myLocation.add(representativeForSlot[bestSlot],1.75f),1.1f,-200);//Doesn;t seem neccessary, but we've had some bullet collision issues.
                    }
                }
            }
        }



    }



    private void initialOwnScoutBroadcast() throws GameActionException {
            if (rc.readBroadcast(OUR_SCOUT_1_ID) <= 0 || turn - rc.readBroadcast(OUR_SCOUT_1_LASTSEEN) >= 2) {
                our_scout_nr = 1;
                rc.broadcast(OUR_SCOUT_1_ID, rc.getID());
                //  System.out.println("Scout init as 1");
            } else if (rc.readBroadcast(OUR_SCOUT_2_ID) <= 0 || turn - rc.readBroadcast(OUR_SCOUT_2_LASTSEEN) >= 2) {
                our_scout_nr = 2;
                rc.broadcast(OUR_SCOUT_2_ID, rc.getID());
                // System.out.println("Scout init as 2");
            } else if (rc.readBroadcast(OUR_SCOUT_3_ID) <= 0 || turn - rc.readBroadcast(OUR_SCOUT_3_LASTSEEN) >= 2) {
                our_scout_nr = 3;
                rc.broadcast(OUR_SCOUT_3_ID, rc.getID());
                //isExplorer = true;
                //   System.out.println("Scout init as 3");
            }

    }

    private void broadcastOwnLocation() throws GameActionException{
        if (our_scout_nr == 1) {
            rc.broadcast(OUR_SCOUT_1_X, (int) myLocation.x);
            rc.broadcast(OUR_SCOUT_1_Y, (int) myLocation.y);
            rc.broadcast(OUR_SCOUT_1_LASTSEEN, turn);
        } else if (our_scout_nr == 2) {
            rc.broadcast(OUR_SCOUT_2_X, (int) myLocation.x);
            rc.broadcast(OUR_SCOUT_2_Y, (int) myLocation.y);
            rc.broadcast(OUR_SCOUT_2_LASTSEEN, turn);
        } else if (our_scout_nr == 3) {
            rc.broadcast(OUR_SCOUT_3_X, (int) myLocation.x);
            rc.broadcast(OUR_SCOUT_3_Y, (int) myLocation.y);
            rc.broadcast(OUR_SCOUT_3_LASTSEEN, turn);
        } else{
            initialOwnScoutBroadcast();
        }
    }



    
    

    
    public void initial() throws GameActionException{

        maxHp = RobotType.SCOUT.maxHealth;
        radius =  RobotType.SCOUT.bodyRadius;
        sightradius = RobotType.SCOUT.sensorRadius;
        bulletsightradius = RobotType.SCOUT.bulletSightRadius;
        maxMove = RobotType.SCOUT.strideRadius;
        bulletSpeed = RobotType.SCOUT.bulletSpeed;
        attackpower = RobotType.SCOUT.attackPower;
        initialOwnScoutBroadcast();
    }
}
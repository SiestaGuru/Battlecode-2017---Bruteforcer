package bruteforcer;

import battlecode.common.*;

/**
 * Created by Hermen on 10/1/2017.
 */
public class Gardener extends RobotBase {

    static int amountBuild = 0;

    static int attemptedTankBuild = 0;

    static int enemiesNear = 0;
    static int scaryEnemiesNear = 0;

    static int nonScoutsNear = 0;
    static int totalTrees = 0;
    static int totalNonAlliedTrees = 0;
    static int totalNonAlliedTreesClose = 0;
    static float totalNonAlliedTreesRadiusArea = 0;

    static int treesWithStuff = 0;

    static int totalBlockingUnits = 0;

    static int lowHealthTreesDetected = 0;

    static MapLocation mainaggr;


    static boolean newBaseBuilder = false;


    static MapLocation treeBuildSpot = null;

    static Direction treeStartDir = null;

    static int turnsAway = 0;
    static int treeBuildFailures = 0;
    static int unitBuildFailures = 0;

    static int gardenerNr = 0;

    static boolean letWither = false;
    static int witherTreeID = -1;
    static MapLocation reservedSpot = null;
    static int lastPickedWither = -1;


    static int directionDecisionCooldown = 0;
    static int turnsNotStuckToTree = 0;
    static int treesBuildFromSpot = 0;
    static float healthLastTurn = 40;

    static int lastbuildscout = -20;


    static int turnBuild = -1;
    public void step() throws Exception {

        directionDecisionCooldown--;
        turnsNotStuckToTree++;
        becomeCommanderIfNeccessary();
        MapLocation myLocation = RobotBase.myLocation;
        RobotController rc = RobotBase.rc;


        enemiesNear = 0;
        nonScoutsNear = 0;
        totalTrees = 0;
        totalBlockingUnits = 0;
        totalNonAlliedTrees = 0;
        totalNonAlliedTreesRadiusArea = 0;
        totalNonAlliedTreesClose = 0;
        treesWithStuff = 0;
        scaryEnemiesNear = 0;
        detectedScout = null;
        lowHealthTreesDetected = 0;


        int minimumfloatingbullets = rc.readBroadcast(KEEP_BULLETS_FLOATING);

        float bullets = rc.getTeamBullets() - minimumfloatingbullets;

        updateMapSize();
        mainaggr = new MapLocation(rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_X), rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_Y));

        if (turn % 5 == 0) {
            treeBuildFailures--;
            unitBuildFailures--;
            if(treeBuildFailures <0){
                treeBuildFailures =0;
            }
            if(unitBuildFailures <0){
                unitBuildFailures=0;
            }
        }


        boolean stillFindingSpot1 = false;
        boolean stillFindingSpot2 = false;

        boolean relevant = (newBaseBuilder || rc.readBroadcast(GARDENERS_REPORTING_IN_TOTAL) >= 2);
        if (turn - turnBuild < 30 && relevant) {
            stillFindingSpot1 = true;
        }
        if (turn - turnBuild < 20 && relevant) {
            stillFindingSpot2 = true;
        }

        dodgeBullets(rc.senseNearbyBullets());


        boolean shouldConfirmEnemyLoc = false;
        boolean shouldConfirmEnemyTreeLoc = false;
        if (rc.readBroadcast(CONFIRMED_ENEMY_TURN) < turn) {
            shouldConfirmEnemyLoc = true;
        }
        if (rc.readBroadcast(CONFIRMED_ENEMY_TREE_TURN) < turn) {
            shouldConfirmEnemyTreeLoc = true;
        }


        //Only report in every couple of turns to confuse some players (who might depend too much on broadcasts every turn)
        if (turn % 3 == 0) {
            if (rc.readBroadcast(GARDENERS_REPORTING_IN_TURN) != turn) {
                rc.broadcast(GARDENERS_REPORTING_IN_TOTAL, rc.readBroadcast(GARDENERS_REPORTING_IN));
                rc.broadcast(GARDENERS_REPORTING_IN_TURN, turn);
                rc.broadcast(GARDENERS_REPORTING_IN, 1);

            } else {
                rc.broadcast(GARDENERS_REPORTING_IN, rc.readBroadcast(GARDENERS_REPORTING_IN) + 1);
            }
            if (turn - rc.readBroadcast(HOME_LASTUPDATE) > 2) {
                updateMapSize();
                rc.broadcast(HOME_X, (int) myLocation.x);
                rc.broadcast(HOME_Y, (int) myLocation.y);
                rc.broadcast(HOME_LASTUPDATE, turn);

                rc.broadcast(OPPOSITE_HOME_X, (int) (mapLowestX + (mapHighestX - myLocation.x)));
                rc.broadcast(OPPOSITE_HOME_Y, (int) (mapLowestY + (mapHighestY - myLocation.y)));
            }
        }


        //If we are stuck and can't build any units, we should get rid of some trees
        letWither = false;
            if (bullets > 400 && unitBuildFailures > 35) {
                if (rc.readBroadcast(VICTORY_POINTS_MODE) == 0) {
                    if (turn - rc.readBroadcast(LAST_TURN_BUILD_SOMETHING) > 25) {
                        int totalGardners = rc.readBroadcast(GARDENERS_REPORTING_IN_TOTAL);
                        if (totalGardners < 2) {
                            letWither = true;
                        } else if (totalGardners < 5 && gardenerNr % 2 == 0) {
                            letWither = true;
                        } else if (gardenerNr % 4 == 0) {
                            letWither = true;
                        }
                    }
                }
            }



        //Select what tree we should stop watering
        if (letWither) {
            TreeInfo[] witherableTrees = rc.senseNearbyTrees(2);
            if (turn - lastPickedWither > 50) {
                int bestId = -1;
                float bestScore = 99999;

                for (int i = 0; i < witherableTrees.length; i++) {
                    if (witherableTrees[i].team.equals(ally)) {
                        MapLocation loc = witherableTrees[i].location;
                        float dist = loc.distanceTo(mainaggr);
                        if (dist < bestScore) {
                            for (int i2 = 0; i2 < 36; i2++) {
                                if (!rc.isCircleOccupied(loc.add(Direction.NORTH.rotateRightDegrees(10 * i2), 2.05f), 1)) {
                                    reservedSpot = loc;
                                    lastPickedWither = turn;
                                    bestScore = dist;
                                    bestId = witherableTrees[i].ID;
                                    break;
                                }
                            }
                        }
                    }
                }


                if(bestId >= 0){
                    witherTreeID = bestId;
                }

            }




        } else {
            if (reservedSpot != null && turn - lastPickedWither > 100 && rc.readBroadcast(GARDENERS_REPORTING_IN_TOTAL) > 1) {
                lastPickedWither = -1;
                reservedSpot = null;
            }
        }



        TreeInfo closeTree = null;
        TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
        boolean doingTreeMovement = false;
        int bestWaterID = -1;
        float bestHealth = 10000;

        for (int i = 0; i < nearbyTrees.length && i < 15; i++) {
            TreeInfo tree = nearbyTrees[i];
            totalTrees++;
            MapLocation loc = tree.location;
            float distance = loc.distanceTo(myLocation);
            if (distance < tree.radius + 1.7f) {
                if (closeTree == null) {
                    closeTree = tree;
                }


                //Old version of bugpathing. The one in soldier is better
                if (!doingTreeMovement && specialMapLocationsCount == 0) {
                    Direction vectorDir = myLocation.directionTo(new MapLocation(myLocation.x + moveVectorX, myLocation.y + moveVectorY));
                    MapLocation directionDesire = myLocation.add(vectorDir, 0.1f);

                    float treeToApproxDist = tree.location.distanceTo(directionDesire);
                    if (distance > treeToApproxDist) {
                        if (directionLeaning == -1 || directionDecisionCooldown <= 0 || turnsNotStuckToTree > 4) {
                            // Pick either rightleaning or leftleaning, depending on which is closer and create some vector in that direction. Stick to our choice for a bit
                            MapLocation turnLeftLoc = myLocation.add(myLocation.directionTo(loc).rotateLeftDegrees(30), 0.1f);
                            MapLocation turnRightLoc = myLocation.add(myLocation.directionTo(loc).rotateRightDegrees(30), 0.1f);

                            if (turnLeftLoc.distanceTo(directionDesire) < turnRightLoc.distanceTo(directionDesire)) {
                                directionLeaning = 0;
                            } else {
                                directionLeaning = 1;
                            }
                            directionDecisionCooldown = 40;
                        }
                        MapLocation directionPicker;
                        for (int attempt = 0; attempt < 17; attempt++) {
                            Direction newDir;
                            if (directionLeaning == 0) {
                                newDir = new Direction(vectorDir.radians + (attempt * 0.1745f));
                            } else {
                                newDir = new Direction(vectorDir.radians - (attempt * 0.1745f));
                            }

                            directionPicker = myLocation.add(newDir, maxMove - 0.01f);

                            if (rc.canMove(directionPicker)) {
                                doingTreeMovement = true;
                                addSpecialMapLocation(directionPicker, 20);
                                addVector(directionPicker, 2);
                                break;
                            }
                        }
                        turnsNotStuckToTree = -1;
                    }
                }
            }


            if (tree.team.equals(ally)) {
                if (!letWither || tree.ID != witherTreeID) {
                    if (rc.canWater(tree.ID)) {
                        if (tree.health < bestHealth) {
                            bestHealth = tree.health;
                            bestWaterID = tree.ID;
                        }
                    }
                    if (tree.health <= 45) {
                        addVector(tree.location, 6);
                        if (tree.health < 43) {
                            lowHealthTreesDetected++;
                        }
                    }
                }
            } else {
                if (tree.containedBullets > 0) {
                    if (rc.canShake(tree.ID)) {
                        rc.shake(tree.ID);
                    } else if (treeBuildSpot == null) {
                        addVector(tree.getLocation(), 6);
                    }
                }

                if (tree.containedRobot != null) {
                    treesWithStuff++;
                }

                float dist = myLocation.distanceTo(tree.location);
                if (dist < tree.radius + 4.1f) {
                    addCircularDesireZone(tree.location, tree.radius + 3.1f, -25);
                    addVector(tree.location, -2);
                    if (dist < tree.radius + 3.1f) {
                        totalNonAlliedTreesClose += 1;
                    }
                }
                totalNonAlliedTrees++;
                totalNonAlliedTreesRadiusArea += (tree.radius * tree.radius);

                if (shouldConfirmEnemyTreeLoc) {
                    if (tree.team.equals(enemy)) {
                        shouldConfirmEnemyTreeLoc = false;
                        rc.broadcast(CONFIRMED_ENEMY_TREE_X, (int) tree.location.x);
                        rc.broadcast(CONFIRMED_ENEMY_TREE_Y, (int) tree.location.y);
                        rc.broadcast(CONFIRMED_ENEMY_TREE_TURN, turn);
                    }
                }
            }
        }

        if (bestWaterID >= 0) {
            rc.water(bestWaterID);
        }

        //Avoid combat
        int DefMainX = rc.readBroadcast(Archon.MAIN_DEFENSIVE_TARGET_X);

        if (DefMainX >= 0) {
            addVectorSqrt(DefMainX, rc.readBroadcast(Archon.MAIN_DEFENSIVE_TARGET_Y), -0.5f);
        }


        if (mainaggr.x >= 0) {
            addVectorSqrt(mainaggr, -1);
        }


        //Go Home
        if (!newBaseBuilder) {
            addVector(rc.readBroadcast(HOME_X), rc.readBroadcast(HOME_Y), 1.5f);
            desireTowardsMiddle(-1.5f);
        } else {
            addVector(rc.readBroadcast(HOME_X), rc.readBroadcast(HOME_Y), -3f);
            if (rc.getID() % 2 == 0) {
                addVectorSqrt(rc.readBroadcast(OPPOSITE_HOME_X), rc.readBroadcast(HOME_Y), 3f);
            } else {
                addVectorSqrt(rc.readBroadcast(HOME_X), rc.readBroadcast(OPPOSITE_HOME_Y), 3f);
            }
            desireTowardsMiddle(-2.5f);
        }

        boolean foundNearGardener = false;
        boolean foundNearArchon = false;
        boolean updatedDefenseTarget = false;

        RobotInfo archon = null;
        RobotInfo enemy = null;

        RobotInfo[] robots = rc.senseNearbyRobots();
        friendlyForceDetectedThisTurn = 0;
        for (int i = 0; i < robots.length && i < 15; i++) {

            RobotInfo r = robots[i];
            if (!r.type.equals(RobotType.SCOUT)) {
                totalBlockingUnits++;
            }

            if (!r.team.equals(ally)) {
                addVectorSqrt(r.location, -4);

                if (!(r.type.equals(RobotType.ARCHON) || r.type.equals(RobotType.GARDENER))) {
                    if (enemy == null) {
                        enemy = r;
                    }
                    enemiesNear++;
                    if (!r.type.equals(RobotType.SCOUT)) {
                        nonScoutsNear++;
                        scaryEnemiesNear++;
                    } else {
                        detectedScout = r;
                    }

                }

                if(!updatedDefenseTarget) {
                    if (r.type.equals(RobotType.SOLDIER) || r.type.equals(RobotType.TANK)) {
                        if (rc.readBroadcast(DEFENSE_LEVEL) < 8 || turn - rc.readBroadcast(DEFENSIVE_TARGETS_LAST_UPDATE) > 2 || rc.readBroadcast(IS_REAL_DEF_TARGET) == 0) {
                            rc.broadcast(DEFENSE_LEVEL, 8);
                            rc.broadcast(DEFENSIVE_TARGETS_LAST_UPDATE, turn);
                            rc.broadcast(MAIN_DEFENSIVE_TARGET_X, (int)(r.location.x + 0.5f));
                            rc.broadcast(MAIN_DEFENSIVE_TARGET_Y, (int)(r.location.y + 0.5f));
//                            rc.setIndicatorLine(myLocation,r.location,24,100,255);
                            rc.broadcast(IS_REAL_DEF_TARGET, 1);
                            updatedDefenseTarget = true;
                        }
                    }
                }


                if (shouldConfirmEnemyLoc) {
                    if (!r.getType().equals(RobotType.SCOUT)) {
                        rc.broadcast(CONFIRMED_ENEMY_X, (int) r.location.x);
                        rc.broadcast(CONFIRMED_ENEMY_Y, (int) r.location.y);
                        rc.broadcast(CONFIRMED_ENEMY_TURN, turn);
                        shouldConfirmEnemyLoc = false;
                    }
                }
                if (r.type.equals(RobotType.TANK)) {
                    if (rc.readBroadcast(CONFIRMED_ENEMY_TANK_TURN) != turn) {
                        rc.broadcast(CONFIRMED_ENEMY_TANK_X, (int) (r.location.x * 10));
                        rc.broadcast(CONFIRMED_ENEMY_TANK_Y, (int) (r.location.y * 10));
                        rc.broadcast(CONFIRMED_ENEMY_TANK_TURN, turn);
                    }
                } else if (r.type.equals(RobotType.GARDENER)) {
                    if (rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_TURN) != turn) {
                        rc.broadcast(CONFIRMED_ENEMY_GARDENER_X, (int) (r.location.x * 100));
                        rc.broadcast(CONFIRMED_ENEMY_GARDENER_Y, (int) (r.location.y * 100));
                        rc.broadcast(CONFIRMED_ENEMY_GARDENER_TURN, turn);
                    }
                }
            } else {

                if (r.type.equals(RobotType.GARDENER)) {
                    if (r.location.distanceTo(myLocation) <= 6) {
                        if (!stillFindingSpot2) {
                            addDesireZone(r.getLocation(), r.getRadius() + 3f, -30); //Don't stack with other gardeners, causes overpacked build areas where no gardener can build anything
                            addDesireZone(r.getLocation(), r.getRadius() + 4f, -15); //Don't stack with other gardeners, causes overpacked build areas where no gardener can build anything
                            addVectorSqrt(r.getLocation(), -5f);
                        } else {
                            addVector(r.getLocation(), -3);
                        }
                        foundNearGardener = true;
                    }
                } else {
                    float distance = r.location.distanceTo(myLocation);
                    if (distance < 5) {
                        if (!stillFindingSpot2) {
                            addDesireZone(r.getLocation(), r.getRadius() + 2f, -8); //Don't stack with own units, or it'll be hard to build
                        }
                        addVector(r.getLocation(), -1);
                    }

                    if (!r.type.equals(RobotType.ARCHON)) {
                        friendlyForceDetectedThisTurn++;
                    } else {
                        addVector(r.getLocation(), -3);
                        if (r.location.distanceTo(myLocation) <= 5) {
                            foundNearArchon = true;
                            if (archon == null) {
                                archon = r;
                            }
                        }

                    }
                }

            }
        }


        if (archon != null && enemy != null) {
            //Try to hide behind our archon friend. Haven't really seen this work properly
            MapLocation enemyLoc = enemy.location;
            MapLocation archonLoc = archon.location;
            Direction dir = enemyLoc.directionTo(archonLoc);
            float dist = enemyLoc.distanceTo(archonLoc);

            MapLocation rightBehind = enemyLoc.add(dir, dist + archon.getRadius() + 1.01f);

            if (myLocation.distanceTo(rightBehind) < 1) {
//                rc.setIndicatorDot(myLocation, 255, 128, 240);
                addVector(rightBehind, 4);
                if (myLocation.distanceTo(rightBehind) < maxMove && rc.canMove(rightBehind)) {
                    addSpecialMapLocation(rightBehind, 20);
                }
            }
        }
        if (enemy != null && closeTree != null) {
            //Try to hide behind our tree friend. Haven't really seen this work properly
            MapLocation enemyLoc = enemy.location;
            MapLocation treeLoc = closeTree.location;
            Direction dir = enemyLoc.directionTo(treeLoc);
            float dist = enemyLoc.distanceTo(treeLoc);

            MapLocation rightBehind = closeTree.location.add(dir, dist + closeTree.radius + 1.01f);

            if (myLocation.distanceTo(rightBehind) < 1) {
//                rc.setIndicatorLine(myLocation, treeLoc, 255, 128, 240);
                addVector(rightBehind, 4);
                if (myLocation.distanceTo(rightBehind) < maxMove && rc.canMove(rightBehind)) {
                    addSpecialMapLocation(rightBehind, 20);
                }
            }
        }

        if (mapSizeType >= MEDIUM) {
            if (friendlyForceDetectedThisTurn <2) {
                if (rc.readBroadcast(SOLDIERS_BUILD) > 2) {
                    if (turn - rc.readBroadcast(GUARD_JOB_TURN) > 1) {
                        rc.broadcast(GUARD_JOB_TURN, turn);
                        MapLocation guardSpot;

                        if (treeStartDir != null) {
                            guardSpot = myLocation.add(treeStartDir.rotateRightDegrees(60), 4.1f);
                        } else {
                            if (mainaggr.x >= 0) {
                                guardSpot = myLocation.add(myLocation.directionTo(mainaggr), 4.1f);
                            } else {
                                guardSpot = myLocation.add(myLocation.directionTo(new MapLocation((mapLowestX + mapHighestX) / 2, (mapLowestY + mapHighestY) / 2)), 4.1f);
                            }
                        }

                        if(rc.canSenseLocation(guardSpot) && rc.onTheMap(guardSpot) && !rc.isLocationOccupiedByTree(guardSpot)) {

                            rc.broadcast(GUARD_JOB_X, (int) guardSpot.x);
                            rc.broadcast(GUARD_JOB_Y, (int) guardSpot.y);

//                            rc.setIndicatorDot(guardSpot, 255, 150, 150);
                        }
                    }
                }
            }
        }


        //We assign some gardeners that try to move further from the archon to plant their trees. Helps spreading, and sometimes we end up with
        //tree clusters the enemy doesn't find.
        if (newBaseBuilder) {
            if (stillFindingSpot2) {
                addVector(getRandomGoal(), 6f); //A bit of randomness does wonders
            } else {
                addVector(getRandomGoal(), 2f); //A bit of randomness does wonders
            }
        } else {
            if (stillFindingSpot2) {
                addVector(getRandomGoal(), 3f); //A bit of randomness does wonders
            } else {
                addVector(getRandomGoal(), 0.6f); //A bit of randomness does wonders
            }
        }


        if (detectedScout != null) {
            rc.broadcast(DETECTED_SCOUT_TURN, turn);
            rc.broadcast(DETECTED_SCOUT_X, (int) detectedScout.location.x);
            rc.broadcast(DETECTED_SCOUT_Y, (int) detectedScout.location.y);
        }



        desireFarEdges(-20, 3.1f); //Standing next to the edge makes us unable to build trees

        int lastEconUpdate = rc.readBroadcast(ECON_GOALS_LAST_UPDATE);

        int lumberjack = 0;
        int soldier = 0;
        int scout = 0;
        int tank = 0;
        int tree = 0;


        int unitproducer = -1;

        if (mainaggr.x > 0) {
            if (rc.readBroadcast(GARDENERS_REPORTING_IN) > 3) {
                float distance = mainaggr.distanceTo(myLocation);

                if (rc.readBroadcast(UNIT_PRODUCER_GARDENER) == rc.getID()) {
                    unitproducer = 1;
                } else if (rc.readBroadcast(UNIT_PRODUCER_GARDENER_DISTANCE) < distance) {
                    unitproducer = 1;
                    rc.broadcast(UNIT_PRODUCER_GARDENER_DISTANCE, (int) distance);
                    rc.broadcast(UNIT_PRODUCER_GARDENER, rc.getID());
                } else {
                    unitproducer = 0;
                }
            }
        }
        if (turn - lastEconUpdate > 10) {
            //Well, I guess we just spam soldiers and hope we win (probably doesn't happen anymore with distributed commander tasks)
            soldier = 40;
        } else {
            lumberjack = rc.readBroadcast(LUMBERJACK_DESIRE);
            soldier = rc.readBroadcast(SOLDIER_DESIRE);
            scout = rc.readBroadcast(SCOUT_DESIRE);
            tank = rc.readBroadcast(TANK_DESIRE);
            tree = rc.readBroadcast(TREE_DESIRE);
        }


        //Okay, we got our prefered weights from the archons, but we need to adjust them for our local area. So we build lumberjacks on the right gardeners etc

        if (rc.getRobotCount() < 10) {
            tree -= enemiesNear * 2;
            scout -= enemiesNear * 2;
        }

        if (totalNonAlliedTrees < 4) {
            lumberjack += totalNonAlliedTrees;
        } else {
            lumberjack += 4;
        }
        if (totalNonAlliedTreesClose > 0) {
            lumberjack += 2;
            if (totalNonAlliedTreesClose > 2) {
                lumberjack += 4;
                if (totalNonAlliedTreesClose > 3) {
                    lumberjack += 2;
                }
            }
        }
        lumberjack += treesWithStuff * 5;

        if (treeBuildSpot != null) {
            tree += 2; //Let's fill this one up fast and let other gardeners handle the units
        }
        if (foundNearGardener) {
            tree -= 15; //Yeah no.. , we'll get stuck
        }

        if (stillFindingSpot1) {
            tree -= 10;//Give some space to move first
            // rc.setIndicatorDot(myLocation,0,0,0);
        }

        if(turn - lastbuildscout < 20){
            scout -= 20;//Prevent the duble/triple scouts from spawning when not appropriate
        }

        scout += enemyScoutsDetectedThisTurn * 2.5;  //Probably best antiscout is scout
        lumberjack += enemyScoutsDetectedThisTurn;
        soldier += enemyScoutsDetectedThisTurn;


        //Its too busy here to build these units, let other gardeners handle it
        if (totalTrees + totalBlockingUnits > 10) {
            tank -= 10;
        }

        if(totalTrees + totalBlockingUnits > 15){
            soldier -= 4;
        }


        if (totalTrees + totalBlockingUnits < 3) {
            tank += 3;
        }
        if (unitproducer != 1 && totalTrees + totalBlockingUnits > 22) {
            soldier -= 10;
            tree += 5;
            scout -= 3;//To not waste all money on scouts when we dont actually want them
            if (totalTrees + totalBlockingUnits - totalNonAlliedTrees > 22) {
                lumberjack -= 10;
            }
            tank -= 10;
        }

        if (totalNonAlliedTreesRadiusArea > 4) {
            lumberjack += 3;
        }


        if (turn > 300 && turn - rc.readBroadcast(LAST_TURN_BUILD_SOMETHING) > 20 && unitBuildFailures > 10) {
            tree -= 10; //To stop it from instantly planting a tree after withering
        }
        if (letWither) {
            tree -= 5;
        }

        if (myLocation.x - mapLowestX < 6 || myLocation.y - mapLowestY < 6 || mapHighestY - myLocation.y < 6 || mapHighestX - myLocation.x < 6) {
            tank -= 8; //Don't build these on the sides of maps, more likely to get stuck
        }

        boolean allowFinalSpot = false;

        if (scaryEnemiesNear > 0) {

            if (treesBuildFromSpot == 5 && enemyScoutsDetectedThisTurn == 0) {
                //Close the tree wall
                tree += 30;
                allowFinalSpot = true;
                // rc.setIndicatorDot(myLocation,0,255,0);
            } else {
                lumberjack -= 2;
                scout -= 7;
                soldier += 8;
                tree -= 7;
                tank -= 10; //Dont want to get these insta killed
            }
        } else if (treesBuildFromSpot == 5 && enemyScoutsDetectedThisTurn == 0 && rc.getHealth() < 6) {
            if (rc.readBroadcast(GARDENERS_REPORTING_IN_TOTAL) > 1) {
                tree += 30;
                allowFinalSpot = true;
            }
        }


        //0 = soldier, 1 = lumber, 2 = scout, 3= tank, 4 = tree   and -1 for nothing (wait/get victory points)
        int highest = -1;
        int highestscore = 6;


        if (soldier > highestscore) {
            highest = 0;
            highestscore = soldier;
        }
        if (lumberjack > highestscore) {
            highest = 1;
            highestscore = lumberjack;
        }
        if (scout > highestscore) {
            highest = 2;
            highestscore = scout;
        }
        if (attemptedTankBuild < 8 && tank > highestscore) {
            highest = 3;
            highestscore = tank;
        }
        if (tree > highestscore) {
            highest = 4;


        }


        Direction bestUnitDirection;
        if (enemy != null) {
            //Probably plant on the opposite side if possible so we shield it from insta death for a moment while it's spawning
            bestUnitDirection = enemy.location.directionTo(myLocation);
        } else if (mainaggr.x >= 0) {
            bestUnitDirection = myLocation.directionTo(mainaggr);
        } else {
            bestUnitDirection = randomDirection();
        }
        //Turns it into a neat 60 degrees based system, makes it interlock neater with other gardeners
        bestUnitDirection = bestUnitDirection.rotateRightDegrees(bestUnitDirection.getAngleDegrees() - ((int) ((bestUnitDirection.getAngleDegrees() + 30) / 60f)) * 60);

        if (rc.getBuildCooldownTurns() == 0) {

            RobotType type = null;
            if (highest == 0) {
                type = RobotType.SOLDIER;
            } else if (highest == 1) {
                type = RobotType.LUMBERJACK;
            } else if (highest == 2) {
                type = RobotType.SCOUT;
            } else if (highest == 3) {
                type = RobotType.TANK;
            }
            if (type != null) {
                if (bullets > type.bulletCost) {
                    Direction bestDir = findAvailableRobotBuildDir(type, bestUnitDirection);
                    if (bestDir != null) {
                        //We've identified a spot we can build at, but, do we want to? If the unit gets instantly stuck we probably don't want to
                        MapLocation bestSpot = myLocation.add(bestDir, 1 + type.bodyRadius + GameConstants.GENERAL_SPAWN_OFFSET);
                        boolean spotFree = false;
                        if (type.equals(RobotType.SCOUT)) {
                            for (int i = 0; i < 36; i++) {

                                MapLocation m = bestSpot.add(Direction.NORTH.rotateRightDegrees(10 * i), type.strideRadius);
                                if (!rc.isLocationOccupiedByRobot(m) && rc.onTheMap(m,type.bodyRadius)) {
                                    spotFree = true;
                                    break;
                                }
                            }
                        } else if (type.equals(RobotType.LUMBERJACK)) {
                            for (int i = 0; i < 16; i++) {
                                MapLocation spot = bestSpot.add(Direction.NORTH.rotateRightDegrees((float) 22.5 * i), type.strideRadius);
                                if (!rc.isLocationOccupiedByRobot(spot) && rc.onTheMap(spot,type.bodyRadius)) {
                                    if (rc.isLocationOccupiedByTree(spot)) {
                                        TreeInfo t = rc.senseTreeAtLocation(spot);
                                        if (!t.team.equals(ally)) {
                                            spotFree = true;
                                            break;
                                        }
                                    } else {
                                        spotFree = true;
                                        break;
                                    }
                                }
                            }
                        } else {
                            for (int i = 0; i < 36; i++) {
                                MapLocation m = bestSpot.add(Direction.NORTH.rotateRightDegrees(10 * i), type.strideRadius);
                                if (!rc.isCircleOccupied(m, type.bodyRadius) && rc.onTheMap(m,type.bodyRadius)) {
                                    spotFree = true;
                                    break;
                                }
                            }
                        }
                        if (spotFree) {
                            rc.buildRobot(type, bestDir);
                            attemptedTankBuild = 0;

                            if(type.equals(RobotType.SCOUT)){
                                lastbuildscout = turn;
                            }
                                rc.broadcast(LAST_TURN_BUILD_SOMETHING, turn);

                                switch (type) {
                                    case SOLDIER:
                                        rc.broadcast(SOLDIERS_BUILD, rc.readBroadcast(SOLDIERS_BUILD) + 1);
                                        break;
                                    case SCOUT:
                                        rc.broadcast(SCOUTS_BUILD, rc.readBroadcast(SCOUTS_BUILD) + 1);
                                        break;
                                    case LUMBERJACK:
                                        rc.broadcast(LUMBERJACKS_BUILD, rc.readBroadcast(LUMBERJACKS_BUILD) + 1);
                                        break;
                                    case TANK:
                                        rc.broadcast(TANKS_BUILD, rc.readBroadcast(TANKS_BUILD) + 1);
                                        break;
                                }

                        } else {
//                            rc.setIndicatorDot(bestGardenerHexSpot, 123, 34, 234);
                            if (type.equals(RobotType.TANK)) {
                                attemptedTankBuild++;
                            }
                            unitBuildFailures++;
                        }
                    } else {
                        if (type.equals(RobotType.TANK)) {
                            attemptedTankBuild++;
                        }
                        unitBuildFailures++;
                    }
                }
            }
        }



        if (highest == 4 && bullets > 50) {

            if (treeStartDir == null) {
                if (totalNonAlliedTreesClose < 2) {
                    treeStartDir = bestUnitDirection;
                } else {
                    treeStartDir = bestUnitDirection.rotateRightDegrees(60);

                }
            }


            if (treeBuildFailures < 10) {
                if (buildTree(treeStartDir, allowFinalSpot)) {
                    attemptedTankBuild = 0;

                    if (treeBuildSpot != null) {
                        if (myLocation.distanceTo(treeBuildSpot) < 0.1f) {
                            treesBuildFromSpot++;
//                                System.out.println("adding 1, is now " + treesBuildFromSpot);
                        }
                    } else if (!foundNearGardener) {
                        //if(rc.readBroadcast(GARDENERS_REPORTING_IN) > 1) {
                        if (lowHealthTreesDetected == 0) {
                            treeBuildSpot = myLocation;
                            treesBuildFromSpot = 1;

                        }

                    }

                        //rc.broadcast(LAST_TURN_BUILD_SOMETHING, turn);

                        rc.broadcast(TREES_BUILD, rc.readBroadcast(TREES_BUILD) + 1);

                } else {

                    if (rc.getBuildCooldownTurns() == 0) {
                        treeBuildFailures++;
                    }

                }
            } else {
                if (buildTreeRandom(treeStartDir)) {
                    attemptedTankBuild = 0;
                        rc.broadcast(TREES_BUILD, rc.readBroadcast(TREES_BUILD) + 1);
                        //   rc.broadcast(LAST_TURN_BUILD_SOMETHING, turn);

                }
            }

        }

        if (treeBuildFailures < 10) {

            if (treeBuildSpot != null) {
                //  rc.setIndicatorDot(treeBuildSpot,255,0,0);
                if (myLocation.distanceTo(treeBuildSpot) > 1) { //meh, we moved. shame..
                    turnsAway++;
                    if (turnsAway > 8) {
                        treeBuildSpot = null;
                    } else {
                        addVector(treeBuildSpot, 8);
                    }
                } else if (myLocation.distanceTo(treeBuildSpot) > maxMove) {
                    addVector(treeBuildSpot, 8);
                    turnsAway = 0;
                } else {
                    turnsAway = 0;
                    if (rc.canMove(treeBuildSpot)) {
                        if (rc.readBroadcast(GARDENERS_REPORTING_IN) > 1) {
                            addSpecialMapLocation(treeBuildSpot, 60);
                        } else {
                            addSpecialMapLocation(treeBuildSpot, 40);
                        }
                    }
                }
            }
        }

        if (turn < 40 && treeBuildSpot == null) {
            //Run away quickly from our archon at the start to make enemy rushes less effective

            MapLocation archon1 = new MapLocation(rc.readBroadcast(OUR_ARCHON_1_X), rc.readBroadcast(OUR_ARCHON_1_Y));
            if (myLocation.distanceTo(archon1) < 15) {
                addVector(archon1, -20);
//                rc.setIndicatorLine(myLocation, archon1, 123, 255, 200);
            }

            int archon2x = rc.readBroadcast(OUR_ARCHON_2_X);

            if (archon2x > 0) {
                MapLocation archon2 = new MapLocation(archon2x, rc.readBroadcast(OUR_ARCHON_2_Y));
                if (myLocation.distanceTo(archon2) < 15) {
                    addVector(archon2, -20);
//                    rc.setIndicatorLine(myLocation, archon2, 123, 255, 200);

                }
            }
            int archon3x = rc.readBroadcast(OUR_ARCHON_3_X);
            if (rc.readBroadcast(OUR_ARCHON_3_ID) > -2) {
                MapLocation archon3 = new MapLocation(archon3x, rc.readBroadcast(OUR_ARCHON_3_Y));
                if (myLocation.distanceTo(archon3) < 15) {
                    addVector(archon3, -20);
//                    rc.setIndicatorLine(myLocation, archon3, 123, 255, 200);

                }
            }

            desireTowardsMiddle(-15);
        }
        if (Clock.getBytecodesLeft() > 2000) {
            doMovement(650);
        }

        healthLastTurn = rc.getHealth();
    }




    public Direction findAvailableRobotBuildDir(RobotType robot, Direction dir) throws GameActionException{

        if (rc.canBuildRobot(robot, dir)) {
            return dir;
        }

        // Now try a bunch of similar angles
        int currentCheck = 1;

        while(currentCheck<=18) {
            // Try the offset of the left side
            if(rc.canBuildRobot(robot,dir.rotateLeftDegrees(10*currentCheck))) {
                return dir.rotateLeftDegrees(10*currentCheck);
            }
            // Try the offset on the right side
            if(rc.canBuildRobot(robot,dir.rotateRightDegrees(10*currentCheck))) {
                return dir.rotateRightDegrees(10*currentCheck);
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        if(treeBuildSpot != null){
            dir = treeStartDir.opposite();
        }


        currentCheck = 1;

        while(currentCheck<=6) {
            // Try the offset of the left side
            if(rc.canBuildRobot(robot,dir.rotateLeftDegrees(60*currentCheck))) {
                return dir.rotateLeftDegrees(60*currentCheck);
            }
            // Try the offset on the right side
            if(rc.canBuildRobot(robot,dir.rotateRightDegrees(60*currentCheck))) {
                return dir.rotateRightDegrees(60*currentCheck);
            }
            // No move performed, try slightly further
            currentCheck++;
        }


        //Well, try slowly going through all the abailable options then to be completely sure were not missing anything
        currentCheck = 1;
        float max = 7 + unitBuildFailures;
        float degrees = 360 / max;
        dir = Direction.NORTH;
        while (currentCheck <= max) {
            // Try the offset of the left side
            if (rc.canBuildRobot(robot,dir.rotateLeftDegrees(degrees * ((float) currentCheck)))) {
                return dir.rotateLeftDegrees(degrees * ((float) currentCheck));
            }
            // Try the offset on the right side
            if (rc.canBuildRobot(robot,dir.rotateRightDegrees(degrees * ((float) currentCheck)))) {
                return dir.rotateRightDegrees(degrees * ((float) currentCheck));
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        return null;
    }


    public boolean buildTree( Direction dir, boolean allowFinalSpot) throws GameActionException{
        if (rc.canPlantTree(dir)) {

            if(reservedSpot == null || myLocation.add(dir, 2.05f).distanceTo(reservedSpot) > 1.9) {
                rc.plantTree(dir);
                //System.out.println("build tree");
                return true;
            }
        }

        // Now try a bunch of similar angles
        int currentCheck = 1;

        while(currentCheck<=4 || (allowFinalSpot && currentCheck <=5)) {
            // Try the offset of the left side
            if(rc.canPlantTree(dir.rotateLeftDegrees(60*currentCheck))) {
                if(reservedSpot == null || myLocation.add(dir.rotateLeftDegrees(60*currentCheck), 2.05f).distanceTo(reservedSpot) > 1.9) {

                    rc.plantTree(dir.rotateLeftDegrees(60 * currentCheck));
                    //System.out.println("build tree");
                    return true;
                }
            }
            currentCheck++;
        }
        return false;

    }


    public boolean buildTreeRandom( Direction dir) throws GameActionException{
        if (rc.canPlantTree(dir)) {
            if(reservedSpot == null || myLocation.add(dir, 2.05f).distanceTo(reservedSpot) > 1.9) {

                rc.plantTree(dir);
                return true;
            }
        }

        // Now try a bunch of similar angles
        int currentCheck = 1;

        while(currentCheck<=17) { //Not doing 18 to not fill up last tree slot
            // Try the offset of the left side

            if(rc.canPlantTree(dir.rotateLeftDegrees(10*currentCheck))) {
                if(reservedSpot == null || myLocation.add(dir.rotateLeftDegrees(10*currentCheck), 2.05f).distanceTo(reservedSpot) > 1.9) {
                    rc.plantTree(dir.rotateLeftDegrees(10 * currentCheck));
                    //System.out.println("build tree");

                    return true;
                }
            }
            // Try the offset on the right side
            if(rc.canPlantTree(dir.rotateRightDegrees(10*currentCheck))) {
                if(reservedSpot == null || myLocation.add(dir.rotateRightDegrees(10*currentCheck), 2.05f).distanceTo(reservedSpot) > 1.9) {
                    rc.plantTree(dir.rotateRightDegrees(10 * currentCheck));
                    return true;
                }
            }
            // No move performed, try slightly further
            currentCheck++;
        }
        return false;

    }


    //No longer used method that indicated spots on a hex grid nearby
    public MapLocation[] identifyNearbyHexagonalSpotsNearby(float distance){
        int aGridPointX1 = ((int)((myLocation.x)/2f))*2;
        int aGridPointX2 = aGridPointX1 + 2;

        float aGridPointY1;
        float aGridPointY2;


        if(aGridPointX1 % 4 == 0){
            aGridPointY1 = ((int)((myLocation.y / 6f) + 0.5f)) * 6f;
            aGridPointY2 = (((int)(myLocation.y / 6f)) + 0.5f) * 6f;
        }
        else{
            aGridPointY2 = ((int)((myLocation.y / 6f) + 0.5f)) * 6f;
            aGridPointY1 = (((int)(myLocation.y / 6f)) + 0.5f) * 6f;
        }

        MapLocation potentialCenter1 = new MapLocation(aGridPointX1,aGridPointY1);
        MapLocation potentialCenter2 = new MapLocation(aGridPointX2,aGridPointY2);
        MapLocation center;


        int found = 0;
        MapLocation[] locations = new MapLocation[7];


        if(myLocation.distanceTo(potentialCenter1) < myLocation.distanceTo(potentialCenter2)){
            center = potentialCenter1;
        }
        else{
            center = potentialCenter2;
        }

        MapLocation p1 = new MapLocation(center.x-2f,center.y);
        MapLocation p2 = new MapLocation(center.x+2f,center.y);
        MapLocation p3 = new MapLocation(center.x-1f,center.y-1.5f);
        MapLocation p4 = new MapLocation(center.x+1f,center.y-1.5f);
        MapLocation p5 = new MapLocation(center.x-1f,center.y+1.5f);
        MapLocation p6 = new MapLocation(center.x+1f,center.y+1.5f);

        if(center.distanceTo(myLocation) <= distance){
            locations[0] = center;
            found++;
        }

        if(p1.distanceTo(myLocation) <= distance){
            locations[1] = p1;
            found++;
        }
        if(p2.distanceTo(myLocation) <= distance){
            locations[2] = p2;
            found++;
        }
        if(p3.distanceTo(myLocation) <= distance){
            locations[3] = p3;
            found++;
        }
        if(p4.distanceTo(myLocation) <= distance){
            locations[4] = p4;
            found++;
        }
        if(p5.distanceTo(myLocation) <= distance){
            locations[5] = p5;
            found++;
        }
        if(p6.distanceTo(myLocation) <= distance){
            locations[6] = p6;
            found++;
        }


        MapLocation[] results = new MapLocation[found];

        int counter = 0;
        for(int i = 0 ; i < 7; i++){
            if(locations[i] != null){
                results[counter] = locations[i];
                counter++;
            }
        }
        return results;


    }


    //Deprecated
    public boolean buildRobot(RobotType robot, Direction dir) throws GameActionException{

        if (rc.canBuildRobot(robot, dir)) {
            rc.buildRobot(robot,dir);
            unitBuildFailures = 0;

            //System.out.println("build " + robot.name());
            return true;
        }

        // Now try a bunch of similar angles
        int currentCheck = 1;

        while(currentCheck<=18) {
            // Try the offset of the left side
            if(rc.canBuildRobot(robot,dir.rotateLeftDegrees(10*currentCheck))) {
                rc.buildRobot(robot,dir.rotateLeftDegrees(10*currentCheck));
                unitBuildFailures = 0;

                //System.out.println("build " + robot.name());
                return true;
            }
            // Try the offset on the right side
            if(rc.canBuildRobot(robot,dir.rotateRightDegrees(10*currentCheck))) {
                rc.buildRobot(robot,dir.rotateRightDegrees(10*currentCheck));
                unitBuildFailures = 0;

                // System.out.println("build " + robot.name());
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        if(treeBuildSpot != null){
            dir = treeStartDir.opposite();
        }

        currentCheck = 1;

        while(currentCheck<=6) {
            // Try the offset of the left side
            if(rc.canBuildRobot(robot,dir.rotateLeftDegrees(60*currentCheck))) {
                rc.buildRobot(robot,dir.rotateLeftDegrees(60*currentCheck));
                unitBuildFailures = 0;

                //System.out.println("build " + robot.name());
                return true;
            }
            // Try the offset on the right side
            if(rc.canBuildRobot(robot,dir.rotateRightDegrees(60*currentCheck))) {
                rc.buildRobot(robot,dir.rotateRightDegrees(60*currentCheck));
                unitBuildFailures = 0;

                // System.out.println("build " + robot.name());
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }


        //Well, try slowly going through all the abailable options then to be completely sure were not missing anything
        currentCheck = 1;
        float max = 7 + unitBuildFailures;
        float degrees = 360 / max;
        dir = Direction.NORTH;
        while (currentCheck <= max) {
            // Try the offset of the left side
            if (rc.canBuildRobot(robot,dir.rotateLeftDegrees(degrees * ((float) currentCheck)))) {
                rc.buildRobot(robot,dir.rotateLeftDegrees(degrees * ((float) currentCheck)));
                unitBuildFailures = 0;
                return true;
            }
            // Try the offset on the right side
            if (rc.canBuildRobot(robot,dir.rotateRightDegrees(degrees * ((float) currentCheck)))) {
                rc.buildRobot(robot,dir.rotateRightDegrees(degrees * ((float) currentCheck)));
                unitBuildFailures = 0;
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        return false;
    }


    public void initial() throws Exception{
        maxHp = RobotType.GARDENER.maxHealth;
        radius =  RobotType.GARDENER.bodyRadius;
        sightradius = RobotType.GARDENER.sensorRadius;
        bulletsightradius = RobotType.GARDENER.bulletSightRadius;
        maxMove = RobotType.GARDENER.strideRadius;


        turnBuild = rc.getRoundNum();

        if(rc.readBroadcast(GARDENERS_BUILD) == 3 || rc.readBroadcast(GARDENERS_BUILD) % 11 == 0 || rc.readBroadcast(GARDENERS_REPORTING_IN_TOTAL) > 6){
            newBaseBuilder = true;
        }
    }
}
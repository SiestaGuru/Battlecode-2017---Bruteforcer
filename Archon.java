package bruteforcer;

import battlecode.common.*;

/**
 * Created by Hermen on 10/1/2017.
 */
public class Archon extends RobotBase {


    public static int our_archon_nr = -1;
    public static int gardenerBuildFailure = 0;


    public void step() throws Exception {


        MapLocation myLocation = RobotBase.myLocation;
        RobotController rc = RobotBase.rc;

        dodgeBullets(rc.senseNearbyBullets());




//        testBytecode(bullets);

        doCommanderTasks();

        desireTowardsMiddle(-1f);


        boolean shouldConfirmEnemyLoc = false;
        boolean shouldConfirmEnemyTreeLoc = false;

        if (rc.readBroadcast(CONFIRMED_ENEMY_TURN) < turn) {
            shouldConfirmEnemyLoc = true;
        }
        if (rc.readBroadcast(CONFIRMED_ENEMY_TREE_TURN) < turn) {
            shouldConfirmEnemyTreeLoc = true;
        }

        RobotInfo anEnemy = null;
        RobotInfo gardener = null;

        int lastturnbuild = rc.readBroadcast(LAST_TURN_BUILD_SOMETHING);

        //Detected in commander tasks
        for (int i = 0; i < robotsDetected.length; i++) {
            RobotInfo r = robotsDetected[i];

            MapLocation loc = r.location;

            if (r.team.equals(enemy)) {
                if (anEnemy == null) {
                    anEnemy = r;
                }
                addVector(loc, -2);

                if (shouldConfirmEnemyLoc) {
                    if (!rc.getType().equals(RobotType.SCOUT)) {
                        rc.broadcast(CONFIRMED_ENEMY_X, (int) loc.x);
                        rc.broadcast(CONFIRMED_ENEMY_Y, (int) loc.y);
                        rc.broadcast(CONFIRMED_ENEMY_TURN, turn);
                        shouldConfirmEnemyLoc = false;
                    } else {
                        if (rc.readBroadcast(DETECTED_SCOUT_TURN) != turn) {
                            rc.broadcast(DETECTED_SCOUT_TURN, turn);
                            rc.broadcast(DETECTED_SCOUT_X, (int) loc.x);
                            rc.broadcast(DETECTED_SCOUT_Y, (int) loc.x);

                        }
                    }
                }
                if (r.getType().equals(RobotType.TANK)) {
                    if (rc.readBroadcast(CONFIRMED_ENEMY_TANK_TURN) != turn) {
                        rc.broadcast(CONFIRMED_ENEMY_TANK_X, (int) (loc.x * 10));
                        rc.broadcast(CONFIRMED_ENEMY_TANK_Y, (int) (loc.y * 10));
                        rc.broadcast(CONFIRMED_ENEMY_TANK_TURN, turn);
                    }
                } else if (r.getType().equals(RobotType.GARDENER)) {
                    if (rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_TURN) != turn) {
                        rc.broadcast(CONFIRMED_ENEMY_GARDENER_X, (int) (loc.x * 100));
                        rc.broadcast(CONFIRMED_ENEMY_GARDENER_Y, (int) (loc.y * 100));
                        rc.broadcast(CONFIRMED_ENEMY_GARDENER_TURN, turn);
                    }
                }
            } else {

                if (r.type.equals(RobotType.ARCHON)) {
                    addVector(loc, -2f); //Let's spread these out to make more room for bulding
                } else if (r.type.equals(RobotType.GARDENER)) {
                    if (gardener == null) {
                        gardener = r;
                    }
                    if (lastturnbuild == 0 || turn - lastturnbuild > 30) {
                        addVector(loc, -20f);
                    } else {
                        addVector(loc, -2f); //Let's spread these out to make more room for bulding
                    }

                } else if (r.type.equals(RobotType.LUMBERJACK)) {
                    addCircularDesireZone(loc, 3.75f, -20);
                }

                float distance = myLocation.distanceTo(loc);

                if (distance < r.getRadius() + 3.1f) {
                    addDesireZone(loc, r.getRadius() + 2.5f, -15); //But give me a bit of personal space yo
                    addSidewaysVector(loc, 3);
                }

            }
        }


        if (anEnemy != null && gardener != null && rc.getRobotCount() - ownArchonCount < 5) {
            //Try to provide cover for our gardener friend to give it enough time to build something
            MapLocation enemyLoc = anEnemy.location;
            MapLocation gardenerLoc = gardener.location;
            float dist = enemyLoc.distanceTo(gardenerLoc);
            if (dist < 7) {
//                rc.setIndicatorDot(myLocation,255,128,240);
                Direction dir = enemyLoc.directionTo(gardenerLoc);
                MapLocation midpoint = enemyLoc.add(dir, dist / 2f);
                addVector(midpoint, 10);

                MapLocation rightInFront = enemyLoc.add(dir, anEnemy.getRadius() + 2.01f);
                if (myLocation.distanceTo(rightInFront) < maxMove && rc.canMove(rightInFront)) {
                    addSpecialMapLocation(rightInFront, 20);
                }
            }
        }

        //Detected in commandertasks
        for (int i = 0; i < treesDetected.length && i < 20; i++) {
            TreeInfo t = treesDetected[i];
            MapLocation loc = t.location;
            if (loc.distanceTo(myLocation) < 5) {
                addVectorSqrt(loc, -2);
                if (zoneLoc < 10) {
                    addDesireZone(loc, t.getRadius() + 1, -15); // Don't stand too close to trees if possible, makes it harder to dodge bullets etc
                }
            }
            if (t.team.equals(enemy)) {
                if (shouldConfirmEnemyTreeLoc) {
                    shouldConfirmEnemyTreeLoc = false;
                    rc.broadcast(CONFIRMED_ENEMY_TREE_X, (int) loc.x);
                    rc.broadcast(CONFIRMED_ENEMY_TREE_Y, (int) loc.y);
                    rc.broadcast(CONFIRMED_ENEMY_TREE_TURN, turn);
                }
            }
            if (t.containedBullets > 0) {
                if (rc.canShake(t.ID)) {
                    rc.shake(t.ID);
                } else {
                    addVector(loc, 3);
                }
            }
        }


        addVector(getRandomGoal(), 1f);

        int AggrMainX = rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_X);

        if (AggrMainX >= 0) {
            addVector(AggrMainX, rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_X), -1);
        }

        //Go Home
        addVector(rc.readBroadcast(HOME_X), rc.readBroadcast(HOME_Y), 1f);

        if (turn > 5 && rc.readBroadcast(GARDENERS_BUILD) == 0 && rc.getTreeCount() == 0) {
            //Try moving in towards the edges if we're struggling to build gardeners
            if (turn < 20) {
                desireFarEdges(5, 2.1f);
                desireFarEdges(1.5f, 8f);
                desireTowardsMiddle(-30);
            } else if (turn < 40) {
                addVector(getRandomGoal(), 20);
            } else if (turn < 60) {
                desireTowardsMiddle(100);
            } else if (turn < 80) {
                addVector(getRandomGoal(), -500);
            } else {
                randomGoal = null;
                addVector(getRandomGoal(), 500);
            }
        } else {
            //Avoid edges in most situations
            desireFarEdges(-5, 3.1f);
            desireFarEdges(-3, 6f);
        }


        doMovement(1500);


        //Mapsize stuff
        if (turn == 1 || turn % 3 == 0) {
            if (Clock.getBytecodesLeft() > 1000) {
                myLocation = rc.getLocation();
                Archon.myLocation = myLocation;
                MapLocation n = myLocation.add(Direction.NORTH, 9.9f);
                MapLocation e = myLocation.add(Direction.EAST, 9.9f);
                MapLocation s = myLocation.add(Direction.SOUTH, 9.9f);
                MapLocation w = myLocation.add(Direction.WEST, 9.9f);

                if (rc.onTheMap(n)) {
                    if (n.y > mapHighestY) {
                        mapHighestY = n.y;
                        if ((int) (mapHighestY * 100) > rc.readBroadcast(MAP_MAX_Y_BROADCAST)) {
                            rc.broadcast(MAP_MAX_Y_BROADCAST, (int) (mapHighestY * 100));
                        }
                    }
                }
                if (rc.onTheMap(s)) {
                    if (s.y < mapLowestY) {
                        mapLowestY = s.y;
                        if ((int) (mapLowestY * 100) < rc.readBroadcast(MAP_MIN_Y_BROADCAST)) {
                            rc.broadcast(MAP_MIN_Y_BROADCAST, (int) (mapLowestY * 100));
                        }
                    }
                }


                if (rc.onTheMap(e)) {
                    if (e.x > mapHighestX) {
                        mapHighestX = e.x;
                        if ((int) (mapHighestX * 100) > rc.readBroadcast(MAP_MAX_X_BROADCAST)) {
                            rc.broadcast(MAP_MAX_X_BROADCAST, (int) (mapHighestX * 100));
                        }
                    }
                }
                if (rc.onTheMap(w)) {
                    if (w.x < mapLowestX) {
                        mapLowestX = w.x;
                        if ((int) (mapLowestX * 100) < rc.readBroadcast(MAP_MIN_X_BROADCAST)) {
                            rc.broadcast(MAP_MIN_X_BROADCAST, (int) (mapLowestX * 100));
                        }
                    }
                }

                broadcastOwnLocation();
                broadcastMapSize();

                if (turn > 1000 && rc.readBroadcast(GARDENERS_BUILD) == 0 && rc.getTreeCount() == 0) {
                    rc.donate(rc.getVictoryPointCost());
                }
            }

        }
    }

    public void initial() throws GameActionException {
        maxHp = RobotType.ARCHON.maxHealth;
        radius =  RobotType.ARCHON.bodyRadius;
        sightradius = RobotType.ARCHON.sensorRadius;
        bulletsightradius = RobotType.ARCHON.bulletSightRadius;
        maxMove = RobotType.ARCHON.strideRadius;

        if (rc.readBroadcast(INITIATED) != 1) {
            initialBroadcast();
        }

        initialOwnArchonBroadcast();
    }


    private void initialOwnArchonBroadcast() throws GameActionException {

        if (rc.readBroadcast(OUR_ARCHON_1_ID) <= 0) {
            our_archon_nr = 1;
            rc.broadcast(OUR_ARCHON_1_ID, rc.getID());
            //System.out.println("Archon init as 1");
        } else if (rc.readBroadcast(OUR_ARCHON_2_ID) <= 0) {
            our_archon_nr = 2;
            rc.broadcast(OUR_ARCHON_2_ID, rc.getID());
            //System.out.println("Archon init as 2");
        } else if (rc.readBroadcast(OUR_ARCHON_3_ID) <= 0) {
            our_archon_nr = 3;
            rc.broadcast(OUR_ARCHON_3_ID, rc.getID());
            // System.out.println("Archon init as 3");

        }
    }

    private void broadcastOwnLocation() throws GameActionException {

        if (!alreadyBroadcastLocation) {

            shouldbroadcastMapSize = true;

            if (our_archon_nr == 1) {
                rc.broadcast(OUR_ARCHON_1_X, (int) myLocation.x);
                rc.broadcast(OUR_ARCHON_1_Y, (int) myLocation.y);
                rc.broadcast(OUR_ARCHON_1_LASTSEEN, turn);
            } else if (our_archon_nr == 2) {
                rc.broadcast(OUR_ARCHON_2_X, (int) myLocation.x);
                rc.broadcast(OUR_ARCHON_2_Y, (int) myLocation.y);
                rc.broadcast(OUR_ARCHON_2_LASTSEEN, turn);
            } else if (our_archon_nr == 3) {
                rc.broadcast(OUR_ARCHON_3_X, (int) myLocation.x);
                rc.broadcast(OUR_ARCHON_3_Y, (int) myLocation.y);
                rc.broadcast(OUR_ARCHON_3_LASTSEEN, turn);
            }
            alreadyBroadcastLocation = true;
        }
    }


    private void initialBroadcast() throws GameActionException {
        MapLocation[] initialEnemy = rc.getInitialArchonLocations(enemy);

        if (initialEnemy.length >= 1) {
            rc.broadcast(THEIR_ARCHON_1_X, (int) (initialEnemy[0].x * 10));
            rc.broadcast(THEIR_ARCHON_1_Y, (int)( initialEnemy[0].y*10));
            rc.broadcast(THEIR_ARCHON_1_ID, -1);

            if (initialEnemy[0].x < mapLowestX) {
                mapLowestX = (int) initialEnemy[0].x;
            }
            if (initialEnemy[0].x > mapHighestX) {
                mapHighestX = (int) initialEnemy[0].x;
            }
            if (initialEnemy[0].y < mapLowestY) {
                mapLowestY = (int) initialEnemy[0].y;
            }
            if (initialEnemy[0].y > mapHighestY) {
                mapHighestY = (int) initialEnemy[0].y;
            }

        } else {
            rc.broadcast(THEIR_ARCHON_1_ID, -2);
        }
        if (initialEnemy.length >= 2) {
            rc.broadcast(THEIR_ARCHON_2_X, (int) (initialEnemy[1].x*10));
            rc.broadcast(THEIR_ARCHON_2_Y, (int) (initialEnemy[1].y*10));
            rc.broadcast(THEIR_ARCHON_2_ID, -1);

            if (initialEnemy[1].x < mapLowestX) {
                mapLowestX = (int) initialEnemy[1].x;
            }
            if (initialEnemy[1].x > mapHighestX) {
                mapHighestX = (int) initialEnemy[1].x;
            }
            if (initialEnemy[1].y < mapLowestY) {
                mapLowestY = (int) initialEnemy[1].y;
            }
            if (initialEnemy[1].y > mapHighestY) {
                mapHighestY = (int) initialEnemy[1].y;
            }
        } else {
            rc.broadcast(THEIR_ARCHON_2_ID, -2);
        }

        if (initialEnemy.length >= 3) {
            rc.broadcast(THEIR_ARCHON_3_X, (int) (initialEnemy[2].x*10));
            rc.broadcast(THEIR_ARCHON_3_Y, (int) (initialEnemy[2].y*10));
            rc.broadcast(THEIR_ARCHON_3_ID, -1);

            if (initialEnemy[2].x < mapLowestX) {
                mapLowestX = (int) initialEnemy[2].x;
            }
            if (initialEnemy[2].x > mapHighestX) {
                mapHighestX = (int) initialEnemy[2].x;
            }
            if (initialEnemy[2].y < mapLowestY) {
                mapLowestY = (int) initialEnemy[2].y;
            }
            if (initialEnemy[2].y > mapHighestY) {
                mapHighestY = (int) initialEnemy[2].y;
            }
        } else {
            rc.broadcast(THEIR_ARCHON_3_ID, -2);
        }

        myLocation = rc.getLocation();
        broadcastMapSize();

        rc.broadcast(OUR_ARCHON_1_ID, -2);
        rc.broadcast(OUR_ARCHON_2_ID, -2);
        rc.broadcast(OUR_ARCHON_3_ID, -2);

        rc.broadcast(AGRESSIVE_TARGETS_LAST_UPDATE, -100); //Set last aggressive update to never
        rc.broadcast(MAIN_AGRESSIVE_TARGET_X, -1);
        rc.broadcast(MAIN_AGRESSIVE_TARGET_Y, -1);
        rc.broadcast(SECONDARY_AGRESSIVE_TARGET_X, -1);
        rc.broadcast(SECONDARY_AGRESSIVE_TARGET_Y, -1);
        rc.broadcast(MAIN_DEFENSIVE_TARGET_X, -1);
        rc.broadcast(MAIN_DEFENSIVE_TARGET_Y, -1);


        rc.broadcast(INITIATED, 1);

        //System.out.println("Archon doing initial broadcast");
    }




    public boolean buildGardener(Direction dir) throws GameActionException {

        if (rc.canHireGardener(dir)) {
            rc.hireGardener(dir);
            gardenerBuildFailure = 0;
            return true;
        }

        // Now try a bunch of similar angles
        int currentCheck = 1;

        while (currentCheck <= 20) {
            // Try the offset of the left side
            if (rc.canHireGardener(dir.rotateLeftDegrees(9 * currentCheck))) {
                rc.hireGardener(dir.rotateLeftDegrees(9 * currentCheck));
                //System.out.println("build gardener");
                gardenerBuildFailure = 0;
                return true;
            }
            // Try the offset on the right side
            if (rc.canHireGardener(dir.rotateRightDegrees(9 * currentCheck))) {
                rc.hireGardener(dir.rotateRightDegrees(9 * currentCheck));
                //System.out.println("build gardener");
                gardenerBuildFailure = 0;
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }



        if (rc.canHireGardener(Direction.NORTH)) {
            rc.hireGardener(Direction.NORTH);
            gardenerBuildFailure = 0;
            return true;
        }
        if (rc.canHireGardener(Direction.EAST)) {
            rc.hireGardener(Direction.EAST);
            gardenerBuildFailure = 0;
            return true;
        }
        if (rc.canHireGardener(Direction.SOUTH)) {
            rc.hireGardener(Direction.SOUTH);
            gardenerBuildFailure = 0;
            return true;
        }
        if (rc.canHireGardener(Direction.WEST)) {
            rc.hireGardener(Direction.WEST);
            gardenerBuildFailure = 0;
            return true;
        }


        //Ok, so let's try some different angles if we're unable to find anything. No more lil maze!
        currentCheck = 1;
        float max = 21 + (gardenerBuildFailure * 3);
        float degrees = 360 / max;
        dir = Direction.NORTH;


        while (currentCheck <= max) {
            // Try the offset of the left side
            if (rc.canHireGardener(dir.rotateLeftDegrees(degrees * ((float) currentCheck)))) {
                rc.hireGardener(dir.rotateLeftDegrees(degrees * ((float) currentCheck)));
                gardenerBuildFailure = 0;
                return true;
            }
            // Try the offset on the right side
            if (rc.canHireGardener(dir.rotateRightDegrees(degrees * ((float) currentCheck)))) {
                rc.hireGardener(dir.rotateRightDegrees(degrees * ((float) currentCheck)));
                gardenerBuildFailure = 0;
                return true;
            }

            // No move performed, try slightly further
            currentCheck++;
        }

        gardenerBuildFailure++;
        return false;
    }



    public void testBytecode(BulletInfo[] bullets) {

        //Findings:
        //Bytecode into array is 3
        //Simple int assignment is already like 2
        //Putting in local scope instead of static appears to have no effect
        //Taking a loop variable out and putting it in local variable to be reused actually does increase performance
        //Switch not actually that much better than if/else block, in fact worse on what I tested
        //Een constructie zoals m.add(myLocation.directionTo(m).rotateLeftDegrees(50), 10);  is maar 11 bytecodes, acceptabel dus
        //Locations in temp variable seems to improve, but directions don't? Might depend on code

        int[] clockTimes = new int[50];



        float bulletSpotRadius = radius + 0.1f;
        float minDist = bulletSpotRadius + maxMove;

        clockTimes[0] = Clock.getBytecodesLeft();

        zoneLoc = 0;


        for (int i = 0; i < bullets.length ; i++) {
            BulletInfo bullet = bullets[i];
            MapLocation loc = bullet.location;

            float dodgeSize = radius + (bullet.speed / 5);
            if (myLocation.distanceTo(loc) <= minDist) {
                zones[zoneLoc] = new DesireZone(loc,bulletSpotRadius,-200 + -10 * bullet.damage);
                zoneLoc++;
            }
            MapLocation bullet1Loc = loc.add(bullet.dir, bullet.speed * 0.5f);
            if (myLocation.distanceTo(bullet1Loc) <= dodgeSize + maxMove) {
                zones[zoneLoc] = new DesireZone(bullet1Loc,dodgeSize,-120 + -10 * bullet.damage);
                zoneLoc++;
            }
            MapLocation bullet2Loc = loc.add(bullet.dir, bullet.speed * 1f);
            if (myLocation.distanceTo(bullet2Loc) <= minDist) {
                zones[zoneLoc] = new DesireZone(bullet2Loc,bulletSpotRadius,-200 + -10 * bullet.damage);
                zoneLoc++;
            }
            MapLocation bullet3Loc = loc.add(bullet.dir, bullet.speed * 1.7f);
            if (myLocation.distanceTo(bullet3Loc) <= maxMove + dodgeSize + 0.3) {
                zones[zoneLoc] = new DesireZone(bullet3Loc,dodgeSize,-200 + -10 * bullet.damage);
                zoneLoc++;
            }
        }

//        System.out.println("zones : " + zoneLoc);


        clockTimes[1] = Clock.getBytecodesLeft();
        zoneLoc = 0;
        float xMin = myLocation.x - minDist;
        float xPlus = myLocation.x + minDist;
        float yMin = myLocation.y - minDist;
        float yPlus = myLocation.y + minDist;


        int from1 = 0;
        int from2 = 0;
        int from3 = 0;
        int from4 = 0;
//
        rc.setIndicatorLine(new MapLocation(xMin,yMin),new MapLocation(xPlus,yMin),50,50,50);
        rc.setIndicatorLine(new MapLocation(xMin,yMin),new MapLocation(xMin,yPlus),50,50,50);
        rc.setIndicatorLine(new MapLocation(xPlus,yPlus),new MapLocation(xPlus,yMin),50,50,50);
        rc.setIndicatorLine(new MapLocation(xPlus,yPlus),new MapLocation(xMin,yPlus),50,50,50);


        for (int i = 0; i < bullets.length; i++) {
            BulletInfo bullet = bullets[i];
            MapLocation loc = bullet.location;
            float speedFactor = (bullet.speed / 5);

            if (xMin < loc.x && xPlus > loc.x && yMin < loc.y && yPlus > loc.y) {
                zones[zoneLoc] = new DesireZone(loc,bulletSpotRadius,-200 + -10 * bullet.damage);
                zoneLoc++;
                from1++;
                rc.setIndicatorLine(loc,  loc.add(bullet.dir,  0.1f), 255,0,0);
            }
            MapLocation bullet1Loc = loc.add(bullet.dir, bullet.speed * 0.5f);
            if (xMin - speedFactor < bullet1Loc.x && xPlus + speedFactor > bullet1Loc.x && yMin - speedFactor < bullet1Loc.y && yPlus + speedFactor > bullet1Loc.y) {
                zones[zoneLoc] = new DesireZone(bullet1Loc,bulletSpotRadius + speedFactor,-120 + -10 * bullet.damage);
                zoneLoc++;
                from2++;
                rc.setIndicatorLine(bullet1Loc,  bullet1Loc.add(bullet.dir, 0.1f), 255,0,255);

            }
            MapLocation bullet2Loc = loc.add(bullet.dir, bullet.speed * 1f);
            if (xMin < bullet2Loc.x && xPlus > bullet2Loc.x && yMin < bullet2Loc.y && yPlus > bullet2Loc.y) {
                zones[zoneLoc] = new DesireZone(bullet2Loc,bulletSpotRadius,-200 + -10 * bullet.damage);
                zoneLoc++;
                from3++;
                rc.setIndicatorLine(bullet2Loc,  bullet2Loc.add(bullet.dir, 0.1f), 0,255,0);

            }
            MapLocation bullet3Loc = loc.add(bullet.dir, bullet.speed * 1.7f);
            if (xMin - (speedFactor+0.3f) < bullet3Loc.x && xPlus + (speedFactor+0.3f) > bullet3Loc.x && yMin - (speedFactor+0.3f) < bullet3Loc.y && yPlus + (speedFactor+0.3f) > bullet3Loc.y) {
                zones[zoneLoc] = new DesireZone(bullet3Loc,bulletSpotRadius + speedFactor + 0.3f,-200 + -10 * bullet.damage);
                zoneLoc++;
                from4++;
                rc.setIndicatorLine(bullet3Loc,  bullet3Loc.add(bullet.dir,0.1f), 0,0,255);

            }
        }

//        System.out.println("zones : " + zoneLoc +  "  on length: " + bullets.length  +"   from: " + from1 + " ," + from2 + " ," + from3 + " ," + from4);


        clockTimes[2] = Clock.getBytecodesLeft();

        clockTimes[3] = Clock.getBytecodesLeft();

        clockTimes[4] = Clock.getBytecodesLeft();
        clockTimes[5] = Clock.getBytecodesLeft();
        clockTimes[6] = Clock.getBytecodesLeft();

        clockTimes[7] = Clock.getBytecodesLeft();


        clockTimes[8] = Clock.getBytecodesLeft();



        for (int i = 1; i < 50; i++) {
            if (clockTimes[i] != 0) {
                System.out.println("Step " + (i - 1) + " - " + i + "  : " + ((clockTimes[i - 1] - clockTimes[i]) - 3)); //Minus 3, because that appears to be the cost of putting these in the array
            }
        }


    }

    public void lineBulletfunction(){
        //Lijkt er op dat dit toch teveel performance kost :( Bijna dubbele, soms meer dan 6000... En dan kijken we nog niet eens naar de zwaardere zones
        //Performance savers
//        float x = myLocation.x;
//        float y = myLocation.y;
//        float safetyMargin = maxMove + radius + 0.2f;
//        float xMinusSafety = x - safetyMargin;
//        float yMinusSafety = y - safetyMargin;
//        float xPlusSafety = x + safetyMargin;
//        float yPlusSafety = y + safetyMargin;
//        float radiusMinusMove = radius - maxMove;
//
//        for (int i = 0; i < bullets.length; i++) {
//            BulletInfo bullet = bullets[i];
//            float bx = bullet.location.x;
//            float by = bullet.location.y;
//            boolean doLine1 = false;
//            boolean doLine2 = false;
//
//            MapLocation bullet1Loc = bullet.location.add(bullet.originalEnemyDir, bullet.speed);
//            MapLocation bullet2Loc = bullet.location.add(bullet.originalEnemyDir, bullet.speed * 1.5f);
//
//
//            //Were in the rectangle between the current bullet location, and its next
//            if (x > bx) {
//                if (x < bullet1Loc.x) {
//                    if (y > by) {
//                        if (y < bullet1Loc.y) {
//                            doLine1 = true;
//                        }
//                    } else {
//                        if (y > bullet1Loc.y) {
//                            doLine1 = true;
//                        }
//                    }
//                }
//            } else if (x > bullet1Loc.x) {
//                if (y > by) {
//                    if (y < bullet1Loc.y) {
//                        doLine1 = true;
//                    }
//                } else {
//                    if (y > bullet1Loc.y) {
//                        doLine1 = true;
//                    }
//                }
//            }
//            //Were in the rectangle between the next bullet location and the following
//            if (x > bullet1Loc.x) {
//                if (x < bullet2Loc.x) {
//                    if (y > bullet1Loc.y) {
//                        if (y < bullet2Loc.y) {
//                            doLine1 = true;
//                        }
//                    } else {
//                        if (y > bullet2Loc.y) {
//                            doLine1 = true;
//                        }
//                    }
//                }
//            } else if (x > bullet2Loc.x) {
//                if (y > bullet1Loc.y) {
//                    if (y < bullet2Loc.y) {
//                        doLine1 = true;
//                    }
//                } else {
//                    if (y > bullet2Loc.y) {
//                        doLine1 = true;
//                    }
//                }
//            }
//
//            if(!doLine1){
//                //Ok, hier moeten we dus kijken, of we binnen het vierkant zitten van punt 0 en punt 1
//                if(xMinusSafety < bx && xPlusSafety > bx && yMinusSafety < by && yPlusSafety > by){
//                    doLine1 = true;
//                }
//                if(xMinusSafety < bullet1Loc.x && xPlusSafety > bullet1Loc.x && yMinusSafety < bullet1Loc.y && yPlusSafety > bullet1Loc.y){
//                    doLine1 = true;
//                    doLine2 = true;
//                }
//            }
//
//            if(!doLine2){
//                if(xMinusSafety < bullet2Loc.x && xPlusSafety > bullet2Loc.x && yMinusSafety < bullet2Loc.y && yPlusSafety > bullet2Loc.y) {
//                    doLine2 = true;
//                }
//            }
//
//            if(doLine1){
//                addLineZone(bullet.location, bullet.originalEnemyDir, bullet.speed, radius, -200);
//            }
//            if(doLine1){
//                addLineZone(bullet1Loc, bullet.originalEnemyDir, bullet.speed, radius, -200);
//            }
//
//        }
    }
}

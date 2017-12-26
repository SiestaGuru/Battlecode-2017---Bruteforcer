package bruteforcer;

import battlecode.common.*;

/**
 * Created by Hermen on 10/1/2017.
 */
public class Lumberjack extends RobotBase {


    static MapLocation prevLoc = null;
    static MapLocation prevLoc1 = null;
    static MapLocation prevLoc2 = null;
    static MapLocation prevLoc3 = null;

    private static int choppableTree = -1;


    private static MapLocation woodcuttingJob = null;
    private static int woodcuttingJobPriority = -1;
    private static int woodcuttingJobTurn = -1;
    static int directionDecisionCooldown = 0;

        static  int turnsNotStuckToTree = 0;
        static int turnsStuck = 0;
    static int turnsNotDoneAnything = 0;
    static int stuckCooldown = 0;
    static Direction lastBugDirection = null;

    public void step() throws Exception {

        MapLocation myLocation = RobotBase.myLocation;
        RobotController rc = RobotBase.rc;

        //Only report in every couple of turns to confuse some players (who might depend too much on broadcasts every turn)

        if (turn % 3 == 0) {
            if (rc.readBroadcast(LUMBERJACKS_REPORTING_IN_TURN) != turn) {
                rc.broadcast(LUMBERJACKS_REPORTING_IN_TURN, turn);
                rc.broadcast(LUMBERJACKS_REPORTING_IN, 1);

            } else {
                rc.broadcast(LUMBERJACKS_REPORTING_IN, rc.readBroadcast(LUMBERJACKS_REPORTING_IN) + 1);
            }
        }


        turnsNotStuckToTree++;
        stuckCooldown--;
        if (prevLoc3 != null) {
            if (myLocation.distanceTo(prevLoc3) < 0.4f) {
                turnsStuck++;

                //Might be stuck going towards our job
                if(turnsStuck > 8 && turnsNotDoneAnything > 8 && woodcuttingJob != null){
                    woodcuttingJob = null;
                    stuckCooldown = 20;
                }
            } else {
                turnsStuck = 0;
            }
        }


        RobotInfo[] robots = rc.senseNearbyRobots();
        if (turn % 3 == 0 && turnsStuck < 5 && stuckCooldown<= 0) {
            selectwoodcutting:
            if (woodcuttingJob == null || turn - woodcuttingJobTurn > 50 || myLocation.distanceTo(woodcuttingJob) > woodcuttingJobPriority) {
                woodcuttingJob = null; //resetting

                if( turn - woodcuttingJobTurn > 40 ) {
                    stuckCooldown = 20;
                }

                MapLocation job;
                int prio = rc.readBroadcast(WOODCUT_JOB_1_PRIORITY);
                int wcturn;
                if (prio > 0) {
                    wcturn = rc.readBroadcast(WOODCUT_JOB_1_TURN);
                    if (turn - wcturn < 15) {
                        job = new MapLocation(rc.readBroadcast(WOODCUT_JOB_1_X) / 100f, rc.readBroadcast(WOODCUT_JOB_1_Y) /  100f);
                        if (job.distanceTo(myLocation) < prio) {
                            //Ok, we'll take it!
                            woodcuttingJob = job;
                            woodcuttingJobTurn = turn;
                            woodcuttingJobPriority = prio;
                            rc.broadcast(WOODCUT_JOB_1_PRIORITY, -1); // invalidate the job so that itll be overwritten and not taken by others
                            break selectwoodcutting;
                        }
                    }
                }

                prio = rc.readBroadcast(WOODCUT_JOB_2_PRIORITY);
                if (prio > 0) {
                    wcturn = rc.readBroadcast(WOODCUT_JOB_2_TURN);

                    if (turn - wcturn < 10) {

                        job = new MapLocation(rc.readBroadcast(WOODCUT_JOB_2_X)/100f, rc.readBroadcast(WOODCUT_JOB_2_Y)/100f);
                        if (job.distanceTo(myLocation) < prio) {
                            //Ok, we'll take it!
                            woodcuttingJob = job;
                            woodcuttingJobTurn = turn;
                            woodcuttingJobPriority = prio;
                            rc.broadcast(WOODCUT_JOB_2_PRIORITY, -1); // invalidate the job so that itll be overwritten and not taken by others
                            break selectwoodcutting;
                        }
                    }
                }


                prio = rc.readBroadcast(WOODCUT_JOB_3_PRIORITY);
                if (prio > 0) {
                    wcturn = rc.readBroadcast(WOODCUT_JOB_3_TURN);

                    if (turn - wcturn < 10) {

                        job = new MapLocation(rc.readBroadcast(WOODCUT_JOB_3_X)/100f, rc.readBroadcast(WOODCUT_JOB_3_Y)/100f);
                        if (job.distanceTo(myLocation) < prio) {
                            //Ok, we'll take it!
                            woodcuttingJob = job;
                            woodcuttingJobTurn = turn;
                            woodcuttingJobPriority = prio;
                            rc.broadcast(WOODCUT_JOB_3_PRIORITY, -1); // invalidate the job so that itll be overwritten and not taken by others
                            break selectwoodcutting;
                        }
                    }
                }


                prio = rc.readBroadcast(WOODCUT_JOB_4_PRIORITY);
                if (prio > 0) {
                    wcturn = rc.readBroadcast(WOODCUT_JOB_4_TURN);
                    if (turn - wcturn < 10) {
                        job = new MapLocation(rc.readBroadcast(WOODCUT_JOB_4_X)/100f, rc.readBroadcast(WOODCUT_JOB_4_Y)/100f);
                        if (job.distanceTo(myLocation) < prio) {
                            //Ok, we'll take it!
                            woodcuttingJob = job;
                            woodcuttingJobTurn = turn;
                            woodcuttingJobPriority = prio;
                            rc.broadcast(WOODCUT_JOB_4_PRIORITY, -1); // invalidate the job so that itll be overwritten and not taken by others
                            break selectwoodcutting;
                        }
                    }
                }
            }
        }

//        if(woodcuttingJob != null){
//            rc.setIndicatorLine(myLocation,woodcuttingJob, 255,0,0);
//        }


        if (rc.canStrike()) {
            //Try striking before movement
            int allyCount = 0;
            int enemyCount = 0;
            for (int i = robots.length - 1; i >= 0; i--) {
                if (robots[i].getLocation().isWithinDistance(myLocation, 2 + robots[i].getRadius())) {
                    if (robots[i].team.equals(ally)) {
                        if (robots[i].getType().equals(RobotType.SOLDIER) || robots[i].getType().equals(RobotType.LUMBERJACK) || robots[i].getType().equals(RobotType.TANK)) {
                            allyCount += 3;
                        } else {
                            allyCount++;
                        }
                        if (robots[i].getHealth() < 20) {
                            allyCount++;
                        }
                    } else {
                        enemyCount += 2;
                        if (robots[i].getHealth() < 20) {
                            enemyCount++;
                        }
                    }
                }
            }
            if (enemyCount > 0 && enemyCount > allyCount) {
                rc.strike();
            }
        }


        MapLocation home = new MapLocation(rc.readBroadcast(HOME_X), rc.readBroadcast(HOME_Y));

        boolean shouldWoodcut = false;

        if (turn - rc.readBroadcast(LAST_UPDATED_GRAND_STRAT) < 5) {
            if (rc.readBroadcast(WOODCUTTING_FOCUS) == 1) {
                shouldWoodcut = true;
            }
        }

        boolean shouldConfirmEnemyLoc = false;
        boolean shouldConfirmEnemyTreeLoc = false;

        if (rc.readBroadcast(CONFIRMED_ENEMY_TURN) < turn) {
            shouldConfirmEnemyLoc = true;
        }
        if (rc.readBroadcast(CONFIRMED_ENEMY_TREE_TURN) < turn) {
            shouldConfirmEnemyTreeLoc = true;
        }

        TreeInfo[] nearbyTrees = rc.senseNearbyTrees();

        int bestTree = -1;
        float bestHealth = 10000;
        boolean doingTreeMovement = false;
        int attempts = 0;

        int closeTrees = 0;

        for (int i = 0; i < nearbyTrees.length && i < 10; i++) {
            TreeInfo tree = nearbyTrees[i];
            MapLocation loc = tree.location;
            float distance = tree.location.distanceTo(myLocation);

            //Bugpathing. Might be slightly outdated, check soldier for last version
            if (!doingTreeMovement && attempts < 3) {
                attempts++;
                if (distance < tree.radius + 1.7f) {
                    //Ok, we may be stuck here. By now we have a good idea what our movement vector will look like. Use that to figure out which side we should be going in, so let's check
                    Direction vectorDir = myLocation.directionTo(new MapLocation(myLocation.x + moveVectorX, myLocation.y + moveVectorY));
                    MapLocation directionDesire = myLocation.add(vectorDir, 0.3f);

                    float treeToApproxDist = tree.location.distanceTo(directionDesire);
                    if (distance > treeToApproxDist) {
                        if (lastBugDirection != null) {
                            if (myLocation.directionTo(loc).degreesBetween(lastBugDirection) > 130) {
                                if (directionLeaning == 0) {
                                    directionLeaning = 1;
                                    directionDecisionCooldown = 200;
                                } else if (directionLeaning == 1) {
                                    directionLeaning = 0;
                                    directionDecisionCooldown = 200;
                                }
                            }
                        }

                        if (directionLeaning == -1 || directionDecisionCooldown <= 0 || turnsNotStuckToTree > 3) {
                            if (turnsStuck > 10 && directionLeaning == 0) {
                                directionLeaning = 1;
                                directionDecisionCooldown = 200;
                            } else if (turnsStuck > 10 && directionLeaning == 1) {
                                directionLeaning = 0;
                                directionDecisionCooldown = 200;
                            } else {
                                directionLeaning = -1;
                            }
                        }

                        boolean found = false;
                        MapLocation directionPicker;
                        int degs = 10 * ((int) ((Direction.NORTH.degreesBetween(myLocation.directionTo(loc)) / 10) + 0.5f));
                        Direction startDir = Direction.NORTH.rotateLeftDegrees(degs); //Normalizing to a neat angle, because mapmakers like nice angles
                        for (int attempt = 1; attempt < 60; attempt++) {
                            Direction newDir;

                            boolean left;
                            float amount;

                            if (directionLeaning == 0) {
                                left = true;
                                amount = -9 + (3f * attempt);
                            } else if (directionLeaning == 1) {
                                left = false;
                                amount = -9 + (3f * attempt);
                            } else {
                                amount = 2f * attempt;
                                if (attempt % 2 == 0) {
                                    left = true;
                                } else {
                                    left = false;
                                }
                            }

                            if (left) {
                                newDir = startDir.rotateLeftDegrees(amount);
                            } else {
                                newDir = startDir.rotateRightDegrees(amount);
                            }

                            directionPicker = myLocation.add(newDir, maxMove);
                            if (rc.canMove(directionPicker)) {
                                doingTreeMovement = true;
                                addSpecialMapLocation(directionPicker, 40);
                                addVector(directionPicker, 8);


                                if (directionLeaning == -1) {
                                    //Ok, we know now what way is fastest to our source
                                    if (left) {
                                        directionLeaning = 0;
                                    } else {
                                        directionLeaning = 1;
                                    }
                                    directionDecisionCooldown = 120;
                                }

                                found = true;
                                break;
                            } else {
                                directionPicker = myLocation.add(newDir, 0.5f); //Attempt again with smaller stride

                                if (rc.canMove(directionPicker)) {
                                    doingTreeMovement = true;
                                    addSpecialMapLocation(directionPicker, 40);
                                    addVector(directionPicker, 8);

                                    if (directionLeaning == -1) {
                                        if (left) {
                                            directionLeaning = 0;
                                        } else {
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

            if (distance < tree.radius + 3f) {
                closeTrees++;
            }

            if (tree.getContainedBullets() > 0 && rc.canShake(tree.ID)) {
                rc.shake(tree.ID);
            }
            if (!tree.team.equals(ally)) {
                if (!rc.hasAttacked()) {
                    if (tree.getContainedRobot() != null) {

                        int desire = 6;
                        if (distance < 3) {
                            desire += 4;
                        }
                        addVector(loc, desire);
                    } else if (shouldWoodcut) {
                        float healthScore = tree.getHealth();
                        if (woodcuttingJob != null) {
                            healthScore += 2 * tree.location.distanceTo(woodcuttingJob);
                        } else {
                            healthScore += 2 * tree.location.distanceTo(home);
                        }
                        if (healthScore < bestHealth) {
                            bestTree = nearbyTrees[i].ID;
                            bestHealth = healthScore;
                        }
                        if (healthScore < bestHealth) {
                            bestTree = i;
                            bestHealth = healthScore;
                        }
                    }
                }

                if (tree.team.equals(enemy)) {
                    if (shouldConfirmEnemyTreeLoc) {
                        shouldConfirmEnemyTreeLoc = false;
                        rc.broadcast(CONFIRMED_ENEMY_TREE_X, (int) loc.x);
                        rc.broadcast(CONFIRMED_ENEMY_TREE_Y, (int) loc.y);
                        rc.broadcast(CONFIRMED_ENEMY_TREE_TURN, turn);
                    }
                }
            }
        }

        MapLocation friendlyarchon1 = null;
        MapLocation friendlyarchon2 = null;
        MapLocation friendlyarchon3 = null;
        if (turn - rc.readBroadcast(OUR_ARCHON_1_LASTSEEN) < 5) {
            friendlyarchon1 = new MapLocation(rc.readBroadcast(OUR_ARCHON_1_X), rc.readBroadcast(OUR_ARCHON_1_Y));
        }
        if (turn - rc.readBroadcast(OUR_ARCHON_2_LASTSEEN) < 5) {
            friendlyarchon2 = new MapLocation(rc.readBroadcast(OUR_ARCHON_2_X), rc.readBroadcast(OUR_ARCHON_3_Y));
        }
        if (turn - rc.readBroadcast(OUR_ARCHON_3_LASTSEEN) < 5) {
            friendlyarchon3 = new MapLocation(rc.readBroadcast(OUR_ARCHON_2_X), rc.readBroadcast(OUR_ARCHON_3_Y));
        }


        boolean ignoreBullets = false;
        boolean alreadyVectoring = false;

        for (int i = 0; i < robots.length; i++) {

            RobotInfo r = robots[i];
            MapLocation loc = r.getLocation();

            if (r.team.equals(enemy)) {
                float distance = myLocation.distanceTo(r.getLocation());
                float desire = 8 - distance;

                if (rc.canStrike()) {
                    desire += 2;
                }

                if (distance < 3.5f + r.getRadius()) {
                    desire += 5;
                }

                if (closeTrees > 2) {
                    desire += 3; //Easier to catch them in areas with trees
                }

                if (r.type.equals(RobotType.SCOUT)) {

                    if(!rc.isLocationOccupiedByTree(loc)) {
                        desire += 6; //Shoo, you pesky scouts
                    }else{
                        desire += 1;
                    }
                } else if (r.type.equals(RobotType.GARDENER)) {
                    desire += 7;
                    if (rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_TURN) != turn) {
                        rc.broadcast(CONFIRMED_ENEMY_GARDENER_X, (int) (robots[i].location.x * 100));
                        rc.broadcast(CONFIRMED_ENEMY_GARDENER_Y, (int) (robots[i].location.y * 100));
                        rc.broadcast(CONFIRMED_ENEMY_GARDENER_TURN, turn);
                    }
                } else if (r.type.equals(RobotType.LUMBERJACK)) {

                    if (rc.hasAttacked()) {
                        if (rc.getHealth() > r.getHealth() + 6) {
                            desire = 0;
                        } else {
                            desire = -10;
                        }
                    } else {
                        desire += 10;
                    }

                    if (friendlyarchon1 != null && friendlyarchon1.distanceTo(loc) < 6) {
                        if (desire < 0) desire = 0;
                        desire += 5;
                    }
                    if (friendlyarchon2 != null && friendlyarchon2.distanceTo(loc) < 6) {
                        if (desire < 0) desire = 0;
                        desire += 5;
                    }
                    if (friendlyarchon3 != null && friendlyarchon3.distanceTo(loc) < 6) {
                        if (desire < 0) desire = 0;
                        desire += 5;
                    }

                }
                else if (r.type.equals(RobotType.SOLDIER)){
                    if(distance > 6.5f){
                        //We usually want to run away, but will only fight if they're on our archons
                        desire = -30;

                    }
                    else if(distance > 5.5f){
                        //In this area, whether we want to go in or not is kind of ambigious. However, we should really stick
                        //with one choice, otherwise we'll end up running back/forth while getting shot to death
                        int tally = 0;
                        if(rc.getHealth() < r.health){
                            tally -= 3;
                        } else{
                            tally += 3;
                        }
                        if(woodcuttingJob != null){
                            tally -= 2;
                        }
                        if(shouldWoodcut){
                            tally -= 1;
                        }
                        if(rc.isLocationOccupied(myLocation.add(loc.directionTo(myLocation),1.5f))){
                            tally += 4;
                        }
                        if(rc.getHealth() < 20){
                            tally += 2;
                        }
                        if(rc.getHealth() < 50){
                            tally -= 4;
                        }

                        if(tally > 0){
                            desire = 20;
                        }else{
                            desire = -10;
                        }
                    }
                    else{
                        //Close enough that we probably should run in, no matter what, despite possibly dying
                        desire = 20;
                    }

                    if (friendlyarchon1 != null && friendlyarchon1.distanceTo(loc) < 5) {
                        desire = 15;
                    }
                    if (friendlyarchon2 != null && friendlyarchon2.distanceTo(loc) < 5) {
                        desire = 15;
                    }
                    if (friendlyarchon3 != null && friendlyarchon3.distanceTo(loc) < 5) {
                        desire = 15;
                    }
                }
                else if ( r.type.equals(RobotType.TANK)) {
                    desire -= 105;
                    if (rc.readBroadcast(CONFIRMED_ENEMY_TANK_TURN) != turn) {
                        rc.broadcast(CONFIRMED_ENEMY_TANK_X, (int) (robots[i].location.x * 10));
                        rc.broadcast(CONFIRMED_ENEMY_TANK_Y, (int) (robots[i].location.y * 10));
                        rc.broadcast(CONFIRMED_ENEMY_TANK_TURN, turn);
                    }

                } else if (r.type.equals(RobotType.ARCHON)) {
                    if (rc.getHealth() < 50) {
                        desire += 7;
                    }
                }

                if (desire > 10) {
                    ignoreBullets = true;
                    addSpecialMapLocation(myLocation.add(myLocation.directionTo(loc), maxMove), 10);
                }else if(desire < 0){
                    addSpecialMapLocation(myLocation.add(loc.directionTo(myLocation).rotateLeftDegrees(5), maxMove), 10);
                    addSpecialMapLocation(myLocation.add(loc.directionTo(myLocation).rotateRightDegrees(5), maxMove), 10);
                }

                if (r.location.distanceTo(myLocation) < 2.76f + r.getRadius()) {
                    addCircularDesireZone(loc, 2f + r.getRadius(), 40); //Facerush the enemy
                }
                if(!alreadyVectoring || desire < -10) {
                    addVector(loc, desire * 1.5f);
                    alreadyVectoring = true;
                }else{
                    addVector(loc, desire / 4); //We still want to care a little to better position ourselves, but we only run at one
                }

                if (shouldConfirmEnemyLoc && !r.type.equals(RobotType.SCOUT)) {
                    rc.broadcast(CONFIRMED_ENEMY_X, (int) robots[i].getLocation().x);
                    rc.broadcast(CONFIRMED_ENEMY_Y, (int) robots[i].getLocation().y);
                    rc.broadcast(CONFIRMED_ENEMY_TURN, turn);
                    shouldConfirmEnemyLoc = false;
                }

            } else {
                if(shouldWoodcut && woodcuttingJob != null) {
                    if (r.location.distanceTo(myLocation) < 2.76f + r.getRadius()) {
                        if(r.type.equals(RobotType.LUMBERJACK)) {
                            addCircularDesireZone(loc, 2f + r.getRadius(), -80); //But dodge friends, don't want to strike them
                        }
                        else{
                            addCircularDesireZone(loc, 2f + r.getRadius(), -40); //But dodge friends, don't want to strike them
                        }
                    }
                    addVector(loc, -1);
                }
                else{
                    if(enemyForceDetectedThisTurn > 0){
                        if (r.location.distanceTo(myLocation) < 2.76f + r.getRadius()) {

                            addCircularDesireZone(loc, 2f + r.getRadius(), -15); //But dodge friends, don't want to strike them
                        }
                    }else{
                        if (r.location.distanceTo(myLocation) < 2.76f + r.getRadius()) {
                            addCircularDesireZone(loc, 2f + r.getRadius(), -5); //But dodge friends, don't want to strike them
                        }
                    }
                }
            }
        }


        if (!ignoreBullets) {
            dodgeBullets(rc.senseNearbyBullets());
        }


        if (shouldWoodcut && woodcuttingJob != null) {
//            rc.setIndicatorLine(myLocation,woodcuttingJob,0,255,0);
            addVector(woodcuttingJob, 10f);
            if (bestTree >= 0) {
                addVector(rc.senseTree(bestTree).getLocation(), 4f);
            }
//            System.out.println("followingjob" +  woodcuttingJob);
        } else if (shouldWoodcut) {
            if (bestTree >= 0) {
                addVector(rc.senseTree(bestTree).getLocation(), 4f);
            }

            int AggrMainX = rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_X);

            if (AggrMainX > 0) {
                addVectorSqrt(AggrMainX, rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_Y), 1f);
            } else {
                addVectorSqrt(getRandomGoal(), 1);
                desireTowardsMiddle(-2);
                if (rc.getID() % 3 == 0) {
                    addVectorSqrt(home.x, rc.readBroadcast(OPPOSITE_HOME_Y), 1);
                } else if (rc.getID() % 3 == 1) {
                    addVectorSqrt(rc.readBroadcast(OPPOSITE_HOME_X), home.y, 1);
                } else {
                    addVectorSqrt(rc.readBroadcast(OPPOSITE_HOME_X), rc.readBroadcast(OPPOSITE_HOME_Y), 1);
                }
            }

        } else {
            if (bestTree >= 0) {
                addVector(rc.senseTree(bestTree).getLocation(), 2f);
            }

            int AggrMainX = rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_X);

            if (AggrMainX > 0) {
                addVectorSqrt(AggrMainX, rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_Y), 3f);
                //System.out.println("main target chasing " + AggrMainX + " ," + rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_Y));

            } else {
                addVectorSqrt(getRandomGoal(), 1);
                desireTowardsMiddle(-2);

                if (rc.getID() % 3 == 0) {
                    addVectorSqrt(rc.readBroadcast(HOME_X), rc.readBroadcast(OPPOSITE_HOME_Y), 1);
                    //System.out.println("Running 'left'");
                } else if (rc.getID() % 3 == 1) {
                    addVectorSqrt(rc.readBroadcast(OPPOSITE_HOME_X), rc.readBroadcast(HOME_Y), 1);
                    //System.out.println("Running 'right'");
                } else {
                    addVectorSqrt(rc.readBroadcast(OPPOSITE_HOME_X), rc.readBroadcast(OPPOSITE_HOME_Y), 1);
                    //System.out.println("Running opposite");
                }

            }

            if (woodcuttingJob != null) {
                addVector(woodcuttingJob, 1.5f);
            }

            int AggrSecondaryX = rc.readBroadcast(Archon.SECONDARY_AGRESSIVE_TARGET_X);

            if (AggrSecondaryX > 0) {
                addVectorSqrt(AggrSecondaryX, rc.readBroadcast(Archon.SECONDARY_AGRESSIVE_TARGET_Y), 0.8f);
                //System.out.println("second target chasing " + AggrSecondaryX + " ," + rc.readBroadcast(Archon.SECONDARY_AGRESSIVE_TARGET_X));

            }
        }


        int DefMainX = rc.readBroadcast(Archon.MAIN_DEFENSIVE_TARGET_X);

        if (DefMainX > 0) {
            MapLocation m = new MapLocation(DefMainX, rc.readBroadcast(Archon.MAIN_DEFENSIVE_TARGET_Y));
            //System.out.println("Going to def: " + m);

            if (myLocation.distanceTo(m) < 15) {
                addVectorSqrt(m, rc.readBroadcast(DEFENSE_LEVEL) * 1.5f);
            } else {
                addVectorSqrt(m, rc.readBroadcast(DEFENSE_LEVEL));
            }
        }


        doMovement(2000);

        myLocation = rc.getLocation();

        int allyCount = 0;
        int enemyCount = 0;
        //Now try striking again
        if (rc.canStrike()) {
            for (int i = robots.length - 1; i >= 0; i--) {
                if (robots[i].getLocation().isWithinDistance(myLocation, 2 + robots[i].getRadius())) {
                    if (robots[i].team.equals(ally)) {
                        if (robots[i].getType().equals(RobotType.SOLDIER) || robots[i].getType().equals(RobotType.LUMBERJACK) || robots[i].getType().equals(RobotType.TANK)) {
                            allyCount += 3;
                        }
                        else if(robots[i].getType().equals(RobotType.GARDENER)){
                            allyCount += 2;
                        }
                        else {
                            allyCount++;
                        }
                        if (robots[i].getHealth() < 20) {
                            allyCount++;
                        }
                    } else {
                        enemyCount += 2;
                        if (robots[i].getHealth() < 20) {
                            enemyCount++;
                        }
                        if (turnsLeft < 200 || rc.getOpponentVictoryPoints() > 600) {
                            enemyCount += 2;
                        }
                        if (robots[i].getType().equals(RobotType.TANK) || robots[i].getType().equals(RobotType.SCOUT) || robots[i].getType().equals(RobotType.GARDENER)) {
                            enemyCount++;
                        }
                    }
                }
            }
            if (enemyCount > 0 && enemyCount >= allyCount) {
                rc.strike();
            }
        }

        if (!rc.hasAttacked()) {
            bestTree = -1;
            bestHealth = 100000;
            int trees = 0;
            boolean forceChop = false;

            for (int i = 0; i < nearbyTrees.length && i < 15; i++) {
                if (!nearbyTrees[i].team.equals(ally)) {

                    if (nearbyTrees[i].location.distanceTo(myLocation) < 2 + nearbyTrees[i].getRadius()) {
                        trees++;
                    }
                    if (rc.canChop(nearbyTrees[i].ID)) {

                        float healthScore = nearbyTrees[i].getHealth();
                        if (woodcuttingJob != null) {
                            healthScore += 5 * nearbyTrees[i].location.distanceTo(woodcuttingJob);
                        }

                        healthScore -= (nearbyTrees[i].maxHealth * 1.5f);

                        if(nearbyTrees[i].containedRobot != null){
                            healthScore -= 20;

                            if(nearbyTrees[i].containedRobot == RobotType.TANK){
                                healthScore -= 350;
                            } else if(nearbyTrees[i].containedRobot == RobotType.LUMBERJACK){
                                healthScore -= 200;
                            } else if(nearbyTrees[i].containedRobot == RobotType.SOLDIER){
                                healthScore -= 90;
                            }
                            forceChop = true;
                        }

                        if (healthScore < bestHealth) {
                            bestTree = nearbyTrees[i].ID;
                            bestHealth = healthScore;
                        }
                    }
                } else {
                    if (nearbyTrees[i].location.distanceTo(myLocation) < 2 + nearbyTrees[i].getRadius()) {
                        trees--;
                    }
                }
            }

            if (trees >= 3 && !forceChop && allyCount == 0) {
                rc.strike();
            } else if (bestTree >= 0) {
                rc.chop(bestTree);
            }
        }

        if (woodcuttingJob != null) {
            if (rc.canSenseLocation(woodcuttingJob)) {
                if (!rc.isLocationOccupiedByTree(woodcuttingJob)) {
                    woodcuttingJob = null; //we're done!
                } else if (rc.senseTreeAtLocation(woodcuttingJob).getTeam().equals(ally)) {
                    woodcuttingJob = null;
                }
            }
        }

        if(rc.hasAttacked()){
            turnsNotDoneAnything = 0;
        }else{
            turnsNotDoneAnything++;
        }

        prevLoc3 = prevLoc2;
        prevLoc2 = prevLoc1;
        prevLoc1 = prevLoc;
        prevLoc = myLocation;
    }




    public void initial(){
        attackpower = 2;
        maxHp = RobotType.LUMBERJACK.maxHealth;
        radius =  RobotType.LUMBERJACK.bodyRadius;
        sightradius = RobotType.LUMBERJACK.sensorRadius;
        bulletsightradius = RobotType.LUMBERJACK.bulletSightRadius;
        maxMove = RobotType.LUMBERJACK.strideRadius;
    }
}
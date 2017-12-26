package bruteforcer;

import battlecode.common.*;

/**
 * Created by Hermen on 10/1/2017.
 */
public class Tank extends RobotBase {

    static MapLocation prevLoc = null;
    static MapLocation prevLoc1 = null;
    static MapLocation prevLoc2 = null;
    static MapLocation prevLoc3 = null;

    static int enemyArchonCount = 1;
    static int directionDecisionCooldown = 0;
    static int treesDetected = 0;

    static MapLocation someoneFiringAtUs = null;

    static MapLocation lastturnsomeonfiringatus = null;



    public void step() throws Exception {

//        System.out.print("DELETE THIS");
//        rc.broadcast(1000,1); //TEST, DELETE THIS!!!

        MapLocation myLocation = RobotBase.myLocation;
        RobotController rc = RobotBase.rc;


        directionDecisionCooldown--;
        treesDetected = 0;
        int lastAggrUpdate = turn - rc.readBroadcast(AGRESSIVE_TARGETS_LAST_UPDATE);
        int lastStratUpdate = turn - rc.readBroadcast(LAST_UPDATED_GRAND_STRAT);
        int agression = 4;
        int defensive = 2;
        if (lastStratUpdate < 10) {
            agression = rc.readBroadcast(AGRESSION_LEVEL);
            defensive = rc.readBroadcast(DEFENSE_LEVEL);
        }

        BulletInfo[] bullets = rc.senseNearbyBullets();
        dodgeBullets(bullets);


        cluster1 = null;
        cluster2 = null;
        cluster3 = null;

        if (turn - rc.readBroadcast(CLUSTER1_TURN) < 2) {
            cluster1 = new MapLocation(((float) rc.readBroadcast(CLUSTER1_X)) / 10f, ((float) rc.readBroadcast(CLUSTER1_Y)) / 10f);

            float dist = myLocation.distanceTo(cluster1);
            if (dist < 15) {
                //Chase this group
                addVector(cluster1, 2);
            }

        }
        if (turn - rc.readBroadcast(CLUSTER2_TURN) < 2) {
            cluster2 = new MapLocation(((float) rc.readBroadcast(CLUSTER2_X)) / 10f, ((float) rc.readBroadcast(CLUSTER2_Y)) / 10f);

            float dist = myLocation.distanceTo(cluster2);
            if (dist < 20) {
                //Chase this group
                addVector(cluster2, 2);
            }
        }
        if (turn - rc.readBroadcast(CLUSTER3_TURN) < 2) {
            cluster3 = new MapLocation(((float) rc.readBroadcast(CLUSTER3_X)) / 10f, ((float) rc.readBroadcast(CLUSTER3_Y)) / 10f);
            float dist = myLocation.distanceTo(cluster3);
            if (dist < 20) {
                //Chase this group
                addVector(cluster3, 2);
            }
        }



        if(turn % 3 == 0){
            enemyArchonCount = 0;
            if(turn - rc.readBroadcast(THEIR_ARCHON_1_LASTSEEN) < 30){
                enemyArchonCount++;
            }
            if(rc.readBroadcast(THEIR_ARCHON_2_ID) > -1 && turn - rc.readBroadcast(THEIR_ARCHON_1_LASTSEEN) < 30){
                enemyArchonCount++;
            }
            if(rc.readBroadcast(THEIR_ARCHON_3_ID) > -1 && turn - rc.readBroadcast(THEIR_ARCHON_3_LASTSEEN) < 30){
                enemyArchonCount++;
            }
        }

        // See if there are any nearby enemy robots
        RobotInfo[] robots = rc.senseNearbyRobots();

        float alliedTeamStrength = rc.getHealth() / 30;
        float enemyTeamStrength = 0;

        int totalEnemies = 0;
        boolean enemyGardernerDetected = false;

        for (int i = robots.length - 1; i >= 0; i--) {

            RobotInfo r = robots[i];
            MapLocation loc = r.getLocation();

            double health = r.health;

            if (r.team.equals(enemy)) {
                totalEnemies++;

                if (r.type.equals(RobotType.SOLDIER)) {
                    enemyTeamStrength += health / 30;

                } else if (r.type.equals(RobotType.LUMBERJACK)) {
                    addDesireZone(r.location,5.1f, -20);
                    enemyTeamStrength += health / 50;
                } else if (r.type.equals(RobotType.TANK)) {
                    enemyTeamStrength += health / 20;
                    addVector(loc,-10); //We want to avoid enemy tanks, because we have superior sniping
                    addCircularDesireZone(loc,9,-40);


                    if(rc.readBroadcast(CONFIRMED_ENEMY_TANK_TURN) != turn) {
                        rc.broadcast(CONFIRMED_ENEMY_TANK_X, (int) (robots[i].location.x * 10));
                        rc.broadcast(CONFIRMED_ENEMY_TANK_Y, (int) (robots[i].location.y * 10));
                        rc.broadcast(CONFIRMED_ENEMY_TANK_TURN, turn);
                    }
                } else if (r.type.equals(RobotType.ARCHON)) {
                    if (health < 100) {
                        addVector(loc,7);
                    }

                        broadcastEnemyArchon(r.ID, (int) loc.x, (int) loc.y);
                        if (lastAggrUpdate >= 10) {
                            //well, if the archon ain't doing it
                            rc.broadcast(MAIN_AGRESSIVE_TARGET_X, (int) loc.x);
                            rc.broadcast(MAIN_AGRESSIVE_TARGET_Y, (int) loc.y);
                            rc.broadcast(AGRESSIVE_TARGETS_LAST_UPDATE, turn);
                        }

                } else if (r.type.equals(RobotType.GARDENER)) {
                    enemyGardernerDetected = true;
                }
            } else {
                if (r.type.equals(RobotType.SOLDIER)) {
                    alliedTeamStrength += health / 30;
                    addDesireZone(loc, 5.1f, -7);

                } else if (r.type.equals(RobotType.LUMBERJACK)) {
                    alliedTeamStrength += health / 50;
                    addDesireZone(loc, 5.1f, -20);

                } else if (r.type.equals(RobotType.TANK)) {
                    alliedTeamStrength += health / 20;
                    addDesireZone(loc, 6f, -25);

                }

            }
        }

        int AggrMainX = rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_X);

        if (lastAggrUpdate < 10 && AggrMainX >= 0) {
            addVector(AggrMainX, rc.readBroadcast(Archon.MAIN_AGRESSIVE_TARGET_Y), agression);
            //System.out.println("Agrresion" + agression);
        } else {
            addVector(getRandomGoal(), 2); //Halp, we don't know what do
            desireTowardsMiddle(-3);

            if(rc.getID() + (turn / 10) % 3 == 0){
                addVector(rc.readBroadcast(HOME_X),rc.readBroadcast(OPPOSITE_HOME_Y),1);
            }else if(rc.getID()+ (turn / 10) % 3 == 1){
                addVector(rc.readBroadcast(OPPOSITE_HOME_X),rc.readBroadcast(HOME_Y),1);
            } else{
                addVector(rc.readBroadcast(OPPOSITE_HOME_X),rc.readBroadcast(OPPOSITE_HOME_Y),1);
            }
        }


        if (totalEnemies > 0) {
            float repelStr = 0;
            if ((enemyTeamStrength + (defensive * 2)) * 1.1 > (alliedTeamStrength + (agression * 2))) {
                repelStr = -5 / totalEnemies; //flee

            } else {
                repelStr = 5 / totalEnemies; //advance
            }

            for (int i = robots.length - 1; i >= 0; i--) {
                if (robots[i].team.equals(enemy)) {
                    addVector(robots[i].getLocation(), repelStr);
                }
            }
        }

        TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
        boolean triedTreeMovement = false;

        for (int i = 0; i < nearbyTrees.length; i++) {
            treesDetected++;
            TreeInfo tree = nearbyTrees[i];
            MapLocation loc = tree.getLocation();
            float distance = loc.distanceTo(myLocation);

            if (tree.getContainedBullets() > 0 && rc.canShake(tree.ID)) {
                rc.shake(tree.ID);
            }

            if(distance < nearbyTrees[i].radius + 2.3) {
                if (!tree.team.equals(ally) && nearbyTrees.length > 2) {
                    addDesireZone(loc, nearbyTrees[i].getRadius() - 0.2f, 15); // Let's run over trees
                } else {
                    addDesireZone(loc, nearbyTrees[i].getRadius() - 0.2f, -70); // Let's not run over our own trees
                    addVector(loc,-2);
                 //   addSidewaysVector(loc,4);
                }
            }

            if (!triedTreeMovement && (directionDecisionCooldown >= 0 || directionDecisionCooldown < -15)   &&  (tree.team.equals(ally) || nearbyTrees.length < 3)) {
                if (distance < tree.radius + 2.3f) {
                    //Ok, we may be stuck here. By now we have a good idea what our movement vector will look like. Use that to figure out which side we should be going in, so let's check
                    Direction vectorDir = myLocation.directionTo(new MapLocation(myLocation.x + moveVectorX, myLocation.y + moveVectorY));
                    MapLocation directionDesire = myLocation.add(vectorDir, 0.1f);

                    float treeToApproxDist = tree.location.distanceTo(directionDesire);
                    if (distance > treeToApproxDist) {
                        //Aha, we do want to go in that direction


                        if (directionLeaning == -1) {
                            // Pick either rightleaning or leftleaning, depending on which is closer and create some vector in that direction. Stick to our choice for a bit
                            MapLocation turnLeftLoc = myLocation.add(myLocation.directionTo(loc).rotateLeftDegrees(30), 0.1f);
                            MapLocation turnRightLoc = myLocation.add(myLocation.directionTo(loc).rotateRightDegrees(30), 0.1f);

                            if (turnLeftLoc.distanceTo(directionDesire) < turnRightLoc.distanceTo(directionDesire)) {
                                directionLeaning = 0;
                            } else {
                                directionLeaning = 1;
                            }
                            directionDecisionCooldown = 50;
                        }

                        //Now find the first spot that we can actually get to
                        MapLocation directionPicker;

                        boolean found = false;
                        for (int attempt = 0; attempt < 36; attempt++) {

                            if (directionLeaning == 0) {
                                directionPicker = myLocation.add(vectorDir.rotateLeftDegrees(attempt * 10), maxMove - 0.01f);

                            } else {
                                directionPicker = myLocation.add(vectorDir.rotateRightDegrees(attempt * 10), maxMove - 0.01f);

                            }
                            if (rc.canMove(directionPicker) && !rc.isCircleOccupiedExceptByThisRobot(directionPicker,2) && rc.onTheMap(directionPicker)) {
                                addSpecialMapLocation(directionPicker,40);
                                found = true;
                                break;
                            }
                        }
                        if(!found){
                            directionDecisionCooldown -= 10;
                        }

                        triedTreeMovement = true;

                    }
                }
            }
        }


        //We don't see any enemies. But we can see bullets, and bullets can tell us where the enemies are!
        if(enemyTeamStrength <= 0) {
            someoneFiringAtUs = null;
            BulletInfo[] firedAtUs = new BulletInfo[6];
            int foundBullets = 0;
            //Find a soldier bullet clearly shot at us. Starting with furthest for best data.
            // We wont handle tank, because they could be very far/ we could end up having tanks firing at each other
            for (int i = bullets.length - 1; i >= 0 && foundBullets < 6; i--) {
                BulletInfo bullet = bullets[i];
                if (bullet.damage > 1.5f && bullet.damage < 2.5f) {
                    Direction dir = bullet.location.directionTo(myLocation);
                    if (dir.degreesBetween(bullet.dir) < 5) {
                        //Extra check
                        float distance = bullet.location.distanceTo(myLocation);
                        float distance2 = bullet.location.add(bullet.dir, 1).distanceTo(myLocation);
                        if (distance2 + 0.9f < distance) {
                            firedAtUs[foundBullets] = bullet;
                            foundBullets++;
//                        rc.setIndicatorDot(bullet.location,200,0,0);
                        }
                    }
                }
            }

            MapLocation[] possibleSpots = new MapLocation[6];
            int foundSpots = 0;
            if (firedAtUs != null) {
                for (int i2 = 0; i2 < foundBullets; i2++) {
                    Direction opposite = firedAtUs[i2].dir.opposite();
                    for (int i = 0; i < 10; i++) {
                        MapLocation newSpot = firedAtUs[i2].location.add(opposite, i * 1f);
                        if (myLocation.distanceTo(newSpot) > 8.4f) { //This is approx the distance we expect soldiers at
                            possibleSpots[foundSpots] = newSpot;
                            foundSpots++;
//                        rc.setIndicatorDot(newSpot,0,0,255);
                            break;
                        }
                    }
                }
            }

            if(lastturnsomeonfiringatus != null){
                for (int i = 0; i < foundSpots; i++) {
                    if(lastturnsomeonfiringatus.distanceTo(possibleSpots[i]) < 0.5f){
                        someoneFiringAtUs = possibleSpots[i]; //  match!
                        break;
                    }
                }
            }

            if(someoneFiringAtUs == null) {
                outerloop:
                for (int i = 0; i < foundSpots; i++) {
                    for (int i2 = 0; i2 < foundSpots; i2++) {
                        if (i != i2) {
                            if (possibleSpots[i].distanceTo(possibleSpots[i2]) < 1) {
                                someoneFiringAtUs = possibleSpots[i]; //  match!
//                            rc.setIndicatorLine(myLocation, someoneFiringAtUs, 255, 0, 0);
                                break outerloop;
                            }
                        }
                    }
                }
            }

            lastturnsomeonfiringatus = someoneFiringAtUs;
        }




        if(turn % 3 == 0) {
            if (rc.readBroadcast(TANKS_REPORTING_IN_TURN) != turn) {
                rc.broadcast(TANKS_REPORTING_IN_TURN, turn);
                rc.broadcast(TANKS_REPORTING_IN, 1);
            }
            else{
                rc.broadcast(TANKS_REPORTING_IN, rc.readBroadcast(TANKS_REPORTING_IN) + 1);
            }
        }





        int approxBytsReq = 2500;

        if(robots.length < 15){
            approxBytsReq += robots.length * 120;
        }
        else{
            approxBytsReq += 1800;
        }
        if(nearbyTrees.length < 13){
            approxBytsReq += nearbyTrees.length * 100;
        }
        else{
            approxBytsReq += 1300;
        }

        if (approxBytsReq + 1000 > Clock.getBytecodesLeft()) {
            doFiringLogic(rc.senseNearbyRobots(), rc.senseNearbyTrees(), enemyGardernerDetected, agression);
        }
        else {
            doMovement(approxBytsReq);
            doFiringLogic(rc.senseNearbyRobots(), rc.senseNearbyTrees(), enemyGardernerDetected, agression);
        }

//        if(!rc.hasAttacked()){
//            rc.setIndicatorDot(myLocation, 100, 40, 40);
//        }

        prevLoc3 = prevLoc2;
        prevLoc2 = prevLoc1;
        prevLoc1 = prevLoc;
        prevLoc = myLocation;

    }




    public void doFiringLogic(RobotInfo[] robots, TreeInfo[] nearbyTrees, boolean enemyGardenerDetected, int agression) throws GameActionException {





        if(!rc.canFireSingleShot() ){
            return;
        }
        //We slide the circle around our unit into 18 segments. Then well look how many enemy targets there are here, as well as how many allies and add up a total score.
        //Then, we shoot at an enemy we found inside of this circleslice
        float[] targetImportance = new float[18];
        Direction[] representativeForSlot = new Direction[18];


        boolean emergencyMode = false;

        if(rc.getOpponentVictoryPoints() - 300 > rc.getTeamVictoryPoints()){
            emergencyMode = true;
        }

        int markedTargetId = -1;
        if (rc.readBroadcast(MARKED_TARGET_TURN) == turn) {
            markedTargetId = rc.readBroadcast(MARKED_TARGET_ID);
        }

        boolean foundVeryCloseScout = false;

        boolean allowedToDoLargerShots = false;


        int treeCount = rc.getTreeCount();

        if(turn - rc.readBroadcast(CONFIRMED_ENEMY_TURN) < 1) {

            MapLocation snipeSpot = new MapLocation(rc.readBroadcast(CONFIRMED_ENEMY_X),rc.readBroadcast(CONFIRMED_ENEMY_Y));

            if((myLocation.distanceTo(snipeSpot) < 10  && (treesDetected < 5 || treeCount > 10))  || (myLocation.distanceTo(snipeSpot) < 14 && (treesDetected < 2 || treeCount > 13)) || (myLocation.distanceTo(snipeSpot) < 16 && (treesDetected == 0 || treeCount > 20))) {
                Direction dir = myLocation.directionTo(snipeSpot);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                representativeForSlot[slot] = dir;
                targetImportance[slot] += 7;
            }
        }

        if(turn - rc.readBroadcast(MARKED_TARGET_TURN) < 1) {

            MapLocation snipeSpot = new MapLocation(rc.readBroadcast(MARKED_TARGET_X) / 100f,rc.readBroadcast(MARKED_TARGET_Y) / 100f);

            if((myLocation.distanceTo(snipeSpot) < 11  && (treesDetected < 5 || treeCount > 10)) ) {

//                rc.setIndicatorDot(snipeSpot, 255,0,180);

                Direction dir = myLocation.directionTo(snipeSpot);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                representativeForSlot[slot] = dir;
                targetImportance[slot] += 7;
            }
        }

        if (cluster1 != null) {
            //Let's shoot when it's just out of our sensor range (or inside, we don't mind adding a little here)
            if (myLocation.distanceTo(cluster1) < 12) {
                Direction dir = myLocation.directionTo(cluster1);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                if(emergencyMode){
                    targetImportance[slot] += rc.readBroadcast(CLUSTER1_SIZE);
                }else {
                    targetImportance[slot] += rc.readBroadcast(CLUSTER1_SIZE);
                }
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
                if(emergencyMode){
                    targetImportance[slot] += rc.readBroadcast(CLUSTER2_SIZE);
                }else {
                    targetImportance[slot] += rc.readBroadcast(CLUSTER2_SIZE);
                }            }
        }
        if (cluster3 != null) {
            //Let's shoot when it's just out of our sensor range (or inside, we don't mind adding a little here)
            if (myLocation.distanceTo(cluster3) < 12) {
                Direction dir = myLocation.directionTo(cluster3);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                if(emergencyMode){
                    targetImportance[slot] += rc.readBroadcast(CLUSTER3_SIZE);
                }else {
                    targetImportance[slot] += rc.readBroadcast(CLUSTER3_SIZE);
                }
            }
        }


        if(turn - rc.readBroadcast(CONFIRMED_ENEMY_TANK_TURN) < 3) {
            MapLocation snipeSpot = new MapLocation(((float)rc.readBroadcast(CONFIRMED_ENEMY_TANK_X)) / 10f,((float)rc.readBroadcast(CONFIRMED_ENEMY_TANK_Y)) / 10f);
            //Let max distance depend on how many trees we sort of expect in the path
            if((myLocation.distanceTo(snipeSpot) < 12 && (treesDetected < 4 || treeCount > 10)) || (myLocation.distanceTo(snipeSpot) < 15 && (treesDetected < 2 || treeCount > 13)) || (myLocation.distanceTo(snipeSpot) < 30 && (treesDetected == 0 || treeCount > 20))) {
                Direction dir = myLocation.directionTo(snipeSpot);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                representativeForSlot[slot] = dir;
                targetImportance[slot] += 8;
            }
        }



        if(turn - rc.readBroadcast(THEIR_ARCHON_1_LASTSEEN) < 3) {
            MapLocation snipeSpot = new MapLocation(((float)rc.readBroadcast(THEIR_ARCHON_1_X)) / 10f,((float)rc.readBroadcast(THEIR_ARCHON_1_Y)) / 10f);
            //Let max distance depend on how many trees we sort of expect in the path
            if((myLocation.distanceTo(snipeSpot) < 12 && (treesDetected < 4 || treeCount > 10)) || (myLocation.distanceTo(snipeSpot) < 15 && friendlyForceDetectedThisTurn < 1 && (treesDetected < 2 || treeCount > 13)) || (myLocation.distanceTo(snipeSpot) < 25 && friendlyForceDetectedThisTurn < 1 && (treesDetected == 0 || treeCount > 20))) {
                Direction dir = myLocation.directionTo(snipeSpot);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                representativeForSlot[slot] = dir;
                targetImportance[slot] += 3;
            }
        }
        if(turn - rc.readBroadcast(THEIR_ARCHON_2_LASTSEEN) < 3) {
            MapLocation snipeSpot = new MapLocation(((float)rc.readBroadcast(THEIR_ARCHON_2_X)) / 10f,((float)rc.readBroadcast(THEIR_ARCHON_2_Y)) / 10f);
            //Let max distance depend on how many trees we sort of expect in the path
            if((myLocation.distanceTo(snipeSpot) < 12 && (treesDetected < 4 || treeCount > 10)) || (myLocation.distanceTo(snipeSpot) < 15 && friendlyForceDetectedThisTurn < 1 && (treesDetected < 2 || treeCount > 13)) || (myLocation.distanceTo(snipeSpot) < 25 && friendlyForceDetectedThisTurn < 1&& (treesDetected == 0 || treeCount > 20))) {
                Direction dir = myLocation.directionTo(snipeSpot);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                representativeForSlot[slot] = dir;
                targetImportance[slot] += 3;
            }
        }
        if(turn - rc.readBroadcast(THEIR_ARCHON_3_LASTSEEN) < 3) {
            MapLocation snipeSpot = new MapLocation(((float)rc.readBroadcast(THEIR_ARCHON_3_X)) / 10f,((float)rc.readBroadcast(THEIR_ARCHON_3_Y)) / 10f);
            //Let max distance depend on how many trees we sort of expect in the path
            if((myLocation.distanceTo(snipeSpot) < 12 && (treesDetected < 4 || treeCount > 10)) || (myLocation.distanceTo(snipeSpot) < 15 && friendlyForceDetectedThisTurn < 1 && (treesDetected < 2 || treeCount > 13)) || (myLocation.distanceTo(snipeSpot) < 25 && friendlyForceDetectedThisTurn < 1  && (treesDetected == 0 || treeCount > 20))) {
                Direction dir = myLocation.directionTo(snipeSpot);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                representativeForSlot[slot] = dir;
                targetImportance[slot] += 3;
            }
        }

        if(turn - rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_TURN) < 3) {
            MapLocation snipeSpot = new MapLocation(((float)rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_X)) / 10f,((float)rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_Y)) / 10f);
            //Let max distance depend on how many trees we sort of expect in the path
            if((myLocation.distanceTo(snipeSpot) < 12 && (treesDetected < 4 || treeCount > 10)) || (myLocation.distanceTo(snipeSpot) < 18 && friendlyForceDetectedThisTurn < 1 && (treesDetected < 2 || treeCount > 13)) || (myLocation.distanceTo(snipeSpot) < 25 && friendlyForceDetectedThisTurn < 1 && (treesDetected == 0 || treeCount > 20)) || (emergencyMode && myLocation.distanceTo(snipeSpot) < 70)   || (myLocation.distanceTo(snipeSpot) < 60 && (treesDetected == 0 || treeCount > 20))) {
                Direction dir = myLocation.directionTo(snipeSpot);
                int slot = (int) (dir.radians * 2.8647f);
                if (slot < 0) {
                    slot += 18;
                }
                representativeForSlot[slot] = dir;
                targetImportance[slot] += 3;
            }
        }



        if(someoneFiringAtUs != null){
            Direction dir = myLocation.directionTo(someoneFiringAtUs);
            int slot = (int) (dir.radians * 2.8647f);
            if (slot < 0) {
                slot += 18;
            }
            representativeForSlot[slot] = dir;
            targetImportance[slot] += 8;
        }

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
                allowedToDoLargerShots = true;
                curScore = 0;

                if (markedTargetId == -1 && !r.type.equals(RobotType.SCOUT) && distance < 4.5f) {
                    markedTargetId = r.ID;
                    rc.broadcast(MARKED_TARGET_ID, r.ID);
                    rc.broadcast(MARKED_TARGET_TURN, turn);
                }

                if (r.ID == markedTargetId) { //Should theoretically make it much harder for this marked target to dodge
                    curScore += 4;
                }

                if (representativeForSlot[slot] == null) {
                    representativeForSlot[slot] = dir;
                }
                if (health < 20) {
                    curScore += 1;
                }

//                if(!r.getType().equals(RobotType.ARCHON)) {
//                    curScore += 7 - distance;
//
//                    if (distance <= 1.2f + r.getRadius()) {
//                        curScore += 20; //Just shoot, whatever this is
//                        representativeForSlot[slot] = originalEnemyDir;
//                    } else if (distance < 2f + r.getRadius() && !r.type.equals(RobotType.SCOUT)) {
//                        if (!rc.isCircleOccupied(myLocation.add(originalEnemyDir, 1.3f), 0.2f)) {
//                            curScore += 20; //Just shoot. We'll almost certainly hit
//                            representativeForSlot[slot] = originalEnemyDir;
//                        }
//                    }
//                    if (r.moveCount == 0) {
//                        if (distance < 3) { // Basically a guaranteed hit
//                            curScore += 15;
//                        } else if (distance < 5) {
//                            curScore += 9; //Also very easy hits
//                        }
//                    }
//                }

                switch (r.type){
                    case SCOUT:
                        curScore += 6;

                        if(distance <= 6.05f){
                            curScore += 8; //Very hard to dodge, but not sure if we should penta
                            representativeForSlot[slot] = dir;
                        }

                        break;
                    case SOLDIER:
                        curScore += 10; //Possible colleteral hit
                        if (distance <= 6.05f) {
                            curScore += 5; //Sure hit
                            representativeForSlot[slot] = dir;
                            if(distance < 4){
                                curScore += 5; //pentas
                            }
                        }

                        break;
                    case GARDENER:
                        curScore += 8;
                        if (distance <= 6.05f) {
                            curScore += 4; //Sure hit
                            representativeForSlot[slot] = dir;
                        }
                        break;
                    case ARCHON:
                        curScore = 8;
                        if (health < 70) {
                            curScore += 4; //Just finish it off fast

                        }

                        break;
                    case TANK:
                       curScore += 15; //Try pentads
                        representativeForSlot[slot] = dir;

                        break;
                    case LUMBERJACK:
                        curScore += 8;
                        if (distance <= 6.05f) {
                            curScore += 8; //Sure hit
                            representativeForSlot[slot] = dir;
                            if(distance < 4){
                                curScore += 5;
                            }
                        }

                        break;
                }
                if (health < 20) { //Focus fire
                    curScore += 2;
                }
            } else {
                if (r.type.equals(RobotType.ARCHON)) {
                    if (distance < 6) {
                        curScore -= 15;
                        if(health < 100){
                            curScore -= 30;
                        }
                    }
                }
                else{
                    if (distance < 4 + r.getRadius()) {
                        curScore -= 20;
                    }
                }
                curScore -= 10;
            }
            targetImportance[slot] += curScore;
        }

        if(Clock.getBytecodesLeft() < 600){
            return; //Well, too bad..
        }


        for (int i = 0; i < nearbyTrees.length-1 && i < 10; i++) {

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
                if (!enemyGardenerDetected) {
                    curScore += 5;
                }


            } else if (tree.team.equals(ally)) {
                if(!foundVeryCloseScout) {
                    curScore -= 2; //Don't shoot in one of our trees direction! Unless there's a scout sitting there of course (as that'll be hit before the tree)
                    if (distance < 2.5 + tree.getRadius() ) {
                        curScore -= 15; //Don't really want to shoot at trees if they're the closest
                    }
                }
            } else {
                if (distance < 2.5 + tree.getRadius()) {
                    curScore -= 15; //Don't really want to shoot at trees if they're the closest
                }
            }
            targetImportance[slot] += curScore;
        }


        if(Clock.getBytecodesLeft() < 450) return; //Well, too bad..

        //Struggling with bytecodes for soldiers. Just do it without loop..
        int bestSlot = 0;
        float bestScore = targetImportance[0] + (targetImportance[17] + targetImportance[1]) / 3;
        float score1 = targetImportance[1] + (targetImportance[0] + targetImportance[2]) / 3;
        float score2 = targetImportance[2] + (targetImportance[1] + targetImportance[3]) / 3;
        float score3 = targetImportance[3] + (targetImportance[2] + targetImportance[4]) / 3;
        float score4 = targetImportance[4] + (targetImportance[3] + targetImportance[5]) / 3;
        float score5 = targetImportance[5] + (targetImportance[4] + targetImportance[6]) / 3;
        float score6 = targetImportance[6] + (targetImportance[5] + targetImportance[7]) / 3;
        float score7 = targetImportance[7] + (targetImportance[6] + targetImportance[8]) / 3;
        float score8 = targetImportance[8] + (targetImportance[7] + targetImportance[9]) / 3;
        float score9 = targetImportance[9] + (targetImportance[8] + targetImportance[10]) / 3;
        float score10 = targetImportance[10] + (targetImportance[9] + targetImportance[11]) / 3;
        float score11 = targetImportance[11] + (targetImportance[10] + targetImportance[12]) / 3;
        float score12 = targetImportance[12] + (targetImportance[11] + targetImportance[13]) / 3;
        float score13 = targetImportance[13] + (targetImportance[12] + targetImportance[14]) / 3;
        float score14 = targetImportance[14] + (targetImportance[13] + targetImportance[15]) / 3;
        float score15 = targetImportance[15] + (targetImportance[14] + targetImportance[16]) / 3;
        float score16 = targetImportance[16] + (targetImportance[15] + targetImportance[17]) / 3;
        float score17 = targetImportance[17] + (targetImportance[16] + targetImportance[0]) / 3;
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

        if(turn - rc.readBroadcast(GARDENERS_REPORTING_IN_TURN) > 4){
            minBullets = 100;
        }

        if(rc.getRobotCount() <= 2){ //Well just go full out here, we're likely losing anyway. Just hope the enemy is equally weak
            bestScore += 4;
        }else if( turn > 150 && rc.getTreeCount() == 0){         //So we may be shooting a little too much...
            bestScore -= 1;
        }

//        System.out.println("best score:" + bestScore);


        if (bestScore > 2) {
            if (representativeForSlot[bestSlot] != null) {

                if (allowedToDoLargerShots && bestScore > 10 && rc.canFirePentadShot()) {
                    rc.firePentadShot(representativeForSlot[bestSlot]);

                    if (!rc.hasMoved()) {
                        addDesireZone(myLocation.add(representativeForSlot[bestSlot], 1.05f), 1.1f, -70); //Don't shoot urself in the face man
                        addDesireZone(myLocation.add(representativeForSlot[bestSlot].rotateRightDegrees(25), 1.05f), 1.1f, -70); //Don't shoot urself in the face man
                        addDesireZone(myLocation.add(representativeForSlot[bestSlot].rotateLeftDegrees(25), 1.05f), 1.1f, -70); //Don't shoot urself in the face man
                        //Try to do with just 3 zones instead of the 5 req
                    }
                } else if (allowedToDoLargerShots && bestScore > 7 && rc.canFireTriadShot()) {
                    rc.fireTriadShot(representativeForSlot[bestSlot]);
                    if (!rc.hasMoved()) {
                        addDesireZone(myLocation.add(representativeForSlot[bestSlot], 1.05f), 1.1f, -70); //Don't shoot urself in the face man
                        addDesireZone(myLocation.add(representativeForSlot[bestSlot].rotateRightDegrees(20), 1.05f), 1.1f, -70); //Don't shoot urself in the face man
                        addDesireZone(myLocation.add(representativeForSlot[bestSlot].rotateLeftDegrees(20), 1.05f), 1.1f, -70); //Don't shoot urself in the face man
                    }
                } else {
                    rc.fireSingleShot(representativeForSlot[bestSlot]);
                    if (!rc.hasMoved()) {
                        addDesireZone(myLocation.add(representativeForSlot[bestSlot], 2.05f), 1.1f, -70); //Don't shoot urself in the face man

                        addDesireZone(myLocation.add(representativeForSlot[bestSlot], 1.05f), 1.1f, -60); //Don't shoot urself in the face man
                    }
                }
            }
        }
    }


    public void initial(){
        maxHp = RobotType.TANK.maxHealth;
        radius =  RobotType.TANK.bodyRadius;
        sightradius = RobotType.TANK.sensorRadius;
        bulletsightradius = RobotType.TANK.bulletSightRadius;
        maxMove = RobotType.TANK.strideRadius;
        bulletSpeed = RobotType.TANK.bulletSpeed;
        attackpower = RobotType.TANK.attackPower;
    }
}
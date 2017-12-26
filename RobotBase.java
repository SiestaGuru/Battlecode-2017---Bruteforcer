package bruteforcer;

import battlecode.common.*;

import java.util.*;

/**
 * Created by Hermen on 10/1/2017.
 */
public class RobotBase {


    public static RobotController rc;
    public static Team enemy;
    public static Team ally;

    public static int turn;
    public static int turnsLeft;

    public static int turnLimit;
    public static float vicPoints;
    public static MapLocation myLocation = null;

    public static int maxHp = -1;
    public static float radius = -1;
    public static float sightradius = -1;
    public static float bulletsightradius = -1;
    public static float maxMove = -1;
    public static float bulletSpeed = -1;
    public static float attackpower = -1;


    private static int turnsNotMoved = 0;

    public static boolean player1 = false;


    private static int lastTurnSeriousThreat = -10;
    static int enemyArchonCount = 1;
    static int initialEnemyArchonCount = 1;

    static int ownArchonCount = 1;

    static int lastbuildGardener = -30;
    public static RobotInfo detectedScout = null;


    public static DesireZone[] zones = new DesireZone[50];
    public static float moveVectorX = 0;
    public static float moveVectorY = 0;
    public static int zoneLoc = 0;
    private static boolean earlyController = false;


    public static MapLocation randomGoal = null;
    private static int lastRandomChange = -500;

    public static float mapLowestX = 99999;
    public static float mapLowestY = 99999;
    public static float mapHighestX = -99999;
    public static float mapHighestY = -99999;
    public static int lastmapUpdate = -10;


    public static final int TINY = -1;

    public static final int SMALL = 0;
    public static final int MEDIUM = 1;
    public static final int LARGE = 2;
    public static final int HUGE = 3;

    public static int mapSizeType = MEDIUM;

    private static int enemyarchonid1 = -1;
    private static int enemyarchonid2 = -1;
    private static int enemyarchonid3 = -1;

    public  static boolean alreadyBroadcastLocation = false;
    public  static boolean shouldbroadcastMapSize = false;

    //Keeping treack of enemy broadcasts heard
    public static MapLocation pingHeardLast = null;
    public static MapLocation pingHeardBeforeLast = null;
    public static int turnHeardLastPing;
    public static int turnHeardSecondLastPing;

    public static int neutralTreesDetectedThisTurn = 0;

    public static int alliedTreesDetectedThisTurn = 0;
    public static int enemyTreesDetectedThisTurn = 0;
    public static int bulletsDetectedInTrees = 0;
    public static int choppableAreaDetectedThisTurn = 0;
    public static int treesInEnemyDirection = 0;


    public static int enemyForceDetectedThisTurn = 0; //Counting lumberjacks as 1, soldiers as 2, tanks as 4
    public static int friendlyLumberjacksDetectedThisTurn = 0;
    public static int enemyLumberjacksDetectedThisTurn = 0;
    public static int friendlyGardenersDetectedThisTurn = 0;
    public static int enemyScoutsDetectedThisTurn = 0;


    public static int friendlyForceDetectedThisTurn = 0; //Counting lumberjacks as 1, soldiers as 2, tanks as 4
    public static int unitCarryingTreeDetectedThisTurn = 0;
    public static int goodUnitCarryingTreeDetectedThisTurn = 0;
    public static int gardenerCarryingTreeDetectedThisTurn = 0;

    public static int thingsInRange4 = 0;
    public static int neutralsInRange6 = 0;

    public  static float lastHealth = 400;


    public static MapLocation woodcutJob1 = null;
    public static MapLocation woodcutJob2 = null;
    public static MapLocation woodcutJob3 =null;
    public static MapLocation woodcutJob4 =null;

    public static MapLocation[] likelyEnemyLocations = null;
    public static int  likelyEnemyCount = 0;

    public static MapLocation cluster1 = null;
    public static MapLocation cluster2 = null;
    public static MapLocation cluster3 = null;


    public static int prioritywoodcutJob1 = 0;
    public static int prioritywoodcutJob2 = 0;
    public static int prioritywoodcutJob3 = 0;
    public static int prioritywoodcutJob4 = 0;

    public static int directionLeaning = -1; //left = 0, right = 1


    public static int woodcut_lastchanged = -10;

    public static int radioturn = -1;

    public static MapLocation[] specialMapLocations = new MapLocation[70];
    public static float[] extraPoints = new float[70];
    public static int specialMapLocationsCount = 0;

    public static Direction[] usedMovementDirections = new Direction[54];

    public static TreeInfo[] treesDetected;
    public static RobotInfo[] robotsDetected;


    public static int lastTimeSoldiersReport = 0;
    public static int timesNoticedLostSoldier = 0;

    public static int momentumFor = -1;

    public void run(){


        try {
            //Some initialization
            ally = rc.getTeam();
            enemy= ally.opponent();
            turnLimit = rc.getRoundLimit();
            initiateDirections();
            //directionLeaning = rc.getID() % 2;
            turn = rc.getRoundNum();
            myLocation = rc.getLocation();

            //Used for some debugging
            if(ally.name().equals("A")){
                player1 = true;
            }
            else{
                player1 = false;
            }

            initial();
        }
        catch (Exception e){
            //System.out.println("Initial Exception in "  + rc.getType().name());
            e.printStackTrace();
        }


        while(true){
            //Do this every turn
            try {
                turn = rc.getRoundNum();
                turnsLeft = turnLimit - turn;
                myLocation = rc.getLocation();
                moveVectorX = 0;
                moveVectorY = 0;
                zones = new DesireZone[50];
                zoneLoc = 0;
                specialMapLocationsCount = 0;

                if(!rc.hasMoved()) { //Prevents getting over the bytecode limit several times in row
                    step();
                }

                if(Clock.getBytecodesLeft() > 200){
                    doVictoryPointLogic();
                }

                Clock.yield();
            }
            catch (Exception e){
                //System.out.println("Exception in "  + rc.getType().name());
                e.printStackTrace();
            }
        }
    }

    public RobotInfo bestDefensiveTargetBot = null;


    public void initial() throws Exception{

    }
    public void step() throws Exception{

    }



    public void dodgeBullets(BulletInfo[] bullets){
        //Analyze incoming bullets and try to dodge them
        //A couple of shapes are used to model the areas in which we are hit
        //Shapes have been chosen in an attempt to minimize both false positives (believing it will be hit when it wont) and false negatives (believing it wont be hit when it will)
        //But both still appear because we use squares/circles instead of lines
        //The focus on whether we want to minimize false positives/false negatives is based on the amount of bullets we see
        //With a small amount of bullets, we can afford more false positives so that we'll have less false negatives
        //With a large amount of bullets, we'd rather focus a bit more on false positives, so that we won't end up thinking every move results in being hit

        float safetyRange = 0.2f;
        boolean extraFar = false;
        float avoidRadius = radius + 0.03f;
        float minDist = avoidRadius + maxMove;

        if (bullets.length > 10) {
            safetyRange = 0.2f;
        } else if (bullets.length > 3) {
            extraFar = true;
            safetyRange = 0.23f;
        } else {
            extraFar = true;
            safetyRange = 0.27f;
        }


        for (int i = 0; i < bullets.length && zoneLoc < 20; i++) {
            BulletInfo bullet = bullets[i];
            MapLocation loc = bullet.location;

            MapLocation bullet1Loc = loc.add(bullet.dir, bullet.speed * 0.65f);
            float dist0 = myLocation.distanceTo(loc);
            float dist1 = myLocation.distanceTo(bullet1Loc);

            if (dist1 > dist0 + 0.1f) {
                //The bullet is flying away from us
                if (dist0 <= minDist) {
                    zones[zoneLoc] = new DesireZone(loc, avoidRadius, -230 + -10 * bullet.damage);
                    zoneLoc++;
                }
            } else {

                //The bullet might be coming towards us/sideways

                //The shapes used here have been selected to try to minimize both fals epositives/negatives
                //Shapes that would accurately handle most spots (as few false negatives as possible, especially in the first turn, and preferably not too many false positives either)

                if (bullet.speed < 3) {
                    //Scout or soldier bullets, not bothering to do these differently as theyre so similar
                    if (dist0 <= minDist) {
                        zones[zoneLoc] = new DesireZone(loc, avoidRadius, -230 + -10 * bullet.damage);
                        zoneLoc++;

                        //These two spots are common dodge spots. Moving as far awy from the bullet as possible, and moving just outside of the bullets hit radius
                        //Add them, so that we'll at least consider these  (not being able to move is already handled further on)
                        addSpecialMapLocation(myLocation.add(bullet.location.directionTo(myLocation),maxMove),5 );
                        addSpecialMapLocation(bullet.location.add(bullet.location.directionTo(myLocation), radius + 0.1f), 0);
//                       rc.setIndicatorLine(loc, loc.add(bullet.originalEnemyDir, 0.1f), 255, 0, 0);
                    }

                    if (dist1 <= minDist + safetyRange) {
                        zones[zoneLoc] = new DesireZone(bullet1Loc, avoidRadius + safetyRange, -200 + -10 * bullet.damage, true);
                        zoneLoc++;

                        if(extraFar){
                            if (bullet1Loc.distanceTo(myLocation) <= minDist) {
                                zones[zoneLoc] = new DesireZone(bullet1Loc, avoidRadius, -30, true);
                                zoneLoc++;
                            }
                        }
//                        rc.setIndicatorLine(bullet1Loc, bullet1Loc.add(bullet.originalEnemyDir, 0.1f), 0, 255, 0);
                    }

                    MapLocation bullet2Loc = loc.add(bullet.dir, bullet.speed * 1.4f);
                    float dist2 = bullet2Loc.distanceTo(myLocation);
                    if ( dist2 <= minDist + safetyRange) {
                        zones[zoneLoc] = new DesireZone(bullet2Loc, avoidRadius + safetyRange, -180 + -10 * bullet.damage, true);
                        zoneLoc++;

                        if(extraFar){
                            if (dist2 <= minDist) {
                                zones[zoneLoc] = new DesireZone(bullet2Loc, avoidRadius, -30, true);
                                zoneLoc++;
                            }
                        }
//                        rc.setIndicatorLine(bullet2Loc, bullet2Loc.add(bullet.originalEnemyDir, 0.15f), 0, 0, 255);
                    }

                    MapLocation bullet3Loc = loc.add(bullet.dir, bullet.speed * 2.2f);
                    float dist3 = myLocation.distanceTo(bullet3Loc);
                    if (dist3 <= minDist) {
                        zones[zoneLoc] = new DesireZone(bullet3Loc, avoidRadius, -100 + -10 * bullet.damage, true);
                        zoneLoc++;

                        if(extraFar){
                            if (dist3 <= minDist) {
                                zones[zoneLoc] = new DesireZone(bullet3Loc, avoidRadius -safetyRange, -20, true);
                                zoneLoc++;
                            }
                        }

//                        rc.setIndicatorLine(bullet3Loc, bullet3Loc.add(bullet.originalEnemyDir, 0.1f), 200, 200, 0);
                    }

                    if (zoneLoc < 20) {
                        MapLocation bullet4Loc = loc.add(bullet.dir, bullet.speed * 2.8f);
                        if (myLocation.distanceTo(bullet4Loc) <= minDist - 0.2f) {
                            zones[zoneLoc] = new DesireZone(bullet4Loc, avoidRadius - 0.2f, -30 + -5 * bullet.damage);
                            zoneLoc++;
//                            rc.setIndicatorLine(bullet4Loc, bullet4Loc.add(bullet.originalEnemyDir, 0.15f), 0, 200, 200);
                        }
                    }
                } else {
                    //Tank bullets
                    if (dist0 <= minDist) {
                        zones[zoneLoc] = new DesireZone(loc, avoidRadius, -200 + -10 * bullet.damage);
                        zoneLoc++;
//                        rc.setIndicatorLine(loc, loc.add(bullet.originalEnemyDir, 0.1f), 255, 0, 0);
                    }

                    if (dist1 <= minDist + 0.2f) {
                        zones[zoneLoc] = new DesireZone(bullet1Loc, avoidRadius + 0.4f, -200 + -10 * bullet.damage, true);
                        zoneLoc++;
//                        rc.setIndicatorLine(bullet1Loc, bullet1Loc.add(bullet.originalEnemyDir, 0.1f), 0, 255, 0);
                    }

                    MapLocation bullet2Loc = loc.add(bullet.dir, bullet.speed * 1.4f);
                    if (bullet2Loc.distanceTo(myLocation) <= minDist + 0.4f) {
                        zones[zoneLoc] = new DesireZone(bullet2Loc, avoidRadius + 0.4f, -180 + -10 * bullet.damage, true);
                        zoneLoc++;
//                        rc.setIndicatorLine(bullet2Loc, bullet2Loc.add(bullet.originalEnemyDir, 0.15f), 0, 0, 255);
                    }

                    MapLocation bullet3Loc = loc.add(bullet.dir, bullet.speed * 2.1f);
                    if (myLocation.distanceTo(bullet3Loc) <= minDist + 0.2f) {
                        zones[zoneLoc] = new DesireZone(bullet3Loc, avoidRadius + 0.2f, -100 + -10 * bullet.damage, true);
                        zoneLoc++;
//                        rc.setIndicatorLine(bullet3Loc, bullet3Loc.add(bullet.originalEnemyDir, 0.1f), 200, 200, 0);
                    }

                    if (zoneLoc < 20) {
                        MapLocation bullet4Loc = loc.add(bullet.dir, bullet.speed * 2.7f);
                        if (myLocation.distanceTo(bullet4Loc) <= minDist) {
                            zones[zoneLoc] = new DesireZone(bullet4Loc, avoidRadius, -30 + -5 * bullet.damage);
                            zoneLoc++;
//                            rc.setIndicatorLine(bullet4Loc, bullet4Loc.add(bullet.originalEnemyDir, 0.15f), 0, 200, 200);
                        }
                    }

                }
            }
        }
    }


    public void doMovement(int minRemainingBytes) throws Exception{
        MapLocation myLocation = this.myLocation;
        float bestScore = -9999999;
        int bestMovement = 0;

        float moveVectorStr = -1 * (new MapLocation(0,0).distanceTo(new MapLocation(moveVectorX,moveVectorY))); //Negative because we want to move towards it, not away (and we calculate distance)

        if(moveVectorStr > 1){
            moveVectorStr /= 4;
        }

        int resize = zoneLoc;
        int origZoneLoc = zoneLoc;
        //We want to have enough passes to have a decent amount of movement spots looked at
        //So limit the amount of desire zones we use

        int toSpare = Clock.getBytecodesLeft() - minRemainingBytes;
        if( toSpare < 1000){
            resize = 0;
        } else if( toSpare < 1500){
            if(zoneLoc > 3) {
                resize = 3;
            }
        }
        else if( toSpare < 2000){
            if(zoneLoc > 8) {
                resize = 8;
            }
        }
        else if(toSpare < 3000){
            if(zoneLoc > 11) {
                resize = 11;
            }
        }else if(toSpare< 4000){
            if(zoneLoc > 14) {
                resize = 14;
            }
        }else if(toSpare < 5000){
            if(zoneLoc > 19) {
                resize = 19;
            }
        }
        else if(toSpare < 6000){
            if(zoneLoc > 26) {
                resize = 26;
            }
        }else if(toSpare < 7000){
            if(zoneLoc > 30) {
                resize = 30;
            }
        }
        else if(toSpare< 8000){
            if(zoneLoc > 35) {
                resize = 35;
            }
        }else if(toSpare < 9000){
            if(zoneLoc > 42) {
                resize = 42;
            }
        }

        DesireZone[] newOrder;
        if(resize < zoneLoc && resize > 0){
            //Do a fast sort-like thing that makes sure to put the desirezones that are most important at the front
            newOrder = new DesireZone[zoneLoc];
            int count = 0;
            for(int i = zoneLoc -1; i >= 0; i-- ){
                if(zones[i].desire < -20){
                    newOrder[count] = zones[i];
                    count++;
                }
            }
            for(int i = zoneLoc -1; i >= 0; i-- ){
                if(zones[i].desire > 10){
                    newOrder[count] = zones[i];
                    count++;
                }
            }
            for(int i = zoneLoc -1; i >= 0; i-- ){
                if(zones[i].desire >= -20 && zones[i].desire < -5){
                    newOrder[count] = zones[i];
                    count++;
                }
            }
            for(int i = zoneLoc -1; i >= 0; i-- ){
                if(zones[i].desire >= -5 && zones[i].desire <= 10){
                    newOrder[count] = zones[i];
                    count++;
                }
            }
            zoneLoc = resize;
        }
        else{
            newOrder = zones;
        }


        //We have a movement direction, now put it at the constant distance of 10 and apply a strength relative to its size
        //This to be sure our point isn't very close to us (as we don't want it inside our movement circle)
        //Nor very far as it can cause flatness issues (if it's very far, the differences start mattering less, even though they should matter more)
        float finalMoveVectorX;
        float finalMoveVectorY;

        int multX = 1;
        int multY = 1;

        if(moveVectorX < 0){
            moveVectorX *= -1;
            multX = -1;
        }
        if(moveVectorY < 0){
            moveVectorY *= -1;
            multY = -1;
        }
        if(moveVectorStr > -0.05f){
            finalMoveVectorX = myLocation.x;
            finalMoveVectorY = myLocation.y;
            moveVectorStr = 0;
        }
        else{
            float multFactor = 10 / (moveVectorX + moveVectorY);
            finalMoveVectorX = (multFactor * moveVectorX * multX) + myLocation.x;
            finalMoveVectorY = (multFactor * moveVectorY * multY) + myLocation.y;
        }
        MapLocation finalMove = new MapLocation(finalMoveVectorX,finalMoveVectorY);


        int specialCount = specialMapLocationsCount;

        //If have some, put special calculated positions in first (like lining up a shot through trees)
        MapLocation[] movementSlots =new MapLocation[81 + specialCount];
        for(int i = 0; i< specialCount; i++) {
            movementSlots[i] = specialMapLocations[i];
        }


        //And we usually want to at least consider standing still
        movementSlots[specialCount] = myLocation;

        //The best move is usually right towards the final move
        movementSlots[specialCount+1] = myLocation.add(myLocation.directionTo(finalMove),maxMove);


//      rc.setIndicatorLine(myLocation,finalMove, rc.getID() % 255, (rc.getID() + 100) % 255, (rc.getID() + 175) % 255);

        int currentMax = 2 + specialCount;
        int slot = 0;
        int zoneMinus1 = zoneLoc - 1;

        //We want to cut off this procedure a little bit before we hit the limit
        //Doing the minus calculation here to save a bit of bytecode
        int minRemainingPlus200 = minRemainingBytes + 200;
        int minRemainingPlus600 = minRemainingBytes + 600;

        outerloop:
        for(int pass = 0; pass < 8; pass++) {

            if(Clock.getBytecodesLeft() < minRemainingPlus600){
//                System.out.println("pass : " + pass + " slot: " + slot   +  "  final choice: " + bestMovement  +  " vector: " +  finalMove  +  "  zones used:  " + zoneLoc +  " zones total:" + origZoneLoc +  "bestscore: " + bestScore);
//                System.out.println("slot: " + slot + " zones: " + zoneLoc + "/" + origZoneLoc);
//                printed = true;
                break;
            }
            currentMax += 8;


            int angleIndex = 0;
            float distance = maxMove;

            //Every pass adds 8 points, nicely separated around the potential movement circle
            //First pass is just max distance with N, NE, E, SE, S, SW, W, NW angles
            //The others are variations, either less than max distance, or different angles
            switch(pass){
                case 1:
                    angleIndex = 8;
                    break;
                case 2:
                    angleIndex = 24;
                    distance *= 0.5f;
                    break;
                case 3:
                    angleIndex = 16;
                    distance *= 0.66f;
                    break;
                case 4:
                    angleIndex = 40;
                    distance *= 0.33f;
                    break;
                case 5:
                    angleIndex = 24;
                    break;
                case 6:
                    angleIndex = 32;
                    break;
                case 7:
                    angleIndex = 8;
                    distance *= 0.1f;
                    break;

            }

            for(int i = currentMax;i>slot;i--){
                movementSlots[i] = myLocation.add(usedMovementDirections[angleIndex],distance);
                angleIndex++;
            }

            //This loop calculates the valuations of all the potential move spots.
            //It's the core of the movement system, and tends to take up most of the bytecode budget
            for (;slot < currentMax; slot++ ) {
                MapLocation loc = movementSlots[slot];
                if (rc.canMove(loc)) {
                    //Start with valuing the spots based on the distance to the total move vector point
                    float curScore = loc.distanceTo(finalMove) * moveVectorStr;

                    //Give some extra valuation boost to the 'special' locations that have been calculated elsewhere (example: spot that lines up a shot through trees)
                    if(slot < specialCount){
                        curScore += extraPoints[slot];
                    }

                    //Change the valuation if this spot is inside of any of the desirezones
                    for (int i = zoneMinus1; i >= 0; i--) {
                        DesireZone zone = newOrder[i];
                        if (loc.x > zone.left && loc.x < zone.right && loc.y > zone.top && loc.y < zone.bot) {
                            if(zone.circular){
                                if(loc.distanceTo(zone.center) < zone.radius){
                                    curScore += zone.desire;
                                }
                            }else {
                                curScore += zone.desire;
                            }
                        }
                    }

                    if (curScore > bestScore) {
                        bestScore = curScore;
                        bestMovement = slot;
                    }
                }

                //Gotta exit if we're close to hitting the bytecode limit
                if(Clock.getBytecodesLeft() < minRemainingPlus200){
//                    System.out.println("pass : " + pass + " slot: " + slot   +  "  final choice: " + bestMovement  +  " vector: " +  finalMove  +  "  zones used:  " + zoneLoc +  " zones total:" + origZoneLoc +  "bestscore: " + bestScore);
//                    System.out.println("slot: " + slot + " zones: " + zoneLoc + "/" + origZoneLoc);

//                    printed = true;
                    break outerloop;
                }
            }
        }
        //if(bestScore == -9999999) {
//            if(!printed){
//                System.out.println("All slots! Zones: " + zoneLoc + "/" + origZoneLoc);
//                System.out.println(" did all passes!  final choice: " + bestMovement + " vector: " + finalMove + "  zones used:  " + zoneLoc + " zones total:" + origZoneLoc + "bestscore: " + bestScore);
//             }
        //}

        rc.move(movementSlots[bestMovement]);
    }



    public void initiateDirections(){
        float startAngle = 0;

        for(int i = 0; i < 8; i++){
            usedMovementDirections[i] = Direction.NORTH.rotateRightDegrees(startAngle + i * 45);
        }
        startAngle = 22.5f;
        for(int i = 8; i < 16; i++){
            usedMovementDirections[i] = Direction.NORTH.rotateRightDegrees(startAngle + i * 45);
        }
        startAngle = 60f;
        for(int i = 16; i < 24; i++){
            usedMovementDirections[i] = Direction.NORTH.rotateRightDegrees(startAngle + i * 45);
        }
        startAngle = 30;
        for(int i = 24; i < 32; i++){
            usedMovementDirections[i] = Direction.NORTH.rotateRightDegrees(startAngle + i * 45);
        }
        startAngle = 0;
        for(int i = 32; i < 40; i++){
            usedMovementDirections[i] = Direction.NORTH.rotateRightDegrees(startAngle + i * 45);
        }
        startAngle = 0.1f;
        for(int i = 40; i < 48; i++){
            usedMovementDirections[i] = Direction.NORTH.rotateRightDegrees(startAngle + i * 45);
        }
    }


    public void becomeCommanderIfNeccessary() throws Exception{
        int aggr_lastupdate = rc.readBroadcast(AGRESSIVE_TARGETS_LAST_UPDATE);
        if (turn - aggr_lastupdate > 6 && rc.getRobotCount() > 2) {
            //System.out.print(rc.getType().name() + "  JUST BECAME COMMANDER");
            doCommanderTasks();
//            rc.setIndicatorDot(myLocation.add(Direction.NORTH, 1.5f), 255,0,255 );
        }
    }

    public void doCommanderTasks() throws Exception {
        RobotController rc = RobotBase.rc;

        alreadyBroadcastLocation = false;
        shouldbroadcastMapSize = false;
        enemyForceDetectedThisTurn = 0;
        friendlyLumberjacksDetectedThisTurn = 0;
        neutralTreesDetectedThisTurn = 0;
        choppableAreaDetectedThisTurn = 0;
        bulletsDetectedInTrees = 0;
        enemyTreesDetectedThisTurn = 0;
        alliedTreesDetectedThisTurn = 0;
        enemyLumberjacksDetectedThisTurn = 0;
        friendlyGardenersDetectedThisTurn = 0;
        friendlyForceDetectedThisTurn = 0;
        bestDefensiveTargetBot = null;
        enemyScoutsDetectedThisTurn = 0;
        unitCarryingTreeDetectedThisTurn = 0;
        goodUnitCarryingTreeDetectedThisTurn = 0;
        gardenerCarryingTreeDetectedThisTurn = 0;
        treesInEnemyDirection = 0;
        thingsInRange4 = 0;
        neutralsInRange6 = 0;
        woodcutJob1 = null;
        woodcutJob2 = null;
        woodcutJob3 = null;
        woodcutJob4 = null;
        detectedScout = null;


        if(this instanceof  Archon) {
            //Too expensive for other commanders most of the time
            processPings();
        }

        treesDetected = rc.senseNearbyTrees();

        int max = 100;
        if(Clock.getBytecodesLeft() < 14000){
            max = 20;
        }

        for (int i = 0; i < treesDetected.length; i++) {
            TreeInfo tree = treesDetected[i];
            float distance = tree.getLocation().distanceTo(myLocation);
            if (distance - tree.radius < 6f) {
                if (distance - tree.radius < 4) {
                    thingsInRange4++;
                }
                if (!tree.getTeam().equals(ally) && !tree.getTeam().equals(enemy)) {
                    neutralsInRange6++;
                }
            } else {
                break;//Sensetrees orders them based on distance
            }
        }

        float bestScore = -999;
        TreeInfo bestTreeCarrier = null;

        MapLocation mainTarget = null;
        boolean shouldCountDirectional = this instanceof Archon;
        float myDistance = -1;
        if(shouldCountDirectional) {
            mainTarget = new MapLocation(rc.readBroadcast(MAIN_AGRESSIVE_TARGET_X), rc.readBroadcast(MAIN_AGRESSIVE_TARGET_Y));

            if (mainTarget.x <= 0) {
                shouldCountDirectional = false;
            } else {
                myDistance = myLocation.distanceTo(mainTarget);
            }
        }

        for (int i = 0; i < treesDetected.length && i < max ; i++) {
            TreeInfo tree = treesDetected[i];
            MapLocation loc = tree.location;
            if (tree.team.equals(ally)) {
                alliedTreesDetectedThisTurn++;
            } else if (tree.team.equals(enemy)) {
                enemyTreesDetectedThisTurn++;
//                woodcut_x = (int)tree.getLocation().x;
//                woodcut_y = (int)tree.getLocation().y;

                if (woodcutJob1 == null) {
                    woodcutJob1 = loc;
                    prioritywoodcutJob1 = 10 + (int)tree.radius;
                } else if (woodcutJob2 == null) {
                    woodcutJob2 = loc;
                    prioritywoodcutJob2 =  10 + (int)tree.radius;
                } else if (woodcutJob3 == null) {
                    woodcutJob3 = loc;
                    prioritywoodcutJob3 = 10 + (int)tree.radius;
                } else if (woodcutJob4 == null) {
                    woodcutJob4 = loc;
                    prioritywoodcutJob4 =  10 + (int)tree.radius;
                }

                if(myLocation.distanceTo(loc) - tree.radius < 7.5) {
                    choppableAreaDetectedThisTurn += (tree.radius * tree.radius);
                }

            } else {

                if(shouldCountDirectional){
                    if(loc.distanceTo(mainTarget) < myDistance){
                        treesInEnemyDirection++;
                    }

                }

                if (turn - woodcut_lastchanged > 8) {
//                    woodcut_x = (int)tree.getLocation().x;
//                    woodcut_y = (int)tree.getLocation().y;
                    woodcut_lastchanged = turn;
                }
                neutralTreesDetectedThisTurn++;
                if(myLocation.distanceTo(loc) - tree.radius < 7.5) {

                    choppableAreaDetectedThisTurn += (tree.radius * tree.radius);
                }

                if (tree.getContainedRobot() != null) {
                    unitCarryingTreeDetectedThisTurn++;

                    float score = 0;
                    if (tree.containedRobot.equals(RobotType.TANK)) {
                        score = 3;
                        goodUnitCarryingTreeDetectedThisTurn++;
                    }
                    else if (tree.containedRobot.equals(RobotType.LUMBERJACK)) {
                        score = 2;
                        goodUnitCarryingTreeDetectedThisTurn++;
                    }
                    else if (tree.containedRobot.equals(RobotType.SOLDIER)) {
                        score = 1;
                        goodUnitCarryingTreeDetectedThisTurn++;
                    }
                    else if (tree.containedRobot.equals(RobotType.ARCHON)) {
                        score = -1;
                    } else if(tree.containedRobot.equals(RobotType.GARDENER)){
                        gardenerCarryingTreeDetectedThisTurn++;

                    }
                    if (score > bestScore) {
                        bestScore = score;
                        bestTreeCarrier = tree;
                    }

                } else {

                    int business = (int)(((thingsInRange4 / 2) + neutralsInRange6 + choppableAreaDetectedThisTurn)/2);
                    if(business > 8){
                        business = 8;
                    }
                    if (woodcutJob1 == null) {
                        woodcutJob1 = loc;
                        prioritywoodcutJob1 = 7 +  business +(int)tree.radius;
                    } else if (woodcutJob2 == null) {
                        woodcutJob2 = loc;
                        prioritywoodcutJob2 = 7 + business + (int)tree.radius;
                    } else if (woodcutJob3 == null) {
                        woodcutJob3 = loc;
                        prioritywoodcutJob3 = 7 + business + (int)tree.radius;
                    } else if (woodcutJob4 == null) {
                        woodcutJob4 = loc;
                        prioritywoodcutJob4 = 7 + business + (int)tree.radius;
                    }
                }

                    bulletsDetectedInTrees+=tree.containedBullets;

            }
        }

        if (bestTreeCarrier != null) {
          //  rc.setIndicatorDot(bestTreeCarrier.getLocation(), 255, 255, 0);
            woodcutJob1 = bestTreeCarrier.getLocation();
            woodcutJob2 = bestTreeCarrier.getLocation();
            prioritywoodcutJob1 = 19 + (int) bestTreeCarrier.radius;
            prioritywoodcutJob2 = 11 + (int) bestTreeCarrier.radius;

            if(5 + (int)bestTreeCarrier.radius > prioritywoodcutJob3) {
                woodcutJob3 = bestTreeCarrier.getLocation();
                prioritywoodcutJob3 = 5 + (int) bestTreeCarrier.radius;
            }

            if(5 + (int)bestTreeCarrier.radius > prioritywoodcutJob4) {
                woodcutJob4 = bestTreeCarrier.getLocation();
                prioritywoodcutJob4 = 4 + (int) bestTreeCarrier.radius;

            }
        }





        // MapLocation aggrTarget = new MapLocation(rc.readBroadcast(MAIN_AGRESSIVE_TARGET_X), rc.readBroadcast(MAIN_AGRESSIVE_TARGET_Y));

        robotsDetected = rc.senseNearbyRobots();

        float bestGardenerDistance = 9999999;
        int bestGardenerId = -1;

        for (int i = 0; i < robotsDetected.length && i < max; i++) {

            RobotInfo robot = robotsDetected[i];
            float distance = robot.getLocation().distanceTo(myLocation);

            if (distance - robot.getRadius() < 4f) {
                thingsInRange4++;
            }

            if (robot.team.equals(enemy)) {
                if (bestDefensiveTargetBot == null && !robot.type.equals(RobotType.SCOUT)) bestDefensiveTargetBot = robot;

                if (robot.type.equals(RobotType.SOLDIER)) {
                    enemyForceDetectedThisTurn += 2;
                } else if (robot.type.equals(RobotType.LUMBERJACK)) {
                    enemyForceDetectedThisTurn += 1;
                    enemyLumberjacksDetectedThisTurn += 1;

                    if (!bestDefensiveTargetBot.type.equals(RobotType.TANK)) {
                        bestDefensiveTargetBot = robot;
                    }
                } else if (robot.type.equals(RobotType.TANK)) {
                    enemyForceDetectedThisTurn += 4;
                    bestDefensiveTargetBot = robot;
                } else if (robot.type.equals(RobotType.SCOUT)) {
                    enemyScoutsDetectedThisTurn++;
                    detectedScout = robot;
                }

            } else {
                if (robot.type.equals(RobotType.SOLDIER)) {
                    friendlyForceDetectedThisTurn += 2;
                } else if (robot.type.equals(RobotType.LUMBERJACK)) {
                    friendlyForceDetectedThisTurn += 1;
                    friendlyLumberjacksDetectedThisTurn += 1;
                } else if (robot.type.equals(RobotType.TANK)) {
                    friendlyForceDetectedThisTurn += 4;
                } else if (robot.type.equals(RobotType.GARDENER)) {
                    friendlyGardenersDetectedThisTurn++;

                    if (distance < bestGardenerDistance) {
                        bestGardenerDistance = distance;
                        bestGardenerId = robot.ID;
                    }
                }
            }
        }

        if (enemyForceDetectedThisTurn - enemyScoutsDetectedThisTurn > 2) {
            lastTurnSeriousThreat = turn;
        }

        int soldiers = rc.readBroadcast(SOLDIERS_REPORTING_IN);
        if(soldiers < lastTimeSoldiersReport){
            timesNoticedLostSoldier++;
//            System.out.println("Soldier deaths " + timesNoticedLostSoldier);
        }
        lastTimeSoldiersReport = soldiers;


        //Update strategic aggressive/defensive targets every couple of turns
        //Might have to optimize this later, either splitting it up between turns to allow more indepth calculations within
        //the limit. Or doing it on the same turn to have to broadcast less



        int aggr_lastupdate = rc.readBroadcast(AGRESSIVE_TARGETS_LAST_UPDATE);
        int def_lastupdate = rc.readBroadcast(DEFENSIVE_TARGETS_LAST_UPDATE);
        int econ_lastupdate = rc.readBroadcast(ECON_GOALS_LAST_UPDATE);
        int strat_lastupdate = rc.readBroadcast(LAST_UPDATED_GRAND_STRAT);


        int turnsAgo = 2;

        if (this instanceof Archon) {
            boolean arch1Alive = false;
            boolean arch2Alive = false;
            boolean arch3Alive = false;
            int total = 0;
            if (((Archon) this).our_archon_nr == 1 || turn - rc.readBroadcast(OUR_ARCHON_1_LASTSEEN) <= 3) {
                arch1Alive = true;
                total++;
            }
            if (((Archon) this).our_archon_nr == 2 || turn - rc.readBroadcast(OUR_ARCHON_2_LASTSEEN) <= 3) {
                arch2Alive = true;
                total++;
            }
            if (((Archon) this).our_archon_nr == 3 || turn - rc.readBroadcast(OUR_ARCHON_3_LASTSEEN) <= 3) {
                arch3Alive = true;
                total++;
            }

            if (((Archon) this).our_archon_nr == 1 && (arch2Alive || arch3Alive)) {
                if (Math.random() > 0.5f) {
                    turnsAgo++; //Allow another archon to take it this turn, mostly for gardener build purposes
                }
                if (thingsInRange4 > 6) {
                    turnsAgo++;
                }
            } else if (((Archon) this).our_archon_nr == 2 && arch3Alive) {
                if (Math.random() > 0.5f) {
                    turnsAgo++; //Allow another archon to take it this turn, mostly for gardener build purposes
                }
                if (thingsInRange4 > 6) {
                    turnsAgo++;
                }
            }

            if(turn > 5) {
                if (total > 1 && ((Archon) this).our_archon_nr == 1 && lastbuildGardener <= 0){
                    //This archon is probably stuck, use another archon so we dont give weird data (like on defenseless)
                    turnsAgo++;
                }
                if (total > 2 && ((Archon) this).our_archon_nr == 2 && lastbuildGardener <= 0){
                    //This archon is probably stuck, use another archon so we dont give weird data (like on defenseless)
                    turnsAgo++;
                }
            }
            if (total > 1) {
                if (enemyForceDetectedThisTurn > 0) { //If we're udner attack here, and there's other arhons. Let one of the others have a go at it, so we don't build gardeners where were already udner attack etc
                    turnsAgo += 2;
                }
            }

            // System.out.println(" turnsago: " + turnsAgo  + " by: " + ((Archon)this).our_archon_nr);
        }

        if ( Clock.getBytecodesLeft() > 12000 &&(  def_lastupdate == 0 || turn - def_lastupdate >= 2 || aggr_lastupdate == 0 || turn - aggr_lastupdate >= 2 || turn < 3)) {
            calculateEnemyClusters();
            earlyController = true;
        }

        //Not allowing this one to be distributed, because it makes the trget around too much
        if (Clock.getBytecodesLeft() > 12000 &&(def_lastupdate == 0 || turn - def_lastupdate >= 2 ||  turn < 3)) {
            calculateDefensiveTargets();
        }

        if (Clock.getBytecodesLeft() > 9000 &&((econ_lastupdate == 0 || turn - econ_lastupdate >= turnsAgo ||  turn < 3))) {

            calculateEconGoals();
//                if(this instanceof  Archon) {
//                    System.out.println(" econing  by: " + ((Archon) this).our_archon_nr);
//                }

            rc.broadcast(UNIT_PRODUCER_GARDENER, bestGardenerId);
            rc.broadcast(UNIT_PRODUCER_GARDENER_DISTANCE, (int) bestGardenerDistance);
        }
        if (Clock.getBytecodesLeft() > 12000 &&(strat_lastupdate == 0 || turn - strat_lastupdate >= turnsAgo || turn < 3)) {
            calculateStrategicGoals();
        }
        if(Clock.getBytecodesLeft() > 12000) {
            updateWoodCuttingTargets(); //Let all the archons do this
        }

        //Not allowing this one to be distributed, because it makes the target around too much (unless of course our main archon is dead)
        if (Clock.getBytecodesLeft() > 12000 && (aggr_lastupdate == 0 || turn - aggr_lastupdate >= 2 ||  turn < 3)) {
            if(!(this instanceof  Archon) && Clock.getBytecodesLeft() > 13000) {
                processPings();
            }
            calculateAttackTargets();
        }

        if (shouldbroadcastMapSize) {
            broadcastMapSize();
        }

        lastHealth = rc.getHealth();


    }



    public void calculateAttackTargets() throws GameActionException{
        //Use sense broadcasting and latest updates etc to figure out a main and a secondary target
        //Include last-seen variables too


        //don't look at code neatness here please..

        int idArchon1 = rc.readBroadcast(THEIR_ARCHON_1_ID);
        int idArchon2 = rc.readBroadcast(THEIR_ARCHON_2_ID);
        int idArchon3 = rc.readBroadcast(THEIR_ARCHON_3_ID);
        int lastSeen1 = rc.readBroadcast(THEIR_ARCHON_1_LASTSEEN);
        int lastSeen2 = rc.readBroadcast(THEIR_ARCHON_2_LASTSEEN);
        int lastSeen3 = rc.readBroadcast(THEIR_ARCHON_3_LASTSEEN);

        int lastBestX = rc.readBroadcast(MAIN_AGRESSIVE_TARGET_X);
        int lastBestY = rc.readBroadcast(MAIN_AGRESSIVE_TARGET_Y);
        int last2ndBestX = rc.readBroadcast(SECONDARY_AGRESSIVE_TARGET_X);
        int last2ndBestY = rc.readBroadcast(SECONDARY_AGRESSIVE_TARGET_Y);


        int turnConfirmedTarget = rc.readBroadcast(CONFIRMED_ENEMY_TURN);
        int turnConfirmedTargetTree = rc.readBroadcast(CONFIRMED_ENEMY_TREE_TURN);
        int turnConfirmedGardener = rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_TURN);



        /*
            Possible targets:
            archon1 = 1
            archon2 = 2
            archon3 = 3
            lastping = 4
            secondlastping = 5
            confirmedtarget = 6
            confirmedtree = 7

         */
        int bestTarget = -1;
        int secondBestTarget = -1;
        float bestTargetScore = -99999;
        float secondBestTargetScore = -99999;


        if(idArchon1 > -2){ //Does this one even exist
            bestTarget = 1;//Don't need temp variable here cause always the first
            bestTargetScore = 10;
            if(idArchon1 > -1){
                bestTargetScore += 10; //Archon has actually been spotted, we may be close already
                //Maybe check for last known archon health
            }

            if(turn < 600 && turn - rc.readBroadcast(THEIR_ARCHON_1_LASTTAGGED) < 8){
                bestTargetScore -= 60; //We were just here with our soldiers, let's not hang around too much if we have other good goals

            }
            if(turn < 180){
                //Probably still around here
                bestTargetScore -=  (turn - lastSeen1) * 1.5;
            }
            else{
                bestTargetScore -=  (turn - lastSeen1) * 7.5;
            }

            if(momentumFor == 1){
                bestTargetScore += 20;
            }

//            System.out.println("archon score: " + bestTargetScore);

        }

        if(idArchon2 > -2){
            float curScore = 11;

            if(idArchon2 > -1){
                curScore += 10; //Archon has actually been spotted, we may be close already
                //Maybe check for last known archon health
            }
            if(turn < 180){
                //Probably still around here
                curScore -=  (turn - lastSeen2) * 1.5;
            }
            else{
                curScore -=  (turn - lastSeen2) * 8;
            }
            if(turn < 600 && turn - rc.readBroadcast(THEIR_ARCHON_2_LASTTAGGED) < 8){
                curScore -= 60; //We were just here with our soldiers, let's not hang around too much if we have other good goals
            }

            if(momentumFor == 2){
                curScore += 20;
            }

            if(curScore > bestTargetScore){
                secondBestTarget = bestTarget;
                secondBestTargetScore = bestTargetScore;
                bestTarget = 2;
                bestTargetScore = curScore;
            }



        }

        if(idArchon3 > -2){
            float curScore = 10;

            if(idArchon3 > -1){
                curScore += 10; //Archon has actually been spotted, we may be close already
                //Maybe check for last known archon health
            }
            if(turn < 180){
                //Probably still around here
                curScore -=  (turn - lastSeen3) * 1.5;
            }
            else{
                curScore -=  (turn - lastSeen3) * 8;
            }

            if(turn < 600 && turn - rc.readBroadcast(THEIR_ARCHON_3_LASTTAGGED) < 8){
                curScore -= 60; //We were just here with our soldiers, let's not hang around too much if we have other good goals
            }

            if(momentumFor == 3){
                curScore += 20;
            }

            if(curScore > bestTargetScore){
                secondBestTarget = bestTarget;
                secondBestTargetScore = bestTargetScore;
                bestTarget = 3;
                bestTargetScore = curScore;
            }
            else if(curScore > secondBestTargetScore){
                secondBestTargetScore = curScore;
                secondBestTarget = 3;
            }
        }


        if(turnConfirmedTargetTree != 0){
            float curScore = -80; //Trees are not very desirable targets, but expire slow

            curScore -= (turn - turnConfirmedTargetTree) * 5;

            if(momentumFor == 7){
                curScore += 20;
            }


            if(curScore > bestTargetScore){
                secondBestTarget = bestTarget;
                secondBestTargetScore = bestTargetScore;
                bestTarget = 7;
                bestTargetScore = curScore;
            }
            else if(curScore > secondBestTargetScore){
                secondBestTargetScore = curScore;
                secondBestTarget = 7;
            }

        }

        if(turnConfirmedTarget != 0){
            float curScore = -25;

            MapLocation confirmed = new MapLocation(rc.readBroadcast(CONFIRMED_ENEMY_X),rc.readBroadcast(CONFIRMED_ENEMY_Y));
            MapLocation home = new MapLocation(rc.readBroadcast(HOME_X), rc.readBroadcast(HOME_Y));

            if(confirmed.distanceTo(home) < 15){
                curScore -= 30; //Should let this be handled by defensive targets if possible
            }

            curScore -= (turn - turnConfirmedTarget) * 15; //This stuff expires fast

            if(momentumFor == 6){
                curScore += 20;
            }

            if(curScore > bestTargetScore){
                secondBestTarget = bestTarget;
                secondBestTargetScore = bestTargetScore;
                bestTarget = 6;
                bestTargetScore = curScore;
            }
            else if(curScore > secondBestTargetScore){
                secondBestTargetScore = curScore;
                secondBestTarget = 6;
            }

//            System.out.println("confirmed score: " + curScore);

        }



        if(pingHeardLast != null){
            float curScore = -30; //Don't like these as much as confirmed targets.

            curScore -=  (turn - turnHeardLastPing) * 18;
            if(momentumFor == 4){
                curScore += 20;
            }
            if(curScore > bestTargetScore){
                secondBestTarget = bestTarget;
                secondBestTargetScore = bestTargetScore;
                bestTarget = 4;
                bestTargetScore = curScore;
            }
            else if(curScore > secondBestTargetScore){
                secondBestTargetScore = curScore;
                secondBestTarget = 4;
            }

//            System.out.println("ping1 score: " + curScore);

        }

        if(pingHeardBeforeLast != null){
            float curScore = -31; //Don't like these as much as confirmed targets.

            curScore -=  (turn - turnHeardSecondLastPing) * 18;

            if(momentumFor == 5){
                curScore += 20;
            }
            if(curScore > bestTargetScore){
                secondBestTarget = bestTarget;
                secondBestTargetScore = bestTargetScore;
                bestTarget = 5;
                bestTargetScore = curScore;
            }
            else if(curScore > secondBestTargetScore){
                secondBestTargetScore = curScore;
                secondBestTarget = 5;
            }

//            System.out.println("ping2 score: " + curScore);

        }

        if(cluster1 != null){
            float curScore = -15;

            curScore += rc.readBroadcast(CLUSTER1_SIZE) * 2;

            if(momentumFor == 8){
                curScore += 20;
            }

            if(curScore > bestTargetScore){
                secondBestTarget = bestTarget;
                secondBestTargetScore = bestTargetScore;
                bestTarget = 8;
                bestTargetScore = curScore;
            }
            else if(curScore > secondBestTargetScore){
                secondBestTargetScore = curScore;
                secondBestTarget = 8;
            }

//            System.out.println("cluster1 score: " + curScore);

        }

        if(cluster2 != null){
            float curScore = -20;

            curScore += rc.readBroadcast(CLUSTER2_SIZE) * 2;

            if(momentumFor == 9){
                curScore += 20;
            }

            if(curScore > bestTargetScore){
                secondBestTarget = bestTarget;
                secondBestTargetScore = bestTargetScore;
                bestTarget = 9;
                bestTargetScore = curScore;
            }
            else if(curScore > secondBestTargetScore){
                secondBestTargetScore = curScore;
                secondBestTarget = 9;
            }
        }

        if(turnConfirmedGardener != 0){



            float curScore = 10; //We really like gardeners as a main target!



            if(turn < 800){
                curScore += 40;
                if(turn < 300){
                    curScore += 60; //Especially early
                }
            }
            curScore -= (turn - turnConfirmedGardener) * 15; //But they expire, as do all things in time...
//            System.out.println("gardener score: " + curScore);
            if(momentumFor == 10){
                curScore += 20;
            }

            if(curScore > bestTargetScore){
                secondBestTarget = bestTarget;
                secondBestTargetScore = bestTargetScore;
                bestTarget = 10;
                bestTargetScore = curScore;
            }
            else if(curScore > secondBestTargetScore){
                secondBestTargetScore = curScore;
                secondBestTarget = 10;
            }

        }

//        System.out.println("best score: " + bestTargetScore);
//        System.out.println("2ndbest score: " + secondBestTargetScore);

        if(bestTargetScore < -200){
            bestTarget = -1;
        }
        if(secondBestTargetScore < -200){
            secondBestTarget = -1;
        }


        int bestX = -1;
        int bestY = -1;
        int secondbestX = -1;
        int secondbestY = -1;

        if(bestTarget == 1){
            bestX = (int)(((float)rc.readBroadcast(THEIR_ARCHON_1_X))/ 10f);
            bestY = (int)(((float)rc.readBroadcast(THEIR_ARCHON_1_Y))/10f);

//            rc.setIndicatorDot(myLocation, 255,0,0);
            momentumFor = 1;

           // if(bestX != lastBestX || bestY != lastBestY) {
                //System.out.println("Main target: archon1 ("+ bestX + "," + bestY +") ");
            //}

        }
        else if(bestTarget == 2){
            bestX = (int)(((float)rc.readBroadcast(THEIR_ARCHON_2_X))/ 10f);
            bestY = (int)(((float)rc.readBroadcast(THEIR_ARCHON_2_Y))/10f);
//            rc.setIndicatorDot(myLocation, 170,0,0);
            momentumFor = 2;

//            if(bestX != lastBestX || bestY != lastBestY) {
//                System.out.println("Main target: archon2 ("+ bestX + "," + bestY +")    set by" + our_archon_nr);
//            }

        }
        else if(bestTarget == 3){
            bestX = (int)(((float)rc.readBroadcast(THEIR_ARCHON_3_X))/ 10f);
            bestY = (int)(((float)rc.readBroadcast(THEIR_ARCHON_3_Y))/10f);
//            rc.setIndicatorDot(myLocation, 100,0,0);
            momentumFor = 3;

//            if(bestX != lastBestX || bestY != lastBestY) {
//                System.out.println("Main target: archon3 ("+ bestX + "," + bestY +")    set by" + our_archon_nr);
//            }

        }
        else if(bestTarget == 4){
            bestX = (int)pingHeardLast.x;
            bestY = (int)pingHeardLast.y;
//            rc.setIndicatorDot(myLocation, 0,255,0);
            momentumFor = 4;

          //  if(bestX != lastBestX || bestY != lastBestY) {
           //     if(turn % 10 == 0)
                    //System.out.println("Main target: ping1 ("+ bestX + "," + bestY +")");

            //}

        }
        else if(bestTarget == 5){
            bestX = (int)pingHeardBeforeLast.x;
            bestY = (int)pingHeardBeforeLast.y;
//            rc.setIndicatorDot(myLocation, 0,180,0);
            momentumFor = 5;
//
//            if(bestX != lastBestX || bestY != lastBestY) {
//                System.out.println("Main target: ping2 ("+ bestX + "," + bestY +")    set by" + our_archon_nr);
//            }

        }else if (bestTarget == 6){
            bestX = rc.readBroadcast(CONFIRMED_ENEMY_X);
            bestY = rc.readBroadcast(CONFIRMED_ENEMY_Y);
            //System.out.println("Main target: confirmed target ("+ bestX + "," + bestY +")");
//            rc.setIndicatorDot(myLocation, 150,150,150);
            momentumFor = 6;

        }
        else if(bestTarget == 7){
            bestX = rc.readBroadcast(CONFIRMED_ENEMY_TREE_X);
            bestY = rc.readBroadcast(CONFIRMED_ENEMY_TREE_Y);
            //System.out.println("Main target: confirmed tree ("+ bestX + "," + bestY +")");
//            rc.setIndicatorDot(myLocation, 0,0,0);
            momentumFor = 7;

        }
        else if(bestTarget == 8){

            bestX = (int)cluster1.x;
            bestY = (int)cluster1.y;

//            rc.setIndicatorDot(myLocation, 0,0,255);
            momentumFor = 8;

        } else if(bestTarget == 9){

            bestX = (int)cluster2.x;
            bestY = (int)cluster2.y;

//            rc.setIndicatorDot(myLocation, 0,0,200);
            momentumFor = 9;

        }
        else if(bestTarget == 10){
            bestX = rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_X) / 100;
            bestY = rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_Y) / 100;

            momentumFor = 10;

//            rc.setIndicatorDot(myLocation, 0,200,200);

        }
        else{
//            rc.setIndicatorDot(myLocation, 100,0,100);
            momentumFor = -1;
        }

       // else{
              //System.out.println("No good targets");

        //}





        if(secondBestTarget == 1){
            secondbestX = (int)(((float)rc.readBroadcast(THEIR_ARCHON_1_X))/ 10f);
            secondbestY = (int)(((float)rc.readBroadcast(THEIR_ARCHON_1_Y))/10f);

//            if(secondbestX != last2ndBestX || secondbestY != last2ndBestY) {
//                System.out.println("Secondary target: archon1 ("+ bestX + "," + bestY +")    set by" + our_archon_nr);
//            }

        }
        else if(secondBestTarget == 2){
            secondbestX = (int)(((float)rc.readBroadcast(THEIR_ARCHON_2_X))/ 10f);
            secondbestY = (int)(((float)rc.readBroadcast(THEIR_ARCHON_2_Y))/10f);

//            if(secondbestX != last2ndBestX || secondbestY != last2ndBestY) {
//                System.out.println("Secondary target: archon2  ("+ bestX + "," + bestY +")   set by" + our_archon_nr);
//            }

        }
        else if(secondBestTarget == 3){
            secondbestX = (int)(((float)rc.readBroadcast(THEIR_ARCHON_3_X))/ 10f);
            secondbestY = (int)(((float)rc.readBroadcast(THEIR_ARCHON_3_Y))/10f);

//            if(secondbestX != last2ndBestX || secondbestY != last2ndBestY) {
//                System.out.println("Secondary target: archon3 ("+ bestX + "," + bestY +")    set by" + our_archon_nr);
//            }

        }
        else if(secondBestTarget == 4){
            secondbestX = (int)pingHeardLast.x;
            secondbestY = (int)pingHeardLast.y;

//            if(secondbestX != last2ndBestX || secondbestY != last2ndBestY) {
//                System.out.println("Secondary target: ping1 ("+ bestX + "," + bestY +")    set by" + our_archon_nr);
//            }

        }
        else if(secondBestTarget == 5){
            secondbestX = (int)pingHeardBeforeLast.x;
            secondbestY = (int)pingHeardBeforeLast.y;

//            if(secondbestX != last2ndBestX || secondbestY != last2ndBestY) {
//                System.out.println("Secondary target: ping2  ("+ bestX + "," + bestY +")   set by" + our_archon_nr);
//            }
        }else if (secondBestTarget == 6){
            secondbestX = rc.readBroadcast(CONFIRMED_ENEMY_X);
            secondbestY = rc.readBroadcast(CONFIRMED_ENEMY_Y);
        }
        else if(secondBestTarget == 7){
            secondbestX = rc.readBroadcast(CONFIRMED_ENEMY_TREE_X);
            secondbestY = rc.readBroadcast(CONFIRMED_ENEMY_TREE_Y);
        }else if(secondBestTarget == 8){
            secondbestX = (int)cluster1.x;
            secondbestY = (int)cluster1.y;
        } else if(secondBestTarget == 9){
            secondbestX = (int)cluster2.x;
            secondbestY = (int)cluster2.y;
        }  else if(secondBestTarget == 10){
            secondbestX = rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_X) / 100;
            secondbestY = rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_Y) / 100;
        }



//        rc.broadcast(WOODCUT_X, woodcut_x);
//        rc.broadcast(WOODCUT_Y, woodcut_y);

        //System.out.println(" woodxy " + woodcut_x + " ," + woodcut_y);

//        if(ally.name().equals("A")) {
//            rc.setIndicatorLine(myLocation, new MapLocation(bestX, bestY), 200, 20, 44);
//
//            rc.setIndicatorLine(myLocation, new MapLocation(secondbestX, secondbestY), 60, 40, 200);
//        }else{
//            rc.setIndicatorLine(myLocation, new MapLocation(bestX, bestY), 230, 80, 0);
//            rc.setIndicatorLine(myLocation, new MapLocation(secondbestX, secondbestY), 35, 230, 255);
//        }

//        System.out.println("main: " + bestTarget + "  " + bestX +"," + bestY +   " second: " + secondBestTarget + " " + secondbestX +"," + secondbestY);


        rc.broadcast(MAIN_AGRESSIVE_TARGET_X, bestX);
        rc.broadcast(MAIN_AGRESSIVE_TARGET_Y, bestY);
        rc.broadcast(SECONDARY_AGRESSIVE_TARGET_X, secondbestX);
        rc.broadcast(SECONDARY_AGRESSIVE_TARGET_Y, secondbestY);

        rc.broadcast(AGRESSIVE_TARGETS_LAST_UPDATE,turn);


    }
    public void processPings() throws GameActionException {

        MapLocation[] pingsheard = rc.senseBroadcastingRobotLocations();


        int maintargetX = rc.readBroadcast(MAIN_AGRESSIVE_TARGET_X);
        int maintargetY = rc.readBroadcast(MAIN_AGRESSIVE_TARGET_Y);


        //Filter out the pings made by our commanders
        int x1 = rc.readBroadcast(OUR_ARCHON_1_X);
        int x2 = rc.readBroadcast(OUR_ARCHON_2_X);
        int x3 = rc.readBroadcast(OUR_ARCHON_3_X);

        int x5 = rc.readBroadcast(OUR_SCOUT_1_X);
        int x6 = rc.readBroadcast(OUR_SCOUT_2_X);
        int x7 = rc.readBroadcast(OUR_SCOUT_3_X);

        int y1 = rc.readBroadcast(OUR_ARCHON_1_Y);
        int y2 = rc.readBroadcast(OUR_ARCHON_2_Y);
        int y3 = rc.readBroadcast(OUR_ARCHON_3_Y);

        int y5 = rc.readBroadcast(OUR_SCOUT_1_Y);
        int y6 = rc.readBroadcast(OUR_SCOUT_2_Y);
        int y7 = rc.readBroadcast(OUR_SCOUT_3_Y);

        float x4 = myLocation.x;

        MapLocation oldTankLocation = null;
        MapLocation newTankLocation = null;

        MapLocation oldGardenerLocation = null;


        if(rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_TURN) > 0) {
            oldGardenerLocation = new MapLocation((float) rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_X) / 100f, rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_Y) / 100f);
        }
        MapLocation newGardenerLocation = null;


        MapLocation oldArchon1Loc = null;
        MapLocation oldArchon2Loc = null;
        MapLocation oldArchon3Loc = null;
        MapLocation newArchon1Loc = null;
        MapLocation newArchon2Loc = null;
        MapLocation newArchon3Loc = null;

        int turnsAgoTank = turn - rc.readBroadcast(CONFIRMED_ENEMY_TANK_TURN);
        if (turnsAgoTank == 1 || turnsAgoTank == 2) {
            oldTankLocation = new MapLocation(((float) rc.readBroadcast(CONFIRMED_ENEMY_TANK_X)) / 10f, ((float) rc.readBroadcast(CONFIRMED_ENEMY_TANK_Y)) / 10f);
            //System.out.print("detected tank last turn at " + oldTankLocation);
        }


        int turnsgoarchon1 = turn - rc.readBroadcast(THEIR_ARCHON_1_LASTSEEN);
        if (turnsgoarchon1 <= 2) {
            oldArchon1Loc = new MapLocation(((float) rc.readBroadcast(THEIR_ARCHON_1_X)) / 10f, ((float) rc.readBroadcast(THEIR_ARCHON_1_Y)) / 10f);

//            rc.setIndicatorDot(oldArchon1Loc,255,0,0);

        }
        int turnsgoarchon2 = turn - rc.readBroadcast(THEIR_ARCHON_2_LASTSEEN);
        if (turnsgoarchon2 <= 2) {
            oldArchon2Loc = new MapLocation(((float) rc.readBroadcast(THEIR_ARCHON_2_X)) / 10f, ((float) rc.readBroadcast(THEIR_ARCHON_2_Y)) / 10f);
//            rc.setIndicatorDot(oldArchon2Loc,200,100,100);
//            rc.setIndicatorDot(oldArchon2Loc,0,255,0);

        }
        int turnsgoarchon3 = turn - rc.readBroadcast(THEIR_ARCHON_3_LASTSEEN);
        if (turnsgoarchon3 <= 2) {
            oldArchon3Loc = new MapLocation(((float) rc.readBroadcast(THEIR_ARCHON_3_X)) / 10f, ((float) rc.readBroadcast(THEIR_ARCHON_3_Y)) / 10f);
//            rc.setIndicatorDot(oldArchon3Loc,200,100,100);
//            rc.setIndicatorDot(oldArchon3Loc,0,0,255);

        }


        if ( likelyEnemyCount < 100 && turn > radioturn || (turn % 3 != 1)){
            likelyEnemyLocations = new MapLocation[100];
            likelyEnemyCount = 0;
        }

        MapLocation ping1 = null;
        MapLocation ping2 = null;
        for(int i = 0; i < pingsheard.length;i++) {

            boolean isKnownTarget = false;
            MapLocation ping = pingsheard[i];

//            if((turn % 3 != 1)) {
//                rc.setIndicatorDot(ping, 120, 120, 120);
//            }


            //Update map size based on what we heard
            if(ping.x + 1 > mapHighestX){
                mapHighestX = (int)ping.x + 1;
            }
            if(ping.x - 1 < mapLowestX){
                mapLowestX = (int)ping.x - 1;
            }
            if(ping.y + 1 > mapHighestY){
                mapHighestY = (int)ping.y + 1;
            }
            if(ping.y - 1 < mapLowestY){
                mapLowestY = (int)ping.y - 1;
            }




            if(oldTankLocation != null) {
                    if (ping.x - 0.7 < oldTankLocation.x && ping.x + 0.7 > oldTankLocation.x) {
                        if (ping.y - 0.7 < oldTankLocation.y && ping.y + 0.7 > oldTankLocation.y) {
                            newTankLocation = ping;
                            oldTankLocation = null; //we dont have to do this anymore
                            isKnownTarget = true;


                        }
                    }

            }

            if(oldArchon1Loc != null && !isKnownTarget) {
                    if (ping.x - 1.51 < oldArchon1Loc.x && ping.x + 1.51 > oldArchon1Loc.x) {
                        if (ping.y - 1.51 < oldArchon1Loc.y && ping.y + 1.51 > oldArchon1Loc.y) {
                            newArchon1Loc = ping;
                            oldArchon1Loc = null; //we dont have to do this anymore
                            isKnownTarget = true;

//                            if(ally.name().equals("A") && Archon.our_archon_nr == 1) {
//                                rc.setIndicatorLine(myLocation, ping, 200, 122, 122);
//                            }

                        }
                    }

            }
            if(oldArchon2Loc != null && !isKnownTarget) {
                    if (ping.x - 1.51 < oldArchon2Loc.x && ping.x + 1.51 > oldArchon2Loc.x) {
                        if (ping.y - 1.51 < oldArchon2Loc.y && ping.y + 1.51> oldArchon2Loc.y) {
                            newArchon2Loc = ping;
                            oldArchon2Loc = null; //we dont have to do this anymore
                            isKnownTarget = true;
//                            if(ally.name().equals("A") && Archon.our_archon_nr == 1) {

//                                rc.setIndicatorLine(myLocation, ping, 122, 200, 122);
//                            }

                        }
                    }

            }
            if(oldArchon3Loc != null && !isKnownTarget ) {
                    if (ping.x -1.51< oldArchon3Loc.x && ping.x + 1.51 > oldArchon3Loc.x) {
                        if (ping.y - 1.51 < oldArchon3Loc.y && ping.y + 1.51> oldArchon3Loc.y) {
                            newArchon3Loc = ping;
                            oldArchon3Loc = null; //we dont have to do this anymore
                            isKnownTarget = true;
//                            if(ally.name().equals("A") && Archon.our_archon_nr == 1) {

//                                rc.setIndicatorLine(myLocation, ping, 122, 122, 200);
//                            }

                        }
                    }

            }

                if(newGardenerLocation == null && oldGardenerLocation != null) {
                    if (ping.x - 0.6 < oldGardenerLocation.x && ping.x + 0.6 > oldGardenerLocation.x) {
                        if (ping.y - 0.6 < oldGardenerLocation.y && ping.y + 0.6 > oldGardenerLocation.y) {
                            newGardenerLocation = new MapLocation((int)(ping.x * 100),(int)(ping.y * 100));
                            isKnownTarget = true;
                        }
                    }

                }






            if(turn % 3 != 1) { //We broadcast a lot at  turn %3 ==0, and so we don't want to listen at turn %3==1 because then we'd be receiving a lot of our own messages

                if ((int) ping.x - 10 < x1 && (int) ping.x + 10 > x1) {
                    if ((int) ping.y - 10 < y1 && (int) ping.y + 10 > y1) {
                        continue;
                    }

                }
                if ((int) ping.x - 10 < x2 && (int) ping.x + 10 > x2) {

                    if ((int) ping.y - 10 < y2 && (int) ping.y + 10 > y2) {
                        continue;
                    }
                }
                if ((int) ping.x - 10 < x3 && (int) ping.x + 10 > x3) {
                    if ((int) ping.y - 10 < y3 && (int) ping.y + 10 > y3) {
                        continue;
                    }
                }

                if ((int) ping.x - 10 < x4 && (int) ping.x + 10 > x4) {
                    if ((int) ping.y - 10 < myLocation.y && (int) ping.y + 10 > myLocation.y) {
                        continue;
                    }
                }
                if ((int) ping.x - 10 < x5 && (int) ping.x + 10 > x5) {
                    if ((int) ping.y - 10 < y5 && (int) ping.y + 10 > y5) {
                        continue;
                    }

                }
                if ((int) ping.x - 10 < x6 && (int) ping.x + 10 > x6) {

                    if ((int) ping.y - 10 < y6 && (int) ping.y + 10 > y6) {
                        continue;
                    }
                }
                if ((int) ping.x - 10 < x7 && (int) ping.x + 10 > x7) {
                    if ((int) ping.y - 10 < y7 && (int) ping.y + 10 > y7) {
                        continue;
                    }
                }


                //Detect enemy gardeners produced at the start
                if(oldGardenerLocation == null && newGardenerLocation == null && turn < 10 && !isKnownTarget){
                    newGardenerLocation = new MapLocation((int)(ping.x * 100),(int)(ping.y * 100));
                }





                //We can also filter out units around our main target, as they're very likely ours, or if they aren't, it doesn't matter
                //As our purpose is to find units in places we are blind to
                if ((int) ping.x - 10 < maintargetX && (int) ping.x + 10 > x4) {
                    if ((int) ping.y - 10 < maintargetX && (int) ping.y + 10 > maintargetY) {
                        continue;
                    }
                }




                likelyEnemyLocations[likelyEnemyCount] = ping;
                likelyEnemyCount++;

                if (ping1 == null) {
                    ping1 = ping;

                } else if (ping2 == null) {
                    ping2 = ping;
                }
            }
        }

            if(newTankLocation != null){
                rc.broadcast(CONFIRMED_ENEMY_TANK_TURN, turn);
                rc.broadcast(CONFIRMED_ENEMY_TANK_X, (int)(newTankLocation.x* 10));
                rc.broadcast(CONFIRMED_ENEMY_TANK_Y, (int)(newTankLocation.y*10));
            }

            if(newArchon1Loc != null){
                rc.broadcast(THEIR_ARCHON_1_LASTSEEN, turn);
                rc.broadcast(THEIR_ARCHON_1_X, (int)(newArchon1Loc.x* 10));
                rc.broadcast(THEIR_ARCHON_1_Y, (int)(newArchon1Loc.y*10));
//                rc.setIndicatorDot(newArchon1Loc,100,100,200);
            }

            if(newArchon2Loc != null){
                rc.broadcast(THEIR_ARCHON_2_LASTSEEN, turn);
                rc.broadcast(THEIR_ARCHON_2_X, (int)(newArchon2Loc.x* 10));
                rc.broadcast(THEIR_ARCHON_2_Y, (int)(newArchon2Loc.y*10));
//                rc.setIndicatorDot(newArchon2Loc,100,100,200);

            }

            if(newArchon3Loc != null){
                rc.broadcast(THEIR_ARCHON_3_LASTSEEN, turn);
                rc.broadcast(THEIR_ARCHON_3_X, (int)(newArchon3Loc.x* 10));
                rc.broadcast(THEIR_ARCHON_3_Y, (int)(newArchon3Loc.y*10));
//                rc.setIndicatorDot(newArchon3Loc,100,100,200);

            }


            if(newGardenerLocation != null){
                int timeAgo = turn - rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_TURN);
                if(timeAgo <= 1 || oldGardenerLocation == null){
                    //We saw it last turns, and still see it
                    rc.broadcast(CONFIRMED_ENEMY_GARDENER_X,(int)(newGardenerLocation.x+0.5f));
                    rc.broadcast(CONFIRMED_ENEMY_GARDENER_Y,(int)(newGardenerLocation.y+0.5f));
                    rc.broadcast(CONFIRMED_ENEMY_GARDENER_TURN, turn);

//                    if(ally.name().equals("A") && Archon.our_archon_nr == 1) {

//                        rc.setIndicatorLine(myLocation, new MapLocation(newGardenerLocation.x / 100, newGardenerLocation.y / 100), 200, 50, 255);
//                    }

//                    rc.setIndicatorDot(new MapLocation(myLocation.x + 2,myLocation.y), 200,50,50);

                }
                else if(timeAgo > 6 || turn % 3 == 0){
                    if(rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_X)== (int)(newGardenerLocation.x+0.5f)&&rc.readBroadcast(CONFIRMED_ENEMY_GARDENER_Y) == (int)(newGardenerLocation.y+0.5f)  ){
                        //Exactly the same spot,this is likely the same gardener we saw earlier
                        rc.broadcast(CONFIRMED_ENEMY_GARDENER_X,(int)(newGardenerLocation.x+0.5f));
                        rc.broadcast(CONFIRMED_ENEMY_GARDENER_Y,(int)(newGardenerLocation.y+0.5f));
                        rc.broadcast(CONFIRMED_ENEMY_GARDENER_TURN, turn);
                        //rc.setIndicatorDot(new MapLocation(newGardenerLocation.x / 100, newGardenerLocation.y/100), 50,200,50);
//                        rc.setIndicatorDot(new MapLocation(myLocation.x + 2,myLocation.y), 50,200,50);

                    }
                }
            }



        if(turn % 3 != 0) { //Only listen when we know we'll send out the least pings
            //Register the pings as viable targets
            if (ping1 != null && ping2 != null) {
                pingHeardLast = ping1;
                pingHeardBeforeLast = ping2;
                turnHeardLastPing = turn;
                turnHeardSecondLastPing = turn;
            } else if (ping1 != null) {
                pingHeardBeforeLast = pingHeardLast;
                turnHeardSecondLastPing = turnHeardLastPing;
                pingHeardLast = ping1;
                turnHeardLastPing = turn;
            }
        }


        if(!(this instanceof Archon)){
            shouldbroadcastMapSize = true;
        }

    }


    public void calculateEnemyClusters() throws GameActionException{
        cluster1 = null;
        cluster2 = null;
        cluster3 = null;
        if(likelyEnemyLocations != null && (this instanceof Archon)){ //Let's not do this in non-archon commanders, too much bytecode
            int sampling = 1;
            if(likelyEnemyCount > 50){
                sampling = 3;
            } else if(likelyEnemyCount > 30){
                sampling = 2;
            }

            int score1 = 3; //Only accept clusters of more than 2 units
            int score2 = 3;
            int score3 = 3;


            for(int i = 0; i < likelyEnemyCount; i+=sampling){
                int count = 0;
                float centerX = likelyEnemyLocations[i].x;
                float centerY = likelyEnemyLocations[i].y;
                for(int i2 = 0; i2 < likelyEnemyCount; i2++){
                   // rc.setIndicatorDot(likelyEnemyLocations[i],120,120,120);
                    if(likelyEnemyLocations[i].x - 7 < centerX &&  likelyEnemyLocations[i].x + 7 > centerX && likelyEnemyLocations[i].y - 7 < centerY &&  likelyEnemyLocations[i].y + 7 > centerY) {
                        count++;
                    }
                }

                if(count > score1){
                    if(cluster1 != null){
                        cluster3 = cluster2;
                        cluster2 = cluster1;
                        score3 = score2;
                        score2 = score1;
                    }

                    cluster1 = likelyEnemyLocations[i];
                    score1 = count;

                } else if(count > score2){
                    if(cluster2 != null){
                        cluster3 = cluster2;
                        score3 = score2;
                    }

                    cluster2= likelyEnemyLocations[i];
                    score2 = count;

                }else if(count > score3){
                    cluster3= likelyEnemyLocations[i];
                    score3 = count;
                }
            }


            if(cluster1 != null){
               // rc.setIndicatorDot(cluster1,255,0,0);
                rc.broadcast(CLUSTER1_TURN,turn);
                rc.broadcast(CLUSTER1_X,(int)(cluster1.x * 10));
                rc.broadcast(CLUSTER1_Y,(int)(cluster1.y * 10));
                rc.broadcast(CLUSTER1_SIZE, score1);

            }
            if(cluster2 != null){
            //    rc.setIndicatorDot(cluster2,0,255,0);
                rc.broadcast(CLUSTER2_TURN,turn);
                rc.broadcast(CLUSTER2_X,(int)(cluster2.x * 10));
                rc.broadcast(CLUSTER2_Y,(int)(cluster2.y * 10));
                rc.broadcast(CLUSTER2_SIZE, score2);

            }
            if(cluster3 != null){
             //   rc.setIndicatorDot(cluster3,0,0,255);
                rc.broadcast(CLUSTER3_TURN,turn);
                rc.broadcast(CLUSTER3_X,(int)(cluster3.x * 10));
                rc.broadcast(CLUSTER3_Y,(int)(cluster3.y * 10));
                rc.broadcast(CLUSTER3_SIZE, score3);

            }
        }
    }

    public void calculateStrategicGoals() throws Exception{

        float aggresion = 1.5f;

        float defense = 0f;
        float woodcutting = 0;

       // float defenseScout = 1f;

        aggresion += rc.getTeamBullets() / 800f; //We're doing well, gogo attack
        aggresion += friendlyForceDetectedThisTurn / 5f; //Stop standing around
        aggresion += rc.readBroadcast(SOLDIERS_REPORTING_IN) /5f; //More units, more agression

        if(turn - lastTurnSeriousThreat < 25){
            aggresion += (turn - lastTurnSeriousThreat) / 5;
        }
        else{
            aggresion += 5;
        }

        if(turn < 250){
            aggresion += 1;
        }

        if(mapSizeType == HUGE){
            aggresion -= 2;
        }


        defense += enemyForceDetectedThisTurn;

        if(rc.getTeamVictoryPoints() > 600){
            defense += 3; //Victory in range
        }

        if(rc.readBroadcast(IS_REAL_DEF_TARGET) == 1){
            defense += 4;
        }

       // defenseScout += enemyScoutsDetectedThisTurn * 3;

        if(lastHealth - rc.getHealth() > 0.1){
            //We're losing health boys! Defend!
            defense += 2;

            if(lastHealth - rc.getHealth() > 5){
                //We're losing health boys! Defend!
                defense += 4;
            }
//            if(rc.getHealth() < 10){
//                //Actually.. forget about me! Go on without me guys and kill them fast
//                aggresion += 10;
//                defense -= 10;
//            }
        }


        //Don't want to mess up some evaluation things too much
        if(aggresion > 8  && aggresion > defense){
            aggresion = 8;
            defense = 0;
        }
        if(defense > 8  && aggresion < defense){
            defense = 8;
            aggresion = 1;
        }

        if(aggresion == defense){
            aggresion++;
        }

        if(aggresion < 1){
            aggresion = 1;
        }
        if(defense < 0){
            defense = 0;
        }

        woodcutting = ((neutralTreesDetectedThisTurn * 2) + choppableAreaDetectedThisTurn/1.5f) - (aggresion);
        woodcutting += neutralsInRange6;
        if(mapSizeType >= LARGE){
            woodcutting += 2;
        }

       // System.out.println("woodcutting: " + woodcutting + " aggr: " + aggresion + " neut: " + neutralTreesDetectedThisTurn + "  neut6: " + neutralsInRange6 );

        if( woodcutting > 7){
            rc.broadcast(WOODCUTTING_FOCUS, 1);
        }
        else{
            rc.broadcast(WOODCUTTING_FOCUS, 0);
        }

        //Were in a situation where our archon is 'captured', in this case, just shoot at the enemy archon instead of trying to save bullets
        if(enemyForceDetectedThisTurn > 0 && friendlyForceDetectedThisTurn == 0 &&  ownArchonCount == 1 && turn - rc.readBroadcast(GARDENERS_REPORTING_IN_TURN) > 4){
            rc.broadcast(OUR_ARCHON_CAPTURED, 1);
        }else{
            rc.broadcast(OUR_ARCHON_CAPTURED, 0);
        }



//        System.out.println("wc: " + woodcutting + " aggr: " + aggresion);

        rc.broadcast(AGRESSION_LEVEL, (int)aggresion);
        rc.broadcast(DEFENSE_LEVEL, (int)defense);
        rc.broadcast(LAST_UPDATED_GRAND_STRAT, turn);
        // System.out.println("Updating strat goals");

    }

    public void calculateEconGoals() throws GameActionException {
        updateMapSize();
        rc.broadcast(ECON_GOALS_LAST_UPDATE, turn);

        enemyArchonCount = 0;
        if(turn - rc.readBroadcast(THEIR_ARCHON_1_LASTSEEN) < 30){
            enemyArchonCount++;
        }
        if(rc.readBroadcast(THEIR_ARCHON_2_ID) >= -1 && turn - rc.readBroadcast(THEIR_ARCHON_2_LASTSEEN) < 30){
            enemyArchonCount++;
        }
        if(rc.readBroadcast(THEIR_ARCHON_3_ID) >= -1 && turn - rc.readBroadcast(THEIR_ARCHON_3_LASTSEEN) < 30){
            enemyArchonCount++;
        }
        ownArchonCount = 0;
        if(turn - rc.readBroadcast(OUR_ARCHON_1_LASTSEEN) < 5){
            ownArchonCount++;
        }
        if(rc.readBroadcast(OUR_ARCHON_3_ID) >= -1 && turn - rc.readBroadcast(OUR_ARCHON_2_LASTSEEN) < 5){
            ownArchonCount++;
        }
        if(rc.readBroadcast(OUR_ARCHON_2_ID) >= -1 && turn - rc.readBroadcast(OUR_ARCHON_3_LASTSEEN) < 5){
            ownArchonCount++;
        }

        int vpMode = 0;


        int floatingBullets = 5; //Realized the floating bullet system actually makes us build less units


        if(rc.getTreeCount() > 10 && enemyForceDetectedThisTurn == 0){
            floatingBullets += 100;
        }

        float lumberjack = 0.1f;
        float soldier = 0.1f;
        float scout = 0.1f;
        float tank = 0.1f;
        float tree = 0.1f;
        float gardener = 0.1f;

        float totalbullets = rc.getTeamBullets();
        float remainingBullets = totalbullets - floatingBullets;

        int reportedSoldiers = rc.readBroadcast(SOLDIERS_REPORTING_IN);
        int stuckSoldiers = rc.readBroadcast(SOLDIERS_REPORTING_IN_STUCK);
        int treecount = rc.getTreeCount();
        boolean stuckMode = false;
        int lumberJAckCount = rc.readBroadcast(LUMBERJACKS_REPORTING_IN);
        int soldierreports = rc.readBroadcast(SOLDIERS_REPORTING_IN);
        int lastturnconfirmd = rc.readBroadcast(CONFIRMED_ENEMY_TURN);
        int lastturnbuildsomething = rc.readBroadcast(LAST_TURN_BUILD_SOMETHING);


        updateMapSize();
        int sides = 0;
        int closesides = 0;

        if(myLocation.x - mapLowestX < 5){
            sides++;
        }
        if(myLocation.y - mapLowestY < 5){
            sides++;
        }
        if(mapHighestX - myLocation.x < 5){
            sides++;
        }
        if(mapHighestY - myLocation.y < 5){
            sides++;
        }
        if(myLocation.x - mapLowestX < 3.5f){
            closesides++;
        }
        if(myLocation.y - mapLowestY < 3.5f){
            closesides++;
        }
        if(mapHighestX - myLocation.x < 3.5f){
            closesides++;
        }
        if(mapHighestY - myLocation.y < 3.5f){
            closesides++;
        }

        if(turn - rc.readBroadcast(LUMBERJACKS_REPORTING_IN_TURN) > 4){
            lumberJAckCount = 0;
        }

        MapLocation mainTarget =  new MapLocation(rc.readBroadcast(MAIN_AGRESSIVE_TARGET_X),  rc.readBroadcast(MAIN_AGRESSIVE_TARGET_Y));

        if(reportedSoldiers >0 && stuckSoldiers > 0){
            if( ((float)stuckSoldiers)/((float)reportedSoldiers) > 0.4f){
                stuckMode = true;
            }
        }

        //Scouts
        int scoutcount = 0;
        if (rc.readBroadcast(OUR_SCOUT_1_ID) >= 0 && turn - rc.readBroadcast(OUR_SCOUT_1_LASTSEEN) < 3) {
            scoutcount++;
        }
        if (rc.readBroadcast(OUR_SCOUT_2_ID) >= 0 && turn - rc.readBroadcast(OUR_SCOUT_2_LASTSEEN) < 3) {
            scoutcount++;
        }
        if (rc.readBroadcast(OUR_SCOUT_3_ID) >= 0 && turn - rc.readBroadcast(OUR_SCOUT_3_LASTSEEN) < 3) {
            scoutcount++;
        }

        int reported = rc.readBroadcast(SCOUTS_REPORTING_IN_TOTAL);
        if( reported > scoutcount){
            scoutcount = reported;
        }

        if(turn < 100){
            int build = rc.readBroadcast(SCOUTS_BUILD);
            if(build > scoutcount){
                scoutcount = build; //scouts dont report in while building, so use this as an alternative early game
            }
        }

        scout = 2;



        if(scoutcount == 0){
            scout += 5;
            if(turn < 30 && enemyForceDetectedThisTurn == 0){
                if(bulletsDetectedInTrees > 20) {
                    if(mapSizeType > SMALL) {
                        scout += 55; //Early extr boost on maps where there's bullets, quick return on investment
                    }
                }else if(choppableAreaDetectedThisTurn > 8){
                    scout += 7; //Early scout on maps where we might get stuck
                }
            }
            if(bulletsDetectedInTrees > 0){
                scout += 10;
                if(bulletsDetectedInTrees > 20){
                    scout += 10;
                    if(bulletsDetectedInTrees > 60){
                        scout += 20;
                        if(bulletsDetectedInTrees > 100){
                            scout += 40;
                        }
                    }
                }
            }
        }
        if (scoutcount < 2) {

            scout += 4;

            if(turn < 60 || rc.getTreeCount() != 0) {
                scout += 5;
            }

        }
        if(bulletsDetectedInTrees > 20) {
            if(scoutcount < 3){
                scout += 4;
            }
        }



        if(scoutcount == 1 && turn < 70){
            scout -= 20; //Too risky, might be on a rush map still
        }

        if(scoutcount < 3){
            scout++;
        }

        if(mapSizeType == MEDIUM){
            scout+=1;
        }
        if (mapSizeType >= LARGE) {
            scout+=3;
        }

        scout -= turn / 300; //Scout harass less effective late

        scout -= (scoutcount/1.5f); //Harasses individually dont become as good if theres too many of these

        if(mainTarget.x < 0){
            scout += 4;
        }

//        if (turn < 5) {
//            scout += 5; //Get quick scouts out to shuffle some trees
//        }

        if (neutralTreesDetectedThisTurn >= 4) { //Scouts help in more dense maps
            scout+=4;
        }

        if (enemyForceDetectedThisTurn > 0) {
            scout -= 5;
        }
        scout += enemyScoutsDetectedThisTurn / 2;

        if(turn - rc.readBroadcast(DETECTED_SCOUT_TURN) < 4){
            scout += 2; //Defend against enemy scouts
        }

        if(scoutcount > 4){
            scout -= 100; //Scout meta is dead meta
        }

//        System.out.println("scoutcoint: " + scoutcount  +  " reported: " + rc.readBroadcast(SCOUTS_REPORTING_IN_TOTAL) );

        //Lumberjacks
        lumberjack = -7;



        int cappedNeutrals = neutralTreesDetectedThisTurn;
        float cappedArea = choppableAreaDetectedThisTurn;

        if(cappedNeutrals > 12){
            cappedNeutrals = 12;
        }
        if(cappedArea > 35){
            cappedArea = 35;
        }

        float treeScore = (neutralsInRange6 * 0.1f) + (cappedNeutrals * 0.8f) + (treesInEnemyDirection * 4f) + (cappedArea * 0.75f) +  (thingsInRange4 / 3f) - rc.getTreeCount();
        if(sides == 1){
            treeScore *= 1.5;
        } else if(sides == 2){
            treeScore *= 2.5f;
        }
        if(mapSizeType <= SMALL){
            treeScore *= 0.8f;
        }

        if(treeScore > 200){
            if(lumberJAckCount > 3){
                lumberjack += 30;
            }
            else{
                lumberjack += 50;
            }
        }
        else if(treeScore > 100){
            if(lumberJAckCount> 2){
                lumberjack += 15;
            }else if(lumberJAckCount == 2){
                lumberjack += 20;
            } else if(lumberJAckCount == 1){
                lumberjack += 35;
            }else{
                lumberjack += 50;
            }
        }
        else if(treeScore > 65){
            if(lumberJAckCount> 2){
                lumberjack += 15;
            }else if(lumberJAckCount == 2){
                lumberjack += 20;
            } else if(lumberJAckCount == 1){
                lumberjack += 25;
            }else{
                lumberjack += 35;
            }
        }
        else if(treeScore > 40){
            if(lumberJAckCount > 1){
                lumberjack += 10;
            } else if(lumberJAckCount == 1){
                lumberjack += 15;
            } else{
                lumberjack += 20;
            }
        } else if(treeScore > 10){
            if(lumberJAckCount > 0){
                lumberjack += 5;
            }
            else{
                lumberjack += 8;
            }
        }


//        System.out.println("TREESCORE " + treeScore);

//        if(sides == 0) {
//            if (lumberJAckCount < 1 || friendlyLumberjacksDetectedThisTurn < 1) {
//                if (choppableAreaDetectedThisTurn < 12) {
//                    lumberjack += choppableAreaDetectedThisTurn * 1f;
//                } else {
//                    lumberjack += 12f;
//                }
//                if (neutralTreesDetectedThisTurn < 12) {
//                    lumberjack += neutralTreesDetectedThisTurn * 1.25f;
//                } else {
//                    lumberjack += 15;
//                }
//            } else if (lumberJAckCount < 2) {
//                if (choppableAreaDetectedThisTurn < 15) {
//                    lumberjack += choppableAreaDetectedThisTurn * 0.6f;
//                } else {
//                    lumberjack += 9f;
//                }
//                if (neutralTreesDetectedThisTurn < 15) {
//                    lumberjack += neutralTreesDetectedThisTurn * 0.75f;
//                } else {
//                    lumberjack += 11.25f;
//                }
//            } else {
//                if (choppableAreaDetectedThisTurn < 15) {
//                    lumberjack += choppableAreaDetectedThisTurn * 0.5f;
//                } else {
//                    lumberjack += 7.5f;
//                }
//
//                if (neutralTreesDetectedThisTurn < 15) {
//                    lumberjack += neutralTreesDetectedThisTurn * 1.2f;
//                } else {
//                    lumberjack += 18f;
//                }
//            }
//        }
//        else if (sides == 1){
//            if (lumberJAckCount < 1 || friendlyLumberjacksDetectedThisTurn < 1) {
//                if (choppableAreaDetectedThisTurn < 12) {
//                    lumberjack += choppableAreaDetectedThisTurn * 3f;
//                } else {
//                    lumberjack += 36f;
//                }
//                if (neutralTreesDetectedThisTurn < 12) {
//                    lumberjack += neutralTreesDetectedThisTurn * 3f;
//                } else {
//                    lumberjack += 36;
//                }
//            } else if (lumberJAckCount < 2) {
//                if (choppableAreaDetectedThisTurn < 12) {
//                    lumberjack += choppableAreaDetectedThisTurn * 2.5f;
//                } else {
//                    lumberjack += 30f;
//                }
//                if (neutralTreesDetectedThisTurn < 12) {
//                    lumberjack += neutralTreesDetectedThisTurn * 2.5f;
//                } else {
//                    lumberjack += 30;
//                }
//            } else {
//                if (choppableAreaDetectedThisTurn < 12) {
//                    lumberjack += choppableAreaDetectedThisTurn * 2f;
//                } else {
//                    lumberjack += 24f;
//                }
//                if (neutralTreesDetectedThisTurn < 15) {
//                    lumberjack += neutralTreesDetectedThisTurn * 2f;
//                } else {
//                    lumberjack += 30;
//                }
//            }
//        } else if (sides > 1){
//            if (lumberJAckCount < 1 || friendlyLumberjacksDetectedThisTurn < 1) {
//                if (choppableAreaDetectedThisTurn < 12) {
//                    lumberjack += choppableAreaDetectedThisTurn * 4f;
//                } else {
//                    lumberjack += 48f;
//                }
//                if (neutralTreesDetectedThisTurn < 12) {
//                    lumberjack += neutralTreesDetectedThisTurn * 4f;
//                } else {
//                    lumberjack += 48;
//                }
//            } else if (lumberJAckCount < 2) {
//                if (choppableAreaDetectedThisTurn < 12) {
//                    lumberjack += choppableAreaDetectedThisTurn * 3.5f;
//                } else {
//                    lumberjack += 42f;
//                }
//                if (neutralTreesDetectedThisTurn < 12) {
//                    lumberjack += neutralTreesDetectedThisTurn * 3f;
//                } else {
//                    lumberjack += 36;
//                }
//            } else {
//                if (choppableAreaDetectedThisTurn < 12) {
//                    lumberjack += choppableAreaDetectedThisTurn * 2.5f;
//                } else {
//                    lumberjack += 30f;
//                }
//                if (neutralTreesDetectedThisTurn < 12) {
//                    lumberjack += neutralTreesDetectedThisTurn * 2.5f;
//                } else {
//                    lumberjack += 30f;
//                }
//            }
//        }


        if(neutralTreesDetectedThisTurn > 4 && lastturnconfirmd == 0){
            lumberjack += 3;
        }


        if(lastturnconfirmd > 0 && lumberJAckCount > 0){
            lumberjack -= 5;
            if(mapSizeType <= MEDIUM){
                lumberjack -= 3;
            }
        }



        lumberjack += enemyTreesDetectedThisTurn * 4;

        lumberjack += enemyForceDetectedThisTurn;

        if(unitCarryingTreeDetectedThisTurn > 0 && mapSizeType > SMALL){
            lumberjack += 4;
        }
        if(gardenerCarryingTreeDetectedThisTurn > 0 && rc.readBroadcast(GARDENERS_REPORTING_IN_TOTAL) < 2 ){
            lumberjack += 3;
        }

        if(mapSizeType > SMALL) {
            if (goodUnitCarryingTreeDetectedThisTurn < 4) {
                lumberjack += goodUnitCarryingTreeDetectedThisTurn * 8;
            } else {
                lumberjack += 32;
            }
        }else {
            if (enemyForceDetectedThisTurn == 0) {
                if (goodUnitCarryingTreeDetectedThisTurn < 4) {
                    lumberjack += goodUnitCarryingTreeDetectedThisTurn * 5;
                } else {
                    lumberjack += 20;
                }
            } else {
                if (goodUnitCarryingTreeDetectedThisTurn < 4) {
                    lumberjack += goodUnitCarryingTreeDetectedThisTurn * 2;
                } else {
                    lumberjack += 8;
                }
            }
        }


        lumberjack -= friendlyLumberjacksDetectedThisTurn;
        lumberjack -= lumberJAckCount * 3f;

        if(mapSizeType == SMALL){
            lumberjack -= 4;
        }
        if(mapSizeType == TINY && turn < 100){
            lumberjack -= 6;
        }

        if(turn < 250 && lumberJAckCount > 2){
            lumberjack -= 5;
        }

        if(mapSizeType == TINY && lumberJAckCount > 0){
            lumberjack -= 6;
        }
        if(mapSizeType <= SMALL && lumberJAckCount > 2){
            lumberjack -= 8;
        }
        if(mapSizeType <= MEDIUM && lumberJAckCount > 4){
            lumberjack -= 7;
        }

       // lumberjack += enemyScoutsDetectedThisTurn; //Decent defender against scout harass

//        if(friendlyForceDetectedThisTurn ==0 && enemyForceDetectedThisTurn > 0){
//            lumberjack += 20; //Best emergency defence unit
//        }

//        if(turn < 10){
//            lumberjack += 5; //An early lumber is pretty good since they don't use bullets
//        }

        if(stuckMode && soldierreports > 4){
            if(lumberJAckCount < 4){
                if(treecount > 8 ) {
                    lumberjack += 15;
                } else if(treecount > 5){
                    lumberjack += 5;
                } else{
                    lumberjack += 2;
                }
            }

        }

        if(enemyForceDetectedThisTurn > 0){
            lumberjack -= 5;
        }

        if(rc.readBroadcast(THEIR_ARCHON_1_LASTTAGGED) > 0   || rc.readBroadcast(THEIR_ARCHON_2_LASTTAGGED) > 0 || rc.readBroadcast(THEIR_ARCHON_1_LASTTAGGED) > 0  ){
            lumberjack -= 5;
        }

        if (mapSizeType >= LARGE) {
            lumberjack += 2;
        }
        if(rc.getRobotCount() * 0.35f <  soldierreports){
            lumberjack -= 5;
        }

        if(turn < 100 && mapSizeType <= SMALL && lumberJAckCount > 0 && soldierreports < 2){
            lumberjack -= 6;
        }

        //Stuck?
        if(turn > 250 && timesNoticedLostSoldier == 0 && soldierreports > 0){
            lumberjack += 2;
            if(turn > 400) {
                lumberjack += 2;
            }
        }



        //Tanks
        tank = 12f;


        if(treecount >= 5){
            tank += 3;
            if(treecount > 10){
                tank += 4;
                if(treecount > 15){
                    tank += 3;
                }
            }
        }
        if(turn - rc.readBroadcast(CONFIRMED_ENEMY_TANK_TURN) < 4){
            tank += 7; //Our tanks are pretty good at killing enemy tanks with their snipe shot
        }
        tank -= neutralTreesDetectedThisTurn * 2; //We don't like these much
        tank += soldierreports * 1.5f;
        tank += lumberJAckCount / 3;
        int totaltanks = rc.readBroadcast(TANKS_REPORTING_IN);

        int tanksbuild = rc.readBroadcast(TANKS_BUILD);
        tank -= totaltanks * 3;
        tank -= tanksbuild;

        if(turn > 450 && turn < 550 && tanksbuild == 0 && totaltanks == 0 && neutralTreesDetectedThisTurn < 3 && treecount >= 5){
            tank += 20; //One surprise early tank will  crush many opponents

        }


        if(turn > 1000){
            tank += 3;
        }



        if(mapSizeType <= SMALL || mapSizeType == HUGE){
            tank -=7; //Too small and we'll have no room, too big and the tanks may never get to the enemy
        }

        //Soldiers
        soldier = 20.1f;
        soldier -= friendlyForceDetectedThisTurn / 2;

        soldier += enemyForceDetectedThisTurn * 1.5f; //Defensive movements
        soldier += enemyScoutsDetectedThisTurn;

        if(turn < 25 && mapSizeType == TINY){
            soldier += 10;
            if(enemyForceDetectedThisTurn > 0){
                soldier += 25;
            }
        }

        if(turn < 25 && mapSizeType == SMALL){
            soldier += 5;
            if(choppableAreaDetectedThisTurn < 15){
                soldier += 5;
            }
            if(enemyForceDetectedThisTurn > 0){
                soldier += 25;
            }
        }



        if(stuckMode){
            soldier -= 5;
        }
        soldier += remainingBullets / 50f;


        if(treeScore > 130){
            soldier -= 20;
//            System.out.println("too many trees0");
        }
        else if(treeScore > 70){
            soldier -= 10;
//            System.out.println("too many trees 1");

        }


        if((rc.getRobotCount() - stuckSoldiers) * 0.5f <  (soldierreports - stuckSoldiers) ){
            soldier -= 5;
        }
        if((rc.getRobotCount() - stuckSoldiers) * 0.2f >  (soldierreports - stuckSoldiers) ){
            soldier += 5;
        }

        if(turn > 700 && turn <900){
            soldier += 4; // Try to do a bit of a soldier push around here if game still going
        }

        if(turn < 150 && timesNoticedLostSoldier > 0){
            soldier += 3; //Shit, we may be losing the rush
        }

        if(turn > 350 && soldierreports < 4){
            soldier += 3;
        }
        if (mapSizeType <= SMALL) {
            soldier += 3;
        }
        if (mapSizeType == MEDIUM) {
            soldier += 1;
        }

        if(soldierreports < turn / 60){
            soldier += 8;
        }

        if(friendlyForceDetectedThisTurn ==0 && enemyForceDetectedThisTurn > 0){
            soldier += 10; //Best emergency defence unit
        }

        if(turn > 30 && turn - rc.readBroadcast(SOLDIERS_REPORTING_IN_TURN) > 4){
            soldier += 20;
        }

        //Trees
        tree = 13.5f;

        tree -= alliedTreesDetectedThisTurn * 2; //don't overbuild
        tree += friendlyForceDetectedThisTurn; //we're safe, lets econ

        if(neutralTreesDetectedThisTurn > 6){
            tree += 8; //a lot of trees indicates were locked in, go econ a little more
        }

        if (enemyForceDetectedThisTurn != 0) {
            tree -= 4;
        }
        tree -= enemyForceDetectedThisTurn;
        tree -= enemyLumberjacksDetectedThisTurn * 4;

        if(turn > 300) {
            tree += turn % 10;
        }

        if(turn < 150 && rc.getRobotCount() - ownArchonCount > 5  ||  turn < 200 && rc.getRobotCount() - ownArchonCount > 6 || turn < 250 && rc.getRobotCount() - ownArchonCount > 7){
            tree += 5; //We may be getting lots of free robots thanks to lumbers or something, just go econ
        }


        if(mapSizeType >= MEDIUM && (neutralsInRange6 + sides *2) < 7 && treecount == 0 && turn <30){
            tree += 10;
        }

        if (mapSizeType == LARGE) {
            tree += 4;
        }
        if (mapSizeType == HUGE) {
            tree += 7;
        }

        if (totalbullets < 100) { //Maybe calculate income instead
            tree += 1;
        }
        tree -= totalbullets / 1000;
        if(stuckMode){
            tree += 6;
        }

        if(turn > 150  && treecount < turn / 80){
            tree += 5;
        }
        if(turn > 450  && treecount < turn / 60){
            tree += 4;
        }

//        if(treecount < 8){
//            tree -= 7;
//        }
        if(turn - lastTurnSeriousThreat > 30){
            tree += 4;
        }
        if(turn - lastturnconfirmd > 8){
            tree += 3;
        }

        if(turn > 150 && lastturnconfirmd == 0){
            tree += 6; //We cant find any enemies..
        }

        if(turn > 200 && timesNoticedLostSoldier == 0 && soldierreports > 0){
            tree += 4;
            if(turn > 400) {
                tree += 4;
                if(turn > 600) {
                    tree += 6;
                }
            }
        }


        if(turn > 25 && turn <160){
            if(rc.getRobotCount() >= 4 && rc.getTreeCount() == 0){
                tree += 15; //One quick early tree
            }
        }



        int totalGardeners = rc.readBroadcast(GARDENERS_REPORTING_IN);
        int gardenersBuild = rc.readBroadcast(GARDENERS_BUILD);

        if(turn - rc.readBroadcast(GARDENERS_REPORTING_IN_TURN) > 3){
            if(turn < 10 &&  gardenersBuild > 0){
                totalGardeners = 1;
            }else{
                totalGardeners = 0;
            }
        }
        boolean recentlyBuildGardener = (turn - lastbuildGardener) < 20;
        if(recentlyBuildGardener && totalGardeners < 1){
            totalGardeners = 1;
        }


//        if (rc.getTreeCount() * 3 < totalGardeners) {
//            tree += 4;
//        }
        if (rc.getTreeCount() < totalGardeners) {
            tree += 2;
        }
        if(thingsInRange4 > 6){
            tree -= 3;//Too overcrowded
        }
        if(mainTarget.x < 0){
            tree += 2;
        }

        if(rc.getTreeCount() < 3){
            tree += 4;
        }


        if(soldierreports > 2) {
            if(mapSizeType == HUGE){
                if (turn > 600 && rc.getTreeCount() < totalGardeners * 4) {
                    tree += 9;
                } else if (turn > 400 && rc.getTreeCount() < totalGardeners * 3.8) {
                    tree += 8;
                } else if (turn > 250 && rc.getTreeCount() < totalGardeners * 3.4) {
                    tree += 7;
                } else if (turn > 150 && rc.getTreeCount() < totalGardeners * 3) {
                    tree += 6;
                }
            }else {
                if (turn > 800 && rc.getTreeCount()  < totalGardeners * 4) {
                    tree += 9;
                } else if (turn > 600 && rc.getTreeCount() < totalGardeners * 3.8) {
                    tree += 8;
                } else if (turn > 400 && rc.getTreeCount() < totalGardeners * 3.4) {
                    tree += 7;
                } else if (turn > 250 && rc.getTreeCount() < totalGardeners * 3) {
                    tree += 6;
                }
            }
        }





        if(rc.getRobotCount() - (ownArchonCount + scoutcount + totalGardeners) < 5){
            tree -= 2;
            if(rc.getRobotCount() - (ownArchonCount + scoutcount + totalGardeners) < 3){
                tree -= 5;
            }
        }

        float vpcost = rc.getVictoryPointCost();

        if(rc.getOpponentVictoryPoints() > 400){
            if (rc.getTeamVictoryPoints() < 100) {
                if ((reportedSoldiers + lumberJAckCount + 2 * rc.readBroadcast(TANKS_REPORTING_IN) + treecount) > 80 && treecount > 30) {
                    if (totalbullets > 105.01 + 2f * vpcost) {
                        rc.donate(0.01f + 2f * vpcost);
                    }
                    if (totalbullets > 105.01 + 6f * vpcost) {
                        rc.donate(0.01f + 4f * vpcost);
                    }
                }
                else{
                    soldier += 3;
                    scout += 3;
                    tank += 3;
                }
            } else if (rc.getTeamVictoryPoints() < 600) {
                if ((reportedSoldiers + lumberJAckCount + 2 * rc.readBroadcast(TANKS_REPORTING_IN) + treecount) > 50 && treecount > 10) {

                    if (totalbullets > 100.01 + 2f * vpcost) {
                        rc.donate(0.01f + 2f * vpcost);
                    }
                    if (totalbullets > 100.01 + 6f * vpcost) {
                        rc.donate(0.01f + 4f * vpcost);
                    }

                }
            } else  {
                if ((reportedSoldiers + rc.readBroadcast(TANKS_REPORTING_IN) + treecount) > 20) {
                    if (totalbullets > 0.01 + 4f * vpcost) {
                        rc.donate(0.01f + 4f * vpcost);
                    }
                    vpMode = 1;

                }
            }
        }
        else {
            if (rc.getTeamVictoryPoints() < 100) {
                if ((reportedSoldiers + lumberJAckCount + 2 * rc.readBroadcast(TANKS_REPORTING_IN) + treecount) > 80 && treecount > 40) {
                    if (totalbullets > 105.01 + 2f * vpcost) {
                        rc.donate(0.01f + 2f * vpcost);
                    }
                    if (totalbullets > 105.01 + 6f * vpcost) {
                        rc.donate(0.01f + 4f * vpcost);
                    }
                }
            } else if (rc.getTeamVictoryPoints() < 400) {
                if ((reportedSoldiers + lumberJAckCount + 2 * rc.readBroadcast(TANKS_REPORTING_IN) + treecount) > 70 && treecount > 30) {
                    if (totalbullets > 105.01 + 2f * vpcost) {
                        rc.donate(0.01f + 2f * vpcost);
                    }
                    if (totalbullets > 105.01 + 6f * vpcost) {
                        rc.donate(0.01f + 4f * vpcost);
                    }
                }
            } else if (rc.getTeamVictoryPoints() < 800) {
                if ((reportedSoldiers + rc.readBroadcast(TANKS_REPORTING_IN) + treecount) > 40 && treecount > 10) {
                    if (totalbullets > 105.01 + 2f * vpcost) {
                        rc.donate(0.01f + 2f * vpcost);
                    }
                    if (totalbullets > 105.01 + 6f * vpcost) {
                        rc.donate(0.01f + 4f * vpcost);
                    }
                    vpMode = 1;

                }
            } else {
                if (treecount > 10) {

                    float total = (int)(totalbullets / vpcost);
                    rc.donate(0.02f + total * vpcost);

                    vpMode = 1;
                }
            }
        }


        if(ownArchonCount == 0){
            if(enemyArchonCount == 0){
                if(rc.readBroadcast(GARDENERS_REPORTING_IN) > 1){
                    tree += 3;
                    soldier += 4;
                    scout += 2;
                }else{
                    tree += 1;
                    soldier += 4;
                    lumberjack += 2;
                }
            } else if(enemyArchonCount == 1){
                if(rc.readBroadcast(GARDENERS_REPORTING_IN) > 1){
                    tree += 3;
                    soldier += 4;
                    scout += 2;
                }else{
                    tree += 1;
                    soldier += 4;
                    lumberjack += 2;
                }
            }else if(enemyArchonCount == 2){
                tree += 2;
                soldier += 4;
                lumberjack += 3;
            }else if(enemyArchonCount == 3){
                //Well.. That's not good
                soldier += 4;
                lumberjack += 3;
            }
        } else if(ownArchonCount == 1){
            if(enemyArchonCount == 0){
                //Probably winning a rush game
                soldier += 2;
                tree += 1;
                scout += 1;
            } else if(enemyArchonCount == 1){
                //Might be a rush game
                soldier += 3;
                lumberjack += 1;

                if(turn < 50){
                    tree -= 7; //Let's not build tree first here
                }

            }else if(enemyArchonCount == 2){
                soldier += 3;
                lumberjack += 2;
                scout += 1;
            }else if(enemyArchonCount == 3){
                soldier += 4;
                lumberjack += 3;
            }
        } else if(ownArchonCount == 2){
            if(enemyArchonCount == 0){
                tree += 1;
                scout += 2;
            } else if(enemyArchonCount == 1){
                tree += 2;
                soldier += 1;
                lumberjack += 1;
                gardener += 1;
            }else if(enemyArchonCount == 2){
                tree += 2;
                scout += 1;
                gardener += 1;
            }else if(enemyArchonCount == 3){
                tree += 3;
                soldier += 3;
            }
        } else if(ownArchonCount == 3){
            //This is probably a longer game. Also possibly a good time to build on our advantage by econing
            if(enemyArchonCount == 0){
                tree += 2;
                scout += 3;
            } else if(enemyArchonCount == 1){
                tree += 2;
                scout += 3;
                gardener += 1;
            }else if(enemyArchonCount == 2){
                tree += 4;
                scout += 3;
                gardener += 3;
            }else if(enemyArchonCount == 3){

                if(turn < 40 && enemyForceDetectedThisTurn == 0){
                    if( mapSizeType == HUGE){
                        tree += 20; //Just get some quick macro out in these games. We keep losing on lack of trees
                        gardener += 3;
                        soldier -= 30;
                        lumberjack -= 30;
                        scout -= 30;
//                        System.out.println("ECON MODE");
                    }
//                    else{
//                        System.out.println("not huge? " + mapSizeType);
//
//                    }
                }
                tree += 4;
                scout += 4;
                gardener += 3;
            }
        }

//        System.out.println("our count " + ownArchonCount +  " their count " + enemyArchonCount);



            //Gardener
        if(this instanceof  Archon) {

            gardener += 11;
            gardener -= enemyForceDetectedThisTurn * 3;
            gardener += friendlyForceDetectedThisTurn / 2;

            if (turn < 5 && gardenersBuild < 1) { //First turn gardener
                gardener += 200;
            }
                int turnsSinceLastReport = turn - rc.readBroadcast(GARDENERS_REPORTING_IN_TURN);
                if (turnsSinceLastReport> 5 && !recentlyBuildGardener) { //Seems our gardeners are dead
                    if(ownArchonCount > 0 && enemyForceDetectedThisTurn > 0){
                        //Okay, well, let's ramp it up slowly until one of our archons pulls the trigger
                        if(totalbullets > 180){
                            gardener += turnsSinceLastReport * 1.5f; //Extra fast, since we can almost make a combat unit
                            soldier += 10;
                        }
                        if(totalbullets > 150){
                            gardener += turnsSinceLastReport * 1.5f; //Extra fast, since we can insta plant a tree and maybe be ok
                            tree += 30;
                        }
                        else{
                            gardener += turnsSinceLastReport;
                        }
                    }else {
                        gardener += 300;
                    }

            }

            if (neutralTreesDetectedThisTurn > 6) {
                gardener += 5; //a lot of trees indicates were locked in, go econ a little more
            }

            if(sides == 1){
                gardener -= 3;
            }
            if(sides == 2){
                gardener -= 6;
            }

            gardener -= totalGardeners * 2;

            if (turn > 800 && rc.getTreeCount() < totalGardeners * 3.5) {
                gardener -= 12; //
            }
            else if (turn > 600 && rc.getTreeCount() < totalGardeners * 3) {
                gardener -= 10; //
            }
            else if (turn > 400 && rc.getTreeCount() < totalGardeners * 2.5) {
                gardener -= 7; //
            }
            else if (turn > 200 && rc.getTreeCount()  < totalGardeners * 2) {
                gardener -= 5; //
            }

            if(totalGardeners > rc.getTreeCount()){
                gardener -= 3;
            }

            if(recentlyBuildGardener){
                gardener -= 5;
            }


            if((totalGardeners > 0 || recentlyBuildGardener) && rc.getTreeCount() < 2){
                gardener -= 10;
            }



            if (totalGardeners > 16) { //If we're this rich, just go get victory points or something
                gardener -= 100;
            }

            if( turn < 200 &&  turn - lastturnbuildsomething > 50 && totalGardeners < 2 && !recentlyBuildGardener && totalbullets > 150){
                gardener += 100; //We may be stuck
            }

            if(turn > 200 && timesNoticedLostSoldier == 0 && soldierreports > 0){
                gardener += 4;
                if(turn > 400) {
                    gardener += 4;
                    if(turn > 600) {
                        gardener += 6;
                    }
                }
            }

            if (mapSizeType == SMALL) { //Build new ones over time, compensated for by gardeners build
                gardener += (turn / 100f);
                gardener -= 2;
            } else if (mapSizeType == MEDIUM) {
                gardener += (turn / 75f);
                gardener -= 1;
            } else if (mapSizeType == LARGE) {
                gardener += (turn / 45f);
            } else if(mapSizeType == HUGE){
                gardener += (turn / 30f);

            }
            if(thingsInRange4 + closesides * 2 > 6 && totalGardeners > 1){
                gardener -= 20;//Too overcrowded
            }

            if(stuckMode){
                gardener += 2;
                if(friendlyGardenersDetectedThisTurn < 4) {
                    gardener += 5;
                }
            }

            gardener += remainingBullets / 40f;//We have a lot of bullets, maybe we dont have enough build capacity
            if (rc.getTeamBullets() - floatingBullets > 300)//might be stuck without gardeners who can produce
            {
                gardener += 8;
                //System.out.print("Too many bullets");
            }
        }



        if(enemyForceDetectedThisTurn < 2  && friendlyForceDetectedThisTurn > 15 && alliedTreesDetectedThisTurn > 15 && rc.getTeamBullets() > 500){
            //We may be overstuffing our base, instead go try and win the game with victory points
            gardener -= 10;
            soldier -= 10;
            tank -= 10;
            lumberjack -= 10;
            scout -= 10;
            tree -= 4;
            tree -= turn % 10;
            //System.out.println("Try to win game");
        }



        if(this instanceof  Archon) {

            if(thingsInRange4 < 8 || totalGardeners < 3) {
                if (rc.getTeamBullets() > 100) {
                    if (gardener > 6 && gardener > tree && gardener > lumberjack && gardener > tank && gardener > soldier && gardener > scout) {


                        Direction bestDirection = null;

                        if (bestDefensiveTargetBot != null) {
                            bestDirection = myLocation.directionTo(bestDefensiveTargetBot.location).opposite();
                        }
                        else{
                            if(mainTarget.x > 0){
                                bestDirection = myLocation.directionTo(mainTarget);
                            }
                            else{
                                bestDirection = myLocation.directionTo(new MapLocation(OPPOSITE_HOME_X,OPPOSITE_HOME_Y));
                            }
                        }

                        if (((Archon) this).buildGardener(bestDirection)) {
                            rc.broadcast(LAST_TURN_BUILD_SOMETHING,turn);
                            gardenersBuild++;
                            rc.broadcast(GARDENERS_BUILD, gardenersBuild);
                            lastbuildGardener = turn;
//                            System.out.println("BUILD!");
                        }

                    }
                }
            }



            if(turn > 80 && lastturnbuildsomething == 0 && soldierreports == 0 && lumberJAckCount == 0 && gardenersBuild > 0 && totalbullets > 150){
                //Seems we got a gardener out, but then couldnt follow up with units
//                System.out.println("DISINTEGRATE 1");
                lumberjack += 5;
                rc.disintegrate();
            }

            if(turn > 600 &&  turn - lastturnbuildsomething > (600 + Archon.our_archon_nr * 20) &&   rc.getRobotCount() > 1 && totalbullets > 150  &&   (gardenersBuild == 0 || (gardenersBuild > 0 && totalGardeners > 0 )  )){
//                System.out.println("DISINTEGRATE 2");

                lumberjack += 500;
                rc.disintegrate(); //Emergency scenario, were stuck somehow while we do have a gardener, might be lategame thing too.
            }

            if( turn > 70 && gardenersBuild == 0 && ownArchonCount > 1 && (Archon.our_archon_nr == 2 ||   Archon.our_archon_nr == 3 )&& rc.getRobotCount() == ownArchonCount){
//                System.out.println("DISINTEGRATE 3");

                if(neutralsInRange6 > 3){
                    gardener += 150;
                    lumberjack += 150;
                }
                rc.disintegrate(); //We cant even build a gardener, and we do have multiple archons, try disintegrating in case theyre blocking each other
            }

        }


        if (tank > 6 && tank > tree && tank > lumberjack && tank > gardener && tank > soldier && tank > scout) {
            floatingBullets = 305;
        }


        rc.broadcast(LUMBERJACK_DESIRE, (int)lumberjack);
        rc.broadcast(SOLDIER_DESIRE, (int)soldier);
        rc.broadcast(SCOUT_DESIRE, (int)scout);
        rc.broadcast(TANK_DESIRE, (int)tank);
        rc.broadcast(TREE_DESIRE, (int)tree);

        rc.broadcast(KEEP_BULLETS_FLOATING, floatingBullets);
        rc.broadcast(VICTORY_POINTS_MODE, vpMode);

       // if(turn < 10 || turn % 2 == 0) {
//            System.out.println("Econ goals: lumberjack: " + lumberjack + " soldier: " + soldier + " tree: " + tree + " scout: " + scout + " gardener: " + gardener + " tank: " + tank);

//            if(stuckMode){
//                System.out.println("STUCK MODE");
//            }
     //   }



        float x1;
        float x2;
        float x3;
        float x4;
        float x5;
        float x6;

        if(ally.name().equals("A")){
            x1 = mapLowestX + 1;
            x2 = mapLowestX + 2;
            x3 = mapLowestX + 3;
            x4 = mapLowestX + 4;
            x5 = mapLowestX + 5;
            x6 = mapLowestX + 6;
        }
        else{
            x1 = mapHighestX - 6;
            x2 = mapHighestX - 5;
            x3 = mapHighestX - 4;
            x4 = mapHighestX - 3;
            x5 = mapHighestX - 2;
            x6 = mapHighestX - 1;
        }
        MapLocation m1 = new MapLocation(x1,mapLowestY);
        MapLocation m2 = new MapLocation(x2,mapLowestY);
        MapLocation m3 = new MapLocation(x3,mapLowestY);
        MapLocation m4 = new MapLocation(x4,mapLowestY);
        MapLocation m5 = new MapLocation(x5,mapLowestY);
        MapLocation m6 = new MapLocation(x6,mapLowestY);

//        System.out.println(m1 + " " + m6);

//        float scale = (mapHighestY - mapLowestY) / 50;

//        rc.setIndicatorLine(m1,  m1.add(Direction.NORTH, gardener * scale), 50, 230,0);
//        rc.setIndicatorLine(m2,  m2.add(Direction.NORTH, lumberjack * scale), 165, 28,238);
//        rc.setIndicatorLine(m3,  m3.add(Direction.NORTH, soldier * scale), 255, 20,0);
//        rc.setIndicatorLine(m4,  m4.add(Direction.NORTH, tank * scale), 120, 5,5);
//        rc.setIndicatorLine(m5,  m5.add(Direction.NORTH, scout * scale), 30, 236,180);
//        rc.setIndicatorLine(m6,  m6.add(Direction.NORTH, tree * scale), 250, 91,14);

    }






    public void desireFarEdges(float intensity, float distance) throws GameActionException{

        float checkDistance = distance + maxMove;
        MapLocation down = new MapLocation(myLocation.x, myLocation.y - checkDistance);
        MapLocation up = new MapLocation(myLocation.x, myLocation.y  + checkDistance);
        MapLocation left = new MapLocation(myLocation.x- checkDistance, myLocation.y);
        MapLocation right = new MapLocation(myLocation.x+checkDistance, myLocation.y);

        if(rc.canSenseLocation(down)){
            if(!rc.onTheMap(down)){
                addVector(down, intensity / 2);
                addDesireZone(down, checkDistance, intensity);
            }
        }
        if(rc.canSenseLocation(up)){
            if(!rc.onTheMap(up)){
                addVector(up, intensity /2);
                addDesireZone(up, checkDistance, intensity);
            }
        }
        if(rc.canSenseLocation(left)){
            if(!rc.onTheMap(left)){
                addVector(left, intensity /2);
                addDesireZone(left, checkDistance, intensity);
            }
        }
        if(rc.canSenseLocation(right)){
            if(!rc.onTheMap(right)){
                addVector(right, intensity /2);
                addDesireZone(right, checkDistance, intensity);
            }
        }


//        System.out.println("myloc: " + myLocation);
//        System.out.println(mapLowestX + " ," +  mapHighestX + " ," + mapLowestY + " ,"  + mapHighestY );
//        System.out.println(zoneLoc);

//        if(myLocation.x - mapLowestX < (distance + maxMove)){
//            addDesireZone(new DesireZone(mapLowestX - 5, mapLowestX + distance, mapLowestY - 5, mapHighestY + 5, intensity));
//        }
//        if(myLocation.x - mapLowestX < (distance)) {
//            addVectorSqrt(myLocation.x - 10, myLocation.y,intensity );
//        }

//         if(mapHighestX - myLocation.x < (distance + maxMove)){
//
//            addDesireZone(new DesireZone(mapHighestX - distance, mapHighestX + 5, mapLowestY - 5, mapHighestY + 5, intensity));
//        }
//        if(mapHighestX - myLocation.x< (distance)) {
//            addVectorSqrt(myLocation.x + 10, myLocation.y, intensity);
//        }

//        if(myLocation.y - mapLowestY < (distance + maxMove)){
//            addDesireZone(new DesireZone(mapLowestX - 5, mapHighestX + 5, mapLowestY  - 5, mapLowestY + distance, intensity));
//        }
//        if(myLocation.y - mapLowestY < (distance)) {
//            addVectorSqrt(myLocation.x, myLocation.y - 10, intensity);
//        }

//        if(mapHighestY - myLocation.y < (distance + maxMove)){
//            addDesireZone(new DesireZone(mapLowestX - 5, mapHighestX + 5, mapHighestY - distance, mapHighestY + 5, intensity));
//        }
//        if(mapHighestY - myLocation.y < (distance)) {
//            addVectorSqrt(myLocation.x, myLocation.y + 10, intensity);
//        }

    }

    public void desireTowardsMiddle(float intensity)  throws GameActionException{
        updateMapSize();
        addVectorSqrt((mapLowestX + mapHighestX)/2, (mapLowestY+mapHighestY) /2, intensity);
    }

    private void calculateDefensiveTargets() throws GameActionException{
        updateMapSize();

        float homex;
        float homey;

        if(turn - rc.readBroadcast(HOME_LASTUPDATE) > 4) {
            homex = myLocation.x;
            homey = myLocation.y;
            rc.broadcast(HOME_X, (int) myLocation.x);
            rc.broadcast(HOME_Y, (int) myLocation.y);
            rc.broadcast(HOME_LASTUPDATE, turn);
            rc.broadcast(OPPOSITE_HOME_X,  (int)(mapLowestX +    (mapHighestX - myLocation.x))  );
            rc.broadcast(OPPOSITE_HOME_Y,  (int)(mapLowestY +    (mapHighestY - myLocation.y))  );
        }
        else{
            homex = rc.readBroadcast(HOME_X);
            homey = rc.readBroadcast(HOME_Y);
        }

        MapLocation home = new MapLocation(homex, homey);

        if(bestDefensiveTargetBot != null){
            rc.broadcast(MAIN_DEFENSIVE_TARGET_X, (int)bestDefensiveTargetBot.getLocation().x);
            rc.broadcast(MAIN_DEFENSIVE_TARGET_Y, (int)bestDefensiveTargetBot.getLocation().y);

            rc.broadcast(IS_REAL_DEF_TARGET, 1);

//            rc.setIndicatorLine(myLocation,bestDefensiveTargetBot.location,255,0,0);
        }
        else{
            //If we don't have an actually visible defendable bot, go defend against a cluster
            MapLocation target = null;

            if(cluster3 != null){
                float dist1 = home.distanceTo(cluster1);
                float dist2 = home.distanceTo(cluster2);
                float dist3 = home.distanceTo(cluster3);

                if(dist1 < dist2 && dist1 < dist3){
                    target = cluster3;
                } else if(dist2 < dist3){
                    target = cluster2;
                } else{
                    target = cluster1;
                }
            }
            else if(cluster2 != null){
                float dist1 = home.distanceTo(cluster1);
                float dist2 = home.distanceTo(cluster2);

                if(dist1 < dist2){
                    target = cluster1;
                } else{
                    target = cluster2;
                }
            }
            else if(cluster1 != null){
                target = cluster1;
            }

            if(target != null){
                //Just pick a cluster as a def target if we can see one
                rc.broadcast(MAIN_DEFENSIVE_TARGET_X, (int) target.x);
                rc.broadcast(MAIN_DEFENSIVE_TARGET_Y, (int) target.y);
                rc.broadcast(IS_REAL_DEF_TARGET, 1);

//                rc.setIndicatorLine(myLocation,target,255,255,0);

            }else {
                //Fallback def positions
                MapLocation middleOfMap = new MapLocation(mapLowestX + ((mapHighestX - mapLowestX) / 2), mapLowestY + ((mapHighestY - mapLowestY) / 2));
                MapLocation defTarget1 = home.add(home.directionTo(middleOfMap), home.distanceTo(middleOfMap) / 1.5f); //Somewhere between home and the middle of the map sounds about right for a defensive position


                MapLocation aggr = new MapLocation(rc.readBroadcast(MAIN_AGRESSIVE_TARGET_X), rc.readBroadcast(MAIN_AGRESSIVE_TARGET_Y));

                if (aggr.x > 0) {
                    MapLocation defTarget2 = home.add(home.directionTo(aggr), home.distanceTo(aggr) / 3f);

                    MapLocation defTarget = defTarget1.add(defTarget1.directionTo(defTarget2), defTarget1.distanceTo(defTarget2) / 1.5f);
                    rc.broadcast(MAIN_DEFENSIVE_TARGET_X, (int) defTarget.x);
                    rc.broadcast(MAIN_DEFENSIVE_TARGET_Y, (int) defTarget.y);

//                    rc.setIndicatorLine(myLocation,defTarget,100,100,100);
                    rc.broadcast(IS_REAL_DEF_TARGET, 0);


                } else {
                    rc.broadcast(MAIN_DEFENSIVE_TARGET_X, (int) defTarget1.x);
                    rc.broadcast(MAIN_DEFENSIVE_TARGET_Y, (int) defTarget1.y);
//                    rc.setIndicatorLine(myLocation,defTarget1,255,0,200);
                    rc.broadcast(IS_REAL_DEF_TARGET, 0);


                }
            }
        }


        if(detectedScout != null){
            rc.broadcast(DETECTED_SCOUT_TURN,turn);
            rc.broadcast(DETECTED_SCOUT_X,(int)detectedScout.location.x);
            rc.broadcast(DETECTED_SCOUT_Y,(int)detectedScout.location.y);
        }

//        //Use sense broadcasting and latest updates etc to figure out a main and a secondary target
//        //Include last-seen variables too
//
//        int idArchon1 = rc.readBroadcast(OUR_ARCHON_1_ID);
//        int idArchon2 = rc.readBroadcast(OUR_ARCHON_2_ID);
//        int idArchon3 = rc.readBroadcast(OUR_ARCHON_3_ID);
//
//        if(idArchon1 > -2){
//            //If their first archon isn't dead yet, set it as main target
//            rc.broadcast(MAIN_DEFENSIVE_TARGET_X, rc.readBroadcast(OUR_ARCHON_1_X));
//            rc.broadcast(MAIN_DEFENSIVE_TARGET_Y, rc.readBroadcast(OUR_ARCHON_1_Y));
//        }
//        else if(idArchon2 > -2){
//            //If their second archon isn't dead yet, set it as main target
//            rc.broadcast(MAIN_DEFENSIVE_TARGET_X, rc.readBroadcast(OUR_ARCHON_2_X));
//            rc.broadcast(MAIN_DEFENSIVE_TARGET_Y, rc.readBroadcast(OUR_ARCHON_2_Y));
//        }
//        else if(idArchon3 > -2){
//            //If their second archon isn't dead yet, set it as main target
//            rc.broadcast(MAIN_DEFENSIVE_TARGET_X, rc.readBroadcast(OUR_ARCHON_3_X));
//            rc.broadcast(MAIN_DEFENSIVE_TARGET_Y, rc.readBroadcast(OUR_ARCHON_3_Y));
//        }
//
        rc.broadcast(DEFENSIVE_TARGETS_LAST_UPDATE,turn);
        //System.out.println("Archon " + our_archon_nr + " updating defend targets");
//
    }



    public void updateMapSize() throws GameActionException{
        if(turn - lastmapUpdate > 3) {
            float map_min_x = ((float)rc.readBroadcast(MAP_MIN_X_BROADCAST))   / 100f;
            float map_max_x = ((float)rc.readBroadcast(MAP_MAX_X_BROADCAST)) / 100f;
            float map_min_y = ((float)rc.readBroadcast(MAP_MIN_Y_BROADCAST)) / 100f;
            float map_max_y = ((float)rc.readBroadcast(MAP_MAX_Y_BROADCAST)) / 100f;



            if(map_min_x > 0.01f && map_min_x < mapLowestX){
                mapLowestX = map_min_x;
            }
            if( map_max_x > mapLowestX && map_max_x > mapHighestX){
                mapHighestX = map_max_x;
            }
            if(map_min_y > 0.01f && map_min_y < mapLowestY){
                mapLowestY = map_min_y;
            }
            if( map_max_y > mapLowestY && map_max_y > mapHighestY){
                mapHighestY = map_max_y;
            }

            if(myLocation.x > mapHighestX){
                mapHighestX = myLocation.x;
            }
            if(myLocation.x < mapLowestX){
                mapLowestX = myLocation.x;
            }
            if(myLocation.y > mapHighestY){
                mapHighestY = myLocation.y;
            }
            if(myLocation.y < mapLowestY){
                mapLowestY = myLocation.y;
            }

            float deltaX = (mapHighestX - mapLowestX);
            float deltaY = (mapHighestY - mapLowestY);
            float delta = deltaX + deltaY ;
            if(deltaX < 30 && deltaY < 30){ //Mapsthis small don't exist, but if at the start, their archon spawns very close to ours, this will happen
                mapSizeType = TINY;
            }
            else if (delta < 70) {
                mapSizeType = SMALL;
            } else if (delta < 100) {
                mapSizeType = MEDIUM;
            } else if (delta < 150){
                mapSizeType = LARGE;
            } else{
                mapSizeType = HUGE;
            }


//            MapLocation m1 = new MapLocation(mapLowestX,mapLowestY);
//            MapLocation m2 = new MapLocation(mapHighestX,mapLowestY);
//            MapLocation m3 = new MapLocation(mapLowestX,mapHighestY);
//            MapLocation m4 = new MapLocation(mapHighestX,mapHighestY);
//
//            rc.setIndicatorLine(m1,m2, 255,0,0);
//            rc.setIndicatorLine(m1,m3, 255,0,0);
//            rc.setIndicatorLine(m2,m4, 255,0,0);
//            rc.setIndicatorLine(m3,m4, 255,0,0);
//
//            MapLocation m5 = new MapLocation(map_min_x,map_min_y);
//            MapLocation m6 = new MapLocation(map_max_x,map_min_y);
//            MapLocation m7 = new MapLocation(map_min_x,map_max_y);
//            MapLocation m8 = new MapLocation(map_max_x,map_max_y);
//
//            rc.setIndicatorLine(m5,m6, 0,0,0);
//            rc.setIndicatorLine(m5,m7, 0,0,0);
//            rc.setIndicatorLine(m6,m8, 0,0,0);
//            rc.setIndicatorLine(m7,m8, 0,0,0);
        }


    }


    public MapLocation getRandomGoal() throws GameActionException{
        if(randomGoal == null ||  turn - lastRandomChange  > 10){
            updateMapSize();

            float deltax = mapHighestX - mapLowestX;
            float deltay = mapHighestY - mapLowestY;

            if(deltax == 0) deltax = 0.01f;
            if(deltay == 0) deltay = 0.01f;
            float x = mapLowestX + ((turn + rc.getID()) %  (deltax) );
            float y = mapLowestY + ((turn + (rc.getID() * 7)) % (deltay ));
            randomGoal = new MapLocation(x,y);
            lastRandomChange = turn;

            //rc.setIndicatorLine(myLocation,randomGoal,0,0,255);
            //System.out.println("random goal:" +  randomGoal.x + "," + randomGoal.y);

        }
        return randomGoal;
    }



    public void doVictoryPointLogic() throws Exception{
        //Also some logic in econ management of commander
        float remainingBullets = rc.readBroadcast(KEEP_BULLETS_FLOATING);
        float vicPointCost = rc.getVictoryPointCost();
        if( (1000 - rc.getTeamVictoryPoints()) * vicPointCost < rc.getTeamBullets() ){
            rc.donate(rc.getTeamBullets());
            //System.out.println("VICTORY!");
        }
        if(remainingBullets > 600 && rc.getTeamVictoryPoints() < 1){
            rc.donate(0.01f + vicPointCost);
        }
        if(turn >400 && rc.getTeamBullets() > 1500){
            rc.donate(0.001f + vicPointCost);
        }
        if(turnsLeft <= 2){
            rc.donate(rc.getTeamBullets());
            //System.out.println("DONATE REMAINDER");
        }
        if(!(this instanceof  Archon) && !(this instanceof Gardener)) {
            if (turn > 800 && rc.getRobotCount() <= 1) { //Last ditch effort, we may be able to win just by dodging the enemy forever
                if (rc.getTeamBullets() > vicPointCost) {
                    rc.donate(0.01f + vicPointCost);
                }
            }
        }
    }


    //Let's just assume this spot isn't exactly where we're at, to save on the divide by zero check

    public void addVector(float x, float y, float force){
//        System.out.println("adding vector: " + x + "," + y +  "  : " + force);

        if(force == 0) return;
        double deltaX = x - myLocation.x;
        double deltaY = y - myLocation.y;

        double xmult = 1;
        double ymult = 1;

        if(deltaX < 0) {
            xmult = -1;
            deltaX *= -1;
        }

        if(deltaY < 0) {
            ymult = -1;
            deltaY *= -1;
        }
        double factor =  (force/2)  /  (deltaX + deltaY);

        moveVectorX +=  factor * deltaX*xmult;
        moveVectorY += factor * deltaY*ymult;
    }

    public void addSidewaysVector(MapLocation m, float force){
        if(directionLeaning == 0){
            addVector(myLocation.add(myLocation.directionTo(m).rotateLeftDegrees(80),maxMove),force);
        }
        else{
            addVector(myLocation.add(myLocation.directionTo(m).rotateRightDegrees(80),maxMove),force);
        }
    }


    public void addSpecialMapLocation(MapLocation m, float force){
//        System.out.println("added spot: " + m + "  force: " + force);
        specialMapLocations[specialMapLocationsCount] = m;
        extraPoints[specialMapLocationsCount] = force;
        specialMapLocationsCount++;

    }

    public void addVector(MapLocation m, float force){
//        System.out.println("adding vector: " + m +  "  : " + force);
        if(force == 0) return;
        double deltaX = m.x - myLocation.x;
        double deltaY = m.y - myLocation.y;

        double xmult = 1;
        double ymult = 1;

        if(deltaX < 0) {
            xmult = -1;
            deltaX *= -1;

        }

        if(deltaY < 0) {
            ymult = -1;
            deltaY *= -1;

        }
        double factor =  (force/2)  /  (deltaX + deltaY);

        moveVectorX +=  factor * deltaX *xmult;
        moveVectorY += factor * deltaY*ymult;
    }


    public void updateWoodCuttingTargets() throws GameActionException{
//        System.out.println("trying woodcutting targets");
        if(woodcutJob1 != null || woodcutJob2 != null || woodcutJob3 != null || woodcutJob4 != null) {
//            System.out.println("have targets");

            int priority1 = rc.readBroadcast(WOODCUT_JOB_1_PRIORITY);
            int priority2 = rc.readBroadcast(WOODCUT_JOB_2_PRIORITY);
            int priority3 = rc.readBroadcast(WOODCUT_JOB_3_PRIORITY);
            int priority4 = rc.readBroadcast(WOODCUT_JOB_4_PRIORITY);

            int turns1  = rc.readBroadcast(WOODCUT_JOB_1_TURN);
            int turns2  = rc.readBroadcast(WOODCUT_JOB_2_TURN);
            int turns3  = rc.readBroadcast(WOODCUT_JOB_3_TURN);
            int turns4  = rc.readBroadcast(WOODCUT_JOB_4_TURN);

            float wc1x  = rc.readBroadcast(WOODCUT_JOB_1_X) / 100f;
            float wc2x  = rc.readBroadcast(WOODCUT_JOB_2_X) / 100f;
            float wc3x  = rc.readBroadcast(WOODCUT_JOB_3_X) / 100f;
            float wc4x  = rc.readBroadcast(WOODCUT_JOB_4_X) /100f;

            float wc1y  = rc.readBroadcast(WOODCUT_JOB_1_Y) / 100f;
            float wc2y  = rc.readBroadcast(WOODCUT_JOB_2_Y) /100f;
            float wc3y  = rc.readBroadcast(WOODCUT_JOB_3_Y) / 100f;
            float wc4y  = rc.readBroadcast(WOODCUT_JOB_4_Y) / 100f;

            boolean changes = false;




            if (woodcutJob1 != null && (priority1 < prioritywoodcutJob1 || turn -  turns1 > 15)) {
//                System.out.print("set 1 with prio " + prioritywoodcutJob1 + " used to be: " +  priority1   +  " at: " + woodcutJob1);



                priority1 = prioritywoodcutJob1;
                wc1x = woodcutJob1.x;
                wc1y = woodcutJob1.y;
                turns1 = turn;
                changes = true;
            }
//            else{
//                System.out.print("cant set because prio:  " + prioritywoodcutJob1 + " used to be: " +  priority1 );

//            }

            if (woodcutJob2 != null && (priority2 < prioritywoodcutJob2 || turn -  turns2 > 15)) {
                priority2 = prioritywoodcutJob2;
                wc2x = woodcutJob2.x;
                wc2y = woodcutJob2.y;
                turns2 = turn;
              //  System.out.print("set 2 to " + priority2);
                changes = true;

            }

            if (woodcutJob3 != null && (priority3 < prioritywoodcutJob3 || turn -  turns3 > 15)) {
                priority3 = prioritywoodcutJob3;
                wc3x = woodcutJob3.x;
                wc3y = woodcutJob3.y;
                turns3 = turn;
                changes = true;

                //  System.out.print("set 3 to " + priority3);

            }
            if (woodcutJob4 != null && (priority4 < prioritywoodcutJob4 || turn -  turns4 > 15)) {
                priority4 = prioritywoodcutJob4;
                wc4x = woodcutJob4.x;
                wc4y = woodcutJob4.y;
                turns4 = turn;
                changes = true;

                // System.out.print("set 4 to " + priority4);

            }


//            rc.setIndicatorDot(new MapLocation(wc1x,wc1y), 0, 255 ,0);
//            rc.setIndicatorDot(new MapLocation(wc2x,wc2y), 0, 200 ,0);
//            rc.setIndicatorDot(new MapLocation(wc3x,wc3y), 0, 150 ,0);
//            rc.setIndicatorDot(new MapLocation(wc4x,wc4y), 0, 100 ,0);

            if(changes) {
                rc.broadcast(WOODCUT_JOB_1_PRIORITY, priority1);
                rc.broadcast(WOODCUT_JOB_2_PRIORITY, priority2);
                rc.broadcast(WOODCUT_JOB_3_PRIORITY, priority3);
                rc.broadcast(WOODCUT_JOB_4_PRIORITY, priority4);

                rc.broadcast(WOODCUT_JOB_1_TURN, turns1);
                rc.broadcast(WOODCUT_JOB_2_TURN, turns2);
                rc.broadcast(WOODCUT_JOB_3_TURN, turns3);
                rc.broadcast(WOODCUT_JOB_4_TURN, turns4);

                rc.broadcast(WOODCUT_JOB_1_X, (int)((wc1x * 100)+0.5f));
                rc.broadcast(WOODCUT_JOB_2_X,  (int)((wc2x * 100)+0.5f));
                rc.broadcast(WOODCUT_JOB_3_X,  (int)((wc3x * 100)+0.5f));
                rc.broadcast(WOODCUT_JOB_4_X,  (int)((wc4x * 100)+0.5f));

                rc.broadcast(WOODCUT_JOB_1_Y,  (int)((wc1y * 100)+0.5f));
                rc.broadcast(WOODCUT_JOB_2_Y,  (int)((wc2y * 100)+0.5f));
                rc.broadcast(WOODCUT_JOB_3_Y,  (int)((wc3y * 100)+0.5f));
                rc.broadcast(WOODCUT_JOB_4_Y,  (int)((wc4y * 100)+0.5f));
            }
        }
    }



    //The sqrt vectors give nicer results, but are much heavier so shouldn't be used in big loops
    public void addVectorSqrt(float x, float y, float force){
        addVector(x,y,force);

    }
//        System.out.println("adding vectorsqrt: " + x + "," + y +  "  : " + force);
//
//        if(force == 0) return;
//        double deltaX = x - myLocation.x;
//        double deltaY = y - myLocation.y;
//
//        double xmult = 1;
//        double ymult = 1;
//
//        if(deltaX < 0) {
//            xmult = -1;
//            deltaX = Math.sqrt(deltaX * -1);
//        }
//        else{
//            deltaX = Math.sqrt(deltaX);
//        }
//
//        if(deltaY < 0) {
//            ymult = -1;
//            deltaY = Math.sqrt(deltaY * -1);
//        }
//        else{
//            deltaY = Math.sqrt(deltaY);
//        }
//        double factor =  force  /  (deltaX + deltaY);
//
//        moveVectorX +=  factor * deltaX *xmult;
//        moveVectorY += factor * deltaY*ymult;
  //  }

    public void addVectorSqrt(MapLocation m, float force){
        addVector(m,force);
    }
//        System.out.println("adding vector: " + m +  "  : " + force);
//
//        if(force == 0) return;
//
//        double deltaX = m.x - myLocation.x;
//        double deltaY = m.y - myLocation.y;
//
//        double xmult = 1;
//        double ymult = 1;
//
//        if(deltaX < 0) {
//            xmult = -1;
//            deltaX = Math.sqrt(deltaX * -1);
//        }
//        else{
//            deltaX = Math.sqrt(deltaX);
//        }
//        if(deltaY < 0) {
//            ymult = -1;
//            deltaY = Math.sqrt(deltaY * -1);
//        }
//        else{
//            deltaY = Math.sqrt(deltaY);
//        }
//        double factor =  force  /  (deltaX + deltaY);
//
//        moveVectorX +=  factor * deltaX *xmult;
//        moveVectorY += factor * deltaY*ymult;
  //  }


//    public void addDesireZone(DesireZone d) {
//        if (zoneLoc > 49) {
//            return;
//        } else {
//            if (!(d.desire < 0.5f && d.desire > -0.5f)) { //Too weak to care
//                zones[zoneLoc] = d;
//                zoneLoc++;
//            }
//        }
//    }

    public void addLineZone(MapLocation m, Direction dir, float length, float width, float force){
        if (zoneLoc > 49) {
            return;
        } else {
            if (!(force < 0.5f && force > -0.5f)) { //Too weak to care
                zones[zoneLoc] = new DesireZone(m, dir, length, width, force);
                zoneLoc++;
            }
        }
    }
    public void addCircularDesireZone(MapLocation m, float size, float force){
//        System.out.println("circ zone: " + m + " size: " + size + "  f:" + force);
        if (zoneLoc > 49) {
            return;
        } else {
            if (!(force < 0.5f && force > -0.5f)) { //Too weak to care
                zones[zoneLoc] = new DesireZone(m,size,force,true);
                zoneLoc++;
            }
        }
    }
    public void addDesireZone(MapLocation m, float size, float force){
//        System.out.println("circ zone: " + m + " size: " + size + "  f:" + force);

        if (zoneLoc > 49) {
            return;
        } else {
            if (!(force < 0.5f && force > -0.5f)) { //Too weak to care
                zones[zoneLoc] = new DesireZone(m,size,force);
                zoneLoc++;
            }
        }
    }

    public void addDesireZone(float left, float right, float top, float bot, float force){
        if (zoneLoc > 49) {
            return;
        } else {
            if (!(force < 0.5f && force > -0.5f)) { //Too weak to care
                zones[zoneLoc] = new DesireZone(left,right,top,bot,force);
                zoneLoc++;
            }
        }
    }







    public void broadcastMapSize() throws GameActionException{

        float map_min_x = ((float)rc.readBroadcast(MAP_MIN_X_BROADCAST))   / 100f;
        float map_max_x = ((float)rc.readBroadcast(MAP_MAX_X_BROADCAST)) / 100f;
        float map_min_y = ((float)rc.readBroadcast(MAP_MIN_Y_BROADCAST)) / 100f;
        float map_max_y = ((float)rc.readBroadcast(MAP_MAX_Y_BROADCAST)) / 100f;

        if(map_min_x > 0.01f && map_min_x < mapLowestX){
            mapLowestX = map_min_x;
        }
        if( map_max_x > mapLowestX && map_max_x > mapHighestX){
            mapHighestX = map_max_x;
        }
        if(map_min_y > 0.01f && map_min_y < mapLowestY){
            mapLowestY = map_min_y;
        }
        if( map_max_y > mapLowestY && map_max_y > mapHighestY){
            mapHighestY = map_max_y;
        }

        if(myLocation.x > mapHighestX){
            mapHighestX = myLocation.x;
        }
        if(myLocation.x < mapLowestX){
            mapLowestX = myLocation.x;
        }
        if(myLocation.y > mapHighestY){
            mapHighestY = myLocation.y;
        }
        if(myLocation.y < mapLowestY){
            mapLowestY = myLocation.y;
        }

        if(mapLowestX != map_min_x){
            rc.broadcast(MAP_MIN_X_BROADCAST, (int)(mapLowestX * 100f));
        }
        if(mapHighestX != map_max_x){
            rc.broadcast(MAP_MAX_X_BROADCAST, (int)(mapHighestX*100f));
        }
        if(mapLowestY != map_min_y){
            rc.broadcast(MAP_MIN_Y_BROADCAST, (int)(mapLowestY*100f));
        }
        if(mapHighestY != map_max_y){
            rc.broadcast(MAP_MAX_Y_BROADCAST, (int)(mapHighestY*100f));
        }

        float deltaX = (mapHighestX - mapLowestX);
        float deltaY = (mapHighestY - mapLowestY);
        float delta = deltaX + deltaY ;
        if(deltaX < 30 && deltaY < 30){ //Mapsthis small don't exist, but if at the start, their archon spawns very close to ours, this will happen
            mapSizeType = TINY;
        }
        else if (delta < 70) {
            mapSizeType = SMALL;
        } else if (delta < 100) {
            mapSizeType = MEDIUM;
        } else if (delta < 150){
            mapSizeType = LARGE;
        } else{
            mapSizeType = HUGE;
        }

       // System.out.print("broadcast map size: " + mapLowestX + "," + mapHighestX + "," + mapLowestY + "," + mapHighestY  );
    }

    public void broadcastEnemyArchon(int id, float x, float y) throws Exception{
        int archonnr = -1;
        boolean shouldbroadcastid = false;


        //Weird code structure is to have as few read/send broadcasts as possible

        if(enemyarchonid1 == id){
            archonnr = 1;
        }
        else if(enemyarchonid2 == id){
            archonnr = 2;
        }
        else if(enemyarchonid3 == id){
            archonnr = 3;
        }
        else {
            int nr1 = rc.readBroadcast(THEIR_ARCHON_1_ID);
            if (nr1 == id) {
                archonnr = 1;
                enemyarchonid1 = id;
            } else {

                int nr2 = 0;
                int nr3 = 0;

                if(enemyarchonid2 != -2) {
                    nr2 = rc.readBroadcast(THEIR_ARCHON_2_ID);

                    if (nr2 == id) {
                        archonnr = 2;
                        enemyarchonid2 = id;
                    } else if( nr2 == -2){
                        enemyarchonid2 = -2;
                    }
                }
                if(archonnr == -1){
                    if(enemyarchonid3 != -2) {
                        nr3 = rc.readBroadcast(THEIR_ARCHON_3_ID);
                        if (nr3 == id) {
                            archonnr = 3;
                            enemyarchonid3 = id;
                        }else if( nr3 == -2){
                            enemyarchonid3 = -2;
                        }
                    }
                    if(archonnr == -1){
                        shouldbroadcastid = true;
                        if (nr1 == -1) {
                            archonnr = 1;
                            enemyarchonid1 = id;
                        } else if (nr2 == -1) {
                            archonnr = 2;
                            enemyarchonid2 = id;
                        } else {
                            archonnr = 3;
                            enemyarchonid3 = id;

                        }
                    }

                }
            }
        }


        if(archonnr == 1 ){
            if(rc.readBroadcast(THEIR_ARCHON_1_LASTSEEN) != turn) {
                rc.broadcast(THEIR_ARCHON_1_X, (int)((x*10f) + 0.5f) ); //Don't bother reading x/y out, we probably save more bytecode overall by assuming it's different
                rc.broadcast(THEIR_ARCHON_1_Y,  (int)((y*10f) + 0.5f));
                rc.broadcast(THEIR_ARCHON_1_LASTSEEN, turn);
                //   System.out.print("found enemy archon at " + x + "," + y);

                if (shouldbroadcastid) {
                    rc.broadcast(THEIR_ARCHON_1_ID, id);
                    //     System.out.print("broadcasting id");

                }
            }

            if(this instanceof Soldier){
                if(rc.readBroadcast(THEIR_ARCHON_1_LASTTAGGED) != turn){
                    rc.broadcast(THEIR_ARCHON_1_LASTTAGGED,turn);
                }
            }

        } else if(archonnr == 2){
            if(rc.readBroadcast(THEIR_ARCHON_2_LASTSEEN) != turn) {

                rc.broadcast(THEIR_ARCHON_2_X, (int)((x*10f) + 0.5f) ); //Don't bother reading x/y out, we probably save more bytecode overall by assuming it's different
                rc.broadcast(THEIR_ARCHON_2_Y,  (int)((y*10f) + 0.5f));
                rc.broadcast(THEIR_ARCHON_2_LASTSEEN, turn);

                if (shouldbroadcastid) {
                    rc.broadcast(THEIR_ARCHON_2_ID, id);
                    //     System.out.print("broadcasting id");

                }
            }
            if(this instanceof Soldier){
                if(rc.readBroadcast(THEIR_ARCHON_2_LASTTAGGED) != turn){
                    rc.broadcast(THEIR_ARCHON_2_LASTTAGGED,turn);
                }
            }
        } else if(archonnr == 3){
            if(rc.readBroadcast(THEIR_ARCHON_3_LASTSEEN) != turn) {

                rc.broadcast(THEIR_ARCHON_3_X, (int)((x*10f) + 0.5f) ); //Don't bother reading x/y out, we probably save more bytecode overall by assuming it's different
                rc.broadcast(THEIR_ARCHON_3_Y,  (int)((y*10f) + 0.5f));
                rc.broadcast(THEIR_ARCHON_3_LASTSEEN, turn);

                if (shouldbroadcastid) {
                    rc.broadcast(THEIR_ARCHON_3_ID, id);
                    //       System.out.print("broadcasting id");

                }
            }
            if(this instanceof Soldier){
                if(rc.readBroadcast(THEIR_ARCHON_3_LASTTAGGED) != turn){
                    rc.broadcast(THEIR_ARCHON_3_LASTTAGGED,turn);
                }
            }
        }

    }


    
    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    



    public static final int OUR_ARCHON_1_X = 0;
    public static final int OUR_ARCHON_1_Y = 1;
    public static final int OUR_ARCHON_1_ID = 2; // -1 = unknown, -2 = doesnt exist
    public static final int OUR_ARCHON_1_LASTSEEN = 3;


    public static final int OUR_ARCHON_2_X = 4;
    public static final int OUR_ARCHON_2_Y = 5;
    public static final int OUR_ARCHON_2_ID = 6;// -1 = unknown, -2 = doesnt exist
    public static final int OUR_ARCHON_2_LASTSEEN = 7;

    public static final int OUR_ARCHON_3_X = 8;
    public static final int OUR_ARCHON_3_Y = 9;
    public static final int OUR_ARCHON_3_ID = 10;// -1 = unknown, -2 = doesnt exist
    public static final int OUR_ARCHON_3_LASTSEEN = 11;


    public static final int THEIR_ARCHON_1_X = 12;
    public static final int THEIR_ARCHON_1_Y = 13;
    public static final int THEIR_ARCHON_1_ID = 14;// -1 = unknown, -2 = doesnt exist
    public static final int THEIR_ARCHON_1_LASTSEEN = 15;


    public static final int THEIR_ARCHON_2_X = 16;
    public static final int THEIR_ARCHON_2_Y = 17;
    public static final int THEIR_ARCHON_2_ID = 18;// -1 = unknown, -2 = doesnt exist
    public static final int THEIR_ARCHON_2_LASTSEEN = 19;

    public static final int THEIR_ARCHON_3_X = 20;
    public static final int THEIR_ARCHON_3_Y = 21;
    public static final int THEIR_ARCHON_3_ID = 22;// -1 = unknown, -2 = doesnt exist
    public static final int THEIR_ARCHON_3_LASTSEEN = 23;


    public static final int AGRESSIVE_TARGETS_LAST_UPDATE = 24;
    public static final int DEFENSIVE_TARGETS_LAST_UPDATE = 25;

    public static final int MAIN_AGRESSIVE_TARGET_X = 26;  //negative if no target
    public static final int MAIN_AGRESSIVE_TARGET_Y = 27;
    public static final int SECONDARY_AGRESSIVE_TARGET_X = 28;  //negative if no target
    public static final int SECONDARY_AGRESSIVE_TARGET_Y = 29;


    public static final int MAIN_DEFENSIVE_TARGET_X = 30;  //negative if no target
    public static final int MAIN_DEFENSIVE_TARGET_Y = 31;

    public static final int INITIATED = 32;

    public static final int MAP_MIN_X_BROADCAST = 33;
    public static final int MAP_MAX_X_BROADCAST = 34;
    public static final int MAP_MIN_Y_BROADCAST = 35;
    public static final int MAP_MAX_Y_BROADCAST = 36;



    public static final int OUR_SCOUT_1_X = 37;
    public static final int OUR_SCOUT_1_Y = 38;
    public static final int OUR_SCOUT_1_ID = 39; // -1 = unknown, -2 = dead/doesnt exist
    public static final int OUR_SCOUT_1_LASTSEEN = 40;


    public static final int OUR_SCOUT_2_X = 41;
    public static final int OUR_SCOUT_2_Y = 42;
    public static final int OUR_SCOUT_2_ID = 43;// -1 = unknown, -2 = dead/doesnt exist
    public static final int OUR_SCOUT_2_LASTSEEN = 44;

    public static final int OUR_SCOUT_3_X = 45;
    public static final int OUR_SCOUT_3_Y = 46;
    public static final int OUR_SCOUT_3_ID = 47;// -1 = unknown, -2 = dead/doesnt exist
    public static final int OUR_SCOUT_3_LASTSEEN = 48;


    public static final int ECON_GOALS_LAST_UPDATE = 49;
    public static final int LUMBERJACK_DESIRE = 50;
    public static final int SOLDIER_DESIRE = 51;
    public static final int SCOUT_DESIRE = 52;
    public static final int TANK_DESIRE = 53;
    public static final int TREE_DESIRE = 54;

    public static final int GARDENERS_BUILD = 55;
    public static final int TREES_BUILD = 56;
    public static final int SOLDIERS_BUILD = 57;
    public static final int TANKS_BUILD = 58;
    public static final int SCOUTS_BUILD = 59;
    public static final int LUMBERJACKS_BUILD = 60;

    public static final int KEEP_BULLETS_FLOATING = 61;


    public static final int SOLDIERS_REPORTING_IN = 62;
    public static final int SOLDIERS_REPORTING_IN_TURN = 63;
    public static final int SOLDIERS_REPORTING_IN_STUCK = 64;

    public static final int AGRESSION_LEVEL = 65;
    public static final int DEFENSE_LEVEL = 66;
    public static final int LAST_UPDATED_GRAND_STRAT = 67;

    public static final int LUMBERJACKS_REPORTING_IN = 68;
    public static final int LUMBERJACKS_REPORTING_IN_TURN = 69;

    public static final int GARDENERS_REPORTING_IN = 70;
    public static final int GARDENERS_REPORTING_IN_TURN = 71;

    public static final int WOODCUTTING_FOCUS = 72;
//    public static final int WOODCUT_X = 73;
//    public static final int WOODCUT_Y = 74;

//    public static final int RADIOSILENCE_TURN = 75;

    public static final int MARKED_TARGET_ID = 76;
    public static final int MARKED_TARGET_TURN = 77;


    public static final int CONFIRMED_ENEMY_TURN = 78;
    public static final int CONFIRMED_ENEMY_X = 79;
    public static final int CONFIRMED_ENEMY_Y = 80;

    public static final int CONFIRMED_ENEMY_TREE_TURN = 81;
    public static final int CONFIRMED_ENEMY_TREE_X = 82;
    public static final int CONFIRMED_ENEMY_TREE_Y = 83;

    public static final int SCOUTS_REPORTING_IN = 84;
    public static final int SCOUTS_REPORTING_IN_TURN = 85;

    public static final int HOME_X = 86;
    public static final int HOME_Y = 87;
    public static final int HOME_LASTUPDATE = 88;
    public static final int OPPOSITE_HOME_X = 89;
    public static final int OPPOSITE_HOME_Y = 90;


    public static final int UNIT_PRODUCER_GARDENER = 91;
    public static final int UNIT_PRODUCER_GARDENER_DISTANCE = 92;


    public static final int WOODCUT_JOB_1_X = 137;
    public static final int WOODCUT_JOB_1_Y = 93;
    public static final int WOODCUT_JOB_1_PRIORITY = 94;
    public static final int WOODCUT_JOB_1_TURN = 95;

    public static final int WOODCUT_JOB_2_X = 96;
    public static final int WOODCUT_JOB_2_Y = 97;
    public static final int WOODCUT_JOB_2_PRIORITY = 98;
    public static final int WOODCUT_JOB_2_TURN = 99;

    public static final int WOODCUT_JOB_3_X = 100;
    public static final int WOODCUT_JOB_3_Y = 101;
    public static final int WOODCUT_JOB_3_PRIORITY = 102;
    public static final int WOODCUT_JOB_3_TURN = 103;

    public static final int WOODCUT_JOB_4_X = 104;
    public static final int WOODCUT_JOB_4_Y = 105;
    public static final int WOODCUT_JOB_4_PRIORITY = 106;
    public static final int WOODCUT_JOB_4_TURN = 107;

    public static final int DETECTED_SCOUT_X = 108;
    public static final int DETECTED_SCOUT_Y = 109;
    public static final int DETECTED_SCOUT_TURN = 110;
    public static final int LAST_TURN_BUILD_SOMETHING = 111;


    public static final int CONFIRMED_ENEMY_TANK_TURN = 112;
    public static final int CONFIRMED_ENEMY_TANK_X = 113;
    public static final int CONFIRMED_ENEMY_TANK_Y = 114;

    public static final int TANKS_REPORTING_IN = 115;
    public static final int TANKS_REPORTING_IN_TURN = 116;
    public static final int GARDENERS_REPORTING_IN_TOTAL = 117;



    public static final int GUARD_JOB_TURN = 118;
    public static final int GUARD_JOB_X = 119;
    public static final int GUARD_JOB_Y = 120;
    public static final int VICTORY_POINTS_MODE = 121;


    public static final int CLUSTER1_TURN = 122;
    public static final int CLUSTER1_X = 123;
    public static final int CLUSTER1_Y = 124;

    public static final int CLUSTER2_TURN = 125;
    public static final int CLUSTER2_X = 126;
    public static final int CLUSTER2_Y = 127;

    public static final int CLUSTER3_TURN = 128;
    public static final int CLUSTER3_X = 129;
    public static final int CLUSTER3_Y = 130;


    public static final int CONFIRMED_ENEMY_GARDENER_TURN = 131;
    public static final int CONFIRMED_ENEMY_GARDENER_X = 132;
    public static final int CONFIRMED_ENEMY_GARDENER_Y = 133;


    public static final int THEIR_ARCHON_1_LASTTAGGED = 134;
    public static final int THEIR_ARCHON_2_LASTTAGGED = 135;
    public static final int THEIR_ARCHON_3_LASTTAGGED = 136;
        //skip 137, mistakes were made

    public static final int MARKED_TARGET_X = 138;
    public static final int MARKED_TARGET_Y = 139;
    public static final int SCOUTS_REPORTING_IN_TOTAL = 140;


    public static final int CLUSTER1_SIZE = 141;
    public static final int CLUSTER2_SIZE = 142;
    public static final int CLUSTER3_SIZE = 143;


    public static final int OUR_ARCHON_CAPTURED = 144;
    public static final int IS_REAL_DEF_TARGET = 145;


}



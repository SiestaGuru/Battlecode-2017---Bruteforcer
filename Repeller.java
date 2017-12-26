package bruteforcer;

import battlecode.common.MapLocation;

/**
 * Created by Hermen on 10/1/2017.
 */
public class Repeller {
    public MapLocation location;
    public float strength;
    public float maxDistance;
    public float minDistance;

    public boolean noGradient;

    public Repeller(MapLocation loc, float str, float maxDistance, float minDistance){
        this.location = loc;
        this.strength = str;
        this.maxDistance = maxDistance;
        this.minDistance = minDistance;
        noGradient = false;
    }

    public Repeller(MapLocation loc, float str, float maxDistance, float minDistance, boolean noGradient){
        this.location = loc;
        this.strength = str;
        this.maxDistance = maxDistance;
        this.minDistance = minDistance;
        this.noGradient = noGradient;
    }
}

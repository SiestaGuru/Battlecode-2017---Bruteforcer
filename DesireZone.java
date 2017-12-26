package bruteforcer;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

/**
 * Created by Hermen on 12/1/2017.
 */
public class DesireZone {

    public float left;
    public float right;
    public float top;
    public float bot;

    public float desire;


    public boolean line = false;
    public boolean circular = false;



    public boolean isVertical = false;
    //These points will fall along the line. Ordered so that the lowest comes first (x if horizontal, y if vertical)
    public MapLocation point1;
    public MapLocation point2;
    public MapLocation point3;
    public MapLocation center;
    public MapLocation point4;
    public MapLocation point5;
    public MapLocation point6;

    public float radius;


    public DesireZone(MapLocation map, float size, float desire){
        left = map.x - size;
        right = map.x + size;
        top = map.y - size;
        bot = map.y + size;
        this.desire = desire * 2;
    }


    //Circular zones are more accurate, but have a performance impact, allowing less movement slots to be checked
    public DesireZone(MapLocation map, float size, float desire, boolean circular){
        left = map.x - size;
        right = map.x + size;
        top = map.y - size;
        bot = map.y + size;
        this.desire = desire * 2;

        if(circular){
            center = map;
            radius = size;
            this.circular = true;
        }
    }

    public DesireZone(float l, float r, float t, float b, float desire){
        this.left = l;
        this.right = r;
        this.top = t;
        this.bot = b;
        this.desire = desire * 2;
    }


    //Currently unused
    public DesireZone(MapLocation l1, Direction d, float length, float width, float force){
        MapLocation l2 = l1.add(d,length);
        float deltax = l2.x - l1.x;
        float deltay = l2.y - l1.y;
        if(deltax < 0) deltax*=-1;
        if(deltay < 0) deltay*=-1;
        if(deltay > deltax){
            isVertical = true;
        }
        float length6 = length / 6;
        if(l1.x < l2.x){
            this.left = l1.x - width;
            this.right = l2.x + width;
            if(!isVertical) {
                point1 = l1;
                point2 = l1.add(d, length6);
                point3 = l1.add(d, length6 * 2);
                center = l1.add(d, length6 * 3);
                point4 = l1.add(d, length6 * 4);
                point5 = l1.add(d, length6 * 5);
                point6 = l2;
            }
        }
        else{
            this.left = l2.x - width;
            this.right = l1.x + width;
            if(!isVertical) {
                point1 = l2;
                point2 = l1.add(d, length6 * 5);
                point3 = l1.add(d, length6 * 4);
                center = l1.add(d, length6 * 3);
                point4 = l1.add(d, length6 * 2);
                point5 = l1.add(d, length6);
                point6 = l1;
            }
        }
        if(l1.y < l2.y){
            this.top = l1.y - width;
            this.bot = l2.y + width;
            if(isVertical){
                point1 = l1;
                point2 = l1.add(d, length6);
                point3 = l1.add(d, length6 * 2);
                center = l1.add(d, length6 * 3);
                point4 = l1.add(d, length6 * 4);
                point5 = l1.add(d, length6 * 5);
                point6 = l2;
            }
        }
        else{
            this.top = l2.y - width;
            this.bot = l1.y + width;
            if(isVertical){
                point1 = l2;
                point2 = l1.add(d, length6 * 5);
                point3 = l1.add(d, length6 * 4);
                center = l1.add(d, length6 * 3);
                point4 = l1.add(d, length6 * 2);
                point5 = l1.add(d, length6);
                point6 = l1;
            }
        }
        this.desire = force;
    }






}

/*
 * MDSPointImpl.java
 *
 * Created on March 13, 2006, 9:03 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.echonest.demo.segviz;


/**
 *
 * @author plamere
 */
public abstract class MDSPointImpl implements MDSPoint {
    private final static double MAX_VEL = 1.;
    private double[] pos;
    private double[] vel;
    private double[] curPos;
    private double [] errSum;
    
    private MDSPoint[] neighbors;
    private double[] neighborDistances;
    private int curNeighborSize = 0;
    private int furthestNeighborIndex = 0;
    private boolean annotateMe = false;
    private boolean selected = false;
    
    private double[] vScratch;
    private MDSLayout mdsLayout;
    
    double kP = .02;
    double kI = .001;
    static int pointCount;
    
    
    public MDSPointImpl(MDSLayout layout, int dim) {
        this.mdsLayout = layout;
        pos = new double[dim];  // the desired position
        curPos = new double[dim] ; // the current position;
        vel = new double[dim];
        vScratch = new double[dim];
        errSum = new double[dim];
        
        for (int i = 0; i < pos.length; i++) {
            pos[i] = Math.random();
//            pos[i] = 0.0;
            curPos[i] = -1;
            vel[i] = 0.0;
        }
        
        if (dim >= 3) {
            gotoStartPos();
        }
        
        int neighborListSize = mdsLayout.getVMax();
        if (neighborListSize > 0) {
            neighbors = new MDSPoint[neighborListSize];
            neighborDistances = new double[neighborListSize];
        }
    }
    
    public abstract double getDistance(MDSPoint other);
    
    
    MDSLayout getLayout() {
        return mdsLayout;
    }
    
    void gotoStartPos() {
        curPos[0] = Math.sin(2 * Math.PI * (pointCount % 100) / 100.0);
        curPos[1] = Math.cos(2 * Math.PI * (pointCount % 100) / 100.0);
        curPos[2] = -1 - ( pointCount % 100 / 100.0);
        pointCount++;
    }
    
    void jiggle() {
        pos[2] += .5;
    }
    
    public boolean tryInsertNeighbor(MDSPoint mp, double distance) {
        if (curNeighborSize < neighborDistances.length) {
            neighborDistances[curNeighborSize] = distance;
            neighbors[curNeighborSize] = mp;
            if (distance > neighborDistances[furthestNeighborIndex]) {
                furthestNeighborIndex = curNeighborSize;
            }
            curNeighborSize++;
            return true;
        } else if (distance < neighborDistances[furthestNeighborIndex]) {
            neighbors[furthestNeighborIndex] = mp;
            neighborDistances[furthestNeighborIndex] = distance;
            
            for (int i = 0; i < neighborDistances.length; i++) {
                if (neighborDistances[i]
                        > neighborDistances[furthestNeighborIndex]) {
                    furthestNeighborIndex = i;
                }
            }
            return true;
        } else {
            return false;
        }
    }
    
    public void setSelected(boolean sel) {
        selected = sel;
    }

    private boolean applyDistanceFactor = false;

    public  double updateForces(MDSPoint[] set, double[] distances) {
        int count = 0;
        double forceSum = 0;
        for (int i = 0; i < set.length; i++) {
            double distance = 0.0;
            MDSPointImpl t2 = (MDSPointImpl) set[i];
            for (int j = 0; j < pos.length; j++) {
                vScratch[j]= pos[j] - t2.pos[j];
                distance += vScratch[j] * vScratch[j];
            }
//            distance = Math.sqrt(distance);
            distance = (distance == 0.0) ? .0001 : distance;
            double force = mdsLayout.getForceMultiplier() * (distances[i] - distance) ;

            // make forces work like gravity (proportional to the inverse of the square of the distance)
            if (applyDistanceFactor) {
                double d = distances[i] + .5;
                force *= 1 / (d * d * d);
            }
            
            for (int j = 0; j < pos.length; j++) {
                double f = vScratch[j] * force;
                vel[j] += f;
                t2.vel[j] -= f;
            }
            
            forceSum += Math.abs(force);
            count++;
        }
        return forceSum / count;
    }
    
    
    public double updateNearestNeighborForces() {
        return updateForces(neighbors, neighborDistances);
    }
    
    
    public void update() {
        for (int j = 0; j < pos.length; j++) {
            vel[j] = (vel[j] > MAX_VEL) ? MAX_VEL : vel[j];
            vel[j] = (vel[j] < -MAX_VEL) ? -MAX_VEL : vel[j];
            pos[j] += vel[j];
            vel[j] = 0;
        }
        
        // now drag the curPos to the pos
        
        for (int i = 0; i < pos.length; i++) {
            double error = pos[i] - curPos[i];
            errSum[i] += error;
            curPos[i] += error * kP + errSum[i] * kI;
        }
    }
    
    public double[] getPosition() {
        return pos.length == 2 ? pos : curPos;
//        return curPos;
//        return pos;
    }
    

}

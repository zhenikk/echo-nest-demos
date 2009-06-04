/*
 * MDSPoint.java
 *
 * Created on March 13, 2006, 6:48 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.echonest.demo.segviz;


/**
 *
 * @author plamere
 */
public interface MDSPoint {
    double getDistance(MDSPoint other);
    boolean tryInsertNeighbor(MDSPoint tp, double distance);
    public  double updateForces(MDSPoint[] set, double[] distances);
    public double updateNearestNeighborForces();
    public void update();
    double[] getPosition();
    public void setSelected(boolean sel);
}

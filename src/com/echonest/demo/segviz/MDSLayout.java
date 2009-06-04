/*
 * MDSLayout.java
 *
 * Created on January 23, 2006, 6:20 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.echonest.demo.segviz;

import java.util.List;
import java.util.Random;

/**
 * Performs a multidimensional scaling (MDS).  Projects a high dimensional
 * space into a low dimensional space.
 * @author plamere
 */
public class MDSLayout {

    //private int vMax = 8;
    private int vMax = 20;
    private int sMax = 12;
//    private int vMax = 8;
//    private int sMax = 16;
    private Random rand = new Random();
    private double forceMultiplier = .001;

    public MDSLayout() {
    }

    /**
     * Calculates velocites for the given data points in a way that
     * will reduce the overall energy in the system
     *
     * @param tss the similarity set
     * @param dataPoints the list of data points
     *
     * A faster way of calculating velocities. This uses the technique
     * described in "A linear iteration time layout algorithm for
     * visualiznig high-dimensinal data" by Matthew Chalmers.
     *
     * http://www.dcs.gla.ac.uk/~matthew/papers/ecsit93.pdf
     */
    double calculateVelocities(List<? extends MDSPoint> dataPoints) {
        double stress = 0;

        // if we don't have enough points to fill our
        // buckets, we bail out

        if (dataPoints.size() < sMax + vMax) {
            return 0.0;
        }

        MDSPoint[] sSet = new MDSPoint[sMax];
        double[] sSetDistances = new double[sMax];

        for (MDSPoint t1 : dataPoints) {
            int sSetSize = 0;
            while (sSetSize < sMax) {
                MDSPoint randomTP = selectRandomPoint(dataPoints, t1);
                double distance = t1.getDistance(randomTP);
                if (!t1.tryInsertNeighbor(randomTP, distance)) {
                    sSet[sSetSize] = randomTP;
                    sSetDistances[sSetSize] = distance;
                    sSetSize++;
                }
            }
            stress = t1.updateNearestNeighborForces();
            stress += t1.updateForces(sSet, sSetDistances);
        }
        return stress;
    }

    public double layout(List<? extends MDSPoint> dataPoints) {
        double stress = calculateVelocities(dataPoints);
        updatePositions(dataPoints);
        return stress;
    }

    public void updatePositions(List<? extends MDSPoint> dataPoints) {
        for (MDSPoint point : dataPoints) {
            point.update();
        }
    }



    double calculateFormalStress(List<? extends MDSPoint> dataPoints) {
        double numerator = 0.0;
        double denominator = 0.0;
        for (int i = 0; i < dataPoints.size() - 1; i++) {
            MDSPoint mdp = dataPoints.get(i);

            for (int j = i + 1; j < dataPoints.size(); j++) {
                MDSPoint mop = dataPoints.get(j);

                double highDistance = mdp.getDistance(mop);
                double lowDistance = getEuclideanDistance(mdp.getPosition(), mop.getPosition());
                numerator += (highDistance - lowDistance) * (highDistance - lowDistance);
                denominator += lowDistance * lowDistance;
            }
        }
        return numerator / denominator;
    }

    /**
     * Select a point at random from the list of tracks
     * @param tracks tracks to select random point from
     * @param t1 random point must not equal this point
     */
    private MDSPoint selectRandomPoint(List<? extends MDSPoint> dataPoints, MDSPoint t1) {
        MDSPoint point = t1;
        do {
            int index = rand.nextInt(dataPoints.size());
            point = dataPoints.get(index);
        } while (point == t1);
        return point;
    }

    /**
     * Sets the force multiplier
     * @param forceMultiplier the new force multiplier
     */
    void setForceMultiplier(double forceMultiplier) {
        this.forceMultiplier = forceMultiplier;
    }

    /*
     * Gets the current force multiplier
     * @return the current force multiplier
     */
    double getForceMultiplier() {
        return forceMultiplier;
    }

    int getVMax() {
        return vMax;
    }

    int getSMax() {
        return sMax;
    }

    public static double getEuclideanDistance(double[] d1, double[] d2) {
        if (d1 == null || d2 == null  || d1.length != d2.length) {
            return -1;
        }

        double sum = 0;
        for (int i = 0; i < d1.length; i++) {
            double delta = d1[i] - d2[i];
            double deltaSquared = delta * delta;
            sum += deltaSquared;
        }

        // note that one can 'tune' the tightness of the visualizations by
        // by modifiying the euclidiean power ..  0.5 is true euclidean
        // but a power of 1.0 gives a better clustering.  It might be interesting
        // to tie this to a slider ...
        // return Math.sqrt(sum);
        // return Math.pow(sum, 1.5);
        return sum;
    }

    public static float getEuclideanDistance(float[] d1, float[] d2) {
        if (d1 == null || d2 == null  || d1.length != d2.length) {
            return -1;
        }

        float sum = 0;
        for (int i = 0; i < d1.length; i++) {
            double delta = d1[i] - d2[i];
            double deltaSquared = delta * delta;
            sum += deltaSquared;
        }

        // note that one can 'tune' the tightness of the visualizations by
        // by modifiying the euclidiean power ..  0.5 is true euclidean
        // but a power of 1.0 gives a better clustering.  It might be interesting
        // to tie this to a slider ...
        // return Math.sqrt(sum);
        // return Math.pow(sum, 1.5);
        return sum;
    }
}

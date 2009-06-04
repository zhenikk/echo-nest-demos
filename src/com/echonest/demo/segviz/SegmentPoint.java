/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.echonest.demo.segviz;

import com.echonest.api.v3.track.Segment;

/**
 *
 * @author plamere
 */
public class SegmentPoint extends MDSPointImpl {
    private Segment segment;
    //private static float LOUDNESS_WEIGHT = 0;
    private static float LOUDNESS_WEIGHT = 0;
    //private static float PITCH_WEIGHT = 0;
    private static float PITCH_WEIGHT = 25000;
    //private static float TIMBRE_WEIGHT = 1;
    private static float TIMBRE_WEIGHT = 1;
    private float[] topTimbre = new float[3];
    private int[] color;
    private int size;
    private float normLoudness = 0;
    private int clusterNumber = -1;


    SegmentPoint(MDSLayout layout, int dim, Segment segment) {
        super(layout, dim);
        this.segment = segment;

        for (int i = 0; i < topTimbre.length; i++) {
            topTimbre[i] = segment.getTimbre()[i];
        }
    }

    @Override
    public double getDistance(MDSPoint o) {
        SegmentPoint other = (SegmentPoint) o;
        float deltaLoudness = segment.getMaxLoudness() - other.getSegment().getMaxLoudness();
        float loudnessDistance = LOUDNESS_WEIGHT * deltaLoudness * deltaLoudness;
        //loudnessDistance = 0;
        float pitchDistance = PITCH_WEIGHT *
                MDSLayout.getEuclideanDistance(segment.getPitches(), other.getSegment().getPitches());
        //pitchDistance = 0;
        //float timbreDistance = TIMBRE_WEIGHT *
        //           MDSLayout.getEuclideanDistance(segment.getTimbre(), other.getSegment().getTimbre());
        float timbreDistance = TIMBRE_WEIGHT *
                    MDSLayout.getEuclideanDistance(topTimbre, other.topTimbre);
        //timbreDistance = 0;
        return loudnessDistance + pitchDistance + timbreDistance;
    }

    public Segment getSegment() {
        return segment;
    }

    void setColor(int[] color) {
        this.color = color;
    }

    int[] getColor() {
        return color;
    }

    int getSize() {
        return size;
    }

    void setSize(int size) {
        this.size = size;
    }

    public float getNormLoudness() {
        return normLoudness;
    }

    public float getProminence() {
        return getNormLoudness() * segment.getDuration();
    }

    public void setNormLoudness(float normLoudness) {
        this.normLoudness = normLoudness;
    }

    public int getClusterNumber() {
        return clusterNumber;
    }

    public void setClusterNumber(int clusterNumber) {
        this.clusterNumber = clusterNumber;
    }


}

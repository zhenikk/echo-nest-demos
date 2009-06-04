/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.echonest.demo.segviz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author plamere
 */
public class Clusterer {

    float minLoudness = .50f;

    void createClusters(List<SegmentPoint> points, int numClusters) {
        List<Node> nodes = new ArrayList<Node>();

        for (SegmentPoint sp : points) {
            if (filter(sp)) {
                nodes.add(new Node(sp));
            }
        }

        while (nodes.size() > numClusters) {
            System.out.println("Nodes remaining " + nodes.size());
            double minDistance = Double.MAX_VALUE;
            Node sn1 = null;
            Node sn2 = null;
            for (Node n1 : nodes) {
                for (Node n2 : nodes) {
                    if (n1 != n2) {
                        double distance = n1.distance(n2);
                        if (distance < minDistance) {
                            minDistance = distance;
                            sn1 = n1;
                            sn2 = n2;
                        }
                    }
                }
            }
            nodes.remove(sn1);
            nodes.remove(sn2);
            Node joined = join(sn1, sn2);
            nodes.add(joined);
        }

        // order clusters by average pitch
        Collections.sort(nodes, new Comparator<Node>() {

            @Override
            public int compare(Node n1, Node n2) {
                return n1.getMostFrequentPitch() - n2.getMostFrequentPitch();
            }
        });
        // assign cluster numbers

        for (int i = 0; i < nodes.size(); i++) {
            for (SegmentPoint sp : nodes.get(i).getPoints()) {
                sp.setClusterNumber(i);
            }
        }
    }

    Node join(Node n1, Node n2) {
        Node nn = new Node();
        nn.set.addAll(n1.getPoints());
        nn.set.addAll(n2.getPoints());
        return nn;
    }

    private boolean filter(SegmentPoint sp) {
        boolean keep = true;
        if (sp.getNormLoudness() < minLoudness) {
            keep = false;
        }
        return keep;
    }
}

class Node {

    Set<SegmentPoint> set = new HashSet<SegmentPoint>();

    Node(SegmentPoint sp) {
        set.add(sp);
    }

    Node() {
    }

    Collection<SegmentPoint> getPoints() {
        return new ArrayList<SegmentPoint>(set);
    }

    double distance(Node other) {
        return distanceFromMostProminent(other);
    }

    double averageInternodeDistance(Node other) {
        int count = 0;
        double distance = 0;
        for (SegmentPoint p : getPoints()) {
            for (SegmentPoint q : getPoints()) {
                distance += p.getDistance(q);
                count++;
            }
        }
        return distance / count;
    }

    double distanceFromMostProminent(Node other) {
        int count = 0;
        double distance = 0;
        SegmentPoint mostProminent = getMostProminentSegment();
        for (SegmentPoint q : getPoints()) {
            distance += mostProminent.getDistance(q);
            count++;
        }
        return distance / count;
    }

    SegmentPoint getMostProminentSegment() {
        SegmentPoint mostProminent = null;
        float maxScore = - Float.MAX_VALUE;
        for (SegmentPoint sp : getPoints()) {
            float score = sp.getProminence();
            if (score > maxScore) {
                maxScore = score;
                mostProminent = sp;
            }
        }
        return mostProminent;
    }

    int getMostFrequentPitch() {
        float[] pitches = new float[12];
        for (SegmentPoint sp : set) {
            float[] segPitches = sp.getSegment().getPitches();
            for (int i = 0; i < segPitches.length; i++) {
                pitches[i] += segPitches[i] * sp.getProminence();
            }
        }

        int pitch = 0;
        float maxPitch = -1;
        for (int i = 0; i < pitches.length; i++) {
            if (pitches[i] > maxPitch) {
                pitch = i;
                maxPitch = pitches[i];
            }
        }
        return pitch;
    }
}

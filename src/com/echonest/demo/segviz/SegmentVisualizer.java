/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.echonest.demo.segviz;

/**
 * This is a processing app that uses the Echo Nest API to generate and
 * show a click plot.  This code is released under a new bsd license.
 */
import com.echonest.api.v3.EchoNestException;
import com.echonest.api.v3.track.Segment;
import com.echonest.api.v3.track.TrackAPI;
import com.echonest.api.v3.track.TrackAPI.AnalysisStatus;
import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import processing.core.PApplet;
import processing.core.PFont;

/**
 * The Pulsar - this was originally developed in processing, which
 * accounts for some of the coding style oddities
 * @author plamere
 */
public class SegmentVisualizer extends PApplet {
    // private String API_KEY = "YOUR_API_KEY";

    private String API_KEY = System.getProperty("ECHO_NEST_API_KEY");
    private TrackAPI trackAPI;
    private PFont font;
    private boolean ready;
    private boolean loading = false;
    private String trackName;
    private String statusMessage = "";
    private String helpMessage = "  p-play/pause  -/+ zoom  n-upload new file";
    private String statusPrefix = "?-help ";
    private String status2 = null;
    private boolean showHelp = false;
    private Minim minim;
    private AudioPlayer player = null;
    private MDSLayout mdsLayout;
    private List<SegmentPoint> allPoints;
    private float MIN_VOLUME = -25;
    private float MIN_DURATION = .1f;
    private float minX = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;
    private int maxSize = 30;
    private PianoRoll pianoRoll;
    private boolean useClustering = false;
    private int numClusters = 12;

    @Override
    public void setup() {
        size(800, 600);
        font = loadFont("Verdana-14.vlw");
        textFont(font, 14);
        smooth();
        stroke(255);
        minim = new Minim(this);
        mdsLayout = new MDSLayout();
        try {
            trackAPI = new TrackAPI(API_KEY);
        } catch (EchoNestException e) {
            error("Trouble connecting to the Echo Nest");
        }
    }

    @Override
    public void draw() {
        // to get the blur
        //fill(0, 4);
        fill(0);
        rect(0, 0, width, height);

        fill(100, 255, 100);
        text("The Echo Nest Segment Visualizer", 300, 20);

        if (ready) {
            pianoRoll.draw(player.position());
        } else {
            showStatus();
        }

        if (false) {
            List<SegmentPoint> curPoints = selectPoints();
            //mdsLayout.layout(curPoints);
            drawPoints(curPoints);
        }
    }

    void addPoints(List<Segment> segments) {
        allPoints = new ArrayList<SegmentPoint>();
        for (Segment segment : segments) {
            if (filter(segment)) {
                SegmentPoint sp = new SegmentPoint(mdsLayout, 2, segment);
                allPoints.add(sp);
            }
        }
    }

    void singlePassLayout() {
        for (int i = 0; i < 1000; i++) {
            double stress = mdsLayout.layout(allPoints);
            if (i % 100 == 0) {
                System.out.println("Layout stress " + stress);
            }
        }
    }

    List<SegmentPoint> selectPoints() {
        float now = player.position() / 1000.f;
        List<SegmentPoint> curPoints = new ArrayList<SegmentPoint>();
        for (SegmentPoint seg : allPoints) {
            float start = seg.getSegment().getStart();
            float finish = start + seg.getSegment().getDuration();
            if (start < now && finish > now) {
                curPoints.add(seg);
            }
        }
        return curPoints;
    }

    void dumpSegments() {
        int count = 0;
        for (SegmentPoint seg : allPoints) {
            float startTime = seg.getSegment().getStart();
            float endTime = startTime + seg.getSegment().getDuration();
            System.out.printf("%d %6.3f %6.3f\n", count++, startTime, endTime);
        }
    }

    private void drawPoints(List<SegmentPoint> points) {
        for (SegmentPoint p : points) {
            float x = (float) p.getPosition()[0];
            float y = (float) p.getPosition()[1];
            x = map(x, minX, maxX, 0 + maxSize, width - maxSize);
            y = map(y, minY, maxY, 0 + maxSize, height - maxSize);
            int[] color = p.getColor();
            stroke(color[0], color[1], color[2]);
            fill(color[0], color[1], color[2]);
            int size = p.getSize();
            ellipse(x, y, size, size);
        //System.out.printf("%.2f/%.2f\n", x, y);
        }
    }

    private boolean filter(Segment seg) {
        boolean keep = true;

        if (seg.getMaxLoudness() < MIN_VOLUME) {
            keep = false;
        }

        if (seg.getDuration() < MIN_DURATION) {
            keep = false;
        }

        return true;
    }

    private void calculateColorMap() {
        float min[] = new float[3];
        float max[] = new float[3];

        for (int i = 0; i < 3; i++) {
            min[i] = Float.MAX_VALUE;
            max[i] = -Float.MAX_VALUE;
        }

        for (SegmentPoint seg : allPoints) {
            for (int i = 0; i < 3; i++) {
                float t = seg.getSegment().getTimbre()[i];
                if (t < min[i]) {
                    min[i] = t;
                }
                if (t > max[i]) {
                    max[i] = t;
                }
            }
        }

        for (SegmentPoint seg : allPoints) {
            int colors[] = new int[3];
            for (int i = 0; i < 3; i++) {
                float t = seg.getSegment().getTimbre()[i];
                colors[i] = (int) map(t, min[i], max[i], 40, 255);
            }
            seg.setColor(colors);
        }
    }

    private void calculateSizes() {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;

        for (SegmentPoint seg : allPoints) {
            for (int i = 0; i < 3; i++) {
                float t = seg.getSegment().getMaxLoudness();
                if (t < min) {
                    min = t;
                }
                if (t > max) {
                    max = t;
                }
            }
        }

        for (SegmentPoint seg : allPoints) {
            float loudness = seg.getSegment().getMaxLoudness();
            int size = (int) map(loudness, min, max, 2, maxSize);
            seg.setSize(size);
            float normLoudness = map(loudness, min, max, 0, 1);
            seg.setNormLoudness(normLoudness);
        }
    }

    void calculateRange() {
        minX = Float.MAX_VALUE;
        maxX = -Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        maxY = -Float.MAX_VALUE;

        for (SegmentPoint seg : allPoints) {
            float x = (float) seg.getPosition()[0];
            float y = (float) seg.getPosition()[1];

            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }

            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }
    }

    private void spawnLoader() {
        Thread t = new Thread() {

            public void run() {
                ready = false;
                if (player != null) {
                    player.pause();
                }
                loading = true;
                manageUpload();
                loading = false;
            }
        };
        t.start();
    }

    private void clusterPoints() {
        if (useClustering) {
            Clusterer clusterer = new Clusterer();
            clusterer.createClusters(allPoints, numClusters);
        }
    }

    private void assignToClusterBasedOnPitch() {
        int lastIn = 0;
        int lastOut = numClusters / 2;
        for (SegmentPoint sp : allPoints) {
            int which = getMaxPitch(sp.getSegment().getPitches());
            lastOut  = constrain(which / 2, 0, numClusters);
            sp.setClusterNumber(lastOut);
            lastIn = which;
            System.out.printf("%d %d\n", which, lastOut);
        }
    }

    void windowedFilter() {
        float windowSize = .15f;
        List<SegmentPoint> filteredPoints = new ArrayList<SegmentPoint>();
        SegmentPoint last = null;

        for (SegmentPoint sg : allPoints) {
            float curTime = sg.getSegment().getStart();
            if (last != null && last.getSegment().getStart() < curTime - windowSize) {
                filteredPoints.add(last);
                last = null;
            }
            if (last == null || getSegmentScore(sg) > getSegmentScore(last)) {
                last = sg;
            }
        }
        if (last != null) {
            filteredPoints.add(last);
        }
        allPoints = filteredPoints;
    }

    private float getSegmentScore(SegmentPoint sp) {
        return sp.getNormLoudness() * sp.getSegment().getDuration();
    }

    int getMaxPitch(float[] pitch) {
        int which = 0;
        for (int i = 1; i < pitch.length; i++) {
            if (pitch[i] > pitch[which]) {
                which = i;
            }
        }
        return which;
    }

    SegmentPoint findMostProminent(float time, float window) {
        float minLoudness = .1f;
        SegmentPoint best = null;
        for (SegmentPoint segp : allPoints) {
            if (segp.getNormLoudness() > minLoudness) {
                float start = segp.getSegment().getStart();
                if (start >= (time - window) && start < (time + window)) {
                    if (best == null || segp.getProminence() > best.getProminence()) {
                        best = segp;
                    }
                }
            }
        }
        return best;
    }

    private void loadFile(String path) {
        File f = new File(path);
        try {
            trackName = f.getName();
            statusMessage("Uploading " + trackName + " to the Echo Nest for analysis (this may take a minute).");
            String id = trackAPI.uploadTrack(f, false);
            player = minim.loadFile(path);
            player.setBalance(.75f);

            statusMessage("Upload complete. Analyzing ...");
            AnalysisStatus status = trackAPI.waitForAnalysis(id, 60000);
            if (status == AnalysisStatus.COMPLETE) {
                statusMessage("Analysis Complete.  Gathering meta data for the plot ...");
                List<Segment> segments = trackAPI.getSegments(id);
                addPoints(segments);
                calculateColorMap();
                calculateSizes();
                statusMessage("Performing layout ...");
                //singlePassLayout();
                calculateRange();
                dumpSegments();
                windowedFilter();
                clusterPoints();
                assignToClusterBasedOnPitch();
                pianoRoll = new PianoRoll(allPoints);
                ready = true;
                statusMessage("Processing done. Ready to go. hit 'p'");
            } else {
                error("No analysis " + status);
            }
        } catch (EchoNestException e) {
            error("Trouble loading file ... " + e.getMessage());
            System.out.println("Trouble loading file ... " + e.getMessage());
        }
    }

    private void manageUpload() {
        if (!ready) {
            statusMessage("Select an MP3 file for upload and analysis");
            String path = selectInput("Select a track for analysis");
            if (path != null) {
                if (path.toLowerCase().endsWith(".mp3")) {
                    loadFile(path);
                } else {
                    error("That doesn't look like an MP3, try something else.");
                }
            } else {
                error("No file selected ... so no plot for you!");
            }
        }

    }

    private void statusMessage(String msg) {
        statusMessage = msg;
    }

    private void showStatus() {
        String status = statusPrefix;
        if (status2 != null) {
            status += status2;
        }
        text(statusMessage, 100, 100);
        text(status, 40, height - 40);
    }

    private void error(String msg) {
        statusMessage = msg;
    }

    private void togglePlaying() {
        if (player != null) {
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.play();
            }
        } else {
            status2 = "  Can only play local audio, upload a file first";
        }
    }

    private void resetPlaying() {
        if (player != null) {
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.play(0);
            }
        } else {
            status2 = "  Can only play local audio, upload a file first";
        }
    }

    @Override
    public void keyPressed() {
        if (key == CODED) {
        } else {

            if (!loading && key == 'n') {
                spawnLoader();
            }

            if (key == 'p') {
                togglePlaying();
            }

            if (key == 'r') {
                resetPlaying();
            }

            if (key == 'L') {
                pianoRoll.adjustLoudnessThreshold(true);
            }
            if (key == 'l') {
                pianoRoll.adjustLoudnessThreshold(false);
            }

            if (key == 'T') {
                pianoRoll.adjustPitchThreshold(true);
            }
            if (key == 't') {
                pianoRoll.adjustPitchThreshold(false);
            }

            if (key == '?') {
                showHelp = !showHelp;
                status2 = showHelp ? helpMessage : null;
            }
        }
    }

    class PianoRoll {

        private List<SegmentPoint> segmentPoints;
        int minY = 60;
        int maxY = height;
        int minX = 20;
        int maxX = width - 20;
        int numRows = 40;
        int rowHeight = (maxY - minY) / numRows;
        int numCols = 12;
        int colWidth;
        float minVolume = .50f;
        float minPitch = .78f;
        int timeBorder = 10;

        PianoRoll(List<SegmentPoint> segmentPoints) {
            this.segmentPoints = segmentPoints;
            if (useClustering) {
                numCols = numClusters;
            } else {
                numCols = 12;
            }
            colWidth = (maxX - minX) / numCols;
        }

        void draw(int curTimeMilli) {
            int curTime = curTimeMilli / 100;
            int startTime = curTime - 10;
            int endTime = startTime + numRows;

            for (int dtime = startTime; dtime < endTime; dtime++) {
                for (SegmentPoint sp : segmentPoints) {
                    Segment seg = sp.getSegment();
                    int segStart = timeToDeciSeconds(seg.getStart());
                    int segEnd = timeToDeciSeconds(seg.getStart() + seg.getDuration());
                    if (dtime >= segStart && dtime < segEnd) {
                        drawSegment(dtime - startTime, sp, dtime == curTime);
                    }
                }
            }
            int nowY = (curTime - startTime) * rowHeight + minY;
            stroke(255, 100, 100);
            line(minX, nowY, maxX, nowY);
        }

        void drawSegment(int row, SegmentPoint sp, boolean highlight) {
            if (true || useClustering) {
                drawSegmentByCluster(row, sp, highlight);
            } else {
                drawSingleSegmentByPitch(row, sp, highlight);
            }

        }

        void drawSegmentByPitch(int row, SegmentPoint sp, boolean highlight) {
            Segment seg = sp.getSegment();
            float[] pitch = seg.getPitches();

            float normLoudness = sp.getNormLoudness();
            if (normLoudness > minVolume) {
                for (int i = 0; i < pitch.length; i++) {
                    if (pitch[i] > minPitch) {
                        int x = i * colWidth + minX;
                        int y = row * rowHeight + minY;

                        int[] color = sp.getColor();

                        if (highlight) {
                            strokeWeight(20);
                            stroke(100, 255, 120);
                        //stroke(255 - color[0], 255 - color[1], 255 - color[2]);
                        } else {
                            strokeWeight(1);
                            stroke(color[0], color[1], color[2]);
                        }

                        fill(color[0], color[1], color[2]);

                        int w = (int) Math.rint(colWidth * pitch[i] * normLoudness);
                        rect(x, y, w, rowHeight);
                    }
                }
            }
        }

        void drawSingleSegmentByPitch(int row, SegmentPoint sp, boolean highlight) {
            Segment seg = sp.getSegment();
            float[] pitch = seg.getPitches();

            float normLoudness = sp.getNormLoudness();
            int which = getMaxPitch(pitch);
            if (pitch[which] > minPitch) {
                int x = which * colWidth + minX;
                int y = row * rowHeight + minY;

                int[] color = sp.getColor();

                if (highlight) {
                    strokeWeight(20);
                    stroke(100, 255, 120);
                //stroke(255 - color[0], 255 - color[1], 255 - color[2]);
                } else {
                    strokeWeight(1);
                    stroke(color[0], color[1], color[2]);
                }

                fill(color[0], color[1], color[2]);

                int w = (int) Math.rint(colWidth * pitch[which] * normLoudness);
                rect(x, y, w, rowHeight);
            }
        }

        void drawSegmentByCluster(int row, SegmentPoint sp, boolean highlight) {
            Segment seg = sp.getSegment();

            float normLoudness = sp.getNormLoudness();
            if (normLoudness > minVolume) {
                int cluster = sp.getClusterNumber();
                int x = cluster * colWidth + minX;
                int y = row * rowHeight + minY;
                int[] color = sp.getColor();

                if (highlight) {
                    strokeWeight(20);
                    stroke(100, 255, 120);
                //stroke(255 - color[0], 255 - color[1], 255 - color[2]);
                } else {
                    strokeWeight(1);
                    stroke(color[0], color[1], color[2]);
                }

                fill(color[0], color[1], color[2]);

                int w = (int) Math.rint(colWidth * normLoudness);
                rect(x, y, w, rowHeight);
            }
        }

        void adjustPitchThreshold(boolean up) {
            if (up) {
                minPitch += .05f;
            } else {
                minPitch -= .05f;
            }
            minPitch = constrain(minPitch, 0, 1);
            System.out.printf("minPitch %.2f\n", minPitch);
        }

        void adjustLoudnessThreshold(boolean up) {
            if (up) {
                minVolume += .05f;
            } else {
                minVolume -= .05f;
            }
            minVolume = constrain(minVolume, 0, 1);
            System.out.printf("minVolume %.2f\n", minVolume);
        }
    }

    int timeToDeciSeconds(float time) {
        return (int) Math.rint(time * 10);
    }

    static public void main(String args[]) {
        PApplet.main(new String[]{"--bgcolor=#c0c0c0", "com.echonest.demo.segviz.SegmentVisualizer"});
    }
}



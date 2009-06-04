/**
 * This is a processing app that uses the Echo Nest API to generate and
 * show a click plot.  This code is released under a new bsd license.
 */
package com.echonest.demo.pulsar;

import com.echonest.api.v3.EchoNestException;
import com.echonest.api.v3.track.FloatWithConfidence;
import com.echonest.api.v3.track.Metadata;
import com.echonest.api.v3.track.Segment;
import com.echonest.api.v3.track.TrackAPI;
import com.echonest.api.v3.track.TrackAPI.AnalysisStatus;
import com.echonest.demo.util.Command;
import com.echonest.demo.util.CommandQueue;
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
public class Pulsar extends PApplet {
    // private String API_KEY = "YOUR_API_KEY";
    private String API_KEY = System.getProperty("ECHO_NEST_API_KEY");
    private TrackAPI trackAPI;
    private PFont font;
    private String artistName = "";
    private String trackName = "";
    private boolean ready;
    private boolean loading = false;

    private String statusMessage = "";
    private String helpMessage = "  p-play/pause  -/+ zoom  n-upload new file";
    private String statusPrefix = "?-help ";
    private String status2 = null;
    private boolean showHelp = false;
    private Minim minim;
    private AudioPlayer player = null;

    float tempo;
    float duration;
    long syncOffset = 100;
    SegmentManager segmentManager = new SegmentManager();

    private CommandQueue commandQueue = new CommandQueue();

    @Override
    public void setup() {
        size(800, 800);
        font = loadFont("Verdana-14.vlw");
        textFont(font, 14);
        smooth();
        stroke(255);
        minim = new Minim(this);
        try {
            trackAPI = new TrackAPI(API_KEY);
        } catch (EchoNestException e) {
            error("Trouble connecting to the Echo Nest");
        }
        commandQueue.setSyncOffset(syncOffset);
    }

    @Override
    public void draw() {
        // to get the blur
        fill(0, 12);
        rect(0,0, width, height);

        fill(100, 255, 100);
        text("The Echo Nest BPM Explorer", 400, 20);
        showStatus();

        while (true) {
            Command command = commandQueue.getNext();
            if (command != null) {
                command.go();
            } else {
                break;
            }
        }

        // draw live segments
        List<Segment> curSegs = segmentManager.getCurrent(commandQueue.now());
        // TODO
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


    private void loadFromID(String id) {
        try {
            Metadata metadata = trackAPI.getMetadata(id);
            commandQueue.clear();
            if (metadata.getArtist() != null) {
                artistName = metadata.getArtist();
            }

            if (metadata.getTitle() != null) {
                trackName = metadata.getTitle();
            }

            tempo = trackAPI.getTempo(id).getValue();
            statusMessage("Data gathered.  Processing beats");
            List<FloatWithConfidence> beatList = trackAPI.getBeats(id);
            for (FloatWithConfidence beat : beatList) {
                commandQueue.add(new BeatCommand(beat));
            }

            statusMessage("Data gathered.  Processing Tatums");
            List<FloatWithConfidence> tatumList = trackAPI.getTatums(id);
            for (FloatWithConfidence tatum : tatumList) {
                commandQueue.add(new TatumCommand(tatum));
            }
            statusMessage("Processing segments");

            List<Segment> segments = trackAPI.getSegments(id);
            for (Segment segment : segments) {
                commandQueue.add(new SegmentCommand(segment));
            }
            ready = true;
            statusMessage("Processing done. Ready to go. hit 'p'");
        } catch (EchoNestException e) {
            error("Problem " + e.getMessage());
        }
    }

    private void loadFile(String path) {
        File f = new File(path);
        try {
            trackName = f.getName();
            statusMessage("Uploading " + trackName + " to the Echo Nest for analysis (this may take a minute).");
            String id = trackAPI.uploadTrack(f, false);

            statusMessage("Upload complete. Analyzing ...");
            AnalysisStatus status = trackAPI.waitForAnalysis(id, 60000);
            if (status == AnalysisStatus.COMPLETE) {
                statusMessage("Analysis Complete.  Gathering meta data for the plot ...");
                loadFromID(id);
                player = minim.loadFile(path);
                player.setBalance(.75f);
            } else {
                error("No analysis " + status);
            }
        } catch (EchoNestException e) {
            error("Trouble loading file ... " + e.getMessage());
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

    private void filter(Beat[] beats, int which, int size) {
        float sum = 0;
        int count = 0;
        for (int i = which - size; i < which + size; i++) {
            if (i >= 0 && i < beats.length - 1) {
                sum += beats[i].bpm;
                count++;
            }
            beats[which].filteredBpm = sum / count;
        }
    }


    class Beat {

        float time;
        float confidence;
        float bpm;
        float filteredBpm;

        Beat(float time, float confidence, float bpm) {
            this.time = time;
            this.confidence = confidence;
            this.bpm = bpm;
        }

        @Override
        public String toString() {
            return time + " " + bpm + " " + confidence;
        }
    }

    private void togglePlaying() {
        if (player != null) {
            if (player.isPlaying()) {
                player.pause();
                commandQueue.pause();
            } else {
                player.play();
                commandQueue.start();
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

            if (key == '?') {
                showHelp = !showHelp;
                status2 = showHelp ? helpMessage : null;
            }
        }
    }

    class BeatCommand extends Command {
        private FloatWithConfidence fwc;

        BeatCommand(FloatWithConfidence fwc) {
            super( (long) (fwc.getValue() * 1000));
            this.fwc = fwc;
        }

        @Override
        public void go() {
            float size = 60 + 100 * fwc.getConfidence();
            fill(199, 64, 64);
            ellipse(width / 2, height / 2, size, size);
        }
    }

    int whichTatum;
    class TatumCommand extends Command {
        private FloatWithConfidence fwc;

        TatumCommand(FloatWithConfidence fwc) {
            super( (long) (fwc.getValue() * 1000));
            this.fwc = fwc;
        }

        @Override
        public void go() {
            whichTatum = (whichTatum + 1) % 12;
            float angle = whichTatum * (2 * PI / 12);
            float size = 10 + 20 * fwc.getConfidence();
            float radius = 100;
            float x = width / 2 +  sin(angle) * radius;
            float y = width / 2 +  cos(angle) * radius;
            fill(32, 199, 64);
            ellipse(x, y, size, size);
        }
    }


    int whichSeg = 0;
    int maxSeg =  16;
    class SegmentCommand extends Command {
        private Segment segment;

        SegmentCommand(Segment segment) {
            super( (long) (segment.getStart() * 1000));
            this.segment = segment;
        }

        @Override
        public void go() {
            float[] timbre = segment.getTimbre();
            fill(timbre[0] * 255, timbre[1] * 255, timbre[2] * 255);
            whichSeg = (whichSeg + 1) % maxSeg;
            float angle = 2 * PI - whichTatum * (2 * PI / 12);
            float radius = 200;
            float x = width / 2 +  sin(angle) * radius;
            float y = width / 2 +  cos(angle) * radius;
            float size = 80 + segment.getStartLoudness();
            ellipse(x, y, size, size);
        }
    }

    class Segment2Command extends Command {
        private Segment segment;

        Segment2Command(Segment segment) {
            super( (long) (segment.getStart() * 1000));
            this.segment = segment;
        }

        @Override
        public void go() {
            float[] timbre = segment.getTimbre();
            fill(timbre[0] * 255, timbre[1] * 255, timbre[2] * 255);

            whichSeg = (whichSeg + 1) % maxSeg;
            float angle = 2 * PI - whichTatum * (2 * PI / 12);
            float radius = 200;
            float x = width / 2 +  sin(angle) * radius;
            float y = width / 2 +  cos(angle) * radius;
            float size = 80 + segment.getStartLoudness();
            ellipse(x, y, size, size);
        }
    }

    class SegmentManager {
        List<Segment> segments = new ArrayList<Segment>();

        void add(Segment segment) {
            segments.add(segment);
        }

        List<Segment> getCurrent(float time) {
            prune(time);
            return segments;
        }

        void prune(float time) {
            List<Segment> keep = new ArrayList<Segment>();
            for (Segment segment : segments) {
                if (time >= segment.getStart() && time < segment.getStart() + segment.getDuration()) {
                    keep.add(segment);
                }
            }
            segments = keep;
        }
    }




    class Point {
        int x;
        int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    static public void main(String args[]) {
        PApplet.main(new String[]{"--bgcolor=#c0c0c0", "com.echonest.demo.pulsar.Pulsar"});
    }
}

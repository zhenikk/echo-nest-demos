/**
 * This is a processing app that uses the Echo Nest API to generate and
 * show a click plot.  This code is released under a new bsd license.
 */
package com.echonest.demo.bpmexplorer;

import com.echonest.api.v3.EchoNestException;
import com.echonest.api.v3.track.FloatWithConfidence;
import com.echonest.api.v3.track.Metadata;
import com.echonest.api.v3.track.TrackAPI;
import com.echonest.api.v3.track.TrackAPI.AnalysisStatus;
import ddf.minim.AudioPlayer;
import ddf.minim.Minim;
import java.io.File;
import java.util.List;
import processing.core.PApplet;
import processing.core.PFont;

/**
 * The ClickTracker - this was originally developed in processing, which
 * accounts for some of the coding style oddities
 * @author plamere
 */
public class ClickTracker extends PApplet {
    // private String API_KEY = "YOUR_API_KEY";
    private String API_KEY = System.getProperty("ECHO_NEST_API_KEY");
    private TrackAPI trackAPI;
    private PFont font;
    private Beat[] beats;
    private float tempo;
    private float duration;
    private float minConfidence = .1f;
    private float scale = .05f;
    private float minBpm;
    private float maxBpm;
    private float minTick;
    private float maxTick;
    private int cursorIndex = 0;
    private String artistName = "";
    private String trackName = "";
    private boolean ready;
    private boolean loading = false;
    private String statusMessage = "Generating a plot for Peggy Sue by Buddy Holly";
    private String initialID = "0aa59ac17d09330584423ce42facf840";
    private String helpMessage = "  p-play/pause  -/+ zoom  n-upload new file";
    private String statusPrefix = "?-help ";
    private String status2 = null;
    private boolean showHelp = false;
    private Minim minim;
    private AudioPlayer player = null;

    @Override
    public void setup() {
        size(1000, 480);
        background(0, 0, 0);
        font = loadFont("Verdana-14.vlw");
        textFont(font, 14);
        smooth();
        stroke(255);

        try {
            trackAPI = new TrackAPI(API_KEY);
            spawnLoaderFromID(initialID);
        } catch (EchoNestException e) {
            error("Trouble connecting to the Echo Nest");
        }
        minim = new Minim(this);
    }

    @Override
    public void draw() {
        background(0);
        fill(100, 255, 100);
        text("The Echo Nest BPM Explorer", 400, 20);
        showStatus();


        // no plot to draw so just show the status message and return
        if (!ready) {
            return;
        }

        // draw the beat track

        float lastX = mapX(0);
        float lastY = mapY(tempo);
        for (int i = 0; i < beats.length; i++) {
            stroke(20 + beats[i].confidence * 200);
            float x = mapX(beats[i].time);
            float y = mapY(beats[i].bpm);
            if (beats[i].confidence > minConfidence) {
                line(lastX, lastY, x, y);
                lastX = x;
                lastY = y;
            }
        }

        // draw the smoothed track

        lastX = mapX(0);
        lastY = mapY(tempo);
        for (int i = 0; i < beats.length; i++) {
            stroke(100, 255, 120);
            float x = mapX(beats[i].time);
            float y = mapY(beats[i].filteredBpm);
            if (beats[i].confidence > minConfidence) {
                line(lastX, lastY, x, y);
                lastX = x;
                lastY = y;
            }
        }

        // draw reference BPM
        {
            float y = mapY(tempo);
            line(0, y, lastX, y);
        }

        setBeatFromMouse();

        // draw the cursors
        {
            Beat beat = getCurBeat();
            float size = 10 * beat.confidence + 2;
            if (size <= 4) {
                size = 2;
            }

            // the blue cursor tracks the bpm
            {
                fill(100, 100, 255);
                stroke(100, 100, 255);
                float x = mapX(beats[cursorIndex].time);
                float y = mapY(beats[cursorIndex].bpm);
                ellipse(x, y, size, size);
            }

            // the red cursor tracks the filtered bpm
            {
                fill(100, 255, 100);
                stroke(100, 255, 100);
                float x = mapX(beat.time);
                float y = mapY(beat.filteredBpm);
                ellipse(x, y, size, size);
            }


            // draw the rest of the text
            text(String.format("Time: %.2f  BPM %.2f  Avg. BPM %.2f",
                    beat.time, beat.bpm, beat.filteredBpm), 40, height - 10);
            text(artistName + " - " + trackName, 400, height - 10);

            // draw the tick marks
            {
                drawBPMTick(minTick, false);
                drawBPMTick(maxTick, true);
                drawTicks();
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

    private void spawnLoaderFromID(final String id) {
        Thread t = new Thread() {

            public void run() {
                ready = false;
                loading = true;
                loadFromID(id);
                loading = false;
            }
        };
        t.start();
    }


    private void loadFromID(String id) {
        try {
            Metadata metadata = trackAPI.getMetadata(id);
            if (metadata.getArtist() != null) {
                artistName = metadata.getArtist();
            }

            if (metadata.getTitle() != null) {
                trackName = metadata.getTitle();
            }

            tempo = trackAPI.getTempo(id).getValue();
            statusMessage("Data gathered.  Generating the plot ...");
            List beatList = trackAPI.getBeats(id);

            statusMessage("Metadata gathered.  Gathering beat data for the plot ...");
            beats = new Beat[beatList.size() - 1];

            statusMessage("All data gathered.  Generating the plot ...");
            for (int i = 1; i < beatList.size(); i++) {
                FloatWithConfidence last = (FloatWithConfidence) beatList.get(i - 1);
                FloatWithConfidence b = (FloatWithConfidence) beatList.get(i);
                float secsPerBeat = b.getValue() - last.getValue();
                float bpm = 60 * 1 / secsPerBeat;
                beats[i - 1] = new Beat(b.getValue(), b.getConfidence(), bpm);
            }
            for (int i = 0; i < beats.length; i++) {
                filter(beats, i, 5);
            }

            duration = beats[beats.length - 1].time;
            applyScale();
            cursorIndex = 0;
            ready = true;
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

    private void applyScale() {
        minBpm = tempo * (1 - scale);
        maxBpm = tempo * (1 + scale);
        minTick = tempo * (1 - scale / 2);
        maxTick = tempo * (1 + scale / 2);
    }

    private Beat getBeatFromTime(float secs) {
        int index = 0;
        for (int i = 0; i < beats.length - 1; i++) {
            if (secs >= beats[i].time && secs < beats[i + 1].time) {
                index = i;
                break;
            }
        }
        cursorIndex = index;
        return beats[index];
    }

    private void setBeatFromMouse() {
        if (mousePressed) {
            getBeatFromTime(map(mouseX, 0, width, 0, duration));
        }
    }

    private Beat getCurBeat() {
        if (player != null && player.isPlaying() && beats.length > 0) {
            float secs = player.position() / 1000;
            return getBeatFromTime(secs);
        }
        return beats[cursorIndex];
    }

    private void drawBPMTick(float tick, boolean above) {
        stroke(128);
        fill(128);
        float y = mapY(tick);
        float x = width - 100;
        line(x, y, width, y);
        float textY = above ? y - 4 : y + 20;
        text(String.format("%.2f BPM", tick), x, textY);
    }

    private void drawTicks() {
        stroke(128);

        for (float i = 0; i < duration; i += 10.0f) {
            float x = mapX(i);
            float y = mapY(tempo);
            line(x, y - 5, x, y + 5);
        }
    }

    private float mapX(float x) {
        return map(x, 0, duration, 0, width);
    }

    private float mapY(float y) {
        return map(y, minBpm, maxBpm, height, 0);
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
            } else {
                Beat beat = beats[cursorIndex];
                player.play(PApplet.parseInt(beat.time * 1000));
            }
        } else {
            status2 = "  Can only play local audio, upload a file first";
        }
    }

    @Override
    public void keyPressed() {
        if (key == CODED) {
            // deal with the arrow keys
            if (keyCode == LEFT) {
                cursorIndex -= 1;
                if (cursorIndex < 0) {
                    cursorIndex = 0;
                }
            } else if (keyCode == RIGHT) {
                cursorIndex += 1;
                if (cursorIndex >= beats.length) {
                    cursorIndex = beats.length - 1;
                }
            }
        } else {
            // deal with the zoom keys
            if (key == '+') {
                scale -= .01f;
            }
            if (key == '-' || key == '_') {
                scale += .01f;
            }
            scale = min(scale, 2);
            scale = max(scale, .01f);
            applyScale();

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

    static public void main(String args[]) {
        PApplet.main(new String[]{"--bgcolor=#c0c0c0", "com.echonest.demo.bpmexplorer.ClickTracker"});
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.echonest.demo.util;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 *
 * @author plamere
 */
public class CommandQueue {
    private PriorityQueue<Command> queue;
    private long startTime;
    private boolean running;
    private long syncOffset;

    public CommandQueue() {
        queue = new PriorityQueue<Command>(100, new Comparator<Command>() {

            @Override
            public int compare(Command arg0, Command arg1) {
                return (int) (arg0.getTime() - arg1.getTime());
            }
        });
    }

    public void start() {
        startTime = System.currentTimeMillis();
        running = true;
    }

    public long getSyncOffset() {
        return syncOffset;
    }

    public void setSyncOffset(long syncOffset) {
        this.syncOffset = syncOffset;
    }


    public void pause() {
        running = false;
    }

    public void add(Command command) {
        queue.add(command);
    }

    public Command getNext() {
        if (running) {
            Command command = queue.peek();
            if (command != null && command.getTime() <= now()) {
                queue.remove();
                return command;
            }
        }
        return null;
    }

    public long now() {
        return syncOffset + System.currentTimeMillis() - startTime;
    }

    public void clear() {
        queue.clear();
        reset();
    }

    public void reset() {
        startTime = 0L;
        running = false;
    }
}

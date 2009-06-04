/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.echonest.demo.util;

/**
 *
 * @author plamere
 */
public abstract class Command {
    private long time;

    public Command(long time) {
        this.time = time;
    }

    public long getTime()  {
        return time;
    }

    public abstract void go();
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package airport.models;

/**
 *
 * @author junyo
 */

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

public class Runway {
    private final ReentrantLock runwayLock;
    private String currentPlane;
    private boolean isLanding; // true for landing, false for takeoff

    public Runway() {
        this.runwayLock = new ReentrantLock(true); // true for fair lock
        this.currentPlane = null;
        this.isLanding = false;
    }

    public boolean acquireForLanding(String planeName, long timeout) throws InterruptedException {
        if (runwayLock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
            currentPlane = planeName;
            isLanding = true;
            return true;
        }
        return false;
    }

    public boolean acquireForTakeoff(String planeName, long timeout) throws InterruptedException {
        if (runwayLock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
            currentPlane = planeName;
            isLanding = false;
            return true;
        }
        return false;
    }

    public void release() {
        String plane = currentPlane;
        currentPlane = null;
        runwayLock.unlock();
    }

    public boolean isOccupied() {
        return runwayLock.isLocked();
    }

    public String getCurrentPlane() {
        return currentPlane;
    }

    public boolean isLanding() {
        return isLanding;
    }

    // Force acquire for emergency situations
    public void forceAcquireForEmergencyLanding(String planeName) throws InterruptedException {
        runwayLock.lockInterruptibly(); // Can be interrupted if needed
        currentPlane = planeName;
        isLanding = true;
    }
}


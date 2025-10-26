/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package airport.threads;

/**
 *
 * @author junyo
 */

import airport.utils.Logger;

public class CleaningCrew extends Thread {
    private static final int CLEANING_TIME = 3000; // 3 seconds to clean
    private final int gateNumber;
    private Plane currentPlane;  // Changed from String to Plane object
    private volatile boolean isRunning;
    private final Object lock = new Object();
    private volatile boolean workCompleted = false;

    public CleaningCrew(int gateNumber) {
        super("CleaningCrew-Gate" + gateNumber);
        this.gateNumber = gateNumber;
        this.isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                synchronized (lock) {
                    while (currentPlane == null && isRunning) {
                        lock.wait();
                    }
                    if (!isRunning) break;
                    
                    Plane plane = currentPlane;
                    Logger.log("Starting to clean " + plane.getName());
                    Thread.sleep(CLEANING_TIME);
                    Logger.log("Finished cleaning " + plane.getName());
                    
                    workCompleted = true;
                    currentPlane = null;
                    
                    // Notify the plane using the actual Plane object
                    synchronized (plane) {
                        plane.notifyAll();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Method that accepts Plane object instead of String
    public void cleanPlane(Plane plane) throws InterruptedException {
        synchronized (lock) {
            workCompleted = false;
            currentPlane = plane;
            lock.notifyAll();
        }
        
        // Wait for cleaning to complete using the actual Plane object as lock
        synchronized (plane) {
            while (!workCompleted) {
                plane.wait();
            }
        }
    }

    // Overloaded method for backward compatibility (if needed)
    public void cleanPlane(String planeName) throws InterruptedException {
        throw new UnsupportedOperationException("Please use cleanPlane(Plane plane) method instead");
    }

    public void shutdown() {
        synchronized (lock) {
            isRunning = false;
            lock.notifyAll();
        }
    }
}
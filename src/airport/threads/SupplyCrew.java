/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package airport.threads;

import airport.models.Gate;

/**
 *
 * @author junyo
 */

import airport.utils.Logger;

public class SupplyCrew extends Thread {
    private static final int SUPPLY_TIME = 3000; // 3 seconds to resupply
    private final int gateNumber;
    private String currentPlane;
    private volatile boolean isRunning;
    private final Object lock = new Object();
    private volatile boolean workCompleted = false;

    public SupplyCrew(int gateNumber) {
        super("SupplyCrew-Gate" + gateNumber);
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
                    
                    String planeName = currentPlane;
                    Logger.log("Starting to resupply " + planeName);
                    Thread.sleep(SUPPLY_TIME);
                    Logger.log("Finished resupplying " + planeName);
                    
                    workCompleted = true;
                    currentPlane = null;
                    
                    // Notify the plane that supply is complete
                    synchronized (planeName.intern()) {
                        planeName.intern().notifyAll();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void supplyPlane(String planeName) throws InterruptedException {
        synchronized (lock) {
            workCompleted = false;
            currentPlane = planeName;
            lock.notifyAll();
        }
        
        // Wait for supply to complete
        synchronized (planeName.intern()) {
            while (!workCompleted) {
                planeName.intern().wait();
            }
        }
    }

    public void shutdown() {
        synchronized (lock) {
            isRunning = false;
            lock.notifyAll();
        }
    }
}

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
import java.util.concurrent.Semaphore;
import java.util.Queue;
import java.util.LinkedList;

public class RefuellingTruck extends Thread {
    private static final int REFUELLING_TIME = 4000; // 4 seconds to refuel
    private final Semaphore semaphore;
    private final Queue<RefuellingRequest> refuellingQueue;
    private volatile boolean isRunning;

    // Inner class to store both plane name and plane object
    private static class RefuellingRequest {
        String planeName;
        Plane plane;

        RefuellingRequest(String planeName, Plane plane) {
            this.planeName = planeName;
            this.plane = plane;
        }
    }

    public RefuellingTruck() {
        super("RefuellingTruck");
        this.semaphore = new Semaphore(1, true); // Fair semaphore with 1 permit
        this.refuellingQueue = new LinkedList<>();
        this.isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                synchronized (refuellingQueue) {
                    RefuellingRequest request = refuellingQueue.peek();
                    if (request != null) {
                        Logger.log("Starting to refuel " + request.planeName);
                        Thread.sleep(REFUELLING_TIME);
                        Logger.log("Finished refuelling " + request.planeName);
                        refuellingQueue.poll();
                        
                        // Notify the plane using the actual Plane object
                        synchronized (request.plane) {
                            request.plane.notifyAll();
                        }
                    }
                }
                Thread.sleep(100); // Small delay if no planes to refuel
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Method that accepts Plane object instead of just String
    public void requestRefuelling(Plane plane) throws InterruptedException {
        semaphore.acquire(); // Wait for truck availability
        synchronized (refuellingQueue) {
            refuellingQueue.offer(new RefuellingRequest(plane.getName(), plane));
        }
        
        // Wait for refuelling to complete using the actual Plane object
        synchronized (plane) {
            plane.wait();
        }
        
        semaphore.release();
    }

    // Overloaded method for backward compatibility (if needed)
    public void requestRefuelling(String planeName) throws InterruptedException {
        throw new UnsupportedOperationException("Please use requestRefuelling(Plane plane) method instead");
    }

    public void shutdown() {
        isRunning = false;
    }
}
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
    private final Queue<String> refuellingQueue;
    private volatile boolean isRunning;

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
                    String planeName = refuellingQueue.peek();
                    if (planeName != null) {
                        Logger.log("Starting to refuel " + planeName);
                        Thread.sleep(REFUELLING_TIME);
                        Logger.log("Finished refuelling " + planeName);
                        refuellingQueue.poll();
                        synchronized (planeName.intern()) {
                            planeName.intern().notifyAll();
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

    public void requestRefuelling(String planeName) throws InterruptedException {
        semaphore.acquire(); // Wait for truck availability
        synchronized (refuellingQueue) {
            refuellingQueue.offer(planeName);
        }
        
        // Wait for refuelling to complete
        synchronized (planeName.intern()) {
            planeName.intern().wait();
        }
        
        semaphore.release();
    }

    public void shutdown() {
        isRunning = false;
    }
}

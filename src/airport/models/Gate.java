/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package airport.models;

/**
 *
 * @author junyo
 */

public class Gate {
    private final int gateNumber;
    private boolean isOccupied;
    private String occupiedBy;
    private final Object lock = new Object(); // For wait/notify mechanism

    public Gate(int gateNumber) {
        this.gateNumber = gateNumber;
        this.isOccupied = false;
        this.occupiedBy = null;
    }

    public int getGateNumber() {
        return gateNumber;
    }

    public boolean isOccupied() {
        synchronized (lock) {
            return isOccupied;
        }
    }

    public String getOccupiedBy() {
        synchronized (lock) {
            return occupiedBy;
        }
    }

    public void occupy(String planeName) {
        synchronized (lock) {
            if (isOccupied) {
                throw new IllegalStateException("Gate " + gateNumber + " is already occupied!");
            }
            isOccupied = true;
            occupiedBy = planeName;
            lock.notifyAll(); // Notify any waiting threads
        }
    }

    public void release() {
        synchronized (lock) {
            if (!isOccupied) {
                throw new IllegalStateException("Gate " + gateNumber + " is not occupied!");
            }
            isOccupied = false;
            occupiedBy = null;
            lock.notifyAll(); // Notify any waiting threads
        }
    }

    public void waitForAvailability() throws InterruptedException {
        synchronized (lock) {
            while (isOccupied) {
                lock.wait();
            }
        }
    }

    @Override
    public String toString() {
        return "Gate-" + gateNumber;
    }
}


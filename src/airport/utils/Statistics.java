/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package airport.utils;

/**
 *
 * @author junyo
 */

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Statistics {
    // ===== Plane counters =====
    private static final AtomicInteger totalPlanes = new AtomicInteger(0);
    private static final AtomicInteger planesLanded = new AtomicInteger(0);
    private static final AtomicInteger planesDeparted = new AtomicInteger(0);

    // ===== Waiting times (ms) =====
    private static final AtomicLong totalLandingWaitTime = new AtomicLong(0);
    private static final AtomicLong totalTakeoffWaitTime = new AtomicLong(0);
    private static final AtomicLong minLandingWaitTime = new AtomicLong(Long.MAX_VALUE);
    private static final AtomicLong maxLandingWaitTime = new AtomicLong(0);
    private static final AtomicLong minTakeoffWaitTime = new AtomicLong(Long.MAX_VALUE);
    private static final AtomicLong maxTakeoffWaitTime = new AtomicLong(0);

    // ===== Passenger counts =====
    private static final AtomicInteger totalPassengersBoarded = new AtomicInteger(0);
    private static final AtomicInteger totalPassengersDisembarked = new AtomicInteger(0);

    // ===== Register plane =====
    public static void registerPlane() {
        totalPlanes.incrementAndGet();
    }

    // ===== Landing statistics =====
    public static void recordLandingWait(long waitMillis) {
        planesLanded.incrementAndGet();
        totalLandingWaitTime.addAndGet(waitMillis);
        updateMin(minLandingWaitTime, waitMillis);
        updateMax(maxLandingWaitTime, waitMillis);
    }

    // ===== Takeoff statistics =====
    public static void recordTakeoffWait(long waitMillis) {
        planesDeparted.incrementAndGet();
        totalTakeoffWaitTime.addAndGet(waitMillis);
        updateMin(minTakeoffWaitTime, waitMillis);
        updateMax(maxTakeoffWaitTime, waitMillis);
    }

    // ===== Passenger statistics =====
    public static void recordPassengersBoarded(int count) {
        totalPassengersBoarded.addAndGet(count);
    }

    public static void recordPassengersDisembarked(int count) {
        totalPassengersDisembarked.addAndGet(count);
    }

    // ===== Helper methods for min/max =====
    private static void updateMin(AtomicLong currentMin, long newValue) {
        currentMin.getAndUpdate(prev -> Math.min(prev, newValue));
    }

    private static void updateMax(AtomicLong currentMax, long newValue) {
        currentMax.getAndUpdate(prev -> Math.max(prev, newValue));
    }

    // ===== Get summary string =====
    public static String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n===== Airport Statistics =====\n");
        sb.append("Total Planes Registered: ").append(totalPlanes.get()).append("\n");
        sb.append("Planes Landed: ").append(planesLanded.get()).append("\n");
        sb.append("Planes Departed: ").append(planesDeparted.get()).append("\n");
        sb.append("Total Passengers Boarded: ").append(totalPassengersBoarded.get()).append("\n");
        sb.append("Total Passengers Disembarked: ").append(totalPassengersDisembarked.get()).append("\n");

        if (planesLanded.get() > 0) {
            double avgLanding = totalLandingWaitTime.get() / 1000.0 / planesLanded.get();
            sb.append("\n--- Landing Wait Times (seconds) ---\n");
            sb.append("Min: ").append(String.format("%.2f", minLandingWaitTime.get() / 1000.0)).append("\n");
            sb.append("Max: ").append(String.format("%.2f", maxLandingWaitTime.get() / 1000.0)).append("\n");
            sb.append("Avg: ").append(String.format("%.2f", avgLanding)).append("\n");
        }

        if (planesDeparted.get() > 0) {
            double avgTakeoff = totalTakeoffWaitTime.get() / 1000.0 / planesDeparted.get();
            sb.append("\n--- Takeoff Wait Times (seconds) ---\n");
            sb.append("Min: ").append(String.format("%.2f", minTakeoffWaitTime.get() / 1000.0)).append("\n");
            sb.append("Max: ").append(String.format("%.2f", maxTakeoffWaitTime.get() / 1000.0)).append("\n");
            sb.append("Avg: ").append(String.format("%.2f", avgTakeoff)).append("\n");
        }

        sb.append("==============================\n");
        return sb.toString();
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package airport.threads;

/**
 *
 * @author junyo
 */

import airport.models.Gate;
import airport.models.Runway;
import airport.utils.Logger;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

public class AirTrafficControl extends Thread {
    private final Runway runway;
    private final List<Gate> gates;
    
    // Single runway queue with skipping capability
    private final Queue<RunwayRequest> runwayQueue;
    
    private final AtomicInteger planesOnGround;
    private volatile boolean isRunning;
    private final Set<String> deniedMessages;

    // Request class
    private static class RunwayRequest {
        String planeName;
        boolean isLanding; // true for landing, false for takeoff
        boolean isEmergency;
        long timestamp;

        RunwayRequest(String planeName, boolean isLanding, boolean isEmergency) {
            this.planeName = planeName;
            this.isLanding = isLanding;
            this.isEmergency = isEmergency;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public AirTrafficControl(Runway runway, List<Gate> gates) {
        super("AirTrafficControl");
        this.runway = runway;
        this.gates = new ArrayList<>(gates);
        this.runwayQueue = new LinkedList<>();
        this.planesOnGround = new AtomicInteger(0);
        this.isRunning = true;
        this.deniedMessages = new HashSet<>();
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                synchronized(this) {
                    processRunwayRequests();
                }
                Thread.sleep(100); // Small delay to prevent busy waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processRunwayRequests() {
        // If runway is occupied, log denials and return
        if (runway.isOccupied()) {
            // Log runway occupied denials for first request in queue (if any)
            RunwayRequest firstRequest = runwayQueue.peek();
            if (firstRequest != null) {
                if (firstRequest.isLanding) {
                    logDenial(firstRequest.planeName, "Landing denied for " + firstRequest.planeName + ", runway occupied.");
                } else {
                    logDenial(firstRequest.planeName, "Takeoff denied for " + firstRequest.planeName + ", runway occupied.");
                }
            }
            return;
        }

        // Find the first request in the queue that can actually be processed
        Iterator<RunwayRequest> iterator = runwayQueue.iterator();
        RunwayRequest requestToProcess = null;
        
        while (iterator.hasNext()) {
            RunwayRequest request = iterator.next();
            
            if (request.isLanding) {
                // Check if landing is possible
                if (canProcessLandingRequest(request)) {
                    requestToProcess = request;
                    iterator.remove(); // Remove from queue
                    break;
                } else {
                    // Log denial reason (only once per plane per reason)
                    if (planesOnGround.get() >= 3) {
                        logDenial(request.planeName, "Landing denied for " + request.planeName + ", airport full.");
                    } else if (findAvailableGate() == null) {
                        logDenial(request.planeName, "Landing denied for " + request.planeName + ", no gates available.");
                    }
                }
            } else {
                // Check if takeoff is possible
                if (canProcessTakeoffRequest(request)) {
                    requestToProcess = request;
                    iterator.remove(); // Remove from queue
                    break;
                }
            }
        }
        
        // Process the found request
        if (requestToProcess != null) {
            if (requestToProcess.isLanding) {
                processLandingRequest(requestToProcess);
            } else {
                processTakeoffRequest(requestToProcess);
            }
        }
    }

    private boolean canProcessLandingRequest(RunwayRequest request) {
        // Can land if airport not full AND there's an available gate
        return planesOnGround.get() < 3 && findAvailableGate() != null;
    }

    private boolean canProcessTakeoffRequest(RunwayRequest request) {
        // Can take off if the plane is actually at a gate
        for (Gate gate : gates) {
            if (request.planeName.equals(gate.getOccupiedBy())) {
                return true;
            }
        }
        return false;
    }

    private void processLandingRequest(RunwayRequest request) {
        Gate availableGate = findAvailableGate();
        if (availableGate != null) {
            Logger.log("Permission granted for " + request.planeName + " to land.");
            Logger.log("Gate-" + availableGate.getGateNumber() + " assigned for " + request.planeName);
            availableGate.occupy(request.planeName);
            planesOnGround.incrementAndGet();
            synchronized (request.planeName.intern()) {
                request.planeName.intern().notifyAll();
            }
        }
    }

    private void processTakeoffRequest(RunwayRequest request) {
        // Find the gate this plane is occupying
        Gate occupiedGate = null;
        for (Gate gate : gates) {
            if (request.planeName.equals(gate.getOccupiedBy())) {
                occupiedGate = gate;
                break;
            }
        }
        
        if (occupiedGate != null) {
            Logger.log("Permission granted for " + request.planeName + " to take off.");
            occupiedGate.release();
            synchronized (request.planeName.intern()) {
                request.planeName.intern().notifyAll();
            }
        }
    }

    private void logDenial(String planeName, String message) {
        String key = planeName + message;
        if (!deniedMessages.contains(key)) {
            Logger.log(message);
            deniedMessages.add(key);
        }
    }

    private Gate findAvailableGate() {
        for (Gate gate : gates) {
            if (!gate.isOccupied()) {
                return gate;
            }
        }
        return null;
    }

    public synchronized void requestLanding(String planeName, boolean isEmergency) {
        if (isEmergency) {
            Logger.log("Emergency landing request from " + planeName);
            // Remove any existing request for this plane
            runwayQueue.removeIf(req -> req.planeName.equals(planeName));
            // Add emergency request to front with early timestamp
            RunwayRequest emergencyRequest = new RunwayRequest(planeName, true, true);
            emergencyRequest.timestamp = 0; // Highest priority
            ((LinkedList<RunwayRequest>) runwayQueue).addFirst(emergencyRequest);
        } else {
            // Check if not already in queue
            boolean alreadyInQueue = runwayQueue.stream()
                .anyMatch(req -> req.planeName.equals(planeName) && req.isLanding);
            if (!alreadyInQueue) {
                // Always add to queue for proper runway management and processing order
                runwayQueue.offer(new RunwayRequest(planeName, true, false));
            }
        }
    }

    public synchronized void requestTakeoff(String planeName) {
        // Check if not already in queue
        boolean alreadyInQueue = runwayQueue.stream()
            .anyMatch(req -> req.planeName.equals(planeName) && !req.isLanding);
        if (!alreadyInQueue) {
            // Always add to queue for proper runway management and processing order
            runwayQueue.offer(new RunwayRequest(planeName, false, false));
        }
    }

    public Gate getGateForPlane(String planeName) {
        for (Gate gate : gates) {
            if (planeName.equals(gate.getOccupiedBy())) {
                return gate;
            }
        }
        return null;
    }

    public void planeLeftGround() {
        planesOnGround.decrementAndGet();
    }

    public void shutdown() {
        isRunning = false;
    }

    public String getGateStatusCheck() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Gate Status Check =====\n");
        
        boolean allEmpty = true;
        for (Gate gate : gates) {
            String status = gate.isOccupied() ? 
                "OCCUPIED by " + gate.getOccupiedBy() : "EMPTY";
            sb.append("Gate-").append(gate.getGateNumber()).append(": ").append(status).append("\n");
            
            if (gate.isOccupied()) {
                allEmpty = false;
            }
        }
        
        sb.append("\nSanity Check Result: ");
        if (allEmpty) {
            sb.append("All gates are empty - PASSED\n");
        } else {
            sb.append("Some gates still occupied - FAILED\n");
        }
        sb.append("==============================");
        
        return sb.toString();
    }
}


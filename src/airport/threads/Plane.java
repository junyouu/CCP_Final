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
import airport.utils.Statistics;
import java.util.Random;

public class Plane extends Thread {
    // Operation times in milliseconds
    private static final int LANDING_TIME = 1000;      // 2 seconds for landing
    private static final int COASTING_TIME = 1000;     // 1 seconds for coasting to gate
    private static final int DOCKING_TIME = 1000;      // 1 seconds for docking procedure
    private static final int TAKEOFF_TIME = 2000;      // 2 seconds for takeoff

    private final AirTrafficControl atc;
    private final Runway runway;
    private final RefuellingTruck refuellingTruck;
    private final CleaningCrew[] cleaningCrews;
    private final SupplyCrew[] supplyCrews;
    private final int planeNumber;
    private final EmbarkPassenger embarkPassenger;
    private final DisembarkPassenger disembarkPassenger; // Add this field
    private final Random random;
    private boolean isEmergency;
    private long landingRequestTime;
    private long takeoffRequestTime;

    public Plane(int number, AirTrafficControl atc, Runway runway, 
                RefuellingTruck refuellingTruck, 
                CleaningCrew[] cleaningCrews,
                SupplyCrew[] supplyCrews,
                EmbarkPassenger embarkPassenger) {
        super("Plane-" + number);
        this.planeNumber = number;
        this.atc = atc;
        this.runway = runway;
        this.refuellingTruck = refuellingTruck;
        this.cleaningCrews = cleaningCrews;
        this.supplyCrews = supplyCrews;
        this.embarkPassenger = embarkPassenger; // Store reference to external passenger group
        this.disembarkPassenger = new DisembarkPassenger(getName()); // Create disembark thread in constructor
        this.random = new Random();
        this.isEmergency = false;
        
        // Set priority based on plane number (Plane-5 gets highest emergency)
        if (number == 5) {
            setEmergencyLevel(3);  // Highest emergency level
        } else {
            setEmergencyLevel(1);  // Normal priority
        }
        
        Statistics.registerPlane();
    }

    @Override
    public void run() {
        try {
            // Wait based on plane number to ensure sequential arrival
            Thread.sleep(planeNumber * 1000); // 1 second delay per plane number
            
            // Request landing
            requestLanding();
            
            // Land and operate
            if (land()) {
                performGroundOperations();
                takeoff();
            }
        } catch (InterruptedException e) {
            Logger.log("Operations interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private void requestLanding() throws InterruptedException {
        landingRequestTime = System.currentTimeMillis();
        Logger.log("Requesting landing permission...");
        atc.requestLanding(getName(), isEmergency);
        
        // Wait for permission (will be notified by ATC)
        synchronized (getName().intern()) {
            getName().intern().wait();
        }
    }

    private boolean land() throws InterruptedException {
        // Try to acquire runway
        if (runway.acquireForLanding(getName(), 5000)) {
            Logger.log("Landing...");
            Thread.sleep(LANDING_TIME); // Landing time
            Logger.log("Landed");
            
            // Record landing statistics
            long waitTime = System.currentTimeMillis() - landingRequestTime;
            Statistics.recordLandingWait(waitTime);
            
            // Get assigned gate
            Gate assignedGate = atc.getGateForPlane(getName());
            Logger.log("Coasting to Gate-" + assignedGate.getGateNumber());
            Thread.sleep(COASTING_TIME); // Coasting time
            Logger.log("Starting docking procedure at Gate-" + assignedGate.getGateNumber());
            Thread.sleep(DOCKING_TIME); // Docking time
            Logger.log("Docked at Gate-" + assignedGate.getGateNumber());
            
            runway.release();
            return true;
        }
        return false;
    }

    private void performGroundOperations() throws InterruptedException {
        Gate assignedGate = atc.getGateForPlane(getName());
        int gateIndex = assignedGate.getGateNumber() - 1;

        // Start passenger disembarkation (using pre-created thread)
        disembarkPassenger.start();

        // Start refuelling (can happen in parallel with disembarkation)
        Thread refuelThread = new Thread(() -> {
            try {
                Logger.log("Request for refuelling");
                refuellingTruck.requestRefuelling(getName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        refuelThread.setName(getName()); // Set thread name to plane name
        refuelThread.start();

        // Wait for disembarkation to complete before cleaning
        disembarkPassenger.join();

        // Sequential cabin operations
        cleaningCrews[gateIndex].cleanPlane(getName());
        supplyCrews[gateIndex].supplyPlane(getName());

        // Wait for refuelling to complete before boarding
        refuelThread.join();

        // Board new passengers
        embarkPassenger.start();
        embarkPassenger.join();
    }

    private void takeoff() throws InterruptedException {
        takeoffRequestTime = System.currentTimeMillis();
        Logger.log("Requesting takeoff permission");
        atc.requestTakeoff(getName());
        
        // Wait for permission from ATC
        synchronized (getName().intern()) {
            getName().intern().wait();
        }

        // ATC has granted permission, now acquire runway for takeoff
        Logger.log("Starting takeoff procedure...");
        runway.acquireForTakeoff(getName(), 5000);
        Thread.sleep(TAKEOFF_TIME); // Takeoff time
        runway.release();
        
        // Record takeoff statistics
        long waitTime = System.currentTimeMillis() - takeoffRequestTime;
        Statistics.recordTakeoffWait(waitTime);
        
        atc.planeLeftGround();
        Logger.log("Successfully departed");
    }

    public void setEmergencyLevel(int level) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Emergency level must be between 1 and 3");
        }
        this.isEmergency = (level > 1);
        setPriority(Thread.MIN_PRIORITY + level - 1);
    }
}
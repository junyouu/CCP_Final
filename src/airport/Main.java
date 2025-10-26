/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package airport;

/**
 *
 * @author junyo
 */

import airport.threads.CleaningCrew;
import airport.threads.Plane;
import airport.threads.SupplyCrew;
import airport.threads.RefuellingTruck;
import airport.threads.AirTrafficControl;
import airport.models.Gate;
import airport.models.Runway;
import airport.threads.*;
import airport.utils.Statistics;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    private static final int NUM_GATES = 3;
    private static final int NUM_PLANES = 6;
    private static final int NUM_PASSENGER_GROUPS = 6;
    
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        
        try {
            Runway runway = new Runway();
            List<Gate> gates = createGates();

            RefuellingTruck refuellingTruck = new RefuellingTruck();
            CleaningCrew[] cleaningCrews = new CleaningCrew[NUM_GATES];
            SupplyCrew[] supplyCrews = new SupplyCrew[NUM_GATES];
            
            for (int i = 0; i < NUM_GATES; i++) {
                cleaningCrews[i] = new CleaningCrew(i + 1);
                supplyCrews[i] = new SupplyCrew(i + 1);
            }

            AirTrafficControl atc = new AirTrafficControl(runway, gates);

            // Start all service threads
            startServiceThreads(refuellingTruck, cleaningCrews, supplyCrews, atc);

            // Create passenger groups
            EmbarkPassenger[] passengerGroups = createPassengerGroups();

            // Create and start planes (now with passenger groups)
            List<Plane> planes = createPlanes(atc, runway, refuellingTruck, cleaningCrews, supplyCrews, passengerGroups);
            startPlanes(planes);

            // Wait for all planes to complete
            waitForPlanesToComplete(planes);
            
            // Calculate simulation duration
            long endTime = System.currentTimeMillis();
            double durationSeconds = (endTime - startTime) / 1000.0;

            // Perform gate sanity check before shutdown
            String gateStatusCheck = atc.getGateStatusCheck();

            // Shutdown all services
            shutdownServices(atc, refuellingTruck, cleaningCrews, supplyCrews);

            // Print final statistics
            System.out.println("\nSimulation completed successfully!");
            System.out.printf("Total simulation time: %.2f seconds%n", durationSeconds);
            System.out.println(Statistics.getSummary());
            System.out.println(gateStatusCheck);

        } catch (Exception e) {
            System.err.println("Simulation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<Gate> createGates() {
        List<Gate> gates = new ArrayList<>();
        for (int i = 1; i <= NUM_GATES; i++) {
            gates.add(new Gate(i));
        }
        return gates;
    }

    private static void startServiceThreads(RefuellingTruck refuellingTruck, 
                                          CleaningCrew[] cleaningCrews,
                                          SupplyCrew[] supplyCrews,
                                          AirTrafficControl atc) {
        refuellingTruck.start();
        for (int i = 0; i < NUM_GATES; i++) {
            cleaningCrews[i].start();
            supplyCrews[i].start();
        }
        atc.start();
    }

    private static List<Plane> createPlanes(AirTrafficControl atc, 
                                        Runway runway,
                                        RefuellingTruck refuellingTruck,
                                        CleaningCrew[] cleaningCrews,
                                        SupplyCrew[] supplyCrews,
                                        EmbarkPassenger[] passengerGroups) {
      List<Plane> planes = new ArrayList<>();
      for (int i = 1; i <= NUM_PLANES; i++) {
          planes.add(new Plane(i, atc, runway, refuellingTruck, cleaningCrews, supplyCrews, passengerGroups[i-1]));
      }
      return planes;
    }

    private static void startPlanes(List<Plane> planes) {
        Random random = new Random();
        for (Plane plane : planes) {
            plane.start();
            try {
                // Wait randomly between 0, 1, or 2 seconds before starting next plane
                int delay = 1000;
                // int delay = random.nextInt(3) * 1000; // 0, 1000, or 2000 milliseconds
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void waitForPlanesToComplete(List<Plane> planes) {
        for (Plane plane : planes) {
            try {
                plane.join();
            } catch (InterruptedException e) {
                System.err.println("Error waiting for plane " + plane.getName() + ": " + e.getMessage());
            }
        }
    }

    private static void shutdownServices(AirTrafficControl atc,
                                       RefuellingTruck refuellingTruck,
                                       CleaningCrew[] cleaningCrews,
                                       SupplyCrew[] supplyCrews) {
        // Shutdown all services
        atc.shutdown();
        refuellingTruck.shutdown();
        for (int i = 0; i < NUM_GATES; i++) {
            cleaningCrews[i].shutdown();
            supplyCrews[i].shutdown();
        }

        // Give services time to shutdown gracefully
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Add new method after createGates() method (around line 85)
    private static EmbarkPassenger[] createPassengerGroups() {
        EmbarkPassenger[] passengerGroups = new EmbarkPassenger[NUM_PASSENGER_GROUPS];
        for (int i = 0; i < NUM_PASSENGER_GROUPS; i++) {
            String planeName = "Plane-" + (i + 1);
            passengerGroups[i] = new EmbarkPassenger(planeName);
        }
        return passengerGroups;
    }

}



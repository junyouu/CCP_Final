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
import airport.utils.Statistics;
import java.util.Random;

public class DisembarkPassenger extends Thread {
    private static final int DISEMBARK_TIME = 3000; // 3 seconds to disembark
    private final String planeName;
    private final int passengerCount; 
    private static final Random rand = new Random();

    public DisembarkPassenger(String planeName) {
        super(planeName + "-Passenger");
        this.planeName = planeName;
        this.passengerCount = rand.nextInt(51); // Random 0–50 passengers
    }

    @Override
    public void run() {
        try {
            Logger.log("Disembarking " + passengerCount + " passengers for " + planeName);
            Thread.sleep(DISEMBARK_TIME);
            Statistics.recordPassengersDisembarked(passengerCount);
            Logger.log("All " + passengerCount + " passengers disembarked from " + planeName);
        } catch (InterruptedException e) {
            Logger.log("Disembarkation interrupted for " + planeName);
            Thread.currentThread().interrupt();
        }
    }
}

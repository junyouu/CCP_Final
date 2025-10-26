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

public class EmbarkPassenger extends Thread {
    private static final int EMBARK_TIME = 3000; // 3 seconds to embark
    private final String planeName;
    private final int passengerCount;
    private final Random random;

    public EmbarkPassenger(String planeName) {
        super("New-" + planeName + "-Passenger");
        this.planeName = planeName;
        this.random = new Random();
        this.passengerCount = random.nextInt(51); // Random 0-50 passengers
    }

    @Override
    public void run() {
        try {
            Logger.log("Embarking " + passengerCount + " passengers for " + planeName);
            Thread.sleep(EMBARK_TIME);
            Statistics.recordPassengersBoarded(passengerCount);
            Logger.log(passengerCount + " passengers successfully boarded " + planeName);
        } catch (InterruptedException e) {
            Logger.log("Boarding interrupted for " + planeName);
            Thread.currentThread().interrupt();
        }
    }
}


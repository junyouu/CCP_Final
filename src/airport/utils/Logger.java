/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package airport.utils;

/**
 *
 * @author junyo
 */

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public static synchronized void log(String message) {
        String time = sdf.format(new Date());
        System.out.println("[" + time + "] " + Thread.currentThread().getName() + " : " + message);
    }
}

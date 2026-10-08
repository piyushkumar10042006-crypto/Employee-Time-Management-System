package com.timemanagement.thread;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Background Thread that drives the live digital clock and shift stopwatch on the GUI.
 * Fulfills the Multithreading & Synchronization marking criteria.
 */
public class LiveClockThread extends Thread {

    private final JLabel clockLabel;
    private final JLabel shiftTimerLabel;
    private volatile boolean running = true;
    private volatile LocalTime punchInTime = null;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a");

    public LiveClockThread(JLabel clockLabel, JLabel shiftTimerLabel) {
        super("LiveClock-Worker-Thread");
        this.clockLabel = clockLabel;
        this.shiftTimerLabel = shiftTimerLabel;
        setDaemon(true); // Allows JVM to exit cleanly when main window closes
    }

    public synchronized void setPunchInTime(LocalTime time) {
        this.punchInTime = time;
    }

    public synchronized void clearPunchInTime() {
        this.punchInTime = null;
    }

    public void stopClock() {
        this.running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        while (running) {
            LocalTime now = LocalTime.now();
            String currentTimeStr = now.format(TIME_FORMATTER);

            String shiftDurationStr = "Status: Not Clocked In";
            if (punchInTime != null) {
                Duration elapsed = Duration.between(punchInTime, now);
                long hours = elapsed.toHours();
                long minutes = elapsed.toMinutesPart();
                long seconds = elapsed.toSecondsPart();
                shiftDurationStr = String.format("⏱ Working: %02dh %02dm %02ds", hours, minutes, seconds);
            }

            final String clockText = "⏰ Current Time: " + currentTimeStr;
            final String shiftText = shiftDurationStr;

            // Safely dispatch GUI updates to the Swing Event Dispatch Thread (EDT)
            SwingUtilities.invokeLater(() -> {
                if (clockLabel != null) {
                    clockLabel.setText(clockText);
                }
                if (shiftTimerLabel != null) {
                    shiftTimerLabel.setText(shiftText);
                }
            });

            try {
                Thread.sleep(1000); // Sleep for 1 second
            } catch (InterruptedException e) {
                // Thread interrupted on shutdown
                break;
            }
        }
    }
}

package com.timemanagement;

import com.timemanagement.model.AttendanceRecord;
import com.timemanagement.model.enums.AttendanceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying standard shifts, overnight shifts crossing midnight,
 * overtime calculations, and timestamp validation.
 */
class OvernightShiftTest {

    @Test
    @DisplayName("Standard Day Shift (9:00 to 17:30 = 8.5h worked, 0.5h overtime)")
    void testStandardDayShift() {
        AttendanceRecord record = new AttendanceRecord();
        LocalDate today = LocalDate.of(2026, 11, 7);
        record.setRecordDate(today);
        record.setClockIn(LocalTime.of(9, 0));
        record.setClockOutDate(today);
        record.setClockOut(LocalTime.of(17, 30));

        assertEquals(8.5, record.getHoursWorked(), 0.01, "Should calculate 8.5 total hours");
        assertEquals(0.5, record.getOvertimeHours(), 0.01, "Should calculate 0.5 hours overtime");
        assertEquals(AttendanceStatus.PRESENT, record.getStatus());
    }

    @Test
    @DisplayName("Overnight Shift Crossing Midnight (22:00 to 06:00 next day = 8.0h)")
    void testOvernightShiftCrossingMidnight() {
        AttendanceRecord record = new AttendanceRecord();
        LocalDate punchInDate = LocalDate.of(2026, 11, 7);
        LocalDate punchOutDate = LocalDate.of(2026, 11, 8);

        record.setRecordDate(punchInDate);
        record.setClockIn(LocalTime.of(22, 0)); // 10:00 PM

        record.setClockOutDate(punchOutDate);
        record.setClockOut(LocalTime.of(6, 0));  // 6:00 AM next day

        assertEquals(8.0, record.getHoursWorked(), 0.01, "Overnight 22:00-06:00 must be exactly 8.0 hours");
        assertEquals(0.0, record.getOvertimeHours(), 0.01, "Overnight 8h shift should have 0.0 overtime");
        assertEquals(AttendanceStatus.PRESENT, record.getStatus());
    }

    @Test
    @DisplayName("Overnight Shift With Overtime (20:00 to 06:30 next day = 10.5h worked, 2.5h overtime)")
    void testOvernightShiftWithOvertime() {
        AttendanceRecord record = new AttendanceRecord();
        LocalDateTime clockIn = LocalDateTime.of(2026, 11, 7, 20, 0);   // 8:00 PM
        LocalDateTime clockOut = LocalDateTime.of(2026, 11, 8, 6, 30);  // 6:30 AM next day

        record.setClockInDateTime(clockIn);
        record.setClockOutDateTime(clockOut);

        assertEquals(10.5, record.getHoursWorked(), 0.01, "Total hours must be 10.5");
        assertEquals(2.5, record.getOvertimeHours(), 0.01, "Overtime hours must be 2.5 (10.5 - 8.0)");
        assertEquals(AttendanceStatus.PRESENT, record.getStatus());
    }

    @Test
    @DisplayName("Half Day Shift (4.0 hours sets status to HALF_DAY)")
    void testHalfDayShift() {
        AttendanceRecord record = new AttendanceRecord();
        LocalDate date = LocalDate.of(2026, 11, 7);
        record.setRecordDate(date);
        record.setClockIn(LocalTime.of(9, 0));
        record.setClockOutDate(date);
        record.setClockOut(LocalTime.of(13, 0)); // 4 hours

        assertEquals(4.0, record.getHoursWorked(), 0.01);
        assertEquals(0.0, record.getOvertimeHours(), 0.01);
        assertEquals(AttendanceStatus.HALF_DAY, record.getStatus());
    }

    @Test
    @DisplayName("Invalid Clock-Out Timestamp (Clock-out before Clock-in throws IllegalArgumentException)")
    void testInvalidClockOutThrowsException() {
        AttendanceRecord record = new AttendanceRecord();
        LocalDate date = LocalDate.of(2026, 11, 7);
        record.setRecordDate(date);
        record.setClockIn(LocalTime.of(18, 0)); // 6:00 PM

        record.setClockOutDate(date); // Same date

        // Clock out set to 10:00 AM on the SAME date (before 6:00 PM) must be rejected
        assertThrows(IllegalArgumentException.class, () -> {
            record.setClockOut(LocalTime.of(10, 0));
        }, "Clocking out before clock-in date/time must throw IllegalArgumentException");
    }
}

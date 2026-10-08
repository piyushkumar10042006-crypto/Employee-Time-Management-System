package com.timemanagement.service;

import com.timemanagement.dao.AttendanceDAO;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.model.AttendanceRecord;
import com.timemanagement.model.enums.AttendanceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service generating attendance, working hours, and overtime statistics.
 * Demonstrates Collections (List, Map) and Java Streams / Lambdas.
 */
public class ReportService {

    private final AttendanceDAO attendanceDAO;

    public ReportService() {
        this.attendanceDAO = new AttendanceDAO();
    }

    public ReportService(AttendanceDAO attendanceDAO) {
        this.attendanceDAO = attendanceDAO;
    }

    /**
     * Computes total hours worked for an employee using Java Streams.
     */
    public double getTotalHoursWorked(int userId) throws DatabaseOperationException {
        List<AttendanceRecord> records = attendanceDAO.findByUserId(userId);
        return records.stream()
                .mapToDouble(AttendanceRecord::getHoursWorked)
                .sum();
    }

    /**
     * Computes total overtime hours for an employee using Java Streams.
     */
    public double getTotalOvertimeHours(int userId) throws DatabaseOperationException {
        List<AttendanceRecord> records = attendanceDAO.findByUserId(userId);
        return records.stream()
                .mapToDouble(AttendanceRecord::getOvertimeHours)
                .sum();
    }

    /**
     * Generates a breakdown of attendance counts grouped by AttendanceStatus.
     */
    public Map<AttendanceStatus, Long> getAttendanceStatusBreakdown(int userId) throws DatabaseOperationException {
        List<AttendanceRecord> records = attendanceDAO.findByUserId(userId);
        return records.stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getStatus, Collectors.counting()));
    }

    /**
     * Generates a formatted text summary report suitable for display in the UI or printing.
     */
    public String generateUserMonthlyReport(int userId, String userName) throws DatabaseOperationException {
        List<AttendanceRecord> records = attendanceDAO.findByUserId(userId);

        double totalHours = records.stream().mapToDouble(AttendanceRecord::getHoursWorked).sum();
        double totalOvertime = records.stream().mapToDouble(AttendanceRecord::getOvertimeHours).sum();
        long presentDays = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        long lateDays = records.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
        long halfDays = records.stream().filter(r -> r.getStatus() == AttendanceStatus.HALF_DAY).count();

        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================\n");
        sb.append("        EMPLOYEE TIME & ATTENDANCE SUMMARY REPORT        \n");
        sb.append("=========================================================\n");
        sb.append(String.format("Employee Name   : %s\n", userName));
        sb.append(String.format("Generated Date  : %s\n", LocalDate.now()));
        sb.append("---------------------------------------------------------\n");
        sb.append(String.format("Total Days Logged: %d\n", records.size()));
        sb.append(String.format("Present Days     : %d\n", presentDays));
        sb.append(String.format("Late Arrivals    : %d\n", lateDays));
        sb.append(String.format("Half Days        : %d\n", halfDays));
        sb.append(String.format("Total Work Hours : %.2f hrs\n", totalHours));
        sb.append(String.format("Total Overtime   : %.2f hrs\n", totalOvertime));
        sb.append("---------------------------------------------------------\n");
        sb.append("Recent Punch Logs:\n");
        sb.append(String.format("%-12s | %-10s | %-10s | %-8s | %-10s\n", "Date", "In", "Out", "Hours", "Status"));
        sb.append("---------------------------------------------------------\n");

        records.stream().limit(10).forEach(r -> {
            String inStr = r.getClockIn() != null ? r.getClockIn().toString() : "--:--";
            String outStr = r.getClockOut() != null ? r.getClockOut().toString() : "--:--";
            sb.append(String.format("%-12s | %-10s | %-10s | %-8.2f | %-10s\n",
                    r.getRecordDate(), inStr, outStr, r.getHoursWorked(), r.getStatus()));
        });
        sb.append("=========================================================\n");

        return sb.toString();
    }
}

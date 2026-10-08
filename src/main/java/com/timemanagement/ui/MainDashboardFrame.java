package com.timemanagement.ui;

import com.timemanagement.exception.AttendanceException;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.exception.TimeManagementException;
import com.timemanagement.model.AttendanceRecord;
import com.timemanagement.model.LeaveRequest;
import com.timemanagement.model.User;
import com.timemanagement.model.enums.LeaveStatus;
import com.timemanagement.model.enums.LeaveType;
import com.timemanagement.service.AttendanceService;
import com.timemanagement.service.LeaveService;
import com.timemanagement.service.ReportService;
import com.timemanagement.thread.AsyncDataWorker;
import com.timemanagement.thread.LiveClockThread;
import com.timemanagement.ui.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Main Application Dashboard Frame.
 * Incorporates LiveClockThread (Multithreading), AsyncDataWorker, and role-based panels.
 */
public class MainDashboardFrame extends JFrame {

    private final User currentUser;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final ReportService reportService;

    // Multithreading Live Clock & Shift Stopwatch
    private LiveClockThread clockThread;
    private JLabel clockLabel;
    private JLabel shiftTimerLabel;

    // Attendance Tab Components
    private JButton checkInButton;
    private JButton checkOutButton;
    private JTextField attendanceNotesField;
    private JLabel attendanceStatusBadge;
    private DefaultTableModel attendanceTableModel;
    private JTable attendanceTable;

    // Leave Tab Components
    private JLabel leaveBalanceLabel;
    private JComboBox<LeaveType> leaveTypeCombo;
    private JTextField leaveStartField;
    private JTextField leaveEndField;
    private JTextField leaveReasonField;
    private DefaultTableModel leaveTableModel;
    private JTable leaveTable;

    // Manager / Admin Approval Tab Components
    private DefaultTableModel pendingLeaveModel;
    private JTable pendingLeaveTable;
    private DefaultTableModel teamAttendanceModel;
    private JTable teamAttendanceTable;

    // Reports Tab Components
    private JTextArea reportTextArea;
    private JLabel totalHoursStatLabel;
    private JLabel totalOvertimeStatLabel;

    public MainDashboardFrame(User user) {
        this.currentUser = user;
        this.attendanceService = new AttendanceService();
        this.leaveService = new LeaveService();
        this.reportService = new ReportService();

        initUI();
        startClockThread();
        loadInitialDataAsync();
    }

    private void initUI() {
        setTitle("TimeTracker Pro - " + currentUser.getName() + " (" + currentUser.getRoleBadge() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 740);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_LIGHT);
        setLayout(new BorderLayout());

        // 1. Top Header Bar
        add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Central Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.HEADER_FONT);
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("⏱ Check-In / Check-Out", createAttendancePanel());
        tabbedPane.addTab("🏖 Leave Management", createLeavePanel());

        // Role-based authorization: Managers and Admins see the Team Management Tab
        if (currentUser.canApproveLeaves() || currentUser.canManageUsers()) {
            tabbedPane.addTab("👥 Team Approvals & Logs", createTeamManagementPanel());
        }

        tabbedPane.addTab("📊 Reports & Analytics", createReportsPanel());

        add(tabbedPane, BorderLayout.CENTER);

        // Window Closing Listener to cleanly stop background threads
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (clockThread != null) {
                    clockThread.stopClock();
                }
            }
        });
    }

    /**
     * Top Header Panel with User Profile, Real-Time Digital Clock, and Shift Stopwatch.
     */
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setBackground(UITheme.BG_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        // Left: Branding & User greeting
        JPanel userInfoPanel = new JPanel();
        userInfoPanel.setLayout(new BoxLayout(userInfoPanel, BoxLayout.Y_AXIS));
        userInfoPanel.setBackground(UITheme.BG_DARK);

        JLabel appTitle = new JLabel("⚡ TimeTracker Pro");
        appTitle.setFont(UITheme.SUBTITLE_FONT);
        appTitle.setForeground(Color.WHITE);

        String userSubtitle = String.format("Logged in: %s | Role: %s | Dept: %s",
                currentUser.getName(), currentUser.getRoleBadge(), currentUser.getDepartment());
        JLabel userDetails = new JLabel(userSubtitle);
        userDetails.setFont(UITheme.BODY_FONT);
        userDetails.setForeground(new Color(148, 163, 184));

        userInfoPanel.add(appTitle);
        userInfoPanel.add(Box.createVerticalStrut(3));
        userInfoPanel.add(userDetails);

        // Center: Live Clock & Shift Timer (Multithreading Demonstration)
        JPanel clockPanel = new JPanel();
        clockPanel.setLayout(new BoxLayout(clockPanel, BoxLayout.Y_AXIS));
        clockPanel.setBackground(UITheme.BG_DARK);

        clockLabel = new JLabel("⏰ Current Time: --:--:--", SwingConstants.CENTER);
        clockLabel.setFont(UITheme.HEADER_FONT);
        clockLabel.setForeground(new Color(56, 189, 248)); // Sky Blue
        clockLabel.setAlignmentX(CENTER_ALIGNMENT);

        shiftTimerLabel = new JLabel("Status: Not Clocked In", SwingConstants.CENTER);
        shiftTimerLabel.setFont(UITheme.BOLD_FONT);
        shiftTimerLabel.setForeground(new Color(74, 222, 128)); // Bright Green
        shiftTimerLabel.setAlignmentX(CENTER_ALIGNMENT);

        clockPanel.add(clockLabel);
        clockPanel.add(Box.createVerticalStrut(3));
        clockPanel.add(shiftTimerLabel);

        // Right: Logout Button
        JButton logoutBtn = UITheme.createStyledButton("Log Out", UITheme.ACCENT_DANGER, Color.WHITE);
        logoutBtn.addActionListener(e -> logout());

        header.add(userInfoPanel, BorderLayout.WEST);
        header.add(clockPanel, BorderLayout.CENTER);
        header.add(logoutBtn, BorderLayout.EAST);

        return header;
    }

    /**
     * Tab 1: Attendance punch actions and personal history table.
     */
    private JPanel createAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        // Top Action Card (Punch In / Punch Out Controls)
        JPanel actionCard = new JPanel(new BorderLayout(15, 10));
        actionCard.setBackground(UITheme.CARD_BG);
        actionCard.setBorder(UITheme.createCardBorder());

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusRow.setBackground(UITheme.CARD_BG);
        JLabel title = new JLabel("Daily Punch Card");
        title.setFont(UITheme.SUBTITLE_FONT);
        title.setForeground(UITheme.TEXT_DARK);
        attendanceStatusBadge = new JLabel(" Checking status... ");
        attendanceStatusBadge.setFont(UITheme.BOLD_FONT);
        attendanceStatusBadge.setOpaque(true);
        attendanceStatusBadge.setBackground(UITheme.BG_LIGHT);
        statusRow.add(title);
        statusRow.add(attendanceStatusBadge);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        buttonRow.setBackground(UITheme.CARD_BG);

        checkInButton = UITheme.createStyledButton("▶ Check In", UITheme.ACCENT_SUCCESS, Color.WHITE);
        checkInButton.setPreferredSize(new Dimension(150, 42));
        checkInButton.addActionListener(e -> performClockIn());

        checkOutButton = UITheme.createStyledButton("⏹ Check Out", UITheme.ACCENT_DANGER, Color.WHITE);
        checkOutButton.setPreferredSize(new Dimension(150, 42));
        checkOutButton.addActionListener(e -> performClockOut());

        JLabel notesLbl = new JLabel("Task/Notes (Optional):");
        notesLbl.setFont(UITheme.BODY_FONT);
        attendanceNotesField = new JTextField(20);
        attendanceNotesField.setFont(UITheme.BODY_FONT);

        buttonRow.add(checkInButton);
        buttonRow.add(checkOutButton);
        buttonRow.add(notesLbl);
        buttonRow.add(attendanceNotesField);

        actionCard.add(statusRow, BorderLayout.NORTH);
        actionCard.add(buttonRow, BorderLayout.CENTER);

        panel.add(actionCard, BorderLayout.NORTH);

        // Center: History Table
        JPanel tableCard = new JPanel(new BorderLayout(0, 10));
        tableCard.setBackground(UITheme.CARD_BG);
        tableCard.setBorder(UITheme.createCardBorder());

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setBackground(UITheme.CARD_BG);
        JLabel historyTitle = new JLabel("My Attendance History");
        historyTitle.setFont(UITheme.SUBTITLE_FONT);
        JButton refreshBtn = new JButton("🔄 Refresh Logs");
        refreshBtn.addActionListener(e -> loadAttendanceHistoryAsync());
        tableHeader.add(historyTitle, BorderLayout.WEST);
        tableHeader.add(refreshBtn, BorderLayout.EAST);

        String[] cols = {"Record ID", "Date", "Check-In", "Check-Out", "Hours Worked", "Overtime", "Status", "Notes"};
        attendanceTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        attendanceTable = new JTable(attendanceTableModel);
        attendanceTable.setRowHeight(28);
        attendanceTable.setFont(UITheme.BODY_FONT);
        attendanceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(attendanceTable), BorderLayout.CENTER);

        panel.add(tableCard, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Tab 2: Leave applications and personal leave history.
     */
    private JPanel createLeavePanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        // Form Card (Left/Top)
        JPanel applyCard = new JPanel();
        applyCard.setLayout(new BoxLayout(applyCard, BoxLayout.Y_AXIS));
        applyCard.setBackground(UITheme.CARD_BG);
        applyCard.setBorder(UITheme.createCardBorder());

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(UITheme.CARD_BG);
        JLabel formTitle = new JLabel("Apply for Time Off / Leave");
        formTitle.setFont(UITheme.SUBTITLE_FONT);
        leaveBalanceLabel = new JLabel("Available Balance: " + currentUser.getLeaveBalance() + " Days");
        leaveBalanceLabel.setFont(UITheme.BOLD_FONT);
        leaveBalanceLabel.setForeground(UITheme.PRIMARY);
        titleBar.add(formTitle, BorderLayout.WEST);
        titleBar.add(leaveBalanceLabel, BorderLayout.EAST);

        JPanel fieldsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        fieldsPanel.setBackground(UITheme.CARD_BG);

        leaveTypeCombo = new JComboBox<>(LeaveType.values());
        leaveTypeCombo.setFont(UITheme.BODY_FONT);

        leaveStartField = new JTextField(LocalDate.now().plusDays(1).toString(), 9);
        leaveEndField = new JTextField(LocalDate.now().plusDays(1).toString(), 9);
        leaveReasonField = new JTextField(15);

        JButton submitLeaveBtn = UITheme.createStyledButton("Submit Leave", UITheme.PRIMARY, Color.WHITE);
        submitLeaveBtn.addActionListener(e -> performLeaveApplication());

        fieldsPanel.add(new JLabel("Type:"));
        fieldsPanel.add(leaveTypeCombo);
        fieldsPanel.add(new JLabel("Start Date:"));
        fieldsPanel.add(leaveStartField);
        fieldsPanel.add(new JLabel("End Date:"));
        fieldsPanel.add(leaveEndField);
        fieldsPanel.add(new JLabel("Reason:"));
        fieldsPanel.add(leaveReasonField);
        fieldsPanel.add(submitLeaveBtn);

        applyCard.add(titleBar);
        applyCard.add(Box.createVerticalStrut(10));
        applyCard.add(fieldsPanel);

        panel.add(applyCard, BorderLayout.NORTH);

        // Leave Requests History Table
        JPanel tableCard = new JPanel(new BorderLayout(0, 10));
        tableCard.setBackground(UITheme.CARD_BG);
        tableCard.setBorder(UITheme.createCardBorder());

        JLabel myLeavesTitle = new JLabel("My Leave Applications");
        myLeavesTitle.setFont(UITheme.SUBTITLE_FONT);

        String[] cols = {"Leave ID", "Leave Type", "Start Date", "End Date", "Total Days", "Reason", "Status", "Applied At"};
        leaveTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        leaveTable = new JTable(leaveTableModel);
        leaveTable.setRowHeight(28);
        leaveTable.setFont(UITheme.BODY_FONT);

        tableCard.add(myLeavesTitle, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(leaveTable), BorderLayout.CENTER);

        panel.add(tableCard, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Tab 3: Manager / Admin Team Leave Approvals and Full Team Attendance.
     */
    private JPanel createTeamManagementPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 15, 15));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        // 1. Pending Approvals Card
        JPanel approvalsCard = new JPanel(new BorderLayout(0, 10));
        approvalsCard.setBackground(UITheme.CARD_BG);
        approvalsCard.setBorder(UITheme.createCardBorder());

        JPanel approvHeader = new JPanel(new BorderLayout());
        approvHeader.setBackground(UITheme.CARD_BG);
        JLabel approvTitle = new JLabel("Pending Team Leave Approvals");
        approvTitle.setFont(UITheme.SUBTITLE_FONT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setBackground(UITheme.CARD_BG);
        JButton approveBtn = UITheme.createStyledButton("✓ Approve Selected", UITheme.ACCENT_SUCCESS, Color.WHITE);
        approveBtn.addActionListener(e -> handleLeaveApproval(LeaveStatus.APPROVED));

        JButton rejectBtn = UITheme.createStyledButton("✕ Reject Selected", UITheme.ACCENT_DANGER, Color.WHITE);
        rejectBtn.addActionListener(e -> handleLeaveApproval(LeaveStatus.REJECTED));

        JButton refreshApprovBtn = new JButton("🔄 Refresh");
        refreshApprovBtn.addActionListener(e -> loadPendingLeavesAsync());

        btnRow.add(refreshApprovBtn);
        btnRow.add(approveBtn);
        btnRow.add(rejectBtn);

        approvHeader.add(approvTitle, BorderLayout.WEST);
        approvHeader.add(btnRow, BorderLayout.EAST);

        String[] pendingCols = {"Leave ID", "Employee", "Type", "Start Date", "End Date", "Days", "Reason", "Status"};
        pendingLeaveModel = new DefaultTableModel(pendingCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        pendingLeaveTable = new JTable(pendingLeaveModel);
        pendingLeaveTable.setRowHeight(28);
        pendingLeaveTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        approvalsCard.add(approvHeader, BorderLayout.NORTH);
        approvalsCard.add(new JScrollPane(pendingLeaveTable), BorderLayout.CENTER);

        // 2. Team Attendance Logs Card
        JPanel teamAttCard = new JPanel(new BorderLayout(0, 10));
        teamAttCard.setBackground(UITheme.CARD_BG);
        teamAttCard.setBorder(UITheme.createCardBorder());

        JPanel teamAttHeader = new JPanel(new BorderLayout());
        teamAttHeader.setBackground(UITheme.CARD_BG);
        JLabel teamAttTitle = new JLabel("All Employees Attendance Logs");
        teamAttTitle.setFont(UITheme.SUBTITLE_FONT);

        JButton refreshTeamAttBtn = new JButton("🔄 Refresh Team Logs");
        refreshTeamAttBtn.addActionListener(e -> loadTeamAttendanceAsync());
        teamAttHeader.add(teamAttTitle, BorderLayout.WEST);
        teamAttHeader.add(refreshTeamAttBtn, BorderLayout.EAST);

        String[] teamCols = {"Employee", "Date", "Check-In", "Check-Out", "Hours", "Overtime", "Status", "Notes"};
        teamAttendanceModel = new DefaultTableModel(teamCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        teamAttendanceTable = new JTable(teamAttendanceModel);
        teamAttendanceTable.setRowHeight(28);

        teamAttCard.add(teamAttHeader, BorderLayout.NORTH);
        teamAttCard.add(new JScrollPane(teamAttendanceTable), BorderLayout.CENTER);

        panel.add(approvalsCard);
        panel.add(teamAttCard);

        return panel;
    }

    /**
     * Tab 4: Summary reports and analytics preview.
     */
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        // Top Summary Cards
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 15, 0));
        statsRow.setBackground(UITheme.BG_LIGHT);

        totalHoursStatLabel = new JLabel("Total Hours: 0.0 hrs", SwingConstants.CENTER);
        totalHoursStatLabel.setFont(UITheme.SUBTITLE_FONT);
        totalHoursStatLabel.setOpaque(true);
        totalHoursStatLabel.setBackground(Color.WHITE);
        totalHoursStatLabel.setBorder(UITheme.createCardBorder());

        totalOvertimeStatLabel = new JLabel("Total Overtime: 0.0 hrs", SwingConstants.CENTER);
        totalOvertimeStatLabel.setFont(UITheme.SUBTITLE_FONT);
        totalOvertimeStatLabel.setOpaque(true);
        totalOvertimeStatLabel.setBackground(Color.WHITE);
        totalOvertimeStatLabel.setBorder(UITheme.createCardBorder());

        JLabel roleSummary = new JLabel("Role: " + currentUser.getRoleBadge(), SwingConstants.CENTER);
        roleSummary.setFont(UITheme.SUBTITLE_FONT);
        roleSummary.setOpaque(true);
        roleSummary.setBackground(Color.WHITE);
        roleSummary.setBorder(UITheme.createCardBorder());

        statsRow.add(totalHoursStatLabel);
        statsRow.add(totalOvertimeStatLabel);
        statsRow.add(roleSummary);

        panel.add(statsRow, BorderLayout.NORTH);

        // Center Report Preview Area
        JPanel previewCard = new JPanel(new BorderLayout(0, 10));
        previewCard.setBackground(UITheme.CARD_BG);
        previewCard.setBorder(UITheme.createCardBorder());

        JPanel previewHeader = new JPanel(new BorderLayout());
        previewHeader.setBackground(UITheme.CARD_BG);
        JLabel previewTitle = new JLabel("Formatted Summary Report Preview");
        previewTitle.setFont(UITheme.SUBTITLE_FONT);

        JButton exportBtn = UITheme.createStyledButton("💾 Export Report to File", UITheme.PRIMARY, Color.WHITE);
        exportBtn.addActionListener(e -> exportReportToFile());

        JButton refreshReportBtn = new JButton("🔄 Generate Fresh Report");
        refreshReportBtn.addActionListener(e -> loadReportDataAsync());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(UITheme.CARD_BG);
        actions.add(refreshReportBtn);
        actions.add(exportBtn);

        previewHeader.add(previewTitle, BorderLayout.WEST);
        previewHeader.add(actions, BorderLayout.EAST);

        reportTextArea = new JTextArea();
        reportTextArea.setFont(UITheme.MONO_FONT);
        reportTextArea.setEditable(false);
        reportTextArea.setBackground(new java.awt.Color(250, 250, 250));

        previewCard.add(previewHeader, BorderLayout.NORTH);
        previewCard.add(new JScrollPane(reportTextArea), BorderLayout.CENTER);

        panel.add(previewCard, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================================
    // Multithreading Live Clock Startup
    // =========================================================================
    private void startClockThread() {
        clockThread = new LiveClockThread(clockLabel, shiftTimerLabel);
        clockThread.start();
    }

    // =========================================================================
    // Asynchronous Background Data Loading (Prevents GUI Freeze)
    // =========================================================================
    private void loadInitialDataAsync() {
        checkTodayAttendanceStatusAsync();
        loadAttendanceHistoryAsync();
        loadLeaveHistoryAsync();
        if (currentUser.canApproveLeaves() || currentUser.canManageUsers()) {
            loadPendingLeavesAsync();
            loadTeamAttendanceAsync();
        }
        loadReportDataAsync();
    }

    private void checkTodayAttendanceStatusAsync() {
        new AsyncDataWorker<>(
                () -> {
                    try {
                        return attendanceService.getTodayRecord(currentUser.getUserId());
                    } catch (DatabaseOperationException e) {
                        return Optional.<AttendanceRecord>empty();
                    }
                },
                opt -> {
                    if (opt.isPresent()) {
                        AttendanceRecord rec = opt.get();
                        if (rec.getClockOut() != null) {
                            attendanceStatusBadge.setText(" Status: Shift Finished Today (" + rec.getHoursWorked() + " hrs) ");
                            attendanceStatusBadge.setForeground(UITheme.PRIMARY);
                            checkInButton.setEnabled(false);
                            checkOutButton.setEnabled(false);
                            clockThread.clearPunchInTime();
                        } else {
                            attendanceStatusBadge.setText(" Status: Checked In at " + rec.getClockIn() + " ");
                            attendanceStatusBadge.setForeground(UITheme.ACCENT_SUCCESS);
                            checkInButton.setEnabled(false);
                            checkOutButton.setEnabled(true);
                            clockThread.setPunchInTime(rec.getClockIn());
                        }
                    } else {
                        attendanceStatusBadge.setText(" Status: Not Checked In Today ");
                        attendanceStatusBadge.setForeground(UITheme.TEXT_MUTED);
                        checkInButton.setEnabled(true);
                        checkOutButton.setEnabled(false);
                        clockThread.clearPunchInTime();
                    }
                },
                null
        ).execute();
    }

    private void loadAttendanceHistoryAsync() {
        new AsyncDataWorker<>(
                () -> {
                    try {
                        return attendanceService.getUserAttendanceHistory(currentUser.getUserId());
                    } catch (DatabaseOperationException e) {
                        return List.<AttendanceRecord>of();
                    }
                },
                records -> {
                    attendanceTableModel.setRowCount(0);
                    for (AttendanceRecord r : records) {
                        attendanceTableModel.addRow(new Object[]{
                                r.getRecordId(),
                                r.getRecordDate(),
                                r.getClockIn(),
                                r.getClockOut() != null ? r.getClockOut() : "--:--",
                                r.getHoursWorked(),
                                r.getOvertimeHours(),
                                r.getStatus(),
                                r.getNotes() != null ? r.getNotes() : ""
                        });
                    }
                },
                null
        ).execute();
    }

    private void loadLeaveHistoryAsync() {
        new AsyncDataWorker<>(
                () -> {
                    try {
                        return leaveService.getUserLeaves(currentUser.getUserId());
                    } catch (DatabaseOperationException e) {
                        return List.<LeaveRequest>of();
                    }
                },
                leaves -> {
                    leaveTableModel.setRowCount(0);
                    for (LeaveRequest l : leaves) {
                        leaveTableModel.addRow(new Object[]{
                                l.getLeaveId(),
                                l.getLeaveType(),
                                l.getStartDate(),
                                l.getEndDate(),
                                l.getDaysCount(),
                                l.getReason(),
                                l.getStatus(),
                                l.getAppliedAt().toLocalDate()
                        });
                    }
                },
                null
        ).execute();
    }

    private void loadPendingLeavesAsync() {
        if (pendingLeaveModel == null) return;
        new AsyncDataWorker<>(
                () -> {
                    try {
                        return leaveService.getPendingLeaves();
                    } catch (DatabaseOperationException e) {
                        return List.<LeaveRequest>of();
                    }
                },
                leaves -> {
                    pendingLeaveModel.setRowCount(0);
                    for (LeaveRequest l : leaves) {
                        pendingLeaveModel.addRow(new Object[]{
                                l.getLeaveId(),
                                l.getUserName() != null ? l.getUserName() : "Emp #" + l.getUserId(),
                                l.getLeaveType(),
                                l.getStartDate(),
                                l.getEndDate(),
                                l.getDaysCount(),
                                l.getReason(),
                                l.getStatus()
                        });
                    }
                },
                null
        ).execute();
    }

    private void loadTeamAttendanceAsync() {
        if (teamAttendanceModel == null) return;
        new AsyncDataWorker<>(
                () -> {
                    try {
                        return attendanceService.getAllAttendanceRecords();
                    } catch (DatabaseOperationException e) {
                        return List.<AttendanceRecord>of();
                    }
                },
                records -> {
                    teamAttendanceModel.setRowCount(0);
                    for (AttendanceRecord r : records) {
                        teamAttendanceModel.addRow(new Object[]{
                                r.getUserName() != null ? r.getUserName() : "Emp #" + r.getUserId(),
                                r.getRecordDate(),
                                r.getClockIn(),
                                r.getClockOut() != null ? r.getClockOut() : "--:--",
                                r.getHoursWorked(),
                                r.getOvertimeHours(),
                                r.getStatus(),
                                r.getNotes() != null ? r.getNotes() : ""
                        });
                    }
                },
                null
        ).execute();
    }

    private void loadReportDataAsync() {
        new AsyncDataWorker<>(
                () -> {
                    try {
                        double totalHours = reportService.getTotalHoursWorked(currentUser.getUserId());
                        double totalOvertime = reportService.getTotalOvertimeHours(currentUser.getUserId());
                        String text = reportService.generateUserMonthlyReport(currentUser.getUserId(), currentUser.getName());
                        return new Object[]{totalHours, totalOvertime, text};
                    } catch (DatabaseOperationException e) {
                        return new Object[]{0.0, 0.0, "Could not generate report: " + e.getMessage()};
                    }
                },
                res -> {
                    Object[] data = (Object[]) res;
                    totalHoursStatLabel.setText(String.format("Total Hours: %.2f hrs", (Double) data[0]));
                    totalOvertimeStatLabel.setText(String.format("Total Overtime: %.2f hrs", (Double) data[1]));
                    reportTextArea.setText((String) data[2]);
                },
                null
        ).execute();
    }

    // =========================================================================
    // User Action Handlers
    // =========================================================================
    private void performClockIn() {
        String notes = attendanceNotesField.getText().trim();
        try {
            AttendanceRecord record = attendanceService.clockIn(currentUser, notes);
            JOptionPane.showMessageDialog(this,
                    "Checked in successfully at " + record.getClockIn() + "!",
                    "Check-In Confirmed", JOptionPane.INFORMATION_MESSAGE);
            checkTodayAttendanceStatusAsync();
            loadAttendanceHistoryAsync();
            loadReportDataAsync();
        } catch (AttendanceException | DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Check-In Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performClockOut() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clock out for today?",
                "Confirm Check-Out", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = attendanceService.clockOut(currentUser);
            if (ok) {
                JOptionPane.showMessageDialog(this,
                        "Checked out successfully! Total work hours updated.",
                        "Check-Out Confirmed", JOptionPane.INFORMATION_MESSAGE);
                checkTodayAttendanceStatusAsync();
                loadAttendanceHistoryAsync();
                loadReportDataAsync();
            }
        } catch (AttendanceException | DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Check-Out Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performLeaveApplication() {
        LeaveType type = (LeaveType) leaveTypeCombo.getSelectedItem();
        String startStr = leaveStartField.getText().trim();
        String endStr = leaveEndField.getText().trim();
        String reason = leaveReasonField.getText().trim();

        try {
            LocalDate start = LocalDate.parse(startStr);
            LocalDate end = LocalDate.parse(endStr);

            LeaveRequest req = leaveService.applyLeave(currentUser, type, start, end, reason);
            JOptionPane.showMessageDialog(this,
                    "Leave application for " + req.getDaysCount() + " day(s) submitted successfully!",
                    "Leave Application Submitted", JOptionPane.INFORMATION_MESSAGE);

            leaveReasonField.setText("");
            loadLeaveHistoryAsync();
            if (currentUser.canApproveLeaves()) {
                loadPendingLeavesAsync();
            }
        } catch (java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Dates must be formatted as YYYY-MM-DD (e.g. 2026-10-15)",
                    "Date Format Error", JOptionPane.ERROR_MESSAGE);
        } catch (TimeManagementException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Leave Application Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLeaveApproval(LeaveStatus newStatus) {
        int selectedRow = pendingLeaveTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a leave application from the table.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int leaveId = (int) pendingLeaveModel.getValueAt(selectedRow, 0);
        String empName = (String) pendingLeaveModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Confirm %s leave application #%d for %s?", newStatus, leaveId, empName),
                "Confirm Decision", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = leaveService.reviewLeave(leaveId, newStatus, currentUser);
            if (ok) {
                JOptionPane.showMessageDialog(this,
                        "Leave request #" + leaveId + " marked as " + newStatus + ".",
                        "Status Updated", JOptionPane.INFORMATION_MESSAGE);
                loadPendingLeavesAsync();
                loadLeaveHistoryAsync();
            }
        } catch (TimeManagementException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Review Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportReportToFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("Attendance_Report_" + currentUser.getName().replace(" ", "_") + ".txt"));
        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            try (PrintWriter out = new PrintWriter(target)) {
                out.print(reportTextArea.getText());
                JOptionPane.showMessageDialog(this, "Report exported successfully to: " + target.getAbsolutePath(),
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save file: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out?", "Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (clockThread != null) {
                clockThread.stopClock();
            }
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}

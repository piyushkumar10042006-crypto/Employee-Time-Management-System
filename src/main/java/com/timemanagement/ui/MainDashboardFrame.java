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
import com.timemanagement.ui.common.ModernTableFactory;
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
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Modern SaaS-Style Main Dashboard.
 * Features a navy sidebar, card-based navigation, live multithreaded shift clock,
 * responsive tables with status pill badges, search filters, and role-based permissions.
 */
public class MainDashboardFrame extends JFrame {

    // Card Names for CardLayout
    private static final String CARD_OVERVIEW = "CARD_OVERVIEW";
    private static final String CARD_ATTENDANCE = "CARD_ATTENDANCE";
    private static final String CARD_LEAVES = "CARD_LEAVES";
    private static final String CARD_TEAM_APPROVALS = "CARD_TEAM_APPROVALS";
    private static final String CARD_TEAM_LOGS = "CARD_TEAM_LOGS";
    private static final String CARD_REPORTS = "CARD_REPORTS";

    private final User currentUser;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final ReportService reportService;

    // Navigation and Layout
    private CardLayout contentCardLayout;
    private JPanel mainContentArea;
    private JLabel pageTitleLabel;
    private JLabel breadcrumbLabel;
    private final List<SidebarNavButton> navButtons = new ArrayList<>();

    // Multithreading Live Clock & Shift Stopwatch
    private LiveClockThread clockThread;
    private JLabel clockLabel;
    private JLabel shiftTimerLabel;

    // Overview Card Components
    private JLabel todayStatusBadge;
    private JLabel todayHoursStat;
    private JLabel monthlyHoursStat;
    private JLabel leaveBalanceStat;
    private JButton checkInButton;
    private JButton checkOutButton;
    private JTextField attendanceNotesField;
    private DefaultTableModel recentAttendanceModel;
    private JTable recentAttendanceTable;

    // Full Attendance History Table & Sorter
    private DefaultTableModel attendanceTableModel;
    private JTable attendanceTable;
    private TableRowSorter<DefaultTableModel> attendanceSorter;
    private JLabel attendanceCountBadge;

    // Leave Tab Components
    private JLabel leaveBalanceHeader;
    private JComboBox<LeaveType> leaveTypeCombo;
    private JTextField leaveStartField;
    private JTextField leaveEndField;
    private JTextField leaveReasonField;
    private DefaultTableModel leaveTableModel;
    private JTable leaveTable;

    // Manager / Admin Approvals Components
    private DefaultTableModel pendingLeaveModel;
    private JTable pendingLeaveTable;
    private TableRowSorter<DefaultTableModel> pendingLeaveSorter;
    private JLabel pendingCountBadge;

    // Manager / Admin Team Attendance Logs
    private DefaultTableModel teamAttendanceModel;
    private JTable teamAttendanceTable;
    private TableRowSorter<DefaultTableModel> teamAttendanceSorter;

    // Reports Tab Components
    private JTextArea reportTextArea;
    private JLabel reportTotalHoursLabel;
    private JLabel reportOvertimeLabel;

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
        setTitle("TimeTracker Pro — " + currentUser.getName() + " (" + currentUser.getRoleBadge() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_LIGHT);
        setLayout(new BorderLayout());

        // 1. Left Sidebar
        add(createSidebarPanel(), BorderLayout.WEST);

        // 2. Right Main Section (Top Bar + CardLayout Body)
        JPanel rightContainer = new JPanel(new BorderLayout());
        rightContainer.setBackground(UITheme.BG_LIGHT);

        rightContainer.add(createTopBar(), BorderLayout.NORTH);

        contentCardLayout = new CardLayout();
        mainContentArea = new JPanel(contentCardLayout);
        mainContentArea.setBackground(UITheme.BG_LIGHT);

        // Add views
        mainContentArea.add(createOverviewPanel(), CARD_OVERVIEW);
        mainContentArea.add(createAttendancePanel(), CARD_ATTENDANCE);
        mainContentArea.add(createLeavePanel(), CARD_LEAVES);

        if (currentUser.canApproveLeaves() || currentUser.canManageUsers()) {
            mainContentArea.add(createTeamApprovalsPanel(), CARD_TEAM_APPROVALS);
            mainContentArea.add(createTeamAttendancePanel(), CARD_TEAM_LOGS);
        }

        mainContentArea.add(createReportsPanel(), CARD_REPORTS);

        rightContainer.add(mainContentArea, BorderLayout.CENTER);
        add(rightContainer, BorderLayout.CENTER);

        // Set Default View
        switchView(CARD_OVERVIEW, "Overview & Daily Punch", "Dashboard / Overview");

        // Window closing hook
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
     * Modern SaaS Left Sidebar Navigation.
     */
    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(245, 0));
        sidebar.setBackground(UITheme.SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(30, 41, 59)));

        // Top Brand Header
        JPanel topBrand = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 18));
        topBrand.setBackground(UITheme.SIDEBAR_BG);

        JLabel brandIcon = new JLabel("⚡");
        brandIcon.setFont(UITheme.TITLE_FONT);
        brandIcon.setForeground(new Color(56, 189, 248)); // Sky Blue

        JLabel brandText = new JLabel("TimeTracker Pro");
        brandText.setFont(UITheme.TITLE_FONT);
        brandText.setForeground(Color.WHITE);

        topBrand.add(brandIcon);
        topBrand.add(brandText);

        // User Profile Mini-Card
        JPanel profileBox = new JPanel();
        profileBox.setLayout(new BoxLayout(profileBox, BoxLayout.Y_AXIS));
        profileBox.setBackground(UITheme.SIDEBAR_HOVER);
        profileBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 14, 0, 14),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel userNameLbl = new JLabel(currentUser.getName());
        userNameLbl.setFont(UITheme.BOLD_FONT);
        userNameLbl.setForeground(Color.WHITE);

        JLabel userDeptLbl = new JLabel(currentUser.getDepartment() + " Department");
        userDeptLbl.setFont(UITheme.SMALL_FONT);
        userDeptLbl.setForeground(UITheme.SIDEBAR_TEXT_MUTED);

        JLabel rolePill = UITheme.createPillBadge(currentUser.getRoleBadge(), new Color(30, 58, 138), new Color(147, 197, 253));

        profileBox.add(userNameLbl);
        profileBox.add(Box.createVerticalStrut(2));
        profileBox.add(userDeptLbl);
        profileBox.add(Box.createVerticalStrut(6));
        profileBox.add(rolePill);

        JPanel profileWrapper = new JPanel(new BorderLayout());
        profileWrapper.setBackground(UITheme.SIDEBAR_BG);
        profileWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        profileWrapper.add(profileBox, BorderLayout.CENTER);

        // Header Section (Brand + Profile)
        JPanel headerSection = new JPanel(new BorderLayout());
        headerSection.setBackground(UITheme.SIDEBAR_BG);
        headerSection.add(topBrand, BorderLayout.NORTH);
        headerSection.add(profileWrapper, BorderLayout.CENTER);

        // Navigation Items
        JPanel navMenu = new JPanel();
        navMenu.setLayout(new BoxLayout(navMenu, BoxLayout.Y_AXIS));
        navMenu.setBackground(UITheme.SIDEBAR_BG);
        navMenu.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        SidebarNavButton btnOverview = addNavItem(navMenu, "🏠 Overview & Punch", CARD_OVERVIEW, "Overview & Daily Punch", "Dashboard / Overview");
        btnOverview.setActive(true);

        addNavItem(navMenu, "📅 Attendance History", CARD_ATTENDANCE, "My Attendance History", "Dashboard / Attendance");
        addNavItem(navMenu, "🏖 Leave Management", CARD_LEAVES, "Time-Off & Leave Management", "Dashboard / Leaves");

        if (currentUser.canApproveLeaves() || currentUser.canManageUsers()) {
            JLabel mgrLabel = new JLabel("  TEAM MANAGEMENT");
            mgrLabel.setFont(UITheme.SMALL_BOLD);
            mgrLabel.setForeground(new Color(100, 116, 139));
            mgrLabel.setBorder(BorderFactory.createEmptyBorder(14, 8, 6, 8));
            navMenu.add(mgrLabel);

            addNavItem(navMenu, "👥 Pending Approvals", CARD_TEAM_APPROVALS, "Pending Leave Approvals", "Team / Approvals");
            addNavItem(navMenu, "📋 Team Attendance", CARD_TEAM_LOGS, "Team Attendance Logs", "Team / Attendance");
        }

        JLabel rptLabel = new JLabel("  ANALYTICS");
        rptLabel.setFont(UITheme.SMALL_BOLD);
        rptLabel.setForeground(new Color(100, 116, 139));
        rptLabel.setBorder(BorderFactory.createEmptyBorder(14, 8, 6, 8));
        navMenu.add(rptLabel);

        addNavItem(navMenu, "📊 Reports & Analytics", CARD_REPORTS, "Attendance Reports & Export", "Analytics / Reports");

        // Bottom Sign Out
        JPanel bottomSection = new JPanel(new BorderLayout());
        bottomSection.setBackground(UITheme.SIDEBAR_BG);
        bottomSection.setBorder(BorderFactory.createEmptyBorder(12, 14, 18, 14));

        JButton logoutBtn = new JButton("🚪 Sign Out");
        logoutBtn.setFont(UITheme.BOLD_FONT);
        logoutBtn.setForeground(new Color(248, 113, 113));
        logoutBtn.setBackground(new Color(30, 41, 59));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        logoutBtn.addActionListener(e -> logout());
        bottomSection.add(logoutBtn, BorderLayout.CENTER);

        sidebar.add(headerSection, BorderLayout.NORTH);
        sidebar.add(new JScrollPane(navMenu) {{
            setBorder(null);
            setOpaque(false);
            getViewport().setOpaque(false);
        }}, BorderLayout.CENTER);
        sidebar.add(bottomSection, BorderLayout.SOUTH);

        return sidebar;
    }

    private SidebarNavButton addNavItem(JPanel container, String title, String cardKey, String pageTitle, String breadcrumb) {
        SidebarNavButton btn = new SidebarNavButton(title, e -> {
            for (SidebarNavButton b : navButtons) b.setActive(false);
            SidebarNavButton source = (SidebarNavButton) e.getSource();
            source.setActive(true);
            switchView(cardKey, pageTitle, breadcrumb);
        });
        navButtons.add(btn);
        container.add(btn);
        container.add(Box.createVerticalStrut(4));
        return btn;
    }

    private void switchView(String cardKey, String pageTitle, String breadcrumb) {
        contentCardLayout.show(mainContentArea, cardKey);
        if (pageTitleLabel != null) pageTitleLabel.setText(pageTitle);
        if (breadcrumbLabel != null) breadcrumbLabel.setText(breadcrumb);
    }

    /**
     * Top Header Bar with dynamic breadcrumbs, live system clock, and shift duration.
     */
    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(15, 0));
        topBar.setPreferredSize(new Dimension(0, 64));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 24, 10, 24)
        ));

        // Left Titles
        JPanel titlesPanel = new JPanel();
        titlesPanel.setLayout(new BoxLayout(titlesPanel, BoxLayout.Y_AXIS));
        titlesPanel.setOpaque(false);

        breadcrumbLabel = new JLabel("Dashboard / Overview");
        breadcrumbLabel.setFont(UITheme.SMALL_FONT);
        breadcrumbLabel.setForeground(UITheme.TEXT_MUTED);

        pageTitleLabel = new JLabel("Overview & Daily Punch");
        pageTitleLabel.setFont(UITheme.TITLE_FONT);
        pageTitleLabel.setForeground(UITheme.TEXT_DARK);

        titlesPanel.add(breadcrumbLabel);
        titlesPanel.add(Box.createVerticalStrut(2));
        titlesPanel.add(pageTitleLabel);

        // Right Multithreaded Live Clock Widgets
        JPanel rightWidgets = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        rightWidgets.setOpaque(false);

        clockLabel = new JLabel("⏰ Loading Clock...");
        clockLabel.setFont(UITheme.BOLD_FONT);
        clockLabel.setForeground(UITheme.PRIMARY);

        shiftTimerLabel = UITheme.createPillBadge("Status: Not Clocked In", UITheme.PRIMARY_LIGHT, UITheme.PRIMARY);
        shiftTimerLabel.setFont(UITheme.BOLD_FONT);

        rightWidgets.add(clockLabel);
        rightWidgets.add(shiftTimerLabel);

        topBar.add(titlesPanel, BorderLayout.WEST);
        topBar.add(rightWidgets, BorderLayout.EAST);

        return topBar;
    }

    // =========================================================================
    // VIEW 1: Overview & Quick Punch Panel
    // =========================================================================
    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        // Top Welcome Banner
        JPanel welcomeBanner = UITheme.createCardPanel();
        welcomeBanner.setLayout(new BorderLayout(15, 0));
        welcomeBanner.setBackground(UITheme.PRIMARY_LIGHT);

        JPanel welcomeText = new JPanel();
        welcomeText.setLayout(new BoxLayout(welcomeText, BoxLayout.Y_AXIS));
        welcomeText.setOpaque(false);

        JLabel greetingLbl = new JLabel("Welcome back, " + currentUser.getName() + " 👋");
        greetingLbl.setFont(UITheme.HERO_FONT);
        greetingLbl.setForeground(UITheme.PRIMARY);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"));
        JLabel dateLbl = new JLabel("Today is " + dateStr + " — Keep up the productive momentum!");
        dateLbl.setFont(UITheme.BODY_FONT);
        dateLbl.setForeground(UITheme.TEXT_BODY);

        welcomeText.add(greetingLbl);
        welcomeText.add(Box.createVerticalStrut(4));
        welcomeText.add(dateLbl);
        welcomeBanner.add(welcomeText, BorderLayout.CENTER);

        // 4 KPI Summary Cards
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 14, 0));
        statsRow.setOpaque(false);

        todayStatusBadge = UITheme.createPillBadge("Checking...", UITheme.PRIMARY_LIGHT, UITheme.PRIMARY);
        statsRow.add(createStatCard("Today's Punch Status", todayStatusBadge, "Daily check-in state"));

        todayHoursStat = new JLabel("0.00 hrs");
        todayHoursStat.setFont(UITheme.TITLE_FONT);
        todayHoursStat.setForeground(UITheme.TEXT_DARK);
        statsRow.add(createStatCard("Active Hours Today", todayHoursStat, "Based on today's punches"));

        monthlyHoursStat = new JLabel("0.00 hrs");
        monthlyHoursStat.setFont(UITheme.TITLE_FONT);
        monthlyHoursStat.setForeground(UITheme.TEXT_DARK);
        statsRow.add(createStatCard("Total Logged Hours", monthlyHoursStat, "Total hours across all records"));

        leaveBalanceStat = new JLabel(currentUser.getLeaveBalance() + " Days");
        leaveBalanceStat.setFont(UITheme.TITLE_FONT);
        leaveBalanceStat.setForeground(UITheme.PRIMARY);
        statsRow.add(createStatCard("Available Leaves", leaveBalanceStat, "Annual leave balance"));

        // Header combining Welcome & Stats
        JPanel topContainer = new JPanel(new BorderLayout(0, 14));
        topContainer.setOpaque(false);
        topContainer.add(welcomeBanner, BorderLayout.NORTH);
        topContainer.add(statsRow, BorderLayout.CENTER);

        panel.add(topContainer, BorderLayout.NORTH);

        // Middle Punch Action Card
        JPanel punchCard = UITheme.createCardPanel();
        punchCard.setLayout(new BorderLayout(15, 12));

        JPanel punchHeader = new JPanel(new BorderLayout());
        punchHeader.setOpaque(false);
        JLabel punchTitle = new JLabel("Daily Punch Card");
        punchTitle.setFont(UITheme.SUBTITLE_FONT);
        punchTitle.setForeground(UITheme.TEXT_DARK);
        JLabel punchHint = new JLabel("Record your arrival and departure times accurately.");
        punchHint.setFont(UITheme.SMALL_FONT);
        punchHint.setForeground(UITheme.TEXT_MUTED);

        JPanel titleGroup = new JPanel();
        titleGroup.setLayout(new BoxLayout(titleGroup, BoxLayout.Y_AXIS));
        titleGroup.setOpaque(false);
        titleGroup.add(punchTitle);
        titleGroup.add(punchHint);
        punchHeader.add(titleGroup, BorderLayout.WEST);

        JPanel controlsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        controlsRow.setOpaque(false);

        checkInButton = UITheme.createSuccessButton("▶  Check In Now");
        checkInButton.setPreferredSize(new Dimension(170, 42));
        checkInButton.addActionListener(e -> performClockIn());

        checkOutButton = UITheme.createDangerButton("⏹  Check Out for Today");
        checkOutButton.setPreferredSize(new Dimension(190, 42));
        checkOutButton.addActionListener(e -> performClockOut());

        JLabel notesLabel = new JLabel("Task Notes (Optional):");
        notesLabel.setFont(UITheme.BODY_FONT);

        attendanceNotesField = new JTextField(24);
        attendanceNotesField.setFont(UITheme.BODY_FONT);
        attendanceNotesField.setBorder(UITheme.createFieldBorder());

        controlsRow.add(checkInButton);
        controlsRow.add(checkOutButton);
        controlsRow.add(Box.createHorizontalStrut(10));
        controlsRow.add(notesLabel);
        controlsRow.add(attendanceNotesField);

        punchCard.add(punchHeader, BorderLayout.NORTH);
        punchCard.add(controlsRow, BorderLayout.CENTER);

        // Bottom: Recent Attendance Mini-Table
        JPanel recentCard = UITheme.createCardPanel();
        recentCard.setLayout(new BorderLayout(0, 10));

        JPanel recentHeader = new JPanel(new BorderLayout());
        recentHeader.setOpaque(false);
        JLabel recentTitle = new JLabel("Recent Punch Activity");
        recentTitle.setFont(UITheme.SUBTITLE_FONT);
        JButton viewAllBtn = UITheme.createSecondaryButton("View Full History →");
        viewAllBtn.addActionListener(e -> {
            for (SidebarNavButton b : navButtons) b.setActive(false);
            if (navButtons.size() > 1) navButtons.get(1).setActive(true);
            switchView(CARD_ATTENDANCE, "My Attendance History", "Dashboard / Attendance");
        });
        recentHeader.add(recentTitle, BorderLayout.WEST);
        recentHeader.add(viewAllBtn, BorderLayout.EAST);

        String[] cols = {"Date", "Check-In", "Check-Out", "Hours Worked", "Overtime", "Status", "Notes"};
        recentAttendanceModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        recentAttendanceTable = new JTable(recentAttendanceModel);
        JScrollPane recentScroll = ModernTableFactory.createModernScrollPane(recentAttendanceTable);

        recentCard.add(recentHeader, BorderLayout.NORTH);
        recentCard.add(recentScroll, BorderLayout.CENTER);

        // Center Content wrapper
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 14));
        centerWrapper.setOpaque(false);
        centerWrapper.add(punchCard, BorderLayout.NORTH);
        centerWrapper.add(recentCard, BorderLayout.CENTER);

        panel.add(centerWrapper, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueComponent, String subtitle) {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 6));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.SMALL_BOLD);
        titleLbl.setForeground(UITheme.TEXT_MUTED);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(UITheme.SMALL_FONT);
        subLbl.setForeground(UITheme.TEXT_MUTED);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        center.setOpaque(false);
        center.add(valueComponent);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);

        return card;
    }

    // =========================================================================
    // VIEW 2: Full Attendance History Panel (with Search & Filters)
    // =========================================================================
    private JPanel createAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 12));

        // Top Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftTools.setOpaque(false);

        JLabel searchIcon = new JLabel("🔍");
        JTextField searchField = new JTextField(18);
        searchField.setFont(UITheme.BODY_FONT);
        searchField.setBorder(UITheme.createFieldBorder());

        leftTools.add(searchIcon);
        leftTools.add(searchField);

        attendanceCountBadge = UITheme.createPillBadge("0 Records", UITheme.PRIMARY_LIGHT, UITheme.PRIMARY);
        leftTools.add(attendanceCountBadge);

        JButton refreshBtn = UITheme.createSecondaryButton("🔄 Refresh Logs");
        refreshBtn.addActionListener(e -> loadAttendanceHistoryAsync());

        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(refreshBtn, BorderLayout.EAST);

        // Table
        String[] cols = {"Record ID", "Date", "Check-In", "Check-Out", "Hours Worked", "Overtime", "Status", "Notes"};
        attendanceTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        attendanceTable = new JTable(attendanceTableModel);
        attendanceSorter = new TableRowSorter<>(attendanceTableModel);
        attendanceTable.setRowSorter(attendanceSorter);
        attendanceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Live Filter Listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilter(); }

            private void applyFilter() {
                String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    attendanceSorter.setRowFilter(null);
                } else {
                    attendanceSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        JScrollPane scrollPane = ModernTableFactory.createModernScrollPane(attendanceTable);

        card.add(toolbar, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // VIEW 3: Leave Management Panel
    // =========================================================================
    private JPanel createLeavePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        // Top Form & Balance Card
        JPanel topSection = new JPanel(new GridLayout(1, 2, 16, 0));
        topSection.setOpaque(false);

        // Apply Form Card
        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new BorderLayout(0, 12));

        JLabel formTitle = new JLabel("Submit Time-Off Application");
        formTitle.setFont(UITheme.SUBTITLE_FONT);
        formTitle.setForeground(UITheme.TEXT_DARK);

        JPanel formGrid = new JPanel(new GridLayout(4, 2, 10, 10));
        formGrid.setOpaque(false);

        leaveTypeCombo = new JComboBox<>(LeaveType.values());
        leaveTypeCombo.setFont(UITheme.BODY_FONT);

        leaveStartField = new JTextField(LocalDate.now().plusDays(1).toString());
        leaveStartField.setFont(UITheme.BODY_FONT);
        leaveStartField.setBorder(UITheme.createFieldBorder());

        leaveEndField = new JTextField(LocalDate.now().plusDays(1).toString());
        leaveEndField.setFont(UITheme.BODY_FONT);
        leaveEndField.setBorder(UITheme.createFieldBorder());

        leaveReasonField = new JTextField();
        leaveReasonField.setFont(UITheme.BODY_FONT);
        leaveReasonField.setBorder(UITheme.createFieldBorder());

        formGrid.add(new JLabel("Leave Type:"));
        formGrid.add(leaveTypeCombo);
        formGrid.add(new JLabel("Start Date (YYYY-MM-DD):"));
        formGrid.add(leaveStartField);
        formGrid.add(new JLabel("End Date (YYYY-MM-DD):"));
        formGrid.add(leaveEndField);
        formGrid.add(new JLabel("Reason for Absence:"));
        formGrid.add(leaveReasonField);

        JButton submitLeaveBtn = UITheme.createPrimaryButton("Submit Application");
        submitLeaveBtn.addActionListener(e -> performLeaveApplication());

        formCard.add(formTitle, BorderLayout.NORTH);
        formCard.add(formGrid, BorderLayout.CENTER);
        formCard.add(submitLeaveBtn, BorderLayout.SOUTH);

        // Leave Balance Summary Card
        JPanel balanceCard = UITheme.createCardPanel();
        balanceCard.setLayout(new BorderLayout(0, 10));

        leaveBalanceHeader = new JLabel(currentUser.getLeaveBalance() + " Days Remaining");
        leaveBalanceHeader.setFont(UITheme.HERO_FONT);
        leaveBalanceHeader.setForeground(UITheme.PRIMARY);

        JPanel balanceContent = new JPanel();
        balanceContent.setLayout(new BoxLayout(balanceContent, BoxLayout.Y_AXIS));
        balanceContent.setOpaque(false);

        JLabel infoTitle = new JLabel("Leave Policy & Entitlement");
        infoTitle.setFont(UITheme.SUBTITLE_FONT);

        JLabel bullet1 = new JLabel("• Casual & Medical leaves require standard manager review.");
        bullet1.setFont(UITheme.BODY_FONT);
        bullet1.setForeground(UITheme.TEXT_BODY);

        JLabel bullet2 = new JLabel("• Approved leave days deduct automatically from remaining balance.");
        bullet2.setFont(UITheme.BODY_FONT);
        bullet2.setForeground(UITheme.TEXT_BODY);

        JLabel bullet3 = new JLabel("• Applications cannot be backdated prior to today.");
        bullet3.setFont(UITheme.BODY_FONT);
        bullet3.setForeground(UITheme.TEXT_BODY);

        balanceContent.add(infoTitle);
        balanceContent.add(Box.createVerticalStrut(8));
        balanceContent.add(leaveBalanceHeader);
        balanceContent.add(Box.createVerticalStrut(10));
        balanceContent.add(bullet1);
        balanceContent.add(Box.createVerticalStrut(4));
        balanceContent.add(bullet2);
        balanceContent.add(Box.createVerticalStrut(4));
        balanceContent.add(bullet3);

        balanceCard.add(balanceContent, BorderLayout.CENTER);

        topSection.add(formCard);
        topSection.add(balanceCard);
        panel.add(topSection, BorderLayout.NORTH);

        // My Leaves Table
        JPanel tableCard = UITheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        JLabel myLeavesTitle = new JLabel("My Leave Applications & Status");
        myLeavesTitle.setFont(UITheme.SUBTITLE_FONT);

        String[] cols = {"Leave ID", "Type", "Start Date", "End Date", "Total Days", "Reason", "Status", "Applied Date"};
        leaveTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        leaveTable = new JTable(leaveTableModel);
        JScrollPane leaveScroll = ModernTableFactory.createModernScrollPane(leaveTable);

        tableCard.add(myLeavesTitle, BorderLayout.NORTH);
        tableCard.add(leaveScroll, BorderLayout.CENTER);

        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // VIEW 4: Manager Pending Approvals Panel
    // =========================================================================
    private JPanel createTeamApprovalsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 14));

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftGroup.setOpaque(false);

        JLabel icon = new JLabel("🔍");
        JTextField searchField = new JTextField(16);
        searchField.setFont(UITheme.BODY_FONT);
        searchField.setBorder(UITheme.createFieldBorder());

        pendingCountBadge = UITheme.createPillBadge("0 Pending", UITheme.WARNING_LIGHT, UITheme.WARNING_TEXT);

        leftGroup.add(icon);
        leftGroup.add(searchField);
        leftGroup.add(pendingCountBadge);

        JPanel actionGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionGroup.setOpaque(false);

        JButton refreshBtn = UITheme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadPendingLeavesAsync());

        JButton approveBtn = UITheme.createSuccessButton("✓ Approve Selected");
        approveBtn.addActionListener(e -> handleLeaveApproval(LeaveStatus.APPROVED));

        JButton rejectBtn = UITheme.createDangerButton("✕ Reject Selected");
        rejectBtn.addActionListener(e -> handleLeaveApproval(LeaveStatus.REJECTED));

        actionGroup.add(refreshBtn);
        actionGroup.add(approveBtn);
        actionGroup.add(rejectBtn);

        toolbar.add(leftGroup, BorderLayout.WEST);
        toolbar.add(actionGroup, BorderLayout.EAST);

        // Table
        String[] cols = {"Leave ID", "Employee", "Type", "Start Date", "End Date", "Days", "Reason", "Status"};
        pendingLeaveModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pendingLeaveTable = new JTable(pendingLeaveModel);
        pendingLeaveSorter = new TableRowSorter<>(pendingLeaveModel);
        pendingLeaveTable.setRowSorter(pendingLeaveSorter);
        pendingLeaveTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String t = searchField.getText().trim();
                pendingLeaveSorter.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + t));
            }
        });

        JScrollPane scroll = ModernTableFactory.createModernScrollPane(pendingLeaveTable);

        card.add(toolbar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // VIEW 5: Manager All Team Attendance Panel
    // =========================================================================
    private JPanel createTeamAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 12));

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel searchGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchGroup.setOpaque(false);

        JLabel searchIcon = new JLabel("🔍");
        JTextField searchField = new JTextField(18);
        searchField.setFont(UITheme.BODY_FONT);
        searchField.setBorder(UITheme.createFieldBorder());

        searchGroup.add(searchIcon);
        searchGroup.add(searchField);

        JButton refreshTeamBtn = UITheme.createSecondaryButton("🔄 Refresh Team Logs");
        refreshTeamBtn.addActionListener(e -> loadTeamAttendanceAsync());

        toolbar.add(searchGroup, BorderLayout.WEST);
        toolbar.add(refreshTeamBtn, BorderLayout.EAST);

        String[] cols = {"Employee", "Date", "Check-In", "Check-Out", "Hours Worked", "Overtime", "Status", "Notes"};
        teamAttendanceModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        teamAttendanceTable = new JTable(teamAttendanceModel);
        teamAttendanceSorter = new TableRowSorter<>(teamAttendanceModel);
        teamAttendanceTable.setRowSorter(teamAttendanceSorter);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String t = searchField.getText().trim();
                teamAttendanceSorter.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + t));
            }
        });

        JScrollPane scroll = ModernTableFactory.createModernScrollPane(teamAttendanceTable);

        card.add(toolbar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // VIEW 6: Reports & Analytics Panel
    // =========================================================================
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));

        // Top Summary Row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);

        reportTotalHoursLabel = new JLabel("0.00 hrs");
        reportTotalHoursLabel.setFont(UITheme.TITLE_FONT);
        reportTotalHoursLabel.setForeground(UITheme.TEXT_DARK);
        statsRow.add(createStatCard("Total Logged Hours", reportTotalHoursLabel, "Cumulative hours worked"));

        reportOvertimeLabel = new JLabel("0.00 hrs");
        reportOvertimeLabel.setFont(UITheme.TITLE_FONT);
        reportOvertimeLabel.setForeground(UITheme.PRIMARY);
        statsRow.add(createStatCard("Total Overtime Hours", reportOvertimeLabel, "Hours worked past 8.0 hrs/day"));

        JLabel userDeptRole = new JLabel(currentUser.getDepartment() + " • " + currentUser.getRoleBadge());
        userDeptRole.setFont(UITheme.TITLE_FONT);
        userDeptRole.setForeground(UITheme.TEXT_DARK);
        statsRow.add(createStatCard("Profile Summary", userDeptRole, "Department and role assignment"));

        panel.add(statsRow, BorderLayout.NORTH);

        // Center Report Preview Box
        JPanel previewCard = UITheme.createCardPanel();
        previewCard.setLayout(new BorderLayout(0, 12));

        JPanel previewHeader = new JPanel(new BorderLayout());
        previewHeader.setOpaque(false);

        JLabel previewTitle = new JLabel("Formatted Summary Report Preview");
        previewTitle.setFont(UITheme.SUBTITLE_FONT);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton refreshReportBtn = UITheme.createSecondaryButton("🔄 Generate Fresh Report");
        refreshReportBtn.addActionListener(e -> loadReportDataAsync());

        JButton exportBtn = UITheme.createPrimaryButton("💾 Export to File");
        exportBtn.addActionListener(e -> exportReportToFile());

        actions.add(refreshReportBtn);
        actions.add(exportBtn);

        previewHeader.add(previewTitle, BorderLayout.WEST);
        previewHeader.add(actions, BorderLayout.EAST);

        reportTextArea = new JTextArea();
        reportTextArea.setFont(UITheme.MONO_FONT);
        reportTextArea.setEditable(false);
        reportTextArea.setBackground(new Color(250, 250, 250));
        reportTextArea.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        previewCard.add(previewHeader, BorderLayout.NORTH);
        previewCard.add(new JScrollPane(reportTextArea) {{
            setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1));
        }}, BorderLayout.CENTER);

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
                            todayStatusBadge.setText("Shift Finished (" + rec.getHoursWorked() + " hrs)");
                            todayStatusBadge.setForeground(UITheme.PRIMARY);
                            todayHoursStat.setText(rec.getHoursWorked() + " hrs");
                            checkInButton.setEnabled(false);
                            checkOutButton.setEnabled(false);
                            clockThread.clearPunchInTime();
                        } else {
                            todayStatusBadge.setText("Checked In at " + rec.getClockIn());
                            todayStatusBadge.setForeground(UITheme.SUCCESS_TEXT);
                            todayHoursStat.setText("Shift in progress...");
                            checkInButton.setEnabled(false);
                            checkOutButton.setEnabled(true);
                            clockThread.setPunchInTime(rec.getClockIn());
                        }
                    } else {
                        todayStatusBadge.setText("Not Checked In Today");
                        todayStatusBadge.setForeground(UITheme.TEXT_MUTED);
                        todayHoursStat.setText("0.00 hrs");
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
                    recentAttendanceModel.setRowCount(0);

                    double totalHrs = 0.0;
                    int count = 0;

                    for (AttendanceRecord r : records) {
                        Object[] row = new Object[]{
                                r.getRecordId(),
                                r.getRecordDate(),
                                r.getClockIn() != null ? r.getClockIn() : "--:--",
                                r.getClockOut() != null ? r.getClockOut() : "--:--",
                                r.getHoursWorked(),
                                r.getOvertimeHours(),
                                r.getStatus(),
                                r.getNotes() != null ? r.getNotes() : ""
                        };
                        attendanceTableModel.addRow(row);
                        totalHrs += r.getHoursWorked();

                        // Populate top 5 into recent table
                        if (count < 5) {
                            recentAttendanceModel.addRow(new Object[]{
                                    r.getRecordDate(),
                                    r.getClockIn() != null ? r.getClockIn() : "--:--",
                                    r.getClockOut() != null ? r.getClockOut() : "--:--",
                                    r.getHoursWorked(),
                                    r.getOvertimeHours(),
                                    r.getStatus(),
                                    r.getNotes() != null ? r.getNotes() : ""
                            });
                            count++;
                        }
                    }

                    monthlyHoursStat.setText(String.format("%.2f hrs", totalHrs));
                    if (attendanceCountBadge != null) {
                        attendanceCountBadge.setText(records.size() + " Records");
                    }
                },
                null
        ).execute();
    }

    private void loadLeaveHistoryAsync() {
        new AsyncDataWorker<>(
                () -> {
                    try {
                        new com.timemanagement.dao.UserDAO().findById(currentUser.getUserId())
                                .ifPresent(u -> currentUser.setLeaveBalance(u.getLeaveBalance()));
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
                    if (leaveBalanceStat != null) {
                        leaveBalanceStat.setText(currentUser.getLeaveBalance() + " Days");
                    }
                    if (leaveBalanceHeader != null) {
                        leaveBalanceHeader.setText(currentUser.getLeaveBalance() + " Days Remaining");
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
                    if (pendingCountBadge != null) {
                        pendingCountBadge.setText(leaves.size() + " Pending");
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
                    reportTotalHoursLabel.setText(String.format("%.2f hrs", (Double) data[0]));
                    reportOvertimeLabel.setText(String.format("%.2f hrs", (Double) data[1]));
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

        int modelRow = pendingLeaveTable.convertRowIndexToModel(selectedRow);
        int leaveId = (int) pendingLeaveModel.getValueAt(modelRow, 0);
        String empName = (String) pendingLeaveModel.getValueAt(modelRow, 1);

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
                "Are you sure you want to sign out?", "Confirm Sign Out", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (clockThread != null) {
                clockThread.stopClock();
            }
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    // =========================================================================
    // Sidebar Navigation Button Helper Component
    // =========================================================================
    private static class SidebarNavButton extends JButton {
        private boolean isActive = false;

        public SidebarNavButton(String text, java.awt.event.ActionListener listener) {
            super(text);
            setFont(UITheme.BOLD_FONT);
            setHorizontalAlignment(SwingConstants.LEFT);
            setForeground(UITheme.SIDEBAR_TEXT);
            setBackground(UITheme.SIDEBAR_BG);
            setFocusPainted(false);
            setOpaque(true);
            setContentAreaFilled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            addActionListener(listener);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isActive) {
                        setBackground(UITheme.SIDEBAR_HOVER);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isActive) {
                        setBackground(UITheme.SIDEBAR_BG);
                    }
                }
            });
        }

        public void setActive(boolean active) {
            this.isActive = active;
            if (active) {
                setBackground(UITheme.SIDEBAR_HOVER);
                setForeground(new Color(56, 189, 248)); // Sky Blue Active Accent
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(56, 189, 248)),
                        BorderFactory.createEmptyBorder(10, 13, 10, 16)
                ));
            } else {
                setBackground(UITheme.SIDEBAR_BG);
                setForeground(UITheme.SIDEBAR_TEXT);
                setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            }
        }
    }
}

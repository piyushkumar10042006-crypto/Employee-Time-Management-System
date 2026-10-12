package com.timemanagement.ui;

import com.timemanagement.exception.AuthenticationException;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.model.User;
import com.timemanagement.service.AuthService;
import com.timemanagement.thread.AsyncDataWorker;
import com.timemanagement.ui.common.UITheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

/**
 * Modern, professional SaaS-style Login Window.
 * Features show/hide password toggle, loading feedback, inline error banner,
 * and quick-fill demo buttons for evaluation.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton togglePasswordBtn;
    private JPanel errorBanner;
    private JLabel errorTextLabel;
    private JButton loginButton;
    private boolean isPasswordVisible = false;

    public LoginFrame() {
        this.authService = new AuthService();
        initUI();
    }

    private void initUI() {
        setTitle("TimeTracker Pro — Workplace Authentication");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(540, 620);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(UITheme.BG_LIGHT);

        setLayout(new GridBagLayout()); // Perfectly centers the card on screen

        // Main Login Card
        JPanel cardPanel = UITheme.createCardPanel();
        cardPanel.setPreferredSize(new Dimension(450, 540));
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

        // 1. Branding Header
        JPanel brandHeader = new JPanel();
        brandHeader.setLayout(new BoxLayout(brandHeader, BoxLayout.Y_AXIS));
        brandHeader.setOpaque(false);
        brandHeader.setAlignmentX(CENTER_ALIGNMENT);

        JLabel logoBadge = new JLabel("⏱", SwingConstants.CENTER);
        logoBadge.setFont(UITheme.HERO_FONT);
        logoBadge.setForeground(UITheme.PRIMARY);
        logoBadge.setAlignmentX(CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("TimeTracker Pro", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.HERO_FONT);
        titleLabel.setForeground(UITheme.TEXT_DARK);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Employee Time Management System", SwingConstants.CENTER);
        subtitleLabel.setFont(UITheme.HEADER_FONT);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("Enter your credentials to access your dashboard", SwingConstants.CENTER);
        descLabel.setFont(UITheme.BODY_FONT);
        descLabel.setForeground(UITheme.TEXT_MUTED);
        descLabel.setAlignmentX(CENTER_ALIGNMENT);

        brandHeader.add(logoBadge);
        brandHeader.add(Box.createVerticalStrut(4));
        brandHeader.add(titleLabel);
        brandHeader.add(Box.createVerticalStrut(2));
        brandHeader.add(subtitleLabel);
        brandHeader.add(Box.createVerticalStrut(4));
        brandHeader.add(descLabel);

        // 2. Inline Error Banner (Hidden by default)
        errorBanner = new JPanel(new BorderLayout(8, 0));
        errorBanner.setBackground(UITheme.DANGER_LIGHT);
        errorBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(254, 202, 202), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        errorBanner.setMaximumSize(new Dimension(390, 40));
        errorBanner.setPreferredSize(new Dimension(390, 40));
        errorBanner.setAlignmentX(CENTER_ALIGNMENT);
        errorBanner.setVisible(false);

        JLabel errorIcon = new JLabel("⚠");
        errorIcon.setForeground(UITheme.DANGER_TEXT);
        errorIcon.setFont(UITheme.BOLD_FONT);

        errorTextLabel = new JLabel("");
        errorTextLabel.setFont(UITheme.BODY_FONT);
        errorTextLabel.setForeground(UITheme.DANGER_TEXT);

        errorBanner.add(errorIcon, BorderLayout.WEST);
        errorBanner.add(errorTextLabel, BorderLayout.CENTER);

        // 3. Email Input
        JLabel emailLabel = new JLabel("Work Email Address");
        emailLabel.setFont(UITheme.HEADER_FONT);
        emailLabel.setForeground(UITheme.TEXT_DARK);
        emailLabel.setAlignmentX(LEFT_ALIGNMENT);

        emailField = new JTextField("employee@company.com");
        emailField.setFont(UITheme.BODY_FONT);
        emailField.setBorder(UITheme.createFieldBorder());
        emailField.setMaximumSize(new Dimension(390, 40));
        emailField.setPreferredSize(new Dimension(390, 40));
        emailField.setAlignmentX(LEFT_ALIGNMENT);

        // 4. Password Input with Show/Hide toggle
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(UITheme.HEADER_FONT);
        passLabel.setForeground(UITheme.TEXT_DARK);
        passLabel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel passRow = new JPanel(new BorderLayout(4, 0));
        passRow.setOpaque(false);
        passRow.setMaximumSize(new Dimension(390, 40));
        passRow.setPreferredSize(new Dimension(390, 40));
        passRow.setAlignmentX(LEFT_ALIGNMENT);

        passwordField = new JPasswordField("employee123");
        passwordField.setFont(UITheme.BODY_FONT);
        passwordField.setBorder(UITheme.createFieldBorder());

        togglePasswordBtn = new JButton("👁 Show");
        togglePasswordBtn.setFont(UITheme.SMALL_BOLD);
        togglePasswordBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        togglePasswordBtn.setFocusPainted(false);
        togglePasswordBtn.setBackground(UITheme.CARD_HEADER_BG);
        togglePasswordBtn.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        togglePasswordBtn.addActionListener(e -> togglePasswordVisibility());

        passRow.add(passwordField, BorderLayout.CENTER);
        passRow.add(togglePasswordBtn, BorderLayout.EAST);

        // 5. Sign In Button with loading feedback
        loginButton = UITheme.createPrimaryButton("Sign In to Dashboard");
        loginButton.setMaximumSize(new Dimension(390, 44));
        loginButton.setPreferredSize(new Dimension(390, 44));
        loginButton.setAlignmentX(CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> performLoginAsync());

        // 6. Demo Credential Quick-Fill Bar
        JPanel demoBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        demoBar.setOpaque(false);
        demoBar.setAlignmentX(CENTER_ALIGNMENT);

        JLabel demoHint = new JLabel("Demo Autofill: ");
        demoHint.setFont(UITheme.SMALL_FONT);
        demoHint.setForeground(UITheme.TEXT_MUTED);
        demoBar.add(demoHint);

        JButton empDemo = new JButton("Employee");
        empDemo.setFont(UITheme.SMALL_BOLD);
        empDemo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        empDemo.addActionListener(e -> setDemoUser("employee@company.com", "employee123"));
        demoBar.add(empDemo);

        JButton mgrDemo = new JButton("Manager");
        mgrDemo.setFont(UITheme.SMALL_BOLD);
        mgrDemo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        mgrDemo.addActionListener(e -> setDemoUser("manager@company.com", "manager123"));
        demoBar.add(mgrDemo);

        JButton admDemo = new JButton("Admin");
        admDemo.setFont(UITheme.SMALL_BOLD);
        admDemo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        admDemo.addActionListener(e -> setDemoUser("admin@company.com", "admin123"));
        demoBar.add(admDemo);

        // Assembly
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(brandHeader);
        cardPanel.add(Box.createVerticalStrut(14));
        cardPanel.add(errorBanner);
        cardPanel.add(Box.createVerticalStrut(12));

        cardPanel.add(emailLabel);
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(emailField);
        cardPanel.add(Box.createVerticalStrut(14));

        cardPanel.add(passLabel);
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(passRow);
        cardPanel.add(Box.createVerticalStrut(18));

        cardPanel.add(loginButton);
        cardPanel.add(Box.createVerticalStrut(14));
        cardPanel.add(demoBar);

        add(cardPanel);

        // Enter key listener
        passwordField.addActionListener(e -> performLoginAsync());
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordField.setEchoChar('•');
            togglePasswordBtn.setText("👁 Show");
            isPasswordVisible = false;
        } else {
            passwordField.setEchoChar((char) 0);
            togglePasswordBtn.setText("🙈 Hide");
            isPasswordVisible = true;
        }
    }

    private void setDemoUser(String email, String pass) {
        emailField.setText(email);
        passwordField.setText(pass);
        hideError();
    }

    private void showError(String message) {
        errorTextLabel.setText(message);
        errorBanner.setVisible(true);
        revalidate();
        repaint();
    }

    private void hideError() {
        errorBanner.setVisible(false);
        revalidate();
        repaint();
    }

    private void performLoginAsync() {
        hideError();
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        loginButton.setText("Signing In...");
        loginButton.setEnabled(false);

        // Reuse AsyncDataWorker to authenticate asynchronously without freezing EDT
        new AsyncDataWorker<>(
                () -> {
                    try {
                        return authService.login(email, password);
                    } catch (AuthenticationException | DatabaseOperationException ex) {
                        throw new RuntimeException(ex.getMessage(), ex);
                    }
                },
                user -> {
                    dispose(); // Close login window
                    new MainDashboardFrame(user).setVisible(true);
                },
                error -> {
                    loginButton.setText("Sign In to Dashboard");
                    loginButton.setEnabled(true);
                    String msg = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
                    showError(msg);
                }
        ).execute();
    }
}

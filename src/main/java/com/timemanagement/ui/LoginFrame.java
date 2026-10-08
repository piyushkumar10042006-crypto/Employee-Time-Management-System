package com.timemanagement.ui;

import com.timemanagement.exception.AuthenticationException;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.model.User;
import com.timemanagement.service.AuthService;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

/**
 * Modern, responsive Login Frame.
 * Features quick-fill buttons for fast testing and demonstration during reviews.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JLabel errorLabel;
    private JButton loginButton;

    public LoginFrame() {
        this.authService = new AuthService();
        initUI();
    }

    private void initUI() {
        setTitle("Employee Time Management System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(UITheme.BG_LIGHT);

        setLayout(new GridBagLayout()); // Centers the login card on screen

        // Main Login Card
        JPanel cardPanel = new JPanel();
        cardPanel.setPreferredSize(new Dimension(440, 480));
        cardPanel.setBackground(UITheme.CARD_BG);
        cardPanel.setBorder(UITheme.createCardBorder());
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

        // Header Title
        JLabel titleLabel = new JLabel("TimeTracker Pro", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.TITLE_FONT);
        titleLabel.setForeground(UITheme.PRIMARY);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Employee Time Management System", SwingConstants.CENTER);
        subtitleLabel.setFont(UITheme.SUBTITLE_FONT);
        subtitleLabel.setForeground(UITheme.TEXT_DARK);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("Sign in to your workplace account", SwingConstants.CENTER);
        descLabel.setFont(UITheme.BODY_FONT);
        descLabel.setForeground(UITheme.TEXT_MUTED);
        descLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Input Form
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(UITheme.CARD_BG);
        formPanel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel emailLabel = new JLabel("Work Email Address");
        emailLabel.setFont(UITheme.HEADER_FONT);
        emailLabel.setForeground(UITheme.TEXT_DARK);

        emailField = new JTextField("employee@company.com");
        emailField.setFont(UITheme.BODY_FONT);
        emailField.setMaximumSize(new Dimension(380, 38));
        emailField.setPreferredSize(new Dimension(380, 38));

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(UITheme.HEADER_FONT);
        passLabel.setForeground(UITheme.TEXT_DARK);

        passwordField = new JPasswordField("employee123");
        passwordField.setFont(UITheme.BODY_FONT);
        passwordField.setMaximumSize(new Dimension(380, 38));
        passwordField.setPreferredSize(new Dimension(380, 38));

        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.BODY_FONT);
        errorLabel.setForeground(UITheme.ACCENT_DANGER);
        errorLabel.setAlignmentX(CENTER_ALIGNMENT);

        loginButton = UITheme.createStyledButton("Sign In to Dashboard", UITheme.PRIMARY, Color.WHITE);
        loginButton.setMaximumSize(new Dimension(380, 42));
        loginButton.setPreferredSize(new Dimension(380, 42));
        loginButton.setAlignmentX(CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> performLogin());

        // Quick demo credential fill bar
        JPanel demoBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        demoBar.setBackground(UITheme.CARD_BG);
        JLabel demoLabel = new JLabel("Demo Autofill: ");
        demoLabel.setFont(UITheme.BODY_FONT);
        demoLabel.setForeground(UITheme.TEXT_MUTED);
        demoBar.add(demoLabel);

        JButton empDemo = new JButton("Employee");
        empDemo.setFont(UITheme.BODY_FONT);
        empDemo.addActionListener(e -> setDemoUser("employee@company.com", "employee123"));
        demoBar.add(empDemo);

        JButton mgrDemo = new JButton("Manager");
        mgrDemo.setFont(UITheme.BODY_FONT);
        mgrDemo.addActionListener(e -> setDemoUser("manager@company.com", "manager123"));
        demoBar.add(mgrDemo);

        JButton admDemo = new JButton("Admin");
        admDemo.setFont(UITheme.BODY_FONT);
        admDemo.addActionListener(e -> setDemoUser("admin@company.com", "admin123"));
        demoBar.add(admDemo);

        // Assembly
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(titleLabel);
        cardPanel.add(Box.createVerticalStrut(4));
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createVerticalStrut(4));
        cardPanel.add(descLabel);
        cardPanel.add(Box.createVerticalStrut(20));

        cardPanel.add(emailLabel);
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(emailField);
        cardPanel.add(Box.createVerticalStrut(14));
        cardPanel.add(passLabel);
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(passwordField);
        cardPanel.add(Box.createVerticalStrut(8));
        cardPanel.add(errorLabel);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(loginButton);
        cardPanel.add(Box.createVerticalStrut(18));
        cardPanel.add(demoBar);

        add(cardPanel);

        // Enter key listener on password field
        passwordField.addActionListener(e -> performLogin());
    }

    private void setDemoUser(String email, String pass) {
        emailField.setText(email);
        passwordField.setText(pass);
        errorLabel.setText(" ");
    }

    private void performLogin() {
        errorLabel.setText("Authenticating...");
        loginButton.setEnabled(false);

        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        try {
            User user = authService.login(email, password);
            dispose(); // Close login window
            // Launch Main Dashboard
            new MainDashboardFrame(user).setVisible(true);
        } catch (AuthenticationException ex) {
            errorLabel.setText("⚠ " + ex.getMessage());
            loginButton.setEnabled(true);
        } catch (DatabaseOperationException ex) {
            errorLabel.setText("⚠ Database Error: " + ex.getMessage());
            loginButton.setEnabled(true);
        }
    }
}

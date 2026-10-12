package com.timemanagement.ui.common;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern SaaS UI Theme providing consistent styling, colors, typography,
 * and reusable component builders for the Employee Time Management System.
 */
public class UITheme {

    // Primary Brand & Accent Colors
    public static final Color PRIMARY = new Color(37, 99, 235);        // Royal Blue #2563EB
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216);  // Deep Blue #1D4ED8
    public static final Color PRIMARY_LIGHT = new Color(239, 246, 255); // Soft Blue Tint #EFF6FF

    public static final Color ACCENT_SUCCESS = new Color(16, 185, 129); // Emerald Green #10B981
    public static final Color SUCCESS_HOVER = new Color(5, 150, 105);
    public static final Color SUCCESS_LIGHT = new Color(236, 253, 245); // Soft Green Tint #ECFDF5
    public static final Color SUCCESS_TEXT = new Color(6, 95, 70);

    public static final Color ACCENT_DANGER = new Color(239, 68, 68);   // Crimson Red #EF4444
    public static final Color DANGER_HOVER = new Color(220, 38, 38);
    public static final Color DANGER_LIGHT = new Color(254, 242, 242);  // Soft Red Tint #FEF2F2
    public static final Color DANGER_TEXT = new Color(153, 27, 27);

    public static final Color ACCENT_WARNING = new Color(245, 158, 11); // Amber #F59E0B
    public static final Color WARNING_LIGHT = new Color(254, 243, 199); // Soft Amber Tint #FEF3C7
    public static final Color WARNING_TEXT = new Color(146, 64, 14);

    public static final Color ACCENT_INFO = new Color(14, 165, 233);    // Sky Blue #0EA5E9
    public static final Color INFO_LIGHT = new Color(240, 249, 255);
    public static final Color INFO_TEXT = new Color(7, 89, 133);

    // Sidebar & Backgrounds
    public static final Color SIDEBAR_BG = new Color(15, 23, 42);         // Charcoal Navy #0F172A
    public static final Color SIDEBAR_HOVER = new Color(30, 41, 59);      // #1E293B
    public static final Color SIDEBAR_ACTIVE = new Color(37, 99, 235);     // Blue Active
    public static final Color SIDEBAR_TEXT = new Color(226, 232, 240);    // #E2E8F0
    public static final Color SIDEBAR_TEXT_MUTED = new Color(148, 163, 184); // #94A3B8

    public static final Color BG_DARK = new Color(15, 23, 42);
    public static final Color BG_LIGHT = new Color(248, 250, 252);        // Pearl White #F8FAFC
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_HEADER_BG = new Color(248, 250, 252);
    public static final Color TEXT_DARK = new Color(15, 23, 42);          // Very Dark Slate #0F172A
    public static final Color TEXT_BODY = new Color(51, 65, 85);          // Slate #334155
    public static final Color TEXT_MUTED = new Color(100, 116, 139);      // Muted Gray #64748B
    public static final Color BORDER_COLOR = new Color(226, 232, 240);    // Border #E2E8F0
    public static final Color INPUT_BG = new Color(255, 255, 255);

    // Typography
    public static final Font HERO_FONT = new Font("SansSerif", Font.BOLD, 22);
    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    public static final Font SUBTITLE_FONT = new Font("SansSerif", Font.BOLD, 15);
    public static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 13);
    public static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BOLD_FONT = new Font("SansSerif", Font.BOLD, 13);
    public static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font SMALL_BOLD = new Font("SansSerif", Font.BOLD, 11);
    public static final Font MONO_FONT = new Font("Monospaced", Font.PLAIN, 12);

    // Borders
    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)
        );
    }

    public static Border createCompactCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        );
    }

    public static Border createFieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        );
    }

    // Button Builders with Modern Hover Effects
    public static JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text);
        btn.setFont(BOLD_FONT);
        btn.setBackground(bgColor);
        btn.setForeground(fgColor);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        btn.setOpaque(true);

        Color hoverColor = getHoverColor(bgColor);
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(bgColor);
                }
            }
        });

        return btn;
    }

    public static JButton createPrimaryButton(String text) {
        return createStyledButton(text, PRIMARY, Color.WHITE);
    }

    public static JButton createSuccessButton(String text) {
        return createStyledButton(text, ACCENT_SUCCESS, Color.WHITE);
    }

    public static JButton createDangerButton(String text) {
        return createStyledButton(text, ACCENT_DANGER, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(BOLD_FONT);
        btn.setBackground(Color.WHITE);
        btn.setForeground(TEXT_BODY);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(9, 16, 9, 16)
        ));
        btn.setOpaque(true);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(new Color(241, 245, 249));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(Color.WHITE);
                }
            }
        });

        return btn;
    }

    private static Color getHoverColor(Color c) {
        if (c.equals(PRIMARY)) return PRIMARY_HOVER;
        if (c.equals(ACCENT_SUCCESS)) return SUCCESS_HOVER;
        if (c.equals(ACCENT_DANGER)) return DANGER_HOVER;
        return c.darker();
    }

    /**
     * Creates a rounded card panel with subtle border.
     */
    public static JPanel createCardPanel() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        return card;
    }

    /**
     * Creates a pill status badge label.
     */
    public static JLabel createPillBadge(String text, Color bg, Color fg) {
        JLabel badge = new JLabel(text, JLabel.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(SMALL_BOLD);
        badge.setForeground(fg);
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        return badge;
    }
}

package com.timemanagement.ui.common;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

/**
 * Centralized UI Theme providing consistent styling, colors, and typography.
 * Fulfills UI/UX Design, Aesthetics, and Responsiveness criteria.
 */
public class UITheme {

    // Color Palette
    public static final Color PRIMARY = new Color(30, 58, 138);       // Navy Blue #1E3A8A
    public static final Color PRIMARY_HOVER = new Color(29, 78, 216); // Vibrant Blue #1D4ED8
    public static final Color ACCENT_SUCCESS = new Color(5, 150, 105); // Emerald Green #059669
    public static final Color ACCENT_DANGER = new Color(220, 38, 38);  // Crimson Red #DC2626
    public static final Color ACCENT_WARNING = new Color(217, 119, 6); // Amber #D97706
    
    public static final Color BG_DARK = new Color(15, 23, 42);         // Slate Charcoal #0F172A
    public static final Color BG_LIGHT = new Color(248, 250, 252);     // Pearl White #F8FAFC
    public static final Color CARD_BG = Color.WHITE;
    public static final Color TEXT_DARK = new Color(30, 41, 59);       // Deep Slate #1E293B
    public static final Color TEXT_MUTED = new Color(100, 116, 139);   // Gray #64748B
    public static final Color BORDER_COLOR = new Color(226, 232, 240); // Soft Border #E2E8F0

    // Fonts
    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 22);
    public static final Font SUBTITLE_FONT = new Font("SansSerif", Font.BOLD, 16);
    public static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 14);
    public static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BOLD_FONT = new Font("SansSerif", Font.BOLD, 13);
    public static final Font MONO_FONT = new Font("Monospaced", Font.PLAIN, 12);

    // Common Borders
    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        );
    }

    public static JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text);
        btn.setFont(BOLD_FONT);
        btn.setBackground(bgColor);
        btn.setForeground(fgColor);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }
}

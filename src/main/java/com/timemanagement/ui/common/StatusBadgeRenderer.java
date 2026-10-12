package com.timemanagement.ui.common;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Custom TableCellRenderer rendering status and type values as attractive rounded pill badges.
 */
public class StatusBadgeRenderer extends DefaultTableCellRenderer {

    private Color badgeBg = new Color(241, 245, 249);
    private Color badgeFg = new Color(71, 85, 105);

    public StatusBadgeRenderer() {
        setHorizontalAlignment(CENTER);
        setFont(UITheme.SMALL_BOLD);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        String text = value != null ? value.toString() : "";
        setText(text);

        // Map status/type to modern color tokens
        switch (text.toUpperCase()) {
            case "PRESENT":
            case "APPROVED":
                badgeBg = UITheme.SUCCESS_LIGHT;
                badgeFg = UITheme.SUCCESS_TEXT;
                break;
            case "LATE":
            case "PENDING":
                badgeBg = UITheme.WARNING_LIGHT;
                badgeFg = UITheme.WARNING_TEXT;
                break;
            case "HALF_DAY":
            case "VACATION":
                badgeBg = UITheme.PRIMARY_LIGHT;
                badgeFg = UITheme.PRIMARY;
                break;
            case "REJECTED":
            case "SICK":
                badgeBg = UITheme.DANGER_LIGHT;
                badgeFg = UITheme.DANGER_TEXT;
                break;
            case "ON_LEAVE":
            case "CASUAL":
                badgeBg = new Color(243, 232, 255); // Soft Violet
                badgeFg = new Color(126, 34, 206);
                break;
            default:
                badgeBg = new Color(241, 245, 249);
                badgeFg = UITheme.TEXT_MUTED;
                break;
        }

        setForeground(badgeFg);
        setOpaque(false); // Let custom rounded fill paint
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Center pill dimensions inside cell
        int pillHeight = 22;
        int pillWidth = Math.min(getWidth() - 16, Math.max(70, getFontMetrics(getFont()).stringWidth(getText()) + 20));
        int x = (getWidth() - pillWidth) / 2;
        int y = (getHeight() - pillHeight) / 2;

        g2.setColor(badgeBg);
        g2.fillRoundRect(x, y, pillWidth, pillHeight, 12, 12);
        g2.dispose();

        super.paintComponent(g);
    }
}

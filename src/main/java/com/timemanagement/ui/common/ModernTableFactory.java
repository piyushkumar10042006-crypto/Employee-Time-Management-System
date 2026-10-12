package com.timemanagement.ui.common;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Factory and styler for creating modern SaaS-style JTables.
 */
public class ModernTableFactory {

    public static void styleTable(JTable table) {
        table.setRowHeight(38);
        table.setFont(UITheme.BODY_FONT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(UITheme.PRIMARY_LIGHT);
        table.setSelectionForeground(UITheme.TEXT_DARK);

        // Header Styling
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
        header.setFont(UITheme.HEADER_FONT);
        header.setBackground(UITheme.CARD_HEADER_BG);
        header.setForeground(UITheme.TEXT_MUTED);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR));

        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);

        // Striped and padded cell renderer for normal columns
        TableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UITheme.TEXT_BODY);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
                return c;
            }
        };

        // Center aligned cell renderer for short values (IDs, hours, dates, times)
        TableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UITheme.TEXT_BODY);
                }
                setHorizontalAlignment(CENTER);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        };

        StatusBadgeRenderer badgeRenderer = new StatusBadgeRenderer();

        // Assign renderers according to column name
        for (int i = 0; i < table.getColumnCount(); i++) {
            String colName = table.getColumnName(i).toLowerCase();
            if (colName.contains("status") || colName.contains("type")) {
                table.getColumnModel().getColumn(i).setCellRenderer(badgeRenderer);
            } else if (colName.contains("id") || colName.contains("date") || colName.contains("hour")
                    || colName.contains("overtime") || colName.contains("days")
                    || colName.contains("check-in") || colName.contains("check-out")
                    || colName.contains("in") || colName.contains("out")) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            } else {
                table.getColumnModel().getColumn(i).setCellRenderer(defaultRenderer);
            }
        }
    }

    public static JScrollPane createModernScrollPane(JTable table) {
        styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }
}

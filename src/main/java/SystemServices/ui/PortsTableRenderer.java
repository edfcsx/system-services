package SystemServices.ui;

import SystemServices.model.PortEntry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import static SystemServices.ui.MainWindow.*;

public class PortsTableRenderer extends DefaultTableCellRenderer {

    private static final Font FONT_MONO    = new Font("Consolas", Font.BOLD,  12);
    private static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD,  12);
    private static final Font FONT_PLAIN   = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Color ROW_ALT     = new Color(32, 32, 50);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setBorder(new EmptyBorder(2, 12, 2, 8));
        setFont(FONT_PLAIN);

        if (isSelected) {
            setBackground(SELECTED_BG);
            setForeground(TEXT_PRIMARY);
            if (column == 1) { setFont(FONT_MONO); setHorizontalAlignment(SwingConstants.RIGHT); }
            else              { setHorizontalAlignment(SwingConstants.LEFT); }
            return this;
        }

        PortsTableModel model = (PortsTableModel) table.getModel();
        PortEntry entry = model.getPortAt(row);
        Color rowBg = (row % 2 == 0) ? BG_CARD : ROW_ALT;
        setBackground(rowBg);
        setHorizontalAlignment(SwingConstants.LEFT);

        switch (column) {
            case 0 -> {  // Protocol
                setFont(FONT_BOLD);
                setForeground(entry.isTcp() ? ACCENT_BLUE : ACCENT_AMBER);
            }
            case 1 -> {  // Port — right-aligned, monospaced
                setFont(FONT_MONO);
                setForeground(entry.isTcp() ? ACCENT_BLUE : ACCENT_AMBER);
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
            case 2 -> setForeground(TEXT_MUTED);   // Address
            case 3 -> {  // State
                setFont(FONT_BOLD);
                setForeground(TEXT_MUTED);
            }
            case 4 -> {  // PID
                setFont(FONT_MONO);
                setForeground(entry.hasProcess() ? TEXT_MUTED : new Color(60, 60, 90));
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
            case 5 -> {  // Process name
                setFont(entry.hasProcess() ? FONT_BOLD : FONT_PLAIN);
                setForeground(entry.hasProcess() ? TEXT_PRIMARY : new Color(70, 70, 100));
            }
            default -> setForeground(TEXT_PRIMARY);
        }

        return this;
    }
}

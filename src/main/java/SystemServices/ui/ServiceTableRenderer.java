package SystemServices.ui;

import SystemServices.model.Service;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import static SystemServices.ui.MainWindow.*;

public class ServiceTableRenderer extends DefaultTableCellRenderer {

    private static final Font FONT_UNIT    = new Font("Consolas",  Font.PLAIN, 12);
    private static final Font FONT_STATUS  = new Font("Segoe UI",  Font.BOLD,  12);
    private static final Font FONT_DEFAULT = new Font("Segoe UI",  Font.PLAIN, 12);
    private static final Color ROW_ALT     = new Color(32, 32, 50);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setBorder(new EmptyBorder(2, 12, 2, 8));

        if (isSelected) {
            setBackground(SELECTED_BG);
            setForeground(TEXT_PRIMARY);
            setFont(column == 0 ? FONT_UNIT : FONT_DEFAULT);
            return this;
        }

        ServiceTableModel model = (ServiceTableModel) table.getModel();
        Service service = model.getServiceAt(row);
        Color rowBg = (row % 2 == 0) ? BG_CARD : ROW_ALT;

        setBackground(rowBg);
        setFont(column == 0 ? FONT_UNIT : FONT_DEFAULT);

        if (service.isFailed()) {
            setForeground(column == 2 ? ACCENT_RED : new Color(200, 150, 150));
            if (column == 2) setFont(FONT_STATUS);

        } else if (service.isActive()) {
            if (column == 2) {
                setForeground(ACCENT_GREEN);
                setFont(FONT_STATUS);
            } else {
                setForeground(TEXT_PRIMARY);
            }

        } else {
            // inactive / dead / unknown
            setForeground(TEXT_MUTED);
            if (column == 2) setFont(FONT_STATUS);
        }

        return this;
    }
}

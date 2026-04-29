package SystemServices.ui;

import SystemServices.model.PortEntry;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class PortsTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Protocolo", "Porta", "Endereço", "Estado", "PID", "Processo"};

    private List<PortEntry> ports = new ArrayList<>();

    public void setPorts(List<PortEntry> ports) {
        this.ports = new ArrayList<>(ports);
        fireTableDataChanged();
    }

    public PortEntry getPortAt(int row) {
        return ports.get(row);
    }

    @Override public int getRowCount()    { return ports.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int col) { return COLUMNS[col]; }

    @Override
    public Object getValueAt(int row, int col) {
        PortEntry p = ports.get(row);
        return switch (col) {
            case 0 -> p.getProtocol().toUpperCase();
            case 1 -> p.getPort();
            case 2 -> p.getLocalAddress();
            case 3 -> p.getState();
            case 4 -> p.hasProcess() ? p.getPid() : "—";
            case 5 -> p.hasProcess() ? p.getProcessName() : "(sem permissão)";
            default -> "";
        };
    }

    @Override public boolean isCellEditable(int row, int col) { return false; }
    @Override public Class<?> getColumnClass(int col) { return col == 1 ? Integer.class : String.class; }
}

package SystemServices.ui;

import SystemServices.model.Service;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ServiceTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Serviço", "Carregamento", "Status", "Sub-estado", "Descrição"};

    private List<Service> services = new ArrayList<>();

    public void setServices(List<Service> services) {
        this.services = new ArrayList<>(services);
        fireTableDataChanged();
    }

    public Service getServiceAt(int row) {
        return services.get(row);
    }

    @Override
    public int getRowCount() {
        return services.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Service s = services.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> s.getUnit();
            case 1 -> s.getLoad();
            case 2 -> s.getActive();
            case 3 -> s.getSub();
            case 4 -> s.getDescription();
            default -> "";
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
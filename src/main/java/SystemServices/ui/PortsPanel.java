package SystemServices.ui;

import SystemServices.model.PortEntry;
import SystemServices.service.PortsCommand;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static SystemServices.ui.MainWindow.*;
import static SystemServices.ui.ServicesPanel.*;

public class PortsPanel extends JPanel {

    private final PortsCommand portsCmd = new PortsCommand();
    private final PortsTableModel tableModel = new PortsTableModel();

    private final Consumer<String> statusSetter;
    private final BiConsumer<Long, Long> chipsSetter;   // (portCount, processCount)

    private JTable table;
    private JTextField searchField;
    private JButton killBtn;

    private List<PortEntry> allPorts = List.of();

    public PortsPanel(Consumer<String> statusSetter, BiConsumer<Long, Long> chipsSetter) {
        this.statusSetter = statusSetter;
        this.chipsSetter  = chipsSetter;

        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);
    }

    // ── Toolbar ────────────────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(BG_DARK);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(12, 24, 12, 24)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JButton refreshBtn = buildButton("↺  Atualizar", ACCENT_BLUE, BG_DARK);
        killBtn = buildButton("✕  Encerrar Processo", ACCENT_RED, BG_DARK);
        killBtn.setEnabled(false);

        refreshBtn.addActionListener(e -> loadPorts());
        killBtn.addActionListener(e    -> killSelectedProcess());

        left.add(refreshBtn);
        left.add(buildDivider());
        left.add(killBtn);

        JLabel hint = new JLabel("  Duplo clique para detalhes");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(new Color(70, 70, 100));
        left.add(hint);

        searchField = buildField("Filtrar por processo, porta ou protocolo...", 26);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterPorts(); }
            public void removeUpdate(DocumentEvent e) { filterPorts(); }
            public void changedUpdate(DocumentEvent e) { filterPorts(); }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        JLabel icon = new JLabel("🔍  ");
        icon.setForeground(TEXT_MUTED);
        icon.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        right.add(icon);
        right.add(searchField);

        toolbar.add(left,  BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    // ── Table ──────────────────────────────────────────────────────────────

    private JScrollPane buildTable() {
        table = new JTable(tableModel);
        styleTable(table);

        TableColumnModel cols = table.getColumnModel();
        cols.getColumn(0).setPreferredWidth(70);
        cols.getColumn(1).setPreferredWidth(70);
        cols.getColumn(2).setPreferredWidth(180);
        cols.getColumn(3).setPreferredWidth(90);
        cols.getColumn(4).setPreferredWidth(80);
        cols.getColumn(5).setPreferredWidth(250);

        PortsTableRenderer renderer = new PortsTableRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) cols.getColumn(i).setCellRenderer(renderer);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                killBtn.setEnabled(row >= 0 && tableModel.getPortAt(row).hasProcess());
            }
        });

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0)
                    showDetails(tableModel.getPortAt(table.getSelectedRow()));
            }
        });

        return wrapInScroll(table);
    }

    // ── Data ───────────────────────────────────────────────────────────────

    public void loadPorts() {
        statusSetter.accept("Escaneando portas...");
        killBtn.setEnabled(false);

        new SwingWorker<List<PortEntry>, Void>() {
            @Override protected List<PortEntry> doInBackground() throws Exception {
                return portsCmd.listPorts();
            }
            @Override protected void done() {
                try {
                    allPorts = get();
                    filterPorts();
                } catch (InterruptedException | ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    statusSetter.accept("Erro ao escanear portas: " + cause.getMessage());
                    showError("Erro ao listar portas", cause.getMessage());
                }
            }
        }.execute();
    }

    private void filterPorts() {
        String filter = searchField.getText().toLowerCase().trim();

        List<PortEntry> visible = filter.isEmpty()
                ? allPorts
                : allPorts.stream()
                        .filter(p -> String.valueOf(p.getPort()).contains(filter)
                                  || p.getProcessName().toLowerCase().contains(filter)
                                  || p.getProtocol().toLowerCase().contains(filter)
                                  || p.getLocalAddress().toLowerCase().contains(filter))
                        .collect(Collectors.toList());

        tableModel.setPorts(visible);
        refreshStatus(visible.size());
    }

    private void refreshStatus(int visible) {
        long withProcess = allPorts.stream().filter(PortEntry::hasProcess).count();
        long tcpCount    = allPorts.stream().filter(PortEntry::isTcp).count();

        chipsSetter.accept((long) allPorts.size(), withProcess);
        statusSetter.accept(String.format(
                "Total: %d  ·  Exibindo: %d  ·  TCP: %d  ·  Com processo: %d",
                allPorts.size(), visible, tcpCount, withProcess
        ));
    }

    public void onActivated() {
        refreshStatus(tableModel.getRowCount());
    }

    // ── Kill process ───────────────────────────────────────────────────────

    private void killSelectedProcess() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        PortEntry entry = tableModel.getPortAt(row);
        if (!entry.hasProcess()) return;

        String msg = String.format(
                "Encerrar o processo  %s  (PID: %d)?\n\n" +
                "Isso vai liberar a porta  %d/%s.\n" +
                "O sinal SIGTERM será enviado (encerramento gracioso).",
                entry.getProcessName(), entry.getPid(),
                entry.getPort(), entry.getProtocol().toUpperCase()
        );

        if (JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this),
                msg, "Confirmar encerramento",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;

        killBtn.setEnabled(false);
        statusSetter.accept("Encerrando processo " + entry.getProcessName()
                + " (PID: " + entry.getPid() + ") …");

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                portsCmd.killProcess(entry.getPid());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    // Small delay before refresh so the OS releases the port
                    Timer t = new Timer(800, e -> loadPorts());
                    t.setRepeats(false);
                    t.start();
                } catch (InterruptedException | ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    statusSetter.accept("Erro ao encerrar processo.");
                    showError("Falha ao encerrar processo", cause.getMessage());
                    int sel = table.getSelectedRow();
                    killBtn.setEnabled(sel >= 0 && tableModel.getPortAt(sel).hasProcess());
                }
            }
        }.execute();
    }

    // ── Details dialog ─────────────────────────────────────────────────────

    private void showDetails(PortEntry e) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Detalhes da Porta",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(460, 260);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(BG_CARD);
        dlg.setLayout(new BorderLayout());

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(24, 28, 8, 28));

        String[][] rows = {
            {"Protocolo", e.getProtocol().toUpperCase()},
            {"Porta",     String.valueOf(e.getPort())},
            {"Endereço",  e.getLocalAddress()},
            {"Estado",    e.getState()},
            {"PID",       e.hasProcess() ? String.valueOf(e.getPid()) : "(desconhecido)"},
            {"Processo",  e.hasProcess() ? e.getProcessName() : "(sem permissão)"},
        };

        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.WEST;
        g.fill   = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < rows.length; i++) {
            JLabel key = new JLabel(rows[i][0]);
            key.setFont(new Font("Segoe UI", Font.BOLD, 12));
            key.setForeground(TEXT_MUTED);

            JLabel val = new JLabel(rows[i][1]);
            val.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            val.setForeground(TEXT_PRIMARY);

            g.gridx = 0; g.gridy = i; g.weightx = 0.3; g.insets = new Insets(0, 0, 9, 0);
            body.add(key, g);
            g.gridx = 1; g.weightx = 0.7; g.insets = new Insets(0, 16, 9, 0);
            body.add(val, g);
        }

        JButton closeBtn = buildButton("Fechar", TEXT_MUTED, BG_DARK);
        closeBtn.addActionListener(ev -> dlg.dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setOpaque(false);
        footer.add(closeBtn);

        dlg.add(body,   BorderLayout.CENTER);
        dlg.add(footer, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void showError(String title, String msg) {
        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                msg, title, JOptionPane.ERROR_MESSAGE);
    }

    private static JLabel buildDivider() {
        JLabel sep = new JLabel("  │  ");
        sep.setForeground(BORDER_COLOR);
        sep.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        return sep;
    }
}

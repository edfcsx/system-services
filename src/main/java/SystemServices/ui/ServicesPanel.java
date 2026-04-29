package SystemServices.ui;

import SystemServices.model.Service;
import SystemServices.service.SystemctlCommand;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static SystemServices.ui.MainWindow.*;

public class ServicesPanel extends JPanel {

    private final SystemctlCommand systemctl = new SystemctlCommand();
    private final ServiceTableModel tableModel = new ServiceTableModel();

    private final Consumer<String> statusSetter;
    private final BiConsumer<Long, Long> chipsSetter;   // (active, failed)

    private JTable table;
    private JTextField searchField;
    private JButton startBtn;
    private JButton stopBtn;
    private JButton restartBtn;

    private List<Service> allServices = List.of();

    public ServicesPanel(Consumer<String> statusSetter, BiConsumer<Long, Long> chipsSetter) {
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

        JButton refreshBtn = buildButton("↺  Atualizar",  ACCENT_BLUE,  BG_DARK);
        startBtn   = buildButton("▶  Iniciar",    ACCENT_GREEN, BG_DARK);
        stopBtn    = buildButton("■  Parar",       ACCENT_RED,   BG_DARK);
        restartBtn = buildButton("⟳  Reiniciar",  ACCENT_AMBER, BG_DARK);

        startBtn.setEnabled(false);
        stopBtn.setEnabled(false);
        restartBtn.setEnabled(false);

        refreshBtn.addActionListener(e -> loadServices());
        startBtn.addActionListener(e   -> performAction("start"));
        stopBtn.addActionListener(e    -> performAction("stop"));
        restartBtn.addActionListener(e -> performAction("restart"));

        left.add(refreshBtn);
        left.add(divider());
        left.add(startBtn);
        left.add(stopBtn);
        left.add(restartBtn);

        searchField = buildField("Filtrar serviços...", 22);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterServices(); }
            public void removeUpdate(DocumentEvent e) { filterServices(); }
            public void changedUpdate(DocumentEvent e) { filterServices(); }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(new SearchIcon());
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
        cols.getColumn(0).setPreferredWidth(240);
        cols.getColumn(1).setPreferredWidth(90);
        cols.getColumn(2).setPreferredWidth(80);
        cols.getColumn(3).setPreferredWidth(100);
        cols.getColumn(4).setPreferredWidth(500);

        ServiceTableRenderer renderer = new ServiceTableRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) cols.getColumn(i).setCellRenderer(renderer);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateButtons();
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0)
                    showDetails(tableModel.getServiceAt(table.getSelectedRow()));
            }
        });

        return wrapInScroll(table);
    }

    // ── Data ───────────────────────────────────────────────────────────────

    public void loadServices() {
        statusSetter.accept("Carregando serviços...");
        disableButtons();

        new SwingWorker<List<Service>, Void>() {
            @Override protected List<Service> doInBackground() throws Exception {
                return systemctl.listServices();
            }
            @Override protected void done() {
                try {
                    allServices = get();
                    filterServices();
                } catch (InterruptedException | ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    statusSetter.accept("Erro ao carregar: " + cause.getMessage());
                    showError("Erro ao listar serviços", cause.getMessage());
                }
            }
        }.execute();
    }

    private void filterServices() {
        String filter = searchField.getText().toLowerCase().trim();

        List<Service> visible = filter.isEmpty()
                ? allServices
                : allServices.stream()
                        .filter(s -> s.getUnit().toLowerCase().contains(filter)
                                  || s.getDescription().toLowerCase().contains(filter)
                                  || s.getActive().toLowerCase().contains(filter)
                                  || s.getSub().toLowerCase().contains(filter))
                        .collect(Collectors.toList());

        tableModel.setServices(visible);
        refreshStatus(visible.size());
    }

    private void refreshStatus(int visible) {
        long active = allServices.stream().filter(Service::isActive).count();
        long failed = allServices.stream().filter(Service::isFailed).count();

        chipsSetter.accept(active, failed);
        statusSetter.accept(String.format(
                "Total: %d  ·  Exibindo: %d  ·  Ativos: %d  ·  Falhas: %d",
                allServices.size(), visible, active, failed
        ));
    }

    public void onActivated() {
        refreshStatus(tableModel.getRowCount());
    }

    // ── Actions ────────────────────────────────────────────────────────────

    private void performAction(String action) {
        int row = table.getSelectedRow();
        if (row < 0) return;

        Service s = tableModel.getServiceAt(row);
        String label = switch (action) {
            case "start"   -> "iniciar";
            case "stop"    -> "parar";
            case "restart" -> "reiniciar";
            default        -> action;
        };

        if (JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this),
                "Deseja " + label + " o serviço:\n\n    " + s.getUnit(),
                "Confirmar ação", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        disableButtons();
        statusSetter.accept("Executando: " + label + " → " + s.getUnit() + " …");

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                switch (action) {
                    case "start"   -> systemctl.startService(s.getUnit());
                    case "stop"    -> systemctl.stopService(s.getUnit());
                    case "restart" -> systemctl.restartService(s.getUnit());
                }
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    loadServices();
                } catch (InterruptedException | ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    statusSetter.accept("Erro ao executar comando.");
                    showError("Falha ao " + label + " serviço", cause.getMessage());
                    updateButtons();
                }
            }
        }.execute();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void showDetails(Service s) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Detalhes do Serviço",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(520, 280);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.getContentPane().setBackground(BG_CARD);
        dlg.setLayout(new BorderLayout());

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(24, 28, 8, 28));

        String[][] rows = {
            {"Serviço",      s.getUnit()},
            {"Carregamento", s.getLoad()},
            {"Status",       s.getActive()},
            {"Sub-estado",   s.getSub()},
            {"Descrição",    s.getDescription()},
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

            g.gridx = 0; g.gridy = i; g.weightx = 0.28; g.insets = new Insets(0, 0, 10, 0);
            body.add(key, g);
            g.gridx = 1; g.weightx = 0.72; g.insets = new Insets(0, 16, 10, 0);
            body.add(val, g);
        }

        JButton closeBtn = buildButton("Fechar", TEXT_MUTED, BG_DARK);
        closeBtn.addActionListener(e -> dlg.dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setOpaque(false);
        footer.add(closeBtn);

        dlg.add(body,   BorderLayout.CENTER);
        dlg.add(footer, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void updateButtons() {
        boolean sel = table.getSelectedRow() >= 0;
        startBtn.setEnabled(sel);
        stopBtn.setEnabled(sel);
        restartBtn.setEnabled(sel);
    }

    private void disableButtons() {
        startBtn.setEnabled(false);
        stopBtn.setEnabled(false);
        restartBtn.setEnabled(false);
    }

    private void showError(String title, String msg) {
        JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                msg, title, JOptionPane.ERROR_MESSAGE);
    }

    // ── Shared widget builders ─────────────────────────────────────────────

    static JButton buildButton(String text, Color fg, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled())             g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 15));
                else if (getModel().isPressed()) g2.setColor(fg.darker());
                else if (getModel().isRollover()) g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 30));
                else                           g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fg, 1),
                new EmptyBorder(8, 20, 8, 20)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        btn.addPropertyChangeListener("enabled", evt -> {
            Color c = btn.isEnabled() ? fg : new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 50);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(c, 1), new EmptyBorder(8, 20, 8, 20)));
            btn.setForeground(btn.isEnabled() ? fg : new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 80));
        });
        return btn;
    }

    static JTextField buildField(String placeholder, int cols) {
        JTextField tf = new JTextField(cols);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(ACCENT_GREEN);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        tf.putClientProperty("JTextField.placeholderText", placeholder);
        return tf;
    }

    static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(SELECTED_BG);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(22, 22, 38));
        header.setForeground(TEXT_MUTED);
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        header.setPreferredSize(new Dimension(0, 34));
        header.setReorderingAllowed(false);
    }

    static JScrollPane wrapInScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBackground(BG_DARK);
        return scroll;
    }

    private static JLabel divider() {
        JLabel sep = new JLabel("  │  ");
        sep.setForeground(BORDER_COLOR);
        sep.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        return sep;
    }

    private static class SearchIcon extends JLabel {
        SearchIcon() {
            super("🔍  ");
            setForeground(TEXT_MUTED);
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }
    }
}

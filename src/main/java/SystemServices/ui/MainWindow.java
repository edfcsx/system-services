package SystemServices.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;

public class MainWindow extends JFrame {

    // ── Shared palette (used by renderer classes via static import) ────────
    public static final Color BG_DARK      = new Color(18,  18,  30);
    public static final Color BG_CARD      = new Color(28,  28,  45);
    public static final Color BG_INPUT     = new Color(38,  38,  58);
    public static final Color BORDER_COLOR = new Color(45,  45,  70);
    public static final Color ACCENT_GREEN = new Color(52,  211, 153);
    public static final Color ACCENT_RED   = new Color(248, 113, 113);
    public static final Color ACCENT_AMBER = new Color(251, 191,  36);
    public static final Color ACCENT_BLUE  = new Color(96,  165, 250);
    public static final Color TEXT_PRIMARY = new Color(236, 236, 245);
    public static final Color TEXT_MUTED   = new Color(120, 120, 160);
    public static final Color SELECTED_BG  = new Color(52,  52,  80);

    private JLabel chipLeft;
    private JLabel chipRight;
    private JLabel statusLabel;

    private ServicesPanel servicesPanel;
    private PortsPanel portsPanel;

    private TabButton servicesTabBtn;
    private TabButton portsTabBtn;
    private CardLayout cardLayout;
    private JPanel contentPanel;

    private int activeTab = 0;

    public MainWindow() {
        super("System Services");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1320, 780);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(980, 580));
        getContentPane().setBackground(BG_DARK);

        applyIcon();
        buildUI();
        servicesPanel.loadServices();   // initial load
    }

    private void applyIcon() {
        URL url = getClass().getResource("/icon.png");
        if (url == null) return;
        try {
            BufferedImage img = ImageIO.read(url);
            setIconImage(img);
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(img);
                }
            }
        } catch (Exception ignored) {}
    }

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(buildHeader(), BorderLayout.NORTH);
        north.add(buildTabBar(), BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        servicesPanel = new ServicesPanel(
                text -> setStatus(text),
                (active, failed) -> { if (activeTab == 0) setChips(
                        active + " ativos",  ACCENT_GREEN,
                        failed + " falhas",  ACCENT_RED); }
        );

        portsPanel = new PortsPanel(
                text -> setStatus(text),
                (ports, procs) -> { if (activeTab == 1) setChips(
                        ports + " portas",       ACCENT_BLUE,
                        procs + " com processo", ACCENT_AMBER); }
        );

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);
        contentPanel.add(servicesPanel, "services");
        contentPanel.add(portsPanel,    "ports");
        add(contentPanel, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ── Header ─────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(BG_CARD);
        header.setBorder(new EmptyBorder(18, 28, 18, 28));

        JLabel title = new JLabel("Gerenciador de Serviços");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Monitore e controle os serviços e portas do sistema operacional");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);

        chipLeft  = buildChip("—", ACCENT_GREEN);
        chipRight = buildChip("—", ACCENT_RED);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        chips.setOpaque(false);
        chips.add(chipLeft);
        chips.add(chipRight);

        header.add(titleBlock, BorderLayout.WEST);
        header.add(chips,      BorderLayout.EAST);

        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(BORDER_COLOR);
        sep.setBackground(BORDER_COLOR);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(header, BorderLayout.CENTER);
        wrapper.add(sep,    BorderLayout.SOUTH);
        return wrapper;
    }

    private JLabel buildChip(String text, Color color) {
        JLabel chip = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 28));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(color);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setForeground(color);
        chip.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chip.setBorder(new EmptyBorder(4, 12, 4, 12));
        chip.setOpaque(false);
        return chip;
    }

    // ── Tab bar ────────────────────────────────────────────────────────────

    private JPanel buildTabBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setBackground(BG_DARK);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        servicesTabBtn = new TabButton("  Serviços  ", () -> activeTab == 0);
        portsTabBtn    = new TabButton("  Portas  ",   () -> activeTab == 1);

        servicesTabBtn.addActionListener(e -> switchTab(0));
        portsTabBtn.addActionListener(e    -> switchTab(1));

        bar.add(servicesTabBtn);
        bar.add(portsTabBtn);
        return bar;
    }

    private void switchTab(int tab) {
        if (activeTab == tab) return;
        activeTab = tab;

        if (tab == 0) {
            cardLayout.show(contentPanel, "services");
            servicesPanel.onActivated();
        } else {
            cardLayout.show(contentPanel, "ports");
            portsPanel.loadPorts();
        }

        Color lc = tab == 0 ? ACCENT_GREEN : ACCENT_BLUE;
        Color rc = tab == 0 ? ACCENT_RED   : ACCENT_AMBER;
        updateChipColor(chipLeft,  lc);
        updateChipColor(chipRight, rc);

        servicesTabBtn.repaint();
        portsTabBtn.repaint();
    }

    // ── Status bar ─────────────────────────────────────────────────────────

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(14, 14, 24));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(5, 24, 5, 24)
        ));

        statusLabel = new JLabel("Inicializando…");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_MUTED);

        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    public void setChips(String leftText, Color leftColor, String rightText, Color rightColor) {
        SwingUtilities.invokeLater(() -> {
            chipLeft.setText(leftText);
            chipRight.setText(rightText);
            updateChipColor(chipLeft,  leftColor);
            updateChipColor(chipRight, rightColor);
        });
    }

    private void updateChipColor(JLabel chip, Color color) {
        chip.setForeground(color);
        chip.repaint();
    }

    // ── TabButton inner class ──────────────────────────────────────────────

    private static class TabButton extends JButton {
        private final java.util.function.Supplier<Boolean> activeSupplier;

        TabButton(String text, java.util.function.Supplier<Boolean> activeSupplier) {
            super(text);
            this.activeSupplier = activeSupplier;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(11, 20, 9, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            boolean active = activeSupplier.get();
            setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
            setForeground(active ? TEXT_PRIMARY : TEXT_MUTED);

            // Hover highlight
            if (!active && getModel().isRollover()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(255, 255, 255, 8));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }

            super.paintComponent(g);

            // Active underline
            if (active) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_BLUE);
                g2.fillRoundRect(8, getHeight() - 3, getWidth() - 16, 3, 3, 3);
                g2.dispose();
            }
        }
    }
}

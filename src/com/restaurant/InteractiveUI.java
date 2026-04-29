package com.restaurant;

import com.restaurant.algorithm.*;
import com.restaurant.model.*;
import com.restaurant.simulation.*;
import com.restaurant.util.JsonLoader;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class InteractiveUI extends JFrame {

    private List<RestaurantSetting> restaurants;
    private List<CustomerScenario> scenarios;
    private List<QueueAlgorithm> algorithms;
    

    private JComboBox<String> restaurantBox, scenarioBox, algorithmBox;
    private JButton loadBtn, nextBtn, prevBtn, resetBtn;
    private JPanel tablePanel;
    private JTextArea queueArea, logArea;
    private JLabel stepLabel, timeLabel, statsLabel;

    private List<SimulationStep> steps = new ArrayList<>();
    private int currentStep = -1;

    public static void main(String[] args) {
        String restaurantFile = args.length > 0 ? args[0] : "data/restaurant_settings.json";
        String scenarioFile = args.length > 1 ? args[1] : "data/customer_scenarios.json";
        SwingUtilities.invokeLater(() -> new InteractiveUI(restaurantFile, scenarioFile).setVisible(true));
    }

    public InteractiveUI(String restaurantFile, String scenarioFile) {
        super("Restaurant Queue Simulation — Interactive Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 780);
        setMinimumSize(new Dimension(1100, 680));

        try {
            restaurants = JsonLoader.loadRestaurantSettings(restaurantFile);
            scenarios = JsonLoader.loadCustomerScenarios(scenarioFile);
            List<CustomerScenario> custom = JsonLoader.tryLoadCustomerScenarios("data/custom_scenarios.json");
            if (!custom.isEmpty()) scenarios.addAll(custom);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Failed to load JSON: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        algorithms = new ArrayList<>();
        algorithms.add(new SingleQueueStaffGuided());
        algorithms.add(new SingleQueueSelfService());
        algorithms.add(new GroupSizeBasedQueue());
        algorithms.add(new PriorityQueueAlgorithm());

        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(new Color(245, 244, 240));

        JPanel topPanel = buildTopPanel();
        add(topPanel, BorderLayout.NORTH);

        JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTableSection(), buildRightPanel());
        center.setDividerLocation(620);
        center.setResizeWeight(0.6);
        center.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(center, BorderLayout.CENTER);

        JPanel bottomBar = buildBottomBar();
        bottomBar.setPreferredSize(new Dimension(getWidth(), 56));
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel buildTopPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(28, 27, 25));
        p.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 4, 0, 4);

        JLabel title = new JLabel("🍽 Restaurant Queue Simulator");
        title.setForeground(new Color(205, 204, 202));
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.weightx = 0;
        p.add(title, gbc);

        gbc.insets = new Insets(0, 16, 0, 4);
        gbc.gridx = 1;
        p.add(styledLabel("Restaurant:"), gbc);

        gbc.insets = new Insets(0, 2, 0, 4);
        restaurantBox = buildCombo(restaurants.stream().map(RestaurantSetting::getName).toArray(String[]::new));
        gbc.gridx = 2;
        p.add(restaurantBox, gbc);

        gbc.insets = new Insets(0, 12, 0, 4);
        gbc.gridx = 3;
        p.add(styledLabel("Scenario:"), gbc);

        gbc.insets = new Insets(0, 2, 0, 4);
        scenarioBox = buildCombo(scenarios.stream().map(CustomerScenario::getName).toArray(String[]::new));
        gbc.gridx = 4;
        p.add(scenarioBox, gbc);

        gbc.insets = new Insets(0, 12, 0, 4);
        gbc.gridx = 5;
        p.add(styledLabel("Algorithm:"), gbc);

        gbc.insets = new Insets(0, 2, 0, 4);
        algorithmBox = buildCombo(algorithms.stream().map(QueueAlgorithm::getName).toArray(String[]::new));
        gbc.gridx = 6;
        p.add(algorithmBox, gbc);

        gbc.insets = new Insets(0, 16, 0, 4);
        /*
        loadBtn = new JButton("▶ Load & Start");
        loadBtn.setBackground(new Color(79, 152, 163));
        loadBtn.setForeground(Color.WHITE);
        loadBtn.setFocusPainted(false);
        loadBtn.setOpaque(true);
        loadBtn.setBorderPainted(false);
        loadBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        loadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loadBtn.setPreferredSize(new Dimension(140, 34));
        loadBtn.setMinimumSize(new Dimension(120, 34));
        loadBtn.addActionListener(e -> loadSimulation());
        gbc.gridx = 7;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        p.add(loadBtn, gbc);
        */

        return p;
    }

    private JPanel buildTableSection() {
        JPanel wrapper = new JPanel(new BorderLayout(4, 4));
        wrapper.setBackground(new Color(245, 244, 240));
        wrapper.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(212, 209, 202)),
                "Restaurant Floor", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(40, 37, 29)));

        timeLabel = new JLabel("Time: — | Step: —");
        timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        timeLabel.setForeground(new Color(1, 105, 111));
        timeLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        wrapper.add(timeLabel, BorderLayout.NORTH);

        tablePanel = new JPanel();
        tablePanel.setBackground(new Color(249, 248, 245));
        tablePanel.setLayout(new WrapLayout(FlowLayout.LEFT, 12, 12));
        JScrollPane sp = new JScrollPane(tablePanel);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(new Color(249, 248, 245));
        wrapper.add(sp, BorderLayout.CENTER);

        statsLabel = new JLabel(" ");
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statsLabel.setForeground(new Color(122, 121, 116));
        statsLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        wrapper.add(statsLabel, BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 8));
        p.setBackground(new Color(245, 244, 240));

        JPanel queueWrapper = new JPanel(new BorderLayout());
        queueWrapper.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(212, 209, 202)),
                "Queue Status", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(40, 37, 29)));
        queueArea = new JTextArea();
        queueArea.setEditable(false);
        queueArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        queueArea.setBackground(new Color(249, 248, 245));
        queueArea.setForeground(new Color(40, 37, 29));
        queueArea.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        queueWrapper.add(new JScrollPane(queueArea), BorderLayout.CENTER);
        p.add(queueWrapper);

        JPanel logWrapper = new JPanel(new BorderLayout());
        logWrapper.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(212, 209, 202)),
                "Event Log", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), new Color(40, 37, 29)));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setBackground(new Color(249, 248, 245));
        logArea.setForeground(new Color(40, 37, 29));
        logArea.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        logWrapper.add(new JScrollPane(logArea), BorderLayout.CENTER);
        p.add(logWrapper);

        return p;
    }

    private JPanel buildBottomBar() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(new Color(34, 33, 31));
        p.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        p.setPreferredSize(new Dimension(getWidth(), 56));

        loadBtn = new JButton("▶ Load & Start");
        styleLoadButton(loadBtn);
        loadBtn.addActionListener(e -> loadSimulation());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setBackground(new Color(34, 33, 31));
        leftPanel.add(loadBtn);
        p.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(new Color(34, 33, 31));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 6, 0, 6);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;

        prevBtn = buildNavButton("◀ Prev");
        prevBtn.addActionListener(e -> stepBack());
        gbc.gridx = 0;
        rightPanel.add(prevBtn, gbc);

        nextBtn = buildNavButton("Next ▶");
        nextBtn.addActionListener(e -> stepForward());
        gbc.gridx = 1;
        rightPanel.add(nextBtn, gbc);

        resetBtn = buildNavButton("↺ Reset");
        resetBtn.addActionListener(e -> resetToStart());
        gbc.gridx = 2;
        rightPanel.add(resetBtn, gbc);

        stepLabel = new JLabel("Load a simulation to begin.");
        stepLabel.setForeground(new Color(180, 178, 174));
        stepLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        gbc.gridx = 3;
        gbc.insets = new Insets(0, 16, 0, 0);
        rightPanel.add(stepLabel, gbc);

        p.add(rightPanel, BorderLayout.EAST);

        setNavEnabled(false);
        return p;
    }

    private JButton buildNavButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(110, 36));
        b.setMinimumSize(new Dimension(90, 36));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setOpaque(false);

        b.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel model = ((JButton) c).getModel();
                Color base = c.isEnabled()
                        ? (model.isPressed() ? new Color(35, 34, 32) : model.isRollover() ? new Color(57, 56, 54) : new Color(44, 43, 41))
                        : new Color(36, 35, 33);
                g2.setColor(base);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 8, 8);
                g2.setColor(c.isEnabled() ? new Color(205, 204, 202) : new Color(100, 99, 97));
                g2.setFont(b.getFont());
                FontMetrics fm = g2.getFontMetrics();
                String txt = b.getText();
                int x = (c.getWidth() - fm.stringWidth(txt)) / 2;
                int y = (c.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(txt, x, y);
                g2.dispose();
            }
        });
        return b;
    }

    private void styleLoadButton(JButton b) {
        b.setText("▶ Load & Start");
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(140, 36));
        b.setMinimumSize(new Dimension(120, 36));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setOpaque(false);

        b.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel model = ((JButton) c).getModel();
                if (model.isPressed()) {
                    g2.setColor(new Color(22, 98, 107));
                } else if (model.isRollover()) {
                    g2.setColor(new Color(34, 127, 139));
                } else {
                    g2.setColor(new Color(1, 105, 111));
                }
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(b.getFont());
                FontMetrics fm = g2.getFontMetrics();
                String text = b.getText();
                int x = (c.getWidth() - fm.stringWidth(text)) / 2;
                int y = (c.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, x, y);
                g2.dispose();
            }
        });
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(180, 178, 174));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    private JComboBox<String> buildCombo(String[] items) {
        JComboBox<String> box = new JComboBox<>(items);
        box.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        box.setBackground(new Color(44, 43, 41));
        box.setForeground(new Color(205, 204, 202));
        box.setPreferredSize(new Dimension(180, 30));
        box.setMinimumSize(new Dimension(140, 30));
        return box;
    }

    private void loadSimulation() {
        int ri = restaurantBox.getSelectedIndex();
        int si = scenarioBox.getSelectedIndex();
        int ai = algorithmBox.getSelectedIndex();

        if (ri < 0 || si < 0 || ai < 0) {
            JOptionPane.showMessageDialog(this, "Please select all options.", "Missing Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        RestaurantSetting restaurant = restaurants.get(ri);
        CustomerScenario scenario = scenarios.get(si);
        QueueAlgorithm algorithm = algorithms.get(ai);

        SimulationEngine engine = new SimulationEngine();
        steps = engine.runWithSteps(algorithm, restaurant, scenario);
        currentStep = -1;
        logArea.setText("");
        queueArea.setText("");
        tablePanel.removeAll();
        tablePanel.revalidate();
        tablePanel.repaint();

        setNavEnabled(true);
        stepLabel.setText("Step 0 / " + steps.size() + "  |  Loaded: " + algorithm.getName());
        timeLabel.setText("Time: 0 | Step: 0 / " + steps.size());
        statsLabel.setText("Restaurant: " + restaurant.getName() + " | Tables: " + restaurant.getTables().size() + " | Total seats: " + restaurant.getTotalSeats());

        renderInitialTables(restaurant.getCopiedTables());
    }

    private void renderInitialTables(List<Table> tables) {
        tablePanel.removeAll();
        for (Table t : tables) {
            tablePanel.add(buildTableCard(t, false, false));
        }
        tablePanel.revalidate();
        tablePanel.repaint();
    }

    private void stepForward() {
        if (steps.isEmpty()) return;
        if (currentStep < steps.size() - 1) {
            currentStep++;
            renderStep(steps.get(currentStep));
        }
    }

    private void stepBack() {
        if (currentStep > 0) {
            currentStep--;
            renderStep(steps.get(currentStep));
        } else if (currentStep == 0) {
            currentStep = -1;
            int ri = restaurantBox.getSelectedIndex();
            renderInitialTables(restaurants.get(ri).getCopiedTables());
            queueArea.setText("");
            timeLabel.setText("Time: — | Step: 0 / " + steps.size());
            stepLabel.setText("Step 0 / " + steps.size());
        }
    }

    private void resetToStart() {
        currentStep = -1;
        logArea.setText("");
        queueArea.setText("");
        int ri = restaurantBox.getSelectedIndex();
        if (ri >= 0) renderInitialTables(restaurants.get(ri).getCopiedTables());
        timeLabel.setText("Time: — | Step: 0 / " + steps.size());
        stepLabel.setText("Step 0 / " + steps.size() + "  (reset)");
    }

    private void renderStep(SimulationStep step) {
        tablePanel.removeAll();
        if (step.tableSnapshot != null) {
            for (Table t : step.tableSnapshot) {
                boolean justUsed = step.tableUsed != null && step.tableUsed.getTableId().equals(t.getTableId());
                boolean justFreed = !t.isOccupied() && justUsed;
                tablePanel.add(buildTableCard(t, justUsed && t.isOccupied(), justFreed));
            }
        }
        tablePanel.revalidate();
        tablePanel.repaint();

        StringBuilder qb = new StringBuilder();
        if (step.queueSnapshot != null) {
            if (step.queueSnapshot.isEmpty()) {
                qb.append("(Queue is empty)\n");
            } else {
                qb.append("Waiting groups:\n");
                for (int i = 0; i < step.queueSnapshot.size(); i++) {
                    CustomerGroup g = step.queueSnapshot.get(i);
                    qb.append(String.format("  %d. %s — size=%d%s, pref=%d-seat, arr=t%d%n",
                            i + 1, g.getGroupId(), g.getGroupSize(),
                            g.isVip() ? " [VIP]" : "",
                            g.getPreferredTableSize(), g.getArrivalTime()));
                }
                if (step.justArrived == null && currentStep + 1 < steps.size()) {
                    SimulationStep next = steps.get(currentStep + 1);
                    if (next.justArrived != null) {
                        CustomerGroup na = next.justArrived;
                        qb.append(String.format("%n→ Next arrival: %s (size=%d%s) at t=%d%n",
                                na.getGroupId(), na.getGroupSize(), na.isVip() ? " [VIP]" : "", na.getArrivalTime()));
                    }
                }
            }
        }
        queueArea.setText(qb.toString());

        logArea.append(String.format("[t=%3d] %s%n", step.time, step.event));
        logArea.setCaretPosition(logArea.getDocument().getLength());

        int s = currentStep + 1;
        stepLabel.setText("Step " + s + " / " + steps.size());
        timeLabel.setText("Time: " + step.time + " min | Step: " + s + " / " + steps.size());

        prevBtn.setEnabled(currentStep >= 0);
        nextBtn.setEnabled(currentStep < steps.size() - 1);
    }

    private JPanel buildTableCard(Table table, boolean highlight, boolean justFreed) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(110, 100));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        highlight ? new Color(1, 105, 111) : justFreed ? new Color(67, 122, 34) : new Color(212, 209, 202), highlight ? 2 : 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        Color bg = table.isOccupied()
                ? (highlight ? new Color(206, 220, 216) : new Color(230, 228, 224))
                : (justFreed ? new Color(212, 223, 204) : new Color(249, 248, 245));
        card.setBackground(bg);

        JLabel idLabel = new JLabel(table.getTableId());
        idLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        idLabel.setForeground(new Color(28, 27, 25));
        idLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(idLabel);

        JLabel capLabel = new JLabel("Capacity: " + table.getCapacity());
        capLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        capLabel.setForeground(new Color(122, 121, 116));
        capLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(capLabel);

        card.add(Box.createVerticalStrut(4));

        String statusText;
        Color statusColor;
        if (table.isOccupied()) {
            CustomerGroup g = table.getCurrentGroup();
            statusText = g != null ? g.getGroupId() + (g.isVip() ? "★" : "") + " (" + g.getGroupSize() + ")" : "Occupied";
            statusColor = new Color(1, 78, 84);
        } else {
            statusText = justFreed ? "Just freed!" : "Available";
            statusColor = justFreed ? new Color(67, 122, 34) : new Color(67, 122, 34);
        }

        JLabel statusLabel = new JLabel(statusText);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        statusLabel.setForeground(statusColor);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(statusLabel);

        if (table.isOccupied()) {
            JLabel freeLabel = new JLabel("free@t=" + table.getFreeAtTime());
            freeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            freeLabel.setForeground(new Color(150, 101, 59));
            freeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(freeLabel);
        }

        JPanel seatRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        seatRow.setBackground(bg);
        for (int i = 0; i < table.getCapacity(); i++) {
            JLabel seat = new JLabel(table.isOccupied() && table.getCurrentGroup() != null && i < table.getCurrentGroup().getGroupSize() ? "●" : "○");
            seat.setForeground(table.isOccupied() && table.getCurrentGroup() != null && i < table.getCurrentGroup().getGroupSize()
                    ? new Color(1, 105, 111) : new Color(186, 185, 180));
            seat.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            seatRow.add(seat);
        }
        card.add(seatRow);

        return card;
    }

    private void setNavEnabled(boolean enabled) {
        nextBtn.setEnabled(enabled);
        prevBtn.setEnabled(false);
        resetBtn.setEnabled(enabled);
    }

    static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int width = target.getWidth();
                if (width == 0) width = Integer.MAX_VALUE;
                Insets insets = target.getInsets();
                int maxWidth = width - insets.left - insets.right - getHgap() * 2;
                int rowWidth = 0, rowHeight = 0, totalHeight = insets.top + insets.bottom + getVgap() * 2;
                for (Component c : target.getComponents()) {
                    if (!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                    if (rowWidth + d.width > maxWidth) {
                        totalHeight += rowHeight + getVgap();
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    rowWidth += d.width + getHgap();
                    rowHeight = Math.max(rowHeight, d.height);
                }
                totalHeight += rowHeight;
                return new Dimension(width, totalHeight);
            }
        }
    }
}
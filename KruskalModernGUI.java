import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class KruskalModernGUI extends JFrame {
    private static final int NODE_RADIUS = 25;
    private Graph graph;
    private KruskalAlgorithm kruskal;
    private List<KruskalAlgorithm.EdgeStatus> edgeStatuses;
    private final List<Edge> mst;
    private int currentStep;

    // GUI Components
    private ModernGraphPanel graphPanel;
    private JTable edgeTable;
    private DefaultTableModel tableModel;
    private JTextField numVerticesField;
    private JTextField sourceField;
    private JTextField destField;
    private JTextField weightField;
    private JLabel statusLabel;
    private GradientButton createGraphButton;
    private GradientButton addEdgeButton;
    private GradientButton computeMSTButton;
    private GradientButton undoButton;
    private GradientButton redoButton;
    private GradientButton clearButton;
    private GradientButton prevStepButton;
    private GradientButton nextStepButton;

    private final Map<Vertex, Point> vertexPositions;
    private final List<Edge> undoneEdges;
    private Edge selectedEdge;
    private boolean addingVertices;
    private javax.swing.Timer highlightTimer;
    private javax.swing.Timer fadeTimer;
    private float fadeAlpha = 1.0f;

    public KruskalModernGUI() {
        super("Kruskal's Algorithm - Modern Visualization");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(30, 30, 30));

        graph = new Graph();
        kruskal = null;
        edgeStatuses = new ArrayList<>();
        mst = new ArrayList<>();
        currentStep = 0;
        vertexPositions = new HashMap<>();
        undoneEdges = new ArrayList<>();
        selectedEdge = null;
        addingVertices = false;

        // Initialize timers
        highlightTimer = new javax.swing.Timer(500, e -> graphPanel.repaint());
        highlightTimer.setRepeats(false);

        fadeTimer = new javax.swing.Timer(50, e -> {
            fadeAlpha = Math.max(0.0f, fadeAlpha - 0.1f);
            graphPanel.repaint();
            if (fadeAlpha <= 0.0f) fadeTimer.stop();
        });

        initializeComponents();
    }

    private void initializeComponents() {
        // Control Panel (Top)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBackground(new Color(40, 40, 40));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        topPanel.add(createStyledLabel("Vertices:"));
        numVerticesField = createStyledTextField(5);
        topPanel.add(numVerticesField);

        createGraphButton = new GradientButton("Create Graph", new Color(138, 43, 226), new Color(75, 0, 130));
        createGraphButton.addActionListener(e -> createGraph());
        topPanel.add(createGraphButton);

        topPanel.add(createStyledLabel("Source:"));
        sourceField = createStyledTextField(5);
        topPanel.add(sourceField);

        topPanel.add(createStyledLabel("Dest:"));
        destField = createStyledTextField(5);
        topPanel.add(destField);

        topPanel.add(createStyledLabel("Weight:"));
        weightField = createStyledTextField(5);
        topPanel.add(weightField);

        addEdgeButton = new GradientButton("Add Edge", new Color(0, 191, 255), new Color(0, 105, 180));
        addEdgeButton.addActionListener(e -> addEdge());
        topPanel.add(addEdgeButton);

        add(topPanel, BorderLayout.NORTH);

        // Graph Panel (Center)
        graphPanel = new ModernGraphPanel();
        add(graphPanel, BorderLayout.CENTER);

        // Edge Table Panel (Right)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(40, 40, 40));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columnNames = {"Edge", "Weight", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0);
        edgeTable = new JTable(tableModel);

        // Style the table
        edgeTable.setBackground(new Color(50, 50, 50));
        edgeTable.setForeground(Color.WHITE);
        edgeTable.setGridColor(new Color(70, 70, 70));
        edgeTable.setRowHeight(30);
        edgeTable.setShowGrid(true);
        edgeTable.setFont(new Font("Arial", Font.BOLD, 12));
        edgeTable.setDefaultRenderer(Object.class, new CustomTableCellRenderer());

        // Style the table header
        JTableHeader header = edgeTable.getTableHeader();
        header.setDefaultRenderer(new GradientTableHeader());
        header.setBackground(new Color(40, 40, 40));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(header.getWidth(), 40));

        // Add hover effect
        edgeTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = edgeTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row != edgeTable.getSelectedRow()) {
                    edgeTable.repaint();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(edgeTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        JLabel mstWeightLabel = new JLabel("Total MST Weight: 0");
        mstWeightLabel.setForeground(Color.WHITE);
        mstWeightLabel.setFont(new Font("Arial", Font.BOLD, 14));
        rightPanel.add(mstWeightLabel, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        // Control Panel (Bottom)
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomPanel.setBackground(new Color(40, 40, 40));

        computeMSTButton = new GradientButton("Compute MST", new Color(255, 165, 0), new Color(200, 100, 0));
        computeMSTButton.addActionListener(e -> computeMST());
        bottomPanel.add(computeMSTButton);

        undoButton = new GradientButton("Undo", new Color(255, 99, 71), new Color(200, 50, 50));
        undoButton.addActionListener(e -> undo());
        bottomPanel.add(undoButton);

        redoButton = new GradientButton("Redo", new Color(255, 99, 71), new Color(200, 50, 50));
        redoButton.addActionListener(e -> redo());
        bottomPanel.add(redoButton);

        clearButton = new GradientButton("Clear", new Color(128, 128, 128), new Color(80, 80, 80));
        clearButton.addActionListener(e -> clear());
        bottomPanel.add(clearButton);

        prevStepButton = new GradientButton("Prev Step", new Color(50, 205, 50), new Color(34, 139, 34));
        prevStepButton.addActionListener(e -> previousStep());
        bottomPanel.add(prevStepButton);

        nextStepButton = new GradientButton("Next Step", new Color(50, 205, 50), new Color(34, 139, 34));
        nextStepButton.addActionListener(e -> nextStep());
        bottomPanel.add(nextStepButton);

        statusLabel = createStyledLabel("Status: Ready");
        bottomPanel.add(statusLabel);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        return label;
    }

    private JTextField createStyledTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setBackground(new Color(50, 50, 50));
        field.setForeground(Color.WHITE);
        field.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));
        field.setCaretColor(Color.WHITE);
        return field;
    }

    private void createGraph() {
        try {
            int numVertices = Integer.parseInt(numVerticesField.getText());
            if (numVertices <= 0) throw new NumberFormatException();

            graph = new Graph();
            vertexPositions.clear();
            tableModel.setRowCount(0);
            mst.clear();
            edgeStatuses.clear();
            currentStep = 0;
            undoneEdges.clear();
            selectedEdge = null;
            addingVertices = true;
            graphPanel.resetVerticesPlaced();
            for (int i = 0; i < numVertices; i++) {
                graph.addVertex(new Vertex(i));
            }
            statusLabel.setText("Status: Click to place " + numVertices + " vertices");
            fadeAlpha = 1.0f;
            fadeTimer.start();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number of vertices", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addEdge() {
        try {
            int source = Integer.parseInt(sourceField.getText());
            int dest = Integer.parseInt(destField.getText());
            double weight = Double.parseDouble(weightField.getText());

            if (source < 0 || source >= graph.getVertices().size() || dest < 0 || dest >= graph.getVertices().size()) {
                throw new IllegalArgumentException("Invalid vertex");
            }

            Vertex v1 = graph.getVertices().get(source);
            Vertex v2 = graph.getVertices().get(dest);
            graph.addEdge(v1, v2, weight);

            tableModel.addRow(new Object[]{"(" + source + ", " + dest + ")", weight, "Pending"});
            graphPanel.repaint();
            statusLabel.setText("Status: Edge (" + source + ", " + dest + ") added");
            fadeAlpha = 1.0f;
            fadeTimer.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void computeMST() {
        kruskal = new KruskalAlgorithm(graph);
        edgeStatuses = kruskal.computeSteps();
        mst.clear();
        currentStep = 0;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt("Pending", i, 2);
        }

        graphPanel.repaint();
        statusLabel.setText("Status: MST computed. Use Next Step to visualize.");
        updateMSTWeight();
    }

    private void previousStep() {
        if (currentStep > 0) {
            currentStep--;
            updateStep();
        }
    }

    private void nextStep() {
        if (currentStep < edgeStatuses.size()) {
            currentStep++;
            updateStep();
            highlightTimer.restart();
        }
    }

    private void updateStep() {
        mst.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt("Pending", i, 2);
        }

        for (int i = 0; i < currentStep; i++) {
            KruskalAlgorithm.EdgeStatus edgeStatus = edgeStatuses.get(i);
            if (edgeStatus.isAccepted()) {
                mst.add(edgeStatus.getEdge());
            }
            updateTable(edgeStatus.getEdge(), edgeStatus.getStatus());
        }

        graphPanel.repaint();
        if (currentStep > 0 && currentStep <= edgeStatuses.size()) {
            statusLabel.setText("Step " + currentStep + ": " + edgeStatuses.get(currentStep - 1).getStatus());
        }
        updateMSTWeight();
    }

    private void updateTable(Edge edge, String status) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String edgeLabel = (String) tableModel.getValueAt(i, 0);
            if (edgeLabel.equals(getEdgeLabel(edge))) {
                tableModel.setValueAt(status, i, 2);
                break;
            }
        }
    }

    private String getEdgeLabel(Edge edge) {
        return "(" + edge.getVertex1().getId() + ", " + edge.getVertex2().getId() + ")";
    }

    private void updateMSTWeight() {
        double weight = mst.stream().mapToDouble(Edge::getWeight).sum();
        ((JLabel) ((JPanel) getContentPane().getComponent(2)).getComponent(1)).setText("Total MST Weight: " + weight);
    }

    private void undo() {
        if (!graph.getEdges().isEmpty()) {
            Edge lastEdge = graph.getEdges().remove(graph.getEdges().size() - 1);
            undoneEdges.add(lastEdge);
            tableModel.removeRow(tableModel.getRowCount() - 1);
            graphPanel.repaint();
            statusLabel.setText("Status: Last edge removed");
        }
    }

    private void redo() {
        if (!undoneEdges.isEmpty()) {
            Edge edge = undoneEdges.remove(undoneEdges.size() - 1);
            graph.getEdges().add(edge);
            tableModel.addRow(new Object[]{getEdgeLabel(edge), edge.getWeight(), "Pending"});
            graphPanel.repaint();
            statusLabel.setText("Status: Edge restored");
        }
    }

    private void clear() {
        if (JOptionPane.showConfirmDialog(this, "Clear the graph?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            graph = new Graph();
            vertexPositions.clear();
            tableModel.setRowCount(0);
            mst.clear();
            edgeStatuses.clear();
            currentStep = 0;
            undoneEdges.clear();
            selectedEdge = null;
            addingVertices = false;
            graphPanel.resetVerticesPlaced();
            numVerticesField.setText("");
            sourceField.setText("");
            destField.setText("");
            weightField.setText("");
            graphPanel.repaint();
            statusLabel.setText("Status: Graph cleared. Enter number of vertices to start.");
            updateMSTWeight();
        }
    }

    private class ModernGraphPanel extends JPanel {
        private int verticesPlaced;

        public ModernGraphPanel() {
            setBackground(new Color(20, 20, 20));
            verticesPlaced = 0;
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleMouseClick(e);
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    handleMouseMove(e);
                }
            });
        }

        public void resetVerticesPlaced() {
            verticesPlaced = 0;
        }

        private void handleMouseClick(MouseEvent e) {
            if (addingVertices && verticesPlaced < graph.getVertices().size()) {
                Vertex vertex = graph.getVertices().get(verticesPlaced);
                vertexPositions.put(vertex, new Point(e.getX(), e.getY()));
                verticesPlaced++;
                repaint();
                if (verticesPlaced == graph.getVertices().size()) {
                    addingVertices = false;
                    statusLabel.setText("Status: All vertices placed. Add edges.");
                } else {
                    statusLabel.setText("Status: Place " + (graph.getVertices().size() - verticesPlaced) + " vertices");
                }
            } else {
                Edge edge = findEdgeAtPoint(e.getPoint());
                if (edge != null) {
                    selectedEdge = edge;
                    String[] options = {"Delete Edge", "Edit Weight", "Cancel"};
                    int choice = JOptionPane.showOptionDialog(
                        KruskalModernGUI.this,
                        "Edge: " + getEdgeLabel(edge),
                        "Edge Options",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[2]
                    );
                    if (choice == 0) {
                        graph.getEdges().remove(edge);
                        for (int i = 0; i < tableModel.getRowCount(); i++) {
                            if (tableModel.getValueAt(i, 0).equals(getEdgeLabel(edge))) {
                                tableModel.removeRow(i);
                                break;
                            }
                        }
                        selectedEdge = null;
                        statusLabel.setText("Status: Edge deleted");
                        repaint();
                    } else if (choice == 1) {
                        String newWeightStr = JOptionPane.showInputDialog(
                            KruskalModernGUI.this,
                            "New weight for " + getEdgeLabel(edge),
                            edge.getWeight()
                        );
                        try {
                            double newWeight = Double.parseDouble(newWeightStr);
                            for (int i = 0; i < tableModel.getRowCount(); i++) {
                                if (tableModel.getValueAt(i, 0).equals(getEdgeLabel(edge))) {
                                    tableModel.setValueAt(newWeight, i, 1);
                                    break;
                                }
                            }
                            graph.getEdges().remove(edge);
                            Vertex v1 = edge.getVertex1();
                            Vertex v2 = edge.getVertex2();
                            graph.addEdge(v1, v2, newWeight);
                            selectedEdge = null;
                            statusLabel.setText("Status: Edge weight updated");
                            repaint();
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(KruskalModernGUI.this, "Invalid weight", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        }

        private void handleMouseMove(MouseEvent e) {
            Edge edge = findEdgeAtPoint(e.getPoint());
            if (edge != null && !addingVertices) {
                selectedEdge = edge;
                repaint();
            } else if (selectedEdge != null && !addingVertices) {
                selectedEdge = null;
                repaint();
            }
        }

        private Edge findEdgeAtPoint(Point p) {
            for (Edge edge : graph.getEdges()) {
                Point p1 = vertexPositions.get(edge.getVertex1());
                Point p2 = vertexPositions.get(edge.getVertex2());
                double dist = pointToLineDistance(p, p1, p2);
                if (dist < 5 && p.distance((p1.x + p2.x) / 2, (p1.y + p2.y) / 2) < p1.distance(p2) / 2) {
                    return edge;
                }
            }
            return null;
        }

        private double pointToLineDistance(Point p, Point p1, Point p2) {
            double length = p1.distance(p2);
            if (length == 0) return p.distance(p1);
            double t = Math.max(0, Math.min(1, ((p.x - p1.x) * (p2.x - p1.x) + (p.y - p1.y) * (p2.y - p1.y)) / (length * length)));
            Point projection = new Point((int)(p1.x + t * (p2.x - p1.x)), (int)(p1.y + t * (p2.y - p1.y)));
            return p.distance(projection);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw edges
            for (Edge edge : graph.getEdges()) {
                Point p1 = vertexPositions.get(edge.getVertex1());
                Point p2 = vertexPositions.get(edge.getVertex2());

                // Set edge appearance
                if (mst.contains(edge)) {
                    g2d.setColor(new Color(0, 255, 100)); // Green for MST edges
                    g2d.setStroke(new BasicStroke(4));
                } else if (currentStep > 0 && currentStep <= edgeStatuses.size() && edgeStatuses.get(currentStep - 1).getEdge() == edge && highlightTimer.isRunning()) {
                    g2d.setColor(new Color(255, 255, 0, (int)(255 * fadeAlpha))); // Yellow highlight
                    g2d.setStroke(new BasicStroke(4));
                } else if (edge == selectedEdge) {
                    g2d.setColor(new Color(255, 165, 0)); // Orange for selected
                    g2d.setStroke(new BasicStroke(3));
                } else {
                    g2d.setColor(new Color(255, 80, 80)); // Red for pending
                    g2d.setStroke(new BasicStroke(2));
                }
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);

                // Draw weight with background
                int midX = (p1.x + p2.x) / 2;
                int midY = (p1.y + p2.y) / 2;
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.fillRoundRect(midX + 5, midY - 10, 40, 20, 10, 10);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.drawString(String.valueOf(edge.getWeight()), midX + 10, midY + 5);

                // Draw status marker for processed edges
                for (int i = 0; i < currentStep && i < edgeStatuses.size(); i++) {
                    KruskalAlgorithm.EdgeStatus edgeStatus = edgeStatuses.get(i);
                    if (edgeStatus.getEdge() == edge) {
                        int markerX = midX + 50; // Offset right of weight label
                        int markerY = midY + 3; // Align vertically with weight text
                        String markerText = edgeStatus.isAccepted() ? "OK" : "Reject";
                        int textWidth = edgeStatus.isAccepted() ? 20 : 40; // Estimate width for OK vs Reject
                        g2d.setColor(new Color(0, 0, 0, 150)); // Background for contrast
                        g2d.fillRect(markerX - 2, markerY - 10, textWidth + 4, 14);
                        g2d.setFont(new Font("Arial", Font.BOLD, 10));
                        if (edgeStatus.isAccepted()) {
                            // Draw green "OK" for accepted
                            g2d.setColor(new Color(0, 255, 100));
                            g2d.drawString("OK", markerX, markerY);
                        } else {
                            // Draw red "Reject" for rejected
                            g2d.setColor(new Color(255, 80, 80));
                            g2d.drawString("Reject", markerX, markerY);
                        }
                        break;
                    }
                }
            }

            // Draw vertices
            for (Vertex vertex : graph.getVertices()) {
                Point p = vertexPositions.get(vertex);
                if (p != null) {
                    GradientPaint gradient = new GradientPaint(
                        p.x - NODE_RADIUS, p.y - NODE_RADIUS, new Color(0, 200, 255),
                        p.x + NODE_RADIUS, p.y + NODE_RADIUS, new Color(0, 100, 255)
                    );
                    g2d.setPaint(gradient);
                    g2d.fillOval(p.x - NODE_RADIUS, p.y - NODE_RADIUS, 2 * NODE_RADIUS, 2 * NODE_RADIUS);
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 14));
                    g2d.drawString(String.valueOf(vertex.getId()), p.x + NODE_RADIUS + 5, p.y + 5);
                }
            }
        }
    }

    private class CustomTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setFont(new Font("Arial", Font.BOLD, 12));

            // Set background with subtle gradient
            Point mousePoint = table.getMousePosition();
            int hoverRow = mousePoint != null ? table.rowAtPoint(mousePoint) : -1;
            if (row == hoverRow) {
                c.setBackground(new Color(70, 70, 70)); // Lighter gray for hover
            } else {
                c.setBackground(row % 2 == 0 ? new Color(50, 50, 50) : new Color(60, 60, 60)); // Alternating rows
            }

            // Style based on column
            if (column == 2) { // Status column
                String status = value.toString();
                switch (status) {
                    case "Pending":
                        c.setForeground(new Color(255, 255, 0)); // Yellow
                        ((JLabel) c).setText(" ● " + status); // Small dot icon
                        break;
                    case "Accepted":
                        c.setForeground(new Color(0, 255, 100)); // Green
                        ((JLabel) c).setText(" ✓ " + status); // Checkmark icon
                        break;
                    case "Rejected":
                        c.setForeground(new Color(255, 80, 80)); // Red
                        ((JLabel) c).setText(" ✗ " + status); // Cross icon
                        break;
                    default:
                        c.setForeground(Color.WHITE);
                        break;
                }
            } else {
                c.setForeground(Color.WHITE);
            }

            ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
            return c;
        }
    }

    private class GradientTableHeader extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setFont(new Font("Arial", Font.BOLD, 14));
            label.setForeground(Color.WHITE);
            label.setHorizontalAlignment(SwingConstants.CENTER);

            // Apply gradient background
            GradientPaint gradient = new GradientPaint(0, 0, new Color(138, 43, 226), 0, 40, new Color(75, 0, 130));
            label.setOpaque(false);
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            label.setBackground(new Color(0, 0, 0, 0)); // Transparent to show gradient
            label.setUI(new javax.swing.plaf.basic.BasicLabelUI() {
                @Override
                protected void paintEnabledText(JLabel l, Graphics g, String s, int textX, int textY) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, l.getWidth(), l.getHeight());
                    g2d.setColor(l.getForeground());
                    g2d.setFont(l.getFont());
                    g2d.drawString(s, textX, textY);
                }
            });

            return label;
        }
    }

    private class GradientButton extends JButton {
        private Color startColor;
        private Color endColor;
        private boolean isHovered = false;
        private boolean isPressed = false;
        private float pulseScale = 1.0f;
        private javax.swing.Timer pulseTimer;

        public GradientButton(String text, Color startColor, Color endColor) {
            super(text);
            this.startColor = startColor;
            this.endColor = endColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Arial", Font.BOLD, 14));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Pulse animation
            pulseTimer = new javax.swing.Timer(50, e -> {
                pulseScale = 1.0f + 0.05f * (float) Math.sin(System.currentTimeMillis() / 500.0);
                repaint();
            });
            pulseTimer.start();

            // Mouse listeners for hover and press effects
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    isPressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Adjust colors based on state
            Color adjustedStart = startColor;
            Color adjustedEnd = endColor;
            if (!isEnabled()) {
                adjustedStart = new Color(80, 80, 80);
                adjustedEnd = new Color(60, 60, 60);
            } else if (isPressed) {
                adjustedStart = startColor.darker();
                adjustedEnd = endColor.darker();
            } else if (isHovered) {
                adjustedStart = startColor.brighter();
                adjustedEnd = endColor.brighter();
            }

            // Draw shadow
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fill(new RoundRectangle2D.Float(4, 4, getWidth() - 4, getHeight() - 4, 20, 20));

            // Apply pulse scaling
            int width = getWidth();
            int height = getHeight();
            if (isEnabled() && isHovered) {
                width = (int) (width * pulseScale);
                height = (int) (height * pulseScale);
            }

            // Draw gradient background
            GradientPaint gradient = new GradientPaint(0, 0, adjustedStart, 0, height, adjustedEnd);
            g2d.setPaint(gradient);
            g2d.fill(new RoundRectangle2D.Float(0, 0, width, height, 20, 20));

            // Draw border
            g2d.setColor(new Color(255, 255, 255, 50));
            g2d.setStroke(new BasicStroke(1));
            g2d.draw(new RoundRectangle2D.Float(0, 0, width - 1, height - 1, 20, 20));

            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            KruskalModernGUI gui = new KruskalModernGUI();
            gui.setVisible(true);
        });
    }
}
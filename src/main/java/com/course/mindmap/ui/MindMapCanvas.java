package com.course.mindmap.ui;

import com.course.mindmap.layout.LayoutSnapshot;
import com.course.mindmap.layout.MindMapLayoutEngine;
import com.course.mindmap.model.MindMapDocument;
import com.course.mindmap.model.MindMapNode;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class MindMapCanvas extends JPanel {
    private static final int CANVAS_MARGIN = 80;
    private static final int MIN_WIDTH = 110;
    private static final int MIN_HEIGHT = 38;
    private static final Font NODE_FONT = new Font("Microsoft YaHei UI", Font.PLAIN, 14);

    private final MindMapLayoutEngine layoutEngine = new MindMapLayoutEngine();
    private MindMapDocument document;
    private LayoutSnapshot snapshot = new LayoutSnapshot(Map.of());
    private MindMapNode selectedNode;
    private Consumer<MindMapNode> selectionListener = node -> {
    };
    private Consumer<MindMapNode> nodeActivationListener = node -> {
    };
    private int offsetX = CANVAS_MARGIN;
    private int offsetY = CANVAS_MARGIN;

    public MindMapCanvas() {
        setBackground(Color.WHITE);
        setOpaque(true);
        setFont(NODE_FONT);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                MindMapNode clickedNode = findNodeAt(event.getPoint());
                setSelectedNode(clickedNode, false);
                selectionListener.accept(clickedNode);
                if (clickedNode != null && event.getClickCount() == 2) {
                    nodeActivationListener.accept(clickedNode);
                }
            }
        };
        addMouseListener(mouseAdapter);
    }

    public void setSelectionListener(Consumer<MindMapNode> selectionListener) {
        this.selectionListener = selectionListener == null ? node -> {
        } : selectionListener;
    }

    public void setNodeActivationListener(Consumer<MindMapNode> nodeActivationListener) {
        this.nodeActivationListener = nodeActivationListener == null ? node -> {
        } : nodeActivationListener;
    }

    public void setDocument(MindMapDocument document) {
        this.document = document;
        this.selectedNode = document == null ? null : document.getRoot();
        refreshLayout();
    }

    public void setSelectedNode(MindMapNode node, boolean scrollToFit) {
        this.selectedNode = node;
        repaint();
        if (scrollToFit && node != null) {
            LayoutSnapshot.NodePlacement placement = snapshot.getPlacement(node.getId());
            if (placement != null) {
                scrollRectToVisible(toViewBounds(placement.copyBounds()));
            }
        }
    }

    public void refreshLayout() {
        if (document == null) {
            snapshot = new LayoutSnapshot(Map.of());
            setPreferredSize(new Dimension(800, 600));
            revalidate();
            repaint();
            return;
        }

        FontMetrics metrics = getFontMetrics(getFont());
        snapshot = layoutEngine.layout(document, node -> measureNode(node, metrics));

        Rectangle bounds = snapshot.getContentBounds();
        offsetX = CANVAS_MARGIN - bounds.x;
        offsetY = CANVAS_MARGIN - bounds.y;
        int width = Math.max(900, bounds.width + CANVAS_MARGIN * 2);
        int height = Math.max(640, bounds.height + CANVAS_MARGIN * 2);
        setPreferredSize(new Dimension(width, height));
        revalidate();
        repaint();
    }

    public void exportToImage(File file, String format) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(format, "format");
        refreshLayout();

        Rectangle bounds = snapshot.getContentBounds();
        int width = Math.max(1, bounds.width + CANVAS_MARGIN * 2);
        int height = Math.max(1, bounds.height + CANVAS_MARGIN * 2);
        int imageType = "png".equalsIgnoreCase(format)
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;

        BufferedImage image = new BufferedImage(width, height, imageType);
        Graphics2D graphics = image.createGraphics();
        try {
            paintScene(graphics, width, height);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, format, file);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        paintScene((Graphics2D) graphics, getWidth(), getHeight());
    }

    private void paintScene(Graphics2D graphics, int width, int height) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);

        if (document == null || snapshot.getPlacements().isEmpty()) {
            return;
        }

        Graphics2D translated = (Graphics2D) graphics.create();
        try {
            translated.translate(offsetX, offsetY);
            drawConnections(translated);
            drawNodes(translated);
        } finally {
            translated.dispose();
        }
    }

    private void drawConnections(Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(new Color(140, 160, 182));

        for (LayoutSnapshot.NodePlacement placement : snapshot.getPlacements().values()) {
            MindMapNode node = placement.node();
            if (node.isRoot()) {
                continue;
            }

            LayoutSnapshot.NodePlacement parentPlacement = snapshot.getPlacement(node.getParent().getId());
            if (parentPlacement == null) {
                continue;
            }

            Rectangle parentBounds = parentPlacement.copyBounds();
            Rectangle childBounds = placement.copyBounds();
            boolean rightSide = childBounds.getCenterX() >= parentBounds.getCenterX();

            int startX = rightSide ? parentBounds.x + parentBounds.width : parentBounds.x;
            int endX = rightSide ? childBounds.x : childBounds.x + childBounds.width;
            int startY = parentBounds.y + parentBounds.height / 2;
            int endY = childBounds.y + childBounds.height / 2;
            int controlOffset = Math.max(40, Math.abs(endX - startX) / 2);

            CubicCurve2D curve = new CubicCurve2D.Float(
                    startX,
                    startY,
                    rightSide ? startX + controlOffset : startX - controlOffset,
                    startY,
                    rightSide ? endX - controlOffset : endX + controlOffset,
                    endY,
                    endX,
                    endY
            );
            graphics.draw(curve);
        }
    }

    private void drawNodes(Graphics2D graphics) {
        FontMetrics metrics = graphics.getFontMetrics(getFont());
        for (LayoutSnapshot.NodePlacement placement : snapshot.getPlacements().values()) {
            MindMapNode node = placement.node();
            Rectangle bounds = placement.copyBounds();

            graphics.setColor(new Color(0, 0, 0, 18));
            graphics.fill(new RoundRectangle2D.Double(bounds.x + 3, bounds.y + 4, bounds.width, bounds.height, 18, 18));

            boolean selected = selectedNode != null && selectedNode.getId().equals(node.getId());
            Color fillColor;
            Color borderColor;
            if (node.isRoot()) {
                fillColor = selected ? new Color(255, 241, 214) : new Color(227, 241, 255);
                borderColor = selected ? new Color(235, 141, 0) : new Color(56, 116, 203);
            } else {
                fillColor = selected ? new Color(255, 248, 220) : Color.WHITE;
                borderColor = selected ? new Color(235, 141, 0) : new Color(114, 132, 158);
            }

            RoundRectangle2D shape = new RoundRectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height, 18, 18);
            graphics.setColor(fillColor);
            graphics.fill(shape);
            graphics.setColor(borderColor);
            graphics.setStroke(new BasicStroke(selected ? 3.0f : 1.8f));
            graphics.draw(shape);

            graphics.setColor(new Color(33, 43, 54));
            int textWidth = metrics.stringWidth(node.getText());
            int textX = bounds.x + (bounds.width - textWidth) / 2;
            int textY = bounds.y + (bounds.height - metrics.getHeight()) / 2 + metrics.getAscent();
            graphics.drawString(node.getText(), textX, textY);
        }
    }

    private MindMapNode findNodeAt(Point point) {
        Point translated = new Point(point.x - offsetX, point.y - offsetY);
        for (LayoutSnapshot.NodePlacement placement : snapshot.getPlacements().values()) {
            if (placement.bounds().contains(translated)) {
                return placement.node();
            }
        }
        return null;
    }

    private Rectangle toViewBounds(Rectangle nodeBounds) {
        return new Rectangle(
                nodeBounds.x + offsetX - 20,
                nodeBounds.y + offsetY - 20,
                nodeBounds.width + 40,
                nodeBounds.height + 40
        );
    }

    private Dimension measureNode(MindMapNode node, FontMetrics metrics) {
        int width = Math.max(MIN_WIDTH, metrics.stringWidth(node.getText()) + 30);
        int height = Math.max(MIN_HEIGHT, metrics.getHeight() + 16);
        return new Dimension(width, height);
    }
}

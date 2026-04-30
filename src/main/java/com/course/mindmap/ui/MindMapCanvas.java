/*
Script: MindMapCanvas.java
Purpose: Render the mind map canvas and handle direct node interactions, including fill, border, text, and branch styling.
Author: chenyuchong
Created: 2026-03-14
Last Updated: 2026-04-28
Dependencies: Java Swing, AWT, com.course.mindmap.layout, com.course.mindmap.model
Usage: Instantiated by MainFrame as the central drawing surface for the application.

Changelog:
- 2026-03-14 chenyuchong: Initial creation.
- 2026-04-27 陈宗波: Added node right-click context menu callbacks for canvas actions. Original author: chenyuchong. Reason: enable add child, add sibling, and delete actions directly from the drawing area. Impact: backward compatible.
- 2026-04-27 陈宗波: Added support for rendering custom node text, fill, and line colors. Original author: chenyuchong. Reason: allow per-node style customization while preserving selection feedback. Impact: backward compatible.
- 2026-04-28 陈宗波: Simplified rendering to fill-only styling with border-only support. Original author: chenyuchong. Reason: remove unneeded text and line color customization while allowing no-fill nodes. Impact: backward compatible.
- 2026-04-28 陈宗波: Matched node border colors to active fill colors when fills are present. Original author: chenyuchong. Reason: border styling is no longer independently configurable and should follow the fill color. Impact: backward compatible.
- 2026-04-28 陈宗波: Removed node drop shadows for filled nodes. Original author: chenyuchong. Reason: fill styling should render as a flat shape without extra shadow artifacts. Impact: backward compatible.
- 2026-04-28 陈宗波: Added border, text, and branch style rendering with auto-fallback rules. Original author: chenyuchong. Reason: support property-panel-based mind map styling similar to mainstream tools. Impact: backward compatible.
- 2026-04-28 陈宗波: Updated automatic border and branch colors to follow the node's effective fill color. Original author: chenyuchong. Reason: keep automatic styling aligned with the visible node color state. Impact: backward compatible.
- 2026-04-30 chenyuchong: Excluded transient selection highlighting from exported images. Original author: chenyuchong. Reason: exported mind map files should contain only diagram content, not editor state. Impact: backward compatible.
*/
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MindMapCanvas extends JPanel {
    private static final int CANVAS_MARGIN = 80;
    private static final int MIN_WIDTH = 110;
    private static final int MIN_HEIGHT = 38;
    private static final Font BASE_FONT = new Font("Microsoft YaHei UI", Font.PLAIN, 14);
    private static final Color ROOT_FILL_COLOR = new Color(227, 241, 255);
    private static final Color ROOT_BORDER_COLOR = new Color(56, 116, 203);
    private static final Color NODE_FILL_COLOR = Color.WHITE;
    private static final Color NODE_BORDER_COLOR = new Color(114, 132, 158);
    private static final Color NODE_TEXT_COLOR = new Color(33, 43, 54);
    private static final Color BRANCH_FALLBACK_COLOR = Color.BLACK;
    private static final Color SELECTED_HALO_COLOR = new Color(235, 141, 0, 180);

    private final MindMapLayoutEngine layoutEngine = new MindMapLayoutEngine();
    private MindMapDocument document;
    private LayoutSnapshot snapshot = new LayoutSnapshot(Map.of());
    private MindMapNode selectedNode;
    private Consumer<MindMapNode> selectionListener = node -> {
    };
    private Consumer<MindMapNode> nodeActivationListener = node -> {
    };
    private BiConsumer<MindMapNode, Point> nodeContextMenuListener = (node, point) -> {
    };
    private int offsetX = CANVAS_MARGIN;
    private int offsetY = CANVAS_MARGIN;

    public MindMapCanvas() {
        setBackground(Color.WHITE);
        setOpaque(true);
        setFont(BASE_FONT);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                MindMapNode clickedNode = findNodeAt(event.getPoint());
                setSelectedNode(clickedNode, false);
                selectionListener.accept(clickedNode);
                if (clickedNode != null && event.getClickCount() == 2) {
                    nodeActivationListener.accept(clickedNode);
                }
            }

            @Override
            public void mousePressed(MouseEvent event) {
                maybeShowNodeContextMenu(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                maybeShowNodeContextMenu(event);
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

    public void setNodeContextMenuListener(BiConsumer<MindMapNode, Point> nodeContextMenuListener) {
        this.nodeContextMenuListener = nodeContextMenuListener == null ? (node, point) -> {
        } : nodeContextMenuListener;
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

        snapshot = layoutEngine.layout(document, this::measureNode);

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
            paintScene(graphics, width, height, null);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, format, file);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        paintScene((Graphics2D) graphics, getWidth(), getHeight(), selectedNode);
    }

    private void paintScene(Graphics2D graphics, int width, int height, MindMapNode highlightedNode) {
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
            drawNodes(translated, highlightedNode);
        } finally {
            translated.dispose();
        }
    }

    private void drawConnections(Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

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
            graphics.setColor(withAlpha(resolveBranchColor(node), 200));
            graphics.draw(curve);
        }
    }

    private void drawNodes(Graphics2D graphics, MindMapNode highlightedNode) {
        for (LayoutSnapshot.NodePlacement placement : snapshot.getPlacements().values()) {
            MindMapNode node = placement.node();
            Rectangle bounds = placement.copyBounds();
            boolean selected = highlightedNode != null && highlightedNode.getId().equals(node.getId());
            boolean transparentFill = node.isFillTransparent();
            Color fillColor = resolveFillColor(node);
            Color borderColor = resolveBorderColor(node);
            Color textColor = resolveTextColor(node);
            Font nodeFont = resolveFont(node);

            RoundRectangle2D shape = new RoundRectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height, 18, 18);
            RoundRectangle2D haloShape = new RoundRectangle2D.Double(bounds.x - 3, bounds.y - 3, bounds.width + 6, bounds.height + 6, 22, 22);

            if (selected) {
                graphics.setColor(SELECTED_HALO_COLOR);
                graphics.setStroke(new BasicStroke(3.2f));
                graphics.draw(haloShape);
            }

            if (!transparentFill) {
                graphics.setColor(fillColor);
                graphics.fill(shape);
            }

            graphics.setColor(borderColor);
            graphics.setStroke(new BasicStroke(selected ? 2.8f : 1.8f));
            graphics.draw(shape);

            graphics.setFont(nodeFont);
            graphics.setColor(textColor);
            FontMetrics metrics = graphics.getFontMetrics(nodeFont);
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

    private void maybeShowNodeContextMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }

        MindMapNode clickedNode = findNodeAt(event.getPoint());
        if (clickedNode == null) {
            return;
        }

        setSelectedNode(clickedNode, false);
        selectionListener.accept(clickedNode);
        nodeContextMenuListener.accept(clickedNode, event.getPoint());
    }

    private Rectangle toViewBounds(Rectangle nodeBounds) {
        return new Rectangle(
                nodeBounds.x + offsetX - 20,
                nodeBounds.y + offsetY - 20,
                nodeBounds.width + 40,
                nodeBounds.height + 40
        );
    }

    private Dimension measureNode(MindMapNode node) {
        FontMetrics metrics = getFontMetrics(resolveFont(node));
        int width = Math.max(MIN_WIDTH, metrics.stringWidth(node.getText()) + 30);
        int height = Math.max(MIN_HEIGHT, metrics.getHeight() + 16);
        return new Dimension(width, height);
    }

    private Font resolveFont(MindMapNode node) {
        int style = node.isBold() ? Font.BOLD : Font.PLAIN;
        return BASE_FONT.deriveFont(style, (float) node.getFontSize());
    }

    private Color resolveFillColor(MindMapNode node) {
        Color fallback = node.isRoot() ? ROOT_FILL_COLOR : NODE_FILL_COLOR;
        return parseColor(node.getFillColorHex(), fallback);
    }

    private Color resolveBorderColor(MindMapNode node) {
        Color fallback = node.isRoot() ? ROOT_BORDER_COLOR : NODE_BORDER_COLOR;
        if (node.getBorderColorHex() != null) {
            return parseColor(node.getBorderColorHex(), fallback);
        }
        if (!node.isFillTransparent()) {
            return resolveFillColor(node);
        }
        return fallback;
    }

    private Color resolveTextColor(MindMapNode node) {
        return parseColor(node.getTextColorHex(), NODE_TEXT_COLOR);
    }

    private Color resolveBranchColor(MindMapNode node) {
        if (node.getBranchColorHex() != null) {
            return parseColor(node.getBranchColorHex(), BRANCH_FALLBACK_COLOR);
        }
        if (!node.isFillTransparent()) {
            return resolveFillColor(node);
        }
        return BRANCH_FALLBACK_COLOR;
    }

    private Color parseColor(String colorHex, Color fallbackColor) {
        if (colorHex == null || colorHex.isBlank()) {
            return fallbackColor;
        }
        try {
            return Color.decode(colorHex);
        } catch (NumberFormatException exception) {
            return fallbackColor;
        }
    }

    private Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}

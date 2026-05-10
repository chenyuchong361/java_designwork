/*
Script: MindMapCanvas.java
Purpose: Render the mind map canvas and handle direct node interactions, including fill, border, text, and branch styling.
Author: chenyuchong
Created: 2026-03-14
Last Updated: 2026-05-10
Dependencies: Java Swing, AWT, com.course.mindmap.layout, com.course.mindmap.model
Usage: Instantiated by MainFrame as the central drawing surface for the application.

Changelog:
- 2026-03-14 chenyuchong: Initial creation.
- 2026-04-27 Codex: Added node right-click context menu callbacks for canvas actions. Original author: chenyuchong. Reason: enable add child, add sibling, and delete actions directly from the drawing area. Impact: backward compatible.
- 2026-04-27 Codex: Added support for rendering custom node text, fill, and line colors. Original author: chenyuchong. Reason: allow per-node style customization while preserving selection feedback. Impact: backward compatible.
- 2026-04-28 Codex: Simplified rendering to fill-only styling with border-only support. Original author: chenyuchong. Reason: remove unneeded text and line color customization while allowing no-fill nodes. Impact: backward compatible.
- 2026-04-28 Codex: Matched node border colors to active fill colors when fills are present. Original author: chenyuchong. Reason: border styling is no longer independently configurable and should follow the fill color. Impact: backward compatible.
- 2026-04-28 Codex: Removed node drop shadows for filled nodes. Original author: chenyuchong. Reason: fill styling should render as a flat shape without extra shadow artifacts. Impact: backward compatible.
- 2026-04-28 Codex: Added border, text, and branch style rendering with auto-fallback rules. Original author: chenyuchong. Reason: support property-panel-based mind map styling similar to mainstream tools. Impact: backward compatible.
- 2026-04-28 Codex: Updated automatic border and branch colors to follow the node's effective fill color. Original author: chenyuchong. Reason: keep automatic styling aligned with the visible node color state. Impact: backward compatible.
- 2026-04-30 温文辉: Preserved blank-area deselection behavior and kept canvas callbacks aligned with tree synchronization. Original author: chenyuchong. Reason: stabilize task B selection flow across drawing and structure views. Impact: backward compatible.
- 2026-05-10 Codex: Added direct node dragging with synchronized branch rerouting and position-change callbacks. Original author: chenyuchong. Reason: let users reposition modules freely on the canvas while keeping visual links up to date. Impact: backward compatible.
- 2026-05-10 Codex: Restricted dragging to the center node only. Original author: chenyuchong. Reason: child-node free dragging produced unstable branch geometry and was replaced with root-only repositioning. Impact: backward compatible.
- 2026-05-10 Codex: Added centered viewport focusing for selected nodes. Original author: chenyuchong. Reason: keep the root node visible in the middle of the canvas when a document is first opened or recentered. Impact: backward compatible.
- 2026-05-10 Codex: Deferred viewport centering until the real visible extent is ready. Original author: chenyuchong. Reason: early centering used the full virtual canvas size instead of the actual scroll viewport, which left the root node parked in the lower-right area on startup. Impact: backward compatible.
- 2026-05-10 Codex: Replaced the fixed virtual origin with viewport-based canvas anchoring. Original author: chenyuchong. Reason: the old large translation constants always pushed the root node into the lower-right area on startup instead of drawing it at the visible center. Impact: backward compatible.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

public class MindMapCanvas extends JPanel {
    private static final int CANVAS_MARGIN = 80;
    private static final int CENTERING_MAX_ATTEMPTS = 8;
    private static final int CENTERING_TOLERANCE = 2;
    private static final int DEFAULT_VIEWPORT_WIDTH = 1200;
    private static final int DEFAULT_VIEWPORT_HEIGHT = 760;
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
    private Consumer<MindMapNode> nodePositionChangeListener = node -> {
    };
    private BiConsumer<MindMapNode, Point> nodeContextMenuListener = (node, point) -> {
    };
    private int offsetX = CANVAS_MARGIN;
    private int offsetY = CANVAS_MARGIN;
    private MindMapNode draggedNode;
    private Point lastDragPoint;
    private boolean draggingNode;

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
                if (SwingUtilities.isLeftMouseButton(event)) {
                    MindMapNode candidateNode = findNodeAt(event.getPoint());
                    draggedNode = candidateNode != null && candidateNode.isRoot() ? candidateNode : null;
                    lastDragPoint = draggedNode == null ? null : event.getPoint();
                    draggingNode = false;
                    if (draggedNode != null && !Objects.equals(selectedNode, draggedNode)) {
                        setSelectedNode(draggedNode, false);
                        selectionListener.accept(draggedNode);
                    }
                }
                maybeShowNodeContextMenu(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    draggingNode = false;
                    draggedNode = null;
                    lastDragPoint = null;
                }
                maybeShowNodeContextMenu(event);
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0
                        || draggedNode == null
                        || lastDragPoint == null) {
                    return;
                }

                Point currentPoint = event.getPoint();
                int deltaX = currentPoint.x - lastDragPoint.x;
                int deltaY = currentPoint.y - lastDragPoint.y;
                if (deltaX == 0 && deltaY == 0) {
                    return;
                }

                draggedNode.translateManualOffset(deltaX, deltaY);
                lastDragPoint = currentPoint;
                draggingNode = true;
                refreshLayout();
                nodePositionChangeListener.accept(draggedNode);
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    public void setSelectionListener(Consumer<MindMapNode> selectionListener) {
        this.selectionListener = selectionListener == null ? node -> {
        } : selectionListener;
    }

    public void setNodeActivationListener(Consumer<MindMapNode> nodeActivationListener) {
        this.nodeActivationListener = nodeActivationListener == null ? node -> {
        } : nodeActivationListener;
    }

    public void setNodePositionChangeListener(Consumer<MindMapNode> nodePositionChangeListener) {
        this.nodePositionChangeListener = nodePositionChangeListener == null ? node -> {
        } : nodePositionChangeListener;
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

    public void centerNodeInView(MindMapNode node) {
        centerNodeInView(node, 0);
    }

    private void centerNodeInView(MindMapNode node, int attempt) {
        if (node == null) {
            return;
        }

        Runnable centerTask = () -> {
            LayoutSnapshot.NodePlacement placement = snapshot.getPlacement(node.getId());
            if (placement == null) {
                return;
            }

            Rectangle viewBounds = toViewBounds(placement.copyBounds());
            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
            if (viewport == null) {
                scrollRectToVisible(viewBounds);
                return;
            }

            Dimension extent = viewport.getExtentSize();
            if (attempt < CENTERING_MAX_ATTEMPTS && shouldWaitForViewport(extent)) {
                SwingUtilities.invokeLater(() -> centerNodeInView(node, attempt + 1));
                return;
            }

            Rectangle visibleRect = viewport.getViewRect();
            int targetX = viewBounds.x + viewBounds.width / 2 - extent.width / 2;
            int targetY = viewBounds.y + viewBounds.height / 2 - extent.height / 2;
            int maxX = Math.max(0, getWidth() - extent.width);
            int maxY = Math.max(0, getHeight() - extent.height);
            viewport.setViewPosition(new Point(
                    Math.max(0, Math.min(targetX, maxX)),
                    Math.max(0, Math.min(targetY, maxY))
            ));

            if (attempt < CENTERING_MAX_ATTEMPTS && !isCentered(viewBounds, viewport.getViewRect())) {
                SwingUtilities.invokeLater(() -> centerNodeInView(node, attempt + 1));
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            centerTask.run();
        } else if (isShowing()) {
            SwingUtilities.invokeLater(centerTask);
        } else {
            centerTask.run();
        }
    }

    private boolean shouldWaitForViewport(Dimension extent) {
        return extent.width <= 0
                || extent.height <= 0
                || (getWidth() > 0 && getHeight() > 0
                && extent.width >= getWidth()
                && extent.height >= getHeight());
    }

    private boolean isCentered(Rectangle viewBounds, Rectangle visibleRect) {
        int nodeCenterX = viewBounds.x + viewBounds.width / 2;
        int nodeCenterY = viewBounds.y + viewBounds.height / 2;
        int viewCenterX = visibleRect.x + visibleRect.width / 2;
        int viewCenterY = visibleRect.y + visibleRect.height / 2;
        return Math.abs(nodeCenterX - viewCenterX) <= CENTERING_TOLERANCE
                && Math.abs(nodeCenterY - viewCenterY) <= CENTERING_TOLERANCE;
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
        Dimension viewportExtent = resolveViewportExtent();
        int horizontalAnchor = Math.max(CANVAS_MARGIN, viewportExtent.width / 2);
        int verticalAnchor = Math.max(CANVAS_MARGIN, viewportExtent.height / 2);

        offsetX = Math.max(horizontalAnchor, CANVAS_MARGIN - bounds.x);
        offsetY = Math.max(verticalAnchor, CANVAS_MARGIN - bounds.y);

        int width = Math.max(viewportExtent.width, bounds.x + bounds.width + offsetX + horizontalAnchor);
        int height = Math.max(viewportExtent.height, bounds.y + bounds.height + offsetY + verticalAnchor);
        setPreferredSize(new Dimension(width, height));
        revalidate();
        repaint();
    }

    private Dimension resolveViewportExtent() {
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
        if (viewport != null) {
            Dimension extent = viewport.getExtentSize();
            if (extent.width > 0 && extent.height > 0) {
                return extent;
            }
        }

        Rectangle visibleRect = getVisibleRect();
        if (visibleRect.width > 0 && visibleRect.height > 0) {
            return visibleRect.getSize();
        }

        Dimension parentSize = getParent() == null ? null : getParent().getSize();
        if (parentSize != null && parentSize.width > 0 && parentSize.height > 0) {
            return parentSize;
        }

        Dimension currentSize = getSize();
        if (currentSize.width > 0 && currentSize.height > 0) {
            return currentSize;
        }

        return new Dimension(DEFAULT_VIEWPORT_WIDTH, DEFAULT_VIEWPORT_HEIGHT);
    }

    public void exportToImage(File file, String format) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(format, "format");
        refreshLayout();

        Rectangle bounds = snapshot.getContentBounds();
        int width = Math.max(1, bounds.width + CANVAS_MARGIN * 2);
        int height = Math.max(1, bounds.height + CANVAS_MARGIN * 2);
        int exportOffsetX = CANVAS_MARGIN - bounds.x;
        int exportOffsetY = CANVAS_MARGIN - bounds.y;
        int imageType = "png".equalsIgnoreCase(format)
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;

        BufferedImage image = new BufferedImage(width, height, imageType);
        Graphics2D graphics = image.createGraphics();
        try {
            paintScene(graphics, width, height, exportOffsetX, exportOffsetY);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, format, file);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        paintScene((Graphics2D) graphics, getWidth(), getHeight(), offsetX, offsetY);
    }

    private void paintScene(Graphics2D graphics, int width, int height, int translateX, int translateY) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);

        if (document == null || snapshot.getPlacements().isEmpty()) {
            return;
        }

        Graphics2D translated = (Graphics2D) graphics.create();
        try {
            translated.translate(translateX, translateY);
            drawConnections(translated);
            drawNodes(translated);
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

    private void drawNodes(Graphics2D graphics) {
        for (LayoutSnapshot.NodePlacement placement : snapshot.getPlacements().values()) {
            MindMapNode node = placement.node();
            Rectangle bounds = placement.copyBounds();
            boolean selected = selectedNode != null && selectedNode.getId().equals(node.getId());
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
        List<LayoutSnapshot.NodePlacement> placements = new ArrayList<>(snapshot.getPlacements().values());
        for (int index = placements.size() - 1; index >= 0; index--) {
            LayoutSnapshot.NodePlacement placement = placements.get(index);
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

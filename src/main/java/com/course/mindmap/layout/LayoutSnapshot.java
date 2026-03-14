package com.course.mindmap.layout;

import com.course.mindmap.model.MindMapNode;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class LayoutSnapshot {
    private final Map<String, NodePlacement> placements;
    private final Rectangle contentBounds;

    public LayoutSnapshot(Map<String, NodePlacement> placements) {
        this.placements = Collections.unmodifiableMap(new LinkedHashMap<>(placements));
        this.contentBounds = calculateBounds(placements);
    }

    public Map<String, NodePlacement> getPlacements() {
        return placements;
    }

    public NodePlacement getPlacement(String nodeId) {
        return placements.get(nodeId);
    }

    public Rectangle getContentBounds() {
        return new Rectangle(contentBounds);
    }

    private Rectangle calculateBounds(Map<String, NodePlacement> placements) {
        Rectangle bounds = null;
        for (NodePlacement placement : placements.values()) {
            Rectangle nodeBounds = placement.copyBounds();
            if (bounds == null) {
                bounds = nodeBounds;
            } else {
                bounds.add(nodeBounds);
            }
        }
        return bounds == null ? new Rectangle(0, 0, 0, 0) : bounds;
    }

    public record NodePlacement(MindMapNode node, Rectangle bounds) {
        public Rectangle copyBounds() {
            return new Rectangle(bounds);
        }
    }
}


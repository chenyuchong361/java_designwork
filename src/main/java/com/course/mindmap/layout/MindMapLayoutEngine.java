/*
Script: MindMapLayoutEngine.java
Purpose: Compute automatic node placement and blend it with user-adjusted manual offsets.
Author: chenyuchong
Created: 2026-03-14
Last Updated: 2026-05-10
Dependencies: java.awt, java.util, com.course.mindmap.model
Usage: Invoked by MindMapCanvas to create the current placement snapshot for rendering and export.

Changelog:
- 2026-03-14 chenyuchong: Initial creation.
- 2026-05-10 Codex: Applied persisted manual node offsets on top of automatic layout positions. Original author: chenyuchong. Reason: keep branch routing aligned when users drag modules away from the default layout. Impact: backward compatible.
- 2026-05-10 Codex: Limited manual offset application to the center node. Original author: chenyuchong. Reason: prevent existing child offsets from distorting the automatic branch layout. Impact: backward compatible.
*/
package com.course.mindmap.layout;

import com.course.mindmap.model.LayoutMode;
import com.course.mindmap.model.MindMapDocument;
import com.course.mindmap.model.MindMapNode;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class MindMapLayoutEngine {
    private static final int HORIZONTAL_GAP = 100;
    private static final int VERTICAL_GAP = 24;

    public LayoutSnapshot layout(MindMapDocument document, Function<MindMapNode, Dimension> sizeProvider) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(sizeProvider, "sizeProvider");

        MindMapNode root = document.getRoot();
        Map<String, Integer> subtreeHeights = new HashMap<>();
        computeSubtreeHeight(root, sizeProvider, subtreeHeights);

        Map<String, LayoutSnapshot.NodePlacement> placements = new LinkedHashMap<>();
        Dimension rootSize = sizeProvider.apply(root);
        Rectangle rootBounds = new Rectangle(
                -rootSize.width / 2,
                -rootSize.height / 2,
                rootSize.width,
                rootSize.height
        );
        rootBounds.translate(root.getManualOffsetX(), root.getManualOffsetY());
        placements.put(root.getId(), new LayoutSnapshot.NodePlacement(root, rootBounds));

        List<MindMapNode> rootChildren = root.getChildren();
        if (rootChildren.isEmpty()) {
            return new LayoutSnapshot(placements);
        }

        if (document.getLayoutMode() == LayoutMode.LEFT) {
            layoutChildGroup(rootBounds, rootChildren, Side.LEFT, sizeProvider, subtreeHeights, placements);
        } else if (document.getLayoutMode() == LayoutMode.RIGHT) {
            layoutChildGroup(rootBounds, rootChildren, Side.RIGHT, sizeProvider, subtreeHeights, placements);
        } else {
            RootBuckets buckets = splitRootChildren(rootChildren, subtreeHeights);
            layoutChildGroup(rootBounds, buckets.rightNodes, Side.RIGHT, sizeProvider, subtreeHeights, placements);
            layoutChildGroup(rootBounds, buckets.leftNodes, Side.LEFT, sizeProvider, subtreeHeights, placements);
        }

        return new LayoutSnapshot(placements);
    }

    private int computeSubtreeHeight(
            MindMapNode node,
            Function<MindMapNode, Dimension> sizeProvider,
            Map<String, Integer> subtreeHeights
    ) {
        Dimension size = sizeProvider.apply(node);
        if (node.getChildren().isEmpty()) {
            subtreeHeights.put(node.getId(), size.height);
            return size.height;
        }

        int childrenHeight = accumulateGroupHeight(node.getChildren(), subtreeHeights, sizeProvider);
        int subtreeHeight = Math.max(size.height, childrenHeight);
        subtreeHeights.put(node.getId(), subtreeHeight);
        return subtreeHeight;
    }

    private int accumulateGroupHeight(
            List<MindMapNode> nodes,
            Map<String, Integer> subtreeHeights,
            Function<MindMapNode, Dimension> sizeProvider
    ) {
        int total = 0;
        for (MindMapNode child : nodes) {
            int childHeight = subtreeHeights.containsKey(child.getId())
                    ? subtreeHeights.get(child.getId())
                    : computeSubtreeHeight(child, sizeProvider, subtreeHeights);
            if (total > 0) {
                total += VERTICAL_GAP;
            }
            total += childHeight;
        }
        return total;
    }

    private void layoutChildGroup(
            Rectangle parentBounds,
            List<MindMapNode> children,
            Side side,
            Function<MindMapNode, Dimension> sizeProvider,
            Map<String, Integer> subtreeHeights,
            Map<String, LayoutSnapshot.NodePlacement> placements
    ) {
        if (children.isEmpty()) {
            return;
        }

        int totalHeight = 0;
        for (MindMapNode child : children) {
            if (totalHeight > 0) {
                totalHeight += VERTICAL_GAP;
            }
            totalHeight += subtreeHeights.get(child.getId());
        }

        int cursorY = parentBounds.y + parentBounds.height / 2 - totalHeight / 2;
        for (MindMapNode child : children) {
            int subtreeHeight = subtreeHeights.get(child.getId());
            int centerY = cursorY + subtreeHeight / 2;
            placeNode(parentBounds, child, centerY, side, sizeProvider, subtreeHeights, placements);
            cursorY += subtreeHeight + VERTICAL_GAP;
        }
    }

    private void placeNode(
            Rectangle parentBounds,
            MindMapNode node,
            int centerY,
            Side side,
            Function<MindMapNode, Dimension> sizeProvider,
            Map<String, Integer> subtreeHeights,
            Map<String, LayoutSnapshot.NodePlacement> placements
    ) {
        Dimension size = sizeProvider.apply(node);
        int x = side == Side.RIGHT
                ? parentBounds.x + parentBounds.width + HORIZONTAL_GAP
                : parentBounds.x - HORIZONTAL_GAP - size.width;
        Rectangle bounds = new Rectangle(x, centerY - size.height / 2, size.width, size.height);
        if (node.isRoot()) {
            bounds.translate(node.getManualOffsetX(), node.getManualOffsetY());
        }
        placements.put(node.getId(), new LayoutSnapshot.NodePlacement(node, bounds));
        layoutChildGroup(bounds, node.getChildren(), side, sizeProvider, subtreeHeights, placements);
    }

    private RootBuckets splitRootChildren(List<MindMapNode> rootChildren, Map<String, Integer> subtreeHeights) {
        List<MindMapNode> leftNodes = new ArrayList<>();
        List<MindMapNode> rightNodes = new ArrayList<>();
        int leftHeight = 0;
        int rightHeight = 0;

        for (MindMapNode child : rootChildren) {
            int childHeight = subtreeHeights.get(child.getId());
            boolean placeOnRight = rightHeight <= leftHeight;
            if (placeOnRight) {
                rightHeight = appendHeight(rightHeight, childHeight);
                rightNodes.add(child);
            } else {
                leftHeight = appendHeight(leftHeight, childHeight);
                leftNodes.add(child);
            }
        }

        return new RootBuckets(leftNodes, rightNodes);
    }

    private int appendHeight(int currentHeight, int nextHeight) {
        return currentHeight == 0 ? nextHeight : currentHeight + VERTICAL_GAP + nextHeight;
    }

    private record RootBuckets(List<MindMapNode> leftNodes, List<MindMapNode> rightNodes) {
    }

    private enum Side {
        LEFT,
        RIGHT
    }
}


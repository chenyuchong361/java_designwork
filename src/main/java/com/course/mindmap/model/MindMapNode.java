/*
Script: MindMapNode.java
Purpose: Represent a mind map node, including hierarchy and custom visual style settings.
Author: chenyuchong
Created: 2026-03-14
Last Updated: 2026-04-27
Dependencies: java.util
Usage: Used by the document, UI, and file persistence layers to manage node data.

Changelog:
- 2026-03-14 chenyuchong: Initial creation.
- 2026-04-27 Codex: Added per-node text, fill, and line color properties for customizable rendering. Original author: chenyuchong. Reason: support user-controlled node styling that can be saved and restored. Impact: backward compatible.
*/
package com.course.mindmap.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class MindMapNode {
    private final String id;
    private String text;
    private String textColorHex;
    private String fillColorHex;
    private String lineColorHex;
    private MindMapNode parent;
    private final List<MindMapNode> children = new ArrayList<>();

    public MindMapNode(String text) {
        this(UUID.randomUUID().toString(), text);
    }

    public MindMapNode(String id, String text) {
        this.id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        setText(text);
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        String normalized = text == null ? "" : text.strip();
        this.text = normalized.isBlank() ? "\u65b0\u8282\u70b9" : normalized;
    }

    public String getTextColorHex() {
        return textColorHex;
    }

    public void setTextColorHex(String textColorHex) {
        this.textColorHex = normalizeColorHex(textColorHex);
    }

    public String getFillColorHex() {
        return fillColorHex;
    }

    public void setFillColorHex(String fillColorHex) {
        this.fillColorHex = normalizeColorHex(fillColorHex);
    }

    public String getLineColorHex() {
        return lineColorHex;
    }

    public void setLineColorHex(String lineColorHex) {
        this.lineColorHex = normalizeColorHex(lineColorHex);
    }

    public MindMapNode getParent() {
        return parent;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public List<MindMapNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public MindMapNode addChild(String text) {
        MindMapNode child = new MindMapNode(text);
        addChild(child);
        return child;
    }

    public void addChild(MindMapNode child) {
        insertChild(children.size(), child);
    }

    public void insertChild(int index, MindMapNode child) {
        Objects.requireNonNull(child, "child");
        if (child == this) {
            throw new IllegalArgumentException("A node cannot be added as its own child.");
        }
        if (child.parent != null) {
            child.parent.removeChild(child);
        }
        int safeIndex = Math.max(0, Math.min(index, children.size()));
        child.parent = this;
        children.add(safeIndex, child);
    }

    public MindMapNode addSiblingAfter(String text) {
        if (parent == null) {
            throw new IllegalStateException("Root node does not support siblings.");
        }
        MindMapNode sibling = new MindMapNode(text);
        int index = parent.children.indexOf(this);
        parent.insertChild(index + 1, sibling);
        return sibling;
    }

    public void removeChild(MindMapNode child) {
        if (children.remove(child)) {
            child.parent = null;
        }
    }

    public boolean hasCustomStyle() {
        return textColorHex != null || fillColorHex != null || lineColorHex != null;
    }

    public void clearCustomStyle() {
        textColorHex = null;
        fillColorHex = null;
        lineColorHex = null;
    }

    public MindMapNode findById(String nodeId) {
        if (id.equals(nodeId)) {
            return this;
        }
        for (MindMapNode child : children) {
            MindMapNode found = child.findById(nodeId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public int countNodes() {
        int count = 1;
        for (MindMapNode child : children) {
            count += child.countNodes();
        }
        return count;
    }

    private String normalizeColorHex(String colorHex) {
        if (colorHex == null) {
            return null;
        }

        String normalized = colorHex.strip().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.matches("[0-9A-F]{6}")) {
            throw new IllegalArgumentException("Color value must be a 6-digit hex string.");
        }
        return "#" + normalized;
    }

    @Override
    public String toString() {
        return text;
    }
}

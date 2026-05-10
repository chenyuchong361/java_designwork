/*
Script: MindMapNode.java
Purpose: Represent a mind map node, including hierarchy and configurable visual properties.
Author: chenyuchong
Created: 2026-03-14
Last Updated: 2026-05-10
Dependencies: java.util
Usage: Used by the document, UI, and file persistence layers to manage node data.

Changelog:
- 2026-03-14 chenyuchong: Initial creation.
- 2026-04-27 陈宗波: Added per-node text, fill, and line color properties for customizable rendering. Original author: chenyuchong. Reason: support user-controlled node styling that can be saved and restored. Impact: backward compatible.
- 2026-04-28 陈宗波: Reduced styling to fill-only state with no-fill support. Original author: chenyuchong. Reason: keep module styling focused on background fill while allowing border-only display. Impact: backward compatible.
- 2026-04-28 陈宗波: Added node border, text, and branch style properties for the right-click property panel. Original author: chenyuchong. Reason: match modern mind map styling workflows with localized property editing. Impact: backward compatible.
- 2026-05-10 Codex: Added per-node manual position offsets for drag-based layout adjustment. Original author: chenyuchong. Reason: allow users to move modules freely while keeping custom positions available to rendering and persistence logic. Impact: backward compatible.
*/
package com.course.mindmap.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class MindMapNode {
    public static final int DEFAULT_FONT_SIZE = 14;

    private final String id;
    private String text;
    private String fillColorHex;
    private boolean fillTransparent;
    private String borderColorHex;
    private String textColorHex;
    private int fontSize = DEFAULT_FONT_SIZE;
    private boolean bold;
    private String branchColorHex;
    private int manualOffsetX;
    private int manualOffsetY;
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
        this.text = normalized.isBlank() ? "新节点" : normalized;
    }

    public String getFillColorHex() {
        return fillColorHex;
    }

    public void setFillColorHex(String fillColorHex) {
        this.fillColorHex = normalizeColorHex(fillColorHex);
        if (this.fillColorHex != null) {
            this.fillTransparent = false;
        }
    }

    public boolean isFillTransparent() {
        return fillTransparent;
    }

    public void setFillTransparent(boolean fillTransparent) {
        this.fillTransparent = fillTransparent;
        if (fillTransparent) {
            this.fillColorHex = null;
        }
    }

    public String getBorderColorHex() {
        return borderColorHex;
    }

    public void setBorderColorHex(String borderColorHex) {
        this.borderColorHex = normalizeColorHex(borderColorHex);
    }

    public String getTextColorHex() {
        return textColorHex;
    }

    public void setTextColorHex(String textColorHex) {
        this.textColorHex = normalizeColorHex(textColorHex);
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = Math.max(8, Math.min(72, fontSize));
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public String getBranchColorHex() {
        return branchColorHex;
    }

    public void setBranchColorHex(String branchColorHex) {
        this.branchColorHex = normalizeColorHex(branchColorHex);
    }

    public int getManualOffsetX() {
        return manualOffsetX;
    }

    public void setManualOffsetX(int manualOffsetX) {
        this.manualOffsetX = manualOffsetX;
    }

    public int getManualOffsetY() {
        return manualOffsetY;
    }

    public void setManualOffsetY(int manualOffsetY) {
        this.manualOffsetY = manualOffsetY;
    }

    public void setManualOffset(int manualOffsetX, int manualOffsetY) {
        this.manualOffsetX = manualOffsetX;
        this.manualOffsetY = manualOffsetY;
    }

    public void translateManualOffset(int deltaX, int deltaY) {
        this.manualOffsetX += deltaX;
        this.manualOffsetY += deltaY;
    }

    public boolean hasManualOffset() {
        return manualOffsetX != 0 || manualOffsetY != 0;
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

    public boolean hasCustomFillStyle() {
        return fillColorHex != null || fillTransparent;
    }

    public void clearCustomFillStyle() {
        fillColorHex = null;
        fillTransparent = false;
    }

    public boolean hasCustomBorderStyle() {
        return borderColorHex != null;
    }

    public void clearCustomBorderStyle() {
        borderColorHex = null;
    }

    public boolean hasCustomTextStyle() {
        return textColorHex != null || fontSize != DEFAULT_FONT_SIZE || bold;
    }

    public void clearCustomTextStyle() {
        textColorHex = null;
        fontSize = DEFAULT_FONT_SIZE;
        bold = false;
    }

    public boolean hasCustomBranchStyle() {
        return branchColorHex != null;
    }

    public void clearCustomBranchStyle() {
        branchColorHex = null;
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

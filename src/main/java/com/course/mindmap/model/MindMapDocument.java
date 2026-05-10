/*
Script: MindMapDocument.java
Purpose: Hold the current mind map title, root node, layout mode, and document-level state helpers.
Author: chenyuchong
Created: 2026-03-14
Last Updated: 2026-05-10
Dependencies: java.util, com.course.mindmap.model
Usage: Used by the UI, persistence layer, and editing workflows to manage the active document.

Changelog:
- 2026-03-14 chenyuchong: Initial creation.
- 2026-05-10 Codex: Added deep-copy restoration helpers for document undo snapshots. Original author: chenyuchong. Reason: enable Ctrl+Z to restore a previous mind map state without rebuilding all editing flows. Impact: backward compatible.
*/
package com.course.mindmap.model;

import java.util.Objects;

public class MindMapDocument {
    private String title;
    private MindMapNode root;
    private LayoutMode layoutMode;

    public MindMapDocument(String title, MindMapNode root, LayoutMode layoutMode) {
        this.root = Objects.requireNonNull(root, "root");
        this.layoutMode = layoutMode == null ? LayoutMode.AUTO : layoutMode;
        setTitle(title);
    }

    public static MindMapDocument createBlank() {
        MindMapNode root = new MindMapNode("中心主题");
        return new MindMapDocument("中心主题", root, LayoutMode.AUTO);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        String normalized = title == null ? "" : title.strip();
        this.title = normalized.isBlank() ? root.getText() : normalized;
    }

    public MindMapNode getRoot() {
        return root;
    }

    public void restoreFrom(MindMapDocument source) {
        Objects.requireNonNull(source, "source");
        this.title = source.title;
        this.layoutMode = source.layoutMode;
        this.root = copyNode(source.root);
    }

    public MindMapDocument copy() {
        return new MindMapDocument(title, copyNode(root), layoutMode);
    }

    public LayoutMode getLayoutMode() {
        return layoutMode;
    }

    public void setLayoutMode(LayoutMode layoutMode) {
        this.layoutMode = layoutMode == null ? LayoutMode.AUTO : layoutMode;
    }

    public MindMapNode findNodeById(String nodeId) {
        return root.findById(nodeId);
    }

    private MindMapNode copyNode(MindMapNode sourceNode) {
        MindMapNode copiedNode = new MindMapNode(sourceNode.getId(), sourceNode.getText());
        copiedNode.setFillColorHex(sourceNode.getFillColorHex());
        copiedNode.setFillTransparent(sourceNode.isFillTransparent());
        copiedNode.setBorderColorHex(sourceNode.getBorderColorHex());
        copiedNode.setTextColorHex(sourceNode.getTextColorHex());
        copiedNode.setFontSize(sourceNode.getFontSize());
        copiedNode.setBold(sourceNode.isBold());
        copiedNode.setBranchColorHex(sourceNode.getBranchColorHex());
        copiedNode.setManualOffset(sourceNode.getManualOffsetX(), sourceNode.getManualOffsetY());
        for (MindMapNode child : sourceNode.getChildren()) {
            copiedNode.addChild(copyNode(child));
        }
        return copiedNode;
    }
}


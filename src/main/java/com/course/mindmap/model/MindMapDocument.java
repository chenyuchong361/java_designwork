package com.course.mindmap.model;

import java.util.Objects;

public class MindMapDocument {
    private String title;
    private final MindMapNode root;
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

    public LayoutMode getLayoutMode() {
        return layoutMode;
    }

    public void setLayoutMode(LayoutMode layoutMode) {
        this.layoutMode = layoutMode == null ? LayoutMode.AUTO : layoutMode;
    }

    public MindMapNode findNodeById(String nodeId) {
        return root.findById(nodeId);
    }
}


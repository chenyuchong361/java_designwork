package com.course.mindmap.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MindMapNode {
    private final String id;
    private String text;
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

    @Override
    public String toString() {
        return text;
    }
}


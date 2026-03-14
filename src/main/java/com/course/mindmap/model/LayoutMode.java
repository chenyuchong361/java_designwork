package com.course.mindmap.model;

public enum LayoutMode {
    AUTO("自动布局"),
    LEFT("左侧布局"),
    RIGHT("右侧布局");

    private final String displayName;

    LayoutMode(String displayName) {
        this.displayName = displayName;
    }

    public static LayoutMode fromPersistentName(String value) {
        for (LayoutMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return AUTO;
    }

    @Override
    public String toString() {
        return displayName;
    }
}


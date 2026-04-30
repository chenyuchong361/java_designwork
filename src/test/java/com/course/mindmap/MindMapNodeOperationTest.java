/*
Script: MindMapNodeOperationTest.java
Purpose: Verify node insertion, sibling creation, reparenting, and deletion behavior used by the editor.
Author: chenyuchong
Created: 2026-04-30
Last Updated: 2026-04-30
Dependencies: JUnit 5, com.course.mindmap.model
Usage: Run with the Maven test phase.

Changelog:
- 2026-04-30 chenyuchong: Initial creation.
*/
package com.course.mindmap;

import com.course.mindmap.model.MindMapDocument;
import com.course.mindmap.model.MindMapNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MindMapNodeOperationTest {
    @Test
    void addSiblingAfterShouldInsertNodeImmediatelyAfterCurrentNode() {
        MindMapDocument document = MindMapDocument.createBlank();
        MindMapNode root = document.getRoot();
        MindMapNode first = root.addChild("第一个");
        MindMapNode second = root.addChild("第二个");

        MindMapNode inserted = first.addSiblingAfter("插入节点");

        assertEquals(3, root.getChildren().size());
        assertSame(first, root.getChildren().get(0));
        assertSame(inserted, root.getChildren().get(1));
        assertSame(second, root.getChildren().get(2));
        assertSame(root, inserted.getParent());
    }

    @Test
    void addChildShouldReparentExistingNode() {
        MindMapDocument document = MindMapDocument.createBlank();
        MindMapNode root = document.getRoot();
        MindMapNode left = root.addChild("左侧主题");
        MindMapNode right = root.addChild("右侧主题");
        MindMapNode child = left.addChild("待迁移节点");

        right.addChild(child);

        assertEquals(0, left.getChildren().size());
        assertEquals(1, right.getChildren().size());
        assertSame(right, child.getParent());
        assertSame(child, right.getChildren().get(0));
    }

    @Test
    void removeChildShouldDetachNodeFromParent() {
        MindMapDocument document = MindMapDocument.createBlank();
        MindMapNode root = document.getRoot();
        MindMapNode branch = root.addChild("可删除分支");
        branch.addChild("子节点");

        root.removeChild(branch);

        assertEquals(0, root.getChildren().size());
        assertNull(branch.getParent());
        assertEquals(2, branch.countNodes());
    }

    @Test
    void rootShouldNotAllowSiblingCreation() {
        MindMapNode root = MindMapDocument.createBlank().getRoot();

        assertThrows(IllegalStateException.class, () -> root.addSiblingAfter("非法兄弟"));
    }
}

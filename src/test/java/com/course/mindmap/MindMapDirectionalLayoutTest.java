/*
Script: MindMapDirectionalLayoutTest.java
Purpose: Verify that left and right layout modes place root descendants on the expected side of the center node.
Author: chenyuchong
Created: 2026-04-30
Last Updated: 2026-04-30
Dependencies: JUnit 5, java.awt, com.course.mindmap.layout, com.course.mindmap.model
Usage: Run with the Maven test phase.

Changelog:
- 2026-04-30 chenyuchong: Initial creation.
*/
package com.course.mindmap;

import com.course.mindmap.layout.LayoutSnapshot;
import com.course.mindmap.layout.MindMapLayoutEngine;
import com.course.mindmap.model.LayoutMode;
import com.course.mindmap.model.MindMapDocument;
import com.course.mindmap.model.MindMapNode;
import java.awt.Dimension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MindMapDirectionalLayoutTest {
    @Test
    void leftLayoutShouldPlaceAllDescendantsLeftOfTheRoot() {
        MindMapDocument document = buildDocument(LayoutMode.LEFT);

        LayoutSnapshot snapshot = new MindMapLayoutEngine().layout(document, node -> new Dimension(120, 40));

        assertAllDescendantsOnExpectedSide(document, snapshot, true);
    }

    @Test
    void rightLayoutShouldPlaceAllDescendantsRightOfTheRoot() {
        MindMapDocument document = buildDocument(LayoutMode.RIGHT);

        LayoutSnapshot snapshot = new MindMapLayoutEngine().layout(document, node -> new Dimension(120, 40));

        assertAllDescendantsOnExpectedSide(document, snapshot, false);
    }

    private MindMapDocument buildDocument(LayoutMode layoutMode) {
        MindMapDocument document = MindMapDocument.createBlank();
        document.setLayoutMode(layoutMode);

        MindMapNode root = document.getRoot();
        MindMapNode analysis = root.addChild("需求分析");
        analysis.addChild("用户故事");
        analysis.addChild("数据结构");

        MindMapNode export = root.addChild("图像导出");
        export.addChild("PNG");
        export.addChild("JPG");
        return document;
    }

    private void assertAllDescendantsOnExpectedSide(
            MindMapDocument document,
            LayoutSnapshot snapshot,
            boolean shouldBeLeft
    ) {
        double rootCenterX = snapshot.getPlacement(document.getRoot().getId()).bounds().getCenterX();
        for (MindMapNode child : document.getRoot().getChildren()) {
            assertNodeSide(child, snapshot, rootCenterX, shouldBeLeft);
        }
    }

    private void assertNodeSide(
            MindMapNode node,
            LayoutSnapshot snapshot,
            double rootCenterX,
            boolean shouldBeLeft
    ) {
        double nodeCenterX = snapshot.getPlacement(node.getId()).bounds().getCenterX();
        if (shouldBeLeft) {
            assertTrue(nodeCenterX < rootCenterX, "Expected node on left side: " + node.getText());
        } else {
            assertTrue(nodeCenterX > rootCenterX, "Expected node on right side: " + node.getText());
        }

        for (MindMapNode child : node.getChildren()) {
            assertNodeSide(child, snapshot, rootCenterX, shouldBeLeft);
        }
    }
}

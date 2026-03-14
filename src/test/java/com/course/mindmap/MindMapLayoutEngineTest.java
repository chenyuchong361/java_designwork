package com.course.mindmap;

import com.course.mindmap.layout.LayoutSnapshot;
import com.course.mindmap.layout.MindMapLayoutEngine;
import com.course.mindmap.model.LayoutMode;
import com.course.mindmap.model.MindMapDocument;
import com.course.mindmap.model.MindMapNode;
import java.awt.Dimension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MindMapLayoutEngineTest {
    @Test
    void autoLayoutShouldPlaceChildrenOnBothSides() {
        MindMapDocument document = MindMapDocument.createBlank();
        document.setLayoutMode(LayoutMode.AUTO);

        MindMapNode root = document.getRoot();
        root.addChild("需求分析");
        root.addChild("界面设计");
        root.addChild("文件存储");
        root.addChild("图像导出");

        MindMapLayoutEngine engine = new MindMapLayoutEngine();
        LayoutSnapshot snapshot = engine.layout(document, node -> new Dimension(120, 40));

        assertEquals(5, snapshot.getPlacements().size());
        double rootCenterX = snapshot.getPlacement(root.getId()).bounds().getCenterX();

        long leftCount = root.getChildren().stream()
                .filter(child -> snapshot.getPlacement(child.getId()).bounds().getCenterX() < rootCenterX)
                .count();
        long rightCount = root.getChildren().stream()
                .filter(child -> snapshot.getPlacement(child.getId()).bounds().getCenterX() > rootCenterX)
                .count();

        assertTrue(leftCount > 0);
        assertTrue(rightCount > 0);
    }
}


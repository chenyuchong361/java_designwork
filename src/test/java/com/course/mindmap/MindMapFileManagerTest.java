package com.course.mindmap;

import com.course.mindmap.io.MindMapFileManager;
import com.course.mindmap.model.LayoutMode;
import com.course.mindmap.model.MindMapDocument;
import com.course.mindmap.model.MindMapNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MindMapFileManagerTest {
    @Test
    void saveAndLoadShouldPreserveStructure() throws Exception {
        MindMapDocument document = MindMapDocument.createBlank();
        document.getRoot().setText("Java课程设计");
        document.setTitle("Java课程设计");
        document.setLayoutMode(LayoutMode.LEFT);

        MindMapNode ui = document.getRoot().addChild("Swing");
        ui.addChild("工具栏");
        ui.addChild("绘图区");
        document.getRoot().addChild("文件导出");

        Path tempFile = Files.createTempFile("mind-map-", ".dt");
        try {
            MindMapFileManager fileManager = new MindMapFileManager();
            fileManager.save(document, tempFile.toFile());

            MindMapDocument loaded = fileManager.load(tempFile.toFile());
            assertEquals("Java课程设计", loaded.getTitle());
            assertEquals(LayoutMode.LEFT, loaded.getLayoutMode());
            assertEquals(5, loaded.getRoot().countNodes());
            assertEquals(2, loaded.getRoot().getChildren().size());
            assertEquals("Swing", loaded.getRoot().getChildren().get(0).getText());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}


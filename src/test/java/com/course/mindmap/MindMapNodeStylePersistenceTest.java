/*
Script: MindMapNodeStylePersistenceTest.java
Purpose: Verify that custom node text, fill, and line colors survive save/load round trips.
Author: Codex
Created: 2026-04-27
Last Updated: 2026-04-27
Dependencies: JUnit 5, java.nio.file, com.course.mindmap.io, com.course.mindmap.model
Usage: Run with the Maven test phase.

Changelog:
- 2026-04-27 Codex: Initial creation.
*/
package com.course.mindmap;

import com.course.mindmap.io.MindMapFileManager;
import com.course.mindmap.model.MindMapDocument;
import com.course.mindmap.model.MindMapNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MindMapNodeStylePersistenceTest {
    @Test
    void saveAndLoadShouldPreserveNodeStyles() throws Exception {
        MindMapDocument document = MindMapDocument.createBlank();
        MindMapNode root = document.getRoot();
        root.setTextColorHex("#112233");
        root.setFillColorHex("#445566");
        root.setLineColorHex("#778899");

        MindMapNode child = root.addChild("Styled Child");
        child.setTextColorHex("#AA5500");
        child.setFillColorHex("#DDEEFF");
        child.setLineColorHex("#00AA88");

        Path tempFile = Files.createTempFile("mind-map-style-", ".dt");
        try {
            MindMapFileManager fileManager = new MindMapFileManager();
            fileManager.save(document, tempFile.toFile());

            MindMapDocument loaded = fileManager.load(tempFile.toFile());
            MindMapNode loadedRoot = loaded.getRoot();
            MindMapNode loadedChild = loadedRoot.getChildren().get(0);

            assertEquals("#112233", loadedRoot.getTextColorHex());
            assertEquals("#445566", loadedRoot.getFillColorHex());
            assertEquals("#778899", loadedRoot.getLineColorHex());
            assertEquals("#AA5500", loadedChild.getTextColorHex());
            assertEquals("#DDEEFF", loadedChild.getFillColorHex());
            assertEquals("#00AA88", loadedChild.getLineColorHex());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}

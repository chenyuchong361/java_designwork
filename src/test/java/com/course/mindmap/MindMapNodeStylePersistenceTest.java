/*
Script: MindMapNodeStylePersistenceTest.java
Purpose: Verify that node fill, border, text, and branch styles survive save/load round trips.
Author: 陈宗波
Created: 2026-04-27
Last Updated: 2026-04-28
Dependencies: JUnit 5, java.nio.file, com.course.mindmap.io, com.course.mindmap.model
Usage: Run with the Maven test phase.

Changelog:
- 2026-04-27 陈宗波: Initial creation.
- 2026-04-28 陈宗波: Expanded the test to cover border, text, and branch style persistence. Reason: validate the new node property panel data model. Impact: backward compatible.
*/
package com.course.mindmap;

import com.course.mindmap.io.MindMapFileManager;
import com.course.mindmap.model.MindMapDocument;
import com.course.mindmap.model.MindMapNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MindMapNodeStylePersistenceTest {
    @Test
    void saveAndLoadShouldPreserveNodeStyles() throws Exception {
        MindMapDocument document = MindMapDocument.createBlank();
        MindMapNode root = document.getRoot();
        root.setFillColorHex("#445566");
        root.setBorderColorHex("#778899");
        root.setTextColorHex("#112233");
        root.setFontSize(18);
        root.setBold(true);

        MindMapNode child = root.addChild("Styled Child");
        child.setFillTransparent(true);
        child.setBorderColorHex("#AA5500");
        child.setTextColorHex("#2255AA");
        child.setFontSize(20);
        child.setBold(true);
        child.setBranchColorHex("#00AA88");

        Path tempFile = Files.createTempFile("mind-map-style-", ".dt");
        try {
            MindMapFileManager fileManager = new MindMapFileManager();
            fileManager.save(document, tempFile.toFile());

            MindMapDocument loaded = fileManager.load(tempFile.toFile());
            MindMapNode loadedRoot = loaded.getRoot();
            MindMapNode loadedChild = loadedRoot.getChildren().get(0);

            assertEquals("#445566", loadedRoot.getFillColorHex());
            assertEquals("#778899", loadedRoot.getBorderColorHex());
            assertEquals("#112233", loadedRoot.getTextColorHex());
            assertEquals(18, loadedRoot.getFontSize());
            assertTrue(loadedRoot.isBold());

            assertTrue(loadedChild.isFillTransparent());
            assertNull(loadedChild.getFillColorHex());
            assertEquals("#AA5500", loadedChild.getBorderColorHex());
            assertEquals("#2255AA", loadedChild.getTextColorHex());
            assertEquals(20, loadedChild.getFontSize());
            assertTrue(loadedChild.isBold());
            assertEquals("#00AA88", loadedChild.getBranchColorHex());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}

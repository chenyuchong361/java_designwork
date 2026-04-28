/*
Script: MindMapNodeStylePersistenceTest.java
Purpose: Verify that custom fill color and no-fill node states survive save/load round trips.
Author: Codex
Created: 2026-04-27
Last Updated: 2026-04-28
Dependencies: JUnit 5, java.nio.file, com.course.mindmap.io, com.course.mindmap.model
Usage: Run with the Maven test phase.

Changelog:
- 2026-04-27 Codex: Initial creation.
- 2026-04-28 Codex: Updated the test to cover fill-only styling and border-only nodes. Reason: match the reduced styling scope requested by the user. Impact: backward compatible.
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
    void saveAndLoadShouldPreserveNodeFillStyles() throws Exception {
        MindMapDocument document = MindMapDocument.createBlank();
        MindMapNode root = document.getRoot();
        root.setFillColorHex("#445566");

        MindMapNode child = root.addChild("Border Only Child");
        child.setFillTransparent(true);

        Path tempFile = Files.createTempFile("mind-map-style-", ".dt");
        try {
            MindMapFileManager fileManager = new MindMapFileManager();
            fileManager.save(document, tempFile.toFile());

            MindMapDocument loaded = fileManager.load(tempFile.toFile());
            MindMapNode loadedRoot = loaded.getRoot();
            MindMapNode loadedChild = loadedRoot.getChildren().get(0);

            assertEquals("#445566", loadedRoot.getFillColorHex());
            assertEquals(false, loadedRoot.isFillTransparent());
            assertTrue(loadedChild.isFillTransparent());
            assertNull(loadedChild.getFillColorHex());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}

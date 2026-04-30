/*
Script: MindMapCanvasExportTest.java
Purpose: Verify that mind map image export creates valid files and omits transient selection highlighting.
Author: chenyuchong
Created: 2026-04-30
Last Updated: 2026-04-30
Dependencies: JUnit 5, java.nio.file, javax.imageio, com.course.mindmap.model, com.course.mindmap.ui
Usage: Run with the Maven test phase.

Changelog:
- 2026-04-30 chenyuchong: Initial creation.
*/
package com.course.mindmap;

import com.course.mindmap.model.MindMapDocument;
import com.course.mindmap.ui.MindMapCanvas;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MindMapCanvasExportTest {
    @Test
    void pngExportShouldIgnoreCurrentSelection() throws Exception {
        MindMapDocument document = MindMapDocument.createBlank();
        document.getRoot().addChild("布局算法");
        document.getRoot().addChild("图像导出");

        MindMapCanvas canvas = new MindMapCanvas();
        canvas.setDocument(document);

        Path selectedExport = Files.createTempFile("mind-map-selected-", ".png");
        Path plainExport = Files.createTempFile("mind-map-plain-", ".png");
        try {
            canvas.setSelectedNode(document.getRoot(), false);
            canvas.exportToImage(selectedExport.toFile(), "png");

            canvas.setSelectedNode(null, false);
            canvas.exportToImage(plainExport.toFile(), "png");

            BufferedImage selectedImage = ImageIO.read(selectedExport.toFile());
            BufferedImage plainImage = ImageIO.read(plainExport.toFile());

            assertNotNull(selectedImage);
            assertNotNull(plainImage);
            assertEquals(plainImage.getWidth(), selectedImage.getWidth());
            assertEquals(plainImage.getHeight(), selectedImage.getHeight());
            assertImagesEqual(plainImage, selectedImage);
        } finally {
            Files.deleteIfExists(selectedExport);
            Files.deleteIfExists(plainExport);
        }
    }

    @Test
    void jpgExportShouldCreateReadableImage() throws Exception {
        MindMapDocument document = MindMapDocument.createBlank();
        document.getRoot().addChild("PNG");
        document.getRoot().addChild("JPG");

        MindMapCanvas canvas = new MindMapCanvas();
        canvas.setDocument(document);

        Path exportFile = Files.createTempFile("mind-map-export-", ".jpg");
        try {
            canvas.exportToImage(exportFile.toFile(), "jpg");

            BufferedImage image = ImageIO.read(exportFile.toFile());
            assertTrue(Files.size(exportFile) > 0);
            assertNotNull(image);
            assertTrue(image.getWidth() > 0);
            assertTrue(image.getHeight() > 0);
        } finally {
            Files.deleteIfExists(exportFile);
        }
    }

    private void assertImagesEqual(BufferedImage expected, BufferedImage actual) {
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertEquals(expected.getRGB(x, y), actual.getRGB(x, y), "Pixel mismatch at (" + x + "," + y + ")");
            }
        }
    }
}

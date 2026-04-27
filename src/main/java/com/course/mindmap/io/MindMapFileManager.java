/*
Script: MindMapFileManager.java
Purpose: Save and load mind map documents, including per-node visual style settings.
Author: chenyuchong
Created: 2026-03-14
Last Updated: 2026-04-27
Dependencies: Java XML APIs, com.course.mindmap.model
Usage: Called by MainFrame when persisting or opening .dt mind map files.

Changelog:
- 2026-03-14 chenyuchong: Initial creation.
- 2026-04-27 Codex: Added persistence for node text, fill, and line colors. Original author: chenyuchong. Reason: preserve user-selected node styles across save and load operations. Impact: backward compatible.
*/
package com.course.mindmap.io;

import com.course.mindmap.model.LayoutMode;
import com.course.mindmap.model.MindMapDocument;
import com.course.mindmap.model.MindMapNode;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MindMapFileManager {
    public void save(MindMapDocument document, File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document xmlDocument = builder.newDocument();

        Element rootElement = xmlDocument.createElement("mind-map");
        rootElement.setAttribute("title", document.getTitle());
        rootElement.setAttribute("layout", document.getLayoutMode().name());
        xmlDocument.appendChild(rootElement);
        rootElement.appendChild(writeNode(xmlDocument, document.getRoot()));

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(xmlDocument), new StreamResult(file));
    }

    public MindMapDocument load(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document xmlDocument = builder.parse(file);
        xmlDocument.getDocumentElement().normalize();

        Element rootElement = xmlDocument.getDocumentElement();
        if (!"mind-map".equals(rootElement.getNodeName())) {
            throw new ParserConfigurationException("Invalid mind map file.");
        }

        Element rootNodeElement = firstNodeElement(rootElement);
        if (rootNodeElement == null) {
            throw new ParserConfigurationException("Mind map file does not contain any node.");
        }

        MindMapNode rootNode = readNode(rootNodeElement);
        String title = rootElement.getAttribute("title");
        LayoutMode layoutMode = LayoutMode.fromPersistentName(rootElement.getAttribute("layout"));
        return new MindMapDocument(title, rootNode, layoutMode);
    }

    private Element writeNode(org.w3c.dom.Document xmlDocument, MindMapNode node) {
        Element element = xmlDocument.createElement("node");
        element.setAttribute("id", node.getId());
        element.setAttribute("text", node.getText());
        writeOptionalAttribute(element, "text-color", node.getTextColorHex());
        writeOptionalAttribute(element, "fill-color", node.getFillColorHex());
        writeOptionalAttribute(element, "line-color", node.getLineColorHex());

        for (MindMapNode child : node.getChildren()) {
            element.appendChild(writeNode(xmlDocument, child));
        }
        return element;
    }

    private MindMapNode readNode(Element element) {
        MindMapNode node = new MindMapNode(element.getAttribute("id"), element.getAttribute("text"));
        node.setTextColorHex(readOptionalAttribute(element, "text-color"));
        node.setFillColorHex(readOptionalAttribute(element, "fill-color"));
        node.setLineColorHex(readOptionalAttribute(element, "line-color"));

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE && "node".equals(childNode.getNodeName())) {
                node.addChild(readNode((Element) childNode));
            }
        }
        return node;
    }

    private Element firstNodeElement(Element parent) {
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE && "node".equals(childNode.getNodeName())) {
                return (Element) childNode;
            }
        }
        return null;
    }

    private void writeOptionalAttribute(Element element, String name, String value) {
        if (value != null && !value.isBlank()) {
            element.setAttribute(name, value);
        }
    }

    private String readOptionalAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        return value == null || value.isBlank() ? null : value;
    }
}

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
        javax.xml.parsers.DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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
        for (MindMapNode child : node.getChildren()) {
            element.appendChild(writeNode(xmlDocument, child));
        }
        return element;
    }

    private MindMapNode readNode(Element element) {
        MindMapNode node = new MindMapNode(element.getAttribute("id"), element.getAttribute("text"));
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
}

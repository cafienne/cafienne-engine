/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Helper class for various XML functions.
 */
public class XMLHelper {
    private final static Logger logger = LoggerFactory.getLogger(XMLHelper.class);

    public static String printXMLNode(Node node) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute("indent-number", 2);
            Transformer transformer = tf.newTransformer();
            String omitDeclaration = node instanceof Document || node == node.getOwnerDocument().getDocumentElement() ? "no" : "yes";
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitDeclaration);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            String output = writer.getBuffer().toString();
            output = removeEmptyLines(output);// .replaceAll("\n|\r", "");
            return output;
        } catch (TransformerException te) {
            throw new RuntimeException(te);
        }
    }

    public static void persist(Node node, File file) throws IOException, TransformerException {
        String xmlString = printXMLNode(node);
        file.getParentFile().mkdirs(); // make sure to create the parent directory if required
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(xmlString.getBytes());
        fos.flush();
        fos.close();
    }

    private static String removeEmptyLines(String text) {
        StringBuffer buffer = new StringBuffer();
        String[] lines = text.split("\n");
        for (String string : lines) {
            if (string.trim().isEmpty()) {
                continue;
            }
            // Dunno why this is needed, but sometimes we see this strange character appearing. Perhaps we need to revisit the settings of the Transformer above
            if (string.contains("&#13;")) {
                string = string.replace("&#13;", "");
            }
            buffer.append(string + "\n");
        }
        return buffer.toString();
    }

    /**
     * Parses the input stream into a namespace aware XMLDocument
     *
     * @param inputStream
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static Document getXMLDocument(InputStream inputStream) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputStream);

        // optional, but recommended
        // read this -
        // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
        doc.getDocumentElement().normalize();

        return doc;
    }

    /**
     * Parses a string into an XMLDocument. Internally invokes the {@link XMLHelper#loadXML(byte[])} method, and converts the string to bytes
     * by invoking String.getBytes().
     *
     * @param xmlString
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static Document loadXML(String xmlString) throws IOException, ParserConfigurationException, SAXException {
        return loadXML(xmlString.getBytes());
    }

    /**
     * Parses a byte[] into an XMLDocument; internally invokes {@link XMLHelper#getXMLDocument(InputStream)} method.
     *
     * @param xmlBytes the bytes to parse.
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static Document loadXML(byte[] xmlBytes) throws ParserConfigurationException, SAXException, IOException {
        InputStream inputStream = new ByteArrayInputStream(xmlBytes);
        return getXMLDocument(inputStream);
    }

    /**
     * Returns all elements under the element with the specified tagname.
     *
     * @param element
     * @param tagName
     * @return
     */
    public static List<Element> getChildrenWithTagName(Element element, String tagName) {
        NodeList nList = element.getElementsByTagName(tagName);
        List<Element> list = new ArrayList<Element>();
        int listLength = nList.getLength();
        for (int i = 0; i < listLength; i++) {
            Element listElement = (Element) nList.item(i);
            if (listElement.getParentNode() == element) {
                list.add(listElement);
            }
        }
        return list;
    }

    /**
     * Get a child element with the specified localName. Assumption is that it is in the same namespace as the specified element.
     *
     * @param element
     * @param localName
     * @return
     */
    public static Element getElement(Element element, String localName) {
        Collection<Element> elements = getChildrenWithTagName(element, localName);
        if (elements.isEmpty()) {
            return null;
        } else {
            return elements.iterator().next();
        }
    }

    /**
     * Get a child element with the specified localName. Assumption is that it is in the same namespace as the specified element.
     *
     * @param element Starting point from where to start the search
     * @param elementName The name of the element to find
     * @param elementNS The namespace in which to search for the element
     * @return
     */
    public static Element getElementNS(Element element, String elementNS, String elementName) {
        NodeList nList = element.getElementsByTagNameNS(elementNS, elementName);
        int listLength = nList.getLength();
        for (int i = 0; i < listLength; i++) {
            Element listElement = (Element) nList.item(i);
            if (listElement.getParentNode() == element) {
                return listElement;
            }
        }

        return null;
    }

    /**
     * Searches for the element with the specified name in the whole subtree of element.
     *
     * @param element
     * @param localName
     * @return
     */
    public static Element findElement(Element element, String localName) {
        NodeList nList = element.getElementsByTagName(localName);
        if (nList.getLength() == 0) {
            return null;
        }
        return (Element) nList.item(0);
    }

    /**
     * Returns the text content of the child element with the specified tag name, and the default value if no such element exists or the element does not have any text content.
     * If the tag name is not specified, the text content of the element itself will be taken.
     *
     * @param element
     * @param localName If localName is null, then the text content of the element itself will be taken.
     * @return
     */
    public static String getContent(Element element, String localName, String defaultValue) {
        Element child = localName == null ? element : getElement(element, localName);
        if (child != null) {
            String value = child.getTextContent();
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }
}

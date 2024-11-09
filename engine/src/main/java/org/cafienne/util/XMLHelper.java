/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.util;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Helper class for various XML functions.
 */
public class XMLHelper {
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
        StringBuilder buffer = new StringBuilder();
        String[] lines = text.split("\n");
        for (String string : lines) {
            if (string.trim().isEmpty()) {
                continue;
            }
            // Don't know why this is needed, but sometimes we see this strange character appearing. Perhaps we need to revisit the settings of the Transformer above
            if (string.contains("&#13;")) {
                string = string.replace("&#13;", "");
            }
            buffer.append(string).append("\n");
        }
        return buffer.toString();
    }

    /**
     * Parses the input stream into a namespace aware XMLDocument
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
     */
    public static Document loadXML(String xmlString) throws IOException, ParserConfigurationException, SAXException {
        return loadXML(xmlString.getBytes());
    }

    /**
     * Parses a byte[] into an XMLDocument; internally invokes {@link XMLHelper#getXMLDocument(InputStream)} method.
     *
     * @param xmlBytes the bytes to parse.
     */
    public static Document loadXML(byte[] xmlBytes) throws ParserConfigurationException, SAXException, IOException {
        InputStream inputStream = new ByteArrayInputStream(xmlBytes);
        return getXMLDocument(inputStream);
    }

    /**
     * Returns all elements under the element with the specified tagName.
     */
    public static List<Element> getChildrenWithTagName(Element element, String tagName) {
        NodeList nList = element.getElementsByTagName(tagName);
        List<Element> list = new ArrayList<>();
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
     * Returns all direct child elements under the element with the specified tag and namespace.
     */
    public static List<Element> getElementsNS(Element element, String elementNS, String localName) {
        NodeList nList = element.getElementsByTagNameNS(elementNS, localName);
        List<Element> list = new ArrayList<>();
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
     * @param element   Starting point from where to start the search
     * @param localName The name of the element to find
     * @param elementNS The namespace in which to search for the element
     * @return The first element found, or null.
     */
    public static Element getElementNS(Element element, String elementNS, String localName) {
        NodeList nList = element.getElementsByTagNameNS(elementNS, localName);
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
     */
    public static Element findElement(Element element, String localName) {
        NodeList nList = element.getElementsByTagName(localName);
        if (nList.getLength() == 0) {
            return null;
        }
        return (Element) nList.item(0);
    }

    /**
     * Returns the text content of the child element with the specified tagName, and the default value if no such element exists or the element does not have any text content.
     * If the tagName is not specified, the text content of the element itself will be taken.
     *
     * @param localName If localName is null, then the text content of the element itself will be taken.
     */
    public static String getContent(Element element, String localName, String defaultValue) {
        Element child = localName == null ? element : getElement(element, localName);
        if (child != null) {
            String value = getTextOfChildren(child);
            if (!value.isBlank()) {
                return value;
            }
        }
        return defaultValue;
    }

    /**
     * Recursive alternative to Element.getTextContent();
     * Ignores whitespace around CDATASections
     * <p>
     * 1. Append CDATASections in their entirety.
     * 2. Append TextNodes only if non-blank.
     * 3. Recurse into Element type of children
     */
    private static String getTextOfChildren(Element element) {
        StringBuilder sb = new StringBuilder();
        NodeList children = element.getChildNodes();
        int length = children.getLength();
        for (int i = 0; i < length; i++) {
            Node node = children.item(i);
            if (node instanceof CDATASection) {
                sb.append(node.getNodeValue());
            } else if (node instanceof Text) {
                String value = node.getNodeValue();
                if (!value.isBlank()) sb.append(value);
            } else if (node instanceof Element) {
                sb.append(getTextOfChildren((Element) node));
            }
        }
        return sb.toString();
    }
}

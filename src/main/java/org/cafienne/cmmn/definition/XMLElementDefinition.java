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

package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinitionDefinition;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for parsing XML elements defined in the CMMN specification.
 */
public abstract class XMLElementDefinition implements DefinitionElement {
    private static final Logger logger = LoggerFactory.getLogger(XMLElementDefinition.class);

    protected final static String NAMESPACE_URI = "org.cafienne";
    private final String id;
    private String name;

    private final ModelDefinition modelDefinition;
    private final XMLElementDefinition parentElement;
    private final Element element;

    private static final String EXTENSIONELEMENTS = "extensionElements";

    protected XMLElementDefinition(Element element, ModelDefinition modelDefinition, XMLElementDefinition parentElement, boolean... identifierRequired) {
        this.element = element;
        this.modelDefinition = modelDefinition;
        this.parentElement = parentElement;
        this.id = parseAttribute("id", false);
        this.name = parseAttribute("name", false);
        if (identifierRequired.length > 0 && identifierRequired[0] == true) {
            if (this.getName().isEmpty() && this.getId().isEmpty()) {
                getModelDefinition().addDefinitionError("An element of type '" + printElement() + "' does not have an identifier " + XMLHelper.printXMLNode(element));
            }
        }
    }

    /**
     * Returns the name of the element. Can be used in combination with the id of the element to resolve an XSD IDREF to this element.
     *
     * @return
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Subclasses are allowed to give a different name than what is specified in the element itself.
     *
     * @param name
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the identifier of the element. Can be used in combination with the name of the element to resolve an XSD IDREF to this element.
     *
     * @return
     */
    public String getId() {
        if (this.id == null || this.id.isEmpty()) {
            return this.name;
        } else {
            return this.id;
        }
    }

    /**
     * Returns a description of the context this element provides to it's children. Can be used e.g. in expressions or on parts
     * to get the description of the parent element when encountering validation errors.
     *
     * @return
     */
    public String getContextDescription() {
        return "";
    }

    /**
     * Returns the top level element within the &lt;definitions&gt; to which this element belongs. This is typically
     * the {@link CaseDefinition}, {@link ProcessDefinition} or {@link CaseFileItemDefinitionDefinition}.
     *
     * @return
     */
    public ModelDefinition getModelDefinition() {
        return modelDefinition;
    }

    public CaseDefinition getCaseDefinition() {
        return (CaseDefinition) getModelDefinition();
    }

    public ProcessDefinition getProcessDefinition() {
        return (ProcessDefinition) getModelDefinition();
    }

    /**
     * Casts the parent element to the expected type for convenience.
     *
     * @return
     */
    public <T extends CMMNElementDefinition> T getParentElement() {
        @SuppressWarnings("unchecked")
        T typedParent = (T) parentElement;
        return typedParent;
    }

    public Element getElement() {
        return element;
    }

    public String getType() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Definition")) {
            simpleName = simpleName.substring(0, simpleName.length() - "Definition".length());
        }
        return simpleName;
    }

    /**
     * CMMN.xsd compliant XML documents are parsed in 2 phases.
     * In the first phase, for every element defined in the XSD, a class extending CMMNElementDefinition is instantiated.
     * In the second phase, when all elements have been made available in the definition, they can start referring to
     * each other. This logic can be implemented in the resolveReferences method.
     * Subclasses implementing this method are encouraged not to forget to invoke <code>super.resolveReferences()</code>, especially if they are subclassing
     * a subclass from {@link XMLElementDefinition}.
     */
    protected void resolveReferences() {
    }

    /**
     * New hook invoked after resolveReferences that can be used ot do logical validation of the element.
     * Is only invoked when resolveReferences does not reveal errors.
     */
    protected void validateElement() {
    }

    /**
     * Creates a new instance of class T based on the XML element. If T is of type {@link String}, then it will simply return the text content of the
     * XML element. Otherwise T is expected to have a constructor with {@link Element}, {@link ModelDefinition} and {@link XMLElementDefinition}.
     *
     * @param xmlElement
     * @param typeClass
     * @return
     */
    private <T> T instantiateT(Element xmlElement, Class<T> typeClass) {
        try {
            if (typeClass.equals(String.class)) {
                @SuppressWarnings("unchecked")
                // we just checked it...
                T t = (T) xmlElement.getTextContent();
                return t;
            }

            Constructor<T> tConstructor = typeClass.getConstructor(Element.class, ModelDefinition.class, CMMNElementDefinition.class);
            T childDefinition = tConstructor.newInstance(xmlElement, getModelDefinition(), this);
            return childDefinition;
        } catch (InstantiationException | InvocationTargetException | IllegalArgumentException e) {
            String msg = "The class " + typeClass.getName() + " cannot be instantiated";
            getModelDefinition().fatalError(msg, e);
        } catch (IllegalAccessException e) {
            String msg = "The class " + typeClass.getName() + " cannot be accessed";
            getModelDefinition().fatalError(msg, e);
        } catch (NoSuchMethodException e) {
            String msg = "The class " + typeClass.getName() + " must have a constructor with 3 arguments: org.w3c.dom.Element, Definition and  CMMNElementDefinition";
            getModelDefinition().fatalError(msg, e);
        } catch (SecurityException e) {
            String msg = "The class " + typeClass.getName() + " cannot be accessed due to a security exception";
            getModelDefinition().fatalError(msg, e);
        }
        return null;
    }

    /**
     * Simple helper function that parses all children of the element that have the specified tag name into an instance of the specified generic T. Additionally adds them to the Collection. In case the
     * generic T is String.class, it will simply add the text content of the specified tag to the list
     *
     * @param childTagName
     * @param typeClass
     * @param tCollection
     * @return
     */
    protected <T> void parse(String childTagName, Class<? extends T> typeClass, Collection<T> tCollection) {
        Collection<Element> namedChildren = XMLHelper.getChildrenWithTagName(element, childTagName);
        for (Element child : namedChildren) {
            T t = instantiateT(child, typeClass);
            tCollection.add(t);
        }
    }

    /**
     * Parses all elements within the extension element that have the specified name, and adds them to the collection.
     *
     * @param childTagName
     * @param typeClass
     * @param tCollection
     */
    protected <T> void parseExtension(String childTagName, Class<? extends T> typeClass, Collection<T> tCollection) {
        parseGrandChildren(EXTENSIONELEMENTS, childTagName, typeClass, tCollection);
    }

    /**
     * Parses the first child element within the extension element that has the specified tagname, and tries to instantiate it into the typeClass.
     *
     * @param childTagName
     * @param typeClass
     * @return
     */
    protected <T> T parseExtension(String childTagName, Class<? extends T> typeClass) {
        return parseGrandChild(EXTENSIONELEMENTS, childTagName, typeClass);
    }

    /**
     * Simple helper function that searches for the child element with the specified tagname, and then finds the first
     * grand child inside that element, and instantiates it into the specified type class
     *
     * @param childTagName
     * @param grandChildName
     * @param typeClass
     */
    private <T> T parseGrandChild(String childTagName, String grandChildName, Class<? extends T> typeClass) {
        Element child = XMLHelper.findElement(element, childTagName);
        if (child != null) {
            Element grandChild = XMLHelper.findElement(element, grandChildName);
            if (grandChild != null) {
                T t = instantiateT(grandChild, typeClass);
                return t;
            }
        }
        return null;
    }

    /**
     * Simple helper function that searches for the child element with the specified tagname, and then parses it's children into the specified type class
     * and adds them to the collection
     *
     * @param childTagName
     * @param grandChildName
     * @param typeClass
     * @param tCollection
     */
    protected <T> void parseGrandChildren(String childTagName, String grandChildName, Class<? extends T> typeClass, Collection<T> tCollection) {
        Element child = XMLHelper.findElement(element, childTagName);
        if (child != null) {
            Collection<Element> namedGrandChildren = XMLHelper.getChildrenWithTagName(child, grandChildName);
            for (Element grandChild : namedGrandChildren) {
                T t = instantiateT(grandChild, typeClass);
                tCollection.add(t);
            }
        }
    }

    /**
     * Simple helper function that parses all children of the element that have the specified tag name into an instance of the specified generic T. Additionally puts them in the Map, based on the
     * &lt;name&gt; element inside the child. E.g., Multiple Task definitions within the case plan, each having their own name, will be put in the Dictionary for easy lookup later.
     *
     * @param childTagName
     * @param typeClass
     * @param tMap
     * @return
     */
    protected <T extends XMLElementDefinition> void parse(String childTagName, Class<? extends T> typeClass, Map<String, T> tMap) {
        Collection<Element> namedChildren = XMLHelper.getChildrenWithTagName(element, childTagName);
        for (Element child : namedChildren) {
            T t = instantiateT(child, typeClass);
            if (t.getName().isEmpty()) {
                getModelDefinition().addDefinitionError("The element does not have a name, but it is required in order to be able to look it up\n" + XMLHelper.printXMLNode(child));
            }
            tMap.put(t.getName(), t);
        }
    }

    /**
     * Simple helper function that parses the first child of element that has the specified tag name into an instance of the specified generic T. Throws an exception if the tag is required, but cannot
     * be found. In case the generic T is String.class, it will simply return the text content of the specified tag.
     *
     * @param childTagName
     * @param typeClass
     * @param presenceRequired
     * @return
     */
    protected <T> T parse(String childTagName, Class<? extends T> typeClass, boolean presenceRequired) {
        if (element == null) {
            if (presenceRequired) {
                getModelDefinition().addDefinitionError("A '" + childTagName + "' cannot be found in the class " + getClass().getName() + ", because there is no XML element defined");
            }
            return null;
        }
        Element child = XMLHelper.getElement(element, childTagName);
        if (child != null) {
            return instantiateT(child, typeClass);
        } else if (presenceRequired) {
            getModelDefinition().addDefinitionError("A '" + childTagName + "' cannot be found in the element " + printElement() + ", but it is required");
        }
        return null;
    }

    protected String parseString(String childTagName, boolean presenceRequired, String... defaultValue) {
        String value = XMLHelper.getContent(element, childTagName, null);
        if (presenceRequired && value == null) {
            getModelDefinition().addDefinitionError("A '" + childTagName + "' cannot be found in the element " + printElement() + ", but it is required");
            return null;
        }

        // If value is null, we may set it to the (optional) default value
        if (value == null && defaultValue.length > 0) {
            value = defaultValue[0];
        }

        return value;
    }

    protected String printElement() {
        String xml = XMLHelper.printXMLNode(element.cloneNode(false));
        xml = xml.replace("\n", "");
        xml = xml.replace("\r", "");
        return xml;
    }

    /**
     * Searches for the extension with the specified name, and parse the extension definition tag <code>&lt;extensionDefinition&gt;<code>.
     *
     * @param elementName
     * @param typeClass
     * @param presenceRequired
     * @return
     */
    public <T> T getExtension(String elementName, Class<? extends T> typeClass, boolean presenceRequired) {
        Element implementationElement = getExtension(elementName, presenceRequired);
        if (implementationElement == null) {
            return null;
        }

        String implementationClassName = implementationElement.getAttribute("class");
        if (implementationClassName.isEmpty()) {
            if (presenceRequired) {
                getModelDefinition().addDefinitionError("A custom " + elementName + " tag does not contain the class attribute in " + printElement() + ", but it is required");
                return null;
            }
            return null;
        }
        try {
            Class<?> implementationClass = Class.forName(implementationClassName);
            if (!typeClass.isAssignableFrom(implementationClass)) {
                throw new RuntimeException("The implementation class " + implementationClassName + " must implement " + typeClass.getName() + ", but it does not");
            }
            @SuppressWarnings("unchecked") // Well, we just did, right?
            T implementationObject = (T) instantiateT(implementationElement, implementationClass);
            return implementationObject;
        } catch (ClassNotFoundException e) {
            String msg = "Cannot find class to parse the custom " + elementName + " - " + implementationClassName;
            getModelDefinition().fatalError(msg, e);
        } catch (SecurityException e) {
            String msg = "The class " + implementationClassName + " cannot be accessed due to a security exception";
            getModelDefinition().fatalError(msg, e);
        }
        // Means we ran into an exception
        return null;
    }

    /**
     * Returns the content of an extension element that has the specified name and namespace.
     * If namespaceURI is null, then the full tagname will be searched for.
     *
     * @param elementName
     * @param presenceRequired If presence is required, and the extensionElements tag is not available or the
     *                         specific tag is not found under it, then an error will be added to the definitions document.
     * @return
     */
    public Element getExtension(String elementName, boolean presenceRequired) {
        Element extensionsElement = XMLHelper.getElement(element, EXTENSIONELEMENTS);

        if (extensionsElement == null) {
            if (presenceRequired) {
                getModelDefinition().addDefinitionError("'" + EXTENSIONELEMENTS + "' tag is not found in " + printElement() + ", but it is required");
                return null;
            }
            return null;
        }

        Element implementationElement = null;
        if (elementName != null && !elementName.isEmpty()) {
            implementationElement = XMLHelper.getElementNS(extensionsElement, NAMESPACE_URI, elementName);
        }

        if (implementationElement == null && presenceRequired) {
            getModelDefinition().addDefinitionError("A custom " + elementName + " tag is not found in " + printElement() + "/" + EXTENSIONELEMENTS + " in " + NAMESPACE_URI + " namespace, but it is required");
        }

        return implementationElement;
    }

    /**
     * Parses the attribute name into a string, or provides the default value if the attribute is not found.
     *
     * @param attributeName
     * @param presenceRequired
     * @param defaultValue
     * @return
     */
    protected String parseAttribute(String attributeName, boolean presenceRequired, String... defaultValue) {
        if (element != null && element.hasAttribute(attributeName)) {
            return element.getAttribute(attributeName);
        }

        // If the attribute is required, add an error message
        if (presenceRequired) {
            if (element == null) {
                getModelDefinition().addDefinitionError("The attribute " + attributeName + " cannot be found, because an XML element is missing in " + this.getType());
            } else {
                getModelDefinition().addDefinitionError("The attribute " + attributeName + " is missing from the element " + XMLHelper.printXMLNode(element));
            }
        }

        // Return the (optional) default value or an empty string
        if (defaultValue.length > 0) {
            return defaultValue[0];
        } else {
            return "";
        }
    }

    protected boolean sameClass(Object object) {
        return object != null && this.getClass().equals(object.getClass());
    }

    public boolean sameName(XMLElementDefinition other) {
        return same(getName(), other.getName());
    }

    public boolean sameId(XMLElementDefinition other) {
        return same(this.getId(), other.getId());
    }

    /**
     * Check whether name and id match on the other element.
     * @param other
     * @return
     */
    public boolean sameIdentifiers(XMLElementDefinition other) {
        return sameName(other)
                && sameId(other);
    }

    @Override
    public boolean differs(Object object) {
        return !equalsWith(object);
    }

    /**
     * Custom compare method. Comparable to Object.equals(), but elements are expected
     * to implement a semantic comparison.
     * @param object
     * @return
     */
    protected abstract boolean equalsWith(Object object);

    /**
     * This checks that other has Class[E] and invokes the matcher on it
     *
     * @param object
     * @param matcher
     * @param <E>
     * @return
     */
    protected <E extends XMLElementDefinition> boolean equalsWith(Object object, DefinitionComparer<E> matcher) {
//        System.out.println("Running EW WIHT MATCHER ON A " + this.getClass().getSimpleName() +" with name " + getName());
        if (!sameClass(object)) {
            return false;
        }

        // TODO: it will be good if we can create a comparison report (a tree) on the definition and log that.
        String currentIndent = indent;
        indent += "  ";
        boolean isEqual = matcher.match((E) object);
        indent = currentIndent;
//        System.out.println(indent + "Comparison result for " + this.getClass().getSimpleName() +"[" + getName() +"] is " + isEqual);
        return isEqual;
    }

    private static String indent = "";

    /**
     * Find an equal definition in the collection, using the equalsWith method.
     * @param mine The element that we want to find an alternative for
     * @param theirs The collection to search the element
     * @param <T> Target type to cast to
     * @param <Z> Base type to compare on, to help also search in generics based collections (e.g. Collection[OnPartDefinition])
     * @return Null if the element was not found in the collection
     */
    public static <T extends Z, Z extends XMLElementDefinition> T findDefinition(T mine, Collection<Z> theirs) {
        for (Z his : theirs) {
            if (mine.equalsWith(his)) {
                return (T) his; // Cast is ok, because it is checked inside the equalsWith method to be the same class.
            }
        }
        return null;
    }

    /**
     * Check equality of both collections
     * @param ours Source to compare
     * @param theirs Target to compare against
     * @param <X> Type to compare about
     * @return false if sizes differ or at leas one element differs.
     */
    protected <X extends XMLElementDefinition> boolean same(Collection<X> ours, Collection<X> theirs) {
        if (ours.size() != theirs.size()) {
            return false;
        }
        for (X mine : ours) { // Iterate all our elements, and check if there is one that does not match any of theirs. If so, return false.
            if (findDefinition(mine, theirs) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine whether both definition elements are the same.
     * Done through regular null check and then invoking the equalsWith method.
     */
    protected <X extends XMLElementDefinition> boolean same(X obj1, X obj2) {
        return obj1 == null && obj2 == null || obj1 != null && obj2 != null && obj1.equalsWith(obj2);
    }

    protected boolean same(String obj1, String obj2) {
        return Objects.equals(obj1, obj2);
    }

    protected <E extends Enum<?>> boolean same(E obj1, E obj2) {
        return Objects.equals(obj1, obj2);
    }

    protected boolean same(boolean obj1, boolean obj2) {
        return Objects.equals(obj1, obj2);
    }

    /**
     * Shortcut to Objects.equals()
     */
    protected boolean same(Object obj1, Object obj2) {
        return Objects.equals(obj1, obj2);
    }

    @FunctionalInterface
    protected interface DefinitionComparer<X extends XMLElementDefinition> {
        boolean match(X other);
    }

    protected boolean notYetImplemented() {
        logger.error("Definition comparison is not yet implemented on definition elements of type " + getClass().getName());
        return false;
    }
}

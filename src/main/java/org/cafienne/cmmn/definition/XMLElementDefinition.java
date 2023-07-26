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

    public static final String EXTENSION_ELEMENTS = "extensionElements";
    public static final String CAFIENNE_NAMESPACE = "org.cafienne";
    public static final String CAFIENNE_IMPLEMENTATION = "implementation";

    private final String id;
    private String name;

    private final ModelDefinition modelDefinition;
    private final XMLElementDefinition parentElement;
    private final Element element;


    protected XMLElementDefinition(Element element, ModelDefinition modelDefinition, XMLElementDefinition parentElement, boolean... identifierRequired) {
        this.element = element;
        this.modelDefinition = modelDefinition;
        this.parentElement = parentElement;
        this.id = parseAttribute("id", false);
        this.name = parseAttribute("name", false);
        if (identifierRequired.length > 0 && identifierRequired[0]) {
            if (this.getName().isEmpty() && this.getId().isEmpty()) {
                getModelDefinition().addDefinitionError("An element of type '" + printElement() + "' does not have an identifier " + XMLHelper.printXMLNode(element));
            }
        }
    }

    /**
     * Returns the name of the element. Can be used in combination with the id of the element to resolve an XSD IDREF to this element.
     *
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
     */
    public String getContextDescription() {
        return "";
    }

    /**
     * Returns the top level element within the &lt;definitions&gt; to which this element belongs. This is typically
     * the {@link CaseDefinition}, {@link ProcessDefinition} or {@link CaseFileItemDefinitionDefinition}.
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
            return simpleName.substring(0, simpleName.length() - "Definition".length());
        } else {
            return simpleName;
        }
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
     * XML element. Otherwise, T is expected to have a constructor with {@link Element}, {@link ModelDefinition} and {@link XMLElementDefinition}.
     *
     * @param xmlElement The element to use in the constructor of the typeClass to do parsing
     * @param typeClass  The class to instantiate when the element is found
     * @return The instance of the typeClass
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
            return tConstructor.newInstance(xmlElement, getModelDefinition(), this);
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
     * @param childTagName The name of the child element to search for
     * @param typeClass    The class to instantiate when the element is found
     * @param tCollection  The collection to add the instantiated object to
     */
    protected <T> void parse(String childTagName, Class<? extends T> typeClass, Collection<T> tCollection) {
        Collection<Element> namedChildren = XMLHelper.getChildrenWithTagName(element, childTagName);
        for (Element child : namedChildren) {
            T t = instantiateT(child, typeClass);
            tCollection.add(t);
        }
    }

    /**
     * Simple helper function that parses all children of the element that have the specified tag name into an instance of the specified generic T. Additionally puts them in the Map, based on the
     * &lt;name&gt; element inside the child. E.g., Multiple Task definitions within the case plan, each having their own name, will be put in the Dictionary for easy lookup later.
     *
     * @param childTagName The name of the child element to search for
     * @param typeClass    The class to instantiate when the element is found
     * @param tMap         The map to put the instantiated object in with it's name
     */
    protected <T extends XMLElementDefinition> void parse(String childTagName, Class<? extends T> typeClass, Map<String, T> tMap) {
        Collection<Element> namedChildren = XMLHelper.getChildrenWithTagName(element, childTagName);
        for (Element child : namedChildren) {
            T t = instantiateT(child, typeClass);
            if (t != null) {
                if (t.getName().isEmpty()) {
                    getModelDefinition().addDefinitionError("The element does not have a name, but it is required in order to be able to look it up\n" + XMLHelper.printXMLNode(child));
                }
                tMap.put(t.getName(), t);
            }
        }
    }

    /**
     * Simple helper function that parses the first child of element that has the specified tag name into an instance of the specified generic T. Throws an exception if the tag is required, but cannot
     * be found. In case the generic T is String.class, it will simply return the text content of the specified tag.
     *
     * @param childTagName     The name of the child element to search for
     * @param typeClass        The class to instantiate when the element is found
     * @param presenceRequired If true, and the child element is not found, a validation error is added to the model definition
     * @return The parsed instance of the typeClass
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

    /**
     * Parses the first child element within the extension element that has the specified tagname, and tries to instantiate it into the typeClass.
     *
     * @param childTagName The name of the child element to search for
     * @param typeClass    The class to instantiate when the element is found
     * @return The parsed instance of the typeClass
     */
    protected <T> T parseExtension(String childTagName, Class<? extends T> typeClass) {
        Element extensionElement = XMLHelper.getElement(element, EXTENSION_ELEMENTS);
        if (extensionElement != null) {
            Element grandChild = XMLHelper.getElementNS(extensionElement, CAFIENNE_NAMESPACE, childTagName);
            if (grandChild != null) {
                return instantiateT(grandChild, typeClass);
            }
        }
        return null;
    }

    /**
     * Simple helper function that searches for the child element with the specified tagname, and then parses it's children into the specified type class
     * and adds them to the collection
     *
     * @param childTagName   The name of the child element to search for
     * @param grandChildName The name of the grand child element to search for
     * @param typeClass      The class to instantiate when the element is found
     * @param tCollection    The collection to add the instantiated object to
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
     * Searches for the extensionsElement, and within that for a tag implementation element;
     * If this element is found, it looks up the value of the attribute named 'class', and then tries to instantiate that
     * class. The typeClass must be assignable from the instantiated class.
     *
     * @param typeClass        The class to instantiate when the element is found
     * @param presenceRequired If true, and the child element is not found, a validation error is added to the model definition
     * @return The parsed instance of the typeClass
     */
    protected <T> T getCustomImplementation(Class<? extends T> typeClass, boolean presenceRequired) {
        Element implementationElement = getImplementationElement(presenceRequired);
        if (implementationElement == null) {
            return null;
        }

        String implementationClassName = implementationElement.getAttribute("class");
        if (implementationClassName.isEmpty()) {
            if (presenceRequired) {
                getModelDefinition().addDefinitionError("A custom " + CAFIENNE_IMPLEMENTATION + " tag does not contain the class attribute in " + printElement() + ", but it is required");
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
            String msg = "Cannot find class to parse the custom " + CAFIENNE_IMPLEMENTATION + " - " + implementationClassName;
            getModelDefinition().fatalError(msg, e);
        } catch (SecurityException e) {
            String msg = "The class " + implementationClassName + " cannot be accessed due to a security exception";
            getModelDefinition().fatalError(msg, e);
        }
        // Means we ran into an exception
        return null;
    }

    /**
     * Returns the content of an extension element that has the name "implementation" in the cafienne namespace.
     *
     * @param presenceRequired If presence is required, and the extensionElements tag is not available or the
     *                         implementation element is not found under it, then an error will be added to the definitions document.
     */
    protected Element getImplementationElement(boolean presenceRequired) {
        Element extensionsElement = XMLHelper.getElement(element, EXTENSION_ELEMENTS);
        if (extensionsElement == null) {
            if (presenceRequired) {
                getModelDefinition().addDefinitionError("'" + EXTENSION_ELEMENTS + "' tag is not found in " + printElement() + ", but it is required");
                return null;
            }
            return null;
        }

        Element implementationElement = XMLHelper.getElementNS(extensionsElement, CAFIENNE_NAMESPACE, CAFIENNE_IMPLEMENTATION);
        if (implementationElement == null && presenceRequired) {
            getModelDefinition().addDefinitionError("A custom " + CAFIENNE_IMPLEMENTATION + " tag is not found in " + printElement() + "/" + EXTENSION_ELEMENTS + " in " + CAFIENNE_NAMESPACE + " namespace, but it is required");
        }

        return implementationElement;
    }

    /**
     * Parses a custom attribute on the implementation element and returns true if its value equals to "true" (case insensitive).
     * Otherwise, i.e. when the implementation element is not present or the attribute has a different value than true, return false.
     */
    protected boolean getImplementationAttribute(String attributeName) {
        Element cafienneImplementation = getImplementationElement(false);
        if (cafienneImplementation == null) {
            return false;
        }
        String value = cafienneImplementation.getAttribute(attributeName);
        return value.equalsIgnoreCase("true");
    }

    /**
     * Parses the attribute name into a string, or provides the default value if the attribute is not found.
     *
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

    protected String printElement() {
        String xml = XMLHelper.printXMLNode(element.cloneNode(false));
        xml = xml.replace("\n", "");
        xml = xml.replace("\r", "");
        return xml;
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
     *
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
     *
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
     *
     * @param mine   The element that we want to find an alternative for
     * @param theirs The collection to search the element
     * @param <T>    Target type to cast to
     * @param <Z>    Base type to compare on, to help also search in generics based collections (e.g. Collection[OnPartDefinition])
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
     *
     * @param ours   Source to compare
     * @param theirs Target to compare against
     * @param <X>    Type to compare about
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

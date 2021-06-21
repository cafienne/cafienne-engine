/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.casefile;

import org.cafienne.json.Value;
import org.cafienne.json.ValueList;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.definition.casefile.CaseFileDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.instance.Case;

import java.io.Serializable;

/**
 * Represents a path into the case file to find a specific case file item
 */
public class Path implements Serializable {

    public final String name;
    public final int index;
    private final String originalPath;
    private final Path parent;
    private final Path root;
    private final Path child;
    private final int depth;

    /**
     * Create a new path from a string. Uses the case definition to validate the path.
     * Throws an exception if the path is invalid, i.e., if not all the elements inside the path could be mapped to the case file definition.
     *
     * @param rawPath
     * @throws InvalidPathException
     */
    public Path(String rawPath) throws InvalidPathException {
        this(convertRawPath(rawPath), rawPath);
    }

    /**
     * Creates a path that matches the case file item definition's "path" within the case file.
     *
     * @param cfid
     */
    public Path(CaseFileItemDefinition cfid) {
        this(cfid, null);
    }

    /**
     * Create a path for the case file item instance (including [index] if the item belongs to an array)
     * @param caseFileItem
     */
    public Path(CaseFileItem caseFileItem) {
        this(caseFileItem, null);
    }

    private Path(CaseFileItemDefinition cfid, Path child) {
        this.name = cfid.getName();
        this.index = -1;
        this.child = child;
        if (cfid.getParentElement() instanceof CaseFileDefinition) {
            parent = null;
            root = this;
            depth = 0;
        } else { // ... it is an instance of CaseFileItemDefinition
            parent = new Path(cfid.getParentElement(), this);
            root = parent.root;
            depth = parent.depth + 1;
        }
        this.originalPath = toString();
    }

    private Path(CaseFileItem caseFileItem, Path child) {
        this.name = caseFileItem.getDefinition().getName();
        this.index = caseFileItem.getIndex();
        this.child = child;
        CaseFileItem parentItem = caseFileItem.getParent();
        if (parentItem == null) {
            this.parent = null;
            this.root = this;
            this.depth = 0;
        } else {
            this.parent = new Path(parentItem, this);
            this.root = parent.root;
            this.depth = parent.depth + 1;
        }
        this.originalPath = toString();
    }

    /**
     * Clones the path.
     * @param pathToClone
     * @param newPathRoot
     * @param newPathParent
     * @param requiredDepth
     * @param requiredIndex
     */
    private Path(Path pathToClone, Path newPathRoot, Path newPathParent, int requiredDepth, int requiredIndex) {
        this.name = pathToClone.name;
        this.depth = pathToClone.depth;
        this.root = newPathRoot == null ? this : newPathRoot;
        this.parent = newPathParent;
        // Stop either when we reach depth, or when there is no more child
        if (pathToClone.child == null || pathToClone.depth == requiredDepth) {
            this.index = requiredIndex;
            this.child = null;
        } else {
            this.index = pathToClone.index;
            this.child = new Path(pathToClone.child, this.root, this, requiredDepth, requiredIndex);
        }
        this.originalPath = toString();
    }

    private Path(String[] chain, String originalPath) {
        this(chain, null, chain.length - 1, originalPath);
    }

    private Path(String[] chain, Path child, int depth, String originalPath) {
//        System.out.println("PARSING '" + originalPath +"' with " + chain.length +" chain elements, at depth " + depth + " part is: " + chain[depth]);
        this.child = child;
        this.depth = depth;
        String myPart = chain[depth];
        int openingBracket = myPart.indexOf("[");
        this.index = parseIndex(myPart, originalPath, openingBracket);
        this.name = myPart.substring(0, openingBracket >= 0 ? openingBracket : myPart.length());
        this.parent = depth > 0 ? new Path(chain, this, depth - 1, originalPath) : null;
        this.root = this.parent == null ? this : parent.root;
        this.originalPath = originalPath != null ? originalPath : toString();
    }

    private int parseIndex(String part, String originalPath, int openingBracketPosition) {
        if (openingBracketPosition == 0) {
            throw new InvalidPathException("Missing name part in path, cannot start with an opening bracket '" + originalPath + "'");
        } else if (openingBracketPosition < 0) {
            return -1;
        } else {
            if (!part.endsWith("]")) {
                throw new InvalidPathException("Path should end with a closing bracket, as it has an opening bracket '" + originalPath + "'");
            }
            String indexString = part.substring(openingBracketPosition + 1, part.length() - 1);
            try {
                int index = Integer.parseInt(indexString);
                if (index < 0) {
                    throw new InvalidPathException("'" + indexString + "' is not a valid index. Path index may not be negative '" + originalPath + "'");
                }
                return index;
            } catch (NumberFormatException e) {
                throw new InvalidPathException("'" + indexString + "' is not a valid index in path '" + originalPath + "'");
            }
        }
    }

    /**
     * Resolve the path on the specified case instance to return the corresponding case file item
     *
     * @param caseInstance
     * @return
     * @throws InvalidPathException if the path is not compliant with the case definition, or if it points to
     *                              a non-existing array element that is more than 1 element behind the array size. E.g., if array size is 3, then
     *                              a path [4] will fail, as that assumes 5 elements in the array, whereas array[3] points to a potentially new 4th element.
     */
    public <C extends CaseFileItemCollection> C resolve(Case caseInstance) throws InvalidPathException {
        CaseFile caseFile = caseInstance.getCaseFile();
        if (this.isEmpty()) {
            return (C) caseFile;
        } else {
            return (C) root.resolve(caseFile);
        }
    }

    /**
     * Resolve this path on the parent.
     *
     * @param parentItem
     * @return
     * @throws InvalidPathException if the path is not compliant with the case definition, or if it points to
     *                              a non-existing array element that is more than 1 element behind the array size. E.g., if array size is 3, then
     *                              a path [4] will fail, as that assumes 5 elements in the array, whereas array[3] points to a potentially new 4th element.
     */
    private <C extends CaseFileItemCollection> C resolve(CaseFileItemCollection<?> parentItem) throws InvalidPathException {
        if (isEmpty()) {
            return (C) parentItem;
        }
        CaseFileItem childContainer = parentItem.getItem(name);
        if (childContainer == null) {
            throw new InvalidPathException("The path '" + originalPath + "' is invalid, since the part '" + name + "' is not defined in the case file");
        }
        CaseFileItem item = this.resolveOptionalArrayItem(childContainer);
        if (child != null) {
            return child.resolve(item);
        } else {
            return (C) item;
        }
    }

    private CaseFileItem resolveOptionalArrayItem(CaseFileItem item) {
        if (isArrayElement()) { // This path points to a specific array element, check whether that is valid
            if (item.isArray()) {
                // Ok, that part is ok, but now check whether the index element exists
                CaseFileItemArray array = item.getContainer();
                if (item.getCaseInstance().recoveryRunning()) {
                    return array.getArrayElement(index);
                }
                if (array.size() < index) {
                    throw new InvalidPathException("The array " + array.getPath() + " has only " + array.size() + " items; cannot access " + originalPath);
                } else if (array.size() == index) {
                    throw new InvalidPathException("The array " + array.getPath() + " has only " + array.size() + " items; cannot access " + originalPath + " (Note: index starts at 0 instead of 1)");
                }
                return array.get(index);
            } else {
                throw new InvalidPathException("The path '" + originalPath + "' is invalid because the CaseFileItem is not of type array");
            }
        }
        return item;
    }

    /**
     * Returns the parent map of this path from the case file json object
     * If the path does not yet exist inside the case file, it will be created.
     * Array elements inside the case file that do not have values and are not part of the path will get a null value.
     * E.g., for a path /root/child-array[3]/item/element, this method will resolve to a ValueMap for /item/
     * If child-array does not yet exist in the case file, then the json structure will look like:
     * root: {
     *     child-array:[ null, null, null, {
     *          // this is the json object representing the item
     *     }]
     * }
     *
     * Also, suppose the case file passed contains a structure like
     * root: {
     *     child-array:[ null, null, null, "SOME-STRING-VALUE"]
     * }
     * Then the 4th (string) object in the child-array will be replaced with a new ValueMap (i.e., an empty json object)
     *
     * Note: this method is invoked currently only from CaseFileMerger, so in practice, the child-array will have values.
     *
     * @param casefile
     * @return
     */
    public ValueMap resolveParent(ValueMap casefile) {
        Path parentPath = getParent();
        if (parentPath.isEmpty()) {
            return casefile;
        }
        return parentPath.root.travel(casefile);
    }

    private ValueMap travel(ValueMap parent) {
        ValueMap myValue = getCurrentValue(parent);
        if (child == null) {
            return myValue;
        } else {
            return child.travel(myValue);
        }
    }

    private ValueMap getCurrentValue(ValueMap parent) {
        if (this.isArrayElement()) {
            ValueList list = parent.withArray(this.name);
            if (list.size() > this.index) {
                Value value = list.get(this.index);
                if (value.isMap()) {
                    return value.asMap();
                } else {
                    /// That's really weird! Let's replace the value with a map
                    ValueMap replacement = new ValueMap();
                    list.set(this.index, replacement);
                    return replacement;
                }
            } else {
                // Now what? Let's increase list to proper size with NULL values ...
                for (int i = list.size(); i < this.index; i++) {
                    list.add(Value.NULL);
                }
                // ... but the last one as a ValueMap, so that we can fill it
                ValueMap value = new ValueMap();
                list.add(value);
                return value;
            }
        } else {
            // Note: this will replace an existing non-map element in our parent with a new ValueMap.
            return parent.with(this.name);
        }
    }

    public String toString() {
        if (parent != null) {
            return parent.toString() + "/" + getPart();
        } else {
            return getPart();
        }
    }

    public boolean isEmpty() {
        return name.isEmpty();
    }

    /**
     * Return the relative name of this path.
     *
     * @return
     */
    public String getPart() {
        return isArrayElement() ? name + "[" + index + "]" : name;
    }

    /**
     * Returns this path as array path, so if index is -1 it returns this, else it returns
     * a new path without index element.
     * @return
     */
    public Path getContainer() {
        return new Path(this.root, null, null, this.depth, -1);
    }

    /**
     * Returns the parent path (or an empty path if parent is null)
     * This parent path has no child.
     * @return
     */
    public Path getParent() {
        if (this.parent == null) {
            return new Path("");
        } else {
            return new Path(this.root, null, null, this.depth - 1, this.parent.index);
        }
    }

    /**
     * Return the relative name of this path.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the path index (or -1 if it does not have one)
     *
     * @return
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns true if index greater than or equal to 0;
     * @return
     */
    public boolean isArrayElement() {
        return index >= 0;
    }

    /**
     * Returns true if the other path matches this path
     * @param otherPath
     * @return
     */
    public boolean matches(Path otherPath) {
        return match(this.leaf(), otherPath.leaf());
    }

    /**
     * Returns true if the other path is a child of this path
     * @param otherPath
     * @return
     */
    public boolean hasChild(Path otherPath) {
        if (this.depth >= otherPath.depth) {
            return false;
        }
        while (otherPath.depth > this.depth) {
            otherPath = otherPath.parent;
        }
        return match(this, otherPath);
    }

    /**
     * Returns true if this path element is an element in the other path.
     * E.g. other path is /abc, and this is /abc[0] then it returns true
     * @param otherPath
     * @return
     */
    public boolean isArrayElementOf(Path otherPath) {
        return (this.depth == otherPath.depth && this.isArrayElement() && !otherPath.isArrayElement() && this.name.equals(otherPath.name) && match(this.parent, otherPath.parent));
    }

    private boolean match(Path path1, Path path2) {
        if (path1 == null && path2 == null) {
            return true;
        }
        if (path1.depth != path2.depth) {
            return false;
        }
        if (! path1.name.equals(path2.name)) {
            return false;
        }
        if (path1.index != path2.index) {
            return false;
        }
        return match(path1.parent, path2.parent);
    }

    private Path leaf() {
        if (child == null) {
            return this;
        }
        return child.leaf();
    }

    /**
     * Check whether path is not null, whether it has empty elements,
     * and removes first and last slash if they exist.
     *
     * @param rawPath
     * @return
     * @throws InvalidPathException
     */
    private static String[] convertRawPath(String rawPath) throws InvalidPathException {
        if (rawPath == null) {
            throw new InvalidPathException("Missing path parameter");
        }
        // Empty path elements are no allowed.
        if (rawPath.indexOf("//") >= 0) {
            throw new InvalidPathException("Path should not contain empty elements '//' " + rawPath);
        }
        // Remove optional trailing slash and ending slash
        if (rawPath.startsWith("/")) {
            rawPath = rawPath.substring(1);
        }
        if (rawPath.endsWith("/")) {
            rawPath = rawPath.substring(0, rawPath.length() - 1);
        }

        String[] pathElements = rawPath.split("/");
        for (int i=0; i<pathElements.length; i++) {
//            System.out.println("part["+i+"] = '" + pathElements[i] + "'");
            pathElements[i] = pathElements[i].trim();
        }
        return pathElements;
    }
}

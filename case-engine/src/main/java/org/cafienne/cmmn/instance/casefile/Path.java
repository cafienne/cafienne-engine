/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.casefile;

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
    public CaseFileItem resolve(Case caseInstance) throws InvalidPathException {
        return root.resolve(caseInstance.getCaseFile());
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
    private CaseFileItem resolve(CaseFileItemCollection<?> parentItem) throws InvalidPathException {
        CaseFileItemDefinition itemDefinition = parentItem.getDefinition().getChild(name);
        if (itemDefinition == null) {
            throw new InvalidPathException("The path '" + originalPath + "' is invalid, since the part '" + name + "' is not defined in the case file");
        }
        CaseFileItem item = this.resolveOptionalArrayItem(parentItem.getItem(itemDefinition));
        if (child != null) {
            return child.resolve(item);
        } else {
            return item;
        }
    }

    private CaseFileItem resolveOptionalArrayItem(CaseFileItem item) {
        if (index >= 0) { // This path points to a specific array element, check whether that is valid
            if (item.isArray()) {
                // Ok, that part is ok, but now check whether the index element exists
                CaseFileItemArray array = item.getContainer();
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

    public String toString() {
        String myPart = index < 0 ? name : name + "[" + index + "]";
        if (parent != null) {
            return parent.toString() + "/" + myPart;
        } else {
            return myPart;
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

    public Path getContainer() {
        if (index < 0) {
            return this;
        } else {
            return new Path(this.parent.originalPath + "/" + this.name);
        }
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
        if (this.depth > otherPath.depth) {
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
        return (this.depth == otherPath.depth && this.index != -1 && otherPath.index == -1 && this.name.equals(otherPath.name) && match(this.parent, otherPath.parent));
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

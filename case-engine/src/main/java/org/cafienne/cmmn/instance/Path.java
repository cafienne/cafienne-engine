/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a path into the case file to find a specific case file item
 */
public class Path implements Serializable {
    private final String name;
    private final Map<String, Object> identifiers = new LinkedHashMap<String, Object>();
    private final transient CaseFileItemDefinition definition; // TODO: should this be transient. Unfortunately yes.
    private final Path parent;
    private int index = -1; // TODO: index should become final too. Additionally it should also support "last" and "current. Probably need an
                            // additional class for it.
    private Path child;

    /**
     * Creates a path that matches the case file item definition's "path" within the case file.
     *
     * @param cfid
     */
    public Path(CaseFileItemDefinition cfid) {
        this.name = cfid.getName();
        this.definition = cfid;
        if (cfid.getParentElement() instanceof CaseFileDefinition) {
            parent = null;
        } else { // ... it is an instance of CaseFileItemDefinition
            parent = new Path((CaseFileItemDefinition) cfid.getParentElement());
            parent.child = this;
        }
    }

    /**
     * Create a new path from a string. Uses the case definition to validate the path.
     * Throws an exception if the path is invalid, i.e., if not all the elements inside the path could be mapped to the case file definition.
     *
     * @param path
     * @param caseInstance
     * @throws InvalidPathException
     */
    public Path(String path, Case caseInstance) throws InvalidPathException {
        this(path, path, caseInstance.getDefinition());
    }

    /**
     * Internal constructor that parses the path, whilst keeping the original path in order to be able to throw a proper exception
     *
     * @param path
     * @param originalPath
     * @param caseDefinition
     * @throws InvalidPathException
     */
    private Path(String path, String originalPath, CaseDefinition caseDefinition) throws InvalidPathException {
        // Note, this algorithm works it's way up from the bottom to the top path.
        // So, it also resolves all of the parent paths.

        // First remove slashes at begin and end
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1); // Remove ending slash
        if (path.startsWith("/"))
            path = path.substring(1); // Remove starting slash

        int lastIndexOfSlash = path.lastIndexOf("/");
        if (lastIndexOfSlash >= 0) {
            name = parseName(path.substring(lastIndexOfSlash + 1));
            String parentPath = path.substring(0, lastIndexOfSlash);
            parent = new Path(parentPath, originalPath, caseDefinition);
            parent.child = this;
            definition = resolveCaseFileItemDefinition(parent.definition.getChildren());
        } else {
            parent = null;
            name = parseName(path);
            definition = resolveCaseFileItemDefinition(caseDefinition.getCaseFileModel().getCaseFileItems());
        }
        if (this.definition == null) {
            throw new InvalidPathException("The path '" + originalPath + "' is invalid, since the part '" + name + "' is not defined in the case file");
        }
    }

    /**
     * Simple constructor to copy and create a new path.
     *
     * @param path
     */
    private Path(Path path) {
        name = path.name;
        parent = path.parent;
        child = path.child;
        definition = path.definition;
        index = path.index;
    }

    Path(CaseFileItem caseFileItem) {
        name = caseFileItem.getDefinition().getName();
        CaseFileItem parentItem = caseFileItem.getParent();
        if (parentItem == null) {
            parent = null;
        } else {
            parent = new Path(parentItem);
            parent.child = this;
        }
        definition = caseFileItem.getDefinition();
        index = caseFileItem.getIndex();
    }

    private String parseName(String subPath) throws InvalidPathException {
        int openingBracket = subPath.indexOf("[");
        if (openingBracket == 0) {
            throw new InvalidPathException("Missing name part in path, cannot start with an opening bracket");
        }
        if (openingBracket > 0) {
            if (subPath.lastIndexOf("]") != subPath.length() - 1) {
                throw new InvalidPathException("Path should end with a closing bracket, as it has an opening bracket");
            }
            String indexString = subPath.substring(openingBracket + 1, subPath.length() - 1);
            try {
                index = Integer.parseInt(indexString);
                if (index < 0) {
                    throw new InvalidPathException("'" + indexString + "' is not a valid index. Path index may not be negative");
                }
            } catch (NumberFormatException e) {
                throw new InvalidPathException("'" + indexString + "' is not a valid index in a path object");
            }
        } else {
            openingBracket = subPath.length();
        }
        return subPath.substring(0, openingBracket);
    }

    private CaseFileItemDefinition resolveCaseFileItemDefinition(Collection<CaseFileItemDefinition> itemDefinitions) {
        return itemDefinitions.stream().filter(c -> {
            return c.getName().equals(name);
        }).findFirst().orElse(null);
    }

    public String toString() {
        if (parent != null) {
            return parent.toString() + "/" + getSubPathString();
        } else {
            return getSubPathString();
        }
    }

    private String getSubPathString() {
        StringBuilder subPath = new StringBuilder(name);
        if (index >= 0) {
            subPath.append("[" + index + "]");
        }
        return subPath.toString();
    }

    /**
     * Returns the parent path.
     *
     * @return
     */
    public Path getParent() {
        return parent;
    }

    /**
     * Returns the direct child path
     *
     * @return
     */
    public Path getChild() {
        return child;
    }

    /**
     * Returns a trimmed version of this path. I.e., any trailing children are removed from the path.
     *
     * @return
     */
    public Path trim() {
        Path trimmed = new Path(this);
        trimmed.child = null;
        return trimmed;
    }

    /**
     * Returns the {@link CaseFileItemDefinition} that corresponds with this {@link Path}
     *
     * @return
     */
    CaseFileItemDefinition getCaseFileItemDefinition() {
        return definition;
    }

    /**
     * Returns the map of identifiers for this level in the path
     *
     * @return
     */
    public Map<String, Object> getIdentifiers() {
        return identifiers;
    }

    /**
     * Allows to search for a specific object within the path, e.g., if an order
     * has 10 lines, then through <code>setIdentifier("LineID", 10)</code> the path
     * can be made more specific.
     *
     * @param propertyName
     * @param propertyValue
     */
    public void setIdentifier(String propertyName, Object propertyValue) {
        identifiers.put(propertyName, propertyValue);
    }

    /**
     * Returns the top level path element.
     *
     * @return
     */
    public Path getRoot() {
        if (parent == null) {
            return this;
        }
        return parent.getRoot();
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
     * @return
     */
    public int getIndex() {
        return index;
    }
}
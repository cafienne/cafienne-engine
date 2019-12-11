package org.cafienne.service.api.projection.cases;

import org.cafienne.cmmn.instance.Path;

/**
 * Note: StringPath is basically a copy of {@link Path}. Reason for this is
 * that Path is not serializable (or better, not deserializable) because the
 * case definition is required to construct the path.
 * Code in here is largely copied from Path, and that is an ugly no go, but for now it works.
 * We have decided to keep it, in anticipation of the refactoring of case file handling
 * in the relationship to ElasticSearch (in order to benefit the search capabilities also on case file content)
 *
 */
public class StringPath {
    private final StringPath parent;
    private final String name;
    private int index = -1;
    private StringPath child;

    public StringPath(String path, String originalPath) throws InvalidPath {
        // First remove slashes at begin and end
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1); // Remove ending slash
        }
        if (path.startsWith("/")) {
            path = path.substring(1); // Remove starting slash
        }

        int lastIndexOfSlash = path.lastIndexOf("/");
        if (lastIndexOfSlash >= 0) {
            name = parseName(path.substring(lastIndexOfSlash + 1));
            String parentPath = path.substring(0, lastIndexOfSlash);
            parent = new StringPath(parentPath, originalPath);
            parent.child = this;
        } else {
            parent = null;
            name = parseName(path);
        }
    }

    public String getName() {
        return name;
    }

    public StringPath getChild() {
        return child;
    }

    public StringPath getParent() {
        return parent;
    }

    public StringPath getRoot() {
        if (parent == null) {
            return this;
        }
        return parent.getRoot();
    }

    public int getIndex() {
        return index;
    }

    private String parseName(String subPath) throws InvalidPath {
        int openingBracket = subPath.indexOf("[");
        if (openingBracket == 0) {
            throw new InvalidPath("Missing name part in path, cannot start with an opening bracket");
        }
        if (openingBracket > 0) {
            if (subPath.lastIndexOf("]") != subPath.length() - 1) {
                throw new InvalidPath("Path should end with a closing bracket, as it has an opening bracket");
            }
            String indexString = subPath.substring(openingBracket + 1, subPath.length() - 1);
            try {
                index = Integer.parseInt(indexString);
                if (index < 0) {
                    throw new InvalidPath("'" + indexString + "' is not a valid index. Path index may not be negative");
                }
            } catch (NumberFormatException e) {
                throw new InvalidPath("'" + indexString + "' is not a valid index in a path object");
            }
        } else {
            openingBracket = subPath.length();
        }
        return subPath.substring(0, openingBracket);
    }

    /**
     * See {@link StringPath} comments.
     *
     */
    public static class InvalidPath extends Exception {
        InvalidPath(String msg) {
            super(msg);
        }
    }
}

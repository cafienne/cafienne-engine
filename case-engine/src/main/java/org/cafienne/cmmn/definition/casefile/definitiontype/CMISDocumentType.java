package org.cafienne.cmmn.definition.casefile.definitiontype;

/**
 * By default we accept JSON.
 *
 */
public class CMISDocumentType extends JSONType {
    @Override
    public boolean isDocument() {
        return true;
    }
}

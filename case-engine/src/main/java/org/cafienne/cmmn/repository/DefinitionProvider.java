package org.cafienne.cmmn.repository;

import org.cafienne.cmmn.definition.DefinitionsDocument;
import org.cafienne.cmmn.definition.InvalidDefinitionException;
import org.cafienne.akka.actor.identity.TenantUser;

import java.util.List;

/**
 * Basic interface abstracting retrieval of metadata for the engine.
 */
public interface DefinitionProvider {
    /**
     * Returns a list of names of definition documents that can be viewed by the specified user
     * @return
     */
    List<String> list(TenantUser user);

    /**
     * Reads the DefinitionsDocument if the user has access to it
     * @param user
     * @param name
     * @return
     * @throws MissingDefinitionException
     * @throws InvalidDefinitionException
     */
    DefinitionsDocument read(TenantUser user, String name) throws MissingDefinitionException, InvalidDefinitionException;

    /**
     * Writes a DefinitionsDocument into the DefinitionProvider in the context of the user
     * @param user
     * @param name
     * @param definitionsDocument
     * @throws MissingDefinitionException
     */
    void write(TenantUser user, String name, DefinitionsDocument definitionsDocument) throws WriteDefinitionException;
}

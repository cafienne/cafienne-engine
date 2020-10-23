package org.cafienne.cmmn.repository;

import org.cafienne.akka.actor.identity.PlatformUser;
import org.cafienne.cmmn.definition.DefinitionsDocument;
import org.cafienne.cmmn.definition.InvalidDefinitionException;

import java.util.List;

/**
 * Basic interface abstracting retrieval of metadata for the engine.
 */
public interface DefinitionProvider {
    /**
     * Returns a list of names of definition documents that can be viewed by the specified user
     * @return
     * @param user
     * @param tenant
     */
    List<String> list(PlatformUser user, String tenant);

    /**
     * Reads the DefinitionsDocument if the user has access to it
     * @param user
     * @param tenant
     * @param name
     * @return
     * @throws MissingDefinitionException
     * @throws InvalidDefinitionException
     */
    DefinitionsDocument read(PlatformUser user, String tenant, String name) throws MissingDefinitionException, InvalidDefinitionException;

    /**
     * Writes a DefinitionsDocument into the DefinitionProvider in the context of the user
     * @param user
     * @param tenant
     * @param name
     * @param definitionsDocument
     * @throws MissingDefinitionException
     */
    void write(PlatformUser user, String tenant, String name, DefinitionsDocument definitionsDocument) throws WriteDefinitionException;
}

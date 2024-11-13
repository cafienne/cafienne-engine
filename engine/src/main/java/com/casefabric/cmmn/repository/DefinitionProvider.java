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

package com.casefabric.cmmn.repository;

import com.casefabric.actormodel.identity.UserIdentity;
import com.casefabric.cmmn.definition.DefinitionsDocument;
import com.casefabric.cmmn.definition.InvalidDefinitionException;

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
    List<String> list(UserIdentity user, String tenant);

    /**
     * Reads the DefinitionsDocument if the user has access to it
     * @param user
     * @param tenant
     * @param name
     * @return
     * @throws MissingDefinitionException
     * @throws InvalidDefinitionException
     */
    DefinitionsDocument read(UserIdentity user, String tenant, String name) throws MissingDefinitionException, InvalidDefinitionException;

    /**
     * Writes a DefinitionsDocument into the DefinitionProvider in the context of the user
     * @param user
     * @param tenant
     * @param name
     * @param definitionsDocument
     * @throws MissingDefinitionException
     */
    void write(UserIdentity user, String tenant, String name, DefinitionsDocument definitionsDocument) throws WriteDefinitionException;
}

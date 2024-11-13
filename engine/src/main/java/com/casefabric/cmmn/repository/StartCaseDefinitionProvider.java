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

import com.typesafe.config.Config;
import com.casefabric.actormodel.exception.AuthorizationException;
import com.casefabric.actormodel.identity.UserIdentity;
import com.casefabric.cmmn.definition.DefinitionsDocument;
import com.casefabric.cmmn.definition.InvalidDefinitionException;
import com.casefabric.cmmn.repository.file.SimpleLRUCache;
import com.casefabric.infrastructure.CaseFabric;
import com.casefabric.util.XMLHelper;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StartCaseDefinitionProvider implements DefinitionProvider {
    private final Map<String, DefinitionsDocument> cache = new SimpleLRUCache(CaseFabric.config().repository().cacheSize());
    private final static String AUTHORIZED_TENANT_ROLES = "authorized-tenant-roles";
    private final List<String> authorizedTenantRoles;

    @Override
    public List<String> list(UserIdentity user, String tenant) {
        return new ArrayList<>();
    }

    public StartCaseDefinitionProvider() {
        this.authorizedTenantRoles = readTenantRoles();
    }

    private List<String> readTenantRoles() {
        Config config = CaseFabric.config().repository().config();
        if (config.hasPath(AUTHORIZED_TENANT_ROLES)) {
            return config.getStringList(AUTHORIZED_TENANT_ROLES);
        } else {
            return new ArrayList<>();
        }
    }

    private void checkAccess(UserIdentity user, String tenant) {
        if (authorizedTenantRoles.isEmpty()) return;

        // Extend retrieving tenant user to only if we have configured role based authorization

        // NOTE This logic is temporarily unavailable;

//        TenantUser tenantUser = user.getTenantUser(tenant);
//        for (String role: authorizedTenantRoles) {
//            if (tenantUser.roles().contains(role)) {
//                return;
//            }
//        }
        throw new AuthorizationException("User " + user.id() + " is not allowed to perform this operation");
    }

    @Override
    public DefinitionsDocument read(UserIdentity user, String tenant, String contents) throws InvalidDefinitionException {
        checkAccess(user, tenant);
        try {
            // Now check to see if the file is already in our cache, and, if so, check whether it has the same last modified; if not, put the new one in the cache instead
            DefinitionsDocument cacheEntry = cache.get(contents);
            if (cacheEntry == null) {
                cacheEntry = new DefinitionsDocument(XMLHelper.loadXML(contents));
                cache.put(contents, cacheEntry);
            }
            return cacheEntry;
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new InvalidDefinitionException("Cannot parse definition", e);
        }
    }

    @Override
    public void write(UserIdentity user, String tenant, String name, DefinitionsDocument definitionsDocument) throws WriteDefinitionException {
        throw new WriteDefinitionException("This operation is not supported");
    }
}

package org.cafienne.cmmn.repository;

import com.typesafe.config.Config;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.command.exception.AuthorizationException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.definition.DefinitionsDocument;
import org.cafienne.cmmn.definition.InvalidDefinitionException;
import org.cafienne.cmmn.repository.file.SimpleLRUCache;
import org.cafienne.util.XMLHelper;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StartCaseDefinitionProvider implements DefinitionProvider {
    private final Map<String, DefinitionsDocument> cache = new SimpleLRUCache(CaseSystem.config().repository().cacheSize());
    private final static String AUTHORIZED_TENANT_ROLES = "authorized-tenant-roles";
    private final List<String> authorizedTenantRoles;

    @Override
    public List<String> list(TenantUser user) {
        return new ArrayList();
    }

    public StartCaseDefinitionProvider() {
        this.authorizedTenantRoles = readTenantRoles();
    }

    private List<String> readTenantRoles() {
        Config config = CaseSystem.config().repository().config();
        if (config.hasPath(AUTHORIZED_TENANT_ROLES)) {
            return config.getStringList(AUTHORIZED_TENANT_ROLES);
        } else {
            return new ArrayList();
        }
    }

    private void checkAccess(TenantUser user) {
        if (authorizedTenantRoles.isEmpty()) return;

        for (String role: authorizedTenantRoles) {
            if (user.roles().contains(role)) {
                return;
            }
        }
        throw new AuthorizationException("User " + user.id() + " is not allowed to perform this operation");
    }

    @Override
    public DefinitionsDocument read(TenantUser user, String contents) throws InvalidDefinitionException {
        checkAccess(user);
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
    public void write(TenantUser user, String name, DefinitionsDocument definitionsDocument) throws WriteDefinitionException {
        throw new WriteDefinitionException("This operation is not supported");
    }
}

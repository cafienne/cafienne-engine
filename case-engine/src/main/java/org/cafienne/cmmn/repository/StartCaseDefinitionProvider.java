package org.cafienne.cmmn.repository;

import com.typesafe.config.Config;
import org.cafienne.actormodel.command.exception.AuthorizationException;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.actormodel.identity.PlatformUser;
import org.cafienne.actormodel.identity.TenantUser;
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
    private final Map<String, DefinitionsDocument> cache = new SimpleLRUCache(Cafienne.config().repository().cacheSize());
    private final static String AUTHORIZED_TENANT_ROLES = "authorized-tenant-roles";
    private final List<String> authorizedTenantRoles;

    @Override
    public List<String> list(PlatformUser user, String tenant) {
        return new ArrayList();
    }

    public StartCaseDefinitionProvider() {
        this.authorizedTenantRoles = readTenantRoles();
    }

    private List<String> readTenantRoles() {
        Config config = Cafienne.config().repository().config();
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
    public DefinitionsDocument read(PlatformUser user, String tenant, String contents) throws InvalidDefinitionException {
        checkAccess(user.getTenantUser(tenant));
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
    public void write(PlatformUser user, String tenant, String name, DefinitionsDocument definitionsDocument) throws WriteDefinitionException {
        throw new WriteDefinitionException("This operation is not supported");
    }
}

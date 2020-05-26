package org.cafienne.tenant.akka.command.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.identity.PlatformUser;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.*;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.response.TenantResponse;
import org.cafienne.tenant.akka.event.TenantUserCreated;
import org.cafienne.tenant.akka.event.TenantUserRoleAdded;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import scala.collection.JavaConverters;
import scala.collection.Seq;

@Manifest
public class BootstrapTenant extends CreateTenant {
    private final Set<TenantUser> users;

    private enum Fields {
        users
    }

    public BootstrapTenant(PlatformUser user, String tenantId, String name, Set<TenantUser> owners, Set<TenantUser> users) {
        super(user, tenantId, name, owners);
        // Filter out empty and null user id's for the set of users.
        this.users = users;
    }

    public BootstrapTenant(ValueMap json) {
        super(json);
        this.users = new HashSet();
        ValueList jsonUsers = json.withArray(Fields.users);
        jsonUsers.forEach(value -> {
            ValueMap userJson = (ValueMap) value;
            this.users.add(TenantUser.from(userJson));
        });
    }

    @Override
    public TenantResponse process(TenantActor tenant) {
        super.process(tenant);
        // Register the users as TenantUsers with the specified roles
        users.forEach(user -> {
            tenant.addEvent(new TenantUserCreated(tenant, user.id(), user.name(), user.email()));
            // Add all the roles
            user.roles().foreach(role -> tenant.addEvent(new TenantUserRoleAdded(tenant, user.id(), role)));
        });

        return new TenantResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        generator.writeArrayFieldStart(Fields.users.toString());
        for (TenantUser owner : users) {
            owner.write(generator);
        }
        generator.writeEndArray();
    }

    public static BootstrapTenant parseJSONFile(String fileName) {
        try {
            Value<?> json = JSONReader.parse(new FileInputStream(fileName));
            if ((json instanceof ValueMap)) {
                return createBootstrapTenantCommandFromJSON((ValueMap) json);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONParseFailure jsonParseFailure) {
            jsonParseFailure.printStackTrace();
        }
        return null;
    }

    private static BootstrapTenant createBootstrapTenantCommandFromJSON(ValueMap json) {
        String tenantName = json.raw("name");
        List<String> ownerIds = json.withArray("owners").rawList();
        ValueList jsonUsers = json.withArray("users");

        List<TenantUser> allTenantUsers = jsonUsers.stream().map((Value<?> userValue) -> {
            ValueMap user = (ValueMap) userValue;

            String userId = user.raw("id");
            List<String> rolesList = user.withArray("roles").rawList();
            Seq<String> roles = JavaConverters.asScalaIteratorConverter(rolesList.iterator()).asScala().toSeq();
            String userName = user.raw("name");
            String email = user.raw("email");
            return new TenantUser(userId, roles, tenantName, userName, email, true);
        }).collect(Collectors.toList());


        Set<TenantUser> owners = allTenantUsers.stream().filter(user -> ownerIds.contains(user.id())).collect(Collectors.toSet());
        Set<TenantUser> otherUsers = allTenantUsers.stream().filter(user -> !ownerIds.contains(user.id())).collect(Collectors.toSet());

        String platformOwnerId = CaseSystem.config().platform().platformOwners().get(0);
        Seq<TenantUser> emptySeq = JavaConverters.asScalaIteratorConverter(new ArrayList<TenantUser>().iterator()).asScala().toSeq();
        PlatformUser platformOwner = new PlatformUser(platformOwnerId, emptySeq);
        return new BootstrapTenant(platformOwner, tenantName, tenantName, owners, otherUsers);
    }
}


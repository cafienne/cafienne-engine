package org.cafienne.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.akka.actor.snapshot.ModelActorSnapshot;
import org.cafienne.cmmn.akka.command.platform.CaseUpdate;
import org.cafienne.cmmn.akka.command.platform.PlatformUpdate;
import org.cafienne.cmmn.akka.command.platform.TenantUpdate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Object that can be saved as snapshot offer for the TimerService persistent actor
 */
@Manifest
public class PlatformStorage implements ModelActorSnapshot {
    private List<TenantUpdate> tenantsToUpdate = new ArrayList();
    private List<CaseUpdate> casesToUpdate = new ArrayList();
    private TenantUser user;

    public PlatformStorage() {
    }

    public PlatformStorage(ValueMap json) {
        json.withArray(Fields.tenants).forEach(value -> add(TenantUpdate.deserialize(value.asMap())));
        json.withArray(Fields.cases).forEach(value -> add(CaseUpdate.deserialize(value.asMap())));
        if (json.has(Fields.user)) {
            this.user = TenantUser.from(json.with(Fields.user));
        }
    }

    private boolean changed = false;

    boolean changed() {
        return changed;
    }

    List<InformJob> getJobs() {
        List<InformJob> jobs = new ArrayList();
        tenantsToUpdate.forEach(tenant -> jobs.add(new InformTenantJob(user, tenant)));
        casesToUpdate.forEach(update -> jobs.add(new InformCaseJob(user, update)));
        return jobs;
    }

    void removeTenant(Object tenant) {
        tenantsToUpdate.remove(tenant);
        changed = true;
    }

    void removeCase(Object caseId) {
        casesToUpdate.remove(caseId);
        changed = true;
    }

    void saved() {
        changed = false;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        generator.writeArrayFieldStart(Fields.tenants.toString());
        for (TenantUpdate update : tenantsToUpdate) update.toValue().print(generator);
        generator.writeEndArray();
        generator.writeArrayFieldStart(Fields.cases.toString());
        for (CaseUpdate update : casesToUpdate) update.toValue().print(generator);
        generator.writeEndArray();
        if (this.user != null) {
            writeField(generator, Fields.user, user);
        }
    }

    void add(TenantUpdate tenantUpdate) {
        tenantsToUpdate.add(tenantUpdate);
        changed = true;
    }

    void add(CaseUpdate caseUpdate) {
        casesToUpdate.add(caseUpdate);
        changed = true;
    }

    public void setNewInformation(PlatformUpdate newUserInformation) {
    }

    public void setUser(TenantUser user) {
        this.user = user;
        this.changed = true;
    }
}

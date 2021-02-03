package org.cafienne.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.response.CommandFailure;
import org.cafienne.actormodel.identity.PlatformUser;
import org.cafienne.infrastructure.serialization.CafienneSerializable;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueList;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.actorapi.command.UpdateCaseWithPlatformInformation;
import org.cafienne.platform.actorapi.command.UpdatePlatformInformation;
import org.cafienne.tenant.actorapi.command.platform.UpdateTenantWithPlatformInformation;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BatchJob implements CafienneSerializable {
    private PlatformStorage storage;
    final String batchIdentifier;
    private final ValueList failures;
    private final List<InformJob> jobs = new ArrayList();
    private final Instant createdOn;
    private Instant completedOn;
    private final ValueMap jobCount;

    public BatchJob(PlatformStorage storage, UpdatePlatformInformation command) {
        this.storage = storage;
        this.batchIdentifier = command.getMessageId();
        this.failures = new ValueList();
        // Convert PlatformUpdates into InformJobs
        PlatformUser user = PlatformUser.from(command.getUser());
        command.tenants.forEach(update -> jobs.add(new InformJob(this, new UpdateTenantWithPlatformInformation(user, update))));
        command.cases.forEach(update -> jobs.add(new InformJob(this, new UpdateCaseWithPlatformInformation(user, update))));
        this.createdOn = Instant.now();
        this.jobCount = new ValueMap("total", jobs.size(), "tenants", command.tenants.size(), "cases", command.cases.size());
    }

    BatchJob(PlatformStorage snapshot, ValueMap json) {
        this.storage = snapshot;
        this.batchIdentifier = readField(json, Fields.identifier);
        json.withArray(Fields.jobs).forEach(job -> jobs.add(new InformJob(this, job.asMap())));
        this.failures = json.withArray(Fields.exception);
        this.createdOn = readInstant(json, Fields.createdOn);
        this.completedOn = readInstant(json, Fields.completedOn);
        this.jobCount = json.with(Fields.jobCount);
    }

    void adoptStorage(PlatformStorage storage) {
        this.storage = storage;
    }

    List<InformJob> getJobs() {
        List<InformJob> pendingJobs = jobs.stream().filter(InformJob::pending).collect(Collectors.toList());
        return pendingJobs;
    }

    void reportFailure(InformJob job, CommandFailure failure) {
        failures.add(new ValueMap("job", job, "failure", failure.exception()));
        removeJob(job);
        storage.reportFailure(failure);
    }

    void reportSuccess(InformJob job) {
        removeJob(job);
        // Save the snapshot
        storage.reportSuccess();
    }

    private void removeJob(InformJob job) {
        synchronized (jobs) {
            if (jobs.remove(job)) { // Only if the job was still there will we check whether the list now is empty.
                if (jobs.isEmpty()) {
                    completedOn = Instant.now();
                    logger.info("Batch completed in " + (completedOn.toEpochMilli() - createdOn.toEpochMilli()) + " milliseconds");
                    storage.batchCompleted(this);
                }
            }
        }
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeField(generator, Fields.identifier, batchIdentifier);
        writeField(generator, Fields.createdOn, createdOn);
        writeField(generator, Fields.completedOn, completedOn);
        writeField(generator, Fields.jobCount, jobCount);
        synchronized (jobs) {
            List<InformJob> pendingJobs = jobs.stream().filter(InformJob::pending).collect(Collectors.toList());
            List<InformJob> runningJobs = jobs.stream().filter(InformJob::active).collect(Collectors.toList());
            generator.writeArrayFieldStart(Fields.jobs.toString());
            for (InformJob job : pendingJobs) job.writeThisObject(generator);
            generator.writeEndArray();
            generator.writeArrayFieldStart("running");
            for (InformJob job : runningJobs) job.writeThisObject(generator);
            generator.writeEndArray();
        }
        writeField(generator, Fields.exception, failures);
    }

    ValueMap getHistory() {
        return new ValueMap(Fields.identifier, batchIdentifier, "started", createdOn, "completed", completedOn, Fields.jobCount, jobCount, "failures", failures);
    }

    public ValueMap getStatus() {
        ValueMap status = new ValueMap(Fields.identifier, batchIdentifier, "started", createdOn);
        if (completedOn != null) status.putRaw("completed", completedOn);
        status.putRaw(Fields.jobCount.toString(), jobCount);
        status.putRaw("active", jobs.stream().filter(InformJob::active).count());
        status.putRaw("pending", jobs.stream().filter(InformJob::pending).count());
        status.putRaw("failures", failures);
        return status;
    }
}

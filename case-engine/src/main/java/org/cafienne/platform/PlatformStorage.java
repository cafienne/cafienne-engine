package org.cafienne.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.akka.actor.snapshot.RelaxedSnapshot;
import org.cafienne.cmmn.akka.command.platform.CaseUpdate;
import org.cafienne.cmmn.akka.command.platform.TenantUpdate;
import org.cafienne.platform.akka.command.UpdatePlatformInformation;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Object that can be saved as snapshot offer for the TimerService persistent actor
 */
@Manifest
public class PlatformStorage extends RelaxedSnapshot<PlatformService> {
    private final List<UpdatePlatformInformation> updates = new ArrayList();
    private final List<UpdatePlatformInformation> pendingUpdates = new ArrayList();
    private final ValueList failures;

    private static FiniteDuration getDuration() {
        return Duration.create(CaseSystem.config().engine().platformServiceConfig().persistDelay(), TimeUnit.SECONDS);
    }

    PlatformStorage(PlatformService service) {
        super(service, getDuration());
        failures = new ValueList();
    }

    public PlatformStorage(ValueMap json) {
        // This is a snapshot being deserialized. Does not have a job queue and will be merged into the one and only later
        super();
        json.withArray(Fields.update).forEach(value -> updates.add(new UpdatePlatformInformation(value.asMap())));
        failures = json.withArray(Fields.exception);
    }

    /**
     * Merge snapshot from recovery into the this storage object; adds the objects into the list of pending updates
     * @param snapshot
     */
    void merge(PlatformStorage snapshot) {
        snapshot.updates.forEach(this::registerUpdate);
        failures.addAll(snapshot.failures.getValue());
    }

    private void registerUpdate(UpdatePlatformInformation updatePlatformInformation) {
        pendingUpdates.add(updatePlatformInformation);
        updates.add(updatePlatformInformation);
    }

    List<UpdatePlatformInformation> getPendingUpdates() {
        List<UpdatePlatformInformation> penders = new ArrayList<>(pendingUpdates);
        pendingUpdates.clear();
        return penders;
    }

    void reportFailure(InformJob job, CommandFailure failure) {
        if (job.action instanceof CaseUpdate) {
            failures.add(new ValueMap("action", ((CaseUpdate) job.action).toValue(), "failure", failure.exception()));
        } else if (job.action instanceof TenantUpdate) {
            failures.add(new ValueMap("action", ((TenantUpdate) job.action).toValue(), "failure", failure.exception()));
        }
        save("encountered failure " + failure.exception().getMessage());
    }

    void reportSuccess(InformJob job) {
        // Save the snapshot
        enableTimedSnapshotSaver();
    }

    void addUpdate(UpdatePlatformInformation updatePlatformInformation) {
        registerUpdate(updatePlatformInformation);
        save("received new information to handle");
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        generator.writeArrayFieldStart(Fields.update.toString());
        for (UpdatePlatformInformation update : updates) update.writeThisObject(generator);
        generator.writeEndArray();
        writeField(generator, Fields.exception, failures);
    }

    int numPendingUpdates() {
        return pendingUpdates.size();
    }

    ValueList getFailures() {
        return failures;
    }
}

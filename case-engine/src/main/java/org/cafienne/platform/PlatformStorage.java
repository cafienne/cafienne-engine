package org.cafienne.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.akka.actor.snapshot.RelaxedSnapshot;
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

    public PlatformStorage(PlatformService service) {
        super(service, getDuration());
    }

    private static FiniteDuration getDuration() {
        return Duration.create(CaseSystem.config().engine().platformServiceConfig().persistDelay(), TimeUnit.SECONDS);
    }

    public PlatformStorage(ValueMap json) {
        // This is a snapshot being deserialized. Does not have a job queue and will be merged into the one and only later
        super();
        json.withArray(Fields.update).forEach(value -> updates.add(new UpdatePlatformInformation(value.asMap())));
    }

    void merge(PlatformStorage snapshot) {
        snapshot.updates.forEach(this::registerUpdate);
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

    void changed() {
        enableTimedSnapshotSaver();
    }

    void addUpdate(UpdatePlatformInformation updatePlatformInformation) {
        registerUpdate(updatePlatformInformation);
        save("Received new information to handle");
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        generator.writeArrayFieldStart(Fields.update.toString());
        for (UpdatePlatformInformation update : updates) update.writeThisObject(generator);
        generator.writeEndArray();
    }
}

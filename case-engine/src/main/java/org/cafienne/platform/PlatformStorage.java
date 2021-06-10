package org.cafienne.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.response.CommandFailure;
import org.cafienne.actormodel.config.Cafienne;
import org.cafienne.actormodel.serialization.Fields;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueList;
import org.cafienne.json.ValueMap;
import org.cafienne.actormodel.snapshot.RelaxedSnapshot;
import org.cafienne.platform.actorapi.command.UpdatePlatformInformation;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Object that can be saved as snapshot offer for the TimerService persistent actor
 */
@Manifest
public class PlatformStorage extends RelaxedSnapshot<PlatformService> {
    private final List<BatchJob> batches = new ArrayList();
    private final List<String> pendingBatches = new ArrayList();
    private final ValueList history;

    private static FiniteDuration getDuration() {
        return Duration.create(Cafienne.config().engine().platformServiceConfig().persistDelay(), TimeUnit.SECONDS);
    }

    PlatformStorage(PlatformService service) {
        super(service, getDuration());
        this.history = new ValueList();
    }

    public PlatformStorage(ValueMap json) {
        // This is a snapshot being deserialized. Does not have a job queue and will be merged into the one and only later
        super();
//        getLogger().warn("\n\n\t\tRECOVERING PLATFORM STORAGE WITH JSON " + json + "\n\n\n");
        json.withArray(Fields.update).forEach(value -> batches.add(new BatchJob(this, value.asMap())));
        this.history = json.withArray(Fields.historyState);
    }

    /**
     * Merge snapshot from recovery into the this storage object; adds the objects into the list of pending updates
     * @param snapshot
     */
    void merge(PlatformStorage snapshot) {
        snapshot.batches.forEach(batch -> registerBatch(batch));
        this.history.merge(snapshot.history);
    }

    private void registerBatch(BatchJob batch) {
        // This is needed to move snapshot deserialized batches into the main PlatformStorage object of the service.
        batch.adoptStorage(this);
        batches.add(batch);
        pendingBatches.add(batch.batchIdentifier);
    }

    List<BatchJob> getNewBatches() {
        List<String> newBatches = new ArrayList<>(pendingBatches);
        pendingBatches.clear();
        return this.batches.stream().filter(batch -> newBatches.contains(batch.batchIdentifier)).collect(Collectors.toList());
    }

    void batchCompleted(BatchJob batch) {
        synchronized (batches) {
            if (batches.remove(batch)) {
                // Remove the batch from the list of in-progress batches, and add it to our history
                history.add(batch.getHistory());
            }
        }
        enableTimedSnapshotSaver();
    }

    void reportFailure(CommandFailure failure) {
        save("encountered failure " + failure.exception().getMessage());
    }

    void reportSuccess() {
        // Save the snapshot
        enableTimedSnapshotSaver();
    }

    void addUpdate(UpdatePlatformInformation updatePlatformInformation) {
        registerBatch(new BatchJob(this, updatePlatformInformation));
        save("received new information to handle");
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        generator.writeArrayFieldStart(Fields.update.toString());
        for (BatchJob batch : batches) batch.writeThisObject(generator);
        generator.writeEndArray();
        writeField(generator, Fields.historyState, history);
    }

    public ValueMap getStatus() {
        ValueList batchStatus = new ValueList();
        for (BatchJob batch : batches) batchStatus.add(batch.getStatus());
        return new ValueMap("history", history, "batches", batchStatus);
    }
}

/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.service.db.materializer;

import akka.dispatch.Futures;
import org.cafienne.actormodel.command.response.ActorLastModified;
import org.cafienne.actormodel.event.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Promise;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registration of the last modified timestamp per case instance. Can be used by writers to and query actors to get notified about CaseLastModified.
 */
public class LastModifiedRegistration {
    private final static Logger logger = LoggerFactory.getLogger(LastModifiedRegistration.class);
    /**
     * Global startup moment of the whole JVM for last modified requests trying to be jumpy.
     */
    private final static Instant startupMoment = Instant.now();
    private final Map<String, Instant> lastModifiedRegistration = new HashMap<>();
    private final Map<String, List<Waiter>> waiters = new HashMap<>();
    public final String name;

    public LastModifiedRegistration(String name) {
        this.name = name;
    }

    public Promise<?> waitFor(ActorLastModified notBefore) {
        log("Executing query after response for " + notBefore);
        Promise<String> p = Futures.promise();

        Instant lastKnownMoment = lastModifiedRegistration.get(notBefore.getCaseInstanceId());
        if (lastKnownMoment == null) {
            if (notBefore.getLastModified().isBefore(startupMoment)) {
                p.success("That's quite an old timestamp; we're not gonna wait for it; we started at " + startupMoment);
            } else {
                log("Adding waiter for actor[" + notBefore.getCaseInstanceId() + "] modified at " + notBefore.getLastModified());
                addWaiter(new Waiter(notBefore, p));
            }
        } else if (lastKnownMoment.isBefore(notBefore.getLastModified())) {
            log("Adding waiter for entity " + notBefore.getCaseInstanceId() + ", because last known moment is " + lastKnownMoment + ", and we're waiting for " + notBefore.getLastModified());
            addWaiter(new Waiter(notBefore, p));
        } else {
            log("Returning because already available");
            p.success("Your case last modified arrived already!");
        }
        return p;
    }

    // TEMPORARY LOGGING CODE
    private void log(String msg) {
        msg = name + " in thread: " + Thread.currentThread().getName() + ": " + msg;
        logger.debug(msg);
    }

    public void handle(TransactionEvent<?> event) {
        handle(event.getActorId(), event.lastModified());
    }

    private void handle(String actorId, Instant newTimestamp) {
        Instant lastKnownTimestamp = lastModifiedRegistration.get(actorId);
        // Now check whether the new timestamp is indeed newer, and if so, update the registration
        if (lastKnownTimestamp == null || lastKnownTimestamp.isBefore(newTimestamp)) {
            lastModifiedRegistration.put(actorId, newTimestamp);
            informWaiters(actorId, newTimestamp);
        }
    }

    private void informWaiters(String id, Instant newTimestamp) {
        // TODO: should this be synchronized code?? I think so... Or can we better use scala immutable maps?
        synchronized (waiters) {
            List<Waiter> waiterList = waiters.remove(id);
            List<Waiter> newWaiters = new ArrayList<>();
            if (waiterList == null) {
                return;
            }

            log("Found " + newTimestamp + "/" + id+" for " + waiterList.size() + " waiters");
            for (Waiter waiter : waiterList) {
                if (newTimestamp.isBefore(waiter.moment())) {
                    log("-need " + waiter.notBefore.getLastModified() + "/" + waiter.notBefore.getCaseInstanceId());
                    newWaiters.add(waiter);
                } else {
                    waiter.stopWaiting();
                }
            }

            if (!newWaiters.isEmpty()) {
                waiters.put(id, newWaiters);
            }
        }
    }

    private void addWaiter(Waiter waiter) {
        synchronized (waiters) {
            List<Waiter> waiterList = waiters.computeIfAbsent(waiter.id(), k -> new ArrayList<>());
            waiterList.add(waiter);
        }
    }

    class Waiter {
        private final ActorLastModified notBefore;
        private final Promise<String> promise;
        private final long createdAt = System.currentTimeMillis();

        Waiter(ActorLastModified notBefore, Promise<String> promise) {

            this.notBefore = notBefore;
            this.promise = promise;
        }

        void stopWaiting() {
            log("Waited " + (System.currentTimeMillis() - createdAt) + " milliseconds");
            if (!promise.isCompleted()) {
                // Only invoke the promise if no one has done it yet
                promise.success("Your case last modified arrived just now");
            } else {
                log("AFTER STOP WAITING, BUT ALREADY COMPLETED?!");
            }
        }

        @Override
        public String toString() {
            return "Waiter[" + notBefore.toString() + "]";
        }

        String id() {
            return notBefore.getCaseInstanceId();
        }

        Instant moment() {
            return notBefore.getLastModified();
        }
    }
}

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

package org.cafienne.persistence.infrastructure.lastmodified;

import org.apache.pekko.dispatch.Futures;
import org.cafienne.actormodel.event.ActorModified;
import org.cafienne.actormodel.response.ActorLastModified;
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

    public Promise<String> waitFor(ActorLastModified notBefore) {
        log("Executing query after response for " + notBefore);
        Promise<String> p = Futures.promise();

        Instant lastKnownMoment = lastModifiedRegistration.get(notBefore.getActorId());
        if (lastKnownMoment == null) {
            if (notBefore.getLastModified().isBefore(startupMoment)) {
                p.success("That's quite an old timestamp; we're not gonna wait for it; we started at " + startupMoment);
            } else {
                log("Adding waiter for actor[" + notBefore.getActorId() + "] modified at " + notBefore.getLastModified());
                addWaiter(new Waiter(notBefore, p));
            }
        } else if (lastKnownMoment.isBefore(notBefore.getLastModified())) {
            log("Adding waiter for entity " + notBefore.getActorId() + ", because last known moment is " + lastKnownMoment + ", and we're waiting for " + notBefore.getLastModified());
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

    public void handle(ActorModified event) {
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
                    log("-need " + waiter.notBefore.getLastModified() + "/" + waiter.notBefore.getActorId());
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
            return notBefore.getActorId();
        }

        Instant moment() {
            return notBefore.getLastModified();
        }
    }
}

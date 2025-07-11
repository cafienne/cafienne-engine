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

package org.cafienne.infrastructure.serialization.serializers;

import org.cafienne.actormodel.response.*;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupCreatedResponse;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupResponse;
import org.cafienne.engine.cmmn.actorapi.response.*;
import org.cafienne.engine.cmmn.actorapi.response.migration.MigrationStartedResponse;
import org.cafienne.engine.humantask.actorapi.response.HumanTaskResponse;
import org.cafienne.engine.humantask.actorapi.response.HumanTaskValidationResponse;
import org.cafienne.engine.processtask.actorapi.response.ProcessResponse;
import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.tenant.actorapi.response.TenantOwnersResponse;
import org.cafienne.tenant.actorapi.response.TenantResponse;

public class ResponseSerializers {
    public static void register() {
        addCaseResponses();
        addHumanTaskResponses();
        addProcessResponses();
        addFailureResponses();
        addTenantResponses();
        addConsentGroupResponses();
    }

    private static void addCaseResponses() {
        CafienneSerializer.addManifestWrapper(AddDiscretionaryItemResponse.class, AddDiscretionaryItemResponse::new);
        CafienneSerializer.addManifestWrapper(GetDiscretionaryItemsResponse.class, GetDiscretionaryItemsResponse::new);
        CafienneSerializer.addManifestWrapper(CaseStartedResponse.class, CaseStartedResponse::new);
        CafienneSerializer.addManifestWrapper(MigrationStartedResponse.class, MigrationStartedResponse::new);
        CafienneSerializer.addManifestWrapper(CaseResponse.class, CaseResponse::new);
        CafienneSerializer.addManifestWrapper(CaseNotModifiedResponse.class, CaseNotModifiedResponse::new);
    }

    private static void addHumanTaskResponses() {
        CafienneSerializer.addManifestWrapper(HumanTaskResponse.class, HumanTaskResponse::new);
        CafienneSerializer.addManifestWrapper(HumanTaskValidationResponse.class, HumanTaskValidationResponse::new);
    }

    private static void addProcessResponses() {
        CafienneSerializer.addManifestWrapper(ProcessResponse.class, ProcessResponse::new);
    }

    private static void addFailureResponses() {
        CafienneSerializer.addManifestWrapper(CommandFailure.class, CommandFailure::new);
        CafienneSerializer.addManifestWrapper(SecurityFailure.class, SecurityFailure::new);
        CafienneSerializer.addManifestWrapper(ActorChokedFailure.class, ActorChokedFailure::new);
        CafienneSerializer.addManifestWrapper(ActorExistsFailure.class, ActorExistsFailure::new);
        CafienneSerializer.addManifestWrapper(ActorInStorage.class, ActorInStorage::new);
        CafienneSerializer.addManifestWrapper(EngineChokedFailure.class, EngineChokedFailure::new);
    }

    private static void addTenantResponses() {
        CafienneSerializer.addManifestWrapper(TenantOwnersResponse.class, TenantOwnersResponse::new);
        CafienneSerializer.addManifestWrapper(TenantResponse.class, TenantResponse::new);
    }

    private static void addConsentGroupResponses() {
        CafienneSerializer.addManifestWrapper(ConsentGroupCreatedResponse.class, ConsentGroupCreatedResponse::new);
        CafienneSerializer.addManifestWrapper(ConsentGroupResponse.class, ConsentGroupResponse::new);
    }
}

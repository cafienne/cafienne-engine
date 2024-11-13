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

package com.casefabric.infrastructure.serialization.serializers;

import com.casefabric.actormodel.response.*;
import com.casefabric.cmmn.actorapi.response.*;
import com.casefabric.cmmn.actorapi.response.migration.MigrationStartedResponse;
import com.casefabric.consentgroup.actorapi.response.ConsentGroupCreatedResponse;
import com.casefabric.consentgroup.actorapi.response.ConsentGroupResponse;
import com.casefabric.humantask.actorapi.response.HumanTaskResponse;
import com.casefabric.humantask.actorapi.response.HumanTaskValidationResponse;
import com.casefabric.infrastructure.serialization.CaseFabricSerializer;
import com.casefabric.processtask.actorapi.response.ProcessResponse;
import com.casefabric.tenant.actorapi.response.TenantOwnersResponse;
import com.casefabric.tenant.actorapi.response.TenantResponse;

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
        CaseFabricSerializer.addManifestWrapper(AddDiscretionaryItemResponse.class, AddDiscretionaryItemResponse::new);
        CaseFabricSerializer.addManifestWrapper(GetDiscretionaryItemsResponse.class, GetDiscretionaryItemsResponse::new);
        CaseFabricSerializer.addManifestWrapper(CaseStartedResponse.class, CaseStartedResponse::new);
        CaseFabricSerializer.addManifestWrapper(MigrationStartedResponse.class, MigrationStartedResponse::new);
        CaseFabricSerializer.addManifestWrapper(CaseResponse.class, CaseResponse::new);
        CaseFabricSerializer.addManifestWrapper(CaseNotModifiedResponse.class, CaseNotModifiedResponse::new);
    }

    private static void addHumanTaskResponses() {
        CaseFabricSerializer.addManifestWrapper(HumanTaskResponse.class, HumanTaskResponse::new);
        CaseFabricSerializer.addManifestWrapper(HumanTaskValidationResponse.class, HumanTaskValidationResponse::new);
    }

    private static void addProcessResponses() {
        CaseFabricSerializer.addManifestWrapper(ProcessResponse.class, ProcessResponse::new);
    }

    private static void addFailureResponses() {
        CaseFabricSerializer.addManifestWrapper(CommandFailure.class, CommandFailure::new);
        CaseFabricSerializer.addManifestWrapper(SecurityFailure.class, SecurityFailure::new);
        CaseFabricSerializer.addManifestWrapper(ActorChokedFailure.class, ActorChokedFailure::new);
        CaseFabricSerializer.addManifestWrapper(ActorExistsFailure.class, ActorExistsFailure::new);
        CaseFabricSerializer.addManifestWrapper(ActorInStorage.class, ActorInStorage::new);
        CaseFabricSerializer.addManifestWrapper(EngineChokedFailure.class, EngineChokedFailure::new);
    }

    private static void addTenantResponses() {
        CaseFabricSerializer.addManifestWrapper(TenantOwnersResponse.class, TenantOwnersResponse::new);
        CaseFabricSerializer.addManifestWrapper(TenantResponse.class, TenantResponse::new);
    }

    private static void addConsentGroupResponses() {
        CaseFabricSerializer.addManifestWrapper(ConsentGroupCreatedResponse.class, ConsentGroupCreatedResponse::new);
        CaseFabricSerializer.addManifestWrapper(ConsentGroupResponse.class, ConsentGroupResponse::new);
    }
}

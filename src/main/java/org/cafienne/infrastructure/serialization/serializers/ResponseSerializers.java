package org.cafienne.infrastructure.serialization.serializers;

import org.cafienne.actormodel.response.*;
import org.cafienne.cmmn.actorapi.response.*;
import org.cafienne.cmmn.actorapi.response.migration.MigrationStartedResponse;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupCreatedResponse;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupResponse;
import org.cafienne.humantask.actorapi.response.HumanTaskResponse;
import org.cafienne.humantask.actorapi.response.HumanTaskValidationResponse;
import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.processtask.actorapi.response.ProcessResponse;
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

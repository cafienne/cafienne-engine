package org.cafienne.infrastructure.serialization;

import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.EngineChokedFailure;
import org.cafienne.actormodel.response.SecurityFailure;
import org.cafienne.cmmn.actorapi.response.*;
import org.cafienne.humantask.actorapi.response.HumanTaskResponse;
import org.cafienne.humantask.actorapi.response.HumanTaskValidationResponse;
import org.cafienne.platform.actorapi.response.PlatformResponse;
import org.cafienne.platform.actorapi.response.PlatformUpdateStatus;
import org.cafienne.processtask.actorapi.response.ProcessResponse;
import org.cafienne.tenant.actorapi.response.TenantOwnersResponse;
import org.cafienne.tenant.actorapi.response.TenantResponse;

public class ResponseSerializer extends CafienneSerializer {
    public static void register() {
        addCaseResponses();
        addHumanTaskResponses();
        addProcessResponses();
        addFailureResponses();
        addTenantResponses();
        addPlatformResponses();
    }

    private static void addCaseResponses() {
        addManifestWrapper(AddDiscretionaryItemResponse.class, AddDiscretionaryItemResponse::new);
        addManifestWrapper(GetDiscretionaryItemsResponse.class, GetDiscretionaryItemsResponse::new);
        addManifestWrapper(CaseStartedResponse.class, CaseStartedResponse::new);
        addManifestWrapper(CaseResponse.class, CaseResponse::new);
        addManifestWrapper(CaseNotModifiedResponse.class, CaseNotModifiedResponse::new);
    }

    private static void addHumanTaskResponses() {
        addManifestWrapper(HumanTaskResponse.class, HumanTaskResponse::new);
        addManifestWrapper(HumanTaskValidationResponse.class, HumanTaskValidationResponse::new);
    }

    private static void addProcessResponses() {
        addManifestWrapper(ProcessResponse.class, ProcessResponse::new);
    }

    private static void addFailureResponses() {
        addManifestWrapper(CommandFailure.class, CommandFailure::new);
        addManifestWrapper(SecurityFailure.class, SecurityFailure::new);
        addManifestWrapper(EngineChokedFailure.class, EngineChokedFailure::new);
    }

    private static void addTenantResponses() {
        addManifestWrapper(TenantOwnersResponse.class, TenantOwnersResponse::new);
        addManifestWrapper(TenantResponse.class, TenantResponse::new);
    }

    private static void addPlatformResponses() {
        addManifestWrapper(PlatformResponse.class, PlatformResponse::new);
        addManifestWrapper(PlatformUpdateStatus.class, PlatformUpdateStatus::new);
    }
}

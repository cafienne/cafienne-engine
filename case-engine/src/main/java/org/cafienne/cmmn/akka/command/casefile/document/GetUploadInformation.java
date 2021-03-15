package org.cafienne.cmmn.akka.command.casefile.document;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.casefile.CaseFileCommand;
import org.cafienne.cmmn.akka.command.response.file.CaseFileResponse;
import org.cafienne.cmmn.akka.command.response.file.UploadInformation;
import org.cafienne.cmmn.instance.casefile.CaseFile;

/**
 * Retrieve information from the case definition which type of documents can be uploaded
 */
@Manifest
public class GetUploadInformation extends CaseFileCommand {
    /**
     * Deletes the case file item.
     *
     * @param caseInstanceId The id of the case in which to perform this command.
     */
    public GetUploadInformation(TenantUser tenantUser, String caseInstanceId) {
        super(tenantUser, caseInstanceId);
    }

    public GetUploadInformation(ValueMap json) {
        super(json);
    }

    @Override
    protected CaseFileResponse apply(CaseFile caseFile) {
        return new UploadInformation(this, caseFile);
    }
}

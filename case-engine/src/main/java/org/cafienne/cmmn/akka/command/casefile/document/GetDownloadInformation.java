package org.cafienne.cmmn.akka.command.casefile.document;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.casefile.CaseFileCommand;
import org.cafienne.cmmn.akka.command.response.file.CaseFileResponse;
import org.cafienne.cmmn.akka.command.response.file.DownloadInformation;
import org.cafienne.cmmn.instance.casefile.CaseFile;

/**
 * Retrieve information from the case definition which type of documents can be downloaded
 */
@Manifest
public class GetDownloadInformation extends CaseFileCommand {
    /**
     * Deletes the case file item.
     *
     * @param caseInstanceId The id of the case in which to perform this command.
     */
    public GetDownloadInformation(TenantUser tenantUser, String caseInstanceId) {
        super(tenantUser, caseInstanceId);
    }

    public GetDownloadInformation(ValueMap json) {
        super(json);
    }

    @Override
    protected CaseFileResponse apply(CaseFile caseFile) {
        return new DownloadInformation(this, caseFile);
    }
}

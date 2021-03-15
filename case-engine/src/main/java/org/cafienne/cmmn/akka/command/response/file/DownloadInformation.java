package org.cafienne.cmmn.akka.command.response.file;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.casefile.document.GetDownloadInformation;
import org.cafienne.cmmn.instance.casefile.CaseFile;
import org.cafienne.cmmn.instance.casefile.CaseFileItemCollection;

@Manifest
public class DownloadInformation extends CaseFileResponse {
    public DownloadInformation(GetDownloadInformation command, CaseFile caseFile) {
        super(command);
        analyzeDownloadInformation(caseFile);
    }

    private void analyzeDownloadInformation(CaseFileItemCollection<?> parent) {
        parent.getItems().forEach((definition, item) -> {
            if (item.hasStorageInformation()) {
                item.getStorageInformation().forEach(storageInformation -> {
                    String url = "/cases/"+parent.getCaseInstance().getId()+"/casefile/download/" + item.getPath() +"/" + storageInformation.identifier();
                    ValueMap v = new ValueMap("path", item.getPath(), "url", url, "content-type", storageInformation.contentType());
                    information.add(v);
                });
            }
            analyzeDownloadInformation(item);
        });
    }

    public DownloadInformation(ValueMap json) {
        super(json);
    }
}

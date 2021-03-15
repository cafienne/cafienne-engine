package org.cafienne.cmmn.akka.command.response.file;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.casefile.document.GetUploadInformation;
import org.cafienne.cmmn.definition.casefile.CaseFileItemCollectionDefinition;
import org.cafienne.cmmn.instance.casefile.CaseFile;

@Manifest
public class UploadInformation extends CaseFileResponse {

    public UploadInformation(GetUploadInformation command, CaseFile caseFile) {
        super(command);
        analyzeUploadInformation(caseFile.getDefinition());
    }

    private void analyzeUploadInformation(CaseFileItemCollectionDefinition parent) {
        parent.getChildren().forEach(definition -> {
            if (definition.getCaseFileItemDefinition().getDefinitionType().isDocument()) {
                information.add(new ValueMap("path", definition.getPath()));
            }
            analyzeUploadInformation(definition);
        });
    }

    public UploadInformation(ValueMap json) {
        super(json);
    }
}

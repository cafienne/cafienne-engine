package org.cafienne.cmmn.akka.command.casefile;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition;

import java.io.IOException;

public abstract class CaseFileCommand extends CaseCommand {
    protected final Value<?> content;
    protected final CaseFileItemTransition intendedTransition;

    /**
     * Determine path and content for the CaseFileItem to be touched.
     *  @param caseInstanceId   The id of the case in which to perform this command.
     * @param newContent         A value structure with contents of the new case file item
     * @param intendedTransition
     */
    protected CaseFileCommand(TenantUser tenantUser, String caseInstanceId, Value<?> newContent, CaseFileItemTransition intendedTransition) {
        super(tenantUser, caseInstanceId);
        this.content = newContent;
        this.intendedTransition = intendedTransition;
    }

    protected CaseFileCommand(ValueMap json, CaseFileItemTransition intendedTransition) {
        super(json);
        this.content = json.get(Fields.content.toString());
        this.intendedTransition = intendedTransition;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.content, content);
    }
}

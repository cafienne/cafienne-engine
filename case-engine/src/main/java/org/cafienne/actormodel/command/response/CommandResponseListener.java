package org.cafienne.actormodel.command.response;

import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;

/**
 * When sending a message to another model instance from within a model,
 * the method {@link Case#askCase(CaseCommand, CommandFailureListener, CommandResponseListener...)} can be used.
 * The case instance will respond to "this" case and this case will invoke the registered response listener.
 * This basically supports a simplified ask pattern between cases.
 *
 */
@FunctionalInterface
public interface CommandResponseListener {
    /**
     * The handleResponse method can be implemented to handle a valid {@link CaseResponse} coming back as a result of sending a command to another model.
     * @param response
     */
    void handleResponse(ModelResponse response);
}

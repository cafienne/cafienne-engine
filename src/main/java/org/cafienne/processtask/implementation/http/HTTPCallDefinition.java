/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.http;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.expression.spel.api.APIRootObject;
import org.cafienne.cmmn.expression.spel.api.process.InputMappingRoot;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.cmmn.instance.task.validation.TaskOutputValidator;
import org.cafienne.cmmn.instance.task.validation.TaskValidatorRootAPI;
import org.cafienne.processtask.definition.SubProcessInputMappingDefinition;
import org.cafienne.processtask.definition.SubProcessDefinition;
import org.cafienne.processtask.implementation.http.definition.ContentDefinition;
import org.cafienne.processtask.implementation.http.definition.HeaderDefinition;
import org.cafienne.processtask.implementation.http.definition.MethodDefinition;
import org.cafienne.processtask.implementation.http.definition.URLDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

import java.util.*;
import java.util.stream.Collectors;

public class HTTPCallDefinition extends SubProcessDefinition {
    // Raw, hard coded output parameter names
    public static final String RESPONSE_PAYLOAD_PARAMETER = "responsePayload";
    public static final String RESPONSE_CODE_PARAMETER = "responseCode";
    public static final String RESPONSE_MESSAGE_PARAMETER = "responseMessage";
    public static final String RESPONSE_HEADERS_PARAMETER = "responseHeaders";
    private final ContentDefinition contentTemplate;
    private final MethodDefinition httpMethod;
    private final URLDefinition sourceURL;
    private final List<HeaderDefinition> httpHeaders = new ArrayList<>();

    public HTTPCallDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        this.sourceURL = parse("url", URLDefinition.class, true);
        this.httpMethod = parse("method", MethodDefinition.class, true);
        this.contentTemplate = parse("post-content", ContentDefinition.class, false);
        parseGrandChildren("http-headers", "http-header", HeaderDefinition.class, httpHeaders);
    }

    @Override
    public Set<String> getRawOutputParameterNames() {
        Set<String> pNames = super.getExceptionParameterNames();
        pNames.add(RESPONSE_CODE_PARAMETER);
        pNames.add(RESPONSE_HEADERS_PARAMETER);
        pNames.add(RESPONSE_MESSAGE_PARAMETER);
        pNames.add(RESPONSE_PAYLOAD_PARAMETER);
        return pNames;
    }

    public List<Header> getHeaders(APIRootObject<?> context) {
        return httpHeaders.stream().map(definedHeader -> definedHeader.getHeader(context)).collect(Collectors.toList());
    }

    public List<Header> getHeaders(ProcessTaskActor task) {
        return getHeaders(new InputMappingRoot(task));
    }

    public URLDefinition getURL() {
        return sourceURL;
    }

    public MethodDefinition getMethod() {
        return httpMethod;
    }

    public ContentDefinition getContent() {
        return contentTemplate;
    }

    @Override
    public HTTPCall createInstance(ProcessTaskActor processTaskActor) {
        return new HTTPCall(processTaskActor, this);
    }

    public TaskOutputValidator createValidator(HumanTask task) {
        return new TaskOutputValidator(this, task);
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameHTTPCall);
    }

    public boolean sameHTTPCall(HTTPCallDefinition other) {
        return sameSubProcess(other)
                && same(this.contentTemplate, other.contentTemplate)
                && same(httpMethod, other.httpMethod)
                && same(sourceURL, other.sourceURL)
                && same(httpHeaders, other.httpHeaders);
    }
}

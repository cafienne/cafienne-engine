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

package com.casefabric.processtask.implementation.http;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.cmmn.expression.spel.api.APIRootObject;
import com.casefabric.cmmn.expression.spel.api.process.InputMappingRoot;
import com.casefabric.cmmn.instance.task.humantask.HumanTask;
import com.casefabric.cmmn.instance.task.validation.TaskOutputValidator;
import com.casefabric.processtask.definition.SubProcessDefinition;
import com.casefabric.processtask.implementation.http.definition.ContentDefinition;
import com.casefabric.processtask.implementation.http.definition.HeaderDefinition;
import com.casefabric.processtask.implementation.http.definition.MethodDefinition;
import com.casefabric.processtask.implementation.http.definition.URLDefinition;
import com.casefabric.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    public boolean equalsWith(Object object) {
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

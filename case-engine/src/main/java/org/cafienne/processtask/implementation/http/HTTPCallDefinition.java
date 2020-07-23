/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.http;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.cmmn.instance.task.validation.TaskOutputValidator;
import org.cafienne.processtask.definition.SubProcessDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.cafienne.util.StringTemplate;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class HTTPCallDefinition extends SubProcessDefinition {
    private final String contentTemplate;
    private final String httpMethod;
    private final String sourceURL;
    private final List<Header> httpHeaders = new ArrayList();

    // Raw, hard coded output parameter names
    public static final String RESPONSE_PAYLOAD_PARAMETER = "responsePayload";
    public static final String RESPONSE_CODE_PARAMETER = "responseCode";
    public static final String RESPONSE_MESSAGE_PARAMETER = "responseMessage";
    public static final String RESPONSE_HEADERS_PARAMETER = "responseHeaders";

    public HTTPCallDefinition(Element element, Definition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        this.sourceURL = parse("url", String.class, true);
        this.httpMethod = parse("method", String.class, true);
        this.contentTemplate = parse("post-content", String.class, false);
        Element httpHeadersElement = XMLHelper.getElement(element, "http-headers");
        if (httpHeadersElement != null) {
            Collection<Element> headerElements = XMLHelper.getChildrenWithTagName(httpHeadersElement, "http-header");
            for (Element headerElement : headerElements) {
                String headerName = headerElement.getAttribute("name");
                String headerValue = XMLHelper.getContent(headerElement, null, "");
                httpHeaders.add(new Header(headerName, headerValue));
            }
        }
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

    public List<Header> getHeaders() {
        return httpHeaders;
    }

    public StringTemplate getURL() {
        return new StringTemplate(sourceURL);
    }

    public StringTemplate getMethod() {
        return new StringTemplate(httpMethod);
    }

    public StringTemplate getContent() {
        return new StringTemplate(contentTemplate);
    }

    public class Header {
        private final String sourceName;
        private final String sourceValue;

        Header(String name, String value) {
            this.sourceName = name;
            this.sourceValue = value;
        }

        public String getName(ValueMap processInputParameters) {
            StringTemplate nameTemplate = new StringTemplate(sourceName);
            nameTemplate.resolveParameters(processInputParameters);
            return nameTemplate.toString();
        }

        public String getValue(ValueMap processInputParameters) {
            StringTemplate valueTemplate = new StringTemplate(sourceValue);
            valueTemplate.resolveParameters(processInputParameters);
            return valueTemplate.toString();
        }
    }

    @Override
    public HTTPCall createInstance(ProcessTaskActor processTaskActor) {
        return new HTTPCall(processTaskActor, this);
    }

    public TaskOutputValidator createValidator(HumanTask task) {
        return new TaskOutputValidator(this, task);
    }
}

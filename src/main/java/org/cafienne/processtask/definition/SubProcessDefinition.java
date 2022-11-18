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

package org.cafienne.processtask.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.definition.TaskDefinition;
import org.cafienne.cmmn.definition.parameter.OutputParameterDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Process Tasks in the engine can be implemented by extending {@link SubProcess}.
 * Sub process instances depend on their definition, which can be provided through the {@link SubProcessDefinition}.
 * <br/>
 * The sub process definition is implemented as an <code>implementation</code> element within the extension element of the process tag.
 * The specific class name is specified in the <code>class</code> attribute, e.g. <code>&lt;implementation class=&quot;fqn&quot;&gt;</code>.
 * <br/> The class must have a constructor that takes 3 parameters:
 * <ul>
 * <li>{@link org.w3c.dom.Element} - The xml element <code>implementation</code>, containing the content that can be parsed by the implementation.
 * <li>{@link ModelDefinition} - The Process definition to which this tag belongs
 * <li>{@link CMMNElementDefinition} - The parsed parent element; typically also the process definition
 * </ul>
 * <br/>
 * Furthermore for convenience, the definition can contain parameter mappings, in a manner similar to the {@link TaskDefinition#getParameterMappings()} parameter mappings.
 * Upon completion/failure of the sub process instance, the engine will map the raw, internal process output parameters to the CMMN compliant Process output parameters. In order to
 * enable this functionality, the sub process definition has to provide a list of possible raw output parameter names, which map to keys in the {@link ValueMap} that is passed
 * in the {@link SubProcess#raiseComplete()} and {@link SubProcess#raiseFault(String)} methods.
 * <br/>
 * Alternatively sub process instances may do this mapping themselves.
 * <p>
 * An example:
 * <code>
 * <br/>  &lt;process name=&quot;aSubProcess&quot; implementationType=&quot;http://www.omg.org/spec/CMMN/ProcessType/Unspecified&quot;&gt;
 * <br/>&nbsp;&nbsp;&lt;input id=&quot;inputParameter1&quot;&gt;
 * <br/>&nbsp;&nbsp;&lt;input id=&quot;inputParameter2&quot;&gt;
 * <br/>&nbsp;&nbsp;&lt;output id=&quot;outputParameter1&quot;&gt;
 * <br/>&nbsp;&nbsp;&lt;extensionElements xmlns:cafienne=&quot;org.cafienne&quot;&gt;
 * <br/>&nbsp;&nbsp;&nbsp;&nbsp;&lt;cafienne:implementation class=&quot;HTTPCallDefinition&quot;&gt;
 * <br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;	      ... custom content here, responsibility of the SubProcessDefinition
 * <br/>
 * <br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;              ... output parameter mappings for SubProcessDefinition will be parsed and validated by SubProcessDefinition
 * <br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;parameterMapping targetRef=&quot;outputParameter1&quot; sourceRef=&quot;outputParameter1&quot; /&gt;
 * <br/>&nbsp;&nbsp;&nbsp;&nbsp;&lt;/cafienne:implementation&gt;
 * <br/>&nbsp;&nbsp;	&lt;/extensionElements&gt;
 * <br/> &lt;/process&gt;
 * </code>
 * </p>
 */
public abstract class SubProcessDefinition extends CMMNElementDefinition {

    /**
     * Default exception parameter, can be used to store Throwables.
     * Note: these parameters are convention only.
     */
    public final static String EXCEPTION_PARAMETER = "exception";
    private final Collection<SubProcessOutputMappingDefinition> mappings = new ArrayList<>();
    private final Collection<SubProcessOutputMappingDefinition> successMappings = new ArrayList<>();
    private final Collection<SubProcessOutputMappingDefinition> failureMappings = new ArrayList<>();
    private final boolean isAsync;

    protected SubProcessDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        isAsync = Boolean.parseBoolean(parseAttribute("async", false, "true")); // By default, processes are executed asynchronously
        parse("parameterMapping", SubProcessOutputMappingDefinition.class, mappings);
        successMappings.addAll(mappings);
        failureMappings.addAll(mappings);
        parseGrandChildren("success", "parameterMapping", SubProcessOutputMappingDefinition.class, successMappings);
        parseGrandChildren("failure", "parameterMapping", SubProcessOutputMappingDefinition.class, failureMappings);
    }

    /**
     * If the SubProcessDefinition can be run _within_ the Case, then override this method to return true.
     * Note, and inline SubProcessDefinition is ran within the thread of the creation of the task in the case.
     * Also, the createInstance() method will be invoked with the Task as parameter, instead of the ProcessTaskActor.
     * This method returns false by default - every process runs within it's own Akka Actor context.
     *
     * @return
     */
    public boolean isInline() {
        return false;
    }

    /**
     * Create an instance of this definition that can provide the implementation for the process task
     *
     * @param processTaskActor
     * @return
     */
    public abstract SubProcess<?> createInstance(ProcessTaskActor processTaskActor);

    /**
     * Gets the parameter mappings for the sub-process
     * that must be executed when the subprocess succesfully completes
     *
     * @return parameter mapping
     */
    public Collection<SubProcessOutputMappingDefinition> getSuccessMappings() {
        return successMappings;
    }

    /**
     * Gets the parameter mappings for the sub-process
     * that must be executed upon a failing subprocess
     *
     * @return parameter mapping
     */
    public Collection<SubProcessOutputMappingDefinition> getFailureMappings() {
        return failureMappings;
    }

    /**
     * Returns true if this type of process needs to be executed asynchronously, or false if it must be executed
     * within the current thread upon activation of the process task. By default, process implementation are
     * executed asynchronously from the {@link Case} command handling mechanism.
     *
     * @return
     */
    public boolean isAsync() {
        return isAsync;
    }

    protected abstract Set<String> getRawOutputParameterNames();

    /**
     * Returns the default exception parameter name.
     *
     * @return
     */
    protected Set<String> getExceptionParameterNames() {
        Set<String> names = new HashSet<>();
        names.add(EXCEPTION_PARAMETER);
        return names;
    }

    public Map<String, OutputParameterDefinition> getRawOutputParameters() {
        Set<String> names = getRawOutputParameterNames();
        Map<String, OutputParameterDefinition> rawOutputParameters = new HashMap<>();
        for (String name : names) {
            Element xmlElement = getElement().getOwnerDocument().createElement("parameter");
            xmlElement.setAttribute("name", name);
            xmlElement.setAttribute("id", name);
            OutputParameterDefinition pd = new OutputParameterDefinition(xmlElement, null, this);
            rawOutputParameters.put(name, pd);
        }
        return rawOutputParameters;
    }

    public boolean sameType(SubProcessDefinition other) {
        return this.getClass().equals(other.getClass());
    }

    public boolean sameSubProcess(SubProcessDefinition other) {
        // Note: in principle there is no need to run first check below on the implementation type, as that is
        // also done by comparing classes before this method is invoked.
        // However, for "functional clarity" invoking it here as well.
        return sameType(other)
                && same(isAsync, other.isAsync)
                && same(mappings, other.mappings)
                && same(successMappings, other.successMappings)
                && same(failureMappings, other.failureMappings);
    }
}
